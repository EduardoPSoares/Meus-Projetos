package me.ray.midgard.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Gerencia a conexão com o banco de dados usando HikariCP.
 * Suporta MySQL e SQLite.
 */
public class DatabaseManager {

    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private String databaseType;
    private final ExecutorService executor;

    /**
     * Construtor do DatabaseManager.
     *
     * @param plugin Instância do plugin principal.
     */
    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Named daemon threads for better debugging and graceful shutdown
        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "MidgardRPG-DB-" + threadCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        this.executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()), 
            threadFactory
        );
    }

    /**
     * Inicializa o pool de conexões com as credenciais fornecidas.
     *
     * @param credentials Credenciais do banco de dados.
     */
    public void initialize(DatabaseCredentials credentials) {
        this.databaseType = credentials.type();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(credentials.toJdbcUrl(plugin.getDataFolder()));
        
        if (credentials.type().equalsIgnoreCase("mysql")) {
            config.setUsername(credentials.username());
            config.setPassword(credentials.password());
            
            // Performance properties for MySQL
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
        } else if (credentials.type().equalsIgnoreCase("sqlite")) {
            config.setDriverClassName("org.sqlite.JDBC");
        }
        
        // Pool settings optimized for Minecraft
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("MidgardRPG-Hikari");
        config.setConnectionTimeout(10000); // 10 seconds timeout
        config.setIdleTimeout(300000); // 5 minutes idle timeout
        config.setMaxLifetime(600000); // 10 minutes max lifetime
        config.setLeakDetectionThreshold(30000); // 30 seconds leak detection
        config.setKeepaliveTime(60000); // 1 minute keepalive

        try {
            this.dataSource = new HikariDataSource(config);
            MidgardLogger.info("Pool de conexões com o banco de dados inicializado com sucesso.");
            MidgardLogger.debug(DebugCategory.DATABASE, "Conexão estabelecida: JDBC Url=%s", credentials.toJdbcUrl(plugin.getDataFolder()));
        } catch (Exception e) {
            MidgardLogger.error("Falha ao inicializar o pool de conexões com o banco de dados!", e);
            throw new RuntimeException("Falha crítica ao conectar no banco de dados", e);
        }
    }

    /**
     * Executa uma ação no banco de dados de forma síncrona.
     *
     * @param action Ação a ser executada com a conexão.
     */
    public void execute(Consumer<Connection> action) {
        if (dataSource == null) {
            MidgardLogger.warn("Database operation skipped: dataSource is not initialized.");
            return;
        }
        try (Connection conn = dataSource.getConnection()) {
            try {
                action.accept(conn);
            } catch (Exception e) {
                MidgardLogger.error("Erro crítico na lógica da ação do banco de dados", e);
            }
        } catch (SQLException e) {
            MidgardLogger.error("Erro na conexão com o banco de dados", e);
        }
    }

    /**
     * Executa uma consulta no banco de dados de forma assíncrona.
     *
     * @param action Função que recebe a conexão e retorna um resultado.
     * @param <T> Tipo do resultado.
     * @return CompletableFuture com o resultado.
     */
    public <T> CompletableFuture<T> executeQuery(Function<Connection, T> action) {
        if (dataSource == null) {
            MidgardLogger.warn("Database query skipped: dataSource is not initialized.");
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                try {
                    return action.apply(conn);
                } catch (Exception e) {
                    MidgardLogger.error("Erro crítico na lógica da query", e);
                    return null;
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro na execução de query assíncrona (Conexão)", e);
                return null;
            }
        }, executor);
    }

    /**
     * Executa uma ação no banco de dados de forma assíncrona.
     *
     * @param action Ação a ser executada.
     * @return CompletableFuture vazio.
     */
    public CompletableFuture<Void> executeAsync(Consumer<Connection> action) {
        if (dataSource == null) {
            MidgardLogger.warn("Database async operation skipped: dataSource is not initialized.");
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> execute(action), executor);
    }

    /**
     * Fecha o pool de conexões.
     * Primeiro finaliza o executor para garantir que tasks em andamento terminem,
     * depois fecha o dataSource.
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    MidgardLogger.warn("Executor do banco de dados não terminou em 10s, forçando shutdown...");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            MidgardLogger.info("Pool de conexões com o banco de dados fechado.");
        }
    }

    /**
     * Obtém uma conexão do pool.
     *
     * @return Conexão SQL.
     * @throws SQLException Se ocorrer um erro ao obter a conexão.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("A fonte de dados não foi inicializada.");
        }
        return dataSource.getConnection();
    }

    public String getDatabaseType() {
        return databaseType;
    }

    /**
     * Executes a synchronous update.
     */
    public void executeUpdate(String sql, Consumer<PreparedStatement> preparer) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preparer.accept(ps);
            ps.executeUpdate();
        } catch (SQLException e) {
            MidgardLogger.error("Erro ao executar update: " + sql, e);
        }
    }

    /**
     * Executes an asynchronous update.
     */
    public CompletableFuture<Void> executeUpdateAsync(String sql, Consumer<PreparedStatement> preparer) {
        return CompletableFuture.runAsync(() -> executeUpdate(sql, preparer), executor);
    }

    /**
     * Executes a synchronous query.
     */
    public <T> T executeQuery(String sql, Consumer<PreparedStatement> preparer, Function<ResultSet, T> mapper) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preparer.accept(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return mapper.apply(rs);
            }
        } catch (SQLException e) {
            MidgardLogger.error("Erro ao executar query: " + sql, e);
            return null;
        }
    }

    /**
     * Executes an asynchronous query.
     */
    public <T> CompletableFuture<T> executeQueryAsync(String sql, Consumer<PreparedStatement> preparer, Function<ResultSet, T> mapper) {
        return CompletableFuture.supplyAsync(() -> executeQuery(sql, preparer, mapper), executor);
    }
}
