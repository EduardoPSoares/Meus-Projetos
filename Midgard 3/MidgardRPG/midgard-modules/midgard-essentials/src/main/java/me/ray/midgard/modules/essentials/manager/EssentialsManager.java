package me.ray.midgard.modules.essentials.manager;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.essentials.config.EssentialsConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class EssentialsManager {

    private final EssentialsConfig config;
    private final WarpManager warpManager;
    private final SpawnManager spawnManager;
    private final HomeManager homeManager;
    private final TeleportRequestManager teleportRequestManager;
    private final VanishManager vanishManager;
    private final TeleportHistoryManager teleportHistoryManager;

    public EssentialsManager(JavaPlugin plugin) {
        // Inicializa a config primeiro
        try {
            this.config = new EssentialsConfig(plugin);
        } catch (Exception e) {
            MidgardLogger.error("Erro crítico ao carregar configurações do Essentials", e);
            throw new RuntimeException("Falha na inicialização do EssentialsManager", e);
        }

        // Inicializa os managers
        this.warpManager = safeInit(() -> new WarpManager(plugin, config), "WarpManager", plugin);
        this.spawnManager = safeInit(() -> new SpawnManager(plugin, config), "SpawnManager", plugin);
        this.homeManager = safeInit(() -> new HomeManager(plugin, config), "HomeManager", plugin);
        this.teleportHistoryManager = safeInit(() -> new TeleportHistoryManager(), "TeleportHistoryManager", plugin);

        this.teleportRequestManager = safeInit(() -> new TeleportRequestManager(plugin, config, this), "TeleportRequestManager", plugin);
        this.vanishManager = safeInit(() -> new VanishManager(plugin, config, this), "VanishManager", plugin);
    }

    private <T> T safeInit(java.util.function.Supplier<T> supplier, String name, JavaPlugin plugin) {
        try {
            return supplier.get();
        } catch (Exception e) {
            MidgardLogger.error("Erro ao inicializar " + name, e);
            return null;
        }
    }

    public EssentialsConfig getConfig() {
        return config;
    }

    public WarpManager getWarpManager() {
        return warpManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }

    public TeleportRequestManager getTeleportRequestManager() {
        return teleportRequestManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public TeleportHistoryManager getTeleportHistoryManager() {
        return teleportHistoryManager;
    }

    public String getMessage(String path) {
        return MidgardCore.getLanguageManager().getRawMessage("essentials." + path);
    }

    public List<String> getMessageList(String path) {
        return MidgardCore.getLanguageManager().getStringList("essentials." + path);
    }
}
