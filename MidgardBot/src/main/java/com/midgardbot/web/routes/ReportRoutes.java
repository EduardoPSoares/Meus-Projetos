package com.midgardbot.web.routes;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DatabaseManager;
import com.midgardbot.utils.TextFormatter;
import com.midgardbot.web.auth.AuthController;
import com.midgardbot.web.security.SecurityMiddleware;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Rotas de relatórios da staff — cada membro pode registrar atividades
 * agrupadas por cargo, com visualização em linha do tempo.
 * Suporta anexos (imagens, vídeos, links) e geração de PDF mensal.
 */
public class ReportRoutes {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportRoutes.class);
    private static final Gson GSON = new Gson();

    private static final String[] ROLE_PRIORITY_ORDER = {
        "FUNDADOR", "CEOO", "ADMIN", "DEV", "DEV_JR", "MODERADOR", "LOREMAKER",
        "AJUDANTE", "BUILDER", "CINEGRAFISTA", "INTERPRETE", "STAFF"
    };

    private static final Path UPLOADS_DIR = Path.of("data", "uploads", "reports");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_FILES_PER_REPORT = 5;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
        "video/mp4", "video/webm"
    );

    public static void register(Javalin app, JDA jda) {
        // Criar diretório de uploads
        try {
            Files.createDirectories(UPLOADS_DIR);
        } catch (IOException e) {
            LOGGER.error("[REPORTS] Erro ao criar diretório de uploads", e);
        }

        // GET /api/reports — lista todos os relatórios (paginado, filtro por cargo opcional)
        app.get("/api/reports", ctx -> {
            try {
                String roleFilter = ctx.queryParam("roleId");
                int page = SecurityMiddleware.clampPage(ctx.queryParamAsClass("page", Integer.class).getOrDefault(1));
                int limit = SecurityMiddleware.clampLimit(ctx.queryParamAsClass("limit", Integer.class).getOrDefault(20), 50);
                int offset = (page - 1) * limit;

                List<Map<String, Object>> reports;
                if (roleFilter != null && !roleFilter.isBlank() && SecurityMiddleware.isValidDiscordId(roleFilter)) {
                    reports = DatabaseManager.getReportsByRole(roleFilter);
                } else {
                    reports = DatabaseManager.getReports(limit, offset);
                }

                // IDs para buscar anexos em lote
                List<Integer> reportIds = reports.stream().map(r -> (Integer) r.get("id")).toList();
                var attachmentsMap = DatabaseManager.getAttachmentsForReports(reportIds);

                // Enriquecer com avatar do Discord + anexos
                var guild = AuthController.getMainGuild(jda);
                for (var report : reports) {
                    String authorId = (String) report.get("authorId");
                    Member member = guild.getMemberById(authorId);
                    if (member != null) {
                        report.put("authorAvatar", member.getUser().getAvatarUrl());
                        report.put("authorDisplayName", member.getEffectiveName());
                    }
                    int id = (Integer) report.get("id");
                    report.put("attachments", attachmentsMap.getOrDefault(id, List.of()));
                }

                int total = DatabaseManager.countReports();

                ctx.json(Map.of(
                    "reports", reports,
                    "total", total,
                    "page", page,
                    "pages", Math.max(1, (int) Math.ceil((double) total / limit))
                ));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao listar relatórios", e);
                ctx.status(500).json(Map.of("error", "Erro ao listar relatórios"));
            }
        });

        // GET /api/reports/roles — retorna todos os cargos (para filtro) + o maior cargo do usuário logado
        app.get("/api/reports/roles", ctx -> {
            try {
                var guild = AuthController.getMainGuild(jda);
                String userId = ctx.attribute("userId");
                Member member = guild.getMemberById(userId);

                List<Map<String, String>> allRoles = new ArrayList<>();
                for (String key : ROLE_PRIORITY_ORDER) {
                    String value = BotConfig.get(key);
                    if (value == null || value.isEmpty()) continue;
                    for (String id : value.split(",")) {
                        String roleId = id.trim();
                        Role role = guild.getRoleById(roleId);
                        if (role != null) {
                            allRoles.add(Map.of(
                                "id", role.getId(),
                                "name", role.getName(),
                                "color", String.format("#%06x", role.getColorRaw()),
                                "key", key
                            ));
                        }
                    }
                }

                Map<String, String> highestRole = null;
                if (member != null) {
                    Set<String> memberRoleIds = new HashSet<>();
                    for (Role r : member.getRoles()) {
                        memberRoleIds.add(r.getId());
                    }
                    for (String key : ROLE_PRIORITY_ORDER) {
                        String value = BotConfig.get(key);
                        if (value == null || value.isEmpty()) continue;
                        for (String id : value.split(",")) {
                            String roleId = id.trim();
                            if (memberRoleIds.contains(roleId)) {
                                Role role = guild.getRoleById(roleId);
                                if (role != null) {
                                    highestRole = Map.of(
                                        "id", role.getId(),
                                        "name", role.getName(),
                                        "color", String.format("#%06x", role.getColorRaw()),
                                        "key", key
                                    );
                                }
                                break;
                            }
                        }
                        if (highestRole != null) break;
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("roles", allRoles);
                response.put("highestRole", highestRole);
                ctx.json(response);
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao listar cargos para relatórios", e);
                ctx.status(500).json(Map.of("error", "Erro ao listar cargos"));
            }
        });

        // POST /api/reports — criar novo relatório (multipart com files + JSON text fields)
        app.post("/api/reports", ctx -> {
            try {
                String userId = ctx.attribute("userId");
                String username = ctx.attribute("username");

                // Ler campos — suporta tanto JSON puro quanto multipart/form-data
                String title, description, roleId;
                List<String> links = List.of();
                List<UploadedFile> files = List.of();

                String contentType = ctx.contentType() != null ? ctx.contentType() : "";
                if (contentType.contains("multipart/form-data")) {
                    title = ctx.formParam("title");
                    description = ctx.formParam("description");
                    roleId = ctx.formParam("roleId");
                    String linksJson = ctx.formParam("links");
                    if (linksJson != null && !linksJson.isBlank()) {
                        links = GSON.fromJson(linksJson, new TypeToken<List<String>>(){}.getType());
                    }
                    files = ctx.uploadedFiles("files");
                } else {
                    var body = GSON.fromJson(ctx.body(), ReportBody.class);
                    if (body == null) {
                        ctx.status(400).json(Map.of("error", "Body inválido"));
                        return;
                    }
                    title = body.title;
                    description = body.description;
                    roleId = body.roleId;
                    if (body.links != null) links = body.links;
                }

                if (title == null || title.isBlank() || description == null || description.isBlank()
                        || roleId == null || roleId.isBlank()) {
                    ctx.status(400).json(Map.of("error", "Campos obrigatórios: title, description, roleId"));
                    return;
                }

                if (!SecurityMiddleware.isValidDiscordId(roleId)) {
                    ctx.status(400).json(Map.of("error", "ID de cargo inválido"));
                    return;
                }

                // Validar quantidade de arquivos
                if (files.size() > MAX_FILES_PER_REPORT) {
                    ctx.status(400).json(Map.of("error", "Máximo de " + MAX_FILES_PER_REPORT + " arquivos por relatório"));
                    return;
                }

                // Validar tipos e tamanhos dos arquivos
                for (UploadedFile file : files) {
                    if (file.size() > MAX_FILE_SIZE) {
                        ctx.status(400).json(Map.of("error", "Arquivo \"" + file.filename() + "\" excede 10MB"));
                        return;
                    }
                    String ct = file.contentType() != null ? file.contentType() : "";
                    if (!ALLOWED_IMAGE_TYPES.contains(ct) && !ALLOWED_VIDEO_TYPES.contains(ct)) {
                        ctx.status(400).json(Map.of("error", "Tipo de arquivo não permitido: " + ct + ". Use imagens (JPG, PNG, GIF, WebP) ou vídeos (MP4, WebM)"));
                        return;
                    }
                }

                // Validar links (máx 5, URLs válidas)
                if (links.size() > 5) {
                    ctx.status(400).json(Map.of("error", "Máximo de 5 links por relatório"));
                    return;
                }
                for (String link : links) {
                    if (!link.startsWith("http://") && !link.startsWith("https://")) {
                        ctx.status(400).json(Map.of("error", "Link inválido: " + link));
                        return;
                    }
                }

                title = title.length() > 200 ? title.substring(0, 200) : title;
                description = description.length() > 2000 ? description.substring(0, 2000) : description;

                String activityDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                var guild = AuthController.getMainGuild(jda);
                Member member = guild.getMemberById(userId);

                // Validar roleId vs maior cargo
                if (member != null) {
                    Set<String> memberRoleIds = new HashSet<>();
                    for (Role r : member.getRoles()) {
                        memberRoleIds.add(r.getId());
                    }
                    String allowedRoleId = null;
                    for (String key : ROLE_PRIORITY_ORDER) {
                        String value = BotConfig.get(key);
                        if (value == null || value.isEmpty()) continue;
                        for (String id : value.split(",")) {
                            String rid = id.trim();
                            if (memberRoleIds.contains(rid)) {
                                allowedRoleId = rid;
                                break;
                            }
                        }
                        if (allowedRoleId != null) break;
                    }
                    if (allowedRoleId == null || !allowedRoleId.equals(roleId)) {
                        ctx.status(403).json(Map.of("error", "Você só pode registrar relatórios com seu cargo mais alto"));
                        return;
                    }
                }

                // Aplicar corretor ortográfico (regras, sem IA)
                title = TextFormatter.formatTitle(title);
                description = TextFormatter.format(description);

                Role role = guild.getRoleById(roleId);
                String roleName = role != null ? role.getName() : "Desconhecido";
                String authorName = member != null ? member.getEffectiveName() : username;

                int reportId = DatabaseManager.createReport(userId, authorName, roleId, roleName, title, description, activityDate);
                if (reportId < 0) {
                    ctx.status(500).json(Map.of("error", "Erro ao salvar relatório"));
                    return;
                }

                // Salvar arquivos no disco e registrar no banco
                for (UploadedFile file : files) {
                    String ext = getExtension(file.filename());
                    String storedName = reportId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
                    Path dest = UPLOADS_DIR.resolve(storedName);
                    try (InputStream is = file.content()) {
                        Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                    String type = ALLOWED_IMAGE_TYPES.contains(file.contentType()) ? "image" : "video";
                    DatabaseManager.addReportAttachment(reportId, type, storedName, file.filename(), null);
                }

                // Salvar links
                for (String link : links) {
                    DatabaseManager.addReportAttachment(reportId, "link", null, null, link);
                }

                LOGGER.info("[REPORTS] {} ({}) criou relatório #{}: {} ({} arquivos, {} links)",
                        authorName, userId, reportId, title, files.size(), links.size());
                ctx.status(201).json(Map.of("id", reportId, "message", "Relatório criado com sucesso"));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao criar relatório", e);
                ctx.status(500).json(Map.of("error", "Erro ao criar relatório"));
            }
        });

        // GET /api/reports/uploads/{filename} — servir arquivo de anexo
        app.get("/api/reports/uploads/{filename}", ctx -> {
            try {
                String filename = ctx.pathParam("filename");
                // Sanitizar — impedir path traversal
                if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                    ctx.status(400).json(Map.of("error", "Nome de arquivo inválido"));
                    return;
                }
                Path filePath = UPLOADS_DIR.resolve(filename);
                if (!Files.exists(filePath)) {
                    ctx.status(404).json(Map.of("error", "Arquivo não encontrado"));
                    return;
                }
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) contentType = "application/octet-stream";
                ctx.contentType(contentType);
                ctx.header("Cache-Control", "public, max-age=86400");
                ctx.result(Files.newInputStream(filePath));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao servir arquivo de anexo", e);
                ctx.status(500).json(Map.of("error", "Erro ao servir arquivo"));
            }
        });

        // GET /api/reports/monthly-dashboard?month=2026-03 — dashboard mensal (JSON)
        app.get("/api/reports/monthly-dashboard", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                YearMonth month;
                if (monthParam != null && monthParam.matches("\\d{4}-\\d{2}")) {
                    month = YearMonth.parse(monthParam);
                } else {
                    month = YearMonth.now();
                }

                List<Map<String, Object>> reports = DatabaseManager.getReportsByMonth(month.toString());

                // Enriquecer com avatar Discord
                var guild = AuthController.getMainGuild(jda);
                for (var report : reports) {
                    String authorId = (String) report.get("authorId");
                    Member m = guild.getMemberById(authorId);
                    if (m != null) {
                        report.put("authorAvatar", m.getUser().getAvatarUrl());
                        report.put("authorDisplayName", m.getEffectiveName());
                    }
                }

                // --- Estatísticas por cargo ---
                Map<String, Map<String, Object>> byRole = new LinkedHashMap<>();
                for (var r : reports) {
                    String roleId = (String) r.get("roleId");
                    String roleName = (String) r.get("roleName");
                    byRole.computeIfAbsent(roleId, k -> {
                        Map<String, Object> rm = new LinkedHashMap<>();
                        rm.put("roleId", roleId);
                        rm.put("roleName", roleName);
                        rm.put("count", 0);
                        // Buscar cor do cargo
                        Role role = guild.getRoleById(roleId);
                        rm.put("color", role != null ? String.format("#%06x", role.getColorRaw()) : "#c8a84e");
                        return rm;
                    });
                    Map<String, Object> rm = byRole.get(roleId);
                    rm.put("count", (int) rm.get("count") + 1);
                }

                // --- Estatísticas por autor ---
                Map<String, Map<String, Object>> byAuthor = new LinkedHashMap<>();
                for (var r : reports) {
                    String authorId = (String) r.get("authorId");
                    String authorName = r.get("authorDisplayName") != null
                            ? (String) r.get("authorDisplayName")
                            : (String) r.get("authorName");
                    String authorAvatar = (String) r.get("authorAvatar");
                    String roleName = (String) r.get("roleName");
                    String roleId = (String) r.get("roleId");
                    byAuthor.computeIfAbsent(authorId, k -> {
                        Map<String, Object> am = new LinkedHashMap<>();
                        am.put("authorId", authorId);
                        am.put("authorName", authorName);
                        am.put("authorAvatar", authorAvatar);
                        am.put("roleName", roleName);
                        am.put("roleId", roleId);
                        Role role = guild.getRoleById(roleId);
                        am.put("roleColor", role != null ? String.format("#%06x", role.getColorRaw()) : "#c8a84e");
                        am.put("count", 0);
                        return am;
                    });
                    Map<String, Object> am = byAuthor.get(authorId);
                    am.put("count", (int) am.get("count") + 1);
                }

                // --- Atividade por dia ---
                Map<String, Integer> byDay = new TreeMap<>();
                for (var r : reports) {
                    String date = ((String) r.get("activityDate"));
                    if (date != null && date.length() >= 10) {
                        String day = date.substring(0, 10);
                        byDay.merge(day, 1, Integer::sum);
                    }
                }
                List<Map<String, Object>> dailyActivity = new ArrayList<>();
                for (var entry : byDay.entrySet()) {
                    dailyActivity.add(Map.of("date", entry.getKey(), "count", entry.getValue()));
                }

                // --- Ordenar autores por contagem desc ---
                List<Map<String, Object>> authorRanking = new ArrayList<>(byAuthor.values());
                authorRanking.sort((a, b) -> (int) b.get("count") - (int) a.get("count"));

                // --- Montar resposta ---
                int totalDaysInMonth = month.lengthOfMonth();
                int activeDays = byDay.size();

                ctx.json(Map.of(
                    "month", month.toString(),
                    "totalReports", reports.size(),
                    "totalDaysInMonth", totalDaysInMonth,
                    "activeDays", activeDays,
                    "totalContributors", byAuthor.size(),
                    "byRole", new ArrayList<>(byRole.values()),
                    "byAuthor", authorRanking,
                    "dailyActivity", dailyActivity
                ));
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao gerar dashboard mensal", e);
                ctx.status(500).json(Map.of("error", "Erro ao gerar dashboard"));
            }
        });

        // GET /api/reports/monthly-pdf?month=2026-03 — gerar PDF mensal
        app.get("/api/reports/monthly-pdf", ctx -> {
            try {
                String monthParam = ctx.queryParam("month");
                YearMonth month;
                if (monthParam != null && monthParam.matches("\\d{4}-\\d{2}")) {
                    month = YearMonth.parse(monthParam);
                } else {
                    month = YearMonth.now();
                }

                List<Map<String, Object>> reports = DatabaseManager.getReportsByMonth(month.toString());
                if (reports.isEmpty()) {
                    ctx.status(404).json(Map.of("error", "Nenhum relatório encontrado para " + month));
                    return;
                }

                // Enriquecer com avatar e anexos
                List<Integer> ids = reports.stream().map(r -> (Integer) r.get("id")).toList();
                var attachmentsMap = DatabaseManager.getAttachmentsForReports(ids);
                var guild = AuthController.getMainGuild(jda);
                for (var report : reports) {
                    String authorId = (String) report.get("authorId");
                    Member m = guild.getMemberById(authorId);
                    if (m != null) {
                        report.put("authorDisplayName", m.getEffectiveName());
                    }
                    int id = (Integer) report.get("id");
                    report.put("attachments", attachmentsMap.getOrDefault(id, List.of()));
                }

                byte[] pdf = ReportPdfGenerator.generate(reports, month, UPLOADS_DIR);
                ctx.contentType("application/pdf");
                ctx.header("Content-Disposition", "attachment; filename=\"relatorio-staff-" + month + ".pdf\"");
                ctx.result(pdf);
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao gerar PDF mensal", e);
                ctx.status(500).json(Map.of("error", "Erro ao gerar PDF"));
            }
        });

        // DELETE /api/reports/{id} — deletar relatório (apenas o autor pode)
        app.delete("/api/reports/{id}", ctx -> {
            try {
                String userId = ctx.attribute("userId");
                int reportId;
                try {
                    reportId = Integer.parseInt(ctx.pathParam("id"));
                } catch (NumberFormatException e) {
                    ctx.status(400).json(Map.of("error", "ID inválido"));
                    return;
                }

                // Buscar anexos para deletar arquivos do disco
                var attachments = DatabaseManager.getReportAttachments(reportId);

                boolean deleted = DatabaseManager.deleteReport(reportId, userId);
                if (deleted) {
                    // Deletar arquivos do disco
                    for (var att : attachments) {
                        String filename = (String) att.get("filename");
                        if (filename != null) {
                            try {
                                Files.deleteIfExists(UPLOADS_DIR.resolve(filename));
                            } catch (IOException e) {
                                LOGGER.warn("[REPORTS] Falha ao deletar arquivo: {}", filename);
                            }
                        }
                    }
                    DatabaseManager.deleteAttachmentsByReport(reportId);
                    ctx.json(Map.of("message", "Relatório removido"));
                } else {
                    ctx.status(404).json(Map.of("error", "Relatório não encontrado ou você não é o autor"));
                }
            } catch (Exception e) {
                LOGGER.error("[WEB] Erro ao deletar relatório", e);
                ctx.status(500).json(Map.of("error", "Erro ao deletar relatório"));
            }
        });
    }

    private static String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }

    private record ReportBody(String title, String description, String roleId, List<String> links) {}
}
