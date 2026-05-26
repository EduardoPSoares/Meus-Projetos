package me.ray.midgard.core;

import me.ray.midgard.core.command.AdminCommandRegistry;
import me.ray.midgard.core.command.UnifiedCommandManager;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.debug.MidgardProfiler;
import me.ray.midgard.core.economy.EconomyProvider;
import me.ray.midgard.core.gui.InventoryProtectionManager;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.integration.VaultIntegration;
import me.ray.midgard.core.integration.WorldGuardIntegration;
import me.ray.midgard.core.placeholder.PlaceholderRegistry;
import me.ray.midgard.core.profile.ProfileManager;
import me.ray.midgard.core.region.RegionManager;
import me.ray.midgard.core.utils.ConsoleUtils;
import me.ray.midgard.core.utils.CooldownManager;
import me.ray.midgard.nms.api.NMSHandler;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classe principal do núcleo do MidgardRPG.
 * Gerencia a inicialização e acesso aos principais gerenciadores.
 */
public class MidgardCore {
    
    private static volatile JavaPlugin pluginInstance;
    private static volatile EconomyProvider economyProvider;
    private static volatile ProfileManager profileManager;
    private static volatile LanguageManager languageManager;
    private static volatile UnifiedCommandManager commandManager;
    private static volatile AdminCommandRegistry adminCommandRegistry;
    private static volatile me.ray.midgard.core.command.HelpSystem helpSystem;
    private static volatile me.ray.midgard.core.loot.LootManager lootManager;
    private static volatile InventoryProtectionManager inventoryProtectionManager;
    private static volatile PlaceholderRegistry placeholderRegistry;
    private static volatile NMSHandler nmsHandler;
    private static volatile ModuleManager moduleManager;
    private static volatile me.ray.midgard.core.skill.SkillProvider skillProvider;
    private static volatile me.ray.midgard.core.leaderboard.LeaderboardManager leaderboardManager;
    private static volatile CooldownManager cooldownManager;
    private static volatile me.ray.midgard.core.database.DatabaseManager databaseManager;
    private static volatile me.ray.midgard.core.redis.RedisManager redisManager;
    private static volatile me.ray.midgard.core.scoreboard.ScoreboardManager scoreboardManager;
    
    private static volatile boolean loaded = false;

    /**
     * Inicializa o núcleo do plugin.
     *
     * @param plugin Instância do plugin principal.
     * @param pm Gerenciador de perfis.
     * @param lm Gerenciador de idiomas.
     */
    public static void init(JavaPlugin plugin, ProfileManager pm, LanguageManager lm) {
        if (loaded) {
            plugin.getLogger().warning("Tentativa de inicializar MidgardCore duas vezes!");
            return;
        }
        
        try {
            pluginInstance = plugin;
            profileManager = pm;
            languageManager = lm;
            if (commandManager == null) {
                commandManager = new UnifiedCommandManager();
            }
            lootManager = new me.ray.midgard.core.loot.LootManager(plugin);
            inventoryProtectionManager = new InventoryProtectionManager(plugin);
            plugin.getServer().getPluginManager().registerEvents(inventoryProtectionManager, plugin);
            
            // Initialize Profiler
            MidgardProfiler.init();
            
            // Initialize integrations
            economyProvider = new VaultIntegration();
            
            // Initialize Region Provider
            if (plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
                try {
                    RegionManager.getInstance().registerProvider(new WorldGuardIntegration());
                    ConsoleUtils.log("    <gray>WorldGuard:<white>Enabled</white>");
                } catch (Throwable e) {
                    ConsoleUtils.log("    <gray>WorldGuard:<red>Failed</red>");
                    MidgardLogger.error("Falha ao registrar WorldGuardIntegration", e);
                }
            }
            
            if (plugin.getServer().getPluginManager().isPluginEnabled("Lands")) {
                try {
                    RegionManager.getInstance().registerProvider(new me.ray.midgard.core.integration.MidgardLandsIntegration());
                    ConsoleUtils.log("    <gray>Lands:<white>Enabled</white>");
                } catch (Throwable e) {
                    ConsoleUtils.log("    <gray>Lands:<red>Failed</red>");
                    MidgardLogger.error("Falha ao registrar MidgardLandsIntegration", e);
                }
            }
            loaded = true;
        } catch (Exception e) {
             ConsoleUtils.logStatus("Core Init", false, "Critical Error");
             MidgardLogger.error("Erro crítico na inicialização do MidgardCore", e);
             loaded = false;
        }
    }
    
    public static boolean isLoaded() {
        return loaded;
    }

    public static JavaPlugin getInstance() {
        return pluginInstance;
   }

    public static void setPlaceholderRegistry(PlaceholderRegistry registry) {
        placeholderRegistry = registry;
    }
    
    public static PlaceholderRegistry getPlaceholderRegistry() {
        return placeholderRegistry;
    }

    public static JavaPlugin getPlugin() {
        return pluginInstance;
    }

    public static void setNMSHandler(NMSHandler handler) {
        nmsHandler = handler;
    }
    
    public static void setModuleManager(ModuleManager manager) {
        moduleManager = manager;
    }
    
    public static ModuleManager getModuleManager() {
        return moduleManager;
    }
    
    public static void setSkillProvider(me.ray.midgard.core.skill.SkillProvider provider) {
        skillProvider = provider;
    }
    
    public static me.ray.midgard.core.skill.SkillProvider getSkillProvider() {
        return skillProvider;
    }
    
    public static void setLeaderboardManager(me.ray.midgard.core.leaderboard.LeaderboardManager manager) {
        leaderboardManager = manager;
    }
    
    public static me.ray.midgard.core.leaderboard.LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public static void setCooldownManager(CooldownManager manager) {
        cooldownManager = manager;
    }

    public static CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public static void setDatabaseManager(me.ray.midgard.core.database.DatabaseManager manager) {
        databaseManager = manager;
    }

    public static me.ray.midgard.core.database.DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static void setRedisManager(me.ray.midgard.core.redis.RedisManager manager) {
        redisManager = manager;
    }

    public static me.ray.midgard.core.redis.RedisManager getRedisManager() {
        return redisManager;
    }

    public static void setScoreboardManager(me.ray.midgard.core.scoreboard.ScoreboardManager manager) {
        scoreboardManager = manager;
    }

    public static me.ray.midgard.core.scoreboard.ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    /**
     * Define o registro de comandos administrativos.
     * 
     * @param registry Registro de comandos admin
     */
    public static void setAdminCommand(AdminCommandRegistry registry) {
        adminCommandRegistry = registry;
    }
    
    /**
     * Define o gerenciador de comandos unificado.
     * 
     * @param manager Gerenciador de comandos
     */
    public static void setCommandManager(me.ray.midgard.core.command.UnifiedCommandManager manager) {
        commandManager = manager;
    }
    
    /**
     * Obtém o registro de comandos administrativos.
     * Permite que módulos registrem subcomandos de admin.
     * 
     * @return Registro de comandos admin
     */
    public static AdminCommandRegistry getAdminCommand() {
        return adminCommandRegistry;
    }


    public static InventoryProtectionManager getInventoryProtectionManager() {
        return inventoryProtectionManager;
    }
    public static NMSHandler getNMSHandler() {
        return nmsHandler;
    }
    
    /**
     * Define o provedor de economia.
     *
     * @param provider Novo provedor de economia.
     */
    public static void setEconomyProvider(EconomyProvider provider) {
        economyProvider = provider;
    }
    
    /**
     * Obtém o provedor de economia.
     *
     * @return Provedor de economia.
     */
    public static EconomyProvider getEconomyProvider() {
        return economyProvider;
    }
    
    /**
     * Obtém o gerenciador de perfis.
     *
     * @return Gerenciador de perfis.
     */
    public static ProfileManager getProfileManager() {
        return profileManager;
    }
    
    /**
     * Obtém o gerenciador de idiomas.
     *
     * @return Gerenciador de idiomas.
     */
    public static LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    /**
     * Obtém o gerenciador de comandos.
     *
     * @return Gerenciador de comandos.
     */
    public static UnifiedCommandManager getCommandManager() {
        return commandManager;
    }
    
    /**
     * Obtém o sistema de ajuda.
     *
     * @return Sistema de ajuda.
     */
    public static me.ray.midgard.core.command.HelpSystem getHelpSystem() {
        me.ray.midgard.core.command.HelpSystem hs = helpSystem;
        if (hs == null) {
            synchronized (MidgardCore.class) {
                hs = helpSystem;
                if (hs == null) {
                    hs = new me.ray.midgard.core.command.HelpSystem();
                    helpSystem = hs;
                }
            }
        }
        return hs;
    }

    public static me.ray.midgard.core.loot.LootManager getLootManager() {
        return lootManager;
    }

    /**
     * Encerra o núcleo e libera recursos.
     */
    public static void shutdown() {
        try {
            cleanupRegistries();
            if (economyProvider != null && economyProvider instanceof VaultIntegration) {
                // Cleanup if needed
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro durante limpeza do shutdown do core", e);
        }
        
        pluginInstance = null;
        economyProvider = null;
        profileManager = null;
        languageManager = null;
        commandManager = null;
        adminCommandRegistry = null;
        helpSystem = null;
        lootManager = null;
        inventoryProtectionManager = null;
        placeholderRegistry = null;
        nmsHandler = null;
        moduleManager = null;
        skillProvider = null;
        leaderboardManager = null;
        cooldownManager = null;
        databaseManager = null;
        redisManager = null;
        if (scoreboardManager != null) {
            scoreboardManager.shutdown();
            scoreboardManager = null;
        }
        
        loaded = false;
    }

    private static void cleanupRegistries() {
        try {
            me.ray.midgard.core.attribute.AttributeRegistry.getInstance().clear();
            me.ray.midgard.core.skill.SkillRegistry.getInstance().clear();
            me.ray.midgard.core.effect.EffectRegistry.getInstance().clear();
            me.ray.midgard.core.i18n.MessageRegistry.getInstance().clear();
        } catch (Exception e) {
             MidgardLogger.error("Erro ao limpar registries durante shutdown", e);
        }
    }
}
