package com.midgardbot.web.routes;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DatabaseManager;
import com.midgardbot.web.auth.AuthController;
import io.javalin.Javalin;
import io.javalin.http.Context;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.time.format.DateTimeFormatter;

/**
 * Rotas de gerenciamento de Tickets.
 */
public class TicketRoutes {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketRoutes.class);

    private static Set<String> getOpenCategoryIds() {
        Set<String> ids = new HashSet<>();
        String[] getters = {
            BotConfig.getTicketCategorySupport(),
            BotConfig.getTicketCategoryReport(),
            BotConfig.getTicketCategoryBug(),
            BotConfig.getTicketCategoryLore()
        };
        for (String id : getters) {
            if (id != null && !id.isEmpty()) ids.add(id);
        }
        return ids;
    }

    private static boolean isTicketChannel(TextChannel ch, Set<String> openCategoryIds) {
        String parentId = ch.getParentCategoryId();
        if (parentId == null || !openCategoryIds.contains(parentId)) return false;
        // Verifica pelo topic (TicketID:) — cobre tickets renomeados, com prioridade e assumidos
        String topic = ch.getTopic();
        if (topic != null && topic.contains("TicketID:")) return true;
        // Fallback: nome do canal (qualquer prefixo de emoji antes de "ticket-")
        String name = ch.getName();
        return name.contains("ticket-");
    }

    /**
     * Extrai nome legível do channel_name do ticket.
     * Ex: "ticket-raymonster-0042" → "raymonster"
     */
    private static String extractNameFromChannel(String channelName) {
        if (channelName == null) return null;
        String name = channelName;
        // Remove qualquer prefixo de emoji (🔴-, 🟡-, etc.)
        while (name.length() > 2 && !name.startsWith("ticket-")) {
            int dashIdx = name.indexOf('-');
            if (dashIdx > 0 && dashIdx < 5) {
                name = name.substring(dashIdx + 1);
            } else {
                break;
            }
        }
        if (name.startsWith("ticket-")) name = name.substring(7);
        int lastDash = name.lastIndexOf('-');
        if (lastDash > 0 && name.substring(lastDash + 1).matches("\\d+")) {
            name = name.substring(0, lastDash);
        }
        return name.isEmpty() ? null : name;
    }

    /** Resolve nome/avatar usando apenas o cache do guild (sem chamadas REST). */
    private static void resolveUserFast(Map<String, Object> t, String userId, String channelName, Guild guild) {
        if (userId != null && !userId.isEmpty()) {
            try {
                var member = guild.getMemberById(userId);
                if (member != null) {
                    t.put("userName", member.getUser().getName());
                    t.put("userAvatar", member.getUser().getAvatarUrl());
                    return;
                }
            } catch (Exception ignored) {}
        }
        // Fallback: nome do canal
        String fallback = extractNameFromChannel(channelName);
        if (fallback != null) t.put("userName", fallback);
    }

    private static Map<String, Object> buildOpenTicketInfo(TextChannel ch, Guild guild) {
        Map<String, Object> t = new LinkedHashMap<>();
        String topic = ch.getTopic();
        int ticketId = -1;
        String ownerId = null;
        String mainStaffId = null;

        if (topic != null) {
            for (String part : topic.split("\\|")) {
                String p = part.trim();
                if (p.startsWith("TicketID:")) {
                    try { ticketId = Integer.parseInt(p.substring("TicketID:".length())); } catch (Exception ignored) {}
                } else if (p.startsWith("OwnerID:")) {
                    ownerId = p.substring("OwnerID:".length());
                } else if (p.startsWith("MainStaff:")) {
                    mainStaffId = p.substring("MainStaff:".length());
                }
            }
        }

        String channelName = ch.getName();
        t.put("id", ticketId > 0 ? ticketId : 0);
        t.put("channelName", channelName);
        t.put("userId", ownerId);
        t.put("status", "open");
        t.put("priority", channelName.contains("\uD83D\uDD34") ? "HIGH" : "NORMAL");
        t.put("createdAt", ch.getTimeCreated().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        if (ch.getParentCategory() != null) {
            t.put("category", ch.getParentCategory().getName());
        }

        // Informações do staff que assumiu
        if (mainStaffId != null && !mainStaffId.isEmpty()) {
            t.put("staffId", mainStaffId);
            try {
                var staffMember = guild.getMemberById(mainStaffId);
                if (staffMember != null) {
                    t.put("staffName", staffMember.getUser().getName());
                    t.put("staffAvatar", staffMember.getUser().getAvatarUrl());
                }
            } catch (Exception ignored) {}
        }

        resolveUserFast(t, ownerId, channelName, guild);
        return t;
    }

    private static Guild requireMainGuild(Context ctx, JDA jda) {
        Guild guild = AuthController.getMainGuild(jda);
        if (guild == null) {
            ctx.status(503).json(Map.of("error", "Guild principal não encontrada"));
        }
        return guild;
    }

    public static void register(Javalin app, JDA jda) {

        // GET /api/tickets — lista de tickets
        app.get("/api/tickets", ctx -> {
            String status = ctx.queryParam("status");
            if (status != null && !Set.of("open", "closed", "all").contains(status)) {
                ctx.status(400).json(Map.of("error", "Status inválido"));
                return;
            }
            int page = Math.max(1, ctx.queryParamAsClass("page", Integer.class).getOrDefault(1));
            int limit = Math.max(1, Math.min(ctx.queryParamAsClass("limit", Integer.class).getOrDefault(20), 100));

            try {
                Guild guild = requireMainGuild(ctx, jda);
                if (guild == null) return;
                Set<String> openCategoryIds = getOpenCategoryIds();
                List<Map<String, Object>> allTickets = new ArrayList<>();

                // Coletar tickets abertos + seus IDs do DB
                Set<Integer> openTicketIds = new HashSet<>();
                if (!"closed".equals(status)) {
                    for (TextChannel ch : guild.getTextChannels()) {
                        if (isTicketChannel(ch, openCategoryIds)) {
                            Map<String, Object> info = buildOpenTicketInfo(ch, guild);
                            allTickets.add(info);
                            int tid = (int) info.getOrDefault("id", 0);
                            if (tid > 0) openTicketIds.add(tid);
                        }
                    }
                } else {
                    // Mesmo filtrando só fechados, precisamos dos IDs abertos para excluí-los
                    for (TextChannel ch : guild.getTextChannels()) {
                        if (isTicketChannel(ch, openCategoryIds)) {
                            String topic = ch.getTopic();
                            if (topic != null) {
                                for (String part : topic.split("\\|")) {
                                    String p = part.trim();
                                    if (p.startsWith("TicketID:")) {
                                        try { openTicketIds.add(Integer.parseInt(p.substring("TicketID:".length()))); } catch (Exception ignored) {}
                                    }
                                }
                            }
                        }
                    }
                }

                // Tickets fechados do banco (excluindo os que estão abertos)
                if (!"open".equals(status)) {
                    try (Connection conn = DatabaseManager.getConnection()) {
                        if (conn != null) {
                            String sql = "SELECT id, channel_name, user_id, category_name, priority, closed_at FROM midgard_tickets WHERE closed_at IS NOT NULL ORDER BY id DESC";
                            try (PreparedStatement stmt = conn.prepareStatement(sql);
                                 ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    int id = rs.getInt("id");
                                    // Pular tickets que ainda estão abertos no Discord
                                    if (openTicketIds.contains(id)) continue;

                                    Map<String, Object> t = new LinkedHashMap<>();
                                    t.put("id", id);
                                    String chName = rs.getString("channel_name");
                                    t.put("channelName", chName);
                                    String userId = rs.getString("user_id");
                                    t.put("userId", userId);
                                    t.put("category", rs.getString("category_name"));
                                    t.put("status", "closed");
                                    t.put("priority", rs.getString("priority"));
                                    t.put("closedAt", rs.getString("closed_at"));

                                    resolveUserFast(t, userId, chName, guild);
                                    allTickets.add(t);
                                }
                            }
                        }
                    }
                }

                // Ordenar: abertos primeiro, depois por ID decrescente
                allTickets.sort((a, b) -> {
                    boolean aOpen = "open".equals(a.get("status"));
                    boolean bOpen = "open".equals(b.get("status"));
                    if (aOpen != bOpen) return aOpen ? -1 : 1;
                    int idA = a.get("id") instanceof Integer ? (int) a.get("id") : 0;
                    int idB = b.get("id") instanceof Integer ? (int) b.get("id") : 0;
                    return Integer.compare(idB, idA);
                });

                int total = allTickets.size();
                int offset = (page - 1) * limit;
                int end = Math.min(offset + limit, total);
                List<Map<String, Object>> pageTickets = (offset < total)
                        ? allTickets.subList(offset, end) : List.of();

                ctx.json(Map.of(
                        "total", total,
                        "page", page,
                        "pages", (int) Math.ceil((double) total / limit),
                        "tickets", pageTickets
                ));

            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar tickets", e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar tickets"));
            }
        });

        // GET /api/tickets/stats
        app.get("/api/tickets/stats", ctx -> {
            try {
                Guild guild = requireMainGuild(ctx, jda);
                if (guild == null) return;
                Set<String> openCategoryIds = getOpenCategoryIds();

                // Coletar IDs de tickets abertos no Discord
                int open = 0;
                Set<Integer> openIds = new HashSet<>();
                for (TextChannel ch : guild.getTextChannels()) {
                    if (isTicketChannel(ch, openCategoryIds)) {
                        open++;
                        String topic = ch.getTopic();
                        if (topic != null) {
                            for (String part : topic.split("\\|")) {
                                String p = part.trim();
                                if (p.startsWith("TicketID:")) {
                                    try { openIds.add(Integer.parseInt(p.substring("TicketID:".length()))); } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                }

                // Contar fechados no banco (excluindo IDs de tickets abertos)
                int closed = 0;
                try (Connection conn = DatabaseManager.getConnection()) {
                    if (conn != null) {
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "SELECT id FROM midgard_tickets WHERE closed_at IS NOT NULL");
                             ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                if (!openIds.contains(rs.getInt("id"))) closed++;
                            }
                        }
                    }
                }

                ctx.json(Map.of("open", open, "closed", closed, "total", open + closed));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar stats de tickets", e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar estatísticas"));
            }
        });

        // GET /api/tickets/:id — detalhe de um ticket
        app.get("/api/tickets/{id}", ctx -> {
            int ticketId;
            try {
                ticketId = Integer.parseInt(ctx.pathParam("id"));
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "ID inválido"));
                return;
            }

            try {
                Guild guild = requireMainGuild(ctx, jda);
                if (guild == null) return;
                Set<String> openCategoryIds = getOpenCategoryIds();
                Map<String, Object> ticket = new LinkedHashMap<>();

                // Verificar se está aberto no Discord
                boolean foundOpen = false;
                for (TextChannel ch : guild.getTextChannels()) {
                    if (!isTicketChannel(ch, openCategoryIds)) continue;
                    String topic = ch.getTopic();
                    if (topic == null) continue;
                    int tid = -1;
                    for (String part : topic.split("\\|")) {
                        String p = part.trim();
                        if (p.startsWith("TicketID:")) {
                            try { tid = Integer.parseInt(p.substring("TicketID:".length())); } catch (Exception ignored) {}
                        }
                    }
                    if (tid == ticketId) {
                        ticket = buildOpenTicketInfo(ch, guild);
                        // Extrair staff e collabs do topic
                        for (String part : topic.split("\\|")) {
                            String p = part.trim();
                            if (p.startsWith("MainStaff:")) {
                                String staffId = p.substring("MainStaff:".length());
                                ticket.put("staffId", staffId);
                                var staffMember = guild.getMemberById(staffId);
                                if (staffMember != null) {
                                    ticket.put("staffName", staffMember.getUser().getName());
                                    ticket.put("staffAvatar", staffMember.getUser().getAvatarUrl());
                                }
                            } else if (p.startsWith("Collab:")) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, String>> collabs = (List<Map<String, String>>) ticket.computeIfAbsent("collaborators", k -> new ArrayList<>());
                                String collabId = p.substring("Collab:".length());
                                var cm = guild.getMemberById(collabId);
                                Map<String, String> c = new LinkedHashMap<>();
                                c.put("id", collabId);
                                c.put("name", cm != null ? cm.getUser().getName() : collabId);
                                collabs.add(c);
                            }
                        }
                        // Contar mensagens recentes (últimas 100 do canal)
                        try {
                            var messages = ch.getHistory().retrievePast(100).complete();
                            ticket.put("messageCount", messages.size());
                            if (!messages.isEmpty()) {
                                var last = messages.get(0);
                                ticket.put("lastActivity", last.getTimeCreated().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                                ticket.put("lastAuthor", last.getAuthor().getName());
                            }
                        } catch (Exception ignored) {
                            ticket.put("messageCount", 0);
                        }
                        foundOpen = true;
                        break;
                    }
                }

                // Se não está aberto, buscar do banco
                if (!foundOpen) {
                    try (Connection conn = DatabaseManager.getConnection()) {
                        if (conn != null) {
                            try (PreparedStatement stmt = conn.prepareStatement(
                                    "SELECT id, channel_name, user_id, category_name, priority, content, closed_at FROM midgard_tickets WHERE id = ?")) {
                                stmt.setInt(1, ticketId);
                                try (ResultSet rs = stmt.executeQuery()) {
                                    if (rs.next()) {
                                        String chName = rs.getString("channel_name");
                                        String userId = rs.getString("user_id");
                                        ticket.put("id", rs.getInt("id"));
                                        ticket.put("channelName", chName);
                                        ticket.put("userId", userId);
                                        ticket.put("category", rs.getString("category_name"));
                                        ticket.put("status", "closed");
                                        ticket.put("priority", rs.getString("priority"));
                                        ticket.put("closedAt", rs.getString("closed_at"));
                                        ticket.put("hasTranscript", true);
                                        resolveUserFast(ticket, userId, chName, guild);
                                    }
                                }
                            }
                        }
                    }
                }

                if (ticket.isEmpty()) {
                    ctx.status(404).json(Map.of("error", "Ticket não encontrado"));
                    return;
                }

                ctx.json(ticket);
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar ticket " + ticketId, e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar ticket"));
            }
        });

        // GET /api/tickets/user/:userId — tickets de um usuário
        app.get("/api/tickets/user/{userId}", ctx -> {
            String userId = ctx.pathParam("userId");
            if (!com.midgardbot.web.security.SecurityMiddleware.isValidDiscordId(userId)) {
                ctx.status(400).json(Map.of("error", "ID inválido"));
                return;
            }

            try {
                Guild guild = requireMainGuild(ctx, jda);
                if (guild == null) return;
                Set<String> openCategoryIds = getOpenCategoryIds();
                List<Map<String, Object>> userTickets = new ArrayList<>();
                Set<Integer> openTicketIds = new HashSet<>();

                // Tickets abertos do usuário
                for (TextChannel ch : guild.getTextChannels()) {
                    if (!isTicketChannel(ch, openCategoryIds)) continue;
                    String topic = ch.getTopic();
                    if (topic == null) continue;
                    String ownerId = null;
                    for (String part : topic.split("\\|")) {
                        String p = part.trim();
                        if (p.startsWith("OwnerID:")) ownerId = p.substring("OwnerID:".length());
                    }
                    if (userId.equals(ownerId)) {
                        Map<String, Object> info = buildOpenTicketInfo(ch, guild);
                        userTickets.add(info);
                        int tid = (int) info.getOrDefault("id", 0);
                        if (tid > 0) openTicketIds.add(tid);
                    }
                }

                // Tickets fechados do usuário
                try (Connection conn = DatabaseManager.getConnection()) {
                    if (conn != null) {
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "SELECT id, channel_name, user_id, category_name, priority, closed_at FROM midgard_tickets WHERE user_id = ? AND closed_at IS NOT NULL ORDER BY id DESC LIMIT 50")) {
                            stmt.setString(1, userId);
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    int id = rs.getInt("id");
                                    if (openTicketIds.contains(id)) continue;
                                    Map<String, Object> t = new LinkedHashMap<>();
                                    t.put("id", id);
                                    t.put("channelName", rs.getString("channel_name"));
                                    t.put("userId", userId);
                                    t.put("category", rs.getString("category_name"));
                                    t.put("status", "closed");
                                    t.put("priority", rs.getString("priority"));
                                    t.put("closedAt", rs.getString("closed_at"));
                                    userTickets.add(t);
                                }
                            }
                        }
                    }
                }

                // Ordenar: abertos primeiro, depois por ID desc
                userTickets.sort((a, b) -> {
                    boolean aOpen = "open".equals(a.get("status"));
                    boolean bOpen = "open".equals(b.get("status"));
                    if (aOpen != bOpen) return aOpen ? -1 : 1;
                    int idA = a.get("id") instanceof Integer ? (int) a.get("id") : 0;
                    int idB = b.get("id") instanceof Integer ? (int) b.get("id") : 0;
                    return Integer.compare(idB, idA);
                });

                ctx.json(Map.of("tickets", userTickets, "total", userTickets.size()));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar tickets do user " + userId, e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar tickets do usuário"));
            }
        });

        // GET /api/tickets/:id/transcript — transcrição HTML de um ticket fechado
        app.get("/api/tickets/{id}/transcript", ctx -> {
            int ticketId;
            try {
                ticketId = Integer.parseInt(ctx.pathParam("id"));
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "ID inválido"));
                return;
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                if (conn != null) {
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT content FROM midgard_tickets WHERE id = ?")) {
                        stmt.setInt(1, ticketId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                String content = rs.getString("content");
                                if (content != null && content.length() > 4) {
                                    Map<String, Object> result = new LinkedHashMap<>();
                                    result.put("transcript", content);

                                    // Resolver avatares e nomes dos autores + menções
                                    Guild guild = AuthController.getMainGuild(jda);
                                    if (guild != null) {
                                        Set<String> ids = new HashSet<>();
                                        // IDs dos autores
                                        java.util.regex.Pattern authorIdPat = java.util.regex.Pattern.compile("\"authorId\"\\s*:\\s*\"(\\d+)\"");
                                        var m1 = authorIdPat.matcher(content);
                                        while (m1.find()) ids.add(m1.group(1));
                                        // IDs de menções <@ID> ou <@!ID>
                                        java.util.regex.Pattern mentionPat = java.util.regex.Pattern.compile("<@!?(\\d+)>");
                                        var m2 = mentionPat.matcher(content);
                                        while (m2.find()) ids.add(m2.group(1));
                                        // IDs de menções de role <@&ID>
                                        java.util.regex.Pattern rolePat = java.util.regex.Pattern.compile("<@&(\\d+)>");
                                        var m3 = rolePat.matcher(content);
                                        Set<String> roleIds = new HashSet<>();
                                        while (m3.find()) roleIds.add(m3.group(1));

                                        // Resolver usuários
                                        Map<String, Map<String, String>> users = new LinkedHashMap<>();
                                        for (String uid : ids) {
                                            try {
                                                var member = guild.getMemberById(uid);
                                                if (member != null) {
                                                    Map<String, String> info = new LinkedHashMap<>();
                                                    info.put("name", member.getUser().getName());
                                                    String avatar = member.getUser().getAvatarUrl();
                                                    if (avatar != null) info.put("avatar", avatar + "?size=64");
                                                    info.put("displayName", member.getEffectiveName());
                                                    users.put(uid, info);
                                                }
                                            } catch (Exception ignored) {}
                                        }
                                        result.put("users", users);

                                        // Resolver roles
                                        Map<String, Map<String, String>> roles = new LinkedHashMap<>();
                                        for (String rid : roleIds) {
                                            try {
                                                var role = guild.getRoleById(rid);
                                                if (role != null) {
                                                    Map<String, String> info = new LinkedHashMap<>();
                                                    info.put("name", role.getName());
                                                    info.put("color", String.format("#%06X", role.getColorRaw() & 0xFFFFFF));
                                                    roles.put(rid, info);
                                                }
                                            } catch (Exception ignored) {}
                                        }
                                        result.put("roles", roles);
                                    }

                                    ctx.json(result);
                                    return;
                                }
                            }
                        }
                    }
                }
                ctx.status(404).json(Map.of("error", "Transcrição não encontrada"));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar transcrição do ticket " + ticketId, e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar transcrição"));
            }
        });
    }
}
