package me.ray.midgard.bot.core.database.migration;

import me.ray.midgard.bot.core.database.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MigrationManager {

    private static final Logger logger = LoggerFactory.getLogger(MigrationManager.class);

    private final Database database;
    private final List<Migration> migrations = new ArrayList<>();

    public MigrationManager(Database database) {
        this.database = database;
        ensureMigrationTable();
    }

    private void ensureMigrationTable() {
        database.execute("""
            CREATE TABLE IF NOT EXISTS _migrations (
                version INTEGER PRIMARY KEY,
                description TEXT NOT NULL,
                applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    public MigrationManager register(Migration migration) {
        migrations.add(migration);
        return this;
    }

    public MigrationManager register(Migration... migrations) {
        for (Migration m : migrations) {
            register(m);
        }
        return this;
    }

    public void migrate() {
        int currentVersion = getCurrentVersion();

        List<Migration> pending = migrations.stream()
                .filter(m -> m.getVersion() > currentVersion)
                .sorted(Comparator.comparingInt(Migration::getVersion))
                .toList();

        if (pending.isEmpty()) {
            logger.info("Database is up to date (v{})", currentVersion);
            return;
        }

        logger.info("Running {} pending migration(s)...", pending.size());

        for (Migration migration : pending) {
            try {
                logger.info("Applying migration v{}: {}", migration.getVersion(), migration.getDescription());

                database.transaction(conn -> {
                    migration.up(database);

                    try {
                        database.updateInTransaction(conn,
                                "INSERT INTO _migrations (version, description, applied_at) VALUES (?, ?, ?)",
                                migration.getVersion(),
                                migration.getDescription(),
                                Instant.now().toString()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to record migration", e);
                    }
                });

                logger.info("Migration v{} applied successfully", migration.getVersion());
            } catch (Exception e) {
                logger.error("Migration v{} failed: {}", migration.getVersion(), e.getMessage(), e);
                throw new RuntimeException("Migration v" + migration.getVersion() + " failed", e);
            }
        }

        logger.info("Database migrated to v{}", getCurrentVersion());
    }

    public void rollback() {
        int currentVersion = getCurrentVersion();
        if (currentVersion == 0) {
            logger.info("No migrations to rollback");
            return;
        }

        Migration migration = migrations.stream()
                .filter(m -> m.getVersion() == currentVersion)
                .findFirst()
                .orElse(null);

        if (migration == null) {
            logger.warn("Migration v{} not found for rollback", currentVersion);
            return;
        }

        try {
            logger.info("Rolling back migration v{}: {}", migration.getVersion(), migration.getDescription());

            database.transaction(conn -> {
                migration.down(database);

                try {
                    database.updateInTransaction(conn,
                            "DELETE FROM _migrations WHERE version = ?",
                            migration.getVersion()
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Failed to remove migration record", e);
                }
            });

            logger.info("Migration v{} rolled back successfully", migration.getVersion());
        } catch (Exception e) {
            logger.error("Rollback of v{} failed", migration.getVersion(), e);
            throw new RuntimeException("Rollback failed", e);
        }
    }

    public int getCurrentVersion() {
        try {
            return database.queryInt("SELECT COALESCE(MAX(version), 0) FROM _migrations");
        } catch (Exception e) {
            return 0;
        }
    }

    public List<MigrationRecord> getAppliedMigrations() {
        return database.queryList(
                "SELECT version, description, applied_at FROM _migrations ORDER BY version",
                rs -> new MigrationRecord(
                        rs.getInt("version"),
                        rs.getString("description"),
                        rs.getString("applied_at")
                )
        );
    }

    public int getPendingCount() {
        int currentVersion = getCurrentVersion();
        return (int) migrations.stream()
                .filter(m -> m.getVersion() > currentVersion)
                .count();
    }

    public record MigrationRecord(int version, String description, String appliedAt) {}
}
