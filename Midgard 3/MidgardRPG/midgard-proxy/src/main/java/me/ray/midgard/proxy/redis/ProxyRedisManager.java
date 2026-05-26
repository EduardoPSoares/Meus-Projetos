package me.ray.midgard.proxy.redis;

import org.slf4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProxyRedisManager {

    private final Logger logger;
    private JedisPool jedisPool;
    private boolean enabled = false;

    public ProxyRedisManager(@SuppressWarnings("unused") com.velocitypowered.api.proxy.ProxyServer server, Logger logger) {
        this.logger = logger;
    }

    public void initialize(RedisCredentials credentials) {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(16);
            poolConfig.setMaxIdle(8);
            poolConfig.setMinIdle(2);

            String password = (credentials.password() != null && !credentials.password().isEmpty()) ? credentials.password() : null;

            this.jedisPool = new JedisPool(poolConfig, credentials.host(), credentials.port(), 2000, password, credentials.useSsl());
            
            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }
            
            this.enabled = true;
            logger.info("Conexão com Redis estabelecida com sucesso.");
        } catch (Exception e) {
            logger.error("Falha ao conectar ao Redis!", e);
            this.enabled = false;
        }
    }

    public void shutdown() {
        enabled = false;
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Pool do Redis fechado.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void execute(Consumer<Jedis> action) {
        if (!enabled) return;
        try (Jedis jedis = jedisPool.getResource()) {
            action.accept(jedis);
        } catch (Exception e) {
            logger.error("Erro na execução do Redis", e);
        }
    }

    public <T> T execute(Function<Jedis, T> action) {
        if (!enabled) return null;
        try (Jedis jedis = jedisPool.getResource()) {
            return action.apply(jedis);
        } catch (Exception e) {
            logger.error("Erro na execução do Redis", e);
            return null;
        }
    }

    public CompletableFuture<Void> executeAsync(Consumer<Jedis> action) {
        if (!enabled) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> execute(action));
    }

    public void publish(String channel, String message) {
        executeAsync(jedis -> jedis.publish(channel, message));
    }

    public void subscribe(String channel, JedisPubSub subscriber) {
        if (!enabled) return;
        Thread thread = new Thread(() -> {
            while (enabled && !Thread.currentThread().isInterrupted()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(subscriber, channel);
                } catch (Exception e) {
                    if (enabled) {
                        logger.error("Conexão Redis perdida para o canal: " + channel + ". Reconectando em 5s...", e);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("MidgardRedis-Sub-" + channel);
        thread.start();
    }

    public void psubscribe(String pattern, JedisPubSub subscriber) {
        if (!enabled) return;
        Thread thread = new Thread(() -> {
            while (enabled && !Thread.currentThread().isInterrupted()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.psubscribe(subscriber, pattern);
                } catch (Exception e) {
                    if (enabled) {
                        logger.error("Conexão Redis perdida para o padrão: " + pattern + ". Reconectando em 5s...", e);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.setName("MidgardRedis-PSub-" + pattern);
        thread.start();
    }
}