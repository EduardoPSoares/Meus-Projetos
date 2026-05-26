package com.midgardbot.web.routes;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DatabaseManager;
import com.midgardbot.web.auth.AuthController;
import com.midgardbot.web.security.SecurityMiddleware;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.channel.forums.ForumTagSnowflake;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

/**
 * Rotas de gerenciamento de Moderação (punições).
 */
public class ModerationRoutes {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationRoutes.class);

    public static void register(Javalin app, JDA jda) {

        // GET /api/punishments — lista de punições
        app.get("/api/punishments", ctx -> {
            String type = ctx.queryParam("type"); // ban, warn, mute, tempban
            // Validar tipo para prevenir valores inesperados
            if (type != null && !Set.of("ban", "warn", "mute", "tempban", "kick").contains(type)) {
                ctx.status(400).json(Map.of("error", "Tipo de punição inválido"));
                return;
            }
            String active = ctx.queryParam("active"); // true, false
            if (active != null && !"true".equals(active) && !"false".equals(active)) {
                ctx.status(400).json(Map.of("error", "Valor de 'active' inválido"));
                return;
            }
            String source = ctx.queryParam("source"); // plugin, panel
            if (source != null && !Set.of("plugin", "panel").contains(source)) {
                ctx.status(400).json(Map.of("error", "Valor de 'source' inválido"));
                return;
            }
            String search = Optional.ofNullable(ctx.queryParam("search"))
                    .map(String::trim)
                    .orElse("");
            int page = Math.max(1, ctx.queryParamAsClass("page", Integer.class).getOrDefault(1));
            int limit = Math.max(1, Math.min(ctx.queryParamAsClass("limit", Integer.class).getOrDefault(20), 100));
            int offset = (page - 1) * limit;

            List<Map<String, Object>> punishments = new ArrayList<>();
            int total = 0;

            try (Connection conn = DatabaseManager.getConnection()) {
                if (conn == null) {
                    ctx.json(Map.of("total", 0, "punishments", List.of(), "error", "Banco não conectado"));
                    return;
                }

                StringBuilder sql = new StringBuilder("SELECT * FROM midgard_punishments WHERE 1=1");
                List<Object> params = new ArrayList<>();

                if (type != null && !type.isEmpty()) {
                    sql.append(" AND type = ?");
                    params.add(type);
                }
                if ("true".equals(active)) {
                    sql.append(" AND active = TRUE");
                } else if ("false".equals(active)) {
                    sql.append(" AND active = FALSE");
                }
                if (source != null && !source.isEmpty()) {
                    sql.append(" AND COALESCE(source, 'plugin') = ?");
                    params.add(source);
                }
                if (!search.isEmpty()) {
                    sql.append(" AND (")
                            .append("LOWER(COALESCE(target_name, '')) LIKE ?")
                            .append(" OR LOWER(COALESCE(target_discord_id, '')) LIKE ?")
                            .append(" OR LOWER(COALESCE(target_identifier, '')) LIKE ?")
                            .append(" OR LOWER(COALESCE(moderator_name, '')) LIKE ?")
                            .append(" OR LOWER(COALESCE(moderator_identifier, '')) LIKE ?")
                            .append(" OR LOWER(COALESCE(reason, '')) LIKE ?")
                            .append(")");
                    String searchLike = "%" + search.toLowerCase(Locale.ROOT) + "%";
                    for (int i = 0; i < 6; i++) {
                        params.add(searchLike);
                    }
                }

                // Count
                String countSql = sql.toString().replace("SELECT *", "SELECT COUNT(*)");
                try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) {
                        countStmt.setObject(i + 1, params.get(i));
                    }
                    try (ResultSet rs = countStmt.executeQuery()) {
                        if (rs.next()) total = rs.getInt(1);
                    }
                }

                // Data
                sql.append(" ORDER BY start_time DESC LIMIT ? OFFSET ?");
                try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }
                    stmt.setInt(params.size() + 1, limit);
                    stmt.setInt(params.size() + 2, offset);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> p = new LinkedHashMap<>();
                            p.put("id", rs.getInt("id"));
                            p.put("type", rs.getString("type"));
                            p.put("targetName", rs.getString("target_name"));
                            p.put("targetDiscordId", rs.getString("target_discord_id"));
                            p.put("moderatorName", rs.getString("moderator_name"));
                            p.put("moderatorId", rs.getString("moderator_identifier"));
                            p.put("reason", rs.getString("reason"));
                            p.put("startTime", rs.getLong("start_time"));
                            p.put("endTime", rs.getLong("end_time"));
                            p.put("active", rs.getBoolean("active"));
                            p.put("removedBy", rs.getString("removed_by"));
                            p.put("removedReason", rs.getString("removed_reason"));
                            String src = rs.getString("source");
                            p.put("source", src != null ? src : "plugin");

                            // Resolver nome Discord do alvo se possível
                            try {
                                String targetDiscordId = rs.getString("target_discord_id");
                                if (targetDiscordId != null) {
                                    var guild = AuthController.getMainGuild(jda);
                                    var member = guild.getMemberById(targetDiscordId);
                                    if (member != null) {
                                        p.put("targetDiscordName", member.getUser().getName());
                                        p.put("targetAvatar", member.getUser().getEffectiveAvatarUrl());
                                    }
                                }
                            } catch (Exception ignored) {}

                            // Resolver avatar do moderador
                            try {
                                String modId = rs.getString("moderator_identifier");
                                if (modId != null) {
                                    var guild = AuthController.getMainGuild(jda);
                                    var modMember = guild.getMemberById(modId);
                                    if (modMember != null) {
                                        p.put("moderatorAvatar", modMember.getUser().getEffectiveAvatarUrl());
                                    }
                                }
                            } catch (Exception ignored) {}

                            punishments.add(p);
                        }
                    }
                }

                ctx.json(Map.of(
                        "total", total,
                        "page", page,
                        "pages", (int) Math.ceil((double) total / limit),
                        "punishments", punishments
                ));

            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar punições", e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar punições"));
            }
        });

        // GET /api/punishments/stats — estatísticas de moderação
        app.get("/api/punishments/stats", ctx -> {
            Map<String, Object> stats = new LinkedHashMap<>();

            try (Connection conn = DatabaseManager.getConnection()) {
                if (conn == null) {
                    ctx.json(Map.of("error", "Banco não conectado"));
                    return;
                }

                String statsSource = ctx.queryParam("source");
                String sourceFilter = "";
                List<Object> statsParams = new ArrayList<>();
                if (statsSource != null && Set.of("plugin", "panel").contains(statsSource)) {
                    sourceFilter = " WHERE COALESCE(source, 'plugin') = ?";
                    statsParams.add(statsSource);
                }

                // Total por tipo
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT type, COUNT(*) as count FROM midgard_punishments" + sourceFilter + " GROUP BY type")) {
                    for (int i = 0; i < statsParams.size(); i++) {
                        stmt.setObject(i + 1, statsParams.get(i));
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        Map<String, Integer> byType = new LinkedHashMap<>();
                        while (rs.next()) {
                            byType.put(rs.getString("type"), rs.getInt("count"));
                        }
                        stats.put("byType", byType);
                    }
                }

                // Ativos
                String activeFilter = sourceFilter.isEmpty() ? " WHERE active = TRUE" : sourceFilter + " AND active = TRUE";
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM midgard_punishments" + activeFilter)) {
                    for (int i = 0; i < statsParams.size(); i++) {
                        stmt.setObject(i + 1, statsParams.get(i));
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) stats.put("active", rs.getInt(1));
                    }
                }

                ctx.json(stats);
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao buscar stats de punições", e);
                ctx.status(500).json(Map.of("error", "Erro ao buscar estatísticas"));
            }
        });

        // POST /api/punishments/forum — publicar punição no canal de fórum do Discord
        app.post("/api/punishments/forum", ctx -> {
            try {
                String moderatorId = ctx.attribute("userId");
                String moderatorName = ctx.attribute("username");

                // Ler campos do multipart
                String targetId = ctx.formParam("targetId");
                String type = ctx.formParam("type");
                String severity = ctx.formParam("severity");
                String reason = ctx.formParam("reason");
                List<UploadedFile> images = ctx.uploadedFiles("image");

                // Validações
                if (targetId == null || targetId.isBlank()) {
                    ctx.status(400).json(Map.of("error", "ID do usuário alvo é obrigatório"));
                    return;
                }
                if (!SecurityMiddleware.isValidDiscordId(targetId)) {
                    ctx.status(400).json(Map.of("error", "ID do Discord inválido"));
                    return;
                }
                // Tipo sempre warn (advertência) para punições do painel
                type = "warn";
                if (severity == null || !Set.of("leve", "média", "pesada").contains(severity)) {
                    ctx.status(400).json(Map.of("error", "Severidade inválida (leve, média, pesada)"));
                    return;
                }
                if (reason == null || reason.isBlank()) {
                    ctx.status(400).json(Map.of("error", "Motivo é obrigatório"));
                    return;
                }
                reason = reason.length() > 1500 ? reason.substring(0, 1500) : reason;

                // Validar imagem (máx 10MB, somente imagens)
                UploadedFile image = images != null && !images.isEmpty() ? images.get(0) : null;
                if (image != null) {
                    if (image.size() > 10 * 1024 * 1024) {
                        ctx.status(400).json(Map.of("error", "Imagem excede 10MB"));
                        return;
                    }
                    String ct = image.contentType() != null ? image.contentType() : "";
                    if (!ct.startsWith("image/")) {
                        ctx.status(400).json(Map.of("error", "Somente imagens são permitidas"));
                        return;
                    }
                }

                // Buscar guild e canal do fórum
                String forumId = BotConfig.get("PUNISHMENT_FORUM_ID");
                if (forumId == null || forumId.isBlank()) {
                    ctx.status(500).json(Map.of("error", "PUNISHMENT_FORUM_ID não configurado"));
                    return;
                }

                Guild guild = AuthController.getMainGuild(jda);
                ForumChannel forum = guild.getForumChannelById(forumId);
                if (forum == null) {
                    ctx.status(500).json(Map.of("error", "Canal de fórum não encontrado"));
                    return;
                }

                // Resolver nome do alvo
                String targetName = targetId;
                Member targetMember = null;
                try {
                    targetMember = guild.retrieveMemberById(targetId).complete();
                    if (targetMember != null) {
                        targetName = targetMember.getEffectiveName();
                    }
                } catch (Exception ignored) {}

                // Label do tipo
                String typeLabel = "Advertência";

                // Data atual formatada (dd/MM/yyyy)
                String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                // Montar mensagem
                String message = "\uD83D\uDED1 O Usuário <@" + targetId + "> está recebendo um(a) **"
                        + typeLabel + " " + severity + "** após uma infração de Regras in-game.\n\n"
                        + "**Motivo:** " + reason;

                // Título do post no fórum
                String postTitle = typeLabel + " " + severity + " — " + targetName + " " + dateStr;
                if (postTitle.length() > 100) postTitle = postTitle.substring(0, 100);

                // Criar post no fórum com tag de severidade
                MessageCreateBuilder msgBuilder = new MessageCreateBuilder().setContent(message);

                if (image != null) {
                    byte[] imageBytes = image.content().readAllBytes();
                    msgBuilder.addFiles(FileUpload.fromData(imageBytes, image.filename()));
                }

                MessageCreateData msgData = msgBuilder.build();

                LOGGER.info("[MODERATION] Criando post no fórum: título='{}', severity='{}'", postTitle, severity);

                var forumPost = forum.createForumPost(postTitle, msgData).complete();

                String threadId = forumPost.getThreadChannel().getId();

                // Salvar punição no banco de dados
                try (Connection conn = DatabaseManager.getConnection()) {
                    if (conn != null) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO midgard_punishments (target_identifier, target_name, target_discord_id, type, reason, moderator_identifier, moderator_name, start_time, active, source, forum_thread_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, 'panel', ?)")) {
                            ps.setString(1, targetId);
                            ps.setString(2, targetName);
                            ps.setString(3, targetId);
                            ps.setString(4, type);
                            ps.setString(5, "[" + severity + "] " + reason);
                            ps.setString(6, moderatorId);
                            ps.setString(7, moderatorName);
                            ps.setLong(8, System.currentTimeMillis());
                            ps.setString(9, threadId);
                            ps.executeUpdate();
                        }
                    }
                } catch (Exception dbErr) {
                    LOGGER.warn("[MODERATION] Punição postada no fórum mas erro ao salvar no banco", dbErr);
                }

                // Atribuir cargo de severidade ao membro punido
                Map<String, String> severityRoleIds = Map.of(
                        "leve", "1433222292590039231",
                        "média", "1433222470139121774",
                        "pesada", "1433222561876938752"
                );
                String roleId = severityRoleIds.get(severity.toLowerCase());
                if (roleId != null && targetMember != null) {
                    Role role = guild.getRoleById(roleId);
                    final String logTargetName = targetName;
                    if (role != null) {
                        guild.addRoleToMember(targetMember, role).queue(
                            s -> LOGGER.info("[MODERATION] Cargo '{}' adicionado para {}", role.getName(), logTargetName),
                            e -> LOGGER.warn("[MODERATION] Erro ao adicionar cargo para {}", logTargetName, e)
                        );
                    } else {
                        LOGGER.warn("[MODERATION] Cargo de severidade '{}' não encontrado (ID: {})", severity, roleId);
                    }
                } else if (roleId != null) {
                    LOGGER.warn("[MODERATION] Não foi possível atribuir cargo: membro {} não encontrado no servidor", targetId);
                }

                LOGGER.info("[MODERATION] {} ({}) publicou punição no fórum: {} para {} ({})",
                        moderatorName, moderatorId, type, targetName, targetId);

                ctx.json(Map.of(
                        "success", true,
                        "threadId", threadId,
                        "message", "Punição publicada no fórum com sucesso"
                ));

            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao publicar punição no fórum", e);
                ctx.status(500).json(Map.of("error", "Erro ao publicar punição no fórum"));
            }
        });

        // POST /api/punishments/import-forum — importar punições existentes do fórum para o banco
        app.post("/api/punishments/import-forum", ctx -> {
            try {
                String forumId = BotConfig.get("PUNISHMENT_FORUM_ID");
                if (forumId == null || forumId.isBlank()) {
                    ctx.status(500).json(Map.of("error", "PUNISHMENT_FORUM_ID não configurado"));
                    return;
                }

                Guild guild = AuthController.getMainGuild(jda);
                ForumChannel forum = guild.getForumChannelById(forumId);
                if (forum == null) {
                    ctx.status(500).json(Map.of("error", "Canal de fórum não encontrado"));
                    return;
                }

                // Mapear tags de severidade dinamicamente pelo nome
                Map<String, String> tagToSeverity = new HashMap<>();
                for (ForumTag ft : forum.getAvailableTags()) {
                    String tagName = ft.getName().toLowerCase();
                    if (tagName.equals("leve") || tagName.equals("média") || tagName.equals("pesada")) {
                        tagToSeverity.put(ft.getId(), tagName);
                    }
                }

                // Regex para extrair dados da mensagem
                Pattern mentionPattern = Pattern.compile("<@!?(\\d+)>");
                Pattern reasonPattern = Pattern.compile("\\*\\*Motivo:\\*\\*\\s*(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

                // Coletar todas as threads (ativas + arquivadas)
                List<ThreadChannel> allThreads = new ArrayList<>(forum.getThreadChannels());
                try {
                    var archivedPublic = forum.retrieveArchivedPublicThreadChannels().complete();
                    for (ThreadChannel t : archivedPublic) {
                        if (!allThreads.contains(t)) allThreads.add(t);
                    }
                } catch (Exception e) {
                    LOGGER.warn("[IMPORT] Erro ao buscar threads arquivadas: {}", e.getMessage());
                }

                int imported = 0;
                int skipped = 0;
                int failed = 0;

                try (Connection conn = DatabaseManager.getConnection()) {
                    if (conn == null) {
                        ctx.status(500).json(Map.of("error", "Banco não conectado"));
                        return;
                    }

                    for (ThreadChannel thread : allThreads) {
                        try {
                            // Buscar primeira mensagem da thread
                            List<Message> msgs = thread.getHistory().retrievePast(1).complete();
                            if (msgs.isEmpty()) { skipped++; continue; }
                            Message firstMsg = msgs.get(0);
                            String content = firstMsg.getContentRaw();
                            if (content == null || content.isBlank()) { skipped++; continue; }

                            // Extrair ID do alvo
                            Matcher mentionMatcher = mentionPattern.matcher(content);
                            if (!mentionMatcher.find()) { skipped++; continue; }
                            String targetDiscordId = mentionMatcher.group(1);

                            // Verificar se já existe no banco (por target + start_time próximo)
                            long threadCreated = thread.getTimeCreated().toInstant().toEpochMilli();
                            try (PreparedStatement checkStmt = conn.prepareStatement(
                                    "SELECT COUNT(*) FROM midgard_punishments WHERE target_discord_id = ? AND source = 'panel' AND ABS(start_time - ?) < 60000")) {
                                checkStmt.setString(1, targetDiscordId);
                                checkStmt.setLong(2, threadCreated);
                                try (ResultSet rs = checkStmt.executeQuery()) {
                                    if (rs.next() && rs.getInt(1) > 0) {
                                        skipped++;
                                        continue;
                                    }
                                }
                            }

                            // Tipo sempre warn (advertência)
                            String type = "warn";

                            // Detectar severidade pela tag do fórum
                            String severity = "";
                            for (var tag : thread.getAppliedTags()) {
                                String sev = tagToSeverity.get(tag.getId());
                                if (sev != null) { severity = sev; break; }
                            }

                            // Extrair motivo
                            String reason = "";
                            Matcher reasonMatcher = reasonPattern.matcher(content);
                            if (reasonMatcher.find()) {
                                reason = reasonMatcher.group(1).trim();
                            }
                            String fullReason = severity.isEmpty() ? reason : "[" + severity + "] " + reason;
                            if (fullReason.isBlank()) fullReason = thread.getName();

                            // Resolver nome do alvo
                            String targetName = targetDiscordId;
                            try {
                                Member m = guild.getMemberById(targetDiscordId);
                                if (m != null) targetName = m.getEffectiveName();
                            } catch (Exception ignored) {}

                            // Moderador = autor da mensagem
                            String moderatorId = firstMsg.getAuthor().getId();
                            String moderatorName = firstMsg.getAuthor().getName();

                            // Inserir no banco
                            try (PreparedStatement ps = conn.prepareStatement(
                                    "INSERT INTO midgard_punishments (target_identifier, target_name, target_discord_id, type, reason, moderator_identifier, moderator_name, start_time, active, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE, 'panel')")) {
                                ps.setString(1, targetDiscordId);
                                ps.setString(2, targetName);
                                ps.setString(3, targetDiscordId);
                                ps.setString(4, type);
                                ps.setString(5, fullReason);
                                ps.setString(6, moderatorId);
                                ps.setString(7, moderatorName);
                                ps.setLong(8, threadCreated);
                                ps.executeUpdate();
                                imported++;
                            }

                        } catch (Exception threadErr) {
                            LOGGER.warn("[IMPORT] Erro ao processar thread {}: {}", thread.getName(), threadErr.getMessage());
                            failed++;
                        }
                    }
                }

                LOGGER.info("[IMPORT] Importação do fórum concluída: {} importadas, {} ignoradas, {} erros",
                        imported, skipped, failed);

                ctx.json(Map.of(
                        "success", true,
                        "imported", imported,
                        "skipped", skipped,
                        "failed", failed,
                        "total", allThreads.size(),
                        "message", "Importação concluída: " + imported + " punições importadas, " + skipped + " ignoradas, " + failed + " erros"
                ));

            } catch (Exception e) {
                LOGGER.error("[WEB] Erro na importação de punições do fórum", e);
                ctx.status(500).json(Map.of("error", "Erro na importação de punições"));
            }
        });

        // PUT /api/punishments/:id/revoke — revogar uma punição ativa
        app.put("/api/punishments/{id}/revoke", ctx -> {
            int punishmentId;
            try {
                punishmentId = Integer.parseInt(ctx.pathParam("id"));
            } catch (NumberFormatException e) {
                ctx.status(400).json(Map.of("error", "ID inválido"));
                return;
            }

            String requesterId = ctx.attribute("userId");
            String revokeReason = "";
            try {
                var body = ctx.bodyAsClass(Map.class);
                if (body.containsKey("reason")) {
                    revokeReason = String.valueOf(body.get("reason")).trim();
                }
            } catch (Exception ignored) {}

            try (Connection conn = DatabaseManager.getConnection()) {
                if (conn == null) {
                    ctx.status(500).json(Map.of("error", "Banco não conectado"));
                    return;
                }

                // Verificar se a punição existe e está ativa
                String forumThreadId = null;
                String revokeTargetDiscordId = null;
                String revokeReason2 = null;
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT id, active, target_name, type, reason, forum_thread_id, target_discord_id FROM midgard_punishments WHERE id = ?")) {
                    check.setInt(1, punishmentId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            ctx.status(404).json(Map.of("error", "Punição não encontrada"));
                            return;
                        }
                        if (!rs.getBoolean("active")) {
                            ctx.status(400).json(Map.of("error", "Esta punição já foi revogada"));
                            return;
                        }
                        forumThreadId = rs.getString("forum_thread_id");
                        revokeTargetDiscordId = rs.getString("target_discord_id");
                        revokeReason2 = rs.getString("reason");
                    }
                }

                // Resolver nome do staff que está revogando
                String revokerName = requesterId;
                try {
                    Guild guild = AuthController.getMainGuild(jda);
                    if (guild != null) {
                        Member member = guild.getMemberById(requesterId);
                        if (member != null) {
                            revokerName = member.getUser().getName();
                        }
                    }
                } catch (Exception ignored) {}

                // Revogar a punição
                long now = System.currentTimeMillis();
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE midgard_punishments SET active = FALSE, removed_by = ?, removed_reason = ?, removed_at = ? WHERE id = ?")) {
                    stmt.setString(1, revokerName);
                    stmt.setString(2, revokeReason.isEmpty() ? null : revokeReason);
                    stmt.setLong(3, now);
                    stmt.setInt(4, punishmentId);
                    stmt.executeUpdate();
                }

                // Log da revogação
                try {
                    com.midgardbot.data.LogService.log("moderation",
                            "Punição #" + punishmentId + " revogada",
                            "Revogada por " + revokerName + (revokeReason.isEmpty() ? "" : " — Motivo: " + revokeReason),
                            "🔓");
                } catch (Exception ignored) {}

                // Deletar post do fórum se existir
                if (forumThreadId != null && !forumThreadId.isBlank()) {
                    try {
                        Guild guild = AuthController.getMainGuild(jda);
                        String forumId = BotConfig.get("PUNISHMENT_FORUM_ID");
                        if (guild != null && forumId != null) {
                            ForumChannel forum = guild.getForumChannelById(forumId);
                            if (forum != null) {
                                var thread = guild.getThreadChannelById(forumThreadId);
                                if (thread != null) {
                                    final String threadIdForLog = forumThreadId;
                                    final int pidForLog = punishmentId;
                                    thread.delete().reason("Punição #" + punishmentId + " revogada por " + revokerName).queue(
                                        success -> LOGGER.info("[MOD] Thread {} do fórum deletada (punição #{})", threadIdForLog, pidForLog),
                                        error -> LOGGER.warn("[MOD] Erro ao deletar thread {} do fórum", threadIdForLog, error)
                                    );
                                }
                            }
                        }
                    } catch (Exception forumErr) {
                        LOGGER.warn("[MOD] Erro ao tentar deletar post do fórum para punição #{}", punishmentId, forumErr);
                    }
                }

                // Remover cargo de severidade do membro
                if (revokeTargetDiscordId != null && revokeReason2 != null) {
                    try {
                        Map<String, String> severityRoleIdsRevoke = Map.of(
                                "leve", "1433222292590039231",
                                "média", "1433222470139121774",
                                "pesada", "1433222561876938752"
                        );
                        // Extrair severidade do reason: "[leve] motivo"
                        String revokeSeverity = null;
                        var sevMatch = java.util.regex.Pattern.compile("^\\[(leve|média|pesada)\\]", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(revokeReason2);
                        if (sevMatch.find()) {
                            revokeSeverity = sevMatch.group(1).toLowerCase();
                        }
                        if (revokeSeverity != null) {
                            String revokeRoleId = severityRoleIdsRevoke.get(revokeSeverity);
                            if (revokeRoleId != null) {
                                Guild revokeGuild = AuthController.getMainGuild(jda);
                                Member revokeMember = revokeGuild.getMemberById(revokeTargetDiscordId);
                                if (revokeMember != null) {
                                    Role revokeRole = revokeGuild.getRoleById(revokeRoleId);
                                    if (revokeRole != null) {
                                        revokeGuild.removeRoleFromMember(revokeMember, revokeRole).queue(
                                            s -> LOGGER.info("[MOD] Cargo '{}' removido de {} (punição #{})", revokeRole.getName(), revokeMember.getUser().getName(), punishmentId),
                                            e -> LOGGER.warn("[MOD] Erro ao remover cargo de {} (punição #{})", revokeMember.getUser().getName(), punishmentId, e)
                                        );
                                    }
                                }
                            }
                        }
                    } catch (Exception roleErr) {
                        LOGGER.warn("[MOD] Erro ao remover cargo de severidade na revogação da punição #{}", punishmentId, roleErr);
                    }
                }

                LOGGER.info("[MOD] Punição #{} revogada por {} ({})", punishmentId, revokerName, requesterId);
                ctx.json(Map.of("success", true, "message", "Punição revogada com sucesso"));

            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao revogar punição #" + punishmentId, e);
                ctx.status(500).json(Map.of("error", "Erro ao revogar punição"));
            }
        });
    }
}
