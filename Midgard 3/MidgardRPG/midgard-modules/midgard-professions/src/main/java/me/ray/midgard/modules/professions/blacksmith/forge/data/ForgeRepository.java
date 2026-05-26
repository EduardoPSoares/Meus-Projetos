package me.ray.midgard.modules.professions.blacksmith.forge.data;

import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeSchematic;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeTemplate;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Database repository for forge structures and player forge data.
 * Handles persistence of forges and profession progress.
 */
public class ForgeRepository {

    private final DatabaseManager databaseManager;
    private final String dbType;

    public ForgeRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.dbType = databaseManager.getDatabaseType();
        createTables();
    }

    private void createTables() {
        databaseManager.execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                // Forge structures table
                String forgesTable;
                if (dbType.equalsIgnoreCase("mysql")) {
                    forgesTable = """
                        CREATE TABLE IF NOT EXISTS midgard_forges (
                            forge_id VARCHAR(36) PRIMARY KEY,
                            owner_uuid VARCHAR(36) NOT NULL,
                            world_name VARCHAR(128) NOT NULL,
                            x INT NOT NULL,
                            y INT NOT NULL,
                            z INT NOT NULL,
                            tier VARCHAR(32) NOT NULL,
                            rotation VARCHAR(16) NOT NULL,
                            created_at BIGINT NOT NULL,
                            last_used BIGINT NOT NULL,
                            total_items_forged INT NOT NULL DEFAULT 0,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            name VARCHAR(128) DEFAULT NULL,
                            INDEX idx_forges_owner (owner_uuid),
                            INDEX idx_forges_world (world_name)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
                } else {
                    forgesTable = """
                        CREATE TABLE IF NOT EXISTS midgard_forges (
                            forge_id TEXT PRIMARY KEY,
                            owner_uuid TEXT NOT NULL,
                            world_name TEXT NOT NULL,
                            x INTEGER NOT NULL,
                            y INTEGER NOT NULL,
                            z INTEGER NOT NULL,
                            tier TEXT NOT NULL,
                            rotation TEXT NOT NULL,
                            created_at INTEGER NOT NULL,
                            last_used INTEGER NOT NULL,
                            total_items_forged INTEGER NOT NULL DEFAULT 0,
                            active INTEGER NOT NULL DEFAULT 1,
                            name TEXT DEFAULT NULL
                        )""";
                }
                stmt.executeUpdate(forgesTable);

                // Migration: add name column to existing tables
                try {
                    if (dbType.equalsIgnoreCase("mysql")) {
                        stmt.executeUpdate("ALTER TABLE midgard_forges ADD COLUMN name VARCHAR(128) DEFAULT NULL");
                    } else {
                        stmt.executeUpdate("ALTER TABLE midgard_forges ADD COLUMN name TEXT DEFAULT NULL");
                    }
                } catch (SQLException ignored) {
                    // Column already exists — expected on subsequent starts
                }

                // Player forge data table
                String dataTable;
                if (dbType.equalsIgnoreCase("mysql")) {
                    dataTable = """
                        CREATE TABLE IF NOT EXISTS midgard_forge_data (
                            player_uuid VARCHAR(36) PRIMARY KEY,
                            level INT NOT NULL DEFAULT 0,
                            xp DOUBLE NOT NULL DEFAULT 0,
                            specialization VARCHAR(64),
                            unlocked_recipes TEXT,
                            total_items_forged INT NOT NULL DEFAULT 0,
                            legendary_items_forged INT NOT NULL DEFAULT 0,
                            total_perfect_strikes INT NOT NULL DEFAULT 0,
                            total_forges_built INT NOT NULL DEFAULT 0,
                            highest_quality_score DOUBLE NOT NULL DEFAULT 0,
                            owned_forge_ids TEXT,
                            updated_at BIGINT NOT NULL DEFAULT 0
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
                } else {
                    dataTable = """
                        CREATE TABLE IF NOT EXISTS midgard_forge_data (
                            player_uuid TEXT PRIMARY KEY,
                            level INTEGER NOT NULL DEFAULT 0,
                            xp REAL NOT NULL DEFAULT 0,
                            specialization TEXT,
                            unlocked_recipes TEXT,
                            total_items_forged INTEGER NOT NULL DEFAULT 0,
                            legendary_items_forged INTEGER NOT NULL DEFAULT 0,
                            total_perfect_strikes INTEGER NOT NULL DEFAULT 0,
                            total_forges_built INTEGER NOT NULL DEFAULT 0,
                            highest_quality_score REAL NOT NULL DEFAULT 0,
                            owned_forge_ids TEXT,
                            updated_at INTEGER NOT NULL DEFAULT 0
                        )""";
                }
                stmt.executeUpdate(dataTable);

                // Schematic data table for admin-created forges
                String schematicTable;
                if (dbType.equalsIgnoreCase("mysql")) {
                    schematicTable = """
                        CREATE TABLE IF NOT EXISTS midgard_forge_schematics (
                            forge_id VARCHAR(36) PRIMARY KEY,
                            width INT NOT NULL,
                            height INT NOT NULL,
                            depth INT NOT NULL,
                            blocks_data MEDIUMTEXT NOT NULL,
                            FOREIGN KEY (forge_id) REFERENCES midgard_forges(forge_id) ON DELETE CASCADE
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
                } else {
                    schematicTable = """
                        CREATE TABLE IF NOT EXISTS midgard_forge_schematics (
                            forge_id TEXT PRIMARY KEY,
                            width INTEGER NOT NULL,
                            height INTEGER NOT NULL,
                            depth INTEGER NOT NULL,
                            blocks_data TEXT NOT NULL
                        )""";
                }
                stmt.executeUpdate(schematicTable);

                // Forge templates table (admin-created blueprints)
                String templatesTable;
                if (dbType.equalsIgnoreCase("mysql")) {
                    templatesTable = """
                        CREATE TABLE IF NOT EXISTS midgard_forge_templates (
                            template_id VARCHAR(36) PRIMARY KEY,
                            name VARCHAR(128) NOT NULL,
                            tier VARCHAR(32) NOT NULL,
                            required_level INT NOT NULL DEFAULT 1,
                            created_at BIGINT NOT NULL,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            width INT NOT NULL DEFAULT 0,
                            height INT NOT NULL DEFAULT 0,
                            depth INT NOT NULL DEFAULT 0,
                            blocks_data MEDIUMTEXT
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
                } else {
                    templatesTable = """
                        CREATE TABLE IF NOT EXISTS midgard_forge_templates (
                            template_id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            tier TEXT NOT NULL,
                            required_level INTEGER NOT NULL DEFAULT 1,
                            created_at INTEGER NOT NULL,
                            active INTEGER NOT NULL DEFAULT 1,
                            width INTEGER NOT NULL DEFAULT 0,
                            height INTEGER NOT NULL DEFAULT 0,
                            depth INTEGER NOT NULL DEFAULT 0,
                            blocks_data TEXT
                        )""";
                }
                stmt.executeUpdate(templatesTable);

            } catch (SQLException e) {
                MidgardLogger.error("Erro ao criar tabelas de forge", e);
            }
        });
    }

    // ==================== Forge Structure CRUD ====================

    /**
     * Saves a forge structure to the database.
     */
    public CompletableFuture<Void> saveForge(ForgeStructure forge) {
        return databaseManager.executeAsync(conn -> {
            String sql;
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = """
                    INSERT INTO midgard_forges (forge_id, owner_uuid, world_name, x, y, z, tier, rotation,
                        created_at, last_used, total_items_forged, active, name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE last_used = VALUES(last_used),
                        total_items_forged = VALUES(total_items_forged), active = VALUES(active), name = VALUES(name)""";
            } else {
                sql = """
                    INSERT OR REPLACE INTO midgard_forges (forge_id, owner_uuid, world_name, x, y, z, tier, rotation,
                        created_at, last_used, total_items_forged, active, name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)""";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, forge.getForgeId().toString());
                ps.setString(2, forge.getOwnerUuid().toString());
                ps.setString(3, forge.getWorldName());
                ps.setInt(4, forge.getX());
                ps.setInt(5, forge.getY());
                ps.setInt(6, forge.getZ());
                ps.setString(7, forge.getTier().name());
                ps.setString(8, forge.getRotation().name());
                ps.setLong(9, forge.getCreatedAt());
                ps.setLong(10, forge.getLastUsed());
                ps.setInt(11, forge.getTotalItemsForged());
                ps.setBoolean(12, forge.isActive());
                ps.setString(13, forge.getName());
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar forge " + forge.getForgeId(), e);
            }
        });
    }

    /**
     * Loads all forge structures from the database.
     */
    public List<ForgeStructure> loadAllForges() {
        List<ForgeStructure> forges = new ArrayList<>();
        databaseManager.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM midgard_forges WHERE active = " + (dbType.equalsIgnoreCase("mysql") ? "TRUE" : "1"));
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForgeStructure forge = mapForge(rs);
                    if (forge != null) { forges.add(forge); }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar forjas do banco", e);
            }
        });
        return forges;
    }

    /**
     * Deletes a forge from the database.
     */
    public CompletableFuture<Void> deleteForge(UUID forgeId) {
        return databaseManager.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM midgard_forges WHERE forge_id = ?")) {
                ps.setString(1, forgeId.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao deletar forge " + forgeId, e);
            }
        });
    }

    // ==================== Player Data CRUD ====================

    /**
     * Saves player forge data.
     */
    public CompletableFuture<Void> savePlayerData(UUID playerUuid, ForgeData data) {
        return databaseManager.executeAsync(conn -> {
            String sql;
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = """
                    INSERT INTO midgard_forge_data (player_uuid, level, xp, specialization, unlocked_recipes,
                        total_items_forged, legendary_items_forged, total_perfect_strikes, total_forges_built,
                        highest_quality_score, owned_forge_ids, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE level = VALUES(level), xp = VALUES(xp),
                        specialization = VALUES(specialization), unlocked_recipes = VALUES(unlocked_recipes),
                        total_items_forged = VALUES(total_items_forged), legendary_items_forged = VALUES(legendary_items_forged),
                        total_perfect_strikes = VALUES(total_perfect_strikes), total_forges_built = VALUES(total_forges_built),
                        highest_quality_score = VALUES(highest_quality_score), owned_forge_ids = VALUES(owned_forge_ids),
                        updated_at = VALUES(updated_at)""";
            } else {
                sql = """
                    INSERT OR REPLACE INTO midgard_forge_data (player_uuid, level, xp, specialization, unlocked_recipes,
                        total_items_forged, legendary_items_forged, total_perfect_strikes, total_forges_built,
                        highest_quality_score, owned_forge_ids, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setInt(2, data.getLevel());
                ps.setDouble(3, data.getXp());
                ps.setString(4, data.getSpecialization());
                ps.setString(5, String.join(",", data.getUnlockedRecipes()));
                ps.setInt(6, data.getTotalItemsForged());
                ps.setInt(7, data.getLegendaryItemsForged());
                ps.setInt(8, data.getTotalPerfectStrikes());
                ps.setInt(9, data.getTotalForgesBuilt());
                ps.setDouble(10, data.getHighestQualityScore());
                ps.setString(11, String.join(",", data.getOwnedForgeIds()));
                ps.setLong(12, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar dados de forge do jogador " + playerUuid, e);
            }
        });
    }

    /**
     * Loads player forge data.
     */
    public CompletableFuture<ForgeData> loadPlayerData(UUID playerUuid) {
        return databaseManager.executeQuery(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM midgard_forge_data WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapPlayerData(rs);
                    }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar dados de forge do jogador " + playerUuid, e);
            }
            return new ForgeData();
        });
    }

    // ==================== Schematic Data ====================

    /**
     * Saves a custom schematic for an admin-created forge.
     * Serializes blocks as pipe-delimited rows: relX,relY,relZ,MATERIAL,BLOCK_TYPE
     */
    public CompletableFuture<Void> saveSchematicData(UUID forgeId, ForgeSchematic schematic) {
        return databaseManager.executeAsync(conn -> {
            String sql;
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = """
                    INSERT INTO midgard_forge_schematics (forge_id, width, height, depth, blocks_data)
                    VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE width=VALUES(width), height=VALUES(height),
                    depth=VALUES(depth), blocks_data=VALUES(blocks_data)""";
            } else {
                sql = """
                    INSERT OR REPLACE INTO midgard_forge_schematics (forge_id, width, height, depth, blocks_data)
                    VALUES (?,?,?,?,?)""";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, forgeId.toString());
                ps.setInt(2, schematic.getWidth());
                ps.setInt(3, schematic.getHeight());
                ps.setInt(4, schematic.getDepth());
                ps.setString(5, serializeBlocks(schematic.getBlocks()));
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar schematic da forge " + forgeId, e);
            }
        });
    }

    /**
     * Loads a custom schematic for a forge. Returns null if none exists.
     */
    public ForgeSchematic loadSchematicData(UUID forgeId, ForgeTier tier) {
        final ForgeSchematic[] result = {null};
        databaseManager.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM midgard_forge_schematics WHERE forge_id = ?")) {
                ps.setString(1, forgeId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int width = rs.getInt("width");
                        int height = rs.getInt("height");
                        int depth = rs.getInt("depth");
                        String blocksData = rs.getString("blocks_data");
                        List<ForgeBlock> blocks = deserializeBlocks(blocksData);
                        if (!blocks.isEmpty()) {
                            result[0] = new ForgeSchematic(tier, width, height, depth,
                                    0, 0, 0, blocks);
                        }
                    }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar schematic da forge " + forgeId, e);
            }
        });
        return result[0];
    }

    private String serializeBlocks(List<ForgeBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        for (ForgeBlock block : blocks) {
            if (sb.length() > 0) { sb.append('|'); }
            sb.append(block.getRelX()).append(',')
              .append(block.getRelY()).append(',')
              .append(block.getRelZ()).append(',')
              .append(block.getMaterial().name()).append(',')
              .append(block.getBlockType().name());
        }
        return sb.toString();
    }

    private List<ForgeBlock> deserializeBlocks(String data) {
        List<ForgeBlock> blocks = new ArrayList<>();
        if (data == null || data.isEmpty()) { return blocks; }
        for (String entry : data.split("\\|")) {
            String[] parts = entry.split(",");
            if (parts.length < 5) { continue; }
            try {
                int relX = Integer.parseInt(parts[0]);
                int relY = Integer.parseInt(parts[1]);
                int relZ = Integer.parseInt(parts[2]);
                org.bukkit.Material material = org.bukkit.Material.valueOf(parts[3]);
                ForgeBlock.ForgeBlockType type = ForgeBlock.ForgeBlockType.valueOf(parts[4]);
                blocks.add(new ForgeBlock(relX, relY, relZ, material, type));
            } catch (IllegalArgumentException e) {
                MidgardLogger.error("Erro ao desserializar bloco de schematic: " + entry);
            }
        }
        return blocks;
    }

    // ==================== Forge Templates CRUD ====================

    public CompletableFuture<Void> saveTemplate(ForgeTemplate template) {
        return databaseManager.executeAsync(conn -> {
            String sql;
            ForgeSchematic sch = template.getSchematic();
            if (dbType.equalsIgnoreCase("mysql")) {
                sql = """
                    INSERT INTO midgard_forge_templates (template_id, name, tier, required_level,
                        created_at, active, width, height, depth, blocks_data)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    ON DUPLICATE KEY UPDATE name=VALUES(name), tier=VALUES(tier),
                        required_level=VALUES(required_level), active=VALUES(active),
                        width=VALUES(width), height=VALUES(height), depth=VALUES(depth),
                        blocks_data=VALUES(blocks_data)""";
            } else {
                sql = """
                    INSERT OR REPLACE INTO midgard_forge_templates (template_id, name, tier, required_level,
                        created_at, active, width, height, depth, blocks_data)
                    VALUES (?,?,?,?,?,?,?,?,?,?)""";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, template.getTemplateId().toString());
                ps.setString(2, template.getName());
                ps.setString(3, template.getTier().name());
                ps.setInt(4, template.getRequiredLevel());
                ps.setLong(5, template.getCreatedAt());
                ps.setBoolean(6, template.isActive());
                ps.setInt(7, sch != null ? sch.getWidth() : 0);
                ps.setInt(8, sch != null ? sch.getHeight() : 0);
                ps.setInt(9, sch != null ? sch.getDepth() : 0);
                ps.setString(10, sch != null ? serializeBlocks(sch.getBlocks()) : "");
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar template " + template.getTemplateId(), e);
            }
        });
    }

    public List<ForgeTemplate> loadAllTemplates() {
        List<ForgeTemplate> templates = new ArrayList<>();
        databaseManager.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM midgard_forge_templates");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ForgeTemplate t = mapTemplate(rs);
                    if (t != null) { templates.add(t); }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar templates de forja", e);
            }
        });
        return templates;
    }

    public CompletableFuture<Void> deleteTemplate(UUID templateId) {
        return databaseManager.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM midgard_forge_templates WHERE template_id = ?")) {
                ps.setString(1, templateId.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao deletar template " + templateId, e);
            }
        });
    }

    private ForgeTemplate mapTemplate(ResultSet rs) throws SQLException {
        try {
            UUID templateId = UUID.fromString(rs.getString("template_id"));
            ForgeTier tier = ForgeTier.valueOf(rs.getString("tier"));
            ForgeTemplate t = new ForgeTemplate(templateId,
                    rs.getString("name"), tier, rs.getInt("required_level"),
                    rs.getLong("created_at"), rs.getBoolean("active"));

            int width = rs.getInt("width");
            int height = rs.getInt("height");
            int depth = rs.getInt("depth");
            String blocksData = rs.getString("blocks_data");
            if (blocksData != null && !blocksData.isEmpty() && width > 0) {
                List<ForgeBlock> blocks = deserializeBlocks(blocksData);
                if (!blocks.isEmpty()) {
                    t.setSchematic(new ForgeSchematic(tier, width, height, depth, 0, 0, 0, blocks));
                }
            }
            return t;
        } catch (IllegalArgumentException e) {
            MidgardLogger.error("Erro ao mapear template do banco: " + e.getMessage());
            return null;
        }
    }

    // ==================== Mapping helpers ====================

    private ForgeStructure mapForge(ResultSet rs) throws SQLException {
        try {
            UUID forgeId = UUID.fromString(rs.getString("forge_id"));
            UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
            ForgeTier tier = ForgeTier.valueOf(rs.getString("tier"));
            ForgeRotation rotation = ForgeRotation.valueOf(rs.getString("rotation"));

            return new ForgeStructure(forgeId, ownerUuid,
                    rs.getString("world_name"),
                    rs.getInt("x"), rs.getInt("y"), rs.getInt("z"),
                    tier, rotation,
                    rs.getLong("created_at"), rs.getLong("last_used"),
                    rs.getInt("total_items_forged"),
                    rs.getBoolean("active"),
                    rs.getString("name"));
        } catch (IllegalArgumentException e) {
            MidgardLogger.error("Erro ao mapear forge do banco: " + e.getMessage());
            return null;
        }
    }

    private ForgeData mapPlayerData(ResultSet rs) throws SQLException {
        ForgeData data = new ForgeData();
        data.setLevel(rs.getInt("level"));
        data.setXp(rs.getDouble("xp"));
        data.setSpecialization(rs.getString("specialization"));

        String recipes = rs.getString("unlocked_recipes");
        if (recipes != null && !recipes.isEmpty()) {
            data.setUnlockedRecipes(Arrays.asList(recipes.split(",")));
        }

        data.setTotalItemsForged(rs.getInt("total_items_forged"));
        data.setLegendaryItemsForged(rs.getInt("legendary_items_forged"));
        data.setTotalPerfectStrikes(rs.getInt("total_perfect_strikes"));
        data.setTotalForgesBuilt(rs.getInt("total_forges_built"));
        data.setHighestQualityScore(rs.getDouble("highest_quality_score"));

        String forgeIds = rs.getString("owned_forge_ids");
        if (forgeIds != null && !forgeIds.isEmpty()) {
            data.setOwnedForgeIds(Arrays.asList(forgeIds.split(",")));
        }

        return data;
    }
}
