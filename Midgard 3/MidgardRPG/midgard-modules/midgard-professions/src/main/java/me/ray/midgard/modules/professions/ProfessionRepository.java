package me.ray.midgard.modules.professions;

import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.debug.MidgardLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Repositório de banco de dados para dados de profissões dos jogadores.
 * Tabela: midgard_professions (player_uuid, profession, level, xp, updated_at)
 * Tabela: midgard_profession_active (player_uuid, profession)
 */
public class ProfessionRepository {

    private final DatabaseManager databaseManager;
    private final String dbType;

    public ProfessionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.dbType = databaseManager.getDatabaseType();
        createTable();
        createActiveTable();
    }

    private void createTable() {
        databaseManager.execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                String sql;
                if (dbType.equalsIgnoreCase("mysql")) {
                    sql = """
                        CREATE TABLE IF NOT EXISTS midgard_professions (
                            player_uuid VARCHAR(36) NOT NULL,
                            profession VARCHAR(32) NOT NULL,
                            level INT NOT NULL DEFAULT 0,
                            xp DOUBLE NOT NULL DEFAULT 0,
                            updated_at BIGINT NOT NULL DEFAULT 0,
                            PRIMARY KEY (player_uuid, profession),
                            INDEX idx_prof_player (player_uuid)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
                } else {
                    sql = """
                        CREATE TABLE IF NOT EXISTS midgard_professions (
                            player_uuid TEXT NOT NULL,
                            profession TEXT NOT NULL,
                            level INTEGER NOT NULL DEFAULT 0,
                            xp REAL NOT NULL DEFAULT 0,
                            updated_at INTEGER NOT NULL DEFAULT 0,
                            PRIMARY KEY (player_uuid, profession)
                        )""";
                }
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao criar tabela midgard_professions", e);
            }
        });
    }

    private void createActiveTable() {
        databaseManager.execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                String sql;
                if (dbType.equalsIgnoreCase("mysql")) {
                    sql = """
                        CREATE TABLE IF NOT EXISTS midgard_profession_active (
                            player_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                            profession VARCHAR(32) NOT NULL
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";
                } else {
                    sql = """
                        CREATE TABLE IF NOT EXISTS midgard_profession_active (
                            player_uuid TEXT NOT NULL PRIMARY KEY,
                            profession TEXT NOT NULL
                        )""";
                }
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao criar tabela midgard_profession_active", e);
            }
        });
    }

    // ==========================================
    // Profissão Ativa
    // ==========================================

    /**
     * Carrega a profissão ativa de um jogador (sync — startup/login).
     */
    public Optional<ProfessionType> loadActiveProfession(UUID playerUuid) {
        Optional<ProfessionType>[] result = new Optional[]{ Optional.empty() };
        databaseManager.execute(conn -> {
            try (var ps = conn.prepareStatement(
                    "SELECT profession FROM midgard_profession_active WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        result[0] = ProfessionType.fromId(rs.getString("profession"));
                    }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar profissão ativa do jogador %s", playerUuid, e);
            }
        });
        return result[0];
    }

    /**
     * Salva a profissão ativa de um jogador (async).
     */
    public CompletableFuture<Void> saveActiveProfession(UUID playerUuid, ProfessionType type) {
        return databaseManager.executeAsync(conn -> {
            try {
                String sql;
                if (dbType.equalsIgnoreCase("mysql")) {
                    sql = """
                        INSERT INTO midgard_profession_active (player_uuid, profession)
                        VALUES (?, ?)
                        ON DUPLICATE KEY UPDATE profession = VALUES(profession)""";
                } else {
                    sql = """
                        INSERT OR REPLACE INTO midgard_profession_active (player_uuid, profession)
                        VALUES (?, ?)""";
                }
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, type.getId());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar profissão ativa %s do jogador %s", type.getId(), playerUuid, e);
            }
        });
    }

    /**
     * Carrega todas as profissões de um jogador (sync — usar no login ou startup).
     */
    public Map<ProfessionType, ProfessionProgress> loadPlayer(UUID playerUuid) {
        Map<ProfessionType, ProfessionProgress> result = new EnumMap<>(ProfessionType.class);
        databaseManager.execute(conn -> {
            try (var ps = conn.prepareStatement(
                    "SELECT profession, level, xp FROM midgard_professions WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String profId = rs.getString("profession");
                        ProfessionType.fromId(profId).ifPresent(type -> {
                            try {
                                int level = rs.getInt("level");
                                double xp = rs.getDouble("xp");
                                result.put(type, new ProfessionProgress(type, level, xp));
                            } catch (SQLException e) {
                                MidgardLogger.error("Erro ao ler profissão '%s' do jogador %s", profId, playerUuid, e);
                            }
                        });
                    }
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao carregar profissões do jogador %s", playerUuid, e);
            }
        });
        return result;
    }

    /**
     * Salva o progresso de uma profissão (async).
     */
    public CompletableFuture<Void> saveProgress(UUID playerUuid, ProfessionProgress progress) {
        return databaseManager.executeAsync(conn -> {
            try {
                String sql;
                if (dbType.equalsIgnoreCase("mysql")) {
                    sql = """
                        INSERT INTO midgard_professions (player_uuid, profession, level, xp, updated_at)
                        VALUES (?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE level = VALUES(level), xp = VALUES(xp), updated_at = VALUES(updated_at)""";
                } else {
                    sql = """
                        INSERT OR REPLACE INTO midgard_professions (player_uuid, profession, level, xp, updated_at)
                        VALUES (?, ?, ?, ?, ?)""";
                }
                try (var ps = conn.prepareStatement(sql)) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, progress.getType().getId());
                    ps.setInt(3, progress.getLevel());
                    ps.setDouble(4, progress.getXp());
                    ps.setLong(5, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar profissão %s do jogador %s", progress.getType().getId(), playerUuid, e);
            }
        });
    }

    /**
     * Salva todas as profissões de um jogador (async).
     */
    public CompletableFuture<Void> saveAll(UUID playerUuid, Map<ProfessionType, ProfessionProgress> professions) {
        return databaseManager.executeAsync(conn -> {
            try {
                String sql;
                if (dbType.equalsIgnoreCase("mysql")) {
                    sql = """
                        INSERT INTO midgard_professions (player_uuid, profession, level, xp, updated_at)
                        VALUES (?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE level = VALUES(level), xp = VALUES(xp), updated_at = VALUES(updated_at)""";
                } else {
                    sql = """
                        INSERT OR REPLACE INTO midgard_professions (player_uuid, profession, level, xp, updated_at)
                        VALUES (?, ?, ?, ?, ?)""";
                }
                long now = System.currentTimeMillis();
                try (var ps = conn.prepareStatement(sql)) {
                    for (var entry : professions.entrySet()) {
                        ProfessionProgress progress = entry.getValue();
                        ps.setString(1, playerUuid.toString());
                        ps.setString(2, progress.getType().getId());
                        ps.setInt(3, progress.getLevel());
                        ps.setDouble(4, progress.getXp());
                        ps.setLong(5, now);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                MidgardLogger.error("Erro ao salvar profissões do jogador %s", playerUuid, e);
            }
        });
    }
}
