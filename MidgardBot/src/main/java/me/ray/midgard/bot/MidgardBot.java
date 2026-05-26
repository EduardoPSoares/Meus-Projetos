package me.ray.midgard.bot;

import me.ray.midgard.bot.core.CoreListener;
import me.ray.midgard.bot.core.command.CommandManager;
import me.ray.midgard.bot.core.config.ConfigManager;
import me.ray.midgard.bot.core.config.JsonConfig;
import me.ray.midgard.bot.core.database.DatabaseManager;
import me.ray.midgard.bot.core.interaction.InteractionManager;
import me.ray.midgard.bot.core.module.BotModule;
import me.ray.midgard.bot.core.module.ModuleManager;
import me.ray.midgard.bot.core.redis.BotRedisManager;
import me.ray.midgard.bot.core.scheduler.TaskScheduler;
import me.ray.midgard.bot.core.storage.StorageManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumSet;

public class MidgardBot {

    private static final Logger logger = LoggerFactory.getLogger(MidgardBot.class);

    private static MidgardBot instance;

    private final JDA jda;
    private final BotConfig config;
    private final Instant startTime;

    // Core systems
    private final CommandManager commandManager;
    private final InteractionManager interactionManager;
    private final ModuleManager moduleManager;
    private final ConfigManager configManager;
    private final StorageManager storageManager;
    private final DatabaseManager databaseManager;
    private final TaskScheduler scheduler;
    private BotRedisManager redisManager;

    public MidgardBot(BotConfig config) throws Exception {
        instance = this;
        this.config = config;
        this.startTime = Instant.now();

        logger.info("============================================");
        logger.info("  Starting MidgardBot v1.0.0");
        logger.info("============================================");

        // Initialize core systems
        logger.info("Initializing core systems...");
        Path baseDir = Path.of(System.getProperty("user.dir"));
        this.configManager = new ConfigManager(baseDir.resolve("config"));
        this.storageManager = new StorageManager(baseDir.resolve("data"));
        this.databaseManager = new DatabaseManager(baseDir.resolve("data"));
        this.scheduler = new TaskScheduler(4);

        // Initialize default database
        logger.info("Initializing database...");
        databaseManager.createSQLiteDatabase("midgard");
        this.commandManager = new CommandManager(this);
        this.interactionManager = new InteractionManager(this);
        this.moduleManager = new ModuleManager(this);

        // Load bot config from JSON
        loadBotConfig();

        // Build JDA
        logger.info("Connecting to Discord...");
        this.jda = JDABuilder.createDefault(config.getToken())
                .enableIntents(EnumSet.of(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_PRESENCES,
                        GatewayIntent.GUILD_VOICE_STATES
                ))
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.VOICE_STATE, CacheFlag.ONLINE_STATUS)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("Midgard RPG"))
                .addEventListeners(new CoreListener(this))
                .build();

        this.jda.awaitReady();

        // Setup periodic tasks
        setupPeriodicTasks();

        logger.info("============================================");
        logger.info("  MidgardBot is ready!");
        logger.info("  Logged in as: {}", jda.getSelfUser().getName());
        logger.info("  Guilds: {}", jda.getGuilds().size());
        logger.info("  Commands: {}", commandManager.getCommandCount());
        logger.info("  Modules: {}", moduleManager.getModuleCount());
        logger.info("============================================");
    }

    private void loadBotConfig() {
        JsonConfig botConfig = configManager.getConfig("bot");

        // Config is read-only - never overwrite manual edits
        botConfig.setReadOnly(true);

        config.setOwnerId(botConfig.getString("ownerId", ""));
        config.setDevGuildId(botConfig.getLong("devGuildId", 0));
        config.setPrefix(botConfig.getString("prefix", "!"));

        // Initialize Redis if configured
        boolean redisEnabled = botConfig.getBoolean("redis.enabled", false);
        if (redisEnabled) {
            String redisHost = botConfig.getString("redis.host", "localhost");
            int redisPort = botConfig.getInt("redis.port", 6379);
            String redisPassword = botConfig.getString("redis.password", "");
            this.redisManager = new BotRedisManager(redisHost, redisPort, redisPassword);
            logger.info("Redis initialized: {}:{}", redisHost, redisPort);
        } else {
            logger.info("Redis disabled in config.");
        }
    }

    private void setupPeriodicTasks() {
        // Auto-save storage every 5 minutes (configs are read-only, never auto-saved)
        scheduler.repeatMinutes("auto-save", () -> {
            storageManager.saveAll();
        }, 5, 5);

        // Cleanup expired interactions every minute
        scheduler.repeatMinutes("interaction-cleanup", () -> {
            interactionManager.cleanupExpired();
        }, 1, 1);
    }

    // ==================== Module Registration ====================

    public void registerModule(BotModule module) {
        moduleManager.registerModule(module);
    }

    public void registerModules(BotModule... modules) {
        moduleManager.registerModules(modules);
    }

    public void enableAllModules() {
        moduleManager.enableAll();
    }

    // ==================== Lifecycle ====================

    public void shutdown() {
        logger.info("Shutting down MidgardBot...");

        // Disable modules
        moduleManager.disableAll();

        // Save all data (configs are read-only, only save storage)
        storageManager.saveAll();

        // Close databases
        databaseManager.closeAll();

        // Close Redis
        if (redisManager != null) {
            redisManager.close();
        }

        // Shutdown scheduler
        scheduler.shutdown();

        // Shutdown JDA
        jda.shutdown();

        logger.info("MidgardBot shut down successfully.");
    }

    // ==================== Accessors ====================

    public static MidgardBot getInstance() { return instance; }
    public JDA getJda() { return jda; }
    public BotConfig getConfig() { return config; }
    public CommandManager getCommandManager() { return commandManager; }
    public InteractionManager getInteractionManager() { return interactionManager; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public BotRedisManager getRedisManager() { return redisManager; }
    public TaskScheduler getScheduler() { return scheduler; }
    public Instant getStartTime() { return startTime; }

    public long getUptimeMillis() {
        return System.currentTimeMillis() - startTime.toEpochMilli();
    }

}
