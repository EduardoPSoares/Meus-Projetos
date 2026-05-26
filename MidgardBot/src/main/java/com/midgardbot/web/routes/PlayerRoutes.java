package com.midgardbot.web.routes;

import com.midgardbot.data.DataManager;
import com.midgardbot.data.DatabaseManager;
import com.midgardbot.web.auth.AuthController;
import io.javalin.Javalin;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Rotas de busca e perfil de jogadores.
 */
public class PlayerRoutes {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRoutes.class);

    public static void register(Javalin app, JDA jda) {

        // GET /api/players/search?q=... — busca jogadores
        app.get("/api/players/search", ctx -> {
            String query = com.midgardbot.web.security.SecurityMiddleware.sanitizeQuery(ctx.queryParam("q"));
            if (query == null || query.length() < 2) {
                ctx.json(Map.of("players", List.of()));
                return;
            }

            Guild guild = AuthController.getMainGuild(jda);
            String lowerQuery = query.toLowerCase();
            Set<String> addedIds = new HashSet<>();
            List<Map<String, Object>> results = new ArrayList<>();

            // 1) Buscar por nick do Minecraft na whitelist (DB)
            try (Connection conn = DatabaseManager.getConnection()) {
                if (conn != null) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT discord_id, nickname FROM midgard_whitelist WHERE LOWER(nickname) LIKE ? ESCAPE '\\' LIMIT 20")) {
                        String escapedQuery = lowerQuery.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                        stmt.setString(1, "%" + escapedQuery + "%");
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                String discordId = rs.getString("discord_id");
                                String nickname = rs.getString("nickname");
                                if (!addedIds.add(discordId)) continue;

                                Map<String, Object> player = new LinkedHashMap<>();
                                player.put("discordId", discordId);
                                player.put("nickname", nickname);

                                Member m = com.midgardbot.web.security.SecurityMiddleware.isValidDiscordId(discordId)
                                        ? guild.getMemberById(discordId) : null;
                                if (m != null) {
                                    player.put("name", m.getUser().getName());
                                    player.put("displayName", m.getEffectiveName());
                                    player.put("avatar", m.getUser().getAvatarUrl());
                                } else {
                                    player.put("name", nickname);
                                    player.put("displayName", nickname);
                                }
                                results.add(player);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[WEB] Erro ao buscar por nick minecraft", e);
            }

            // 2) Buscar por nome/apelido do Discord no cache
            guild.getMembers().stream()
                    .filter(m -> m.getUser().getName().toLowerCase().contains(lowerQuery)
                            || m.getEffectiveName().toLowerCase().contains(lowerQuery))
                    .limit(20)
                    .forEach(m -> {
                        if (!addedIds.add(m.getId())) return;
                        Map<String, Object> player = new LinkedHashMap<>();
                        player.put("discordId", m.getId());
                        player.put("name", m.getUser().getName());
                        player.put("displayName", m.getEffectiveName());
                        player.put("avatar", m.getUser().getAvatarUrl());
                        results.add(player);
                    });

            // 3) Fallback REST se nenhum resultado
            if (results.isEmpty()) {
                try {
                    for (Member m : guild.retrieveMembersByPrefix(query, 20).get()) {
                        if (!addedIds.add(m.getId())) continue;
                        Map<String, Object> player = new LinkedHashMap<>();
                        player.put("discordId", m.getId());
                        player.put("name", m.getUser().getName());
                        player.put("displayName", m.getEffectiveName());
                        player.put("avatar", m.getUser().getAvatarUrl());
                        results.add(player);
                    }
                } catch (Exception e) {
                    LOGGER.warn("[WEB] Falha na busca REST por prefixo", e);
                }
            }

            ctx.json(Map.of("players", results));
        });

        // GET /api/players/:id — perfil completo de um jogador
        app.get("/api/players/{id}", ctx -> {
            String discordId = ctx.pathParam("id");
            if (!com.midgardbot.web.security.SecurityMiddleware.isValidDiscordId(discordId)) {
                ctx.status(400).json(Map.of("error", "ID inválido"));
                return;
            }

            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("discordId", discordId);

            // Dados Discord
            try {
                var guild = AuthController.getMainGuild(jda);
                var member = guild.getMemberById(discordId);
                if (member != null) {
                    profile.put("name", member.getUser().getName());
                    profile.put("displayName", member.getEffectiveName());
                    profile.put("avatar", member.getUser().getAvatarUrl());
                    profile.put("joinedAt", member.getTimeJoined().toString());
                    profile.put("roles", member.getRoles().stream()
                            .map(r -> Map.of("id", r.getId(), "name", r.getName(), "color", String.format("#%06x", r.getColorRaw())))
                            .toList());
                }
            } catch (Exception ignored) {}

            // Whitelist
            var whitelistInfo = DataManager.getStatus(discordId);
            if (whitelistInfo != null) {
                profile.put("whitelistStatus", whitelistInfo.status != null ? whitelistInfo.status.name().toLowerCase() : "none");
                profile.put("whitelistNickname", whitelistInfo.nickname);
                profile.put("whitelistReason", whitelistInfo.reason);
            } else {
                profile.put("whitelistStatus", "none");
            }

            // Histórico de whitelist
            var history = DataManager.getHistory(discordId);
            List<Map<String, Object>> historyList = new ArrayList<>();
            if (history != null) {
                for (var entry : history) {
                    Map<String, Object> h = new LinkedHashMap<>();
                    h.put("action", entry.action);
                    h.put("details", entry.details);
                    h.put("staffName", entry.staffName);
                    h.put("timestamp", entry.timestamp);
                    historyList.add(h);
                }
            }
            profile.put("whitelistHistory", historyList);

            // Punições
            List<Map<String, Object>> punishments = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection()) {
                if (conn != null) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT * FROM midgard_punishments WHERE target_discord_id = ? ORDER BY start_time DESC")) {
                        stmt.setString(1, discordId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> p = new LinkedHashMap<>();
                                p.put("id", rs.getInt("id"));
                                p.put("type", rs.getString("type"));
                                p.put("reason", rs.getString("reason"));
                                p.put("moderatorId", rs.getString("moderator_identifier"));
                                p.put("moderatorName", rs.getString("moderator_name"));
                                p.put("startTime", rs.getLong("start_time"));
                                p.put("endTime", rs.getObject("end_time"));
                                p.put("active", rs.getBoolean("active"));
                                p.put("removedBy", rs.getString("removed_by"));
                                punishments.add(p);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar punições do jogador", e);
            }
            profile.put("punishments", punishments);

            // Moderação flags
            profile.put("isBlacklisted", DataManager.isBlacklisted(discordId));
            profile.put("isFlagged", DataManager.isFlagged(discordId));

            ctx.json(profile);
        });
    }
}
