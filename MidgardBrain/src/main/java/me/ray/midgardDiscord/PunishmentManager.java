package me.ray.midgardDiscord;

import org.slf4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PunishmentManager {
    private final Logger logger;
    private final DatabaseManager dbManager;
    
    // Cache for TabCompletion
    private final java.util.Set<String> bannedPlayersCache = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<String> bannedIpsCache = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public enum PunishmentType {
        BAN, IP_BAN, TEMP_BAN, MUTE, TEMP_MUTE, KICK, WARN, WARN_LOW, WARN_MEDIUM, WARN_HIGH
    }

    public static class Punishment {
        public int id;
        public String targetIdentifier;
        public String targetName;
        public String targetDiscordId;
        public PunishmentType type;
        public String reason;
        public String moderatorIdentifier;
        public String moderatorName;
        public long startTime;
        public long endTime;
        public boolean active;
        public String removedBy;
        public String removedReason;
        public long removedAt;

        public boolean isExpired() {
            if (!active) return true;
            if (endTime != -1 && System.currentTimeMillis() > endTime) return true;
            return false;
        }
    }

    public PunishmentManager(Logger logger, File botDataFolder, DatabaseManager dbManager) {
        this.logger = logger;
        this.dbManager = dbManager;
    }

    public void updateCache() {
        if (dbManager == null || !dbManager.isConnected()) return;
        
        // Fetch Banned Players
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT target_name FROM midgard_punishments WHERE type = 'BAN' AND active = 1 ORDER BY id DESC LIMIT 50")) {
             try (ResultSet rs = stmt.executeQuery()) {
                 bannedPlayersCache.clear();
                 while (rs.next()) {
                     String name = rs.getString("target_name");
                     if (name != null && !name.isEmpty()) bannedPlayersCache.add(name);
                 }
             }
        } catch (SQLException e) {
            logger.error("Error updating ban cache", e);
        }

        // Fetch Banned IPs
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT target_identifier FROM midgard_punishments WHERE type = 'IP_BAN' AND active = 1 ORDER BY id DESC LIMIT 50")) {
             try (ResultSet rs = stmt.executeQuery()) {
                 bannedIpsCache.clear();
                 while (rs.next()) {
                     String ip = rs.getString("target_identifier");
                     if (ip != null && !ip.isEmpty()) bannedIpsCache.add(ip);
                 }
             }
        } catch (SQLException e) {
            logger.error("Error updating ip-ban cache", e);
        }
    }

    public java.util.Set<String> getBannedPlayersCache() {
        return bannedPlayersCache;
    }

    public java.util.Set<String> getBannedIpsCache() {
        return bannedIpsCache;
    }

    public void createPunishment(String targetIdentifier, String targetName, String targetDiscordId, PunishmentType type, String reason, String moderatorIdentifier, String moderatorName, long endTime) {
        if (dbManager == null || !dbManager.isConnected()) return;

        String sql = "INSERT INTO midgard_punishments (target_identifier, target_name, target_discord_id, type, reason, moderator_identifier, moderator_name, start_time, end_time, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetIdentifier);
            stmt.setString(2, targetName);
            stmt.setString(3, targetDiscordId);
            stmt.setString(4, type.name());
            stmt.setString(5, reason);
            stmt.setString(6, moderatorIdentifier);
            stmt.setString(7, moderatorName);
            stmt.setLong(8, System.currentTimeMillis());
            stmt.setLong(9, endTime);
            
            stmt.executeUpdate();
            logger.info("MEMBER PUNISHED: " + type + " -> " + targetName + " (" + targetIdentifier + ")");
            
            // Update local cache if applicable
            if (type == PunishmentType.BAN && targetName != null) bannedPlayersCache.add(targetName);
            if (type == PunishmentType.IP_BAN) bannedIpsCache.add(targetIdentifier);
            
        } catch (SQLException e) {
            logger.error("Error creating punishment in DB: ", e);
        }
    }

    public void revokePunishment(String targetIdentifier, PunishmentType type, String removedBy, String removedReason) {
        if (dbManager == null || !dbManager.isConnected()) return;

        String sql = "UPDATE midgard_punishments SET active = 0, removed_by = ?, removed_reason = ?, removed_at = ? WHERE target_identifier = ? AND type = ? AND active = 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, removedBy);
            stmt.setString(2, removedReason);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setString(4, targetIdentifier);
            stmt.setString(5, type.name());
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                 // Atualiza cache inteiro de forma segura (cache armazena nomes, revoke usa UUID)
                 updateCache();
            }
        } catch (SQLException e) {
            logger.error("Error revoking punishment: ", e);
        }
    }

    public Punishment getActivePunishment(String targetIdentifier, PunishmentType type) {
        if (dbManager == null || !dbManager.isConnected()) return null;

        String sql = "SELECT * FROM midgard_punishments WHERE target_identifier = ? AND type = ? AND active = 1 ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetIdentifier);
            stmt.setString(2, type.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Punishment p = mapResultSetToPunishment(rs);
                    if (p.isExpired()) {
                        revokePunishment(targetIdentifier, type, "SYSTEM", "Expired");
                        return null;
                    }
                    return p;
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching active punishment: ", e);
        }
        return null;
    }
    
    public Punishment getActiveBan(String uuid, String ip, String discordId) {
        if (dbManager == null || !dbManager.isConnected()) return null;

        Punishment p = getActivePunishment(uuid, PunishmentType.BAN);
        if (p != null) return p;
        p = getActivePunishment(uuid, PunishmentType.TEMP_BAN);
        if (p != null) return p;

        if (ip != null) {
            p = getActivePunishment(ip, PunishmentType.IP_BAN);
            if (p != null) return p;
        }

        if (discordId != null) {
            p = getActivePunishment(discordId, PunishmentType.BAN);
            if (p != null) return p;
        }

        return null;
    }

    private Punishment mapResultSetToPunishment(ResultSet rs) throws SQLException {
        Punishment p = new Punishment();
        p.id = rs.getInt("id");
        p.targetIdentifier = rs.getString("target_identifier");
        p.targetName = rs.getString("target_name");
        p.targetDiscordId = rs.getString("target_discord_id");
        p.type = PunishmentType.valueOf(rs.getString("type"));
        p.reason = rs.getString("reason");
        p.moderatorIdentifier = rs.getString("moderator_identifier");
        p.moderatorName = rs.getString("moderator_name");
        p.startTime = rs.getLong("start_time");
        p.endTime = rs.getLong("end_time");
        p.active = rs.getBoolean("active");
        return p;
    }
    
    // Legacy support for Ban class and getBan method
    public static class Ban {
        public String userId;
        public String moderatorId;
        public String reason;
        public long timestamp;

        public Ban(String userId, String moderatorId, String reason) {
            this.userId = userId;
            this.moderatorId = moderatorId;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public Ban getBan(String discordId) {
        Punishment p = getActivePunishment(discordId, PunishmentType.BAN);
        if (p != null) return new Ban(discordId, "DB", p.reason);
        return null;
    }
}
