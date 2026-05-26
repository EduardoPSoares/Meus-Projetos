package me.ray.midgard.bot.core.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;

public class BotRedisManager {

    private static final Logger logger = LoggerFactory.getLogger(BotRedisManager.class);

    private final JedisPool pool;
    private boolean connected;

    public BotRedisManager(String host, int port, String password) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(4);
        poolConfig.setMaxIdle(2);

        if (password == null || password.isEmpty()) {
            this.pool = new JedisPool(poolConfig, host, port);
        } else {
            this.pool = new JedisPool(poolConfig, host, port, 2000, password);
        }

        // Test connection
        try (Jedis jedis = pool.getResource()) {
            jedis.ping();
            this.connected = true;
            logger.info("Redis connected to {}:{}", host, port);
        } catch (Exception e) {
            this.connected = false;
            logger.error("Failed to connect to Redis at {}:{}", host, port, e);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void setHash(String key, Map<String, String> fields) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, fields);
        } catch (Exception e) {
            logger.error("Failed to set Redis hash: {}", key, e);
        }
    }

    public void deleteKey(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Failed to delete Redis key: {}", key, e);
        }
    }

    public void publish(String channel, String message) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception e) {
            logger.error("Failed to publish to Redis channel: {}", channel, e);
        }
    }

    public void close() {
        if (pool != null) {
            pool.close();
            logger.info("Redis connection closed.");
        }
    }
}
