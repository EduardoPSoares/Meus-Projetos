package com.midgardbot.web.routes;

import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.features.whitelist.WhitelistConfig;
import com.midgardbot.web.auth.AuthController;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Rotas de gerenciamento de Whitelist.
 */
public class WhitelistRoutes {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistRoutes.class);

    public static void register(Javalin app, JDA jda) {

        // GET /api/whitelists — lista todas as whitelists com filtro, paginação e busca
        app.get("/api/whitelists", ctx -> {
            String filter = ctx.queryParam("status"); // pending, approved, rejected, all
            String search = ctx.queryParam("search"); // busca por nick, discordName ou ID
            int page = 1;
            int limit = 50;
            try { page = Math.max(1, Integer.parseInt(Objects.requireNonNullElse(ctx.queryParam("page"), "1"))); } catch (Exception ignored) {}
            try { limit = Math.min(100, Math.max(1, Integer.parseInt(Objects.requireNonNullElse(ctx.queryParam("limit"), "50")))); } catch (Exception ignored) {}

            var allStatus = DataManager.getAllStatus();
            var pending = DataManager.getAllPendingWhitelists();

            List<Map<String, Object>> results = new ArrayList<>();

            // Montar lista a partir do allStatus (que contém todas as whitelists com status real)
            for (var entry : allStatus.entrySet()) {
                String discordId = entry.getKey();
                WhitelistStatusInfo info = entry.getValue();
                String statusStr = resolveFilterStatus(info.status);

                // Pular PENDING sem respostas (apenas aceitou termos, nunca completou formulário)
                if (info.status == WhitelistStatus.PENDING
                        && (info.answers == null || info.answers.isEmpty() || "{}".equals(info.answers))) {
                    continue;
                }

                if (filter != null && !"all".equals(filter) && !filter.equals(statusStr)) continue;

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("discordId", discordId);
                item.put("status", statusStr);
                item.put("statusLabel", info.status.label);
                item.put("statusIcon", info.status.icon);
                item.put("timestamp", info.timestamp);
                item.put("reason", info.reason);
                item.put("nickname", info.nickname);
                item.put("staffId", info.staffId);

                // Se tem dados pendentes, anexar
                if (pending.containsKey(discordId)) {
                    item.put("data", pending.get(discordId));
                }

                // Tentar resolver nome Discord
                try {
                    var member = AuthController.getMainGuild(jda).getMemberById(discordId);
                    if (member != null) {
                        item.put("discordName", member.getUser().getName());
                        item.put("discordDisplayName", member.getEffectiveName());
                        item.put("discordAvatar", member.getUser().getAvatarUrl());
                    }
                } catch (Exception ignored) {}

                // Filtrar por busca
                if (search != null && !search.isBlank()) {
                    String q = search.toLowerCase();
                    String nick = info.nickname != null ? info.nickname.toLowerCase() : "";
                    String dName = item.containsKey("discordName") ? ((String) item.get("discordName")).toLowerCase() : "";
                    String dDisplay = item.containsKey("discordDisplayName") ? ((String) item.get("discordDisplayName")).toLowerCase() : "";
                    if (!nick.contains(q) && !dName.contains(q) && !dDisplay.contains(q) && !discordId.contains(q)) {
                        continue;
                    }
                }

                results.add(item);
            }

            int total = results.size();
            int pages = Math.max(1, (int) Math.ceil((double) total / limit));
            int fromIndex = Math.min((page - 1) * limit, total);
            int toIndex = Math.min(fromIndex + limit, total);
            List<Map<String, Object>> paged = results.subList(fromIndex, toIndex);

            ctx.json(Map.of(
                    "total", total,
                    "page", page,
                    "pages", pages,
                    "whitelists", paged
            ));
        });

        // GET /api/whitelists/:id — detalhes de uma whitelist específica
        app.get("/api/whitelists/{id}", ctx -> {
            String discordId = ctx.pathParam("id");
            if (!com.midgardbot.web.security.SecurityMiddleware.isValidDiscordId(discordId)) {
                ctx.status(400).json(Map.of("error", "ID inválido"));
                return;
            }

            var pendingData = DataManager.getPendingWhitelist(discordId);
            WhitelistStatusInfo statusInfo = DataManager.getStatus(discordId);
            var history = DataManager.getHistory(discordId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("discordId", discordId);
            if (statusInfo != null) {
                result.put("status", resolveFilterStatus(statusInfo.status));
                result.put("statusLabel", statusInfo.status.label);
                result.put("statusIcon", statusInfo.status.icon);
                result.put("timestamp", statusInfo.timestamp);
                result.put("reason", statusInfo.reason);
                result.put("nickname", statusInfo.nickname);
                result.put("staffId", statusInfo.staffId);
                if (statusInfo.termsAccepted) {
                    result.put("termsAccepted", true);
                }
            } else {
                result.put("status", "unknown");
            }
            result.put("history", history);

            // Montar respostas completas (pergunta + resposta) organizadas por seção
            Map<String, String> answers = null;
            if (statusInfo != null && statusInfo.answers != null && !statusInfo.answers.isEmpty()) {
                try {
                    answers = new Gson().fromJson(statusInfo.answers, new TypeToken<Map<String, String>>(){}.getType());
                } catch (Exception ignored) {}
            }
            if (answers == null && pendingData != null) {
                answers = pendingData;
            }

            if (answers != null) {
                String aiScore = answers.get("_ai_score");
                if (aiScore != null) {
                    result.put("aiScore", aiScore);
                }

                List<Map<String, Object>> sections = new ArrayList<>();
                for (int page = 0; page < 3; page++) {
                    Map<String, Object> section = new LinkedHashMap<>();
                    section.put("title", WhitelistConfig.getPageTitle(page));
                    Map<String, String> questions = WhitelistConfig.getQuestionsByPage(page);

                    List<Map<String, String>> fields = new ArrayList<>();
                    for (var qEntry : questions.entrySet()) {
                        String qKey = qEntry.getKey();
                        String qText = qEntry.getValue();
                        String answer = answers.getOrDefault(qKey, null);
                        fields.add(Map.of(
                            "key", qKey,
                            "question", qText,
                            "answer", answer != null ? answer : ""
                        ));
                    }
                    section.put("fields", fields);
                    sections.add(section);
                }
                result.put("sections", sections);
            }

            // Resolver staff responsável
            if (statusInfo != null && statusInfo.staffId != null && !statusInfo.staffId.isEmpty()) {
                try {
                    var staffMember = AuthController.getMainGuild(jda).getMemberById(statusInfo.staffId);
                    if (staffMember != null) {
                        result.put("staffName", staffMember.getUser().getName());
                        result.put("staffAvatar", staffMember.getUser().getAvatarUrl());
                    }
                } catch (Exception ignored) {}
            }

            try {
                var member = AuthController.getMainGuild(jda).getMemberById(discordId);
                if (member != null) {
                    result.put("discordName", member.getUser().getName());
                    result.put("discordAvatar", member.getUser().getAvatarUrl());
                }
            } catch (Exception ignored) {}

            ctx.json(result);
        });

        // GET /api/whitelists/stats — estatísticas gerais
        app.get("/api/whitelists/stats", ctx -> {
            var allStatus = DataManager.getAllStatus();
            var pending = DataManager.getAllPendingWhitelists();

            long approved = allStatus.values().stream()
                    .filter(s -> s.status == WhitelistStatus.APPROVED || s.status == WhitelistStatus.EXCELLENT)
                    .count();
            long rejected = allStatus.values().stream()
                    .filter(s -> s.status == WhitelistStatus.REJECTED)
                    .count();
            // Pendentes reais: status pendente E tem respostas preenchidas no status
            long pendingReal = allStatus.values().stream()
                    .filter(s -> {
                        if (s.status != WhitelistStatus.PENDING && s.status != WhitelistStatus.REVIEWING
                                && s.status != WhitelistStatus.NEEDS_REVIEW && s.status != WhitelistStatus.FLAGGED
                                && s.status != WhitelistStatus.PRIORITY && s.status != WhitelistStatus.STANDBY) return false;
                        return s.answers != null && !s.answers.isEmpty() && !"{}" .equals(s.answers);
                    })
                    .count();

            ctx.json(Map.of(
                    "total", approved + rejected + pendingReal,
                    "pending", pendingReal,
                    "approved", approved,
                    "rejected", rejected
            ));
        });
    }

    /**
     * Mapeia o enum WhitelistStatus para a categoria de filtro do frontend.
     */
    private static String resolveFilterStatus(WhitelistStatus status) {
        return switch (status) {
            case APPROVED, EXCELLENT -> "approved";
            case REJECTED -> "rejected";
            case PENDING, REVIEWING, NEEDS_REVIEW, FLAGGED, PRIORITY, STANDBY -> "pending";
        };
    }
}
