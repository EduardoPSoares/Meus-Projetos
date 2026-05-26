package me.ray.midgardProxy.whitelist;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.ray.midgardProxy.config.ConfigManager;
import me.ray.midgardProxy.redis.RedisManager;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class WhitelistChecker {

    private static final String REDIS_KEY_PREFIX = "midgard:whitelist:";

    private final HikariDataSource dataSource;
    private final RedisManager redisManager;
    private final Logger logger;

    public WhitelistChecker(ConfigManager config, RedisManager redisManager, Logger logger) {
        this.redisManager = redisManager;
        this.logger = logger;

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mysql://" + config.getWhitelistMysqlHost() + ":" + config.getWhitelistMysqlPort()
                + "/" + config.getWhitelistMysqlDatabase() + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4");
        hikari.setUsername(config.getWhitelistMysqlUsername());
        hikari.setPassword(config.getWhitelistMysqlPassword());
        hikari.setMaximumPoolSize(3);
        hikari.setPoolName("whitelist-pool");

        this.dataSource = new HikariDataSource(hikari);
        logger.info("Whitelist MySQL pool initialized ({}:{})", config.getWhitelistMysqlHost(), config.getWhitelistMysqlPort());
    }

    /**
     * Check whitelist status for a Minecraft nick.
     * Checks Redis cache first, then falls back to MySQL.
     * Returns null if no application found.
     */
    public WhitelistStatus check(String minecraftNick) {
        // 1. Try Redis cache
        if (redisManager != null) {
            WhitelistStatus cached = checkRedis(minecraftNick);
            if (cached != null) {
                return cached;
            }
        }

        // 2. Fall back to MySQL
        return checkMysql(minecraftNick);
    }

    private WhitelistStatus checkRedis(String minecraftNick) {
        try {
            String key = REDIS_KEY_PREFIX + minecraftNick.toLowerCase();
            Map<String, String> data = redisManager.getHash(key);

            if (data != null && !data.isEmpty() && data.containsKey("status")) {
                String status = data.get("status");
                String discordId = data.get("discord_id");
                return new WhitelistStatus(status, discordId);
            }
        } catch (Exception e) {
            logger.warn("Redis cache check failed for {}, falling back to MySQL", minecraftNick, e);
        }
        return null;
    }

    private WhitelistStatus checkMysql(String minecraftNick) {
        String sql = "SELECT status, user_id, username, answers FROM whitelist_applications " +
                "WHERE JSON_UNQUOTE(JSON_EXTRACT(answers, '$.nick')) = ? AND status != 'IN_PROGRESS'";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, minecraftNick);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    String discordId = rs.getString("user_id");
                    String username = rs.getString("username");

                    // Cache to Redis for next time
                    if (redisManager != null) {
                        cacheToRedis(minecraftNick, status, discordId, username);
                    }

                    return new WhitelistStatus(status, discordId);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to check whitelist in MySQL for nick: {}", minecraftNick, e);
        }

        return null;
    }

    private void cacheToRedis(String nick, String status, String discordId, String username) {
        try {
            String key = REDIS_KEY_PREFIX + nick.toLowerCase();
            redisManager.setHash(key, Map.of(
                    "status", status,
                    "discord_id", discordId != null ? discordId : "",
                    "username", username != null ? username : ""
            ));
        } catch (Exception e) {
            logger.warn("Failed to cache whitelist to Redis for nick: {}", nick, e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public record WhitelistStatus(String status, String discordId) {}
}
