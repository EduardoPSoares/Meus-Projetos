package me.ray.midgard.bot.modules.whitelist;

import me.ray.midgard.bot.core.redis.BotRedisManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Syncs whitelist status to Redis as a cache layer.
 * The proxy reads Redis first for fast lookups.
 *
 * Redis key: midgard:whitelist:{nick_lowercase}
 * Fields: status, discord_id, username, forced
 */
public class WhitelistRedisSync {

    private static final Logger logger = LoggerFactory.getLogger(WhitelistRedisSync.class);
    private static final String KEY_PREFIX = "midgard:whitelist:";

    private final BotRedisManager redis;

    public WhitelistRedisSync(BotRedisManager redis) {
        this.redis = redis;
    }

    public void syncApplication(WhitelistApplication app) {
        if (redis == null || !redis.isConnected()) return;

        String nick = app.getAnswer("nick");
        if (nick == null || nick.isBlank()) return;

        String key = KEY_PREFIX + nick.toLowerCase();

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("status", app.getStatus().name());
        fields.put("discord_id", app.getUserId());
        fields.put("username", app.getUsername() != null ? app.getUsername() : "");
        fields.put("forced", app.isForced() ? "true" : "false");

        redis.setHash(key, fields);
        logger.debug("Synced whitelist to Redis: {} -> {}", nick, app.getStatus());
    }

    public void removeApplication(String nick) {
        if (redis == null || !redis.isConnected()) return;
        if (nick == null || nick.isBlank()) return;

        redis.deleteKey(KEY_PREFIX + nick.toLowerCase());
        logger.debug("Removed whitelist from Redis: {}", nick);
    }

    /**
     * Syncs all non-in-progress applications to Redis on startup.
     */
    public void syncAll(WhitelistRepository repository) {
        if (redis == null || !redis.isConnected()) return;

        var all = repository.findAll();
        int count = 0;
        for (WhitelistApplication app : all) {
            if (app.getStatus() == WhitelistApplication.Status.IN_PROGRESS) continue;
            syncApplication(app);
            count++;
        }
        logger.info("Synced {} whitelist entries to Redis", count);
    }
}
