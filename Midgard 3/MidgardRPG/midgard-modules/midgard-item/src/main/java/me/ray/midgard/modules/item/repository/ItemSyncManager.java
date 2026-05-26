package me.ray.midgard.modules.item.repository;

import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.redis.RedisManager;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.item.ItemModule;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Gerencia a sincronização de itens entre servidores.
 * 
 * Inspirado nos melhores padrões de servidores MMO em larga escala:
 * - Redis pub/sub com reconexão automática para propagação instantânea
 * - Cache Redis para evitar hits desnecessários no banco de dados
 * - Atualização proativa de inventários de jogadores online
 * - Suporte a deploy atômico (batch update de múltiplos itens)
 * - Polling como fallback quando Redis não está disponível
 */
public class ItemSyncManager {

    private static final String CHANNEL_UPDATE = "midgard:item:update";
    private static final String CHANNEL_DELETE = "midgard:item:delete";
    private static final String CHANNEL_RELOAD = "midgard:item:reload_all";
    private static final String CHANNEL_BATCH_UPDATE = "midgard:item:batch_update";
    private static final String REDIS_CACHE_KEY = "midgard:items:cache";
    private static final String CACHE_SEPARATOR = "\n---MIDGARD_SPLIT---\n";

    private final ItemModule module;
    private final ItemRepository repository;
    private final RedisManager redisManager;
    private final String serverId;
    private final boolean useRedis;
    private final boolean proactiveUpdate;

    private volatile long lastPollTimestamp;
    private org.bukkit.scheduler.BukkitTask pollingTask;

    public ItemSyncManager(ItemModule module, ItemRepository repository, RedisManager redisManager) {
        this.module = module;
        this.repository = repository;
        this.redisManager = redisManager;
        this.serverId = generateServerId();
        this.lastPollTimestamp = System.currentTimeMillis();
        this.useRedis = redisManager != null && redisManager.isEnabled();
        this.proactiveUpdate = module.getConfig().getBoolean("sync.proactive-update", true);
    }

    /**
     * Inicia a sincronização (Redis ou Polling).
     * Detecta configurações incompatíveis (ex: SQLite em multi-server).
     */
    public void start() {
        // Avisar sobre SQLite em setup multi-server
        warnIfSQLiteMultiServer();

        if (useRedis) {
            startRedisSubscribers();
            MidgardLogger.info("[ItemSync] Sincronização via Redis habilitada (proactive-update: %s).", proactiveUpdate);
        } else {
            startPolling();
            MidgardLogger.info("[ItemSync] Redis não disponível. Sincronização via polling a cada "
                + module.getConfig().getLong("sync.poll-interval-seconds", 30) + "s.");
        }
    }

    /**
     * Notifica outros servidores que um item foi atualizado.
     * Também armazena o item no cache Redis para leitura rápida pelos outros servidores.
     */
    public void notifyUpdate(String itemId) {
        if (!useRedis) {
            return;
        }
        cacheItemToRedis(itemId);
        redisManager.publish(CHANNEL_UPDATE, serverId + ":" + itemId);
    }

    /**
     * Notifica outros servidores que um item foi deletado.
     */
    public void notifyDelete(String itemId) {
        if (!useRedis) {
            return;
        }
        removeCacheFromRedis(itemId);
        redisManager.publish(CHANNEL_DELETE, serverId + ":" + itemId);
    }

    /**
     * Notifica outros servidores para recarregar todos os itens.
     */
    public void notifyReloadAll() {
        if (useRedis) {
            redisManager.publish(CHANNEL_RELOAD, serverId);
        }
    }

    /**
     * Deploy atômico: notifica outros servidores que múltiplos itens foram atualizados.
     * Os servidores receptores atualizam todos os itens antes de atualizar inventários,
     * garantindo consistência (como um "patch" do Wynncraft).
     *
     * @param itemIds IDs dos itens atualizados
     */
    public void notifyBatchUpdate(Collection<String> itemIds) {
        if (!useRedis || itemIds == null || itemIds.isEmpty()) {
            return;
        }
        for (String itemId : itemIds) {
            cacheItemToRedis(itemId);
        }
        String payload = serverId + ":" + String.join(",", itemIds);
        redisManager.publish(CHANNEL_BATCH_UPDATE, payload);
        MidgardLogger.info("[ItemSync] Batch update enviado com %d itens.", itemIds.size());
    }

    /**
     * Para a sincronização.
     */
    public void shutdown() {
        if (pollingTask != null) {
            try { pollingTask.cancel(); } catch (Exception ignored) { /* Shutdown cleanup */ }
        }
    }

    /**
     * @return true se está usando Redis para sincronização
     */
    public boolean isRedisMode() {
        return useRedis;
    }

    // ─── Redis Cache Layer ─────────────────────────────────────────
    // Cache distribuído via Redis Hash — evita hit no banco a cada sync.
    // Inspirado no padrão de cache de servidores MMO em larga escala.

    private void cacheItemToRedis(String itemId) {
        if (module.getItemManager() == null) {
            return;
        }
        me.ray.midgard.modules.item.model.MidgardItem item = module.getItemManager().getMidgardItem(itemId);
        if (item instanceof me.ray.midgard.modules.item.model.MidgardItemImpl impl) {
            String categoryId = impl.getCategoryId();
            String yamlData = impl.serializeConfig();
            String cacheValue = categoryId + CACHE_SEPARATOR + yamlData;
            redisManager.executeAsync(jedis -> jedis.hset(REDIS_CACHE_KEY, itemId, cacheValue));
        }
    }

    private void removeCacheFromRedis(String itemId) {
        redisManager.executeAsync(jedis -> jedis.hdel(REDIS_CACHE_KEY, itemId));
    }

    /**
     * Tenta carregar um item do cache Redis. Se não encontrado, faz fallback para o banco.
     */
    private void loadItemWithCache(String itemId, java.util.function.Consumer<ItemRepository.ItemData> callback) {
        // Tentar Redis cache primeiro
        if (useRedis) {
            redisManager.executeAsync(jedis -> {
                String cached = jedis.hget(REDIS_CACHE_KEY, itemId);
                if (cached != null && cached.contains(CACHE_SEPARATOR)) {
                    int splitIdx = cached.indexOf(CACHE_SEPARATOR);
                    String categoryId = cached.substring(0, splitIdx);
                    String yamlData = cached.substring(splitIdx + CACHE_SEPARATOR.length());
                    ItemRepository.ItemData data = new ItemRepository.ItemData(itemId, categoryId, yamlData);
                    MidgardLogger.debug(DebugCategory.ITEMS,
                        "[ItemSync] Item %s carregado do cache Redis.", itemId);
                    callback.accept(data);
                    return;
                }
                // Cache miss — fallback para DB
                loadItemFromDb(itemId, callback);
            });
        } else {
            loadItemFromDb(itemId, callback);
        }
    }

    private void loadItemFromDb(String itemId, java.util.function.Consumer<ItemRepository.ItemData> callback) {
        repository.loadItem(itemId).thenAccept(itemData -> {
            callback.accept(itemData);
        });
    }

    // ─── Redis Mode ────────────────────────────────────────────────

    private void startRedisSubscribers() {
        redisManager.subscribe(CHANNEL_UPDATE, new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (message.startsWith(serverId + ":")) {
                    return;
                }
                String itemId = extractPayload(message);
                handleItemUpdate(itemId);
            }
        });

        redisManager.subscribe(CHANNEL_DELETE, new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (message.startsWith(serverId + ":")) {
                    return;
                }
                String itemId = extractPayload(message);
                handleItemDelete(itemId);
            }
        });

        redisManager.subscribe(CHANNEL_RELOAD, new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (message.equals(serverId) || message.startsWith(serverId + ":")) {
                    return;
                }
                handleReloadAll();
            }
        });

        redisManager.subscribe(CHANNEL_BATCH_UPDATE, new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (message.startsWith(serverId + ":")) {
                    return;
                }
                String payload = extractPayload(message);
                handleBatchUpdate(payload);
            }
        });
    }

    // ─── Polling Mode (fallback quando Redis não disponível) ───────

    private void startPolling() {
        long intervalTicks = module.getConfig().getLong("sync.poll-interval-seconds", 30) * 20L;
        this.pollingTask = Task.syncTimer(() -> pollForChanges(), intervalTicks, intervalTicks);
    }

    private void pollForChanges() {
        long pollTime = lastPollTimestamp;
        lastPollTimestamp = System.currentTimeMillis();

        // Detectar itens modificados
        repository.getModifiedSince(pollTime).thenAccept(modifiedIds -> {
            if (modifiedIds == null || modifiedIds.isEmpty()) {
                return;
            }
            MidgardLogger.debug(DebugCategory.ITEMS,
                "[ItemSync] Polling detectou %d itens modificados.", modifiedIds.size());
            for (String itemId : modifiedIds) {
                handleItemUpdate(itemId);
            }
        });

        // Detectar itens deletados
        repository.getAllIds().thenAccept(dbIds -> {
            if (dbIds == null || module.getItemManager() == null) {
                return;
            }
            List<String> toRemove = new ArrayList<>();
            for (String loadedId : module.getItemManager().getItemIds()) {
                if (!dbIds.contains(loadedId)) {
                    toRemove.add(loadedId);
                }
            }
            if (!toRemove.isEmpty()) {
                Task.sync(() -> {
                    for (String id : toRemove) {
                        module.getItemManager().unregisterItem(id);
                        if (module.getRecipeManager() != null) {
                            module.getRecipeManager().removeItemRecipesById(id);
                        }
                        MidgardLogger.debug(DebugCategory.ITEMS,
                            "[ItemSync] Item %s removido (detectado via polling).", id);
                    }
                    // Atualizar jogadores online após remoções
                    if (proactiveUpdate) {
                        module.getItemManager().updateAllOnlinePlayers();
                    }
                });
            }
        });
    }

    // ─── Handlers (executados na thread principal) ──────────────────

    private void handleItemUpdate(String itemId) {
        loadItemWithCache(itemId, itemData -> {
            if (itemData == null) {
                return;
            }
            Task.sync(() -> {
                module.getItemManager().reloadItemFromDb(itemId, itemData);
                if (module.getRecipeManager() != null) {
                    module.getRecipeManager().updateItemRecipes(
                        module.getItemManager().getMidgardItem(itemId));
                }
                MidgardLogger.debug(DebugCategory.ITEMS,
                    "[ItemSync] Item %s atualizado via sync.", itemId);

                // Atualizar proativamente inventários de jogadores online
                if (proactiveUpdate) {
                    module.getItemManager().updateAllOnlinePlayers();
                }
            });
        });
    }

    private void handleItemDelete(String itemId) {
        Task.sync(() -> {
            module.getItemManager().unregisterItem(itemId);
            if (module.getRecipeManager() != null) {
                module.getRecipeManager().removeItemRecipesById(itemId);
            }
            MidgardLogger.debug(DebugCategory.ITEMS,
                "[ItemSync] Item %s removido via sync.", itemId);
        });
    }

    private void handleReloadAll() {
        Task.sync(() -> {
            module.getItemManager().loadItems();
            if (module.getRecipeManager() != null) {
                module.getRecipeManager().reload();
            }
            MidgardLogger.info("[ItemSync] Reload completo de itens recebido de outro servidor.");

            // Atualizar todos os jogadores online após reload completo
            if (proactiveUpdate) {
                module.getItemManager().updateAllOnlinePlayers();
            }
        });
    }

    /**
     * Processa um batch update atômico — carrega todos os itens primeiro,
     * depois atualiza inventários uma única vez (evita N atualizações).
     */
    private void handleBatchUpdate(String payload) {
        String[] itemIds = payload.split(",");
        MidgardLogger.info("[ItemSync] Batch update recebido com %d itens.", itemIds.length);

        // Contador atômico para saber quando todos os itens foram carregados
        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(itemIds.length);

        for (String itemId : itemIds) {
            String trimmedId = itemId.trim();
            if (trimmedId.isEmpty()) {
                remaining.decrementAndGet();
                continue;
            }
            loadItemWithCache(trimmedId, itemData -> {
                if (itemData == null) {
                    if (remaining.decrementAndGet() <= 0 && proactiveUpdate) {
                        Task.sync(() -> {
                            MidgardLogger.info("[ItemSync] Batch completo. Atualizando jogadores online...");
                            module.getItemManager().updateAllOnlinePlayers();
                        });
                    }
                    return;
                }
                Task.sync(() -> {
                    module.getItemManager().reloadItemFromDb(trimmedId, itemData);
                    if (module.getRecipeManager() != null) {
                        module.getRecipeManager().updateItemRecipes(
                            module.getItemManager().getMidgardItem(trimmedId));
                    }
                    MidgardLogger.debug(DebugCategory.ITEMS,
                        "[ItemSync] Item %s atualizado (batch).", trimmedId);

                    // Atualizar jogadores somente quando o último item do batch for carregado
                    if (remaining.decrementAndGet() <= 0 && proactiveUpdate) {
                        MidgardLogger.info("[ItemSync] Batch completo. Atualizando jogadores online...");
                        module.getItemManager().updateAllOnlinePlayers();
                    }
                });
            });
        }
    }

    // ─── Detecção de Configuração Incompatível ─────────────────────

    private void warnIfSQLiteMultiServer() {
        DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager == null) {
            return;
        }
        String dbType = dbManager.getDatabaseType();
        if ("sqlite".equalsIgnoreCase(dbType) && useRedis) {
            MidgardLogger.warn("==========================================================");
            MidgardLogger.warn("[ItemSync] ATENÇÃO: Usando SQLite com Redis habilitado!");
            MidgardLogger.warn("[ItemSync] SQLite é um banco local — sincronização entre");
            MidgardLogger.warn("[ItemSync] servidores NÃO funcionará. Use MySQL para setup");
            MidgardLogger.warn("[ItemSync] multi-server.");
            MidgardLogger.warn("==========================================================");
        }
    }

    // ─── Utilitários ───────────────────────────────────────────────

    private String extractPayload(String message) {
        int colonIndex = message.indexOf(':');
        return colonIndex >= 0 ? message.substring(colonIndex + 1) : message;
    }

    private String generateServerId() {
        int port = org.bukkit.Bukkit.getServer().getPort();
        String hex = String.format("%016x", System.nanoTime());
        return "srv-" + port + "-" + hex.substring(hex.length() - 6);
    }
}
