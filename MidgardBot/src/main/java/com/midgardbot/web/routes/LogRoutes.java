package com.midgardbot.web.routes;

import com.midgardbot.data.DatabaseManager;
import io.javalin.Javalin;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Rotas para o sistema de logs do painel.
 */
public class LogRoutes {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogRoutes.class);

    public static void register(Javalin app, JDA jda) {

        // GET /api/logs — lista logs com filtros opcionais
        app.get("/api/logs", ctx -> {
            try {
                String category = ctx.queryParam("category");
                String date = ctx.queryParam("date"); // formato: yyyy-MM-dd
                int limit = 100;
                try {
                    String l = ctx.queryParam("limit");
                    if (l != null) limit = Math.min(Integer.parseInt(l), 500);
                } catch (Exception ignored) {}

                StringBuilder sql = new StringBuilder(
                        "SELECT id, category, title, message, icon, target_id, target_name, target_avatar, created_at FROM midgard_logs WHERE 1=1");
                List<Object> params = new ArrayList<>();

                if (category != null && !category.isBlank()) {
                    sql.append(" AND category = ?");
                    params.add(category);
                }

                if (date != null && !date.isBlank()) {
                    // Filtrar por dia: created_at entre date 00:00:00 e date 23:59:59
                    boolean isSQLite = false;
                    try (Connection c = DatabaseManager.getConnection()) {
                        if (c != null) isSQLite = c.getMetaData().getDriverName().toLowerCase().contains("sqlite");
                    } catch (Exception ignored) {}

                    if (isSQLite) {
                        sql.append(" AND date(created_at) = ?");
                    } else {
                        sql.append(" AND DATE(created_at) = ?");
                    }
                    params.add(date);
                }

                sql.append(" ORDER BY created_at DESC LIMIT ?");
                params.add(limit);

                List<Map<String, Object>> logs = new ArrayList<>();
                try (Connection conn = DatabaseManager.getConnection()) {
                    if (conn == null) {
                        ctx.status(500).json(Map.of("error", "Banco não conectado"));
                        return;
                    }
                    try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                        for (int i = 0; i < params.size(); i++) {
                            Object p = params.get(i);
                            if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                            else ps.setString(i + 1, p.toString());
                        }
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> log = new LinkedHashMap<>();
                                log.put("id", rs.getInt("id"));
                                log.put("category", rs.getString("category"));
                                log.put("title", rs.getString("title"));
                                log.put("message", rs.getString("message"));
                                log.put("icon", rs.getString("icon"));
                                log.put("targetId", rs.getString("target_id"));
                                log.put("targetName", rs.getString("target_name"));
                                log.put("targetAvatar", rs.getString("target_avatar"));
                                log.put("createdAt", rs.getString("created_at"));
                                logs.add(log);
                            }
                        }
                    }
                }

                ctx.json(Map.of("logs", logs, "total", logs.size()));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar logs", e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar logs"));
            }
        });

        // GET /api/logs/categories — lista categorias distintas
        app.get("/api/logs/categories", ctx -> {
            try {
                List<String> categories = new ArrayList<>();
                try (Connection conn = DatabaseManager.getConnection()) {
                    if (conn == null) {
                        ctx.json(Map.of("categories", categories));
                        return;
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT DISTINCT category FROM midgard_logs ORDER BY category")) {
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                categories.add(rs.getString("category"));
                            }
                        }
                    }
                }
                ctx.json(Map.of("categories", categories));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar categorias de logs", e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar categorias"));
            }
        });
    }
}
