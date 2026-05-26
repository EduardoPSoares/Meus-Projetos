package com.midgardbot.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PunishmentManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentManager.class);

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

        public boolean isExpired() {
            if (!active) return true;
            if (endTime != -1 && System.currentTimeMillis() > endTime) return true;
            return false;
        }
    }

    // --- Legacy Classes for Compatibility ---
    public static class Warn {
        public String id;
        public String userId;
        public String moderatorId;
        public String reason;
        public long timestamp;

        public Warn(Punishment p) {
            this.id = String.valueOf(p.id);
            this.userId = p.targetDiscordId != null ? p.targetDiscordId : p.targetIdentifier;
            this.moderatorId = p.moderatorIdentifier;
            this.reason = p.reason;
            this.timestamp = p.startTime;
        }
    }

    public static class TempBan {
        public String userId;
        public String moderatorId;
        public String reason;
        public long startTime;
        public long endTime;

        public TempBan(Punishment p) {
            this.userId = p.targetDiscordId != null ? p.targetDiscordId : p.targetIdentifier;
            this.moderatorId = p.moderatorIdentifier;
            this.reason = p.reason;
            this.startTime = p.startTime;
            this.endTime = p.endTime;
        }
    }

    public static class Ban {
        public String userId;
        public String moderatorId;
        public String reason;
        public long timestamp;

        public Ban(Punishment p) {
            this.userId = p.targetDiscordId != null ? p.targetDiscordId : p.targetIdentifier;
            this.moderatorId = p.moderatorIdentifier;
            this.reason = p.reason;
            this.timestamp = p.startTime;
        }
    }

    // --- Warns ---

    public static void addWarn(String userId, String moderatorId, String reason) {
        createPunishment(userId, null, userId, PunishmentType.WARN, reason, moderatorId, "Staff", -1);
    }

    public static boolean removeWarn(String userId, String warnId) {
        try {
            int id = Integer.parseInt(warnId);
            revokePunishmentById(id, "Console", "Removed via command");
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static List<Warn> getWarns(String userId) {
        List<Punishment> list = getPunishments(userId, PunishmentType.WARN);
        List<Warn> result = new ArrayList<>();
        for (Punishment p : list) {
            result.add(new Warn(p));
        }
        return result;
    }

    public static void clearWarns(String userId) {
        List<Punishment> list = getPunishments(userId, PunishmentType.WARN);
        for (Punishment p : list) {
            revokePunishmentById(p.id, "Console", "Clear Warns");
        }
    }

    // --- TempBans ---

    public static void addTempBan(String userId, String moderatorId, String reason, long durationMillis) {
        createPunishment(userId, null, userId, PunishmentType.TEMP_BAN, reason, moderatorId, "Staff", System.currentTimeMillis() + durationMillis);
    }

    public static TempBan getTempBan(String userId) {
        Punishment p = getActivePunishment(userId, PunishmentType.TEMP_BAN);
        if (p != null) return new TempBan(p);
        return null;
    }

    public static void removeTempBan(String userId) {
        revokePunishment(userId, PunishmentType.TEMP_BAN, "Console", "Unban");
    }

    public static Map<String, TempBan> getActiveTempBans() {
        Map<String, TempBan> map = new HashMap<>();
        // Inefficient for large DBs, implying this method is used for periodic checks?
        // Better to just rely on DB checks. But for compatibility:
        List<Punishment> list = getAllActivePunishments(PunishmentType.TEMP_BAN);
        for (Punishment p : list) {
            map.put(p.targetDiscordId, new TempBan(p));
        }
        return map;
    }

    // --- Bans ---

    public static void addBan(String userId, String moderatorId, String reason) {
        createPunishment(userId, null, userId, PunishmentType.BAN, reason, moderatorId, "Staff", -1);
    }

    public static Ban getBan(String userId) {
        Punishment p = getActivePunishment(userId, PunishmentType.BAN);
        if (p != null) return new Ban(p);
        return null;
    }

    public static void removeBan(String userId) {
        revokePunishment(userId, PunishmentType.BAN, "Console", "Unban");
    }

    public static Map<String, Ban> getActiveBans() {
        Map<String, Ban> map = new HashMap<>();
        List<Punishment> list = getAllActivePunishments(PunishmentType.BAN);
        for (Punishment p : list) {
            map.put(p.targetDiscordId, new Ban(p));
        }
        return map;
    }
    
    // Check if banned
    public static boolean isBanned(String userId) {
        return getBan(userId) != null;
    }

    // --- Database Helpers ---

    public static Punishment createPunishment(String targetIdentifier, String targetName, String targetDiscordId, PunishmentType type, String reason, String moderatorIdentifier, String moderatorName, long endTime) {
        Punishment p = new Punishment();
        p.targetIdentifier = targetIdentifier;
        p.targetName = targetName;
        p.targetDiscordId = targetDiscordId;
        p.type = type;
        p.reason = reason;
        p.moderatorIdentifier = moderatorIdentifier;
        p.moderatorName = moderatorName;
        p.startTime = System.currentTimeMillis();
        p.endTime = endTime;
        p.active = true;

        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return null;
            String sql = "INSERT INTO midgard_punishments (target_identifier, target_name, target_discord_id, type, reason, moderator_identifier, moderator_name, start_time, end_time, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, targetIdentifier);
                stmt.setString(2, targetName);
                stmt.setString(3, targetDiscordId);
                stmt.setString(4, type.name());
                stmt.setString(5, reason);
                stmt.setString(6, moderatorIdentifier);
                stmt.setString(7, moderatorName);
                stmt.setLong(8, p.startTime);
                stmt.setLong(9, endTime);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.id = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error creating punishment", e);
            return null;
        }
        return p;
    }

    private static void revokePunishment(String targetIdentifier, PunishmentType type, String removedBy, String removedReason) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return;
            String sql = "UPDATE midgard_punishments SET active = 0, removed_by = ?, removed_reason = ?, removed_at = ? WHERE (target_identifier = ? OR target_discord_id = ?) AND type = ? AND active = 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, removedBy);
                stmt.setString(2, removedReason);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.setString(4, targetIdentifier);
                stmt.setString(5, targetIdentifier);
                stmt.setString(6, type.name());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Error revoking punishment", e);
        }
    }
    
    private static void revokePunishmentById(int id, String removedBy, String removedReason) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return;
            String sql = "UPDATE midgard_punishments SET active = 0, removed_by = ?, removed_reason = ?, removed_at = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, removedBy);
                stmt.setString(2, removedReason);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.setInt(4, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("Error revoking punishment by id", e);
        }
    }

    private static Punishment getActivePunishment(String targetIdentifier, PunishmentType type) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return null;
            String sql = "SELECT * FROM midgard_punishments WHERE (target_identifier = ? OR target_discord_id = ?) AND type = ? AND active = 1 ORDER BY id DESC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, targetIdentifier);
                stmt.setString(2, targetIdentifier);
                stmt.setString(3, type.name());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Punishment p = mapResultSetToPunishment(rs);
                        if (p.isExpired()) {
                            revokePunishmentById(p.id, "System", "Expired");
                            return null;
                        }
                        return p;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error getting active punishment", e);
        }
        return null;
    }

    private static List<Punishment> getPunishments(String targetIdentifier, PunishmentType type) {
        List<Punishment> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return list;
            String sql = "SELECT * FROM midgard_punishments WHERE (target_identifier = ? OR target_discord_id = ?) AND type = ? ORDER BY id DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, targetIdentifier);
                stmt.setString(2, targetIdentifier);
                stmt.setString(3, type.name());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToPunishment(rs));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error getting punishments", e);
        }
        return list;
    }
    
    private static List<Punishment> getAllActivePunishments(PunishmentType type) {
        List<Punishment> list = new ArrayList<>();
        List<Integer> expiredIds = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return list;
            String sql = "SELECT * FROM midgard_punishments WHERE type = ? AND active = 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, type.name());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Punishment p = mapResultSetToPunishment(rs);
                        if (p.isExpired()) {
                            expiredIds.add(p.id);
                            continue;
                        }
                        list.add(p);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error getting all punishments", e);
        }
        // Revoke expired punishments outside the query connection to avoid deadlock
        for (int id : expiredIds) {
            revokePunishmentById(id, "System", "Expired");
        }
        return list;
    }

    private static Punishment mapResultSetToPunishment(ResultSet rs) throws SQLException {
        Punishment p = new Punishment();
        p.id = rs.getInt("id");
        p.targetIdentifier = rs.getString("target_identifier");
        p.targetName = rs.getString("target_name");
        p.targetDiscordId = rs.getString("target_discord_id");
        try {
            p.type = PunishmentType.valueOf(rs.getString("type"));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unknown punishment type '{}' for id {}, defaulting to WARN", rs.getString("type"), p.id);
            p.type = PunishmentType.WARN;
        }
        p.reason = rs.getString("reason");
        p.moderatorIdentifier = rs.getString("moderator_identifier");
        p.moderatorName = rs.getString("moderator_name");
        p.startTime = rs.getLong("start_time");
        p.endTime = rs.getLong("end_time");
        p.active = rs.getBoolean("active");
        return p;
    }
}