package me.ray.midgard.core.sync;

import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.redis.RedisManager;
import me.ray.midgard.core.utils.Task;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gerenciador genérico de sincronização entre servidores.
 * <p>
 * Dois modos de operação:
 * - Redis (preferido): pub/sub para propagação instantânea
 * - Polling (fallback): consulta periódica ao banco quando Redis não está disponível
 * <p>
 * Reutilizável por qualquer módulo: races, classes, spells, warps, etc.
 */
public class DefinitionSyncManager {

    private final String moduleName;
    private final DefinitionRepository repository;
    private final RedisManager redisManager;
    private final boolean useRedis;
    private final String serverId;

    private final String channelUpdate;
    private final String channelDelete;
    private final String channelReload;

    private final Consumer<String> onUpdate;
    private final Consumer<String> onDelete;
    private final Runnable onReloadAll;
    private final Consumer<List<String>> onDeleteBatch;

    private volatile long lastPollTimestamp;
    private org.bukkit.scheduler.BukkitTask pollingTask;
    private final List<JedisPubSub> activeSubscribers = new ArrayList<>();

    /**
     * @param moduleName       Nome do módulo (ex: "races", "classes")
     * @param repository       Repositório de definições
     * @param redisManager     RedisManager (pode ser null)
     * @param pollIntervalSec  Intervalo de polling em segundos (quando Redis não disponível)
     * @param onUpdate         Callback quando um item é atualizado (recebe itemId)
     * @param onDelete         Callback quando um item é deletado (recebe itemId)
     * @param onReloadAll      Callback para reload completo
     * @param currentIdsSupplier Supplier dos IDs atualmente carregados (para detectar deleções no polling)
     */
    public DefinitionSyncManager(
            String moduleName,
            DefinitionRepository repository,
            RedisManager redisManager,
            long pollIntervalSec,
            Consumer<String> onUpdate,
            Consumer<String> onDelete,
            Runnable onReloadAll,
            Consumer<List<String>> onDeleteBatch) {
        this.moduleName = moduleName;
        this.repository = repository;
        this.redisManager = redisManager;
        this.useRedis = redisManager != null && redisManager.isEnabled();
        this.serverId = generateServerId();
        this.lastPollTimestamp = System.currentTimeMillis();

        this.channelUpdate = "midgard:" + moduleName + ":update";
        this.channelDelete = "midgard:" + moduleName + ":delete";
        this.channelReload = "midgard:" + moduleName + ":reload_all";

        this.onUpdate = onUpdate;
        this.onDelete = onDelete;
        this.onReloadAll = onReloadAll;
        this.onDeleteBatch = onDeleteBatch;

        if (useRedis) {
            startRedisSubscribers();
            MidgardLogger.info("[" + moduleName + " Sync] Sincronização via Redis habilitada.");
        } else {
            startPolling(pollIntervalSec);
            MidgardLogger.info("[" + moduleName + " Sync] Redis não disponível. Polling a cada " + pollIntervalSec + "s.");
        }
    }

    public void notifyUpdate(String id) {
        if (useRedis) redisManager.publish(channelUpdate, serverId + ":" + id);
    }

    public void notifyDelete(String id) {
        if (useRedis) redisManager.publish(channelDelete, serverId + ":" + id);
    }

    public void notifyReloadAll() {
        if (useRedis) redisManager.publish(channelReload, serverId);
    }

    public void shutdown() {
        if (pollingTask != null) {
            try {
                pollingTask.cancel();
            } catch (Exception e) {
                MidgardLogger.warn("Erro ao cancelar polling task durante shutdown: " + e.getMessage());
            }
        }
        for (JedisPubSub sub : activeSubscribers) {
            try {
                sub.unsubscribe();
            } catch (Exception e) {
                MidgardLogger.warn("Erro ao desinscrever subscriber Redis durante shutdown: " + e.getMessage());
            }
        }
        activeSubscribers.clear();
    }

    public boolean isRedisMode() { return useRedis; }

    // ─── Redis ─────────────────────────────────────────────────────

    private void startRedisSubscribers() {
        JedisPubSub updateSub = new JedisPubSub() {
            @Override public void onMessage(String channel, String message) {
                if (message.startsWith(serverId + ":")) {
                    return;
                }
                String id = extractPayload(message);
                Task.sync(() -> onUpdate.accept(id));
            }
        };
        JedisPubSub deleteSub = new JedisPubSub() {
            @Override public void onMessage(String channel, String message) {
                if (message.startsWith(serverId + ":")) {
                    return;
                }
                String id = extractPayload(message);
                Task.sync(() -> onDelete.accept(id));
            }
        };
        JedisPubSub reloadSub = new JedisPubSub() {
            @Override public void onMessage(String channel, String message) {
                if (message.equals(serverId) || message.startsWith(serverId + ":")) {
                    return;
                }
                Task.sync(onReloadAll);
            }
        };
        activeSubscribers.add(updateSub);
        activeSubscribers.add(deleteSub);
        activeSubscribers.add(reloadSub);
        redisManager.subscribe(channelUpdate, updateSub);
        redisManager.subscribe(channelDelete, deleteSub);
        redisManager.subscribe(channelReload, reloadSub);
    }

    // ─── Polling ───────────────────────────────────────────────────

    private void startPolling(long intervalSec) {
        long intervalTicks = intervalSec * 20L;
        this.pollingTask = Task.syncTimer(this::pollForChanges, intervalTicks, intervalTicks);
    }

    private void pollForChanges() {
        long pollTime = lastPollTimestamp;
        lastPollTimestamp = System.currentTimeMillis();

        repository.getModifiedSince(pollTime).thenAccept(modifiedIds -> {
            if (modifiedIds == null || modifiedIds.isEmpty()) {
                return;
            }
            MidgardLogger.debug(DebugCategory.ITEMS,
                "[%s Sync] Polling detectou %d modificados.", moduleName, modifiedIds.size());
            Task.sync(() -> {
                for (String id : modifiedIds) {
                    onUpdate.accept(id);
                }
            });
        });

        if (onDeleteBatch != null) {
            repository.getAllIds().thenAccept(dbIds -> {
                if (dbIds == null) {
                    return;
                }
                Task.sync(() -> onDeleteBatch.accept(new ArrayList<>(dbIds)));
            });
        }
    }

    // ─── Util ──────────────────────────────────────────────────────

    private String extractPayload(String message) {
        int idx = message.indexOf(':');
        return idx >= 0 ? message.substring(idx + 1) : message;
    }

    private String generateServerId() {
        int port = org.bukkit.Bukkit.getServer().getPort();
        String hex = String.format("%016x", System.nanoTime());
        return "srv-" + port + "-" + hex.substring(hex.length() - 6);
    }
}
