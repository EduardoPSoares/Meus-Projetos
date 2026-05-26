package me.ray.midgard.core.profile;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.redis.RedisManager;
import me.ray.midgard.core.profile.data.VanillaData; // Import added
import me.ray.midgard.core.utils.Task;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia os perfis dos jogadores (carregamento, salvamento e cache).
 */
public class ProfileManager implements Listener {

    private final Map<UUID, MidgardProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, java.util.concurrent.CompletableFuture<Void>> pendingSaves = new ConcurrentHashMap<>();
    private final ProfileRepository repository;
    private final RedisManager redisManager;

    /**
     * Construtor do ProfileManager.
     *
     * @param plugin Instância do plugin.
     * @param databaseManager Gerenciador de banco de dados.
     * @param redisManager Gerenciador do Redis (pode ser null).
     */
    public ProfileManager(JavaPlugin plugin, DatabaseManager databaseManager, RedisManager redisManager) {
        this.repository = new ProfileRepository(databaseManager);
        this.redisManager = redisManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadOnlinePlayers();
        startAutoSave();
    }

    private void startAutoSave() {
        // Auto-Save a cada 5 minutos (6000 ticks)
        // Usa global scheduler para iniciar, mas delega para entity scheduler em Folia
        Task.syncTimer(() -> {
            MidgardLogger.debug(DebugCategory.CORE, "Executando Auto-Save periódico...");
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Dispatch to entity scheduler for Folia safety
                Task.sync(player, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    MidgardProfile profile = getProfile(player.getUniqueId());
                    if (profile != null) {
                        try {
                            // Captura dados vanilla (Region Thread)
                            VanillaData vanillaData = VanillaData.fromPlayer(player);
                            profile.setData(vanillaData);
                            
                            // Salva (Async)
                            saveProfile(profile);
                        } catch (Exception e) {
                            MidgardLogger.error("Erro no Auto-Save de " + player.getName(), e);
                        }
                    }
                });
            }
        }, 6000L, 6000L);
    }

    private void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                MidgardProfile profile = repository.loadProfile(player.getUniqueId(), player.getName()).join();
                if (profile != null) {
                    profiles.put(player.getUniqueId(), profile);
                    MidgardLogger.debug(DebugCategory.CORE, "Perfil carregado para %s (recuperação de reload)", player.getName());
                } else {
                    MidgardLogger.warn("Perfil retornou null para %s durante reload - criando novo", player.getName());
                    profiles.put(player.getUniqueId(), new MidgardProfile(player.getUniqueId(), player.getName()));
                }
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar perfil de " + player.getName() + " durante reload", e);
                // Create emergency profile to prevent null errors
                profiles.put(player.getUniqueId(), new MidgardProfile(player.getUniqueId(), player.getName()));
            }
        }
    }

    /**
     * Obtém o perfil de um jogador pelo UUID.
     *
     * @param uuid UUID do jogador.
     * @return Perfil do jogador ou null se não carregado.
     */
    public MidgardProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }
    
    /**
     * Obtém o perfil de um jogador.
     *
     * @param player Jogador.
     * @return Perfil do jogador ou null se não carregado.
     */
    public MidgardProfile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        // Redis Locking Check
        if (redisManager != null && redisManager.isEnabled()) {
            String lockKey = "lock:profile:" + event.getUniqueId();
            int retries = 0;
            boolean locked = true;
            
            while (retries < 10) {
                java.util.function.Function<redis.clients.jedis.Jedis, Boolean> checkLock = j -> j.exists(lockKey);
                Boolean exists = redisManager.execute(checkLock);
                if (exists == null || !exists) {
                    locked = false;
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                retries++;
            }
            
            if (locked) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, net.kyori.adventure.text.Component.text("Sessão anterior ainda está sendo salva. Tente novamente em alguns segundos."));
                return;
            }
        }

        // Load profile from DB
        try {
            MidgardProfile profile = me.ray.midgard.core.debug.MidgardProfiler.monitor("profile_load_async",
                () -> repository.loadProfile(event.getUniqueId(), event.getName()).orTimeout(15, java.util.concurrent.TimeUnit.SECONDS).join()
            );

            if (profile != null) {
                profiles.put(event.getUniqueId(), profile);
                MidgardLogger.debug(DebugCategory.CORE, "Perfil carregado assincronamente para %s (UUID: %s)", event.getName(), event.getUniqueId());
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao carregar perfil para " + event.getName(), e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            // Profile is already loaded in AsyncPreLogin
            MidgardProfile profile = profiles.get(event.getPlayer().getUniqueId());
            if (profile == null) {
                // Fallback if something went wrong
                event.getPlayer().kick(net.kyori.adventure.text.Component.text("Falha ao carregar perfil. Por favor, reconecte-se."));
                return;
            }

            // Apply Vanilla Data (Inventory, Health, etc.)
            if (profile.hasData(VanillaData.class)) {
                VanillaData vanillaData = profile.getData(VanillaData.class);
                vanillaData.applyTo(event.getPlayer());
                MidgardLogger.debug(DebugCategory.CORE, "Dados vanilla restaurados para %s", event.getPlayer().getName());
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar join do jogador " + event.getPlayer().getName(), e);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            UUID uuid = event.getPlayer().getUniqueId();
            MidgardProfile profile = profiles.get(uuid);
            
            // Cleanup CooldownManager to prevent memory leak
            me.ray.midgard.core.utils.CooldownManager cooldownManager = MidgardCore.getCooldownManager();
            if (cooldownManager != null) {
                cooldownManager.cleanupPlayer(uuid);
            }

            // Cleanup ScoreboardManager
            me.ray.midgard.core.scoreboard.ScoreboardManager scoreboardManager = MidgardCore.getScoreboardManager();
            if (scoreboardManager != null) {
                scoreboardManager.cleanup(uuid);
            }
            
            if (profile != null) {
                MidgardLogger.debug(DebugCategory.CORE, "Salvando perfil de %s ao sair...", event.getPlayer().getName());
                
                // Capture Vanilla Data
                try {
                    VanillaData vanillaData = VanillaData.fromPlayer(event.getPlayer());
                    profile.setData(vanillaData);
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao capturar dados vanilla de " + event.getPlayer().getName(), e);
                }

                String lockKey = "lock:profile:" + event.getPlayer().getUniqueId();
                if (redisManager != null && redisManager.isEnabled()) {
                    redisManager.execute(jedis -> { jedis.setex(lockKey, 10, "locked"); });
                }
                
                saveProfile(profile).whenComplete((result, ex) -> {
                    if (ex != null) {
                        MidgardLogger.error("Falha ao salvar perfil de " + profile.getName() + " no logout", ex);
                    }
                    profiles.remove(uuid);
                    if (redisManager != null && redisManager.isEnabled()) {
                        redisManager.execute(jedis -> {
                            jedis.del(lockKey);
                            jedis.publish("sync:saved:" + profile.getUuid(), "saved");
                        });
                    }
                });
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar quit do jogador " + event.getPlayer().getName(), e);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommandPreprocess(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        try {
            String msg = event.getMessage().toLowerCase();
            String[] parts = msg.split("\\s+", 2);
            String cmd = parts[0];
            if (cmd.equals("/server") || cmd.equals("/lobby") || cmd.equals("/hub")) {
                 MidgardProfile profile = getProfile(event.getPlayer());
                 if (profile != null) {
                     MidgardLogger.debug(DebugCategory.CORE, "Detectado comando de troca de servidor (%s). Forçando salvamento prévio...", msg);
                     try {
                         VanillaData vanillaData = VanillaData.fromPlayer(event.getPlayer());
                         profile.setData(vanillaData);
                         saveProfile(profile); 
                     } catch (Exception e) {
                         MidgardLogger.error("Erro ao salvar perfil antes da troca de servidor", e);
                     }
                 }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar comando de jogador", e);
        }
    }
    
    /**
     * Salva um perfil no banco de dados.
     *
     * @param profile Perfil a ser salvo.
     * @return Future que completa quando o salvamento termina.
     */
    public java.util.concurrent.CompletableFuture<Void> saveProfile(MidgardProfile profile) {
        java.util.concurrent.CompletableFuture<Void> future = repository.saveProfile(profile);
        pendingSaves.put(profile.getUuid(), future);
        future.thenRun(() -> pendingSaves.remove(profile.getUuid()));
        return future;
    }
    
    /**
     * Encerra o gerenciador e salva todos os perfis.
     * Garante que TODOS os perfis sejam salvos antes de encerrar.
     */
    public void shutdown() {
        // Collect all save futures BEFORE clearing profiles
        java.util.List<java.util.concurrent.CompletableFuture<Void>> shutdownSaves = new java.util.ArrayList<>();
        
        for (Map.Entry<UUID, MidgardProfile> entry : profiles.entrySet()) {
            MidgardProfile profile = entry.getValue();
            MidgardLogger.debug(DebugCategory.CORE, "Salvando perfil remanescente no shutdown: %s", profile.getName());
            
            // Capture vanilla data for online players
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                try {
                    VanillaData vanillaData = VanillaData.fromPlayer(player);
                    profile.setData(vanillaData);
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao capturar dados vanilla no shutdown para " + profile.getName(), e);
                }
            }
            
            shutdownSaves.add(saveProfile(profile));
        }
        
        // Also include any previously pending saves
        shutdownSaves.addAll(pendingSaves.values());
        
        // Wait for ALL saves to complete
        if (!shutdownSaves.isEmpty()) {
            MidgardLogger.info("Aguardando " + shutdownSaves.size() + " salvamentos no shutdown...");
            try {
                java.util.concurrent.CompletableFuture.allOf(
                    shutdownSaves.toArray(new java.util.concurrent.CompletableFuture[0])
                ).get(30, java.util.concurrent.TimeUnit.SECONDS);
                MidgardLogger.info("Todos os perfis salvos com sucesso no shutdown.");
            } catch (java.util.concurrent.TimeoutException e) {
                MidgardLogger.error("Timeout ao aguardar salvamento de perfis no shutdown! Dados podem ter sido perdidos.");
            } catch (Exception e) {
                MidgardLogger.error("Erro ao aguardar salvamento de perfis no shutdown!", e);
            }
        }
        
        profiles.clear();
        pendingSaves.clear();
    }
}
