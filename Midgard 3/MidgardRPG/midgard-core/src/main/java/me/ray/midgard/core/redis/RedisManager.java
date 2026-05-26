package me.ray.midgard.core.redis;

import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Gerencia a conexão e operações com Redis.
 */
public class RedisManager {

    private final JavaPlugin plugin;
    private JedisPool jedisPool;
    private boolean enabled = false;
    private final List<JedisPubSub> activeSubscriptions = new CopyOnWriteArrayList<>();

    /**
     * Construtor do RedisManager.
     *
     * @param plugin Instância do plugin.
     */
    public RedisManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa a conexão com o Redis.
     *
     * @param credentials Credenciais do Redis.
     */
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
            MidgardLogger.info("Conexão com Redis estabelecida com sucesso.");
            MidgardLogger.debug(DebugCategory.DATABASE, "Redis conectado: %s:%d", credentials.host(), credentials.port());
        } catch (Exception e) {
            MidgardLogger.error("Falha ao conectar ao Redis!", e);
            this.enabled = false;
        }
    }

    /**
     * Encerra o pool do Redis.
     */
    public void shutdown() {
        enabled = false;
        for (JedisPubSub sub : activeSubscriptions) {
            try {
                sub.unsubscribe();
            } catch (Exception e) {
                MidgardLogger.warn("Falha ao cancelar inscrição Redis: %s", e.getMessage());
            }
        }
        activeSubscriptions.clear();
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            MidgardLogger.info("Pool do Redis fechado.");
        }
    }

    /**
     * Verifica se o Redis está habilitado e conectado.
     *
     * @return true se habilitado, false caso contrário.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Executa uma ação no Redis.
     *
     * @param action Ação a ser executada.
     */
    public void execute(Consumer<Jedis> action) {
        if (!enabled) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            action.accept(jedis);
        } catch (Exception e) {
            MidgardLogger.error("Erro na execução do Redis", e);
        }
    }

    /**
     * Executa uma função no Redis e retorna um resultado.
     *
     * @param action Função a ser executada.
     * @param <T> Tipo do resultado.
     * @return Resultado da função ou null se falhar.
     */
    public <T> T execute(java.util.function.Function<Jedis, T> action) {
        if (!enabled) {
            return null;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            return action.apply(jedis);
        } catch (Exception e) {
            MidgardLogger.error("Erro na execução do Redis", e);
            return null;
        }
    }

    /**
     * Executa uma ação no Redis de forma assíncrona.
     *
     * @param action Ação a ser executada.
     * @return CompletableFuture vazio.
     */
    public CompletableFuture<Void> executeAsync(Consumer<Jedis> action) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> execute(action));
    }

    /**
     * Publica uma mensagem em um canal.
     *
     * @param channel Canal.
     * @param message Mensagem.
     */
    public void publish(String channel, String message) {
        executeAsync(jedis -> jedis.publish(channel, message));
    }

    /**
     * Inscreve-se em um canal.
     *
     * @param channel Canal.
     * @param subscriber Assinante.
     */
    public void subscribe(String channel, JedisPubSub subscriber) {
        if (!enabled) {
            return;
        }
        activeSubscriptions.add(subscriber);
        Thread subscribeThread = new Thread(() -> {
            while (enabled && !Thread.currentThread().isInterrupted()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(subscriber, channel);
                } catch (Exception e) {
                    if (enabled) {
                        MidgardLogger.warn("Conexão Redis perdida para o canal: %s. Reconectando em 5s...", channel);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            activeSubscriptions.remove(subscriber);
        });
        subscribeThread.setDaemon(true);
        subscribeThread.setName("Midgard-Redis-Sub-" + channel);
        subscribeThread.start();
    }
}
