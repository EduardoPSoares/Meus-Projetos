package me.ray.midgard.bot.modules.whitelist;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.ray.midgard.bot.core.database.Database;
import me.ray.midgard.bot.core.database.RowMapper;
import me.ray.midgard.bot.core.database.repository.Repository;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WhitelistRepository extends Repository<WhitelistApplication, String> {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, String>>() {}.getType();

    public WhitelistRepository(Database database) {
        super(database, "whitelist_applications", "user_id");
    }

    public void createTable() {
        if (database.isMysql()) {
            createMysqlTable();
        } else {
            createSqliteTable();
        }
    }

    private void createSqliteTable() {
        database.execute(
                "CREATE TABLE IF NOT EXISTS whitelist_applications (" +
                "  user_id TEXT PRIMARY KEY," +
                "  username TEXT," +
                "  status TEXT NOT NULL DEFAULT 'IN_PROGRESS'," +
                "  current_part INTEGER NOT NULL DEFAULT 0," +
                "  answers TEXT NOT NULL DEFAULT '{}'," +
                "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  reviewed_by TEXT," +
                "  review_note TEXT," +
                "  forced INTEGER NOT NULL DEFAULT 0" +
                ")"
        );

        database.execute(
                "CREATE INDEX IF NOT EXISTS idx_whitelist_status ON whitelist_applications(status)"
        );

        // Migration: add forced column if missing (for existing databases)
        try {
            database.execute("ALTER TABLE whitelist_applications ADD COLUMN forced INTEGER NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
            // Column already exists
        }
    }

    private void createMysqlTable() {
        database.execute(
                "CREATE TABLE IF NOT EXISTS whitelist_applications (" +
                "  user_id VARCHAR(64) PRIMARY KEY," +
                "  username VARCHAR(128)," +
                "  status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS'," +
                "  current_part INT NOT NULL DEFAULT 0," +
                "  answers MEDIUMTEXT," +
                "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  reviewed_by VARCHAR(64)," +
                "  review_note TEXT," +
                "  forced TINYINT NOT NULL DEFAULT 0," +
                "  INDEX idx_whitelist_status (status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );

        // Migration: add forced column if missing
        try {
            database.execute("ALTER TABLE whitelist_applications ADD COLUMN forced TINYINT NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
            // Column already exists
        }
    }

    @Override
    protected RowMapper<WhitelistApplication> getMapper() {
        return rs -> {
            String userId = rs.getString("user_id");
            String username = rs.getString("username");
            String statusStr = rs.getString("status");
            int currentPart = rs.getInt("current_part");
            String answersJson = rs.getString("answers");
            String reviewedBy = rs.getString("reviewed_by");
            String reviewNote = rs.getString("review_note");

            boolean forced;
            try {
                forced = rs.getInt("forced") == 1;
            } catch (Exception e) {
                forced = false;
            }

            WhitelistApplication.Status status;
            try {
                status = WhitelistApplication.Status.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                status = WhitelistApplication.Status.IN_PROGRESS;
            }

            Map<String, String> answers;
            try {
                answers = GSON.fromJson(answersJson, MAP_TYPE);
                if (answers == null) answers = new LinkedHashMap<>();
            } catch (Exception e) {
                answers = new LinkedHashMap<>();
            }

            Instant createdAt;
            Instant updatedAt;
            try {
                java.sql.Timestamp tsCreated = rs.getTimestamp("created_at");
                createdAt = tsCreated != null ? tsCreated.toInstant() : Instant.now();
            } catch (Exception e) {
                createdAt = Instant.now();
            }
            try {
                java.sql.Timestamp tsUpdated = rs.getTimestamp("updated_at");
                updatedAt = tsUpdated != null ? tsUpdated.toInstant() : Instant.now();
            } catch (Exception e) {
                updatedAt = Instant.now();
            }

            return new WhitelistApplication(userId, username, status, currentPart,
                    answers, createdAt, updatedAt, reviewedBy, reviewNote, forced);
        };
    }

    @Override
    protected Map<String, Object> toMap(WhitelistApplication app) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user_id", app.getUserId());
        map.put("username", app.getUsername());
        map.put("status", app.getStatus().name());
        map.put("current_part", app.getCurrentPart());
        map.put("answers", GSON.toJson(app.getAnswers()));
        map.put("created_at", java.sql.Timestamp.from(app.getCreatedAt()));
        map.put("updated_at", java.sql.Timestamp.from(app.getUpdatedAt()));
        map.put("reviewed_by", app.getReviewedBy());
        map.put("review_note", app.getReviewNote());
        map.put("forced", app.isForced() ? 1 : 0);
        return map;
    }

    // ==================== Custom Queries ====================

    public List<WhitelistApplication> findByStatus(WhitelistApplication.Status status) {
        return findByColumn("status", status.name());
    }

    public List<WhitelistApplication> findPending() {
        return findByStatus(WhitelistApplication.Status.PENDING);
    }

    public List<WhitelistApplication> findPendingOrderedByDate() {
        return query()
                .where("status", WhitelistApplication.Status.PENDING.name())
                .orderBy("created_at")
                .get(getMapper());
    }

    public long countByStatus(WhitelistApplication.Status status) {
        return countByColumn("status", status.name());
    }

    public long countPending() {
        return countByStatus(WhitelistApplication.Status.PENDING);
    }

    public void updateStatus(String userId, WhitelistApplication.Status status) {
        updateColumn(userId, "status", status.name());
        updateColumn(userId, "updated_at", Instant.now().toString());
    }
}
