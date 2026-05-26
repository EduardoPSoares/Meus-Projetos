package com.midgardbot.web.routes;

import com.midgardbot.web.presence.PresenceManager;
import com.midgardbot.web.presence.PresenceManager.OnlineUser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.javalin.Javalin;
import net.dv8tion.jda.api.JDA;

import java.util.List;
import java.util.Map;

/**
 * Rotas de presença — heartbeat e lista de quem está online no painel.
 */
public class PresenceRoutes {

    private static final Gson GSON = new Gson();

    public static void register(Javalin app, JDA jda) {

        // Heartbeat — o frontend envia a cada 30s
        app.post("/api/presence/heartbeat", ctx -> {
            String userId = ctx.attribute("userId");
            String username = ctx.attribute("username");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "Não autenticado"));
                return;
            }

            String avatarUrl = null;
            String currentPage = null;

            try {
                JsonObject body = GSON.fromJson(ctx.body(), JsonObject.class);
                if (body != null) {
                    if (body.has("avatarUrl") && !body.get("avatarUrl").isJsonNull()) {
                        String rawUrl = body.get("avatarUrl").getAsString();
                        // Só aceita URLs do CDN do Discord para prevenir XSS/SSRF
                        if (rawUrl.startsWith("https://cdn.discordapp.com/")) {
                            avatarUrl = rawUrl;
                        }
                    }
                    if (body.has("currentPage") && !body.get("currentPage").isJsonNull()) {
                        currentPage = body.get("currentPage").getAsString();
                        // Sanitiza — só aceita paths que comecem com /
                        if (currentPage != null && !currentPage.startsWith("/")) {
                            currentPage = null;
                        }
                        if (currentPage != null && currentPage.length() > 50) {
                            currentPage = currentPage.substring(0, 50);
                        }
                    }
                }
            } catch (Exception ignored) {
                // Body vazio ou inválido — segue sem avatar/page
            }

            PresenceManager.heartbeat(userId, username, avatarUrl, currentPage);
            ctx.json(Map.of("ok", true));
        });

        // Lista de quem está online
        app.get("/api/presence/online", ctx -> {
            List<OnlineUser> users = PresenceManager.getOnlineUsers();

            List<Map<String, String>> result = users.stream().map(u -> {
                var map = new java.util.LinkedHashMap<String, String>();
                map.put("userId", u.userId());
                map.put("username", u.username());
                map.put("avatarUrl", u.avatarUrl());
                map.put("currentPage", u.currentPage());
                return (Map<String, String>) map;
            }).toList();

            ctx.json(Map.of("online", result));
        });
    }
}
