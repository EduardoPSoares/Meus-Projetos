package me.ray.midgard.bot.modules.whitelist;

import me.ray.midgard.bot.core.config.JsonConfig;
import me.ray.midgard.bot.core.database.Database;
import me.ray.midgard.bot.core.database.DatabaseConfig;
import me.ray.midgard.bot.core.module.BotModule;
import me.ray.midgard.bot.core.module.ModuleInfo;

@ModuleInfo(
        name = "Whitelist",
        description = "Sistema de whitelist com formulário configurável",
        version = "1.0.0"
)
public class WhitelistModule extends BotModule {

    private WhitelistConfig whitelistConfig;
    private WhitelistRepository repository;
    private WhitelistListener listener;
    private WhitelistReviewManager reviewManager;
    private WhitelistReviewListener reviewListener;
    private WhitelistRedisSync redisSync;

    @Override
    public void onEnable() {
        // Load config
        JsonConfig rawConfig = bot.getConfigManager().getConfig("whitelist");
        this.whitelistConfig = new WhitelistConfig(rawConfig);

        // Setup database (MySQL if configured, otherwise default SQLite)
        Database db = setupDatabase();
        this.repository = new WhitelistRepository(db);
        repository.createTable();

        // Setup Redis cache sync
        if (bot.getRedisManager() != null && bot.getRedisManager().isConnected()) {
            this.redisSync = new WhitelistRedisSync(bot.getRedisManager());
            redisSync.syncAll(repository);
        }

        // Setup listener
        this.listener = new WhitelistListener(bot, whitelistConfig, repository);
        listener.setRedisSync(redisSync);
        listener.register();
        listener.registerReviewButtons();

        // Setup review system
        this.reviewManager = new WhitelistReviewManager(repository);
        this.reviewListener = new WhitelistReviewListener(bot, whitelistConfig, repository, reviewManager);
        reviewListener.setRedisSync(redisSync);
        reviewListener.register();
        listener.setReviewListener(reviewListener);

        // Register commands
        WhitelistCommand command = new WhitelistCommand(whitelistConfig, repository);
        command.setReviewListener(reviewListener);
        command.setRedisSync(redisSync);
        registerCommand(command);

        logger.info("Whitelist module enabled - {} questions in {} parts",
                whitelistConfig.getAllQuestions().stream().mapToInt(java.util.List::size).sum(),
                whitelistConfig.getPartCount());
    }

    private Database setupDatabase() {
        JsonConfig botConfig = bot.getConfigManager().getConfig("bot");
        boolean mysqlEnabled = botConfig.getBoolean("mysql.enabled", false);

        if (mysqlEnabled) {
            String host = botConfig.getString("mysql.host", "localhost");
            int port = botConfig.getInt("mysql.port", 3306);
            String database = botConfig.getString("mysql.database", "midgard");
            String user = botConfig.getString("mysql.username", "root");
            String password = botConfig.getString("mysql.password", "");

            DatabaseConfig config = DatabaseConfig.mysql(host, port, database, user, password)
                    .maxPoolSize(10)
                    .build();

            Database db = bot.getDatabaseManager().createDatabase("whitelist-mysql", config);
            logger.info("Whitelist using MySQL database: {}:{}/{}", host, port, database);
            return db;
        }

        logger.info("Whitelist using default SQLite database");
        return bot.getDatabaseManager().getDefault();
    }

    @Override
    public void onDisable() {
        // Databases are closed by DatabaseManager
    }

    public WhitelistConfig getWhitelistConfig() { return whitelistConfig; }
    public WhitelistRepository getRepository() { return repository; }
    public WhitelistReviewManager getReviewManager() { return reviewManager; }
    public WhitelistReviewListener getReviewListener() { return reviewListener; }
}
