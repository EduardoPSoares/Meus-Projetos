package me.ray.midgard.modules.essentials;

import me.ray.midgard.core.ModulePriority;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.essentials.command.*;
import me.ray.midgard.modules.essentials.listener.TeleportListener;
import me.ray.midgard.modules.essentials.listener.VanishListener;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EssentialsModule extends RPGModule {

    private EssentialsManager manager;
    private me.ray.midgard.core.sync.DefinitionSyncManager warpSyncManager;
    private me.ray.midgard.core.sync.DefinitionSyncManager spawnSyncManager;
    private me.ray.midgard.core.sync.DefinitionSyncManager homeSyncManager;

    public EssentialsModule() {
        super("MidgardEssentials", ModulePriority.NORMAL);
    }

    @Override
    public void onEnable() {
        try {
            this.manager = new EssentialsManager(plugin);
            
            registerCommands(plugin);
            registerListeners(plugin);
            
            // Start sync for warps, spawn, and homes
            me.ray.midgard.core.redis.RedisManager redisManager = me.ray.midgard.core.MidgardCore.getRedisManager();
            
            // Warp sync
            if (manager.getWarpManager() != null && manager.getWarpManager().getRepository() != null) {
                me.ray.midgard.core.database.DefinitionRepository warpRepo = manager.getWarpManager().getRepository();
                this.warpSyncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                    "warps", warpRepo, redisManager, 30,
                    id -> warpRepo.load(id).thenAccept(data -> {
                        if (data != null) me.ray.midgard.core.utils.Task.sync(() -> manager.getWarpManager().reloadWarpFromDb(id, data));
                    }),
                    id -> me.ray.midgard.core.utils.Task.sync(() -> manager.getWarpManager().unregisterWarp(id)),
                    () -> manager.getWarpManager().loadWarps(),
                    dbIds -> {
                        java.util.Set<String> dbSet = new java.util.HashSet<>(dbIds);
                        for (String loadedId : new java.util.ArrayList<>(manager.getWarpManager().getWarps())) {
                            if (!dbSet.contains(loadedId)) manager.getWarpManager().unregisterWarp(loadedId);
                        }
                    }
                );
                // Wire Redis notifications for write operations
                manager.getWarpManager().setSyncNotifiers(
                    id -> warpSyncManager.notifyUpdate(id),
                    id -> warpSyncManager.notifyDelete(id)
                );
            }
            
            // Spawn sync
            if (manager.getSpawnManager() != null && manager.getSpawnManager().getRepository() != null) {
                me.ray.midgard.core.database.DefinitionRepository spawnRepo = manager.getSpawnManager().getRepository();
                this.spawnSyncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                    "spawn", spawnRepo, redisManager, 30,
                    id -> spawnRepo.load(id).thenAccept(data -> {
                        if (data != null) me.ray.midgard.core.utils.Task.sync(() -> manager.getSpawnManager().reloadSpawnFromDb(data));
                    }),
                    id -> {},
                    () -> manager.getSpawnManager().loadSpawn(),
                    null
                );
                // Wire Redis notification for write operations
                manager.getSpawnManager().setSyncNotifier(() -> spawnSyncManager.notifyUpdate("spawn"));
            }
            
            // Home sync
            if (manager.getHomeManager() != null && manager.getHomeManager().getRepository() != null) {
                me.ray.midgard.core.database.DefinitionRepository homeRepo = manager.getHomeManager().getRepository();
                this.homeSyncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                    "homes", homeRepo, redisManager, 60,
                    id -> {},  // Homes reload individually is complex; just reload all
                    id -> {},
                    () -> manager.getHomeManager().loadHomes(),
                    null
                );
                // Wire Redis notifications for write operations
                manager.getHomeManager().setSyncNotifiers(
                    id -> homeSyncManager.notifyUpdate(id),
                    id -> homeSyncManager.notifyDelete(id)
                );
            }
            
        } catch (Exception e) {
            MidgardLogger.error("Erro fatal ao habilitar MidgardEssentials", e);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        try {
            if (manager != null) {
                if (manager.getConfig() != null) {
                     manager.getConfig().reload();
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao recarregar configurações do Essentials", e);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (warpSyncManager != null) {
                try { warpSyncManager.shutdown(); } catch (Exception ignored) { /* Shutdown cleanup */ }
            }
            if (spawnSyncManager != null) {
                try { spawnSyncManager.shutdown(); } catch (Exception ignored) { /* Shutdown cleanup */ }
            }
            if (homeSyncManager != null) {
                try { homeSyncManager.shutdown(); } catch (Exception ignored) { /* Shutdown cleanup */ }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao desabilitar MidgardEssentials", e);
        }
    }
    
    private void registerCommands(JavaPlugin plugin) {
        try {
            if (plugin == null) {
                return;
            }
            
            // Todos os comandos do essentials são standalone (ex: /gm, /fly, /heal)
            me.ray.midgard.core.command.MidgardCommand[] commands = {
                // Comandos de jogador
                new SpawnCommand(manager),
                new WarpCommand(manager),
                new HomeCommand(manager),
                new SetHomeCommand(manager),
                new DelHomeCommand(manager),
                new TpaCommand(manager),
                new TpacceptCommand(manager),
                new TpdenyCommand(manager),
                new BackCommand(manager),
                // Comandos administrativos
                new GamemodeCommand(manager),
                new FlyCommand(manager),
                new HealCommand(manager),
                new FeedCommand(manager),
                new SetSpawnCommand(manager),
                new SetWarpCommand(manager),
                new DelWarpCommand(manager),
                new VanishCommand(manager),
                new TeleportCommand(manager),
                new TeleportHereCommand(manager),
                new TopCommand(manager),
                new SpeedCommand(manager),
                new InvseeCommand(manager),
            };
            
            for (me.ray.midgard.core.command.MidgardCommand cmd : commands) {
                me.ray.midgard.core.command.CommandRegistrar.register(plugin, cmd);
            }
        } catch (Exception e) {
            if (plugin != null) {
                MidgardLogger.error("Erro ao registrar comandos do Essentials", e);
            }
        }
    }

    private void registerListeners(JavaPlugin plugin) {
        try {
            if (plugin == null) {
                return;
            }
            plugin.getServer().getPluginManager().registerEvents(new VanishListener(manager), plugin);
            plugin.getServer().getPluginManager().registerEvents(new TeleportListener(manager), plugin);
            plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.essentials.listener.CommandBlockerListener(manager), plugin);
        } catch (Exception e) {
             if (plugin != null) {
                 MidgardLogger.error("Erro ao registrar listeners do Essentials", e);
             }
        }
    }
}
