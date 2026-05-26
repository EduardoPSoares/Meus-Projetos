package com.midgardbot.web.routes;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.DatabaseManager;
import com.midgardbot.data.StaffStats;
import com.midgardbot.web.auth.AuthController;
import io.javalin.Javalin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.*;

/**
 * Rotas de estatísticas da Staff.
 */
public class StaffRoutes {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaffRoutes.class);

    // Ordem hierárquica dos cargos (índice menor = maior hierarquia)
    private static final String[] ROLE_PRIORITY_ORDER = {
        "FUNDADOR", "CEOO", "ADMIN", "DEV", "DEV_JR", "MODERADOR", "LOREMAKER",
        "AJUDANTE", "BUILDER", "CINEGRAFISTA", "INTERPRETE", "STAFF"
    };

    private static Set<String> getStaffRoleIds() {
        Set<String> roleIds = new HashSet<>();
        for (String key : ROLE_PRIORITY_ORDER) {
            String value = BotConfig.get(key);
            if (value != null && !value.isEmpty()) {
                for (String id : value.split(",")) {
                    roleIds.add(id.trim());
                }
            }
        }
        return roleIds;
    }

    private static Map<String, Integer> buildRolePriorityMap() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < ROLE_PRIORITY_ORDER.length; i++) {
            String val = BotConfig.get(ROLE_PRIORITY_ORDER[i]);
            if (val != null && !val.isEmpty()) {
                for (String id : val.split(",")) {
                    map.putIfAbsent(id.trim(), i);
                }
            }
        }
        return map;
    }

    private static int getMemberPriority(Member member, Map<String, Integer> priorityMap) {
        int best = Integer.MAX_VALUE;
        for (Role r : member.getRoles()) {
            Integer p = priorityMap.get(r.getId());
            if (p != null && p < best) best = p;
        }
        return best;
    }

    private static Map<String, Object> formatStats(StaffStats stats) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (stats == null) return map;

        map.put("Aprovações", stats.approved);
        map.put("Rejeições", stats.rejected);
        map.put("Total Analisado", stats.getTotal());

        if (stats.getTotal() > 0) {
            map.put("Taxa de Aprovação", String.format("%.1f%%", stats.getApprovalRate()));
        } else {
            map.put("Taxa de Aprovação", "—");
        }

        map.put("Tickets Assumidos", stats.ticketsClaimed);
        map.put("Tickets Fechados", stats.ticketsClosed);

        double avgSec = stats.getAverageReviewTime();
        if (avgSec >= 3600) {
            map.put("Tempo Médio", String.format("%.1f h", avgSec / 3600.0));
        } else if (avgSec >= 60) {
            map.put("Tempo Médio", String.format("%.1f min", avgSec / 60.0));
        } else if (avgSec > 0) {
            map.put("Tempo Médio", String.format("%.0f seg", avgSec));
        }

        return map;
    }

    public static void register(Javalin app, JDA jda) {

        // GET /api/staff — lista de staff com estatísticas
        app.get("/api/staff", ctx -> {
            try {
                var guild = AuthController.getMainGuild(jda);
                var staffStats = DataManager.getStaffStats();
                Set<String> staffRoleIds = getStaffRoleIds();
                Map<String, Integer> rolePriorityMap = buildRolePriorityMap();

                // Encontrar membros que possuem pelo menos um cargo de staff
                Set<String> processedIds = new HashSet<>();
                List<Map<String, Object>> staffList = new ArrayList<>();

                for (Member member : guild.getMembers()) {
                    boolean isStaff = member.getRoles().stream()
                            .anyMatch(r -> staffRoleIds.contains(r.getId()));
                    if (!isStaff) continue;

                    String memberId = member.getId();
                    if (!processedIds.add(memberId)) continue;

                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("discordId", memberId);
                    info.put("name", member.getUser().getName());
                    info.put("avatar", member.getUser().getAvatarUrl());
                    info.put("displayName", member.getEffectiveName());
                    info.put("roles", member.getRoles().stream()
                            .map(r -> Map.of("id", r.getId(), "name", r.getName(), "color", String.format("#%06x", r.getColorRaw())))
                            .toList());
                    int priority = getMemberPriority(member, rolePriorityMap);
                    info.put("_priority", priority);

                    // Cargo principal (maior hierarquia)
                    if (priority < ROLE_PRIORITY_ORDER.length) {
                        String configKey = ROLE_PRIORITY_ORDER[priority];
                        String configVal = BotConfig.get(configKey);
                        if (configVal != null) {
                            for (String rid : configVal.split(",")) {
                                Role matchedRole = guild.getRoleById(rid.trim());
                                if (matchedRole != null && member.getRoles().contains(matchedRole)) {
                                    info.put("primaryRole", Map.of(
                                        "id", matchedRole.getId(),
                                        "name", matchedRole.getName(),
                                        "color", String.format("#%06x", matchedRole.getColorRaw()),
                                        "key", configKey
                                    ));
                                    break;
                                }
                            }
                        }
                    }

                    // Stats formatadas em português
                    info.put("stats", formatStats(staffStats.get(memberId)));

                    staffList.add(info);
                }

                // Ordenar por hierarquia de cargo
                staffList.sort(Comparator.comparingInt(m -> (int) m.getOrDefault("_priority", Integer.MAX_VALUE)));
                staffList.forEach(m -> m.remove("_priority"));

                ctx.json(Map.of("staff", staffList));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar dados da staff", e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar dados da staff"));
            }
        });

        // GET /api/staff/:staffId/tickets — tickets atendidos por um staff específico
        app.get("/api/staff/{staffId}/tickets", ctx -> {
            String staffId = ctx.pathParam("staffId");
            if (!com.midgardbot.web.security.SecurityMiddleware.isValidDiscordId(staffId)) {
                ctx.status(400).json(Map.of("error", "ID inválido"));
                return;
            }

            // Verificar permissão: apenas MODERADOR, DEV, CEOO, FUNDADOR
            String requesterId = ctx.attribute("userId");
            if (!hasStaffViewPermission(jda, requesterId)) {
                ctx.status(403).json(Map.of("error", "Sem permissão para visualizar tickets da staff"));
                return;
            }

            try {
                Guild guild = AuthController.getMainGuild(jda);
                if (guild == null) {
                    ctx.status(503).json(Map.of("error", "Guild principal não encontrada"));
                    return;
                }
                List<Map<String, Object>> staffTickets = new ArrayList<>();

                // 1. Tickets abertos onde o staff é MainStaff ou Collab
                Set<String> openCategoryIds = getOpenCategoryIds();
                Set<Integer> openTicketIds = new HashSet<>();
                for (TextChannel ch : guild.getTextChannels()) {
                    if (!isTicketChannel(ch, openCategoryIds)) continue;
                    String topic = ch.getTopic();
                    if (topic == null) continue;

                    boolean isStaffInTicket = false;
                    int ticketId = -1;
                    String ownerId = null;
                    for (String part : topic.split("\\|")) {
                        String p = part.trim();
                        if (p.startsWith("TicketID:")) {
                            try { ticketId = Integer.parseInt(p.substring("TicketID:".length())); } catch (Exception ignored) {}
                        } else if (p.startsWith("OwnerID:")) {
                            ownerId = p.substring("OwnerID:".length());
                        } else if (p.startsWith("MainStaff:") && p.substring("MainStaff:".length()).equals(staffId)) {
                            isStaffInTicket = true;
                        } else if (p.startsWith("Collab:") && p.substring("Collab:".length()).equals(staffId)) {
                            isStaffInTicket = true;
                        }
                    }

                    if (isStaffInTicket) {
                        Map<String, Object> t = new LinkedHashMap<>();
                        t.put("id", ticketId > 0 ? ticketId : 0);
                        t.put("channelName", ch.getName());
                        t.put("userId", ownerId);
                        t.put("status", "open");
                        t.put("priority", "NORMAL");
                        t.put("createdAt", ch.getTimeCreated().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        if (ch.getParentCategory() != null) {
                            t.put("category", ch.getParentCategory().getName());
                        }
                        resolveUserFast(t, ownerId, ch.getName(), guild);
                        staffTickets.add(t);
                        if (ticketId > 0) openTicketIds.add(ticketId);
                    }
                }

                // 2. Tickets fechados onde o staff assumiu ou participou (buscar no claimed_by e content/transcrição)
                try (Connection conn = DatabaseManager.getConnection()) {
                    if (conn != null) {
                        // Primeiro: buscar por claimed_by (rápido) + buscar por authorId no content
                        String sql = "SELECT id, channel_name, user_id, category_name, priority, closed_at, content, claimed_by " +
                                "FROM midgard_tickets WHERE closed_at IS NOT NULL AND " +
                                "(claimed_by = ? OR content LIKE ? OR content LIKE ?) ORDER BY id DESC";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            stmt.setString(1, staffId);
                            stmt.setString(2, "%\"authorId\":\"" + staffId + "\"%");
                            stmt.setString(3, "%\"authorId\": \"" + staffId + "\"%");
                            try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                int id = rs.getInt("id");
                                if (openTicketIds.contains(id)) continue;

                                String content = rs.getString("content");

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
                                t.put("hasTranscript", content != null && !content.isEmpty());

                                // Contar mensagens do staff neste ticket
                                int msgCount = 0;
                                if (content != null) {
                                    String staffPattern = "\"authorId\"\\s*:\\s*\"" + Pattern.quote(staffId) + "\"";
                                    var matcher = Pattern.compile(staffPattern).matcher(content);
                                    while (matcher.find()) {
                                        msgCount++;
                                    }
                                }
                                t.put("staffMessageCount", msgCount);

                                resolveUserFast(t, userId, chName, guild);
                                staffTickets.add(t);
                            }
                            }
                        }
                    }
                }

                // Ordenar: abertos primeiro, depois por ID desc
                staffTickets.sort((a, b) -> {
                    boolean aOpen = "open".equals(a.get("status"));
                    boolean bOpen = "open".equals(b.get("status"));
                    if (aOpen != bOpen) return aOpen ? -1 : 1;
                    int idA = a.get("id") instanceof Integer ? (int) a.get("id") : 0;
                    int idB = b.get("id") instanceof Integer ? (int) b.get("id") : 0;
                    return Integer.compare(idB, idA);
                });

                ctx.json(Map.of("tickets", staffTickets, "total", staffTickets.size()));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar tickets do staff " + staffId, e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar tickets do staff"));
            }
        });

        app.get("/api/staff/{staffId}/whitelists", ctx -> {
            String staffId = ctx.pathParam("staffId");
            if (!com.midgardbot.web.security.SecurityMiddleware.isValidDiscordId(staffId)) {
                ctx.status(400).json(Map.of("error", "ID invÃ¡lido"));
                return;
            }

            String requesterId = ctx.attribute("userId");
            if (!hasStaffViewPermission(jda, requesterId)) {
                ctx.status(403).json(Map.of("error", "Sem permissÃ£o para visualizar whitelists da staff"));
                return;
            }

            try {
                Guild guild = AuthController.getMainGuild(jda);
                if (guild == null) {
                    ctx.status(503).json(Map.of("error", "Guild principal nÃ£o encontrada"));
                    return;
                }

                List<Map<String, Object>> staffWhitelists = new ArrayList<>();
                for (var entry : DataManager.getAllStatus().entrySet()) {
                    String discordId = entry.getKey();
                    var info = entry.getValue();

                    if (info == null || info.status == null) continue;
                    if (info.staffId == null || !staffId.equals(info.staffId)) continue;
                    if (info.answers == null || info.answers.isBlank() || "{}".equals(info.answers)) continue;

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("discordId", discordId);
                    item.put("nickname", info.nickname);
                    item.put("reason", info.reason);
                    item.put("timestamp", info.timestamp);
                    item.put("staffId", info.staffId);
                    item.put("statusKey", info.status.name());
                    item.put("statusLabel", info.status.label);
                    item.put("statusIcon", info.status.icon);
                    item.put("statusCategory", resolveWhitelistCategory(info.status));

                    try {
                        Member member = guild.getMemberById(discordId);
                        if (member != null) {
                            item.put("discordName", member.getUser().getName());
                            item.put("discordDisplayName", member.getEffectiveName());
                            item.put("discordAvatar", member.getUser().getAvatarUrl());
                        }
                    } catch (Exception ignored) {}

                    staffWhitelists.add(item);
                }

                staffWhitelists.sort((a, b) -> {
                    String dateA = String.valueOf(a.getOrDefault("timestamp", ""));
                    String dateB = String.valueOf(b.getOrDefault("timestamp", ""));
                    int dateCompare = dateB.compareTo(dateA);
                    if (dateCompare != 0) return dateCompare;

                    String nameA = String.valueOf(a.getOrDefault("nickname", a.getOrDefault("discordDisplayName", "")));
                    String nameB = String.valueOf(b.getOrDefault("nickname", b.getOrDefault("discordDisplayName", "")));
                    return nameA.compareToIgnoreCase(nameB);
                });

                ctx.json(Map.of("whitelists", staffWhitelists, "total", staffWhitelists.size()));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar whitelists do staff " + staffId, e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar whitelists do staff"));
            }
        });
    }

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
        String name = ch.getName();
        String parentId = ch.getParentCategoryId();
        return (name.startsWith("ticket-") || name.startsWith("\uD83D\uDD34-ticket-") || name.startsWith("\uD83D\uDFE1-ticket-"))
                && parentId != null && openCategoryIds.contains(parentId);
    }

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
        if (channelName != null) {
            String name = channelName;
            if (name.startsWith("\uD83D\uDD34-")) name = name.substring(3);
            if (name.startsWith("\uD83D\uDFE1-")) name = name.substring(3);
            if (name.startsWith("ticket-")) name = name.substring(7);
            int lastDash = name.lastIndexOf('-');
            if (lastDash > 0 && name.substring(lastDash + 1).matches("\\d+")) {
                name = name.substring(0, lastDash);
            }
            if (!name.isEmpty()) t.put("userName", name);
        }
    }

    /**
     * Verifica se o usuário tem permissão para ver tickets da staff.
     * Apenas MODERADOR, DEV, CEOO e FUNDADOR.
     */
    /* Mapeia status da whitelist para categorias visuais do painel. */
    private static String resolveWhitelistCategory(com.midgardbot.data.WhitelistStatus status) {
        return switch (status) {
            case APPROVED, EXCELLENT -> "approved";
            case REJECTED -> "rejected";
            case PENDING, REVIEWING, NEEDS_REVIEW, FLAGGED, PRIORITY, STANDBY -> "pending";
        };
    }

    /* Verifica permissao para ver atividades da staff. */
    private static boolean hasStaffViewPermission(JDA jda, String userId) {
        try {
            Guild guild = AuthController.getMainGuild(jda);
            if (guild == null) return false;
            Member member = guild.getMemberById(userId);
            if (member == null) return false;

            Set<String> allowedRoleIds = new HashSet<>();
            String[] allowedKeys = {"AJUDANTE", "MODERADOR", "DEV", "CEOO", "FUNDADOR"};
            for (String key : allowedKeys) {
                String val = BotConfig.get(key);
                if (val != null && !val.isEmpty()) {
                    for (String id : val.split(",")) {
                        allowedRoleIds.add(id.trim());
                    }
                }
            }

            return member.getRoles().stream()
                    .anyMatch(r -> allowedRoleIds.contains(r.getId()));
        } catch (Exception e) {
            LOGGER.warn("[WEB] Erro ao verificar permissão de staff view: {}", e.getMessage());
            return false;
        }
    }
}
