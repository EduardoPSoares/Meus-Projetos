package me.ray.midgardProxy.redis;

import me.ray.midgardProxy.config.ConfigManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RedisManager {
    private final JedisPool pool;
    private final ExecutorService subscriberThread;
    private final Map<UUID, CompletableFuture<Void>> pendingSaves = new ConcurrentHashMap<>();

    public RedisManager(ConfigManager config) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        
        String password = config.getRedisPassword();
        if (password == null || password.isEmpty()) {
             this.pool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort());
        } else {
             this.pool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(), 2000, password);
        }
        this.subscriberThread = Executors.newSingleThreadExecutor();
    }

    public void publish(String channel, String message) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getHash(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hgetAll(key);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    public void setHash(String key, Map<String, String> fields) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, fields);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void subscribe(JedisPubSub pubSub, String... channels) {
        subscriberThread.submit(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(pubSub, channels);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void psubscribe(JedisPubSub pubSub, String... patterns) {
        subscriberThread.submit(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.psubscribe(pubSub, patterns);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> waitForSave(UUID uuid, long timeoutMs) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        pendingSaves.put(uuid, future);
        
        return future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> null); // Proceed anyway on timeout/error
    }

    public void completeSave(UUID uuid) {
        CompletableFuture<Void> future = pendingSaves.remove(uuid);
        if (future != null) {
            future.complete(null);
        }
    }
    
    public void close() {
        subscriberThread.shutdownNow();
        pool.close();
    }
}
