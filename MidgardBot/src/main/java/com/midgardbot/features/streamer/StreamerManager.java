package com.midgardbot.features.streamer;

import com.midgardbot.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StreamerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamerManager.class);

    public static void addStreamer(String userId, String platform, String channelName) {
        String sql = "INSERT INTO midgard_streamers (user_id, platform, channel_name) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, platform.toLowerCase());
            ps.setString(3, channelName);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao adicionar streamer", e);
        }
    }

    public static void removeStreamer(String userId) {
        String sql = "DELETE FROM midgard_streamers WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao remover streamer", e);
        }
    }

    public static List<Streamer> getStreamers() {
        List<Streamer> streamers = new ArrayList<>();
        String sql = "SELECT * FROM midgard_streamers";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                streamers.add(new Streamer(
                    rs.getInt("id"),
                    rs.getString("user_id"),
                    rs.getString("platform"),
                    rs.getString("channel_name"),
                    rs.getBoolean("last_status"),
                    rs.getTimestamp("last_check").getTime()
                ));
            }
        } catch (SQLException e) {
            LOGGER.error("Erro ao buscar streamers", e);
        }
        return streamers;
    }

    public static void updateStatus(int id, boolean isLive) {
        String sql = "UPDATE midgard_streamers SET last_status = ?, last_check = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isLive);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Erro ao atualizar status do streamer", e);
        }
    }
}
