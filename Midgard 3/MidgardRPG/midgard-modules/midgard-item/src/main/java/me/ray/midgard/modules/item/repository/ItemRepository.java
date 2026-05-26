package me.ray.midgard.modules.item.repository;

import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.debug.MidgardLogger;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Repositório de itens no banco de dados.
 * Armazena cada item como uma entrada na tabela midgard_items,
 * com a ConfigurationSection serializada como YAML string na coluna 'data'.
 */
public class ItemRepository {

    private final DatabaseManager databaseManager;
    private final String databaseType;

    public ItemRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.databaseType = databaseManager.getDatabaseType();
        createTable();
    }

    private void createTable() {
        databaseManager.execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                String sql;
                if (databaseType.equalsIgnoreCase("mysql")) {
                    sql = "CREATE TABLE IF NOT EXISTS midgard_items (" +
                          "id VARCHAR(128) PRIMARY KEY, " +
                          "category_id VARCHAR(64) NOT NULL, " +
                          "data LONGTEXT NOT NULL, " +
                          "updated_at BIGINT NOT NULL DEFAULT 0, " +
                          "updated_by VARCHAR(64) NULL" +
                          ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                } else {
                    sql = "CREATE TABLE IF NOT EXISTS midgard_items (" +
                          "id TEXT PRIMARY KEY, " +
                          "category_id TEXT NOT NULL, " +
                          "data TEXT NOT NULL, " +
                          "updated_at INTEGER NOT NULL DEFAULT 0, " +
                          "updated_by TEXT NULL" +
                          ")";
                }
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao criar tabela midgard_items", e);
            }
        });
    }

    /**
     * Salva ou atualiza um item no banco de dados (assíncrono).
     */
    public CompletableFuture<Void> saveItem(String id, String categoryId, String yamlData, String updatedBy) {
        return databaseManager.executeAsync(conn -> {
            String sql;
            if (databaseType.equalsIgnoreCase("mysql")) {
                sql = "INSERT INTO midgard_items (id, category_id, data, updated_at, updated_by) VALUES (?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE category_id = VALUES(category_id), data = VALUES(data), " +
                      "updated_at = VALUES(updated_at), updated_by = VALUES(updated_by)";
            } else {
                sql = "INSERT OR REPLACE INTO midgard_items (id, category_id, data, updated_at, updated_by) " +
                      "VALUES (?, ?, ?, ?, ?)";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, categoryId);
                ps.setString(3, yamlData);
                ps.setLong(4, System.currentTimeMillis());
                ps.setString(5, updatedBy);
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar item " + id + " no banco de dados", e);
            }
        });
    }

    /**
     * Carrega todos os itens do banco de dados (síncrono, chamado no startup).
     */
    public Map<String, ItemData> loadAll() {
        Map<String, ItemData> result = new LinkedHashMap<>();
        databaseManager.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, category_id, data FROM midgard_items");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String categoryId = rs.getString("category_id");
                    String data = rs.getString("data");
                    result.put(id, new ItemData(id, categoryId, data));
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar itens do banco de dados", e);
            }
        });
        return result;
    }

    /**
     * Carrega um item específico do banco (assíncrono, usado para sync).
     */
    public CompletableFuture<ItemData> loadItem(String id) {
        return databaseManager.executeQuery(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, category_id, data FROM midgard_items WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new ItemData(rs.getString("id"), rs.getString("category_id"), rs.getString("data"));
                    }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar item " + id + " do banco de dados", e);
            }
            return null;
        });
    }

    /**
     * Deleta um item do banco de dados (assíncrono).
     */
    public CompletableFuture<Void> deleteItem(String id) {
        return databaseManager.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM midgard_items WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao deletar item " + id + " do banco de dados", e);
            }
        });
    }

    /**
     * Conta quantos itens existem no banco (síncrono).
     */
    public int count() {
        Integer result = databaseManager.executeQuery(
            "SELECT COUNT(*) FROM midgard_items",
            ps -> {},
            rs -> {
                try { return rs.next() ? rs.getInt(1) : 0; }
                catch (SQLException e) { return 0; } /* Fallback to 0 on read error */
            }
        );
        return result != null ? result : 0;
    }

    /**
     * Retorna IDs de itens modificados desde determinado timestamp (para polling).
     */
    public CompletableFuture<List<String>> getModifiedSince(long timestampMillis) {
        return databaseManager.executeQuery(conn -> {
            List<String> ids = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM midgard_items WHERE updated_at > ?")) {
                ps.setLong(1, timestampMillis);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getString("id"));
                    }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao buscar itens modificados", e);
            }
            return ids;
        });
    }

    /**
     * Retorna todos os IDs de itens no banco (para detectar deleções no polling).
     */
    public CompletableFuture<Set<String>> getAllIds() {
        return databaseManager.executeQuery(conn -> {
            Set<String> ids = new HashSet<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM midgard_items");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("id"));
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar IDs dos itens", e);
            }
            return ids;
        });
    }

    /**
     * Registro de dados de um item do banco.
     */
    public record ItemData(String id, String categoryId, String yamlData) {}
}
