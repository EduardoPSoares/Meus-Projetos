package me.ray.midgard.loader;

import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.redis.RedisManager;
import me.ray.midgard.core.leaderboard.LeaderboardManager;
import me.ray.midgard.core.placeholder.PlaceholderRegistry;
import me.ray.midgard.core.profile.ProfileManager;
import me.ray.midgard.core.utils.CooldownManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classe principal do plugin MidgardRPG.
 * Responsável por inicializar todos os sistemas via MidgardBootstrap.
 */
public class MidgardPlugin extends JavaPlugin {

    private static MidgardPlugin instance;
    private MidgardBootstrap bootstrap;

    @Override
    public void onEnable() {
        instance = this;
        this.bootstrap = new MidgardBootstrap(this);
        this.bootstrap.initialize();
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.shutdown();
        }
        instance = null;
    }

    public static MidgardPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return bootstrap.getDatabaseManager();
    }

    public RedisManager getRedisManager() {
        return bootstrap.getRedisManager();
    }

    public LanguageManager getLanguageManager() {
        return bootstrap.getLanguageManager();
    }

    public CooldownManager getCooldownManager() {
        return bootstrap.getCooldownManager();
    }

    public ProfileManager getProfileManager() {
        return bootstrap.getProfileManager();
    }
    
    public LeaderboardManager getLeaderboardManager() {
        return bootstrap.getLeaderboardManager();
    }
    
    public PlaceholderRegistry getPlaceholderRegistry() {
        return bootstrap.getPlaceholderRegistry();
    }
    
    public me.ray.midgard.core.ModuleManager getModuleManager() {
        return bootstrap.getModuleManager();
    }
}
