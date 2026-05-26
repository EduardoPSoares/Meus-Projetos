package me.ray.midgard.bot.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class Database {

    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    private final DatabaseConfig config;
    private final HikariDataSource dataSource;
    private final ExecutorService asyncExecutor;

    public Database(DatabaseConfig config) {
        this.config = config;

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(config.getIdleTimeout());
        hikariConfig.setMaxLifetime(config.getMaxLifetime());
        hikariConfig.setPoolName("MidgardBot-DB");

        // MySQL credentials
        if (config.getUsername() != null) {
            hikariConfig.setUsername(config.getUsername());
        }
        if (config.getPassword() != null) {
            hikariConfig.setPassword(config.getPassword());
        }

        // SQLite optimizations
        if (config.getUrl().contains("sqlite")) {
            hikariConfig.addDataSourceProperty("journal_mode", "WAL");
            hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
            hikariConfig.addDataSourceProperty("foreign_keys", "ON");
            hikariConfig.addDataSourceProperty("busy_timeout", "5000");
            hikariConfig.addDataSourceProperty("cache_size", "-8000");
        }

        this.dataSource = new HikariDataSource(hikariConfig);
        this.asyncExecutor = Executors.newFixedThreadPool(
                Math.max(2, config.getMaxPoolSize() / 2),
                r -> {
                    Thread t = new Thread(r, "MidgardBot-DB-Async");
                    t.setDaemon(true);
                    return t;
                }
        );

        // Apply SQLite PRAGMA settings via WAL mode
        if (config.isWalMode() && config.getUrl().contains("sqlite")) {
            execute("PRAGMA journal_mode=WAL");
            execute("PRAGMA synchronous=NORMAL");
            execute("PRAGMA foreign_keys=ON");
            execute("PRAGMA busy_timeout=5000");
            execute("PRAGMA cache_size=-8000");
        }

        logger.info("Database initialized: {}", config.getUrl());
    }

    // ==================== Connection ====================

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // ==================== Execute (DDL / DML without result) ====================

    public void execute(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params)) {
            stmt.execute();
        } catch (SQLException e) {
            logger.error("Failed to execute SQL: {}", sql, e);
            throw new DatabaseException("Failed to execute SQL", e);
        }
    }

    public int update(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params)) {
            return stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to execute update: {}", sql, e);
            throw new DatabaseException("Failed to execute update", e);
        }
    }

    // ==================== Query ====================

    public <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return mapper.map(rs);
            }
            return null;
        } catch (SQLException e) {
            logger.error("Failed to query: {}", sql, e);
            throw new DatabaseException("Failed to query", e);
        }
    }

    public <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.map(rs));
            }
            return results;
        } catch (SQLException e) {
            logger.error("Failed to query list: {}", sql, e);
            throw new DatabaseException("Failed to query list", e);
        }
    }

    public <T> Optional<T> queryOptional(String sql, RowMapper<T> mapper, Object... params) {
        return Optional.ofNullable(queryOne(sql, mapper, params));
    }

    public long queryLong(String sql, Object... params) {
        Long result = queryOne(sql, rs -> rs.getLong(1), params);
        return result != null ? result : 0L;
    }

    public int queryInt(String sql, Object... params) {
        Integer result = queryOne(sql, rs -> rs.getInt(1), params);
        return result != null ? result : 0;
    }

    public String queryString(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getString(1), params);
    }

    public boolean queryExists(String sql, Object... params) {
        return queryOne(sql, rs -> true, params) != null;
    }

    // ==================== Batch ====================

    public int[] executeBatch(String sql, List<Object[]> paramSets) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (Object[] params : paramSets) {
                    setParameters(stmt, params);
                    stmt.addBatch();
                }
                int[] results = stmt.executeBatch();
                conn.commit();
                return results;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to execute batch: {}", sql, e);
            throw new DatabaseException("Failed to execute batch", e);
        }
    }

    // ==================== Transaction ====================

    public void transaction(Consumer<Connection> action) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                action.accept(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Transaction failed", e);
            throw new DatabaseException("Transaction failed", e);
        }
    }

    public <T> T transactionResult(Function<Connection, T> action) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = action.apply(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Transaction failed", e);
            throw new DatabaseException("Transaction failed", e);
        }
    }

    // ==================== Transaction-scoped helpers ====================

    public int updateInTransaction(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = prepareStatement(conn, sql, params)) {
            return stmt.executeUpdate();
        }
    }

    public <T> T queryOneInTransaction(Connection conn, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return mapper.map(rs);
            }
            return null;
        }
    }

    // ==================== Async ====================

    public CompletableFuture<Void> executeAsync(String sql, Object... params) {
        return CompletableFuture.runAsync(() -> execute(sql, params), asyncExecutor);
    }

    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> update(sql, params), asyncExecutor);
    }

    public <T> CompletableFuture<T> queryOneAsync(String sql, RowMapper<T> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> queryOne(sql, mapper, params), asyncExecutor);
    }

    public <T> CompletableFuture<List<T>> queryListAsync(String sql, RowMapper<T> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> queryList(sql, mapper, params), asyncExecutor);
    }

    public <T> CompletableFuture<Optional<T>> queryOptionalAsync(String sql, RowMapper<T> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> queryOptional(sql, mapper, params), asyncExecutor);
    }

    public CompletableFuture<Void> transactionAsync(Consumer<Connection> action) {
        return CompletableFuture.runAsync(() -> transaction(action), asyncExecutor);
    }

    // ==================== Table Utilities ====================

    public boolean isMysql() {
        return config.isMysql();
    }

    public boolean tableExists(String tableName) {
        if (isMysql()) {
            return queryExists("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_NAME = ?", tableName);
        }
        return queryExists(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                tableName
        );
    }

    public List<String> getTableNames() {
        if (isMysql()) {
            return queryList("SHOW TABLES", rs -> rs.getString(1));
        }
        return queryList(
                "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",
                rs -> rs.getString("name")
        );
    }

    // ==================== Internal ====================

    private PreparedStatement prepareStatement(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        setParameters(stmt, params);
        return stmt;
    }

    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param == null) {
                stmt.setNull(i + 1, java.sql.Types.NULL);
            } else if (param instanceof String s) {
                stmt.setString(i + 1, s);
            } else if (param instanceof Integer n) {
                stmt.setInt(i + 1, n);
            } else if (param instanceof Long n) {
                stmt.setLong(i + 1, n);
            } else if (param instanceof Double n) {
                stmt.setDouble(i + 1, n);
            } else if (param instanceof Float n) {
                stmt.setFloat(i + 1, n);
            } else if (param instanceof Boolean b) {
                stmt.setBoolean(i + 1, b);
            } else if (param instanceof byte[] bytes) {
                stmt.setBytes(i + 1, bytes);
            } else {
                stmt.setObject(i + 1, param);
            }
        }
    }

    // ==================== Lifecycle ====================

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}
