package me.ray.midgard.bot.core.database;

import me.ray.midgard.bot.core.database.migration.MigrationManager;
import me.ray.midgard.bot.core.database.query.InsertBuilder;
import me.ray.midgard.bot.core.database.query.QueryBuilder;
import me.ray.midgard.bot.core.database.query.TableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final Path dataDir;
    private final Map<String, Database> databases = new ConcurrentHashMap<>();
    private final Map<String, MigrationManager> migrationManagers = new ConcurrentHashMap<>();
    private Database defaultDatabase;

    public DatabaseManager(Path dataDir) {
        this.dataDir = dataDir;
        dataDir.toFile().mkdirs();
    }

    // ==================== Database Management ====================

    public Database createDatabase(String name, DatabaseConfig config) {
        Database db = new Database(config);
        databases.put(name, db);
        if (defaultDatabase == null) {
            defaultDatabase = db;
        }
        logger.info("Created database: {}", name);
        return db;
    }

    public Database createSQLiteDatabase(String name) {
        DatabaseConfig config = DatabaseConfig.sqlite(dataDir.resolve(name + ".db")).build();
        return createDatabase(name, config);
    }

    public Database getDatabase(String name) {
        return databases.get(name);
    }

    public Database getDefault() {
        if (defaultDatabase == null) {
            defaultDatabase = createSQLiteDatabase("midgard");
        }
        return defaultDatabase;
    }

    public void setDefault(String name) {
        Database db = databases.get(name);
        if (db != null) {
            defaultDatabase = db;
        }
    }

    // ==================== Migration Management ====================

    public MigrationManager getMigrations(String dbName) {
        return migrationManagers.computeIfAbsent(dbName, n -> {
            Database db = databases.get(n);
            if (db == null) throw new IllegalStateException("Database not found: " + n);
            return new MigrationManager(db);
        });
    }

    public MigrationManager getMigrations() {
        return getMigrations("midgard");
    }

    public void migrateAll() {
        for (var entry : migrationManagers.entrySet()) {
            logger.info("Running migrations for database: {}", entry.getKey());
            entry.getValue().migrate();
        }
    }

    // ==================== Shortcut methods (default database) ====================

    public void execute(String sql, Object... params) {
        getDefault().execute(sql, params);
    }

    public int update(String sql, Object... params) {
        return getDefault().update(sql, params);
    }

    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        return getDefault().queryOne(sql, mapper, params);
    }

    public <T> java.util.List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        return getDefault().queryList(sql, mapper, params);
    }

    public long queryLong(String sql, Object... params) {
        return getDefault().queryLong(sql, params);
    }

    public boolean queryExists(String sql, Object... params) {
        return getDefault().queryExists(sql, params);
    }

    public QueryBuilder table(String table) {
        return new QueryBuilder(getDefault(), table);
    }

    public InsertBuilder insertInto(String table) {
        return new InsertBuilder(getDefault(), table);
    }

    public TableBuilder createTable(String table) {
        return new TableBuilder(getDefault(), table);
    }

    // ==================== Lifecycle ====================

    public void closeAll() {
        for (var entry : databases.entrySet()) {
            try {
                entry.getValue().close();
                logger.info("Closed database: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Failed to close database: {}", entry.getKey(), e);
            }
        }
        databases.clear();
        migrationManagers.clear();
        defaultDatabase = null;
    }

    public Path getDataDir() { return dataDir; }
}
