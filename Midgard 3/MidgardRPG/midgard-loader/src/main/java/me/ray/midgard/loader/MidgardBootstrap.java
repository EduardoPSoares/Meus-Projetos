package me.ray.midgard.loader;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.ModuleManager;
import me.ray.midgard.core.attribute.Attribute;
import me.ray.midgard.core.attribute.AttributeRegistry;
import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.database.DatabaseCredentials;
import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.gui.GuiListener;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.leaderboard.LeaderboardManager;
import me.ray.midgard.core.placeholder.PlaceholderRegistry;
import me.ray.midgard.core.profile.ProfileManager;
import me.ray.midgard.core.redis.RedisCredentials;
import me.ray.midgard.core.redis.RedisManager;
import me.ray.midgard.core.utils.CooldownManager;
import me.ray.midgard.core.utils.ConsoleUtils;
import me.ray.midgard.core.utils.PDCUtils;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.loader.command.AdminCommand;
import me.ray.midgard.loader.listener.ItemMechanicsListener;
import me.ray.midgard.loader.listener.MobDebugListener;
import me.ray.midgard.loader.listener.StatUpdateListener;
import me.ray.midgard.modules.character.CharacterModule;
import me.ray.midgard.modules.classes.ClassesModule;
import me.ray.midgard.modules.combat.CombatModule;
import me.ray.midgard.modules.commands.CommandsModule;
import me.ray.midgard.modules.economy.EconomyModule;
import me.ray.midgard.modules.essentials.EssentialsModule;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.performance.PerformanceModule;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.spells.SpellsModule;
import org.bukkit.configuration.ConfigurationSection;

public class MidgardBootstrap {

    private final MidgardPlugin plugin;
    
    // Config
    private ConfigWrapper mainConfig;

    // Managers
    private DatabaseManager databaseManager;
    private RedisManager redisManager;
    private LanguageManager languageManager;
    private CooldownManager cooldownManager;
    private ProfileManager profileManager;
    private ModuleManager moduleManager;
    private LeaderboardManager leaderboardManager;
    private PlaceholderRegistry placeholderRegistry;

    public MidgardBootstrap(MidgardPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        ConsoleUtils.printHeader();
        
        // 1. Ambiente
        ConsoleUtils.logSection("Environment Info");
        ConsoleUtils.printEnvironmentInfo();

        // 2. Core Systems
        ConsoleUtils.logSection("Core Systems");
        long startTime = System.currentTimeMillis();
        
        // Utils
        initUtils();
        
        // Configurações
        this.mainConfig = new ConfigWrapper(plugin, "config.yml");
        ConsoleUtils.logStatus("Configuration", true, "config.yml");
        
        // Debug
        boolean debug = this.mainConfig.getConfig().getBoolean("settings.debug", false);
        MidgardLogger.setDebugEnabled(debug);
        // ConsoleUtils.logStatus("Debug Mode", debug, debug ? "Enabled" : "Disabled");
        
        // Resources
        new ResourceInstaller(plugin).install();
        ConsoleUtils.logStatus("Resource Installer", true, "Assets & Configs");
        
        // Attributes
        loadAttributes();
        
        ConsoleUtils.log(""); // Space before Database

        // 3. Persistência (DB & Redis)
        initPersistence(this.mainConfig);
        
        ConsoleUtils.log(""); // Space after Database

        ConsoleUtils.log("  <gradient:#00ffff:#0099ff>● Core Systems</gradient>");

        // 4. Idioma
        initLanguage();
        ConsoleUtils.log("    <gray>Language:<white>Loaded</white>");

        // 5. Managers Principais
        initCoreManagers();
        ConsoleUtils.log("    <gray>Managers:<white>Profile, Leaderboard</white>");

        // 6. Integrações
        initIntegrations();

        // 7. Configuração do Core Estático
        initStaticCore();

        // 8. Comandos Base
        registerBaseCommands();
        ConsoleUtils.log("    <gray>Commands:<white>Unified System</white>");

        // 9. Módulos
        // ConsoleUtils.logSection("Modules");
        initModules();
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Final Summary
        ConsoleUtils.logSection("Startup Summary");
        ConsoleUtils.logItem("Total Time", totalTime + "ms");
        ConsoleUtils.logItem("Status", "<green>READY TO PLAY</green>");
        ConsoleUtils.log("");
    }

    public void shutdown() {
        // Save profiles BEFORE disabling modules so modules can still access profile data
        if (profileManager != null) profileManager.shutdown();
        if (moduleManager != null) moduleManager.disableAll();
        if (redisManager != null) redisManager.shutdown();
        if (databaseManager != null) databaseManager.shutdown();
        MidgardCore.shutdown();
    }

    private void initUtils() {
        Task.init(plugin);
        PDCUtils.init(plugin);
        this.cooldownManager = new CooldownManager();
        MidgardCore.setCooldownManager(this.cooldownManager);

        // ScoreboardManager global
        me.ray.midgard.core.scoreboard.ScoreboardManager sbManager = me.ray.midgard.core.scoreboard.ScoreboardManager.getInstance();
        sbManager.start();
        MidgardCore.setScoreboardManager(sbManager);
    }

    private void loadAttributes() {
        ConfigWrapper attributesConfig = new ConfigWrapper(plugin, "settings/attributes.yml");
        ConfigurationSection section = attributesConfig.getConfig().getConfigurationSection("attributes");
        
        if (section != null) {
            int count = 0;
            for (String key : section.getKeys(false)) {
                ConfigurationSection attrSection = section.getConfigurationSection(key);
                if (attrSection == null) continue;
                
                String name = attrSection.getString("name", key);
                String icon = attrSection.getString("icon", "");
                String format = attrSection.getString("format", "0.0");
                double base = attrSection.getDouble("base", 0.0);
                double min = attrSection.getDouble("min", 0.0);
                double max = attrSection.getDouble("max", 100000.0);
                
                Attribute attribute = new Attribute(key, name, base, min, max, icon, format);
                AttributeRegistry.getInstance().register(key, attribute);
                count++;
            }
            ConsoleUtils.logStatus("Attribute Registry", true, count + " attributes loaded");
        }
    }

    private void initPersistence(ConfigWrapper config) {
        // Database
        try {
            this.databaseManager = new DatabaseManager(plugin);
            DatabaseCredentials credentials = new DatabaseCredentials(
                config.getConfig().getString("database.type", "sqlite"),
                config.getConfig().getString("database.host", "localhost"),
                config.getConfig().getInt("database.port", 3306),
                config.getConfig().getString("database.database", "midgard"),
                config.getConfig().getString("database.username", "root"),
                config.getConfig().getString("database.password", "password"),
                config.getConfig().getBoolean("database.use-ssl", false)
            );
            this.databaseManager.initialize(credentials);
            
            // ConsoleUtils.logStatus("Database", true, config.getConfig().getString("database.type", "sqlite"));
            String dbType = config.getConfig().getString("database.type", "sqlite").toUpperCase();
            ConsoleUtils.log("  <gradient:#ff9900:#ff5500>● Database Connection</gradient>");
            ConsoleUtils.log("    <gray>Type:<white>" + dbType + "</white>");
            if (dbType.equalsIgnoreCase("MYSQL")) {
                ConsoleUtils.log("    <gray>Host:<white>" + credentials.host() + ":" + credentials.port() + "</white>");
            }
            ConsoleUtils.log("    <gray>Status:<green>Connected ✔</green>");
            MidgardCore.setDatabaseManager(this.databaseManager);
            
        } catch (Exception e) {
            ConsoleUtils.log("  <gradient:#ff9900:#ff5500>● Database Connection</gradient>");
            ConsoleUtils.log("    <gray>Status:<red>Failed ✖</red>");
            ConsoleUtils.log("    <gray>Error:<red>" + e.getMessage() + "</red>");
            
            MidgardLogger.error("Erro crítico ao inicializar banco de dados. O plugin será desativado.", e);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new RuntimeException("Falha ao inicializar banco de dados", e);
        }

        // Redis (Opcional)
        try {
            this.redisManager = new RedisManager(plugin);
            if (config.getConfig().getBoolean("redis.enabled", false)) {
                RedisCredentials credentials = new RedisCredentials(
                    config.getConfig().getString("redis.host", "localhost"),
                    config.getConfig().getInt("redis.port", 6379),
                    config.getConfig().getString("redis.password", ""),
                    config.getConfig().getBoolean("redis.use-ssl", false)
                );
                this.redisManager.initialize(credentials);
                this.leaderboardManager = new LeaderboardManager(redisManager);
                MidgardCore.setLeaderboardManager(this.leaderboardManager);
                // ConsoleUtils.logStatus("Redis Connection", true, "Connected");
                ConsoleUtils.log("");
                ConsoleUtils.log("  <gradient:#ff3333:#aa0000>● Redis Connection</gradient>");
                ConsoleUtils.log("    <gray>Status:<green>Connected ✔</green>");
            } else {
                 // ConsoleUtils.logStatus("Redis Connection", false, "Disabled");
            }
        } catch (Exception e) {
            ConsoleUtils.log("");
            ConsoleUtils.log("  <gradient:#ff3333:#aa0000>● Redis Connection</gradient>");
            ConsoleUtils.log("    <gray>Status:<red>Failed ✖</red>");
        }
        MidgardCore.setRedisManager(this.redisManager);
    }

    private void initLanguage() {
        this.languageManager = new LanguageManager(plugin);
        this.languageManager.load("ignored");
    }

    private void initCoreManagers() {
        plugin.getServer().getPluginManager().registerEvents(new GuiListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new StatUpdateListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ItemMechanicsListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MobDebugListener(), plugin);
        
        this.profileManager = new ProfileManager(plugin, databaseManager, redisManager);
        
        // Sync Listener
        if (redisManager != null && redisManager.isEnabled()) {
            me.ray.midgard.core.redis.SyncReqListener syncListener = new me.ray.midgard.core.redis.SyncReqListener(profileManager, redisManager);
            redisManager.subscribe("midgard:sync:req_save", syncListener);
        }
    }

    private void initIntegrations() {
        boolean mm = plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs");
        if (mm) {
            plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.mythicmobs.MythicMobsListener(), plugin);
            
            Task.syncLater(() -> {
                // ConsoleUtils.info("Recarregando MythicMobs para aplicar mecânicas...");
                org.bukkit.Bukkit.dispatchCommand(plugin.getServer().getConsoleSender(), "mm reload");
            }, 20L);
        }

        boolean papi = plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (papi) {
            this.placeholderRegistry = new PlaceholderRegistry(plugin);
            this.placeholderRegistry.register();
            MidgardCore.setPlaceholderRegistry(this.placeholderRegistry);
        }
        
        String hooked = (mm ? "MythicMobs" : "") + (mm && papi ? ", " : "") + (papi ? "PlaceholderAPI" : "");
        if (hooked.isEmpty()) hooked = "None";
        ConsoleUtils.log("    <gray>Integrations:<white>" + hooked + "</white>");
    }

    private void initStaticCore() {
        try {
            String version = plugin.getServer().getBukkitVersion();
            if (version.contains("1.21")) {
                Class<?> implClass = Class.forName("me.ray.midgard.nms.v1_21.NMSHandlerImpl");
                me.ray.midgard.nms.api.NMSHandler nmsHandler = (me.ray.midgard.nms.api.NMSHandler) implClass.getDeclaredConstructor().newInstance();
                MidgardCore.setNMSHandler(nmsHandler);
                ConsoleUtils.log("    <gray>NMS Handler:<white>v1.21</white>");
            } else {
                ConsoleUtils.log("    <gray>NMS Handler:<red>Unsupported: " + version + "</red>");
                ConsoleUtils.error("Versão do servidor não suportada: " + version + ". Funcionalidades NMS serão desativadas. Apenas 1.21 é suportada.");
            }
        } catch (Throwable e) {
            ConsoleUtils.logStatus("NMS Handler", false, "Critical Error");
            ConsoleUtils.error("Erro crítico ao carregar NMS Handler: " + e.getMessage());
        }
        
        MidgardCore.init(plugin, profileManager, languageManager);
    }

    private void registerBaseCommands() {
        // Criar e configurar comando principal unificado
        me.ray.midgard.core.command.UnifiedCommandManager mainCommand = new me.ray.midgard.core.command.UnifiedCommandManager();
        MidgardCore.setCommandManager(mainCommand);
        
        // Criar e configurar AdminCommand
        AdminCommand adminCommand = new AdminCommand(plugin);
        MidgardCore.setAdminCommand(adminCommand);
        
        // Registrar o comando principal no Bukkit
        plugin.getCommand("midgardrpg").setExecutor(mainCommand);
        plugin.getCommand("midgardrpg").setTabCompleter(mainCommand);
    }

    private void initModules() {
        this.moduleManager = new ModuleManager(plugin);
        MidgardCore.setModuleManager(this.moduleManager);
        
        // Helper method to safely register modules
        // Commands module should be loaded first (HIGH priority) to track all command registrations
        registerSafely("commands", () -> new CommandsModule());
        registerSafely("combat", () -> new CombatModule());
        registerSafely("essentials", () -> new EssentialsModule());
        registerSafely("classes", () -> new ClassesModule());
        registerSafely("item", () -> new ItemModule());
        registerSafely("character", () -> new CharacterModule());
        registerSafely("economy", () -> new EconomyModule());
        registerSafely("races", () -> new RacesModule());
        registerSafely("spells", () -> new SpellsModule());
        registerSafely("professions", () -> new ProfessionsModule());
        registerSafely("performance", () -> new PerformanceModule());
        
        moduleManager.enableAll();
    }
    
    private void registerSafely(String name, java.util.function.Supplier<me.ray.midgard.core.RPGModule> moduleSupplier) {
        if (shouldLoadModule(name)) {
            try {
                moduleManager.registerModule(moduleSupplier.get());
            } catch (Throwable e) {
                 ConsoleUtils.error("Falha ao registrar módulo: " + name + ". Ele será desativado. Erro: " + e.getMessage());
            }
        }
    }

    private boolean shouldLoadModule(String moduleName) {
        return mainConfig.getConfig().getBoolean("modules." + moduleName, true);
    }

    // Getters
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public RedisManager getRedisManager() { return redisManager; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public ProfileManager getProfileManager() { return profileManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public PlaceholderRegistry getPlaceholderRegistry() { return placeholderRegistry; }
    public me.ray.midgard.core.ModuleManager getModuleManager() { return moduleManager; }
}
