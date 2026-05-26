package me.ray.midgard.core.database;

import me.ray.midgard.core.debug.MidgardLogger;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Repositório genérico para dados de definição (raças, classes, spells, etc.).
 * Cada módulo usa uma tabela própria com schema: (id, category, data YAML, updated_at, updated_by).
 * <p>
 * Reutilizável por qualquer módulo que armazene entidades do tipo "template/definição".
 */
public class DefinitionRepository {

    private final DatabaseManager databaseManager;
    private final String tableName;
    private final String databaseType;

    public DefinitionRepository(DatabaseManager databaseManager, String tableName) {
        if (!tableName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
        this.databaseManager = databaseManager;
        this.tableName = tableName;
        this.databaseType = databaseManager.getDatabaseType();
        createTable();
    }

    private void createTable() {
        databaseManager.execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                String sql;
                if (databaseType.equalsIgnoreCase("mysql")) {
                    sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                          "id VARCHAR(128) PRIMARY KEY, " +
                          "category VARCHAR(64) NOT NULL DEFAULT '', " +
                          "data LONGTEXT NOT NULL, " +
                          "updated_at BIGINT NOT NULL DEFAULT 0, " +
                          "updated_by VARCHAR(64) NULL" +
                          ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                } else {
                    sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                          "id TEXT PRIMARY KEY, " +
                          "category TEXT NOT NULL DEFAULT '', " +
                          "data TEXT NOT NULL, " +
                          "updated_at INTEGER NOT NULL DEFAULT 0, " +
                          "updated_by TEXT NULL" +
                          ")";
                }
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao criar tabela " + tableName, e);
            }
        });
    }

    /**
     * Salva ou atualiza uma definição (assíncrono).
     */
    public CompletableFuture<Void> save(String id, String category, String yamlData, String updatedBy) {
        return databaseManager.executeAsync(conn -> {
            String sql;
            if (databaseType.equalsIgnoreCase("mysql")) {
                sql = "INSERT INTO " + tableName + " (id, category, data, updated_at, updated_by) VALUES (?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE category = VALUES(category), data = VALUES(data), " +
                      "updated_at = VALUES(updated_at), updated_by = VALUES(updated_by)";
            } else {
                sql = "INSERT OR REPLACE INTO " + tableName + " (id, category, data, updated_at, updated_by) " +
                      "VALUES (?, ?, ?, ?, ?)";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, category);
                ps.setString(3, yamlData);
                ps.setLong(4, System.currentTimeMillis());
                ps.setString(5, updatedBy);
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar " + id + " em " + tableName, e);
            }
        });
    }

    /**
     * Carrega todas as definições (síncrono, chamado no startup).
     */
    public Map<String, DefinitionData> loadAll() {
        Map<String, DefinitionData> result = new LinkedHashMap<>();
        databaseManager.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, category, data FROM " + tableName);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    result.put(id, new DefinitionData(
                        id, rs.getString("category"), rs.getString("data")));
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar dados de " + tableName, e);
            }
        });
        return result;
    }

    /**
     * Carrega uma definição específica (assíncrono).
     */
    public CompletableFuture<DefinitionData> load(String id) {
        return databaseManager.executeQuery(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, category, data FROM " + tableName + " WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new DefinitionData(
                            rs.getString("id"), rs.getString("category"), rs.getString("data"));
                    }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar " + id + " de " + tableName, e);
            }
            return null;
        });
    }

    /**
     * Deleta uma definição (assíncrono).
     */
    public CompletableFuture<Void> delete(String id) {
        return databaseManager.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM " + tableName + " WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao deletar " + id + " de " + tableName, e);
            }
        });
    }

    /**
     * Conta registros (síncrono).
     */
    public int count() {
        Integer result = databaseManager.executeQuery(
            "SELECT COUNT(*) FROM " + tableName,
            ps -> {},
            rs -> {
                try { return rs.next() ? rs.getInt(1) : 0; }
                catch (SQLException e) { return 0; }
            }
        );
        return result != null ? result : 0;
    }

    /**
     * IDs modificados desde timestamp (para polling).
     */
    public CompletableFuture<List<String>> getModifiedSince(long timestampMillis) {
        return databaseManager.executeQuery(conn -> {
            List<String> ids = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM " + tableName + " WHERE updated_at > ?")) {
                ps.setLong(1, timestampMillis);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) ids.add(rs.getString("id"));
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao buscar modificados em " + tableName, e);
            }
            return ids;
        });
    }

    /**
     * Todos os IDs no banco (para detectar deleções no polling).
     */
    public CompletableFuture<Set<String>> getAllIds() {
        return databaseManager.executeQuery(conn -> {
            Set<String> ids = new HashSet<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM " + tableName);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString("id"));
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar IDs de " + tableName, e);
            }
            return ids;
        });
    }

    public String getTableName() { return tableName; }

    /**
     * Registro de dados de uma definição.
     */
    public record DefinitionData(String id, String category, String yamlData) {}
}
