package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.debug.MidgardLogger;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Database repository for smeltery structures.
 * Handles persistence of smelteries (previously lost on restart).
 */
public class SmelteryRepository {

    private final DatabaseManager databaseManager;
    private final String dbType;

    public SmelteryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.dbType = databaseManager.getDatabaseType();
        createTables();
    }

    private void createTables() {
        databaseManager.execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                String smelteriesTable;
                if (dbType.equalsIgnoreCase("mysql")) {
                    smelteriesTable = """
                        CREATE TABLE IF NOT EXISTS midgard_smelteries (
                            smeltery_id VARCHAR(36) PRIMARY KEY,
                            owner_uuid VARCHAR(36) NOT NULL,
                            world_name VARCHAR(128) NOT NULL,
                            x INT NOT NULL,
                            y INT NOT NULL,
                            z INT NOT NULL,
                            tier VARCHAR(32) NOT NULL,
                            created_at BIGINT NOT NULL,
                            last_used BIGINT NOT NULL,
                            total_items_smelted INT NOT NULL DEFAULT 0,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            name VARCHAR(128) DEFAULT NULL,
                            INDEX idx_smelteries_owner (owner_uuid),
                            INDEX idx_smelteries_world (world_name)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
                } else {
                    smelteriesTable = """
                        CREATE TABLE IF NOT EXISTS midgard_smelteries (
                            smeltery_id TEXT PRIMARY KEY,
                            owner_uuid TEXT NOT NULL,
                            world_name TEXT NOT NULL,
                            x INTEGER NOT NULL,
                            y INTEGER NOT NULL,
                            z INTEGER NOT NULL,
                            tier TEXT NOT NULL,
                            created_at INTEGER NOT NULL,
                            last_used INTEGER NOT NULL,
                            total_items_smelted INTEGER NOT NULL DEFAULT 0,
                            active INTEGER NOT NULL DEFAULT 1,
                            name TEXT DEFAULT NULL
                        )""";
                }
                stmt.executeUpdate(smelteriesTable);
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao criar tabelas de smeltery", e);
            }
        });
    }

    // ==================== Smeltery CRUD ====================

    /**
     * Saves a smeltery structure to the database.
     */
    public CompletableFuture<Void> saveSmeltery(SmelteryStructure smeltery) {
        return databaseManager.executeAsync(conn -> {
            String sql;
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = """
                    INSERT INTO midgard_smelteries (smeltery_id, owner_uuid, world_name, x, y, z, tier,
                        created_at, last_used, total_items_smelted, active, name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE last_used = VALUES(last_used),
                        total_items_smelted = VALUES(total_items_smelted), active = VALUES(active), name = VALUES(name)""";
            } else {
                sql = """
                    INSERT OR REPLACE INTO midgard_smelteries (smeltery_id, owner_uuid, world_name, x, y, z, tier,
                        created_at, last_used, total_items_smelted, active, name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, smeltery.getSmelteryId().toString());
                ps.setString(2, smeltery.getOwnerUuid().toString());
                ps.setString(3, smeltery.getWorldName());
                ps.setInt(4, smeltery.getX());
                ps.setInt(5, smeltery.getY());
                ps.setInt(6, smeltery.getZ());
                ps.setString(7, smeltery.getTier().name());
                ps.setLong(8, smeltery.getCreatedAt());
                ps.setLong(9, smeltery.getLastUsed());
                ps.setInt(10, smeltery.getTotalItemsSmelted());
                ps.setBoolean(11, smeltery.isActive());
                ps.setString(12, smeltery.getName());
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar smeltery " + smeltery.getSmelteryId(), e);
            }
        });
    }

    /**
     * Loads all active smeltery structures from the database.
     */
    public List<SmelteryStructure> loadAllSmelteries() {
        List<SmelteryStructure> smelteries = new ArrayList<>();
        databaseManager.execute(conn -> {
            String activeCondition = dbType.equalsIgnoreCase("mysql") ? "TRUE" : "1";
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM midgard_smelteries WHERE active = " + activeCondition);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SmelteryStructure smeltery = mapSmeltery(rs);
                    if (smeltery != null) { smelteries.add(smeltery); }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar smelteries do banco", e);
            }
        });
        return smelteries;
    }

    /**
     * Deletes a smeltery from the database.
     */
    public CompletableFuture<Void> deleteSmeltery(UUID smelteryId) {
        return databaseManager.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM midgard_smelteries WHERE smeltery_id = ?")) {
                ps.setString(1, smelteryId.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao deletar smeltery " + smelteryId, e);
            }
        });
    }

    /**
     * Saves all smelteries in batch (used during shutdown).
     */
    public void saveAll(Collection<SmelteryStructure> smelteries) {
        databaseManager.execute(conn -> {
            String sql;
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = """
                    INSERT INTO midgard_smelteries (smeltery_id, owner_uuid, world_name, x, y, z, tier,
                        created_at, last_used, total_items_smelted, active, name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE last_used = VALUES(last_used),
                        total_items_smelted = VALUES(total_items_smelted), active = VALUES(active), name = VALUES(name)""";
            } else {
                sql = """
                    INSERT OR REPLACE INTO midgard_smelteries (smeltery_id, owner_uuid, world_name, x, y, z, tier,
                        created_at, last_used, total_items_smelted, active, name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (SmelteryStructure smeltery : smelteries) {
                    ps.setString(1, smeltery.getSmelteryId().toString());
                    ps.setString(2, smeltery.getOwnerUuid().toString());
                    ps.setString(3, smeltery.getWorldName());
                    ps.setInt(4, smeltery.getX());
                    ps.setInt(5, smeltery.getY());
                    ps.setInt(6, smeltery.getZ());
                    ps.setString(7, smeltery.getTier().name());
                    ps.setLong(8, smeltery.getCreatedAt());
                    ps.setLong(9, smeltery.getLastUsed());
                    ps.setInt(10, smeltery.getTotalItemsSmelted());
                    ps.setBoolean(11, smeltery.isActive());
                    ps.setString(12, smeltery.getName());
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar smelteries em lote", e);
            }
        });
    }

    // ==================== Mapping ====================

    private SmelteryStructure mapSmeltery(ResultSet rs) throws SQLException {
        try {
            UUID smelteryId = UUID.fromString(rs.getString("smeltery_id"));
            UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
            SmelteryTier tier = SmelteryTier.valueOf(rs.getString("tier"));

            return new SmelteryStructure(smelteryId, ownerUuid,
                    rs.getString("world_name"),
                    rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                    tier,
                    rs.getLong("created_at"), rs.getLong("last_used"),
                    rs.getInt("total_items_smelted"),
                    rs.getBoolean("active"),
                    rs.getString("name"));
        } catch (IllegalArgumentException e) {
            MidgardLogger.error("Erro ao mapear smeltery do banco: " + e.getMessage());
            return null;
        }
    }
}
