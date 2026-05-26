package com.midgardbot.commands.handlers;

import com.midgardbot.config.BotConfig;
import com.midgardbot.config.MessagesConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.features.whitelist.ReviewManager;
import com.midgardbot.features.whitelist.ReviewPanelManager;
import com.midgardbot.features.whitelist.WhitelistConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handler para o fluxo de revisão de whitelist pela staff.
 * Inclui: fila de análise, aprovação, reprovação, paginação, logs.
 */
public final class WhitelistReviewHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistReviewHandler.class);

    private WhitelistReviewHandler() {}

    // ========================
    //   BUTTON INTERACTIONS
    // ========================

    public static boolean handleButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("btn_start_review")) { handleStartReview(event); return true; }
        if (id.equals("btn_review_next")) { handleReviewNext(event); return true; }

        if (id.startsWith("btn_wl_next:") || id.startsWith("btn_wl_prev:")) { handlePageNavigation(event); return true; }
        if (id.startsWith("btn_log_next") || id.startsWith("btn_log_prev")) { handleLogNavigation(event); return true; }

        if (id.startsWith("btn_whitelist_approve:")) { handleWhitelistApproval(event); return true; }
        if (id.startsWith("confirm_approve:")) { handleConfirmedApproval(event); return true; }
        if (id.startsWith("cancel_approve:")) { handleCancelApproval(event); return true; }
        if (id.startsWith("btn_review_exit:")) { handleReviewExit(event); return true; }

        if (id.startsWith("btn_whitelist_reject:")) { handleWhitelistReject(event); return true; }
        if (id.startsWith("reject_predefined:")) { handleRejectPredefined(event); return true; }
        if (id.startsWith("reject_custom:")) { handleRejectCustom(event); return true; }
        if (id.startsWith("cancel_reject:")) { event.deferEdit().queue(hook -> hook.deleteOriginal().queue()); return true; }
        if (id.startsWith("select_reject_reason:")) { handleSelectRejectReason(event); return true; }

        return false;
    }

    // ========================
    //    MODAL INTERACTIONS
    // ========================

    public static boolean handleModal(ModalInteractionEvent event) {
        if (event.getModalId().startsWith("modal_custom_reject:")) {
            String targetUserId = event.getModalId().split(":")[1];
            String customReason = event.getValue("custom_reason").getAsString();
            handleWhitelistRejection(targetUserId, customReason, event);
            return true;
        }
        return false;
    }

    // ========================
    //   REVIEW QUEUE LOGIC
    // ========================

    private static void handleStartReview(ButtonInteractionEvent event) {
        ReviewManager.cleanExpiredLocks();

        Map<String, Map<String, String>> allPending = DataManager.getAllPendingWhitelists();
        if (allPending.isEmpty()) {
            event.reply("✅ Não há whitelists pendentes na fila!").setEphemeral(true).queue();
            return;
        }

        Map.Entry<String, Map<String, String>> entry = findNextEntry(allPending, event.getUser().getId());

        if (entry == null) {
            event.reply("⚠️ Todas as whitelists pendentes já estão sendo analisadas por outros staffs.").setEphemeral(true).queue();
            return;
        }

        String nextUserId = entry.getKey();
        Map<String, String> answers = entry.getValue();

        ReviewManager.startReview(nextUserId, event.getUser().getId());
        ReviewPanelManager.updatePanel(event.getJDA());

        deleteStaffMessage(answers, event.getJDA());

        event.deferReply(true).queue();
        event.getJDA().retrieveUserById(nextUserId).queue(user -> {
            new com.midgardbot.commands.impl.ReviewCommand().sendReviewEmbed(event, nextUserId, user, answers, allPending.size());
        }, failure -> {
            new com.midgardbot.commands.impl.ReviewCommand().sendReviewEmbed(event, nextUserId, null, answers, allPending.size());
        });
    }

    private static void handleReviewNext(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        event.getMessage().delete().queue(s -> {}, e -> {});

        ReviewManager.cleanExpiredLocks();

        Map<String, Map<String, String>> allPending = DataManager.getAllPendingWhitelists();
        if (allPending.isEmpty()) {
            event.getHook().sendMessage("✅ Não há mais whitelists pendentes na fila!").setEphemeral(true).queue();
            return;
        }

        Map.Entry<String, Map<String, String>> entry = findNextEntry(allPending, event.getUser().getId());

        if (entry == null) {
            event.getHook().sendMessage("⚠️ Todas as whitelists pendentes já estão sendo analisadas por outros staffs.").setEphemeral(true).queue();
            return;
        }

        String nextUserId = entry.getKey();
        Map<String, String> answers = entry.getValue();

        ReviewManager.startReview(nextUserId, event.getUser().getId());
        ReviewPanelManager.updatePanel(event.getJDA());

        deleteStaffMessage(answers, event.getJDA());

        event.getJDA().retrieveUserById(nextUserId).queue(user -> {
            new com.midgardbot.commands.impl.ReviewCommand().sendReviewEmbed(event, nextUserId, user, answers, allPending.size());
        }, failure -> {
            new com.midgardbot.commands.impl.ReviewCommand().sendReviewEmbed(event, nextUserId, null, answers, allPending.size());
        });
    }

    private static Map.Entry<String, Map<String, String>> findNextEntry(Map<String, Map<String, String>> allPending, String staffUserId) {
        List<Map.Entry<String, Map<String, String>>> sortedPending = allPending.entrySet().stream()
            .sorted((e1, e2) -> {
                long t1 = Long.MAX_VALUE, t2 = Long.MAX_VALUE;
                try {
                    if (e1.getValue().containsKey("_timestamp")) t1 = Long.parseLong(e1.getValue().get("_timestamp"));
                    if (e2.getValue().containsKey("_timestamp")) t2 = Long.parseLong(e2.getValue().get("_timestamp"));
                } catch (Exception ex) { LOGGER.debug("Erro ao parsear timestamp para ordenação de whitelist", ex); }
                return Long.compare(t1, t2);
            })
            .collect(Collectors.toList());

        // 1. Resume: check if staff already has a lock
        for (Map.Entry<String, Map<String, String>> e : sortedPending) {
            String reviewerId = ReviewManager.getReviewer(e.getKey());
            if (reviewerId != null && reviewerId.equals(staffUserId)) return e;
        }

        // 2. Find next free entry
        for (Map.Entry<String, Map<String, String>> e : sortedPending) {
            WhitelistStatusInfo info = DataManager.getStatus(e.getKey());
            if (info != null && (info.status == WhitelistStatus.APPROVED || info.status == WhitelistStatus.REJECTED)) {
                DataManager.removePendingWhitelist(e.getKey());
                continue;
            }
            if (!ReviewManager.isUnderReview(e.getKey())) return e;
        }

        return null;
    }

    private static void deleteStaffMessage(Map<String, String> answers, net.dv8tion.jda.api.JDA jda) {
        if (answers.containsKey("_staff_message_id")) {
            String msgId = answers.get("_staff_message_id");
            String staffChannelId = BotConfig.getStaffChannelId();
            TextChannel staffChannel = (staffChannelId != null) ? jda.getTextChannelById(staffChannelId) : null;
            if (staffChannel != null) {
                staffChannel.deleteMessageById(msgId).queue(s -> {}, e -> {});
            }
        }
    }

    // ========================
    //     PAGE NAVIGATION
    // ========================

    private static void handlePageNavigation(ButtonInteractionEvent event) {
        if (!InteractionUtils.canReviewWhitelist(event.getMember())) {
            event.reply("❌ Sem permissão.").setEphemeral(true).queue();
            return;
        }

        String id = event.getComponentId();
        String[] parts = id.split(":");
        String action = parts[0];
        String targetUserId = parts[1];

        int currentPage = 0;
        try {
            if (!event.getMessage().getEmbeds().isEmpty()) {
                MessageEmbed.Footer footer = event.getMessage().getEmbeds().get(0).getFooter();
                if (footer != null && footer.getText() != null && footer.getText().startsWith("Página ")) {
                    String pageNum = footer.getText().split(" ")[1].split("/")[0];
                    currentPage = Integer.parseInt(pageNum) - 1;
                }
            }
        } catch (Exception e) {
            currentPage = InteractionUtils.staffViewPages.getOrDefault(event.getMessageId(), 0);
        }

        int newPage = action.equals("btn_wl_next") ? currentPage + 1 : currentPage - 1;
        if (newPage < 0) newPage = 0;
        if (newPage > 2) newPage = 2;

        int finalNewPage = newPage;
        event.getJDA().retrieveUserById(targetUserId).queue(
            user -> sendWhitelistPage(event.getChannel().asTextChannel(), targetUserId, user, finalNewPage, event.getMessageId(), event),
            error -> sendWhitelistPage(event.getChannel().asTextChannel(), targetUserId, null, finalNewPage, event.getMessageId(), event)
        );
    }

    private static void handleLogNavigation(ButtonInteractionEvent event) {
        String messageId = event.getMessageId();
        List<MessageEmbed> pages = InteractionUtils.logCache.get(messageId);

        if (pages == null) {
            event.replyEmbeds(EmbedUtils.createError("Log Expirado",
                "Este log é muito antigo e a paginação não está mais disponível.",
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        int currentPage = InteractionUtils.logViewPages.getOrDefault(messageId, 0);
        int newPage = event.getComponentId().startsWith("btn_log_next") ? currentPage + 1 : currentPage - 1;
        if (newPage < 0) newPage = 0;
        if (newPage >= pages.size()) newPage = pages.size() - 1;

        InteractionUtils.logViewPages.put(messageId, newPage);

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.secondary("btn_log_prev", "⬅️ Anterior").withDisabled(newPage == 0));
        buttons.add(Button.secondary("btn_log_next", "Próxima ➡️").withDisabled(newPage == pages.size() - 1));

        event.editMessageEmbeds(pages.get(newPage)).setActionRow(buttons).queue();
    }

    // ========================
    //      APPROVAL FLOW
    // ========================

    private static void handleWhitelistApproval(ButtonInteractionEvent event) {
        if (!InteractionUtils.canReviewWhitelist(event.getMember())) {
            event.reply("❌ Sem permissão.").setEphemeral(true).queue();
            return;
        }

        String targetId = event.getComponentId().split(":")[1];

        WhitelistStatusInfo status = DataManager.getStatus(targetId);
        if (status != null && (status.status == WhitelistStatus.APPROVED || status.status == WhitelistStatus.REJECTED)) {
            sendAlreadyProcessed(event, status, targetId);
            return;
        }

        EmbedBuilder confirmEmbed = new EmbedBuilder()
            .setTitle("Confirmação de Aprovação")
            .setDescription("Você tem certeza que deseja **APROVAR** a whitelist de <@" + targetId + ">?")
            .setColor(Color.YELLOW);

        if (event.isAcknowledged()) {
            event.getHook().editOriginalEmbeds(confirmEmbed.build())
                .setActionRow(Button.success("confirm_approve:" + targetId, "✅ Confirmar"), Button.danger("cancel_approve:" + targetId, "❌ Cancelar"))
                .queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_WEBHOOK));
        } else {
            event.editMessageEmbeds(confirmEmbed.build())
                .setActionRow(Button.success("confirm_approve:" + targetId, "✅ Confirmar"), Button.danger("cancel_approve:" + targetId, "❌ Cancelar"))
                .queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_WEBHOOK));
        }
    }

    private static void handleConfirmedApproval(ButtonInteractionEvent event) {
        if (!InteractionUtils.canReviewWhitelist(event.getMember())) {
            event.reply("❌ Sem permissão.").setEphemeral(true).queue();
            return;
        }

        String targetId = event.getComponentId().split(":")[1];

        WhitelistStatusInfo status = DataManager.getStatus(targetId);
        if (status != null && (status.status == WhitelistStatus.APPROVED || status.status == WhitelistStatus.REJECTED)) {
            event.replyEmbeds(EmbedUtils.createError("Já Processado",
                "Esta whitelist já foi " + (status.status == WhitelistStatus.APPROVED ? "aprovada" : "reprovada") + ".",
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue(hook -> hook.deleteOriginal().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));

        event.getJDA().retrieveUserById(targetId).queue(
            user -> finalizeApproval(targetId, user, event),
            failure -> finalizeApproval(targetId, null, event)
        );
    }

    private static void handleCancelApproval(ButtonInteractionEvent event) {
        String targetUserId = event.getComponentId().split(":")[1];
        int page = InteractionUtils.staffViewPages.getOrDefault(event.getMessageId(), 0);

        event.getJDA().retrieveUserById(targetUserId).queue(
            user -> sendWhitelistPage(event.getChannel().asTextChannel(), targetUserId, user, page, event.getMessageId(), event),
            error -> sendWhitelistPage(event.getChannel().asTextChannel(), targetUserId, null, page, event.getMessageId(), event)
        );
    }

    private static void handleReviewExit(ButtonInteractionEvent event) {
        String targetUserId = event.getComponentId().split(":")[1];
        ReviewManager.endReview(targetUserId);
        ReviewPanelManager.updatePanel(event.getJDA());
        event.deferEdit().queue(hook -> hook.deleteOriginal().queue());
    }

    private static void finalizeApproval(String targetId, User user, ButtonInteractionEvent event) {
        ReviewManager.endReview(targetId);
        ReviewPanelManager.updatePanel(event.getJDA());

        String resultsChannelId = BotConfig.getResultsChannelId();
        String logChannelId = BotConfig.getLogChannelId();

        Map<String, String> answers = DataManager.getPendingWhitelist(targetId);
        if (answers == null) {
            answers = new HashMap<>();
            answers.put("⚠️ Aviso", "As respostas originais não estão disponíveis (Cache limpo ou Bot reiniciado).");
        }

        TextChannel resultsChannel = (resultsChannelId != null && !resultsChannelId.equals("000000000000000000"))
            ? event.getJDA().getTextChannelById(resultsChannelId) : null;
        TextChannel logChannel = (logChannelId != null && !logChannelId.equals("000000000000000000"))
            ? event.getJDA().getTextChannelById(logChannelId) : null;

        if (logChannel != null) {
            List<MessageEmbed> logEmbeds = createLogEmbeds(user, event.getUser(), "Log: Whitelist Aprovada", "Aprovado", answers, EmbedUtils.COLOR_SUCCESS, event.getJDA().getSelfUser());
            if (logEmbeds.size() > 1) {
                List<Button> buttons = new ArrayList<>();
                buttons.add(Button.secondary("btn_log_prev", "⬅️ Anterior").withDisabled(true));
                buttons.add(Button.secondary("btn_log_next", "Próxima ➡️").withDisabled(false));
                logChannel.sendMessageEmbeds(logEmbeds.get(0)).setActionRow(buttons).queue(msg -> {
                    InteractionUtils.logCache.put(msg.getId(), logEmbeds);
                    InteractionUtils.logViewPages.put(msg.getId(), 0);
                });
            } else {
                logChannel.sendMessageEmbeds(logEmbeds.get(0)).queue();
            }
        }

        DataManager.removePendingWhitelist(targetId);
        ReviewPanelManager.updatePanel(event.getJDA());

        String staffMessageId = InteractionUtils.staffMessages.remove(targetId);
        if (staffMessageId != null) InteractionUtils.staffViewPages.remove(staffMessageId);

        String nickname = answers.getOrDefault("q1_nick", null);
        String answersJson = new com.google.gson.Gson().toJson(answers);

        WhitelistStatusInfo current = DataManager.getStatus(targetId);
        boolean terms = current != null && current.termsAccepted;
        DataManager.setStatus(targetId, WhitelistStatus.APPROVED, null, nickname, answersJson, terms, event.getUser().getId());
        DataManager.incrementStaffApproval(event.getUser().getId());

        // DM de aprovação
        if (user != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("user", user.getAsMention());
            placeholders.put("user_avatar", user.getEffectiveAvatarUrl());
            EmbedBuilder dmEmbed = MessagesConfig.buildEmbed(MessagesConfig.get().whitelist.approved, placeholders);
            dmEmbed.setFooter(event.getJDA().getSelfUser().getName(), event.getJDA().getSelfUser().getEffectiveAvatarUrl());
            File aprovadoFile = new File("data/aprovado.png");
            if (aprovadoFile.exists()) {
                InteractionUtils.sendDM(user, dmEmbed.build(), FileUpload.fromData(aprovadoFile, "aprovado.png"));
            } else {
                InteractionUtils.sendDM(user, dmEmbed.build());
            }
        }

        // Auto-Role
        if (event.getGuild() != null && user != null) {
            String roleId = BotConfig.getCitizenRoleId();
            if (roleId != null && !roleId.isEmpty()) {
                net.dv8tion.jda.api.entities.Role role = event.getGuild().getRoleById(roleId);
                if (role != null) {
                    if (event.getGuild().getSelfMember().canInteract(role)) {
                        event.getGuild().addRoleToMember(user, role).queue(
                            s -> LOGGER.info("Cargo adicionado para {}", user.getName()),
                            e -> LOGGER.error("Erro ao adicionar cargo para {}", user.getName(), e)
                        );
                    } else {
                        LOGGER.warn("⚠️ Não foi possível dar o cargo para {}: Hierarquia insuficiente.", user.getName());
                        event.getChannel().sendMessage("⚠️ **Aviso:** Não consegui dar o cargo de Cidadão. Verifique se meu cargo está acima do cargo de Cidadão.").queue();
                    }
                }
            }
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("user", user != null ? user.getAsMention() : "`" + targetId + "`");
        placeholders.put("user_avatar", user != null ? user.getEffectiveAvatarUrl() : "");

        event.getHook().deleteOriginal().queue(s -> {}, e -> {});

        if (resultsChannel != null) {
            EmbedBuilder resultEmbed = MessagesConfig.buildEmbed(MessagesConfig.get().whitelist.approved, placeholders);
            resultEmbed.setFooter(event.getJDA().getSelfUser().getName(), event.getJDA().getSelfUser().getEffectiveAvatarUrl());
            File aprovadoFile = new File("data/aprovado.png");
            if (aprovadoFile.exists()) {
                FileUpload upload = FileUpload.fromData(aprovadoFile, "aprovado.png");
                if (user != null) resultsChannel.sendMessage(user.getAsMention()).setEmbeds(resultEmbed.build()).addFiles(upload).queue();
                else resultsChannel.sendMessage("Usuário: `" + targetId + "`").setEmbeds(resultEmbed.build()).addFiles(upload).queue();
            } else {
                if (user != null) resultsChannel.sendMessage(user.getAsMention()).setEmbeds(resultEmbed.build()).queue();
                else resultsChannel.sendMessage("Usuário: `" + targetId + "`").setEmbeds(resultEmbed.build()).queue();
            }
        }

        if (staffMessageId != null) {
            String staffChannelId = BotConfig.getStaffChannelId();
            TextChannel staffChannel = (staffChannelId != null) ? event.getJDA().getTextChannelById(staffChannelId) : null;
            if (staffChannel != null) staffChannel.deleteMessageById(staffMessageId).queue(s -> {}, e -> {});
            else event.getChannel().deleteMessageById(staffMessageId).queue(s -> {}, e -> {});
        }

        // Auto-Next
        Map<String, Map<String, String>> pending = DataManager.getAllPendingWhitelists();
        if (!pending.isEmpty()) {
            event.getHook().sendMessageEmbeds(new EmbedBuilder()
                .setColor(Color.CYAN)
                .setDescription("✅ **Aprovado com sucesso!**\nAinda existem **" + pending.size() + "** whitelists na fila.")
                .build())
                .addActionRow(Button.primary("btn_review_next", "➡️ Analisar Próxima"))
                .setEphemeral(true)
                .queue();
        }
    }

    // ========================
    //     REJECTION FLOW
    // ========================

    private static void handleWhitelistReject(ButtonInteractionEvent event) {
        if (!InteractionUtils.canReviewWhitelist(event.getMember())) {
            event.reply("❌ Sem permissão.").setEphemeral(true).queue();
            return;
        }
        String targetUserId = event.getComponentId().split(":")[1];
        event.getJDA().retrieveUserById(targetUserId).queue(
            user -> sendRejectReasonMenu(event, targetUserId, user),
            error -> sendRejectReasonMenu(event, targetUserId, null)
        );
    }

    private static void handleRejectPredefined(ButtonInteractionEvent event) {
        if (!InteractionUtils.canReviewWhitelist(event.getMember())) {
            event.reply("❌ Sem permissão.").setEphemeral(true).queue();
            return;
        }
        String[] parts = event.getComponentId().split(":", 3);
        String targetUserId = parts[1];
        String reasonNumber = parts[2];
        handleWhitelistRejectionDirect(targetUserId, reasonNumber, event);
    }

    private static void handleRejectCustom(ButtonInteractionEvent event) {
        if (!InteractionUtils.canReviewWhitelist(event.getMember())) {
            event.reply("❌ Sem permissão.").setEphemeral(true).queue();
            return;
        }
        String targetUserId = event.getComponentId().split(":")[1];
        openCustomRejectModal(event, targetUserId);
    }

    private static void handleSelectRejectReason(ButtonInteractionEvent event) {
        if (!InteractionUtils.canReviewWhitelist(event.getMember())) {
            event.reply("❌ Sem permissão.").setEphemeral(true).queue();
            return;
        }
        String targetUserId = event.getComponentId().split(":")[1];
        User targetUser = event.getJDA().getUserById(targetUserId);
        if (targetUser != null) {
            event.reply("❌ **" + targetUser.getName() + "** será reprovado.\n\n📝 Agora digite o motivo personalizado (ou use um dos botões abaixo):").setEphemeral(true).queue();
        }
    }

    private static void sendRejectReasonMenu(ButtonInteractionEvent event, String targetUserId, User targetUser) {
        InteractionUtils.staffMessages.put(targetUserId, event.getMessageId());

        EmbedBuilder embed = EmbedUtils.createError("Reprovar Whitelist",
            "**Candidato:** " + (targetUser != null ? targetUser.getAsMention() : "`" + targetUserId + "`") + "\n\n" +
            "Selecione um motivo pré-definido ou escreva um feedback personalizado.",
            event.getJDA().getSelfUser());

        embed.addField("📋 Motivos Comuns",
            "**1️⃣ Respostas curtas:** Falta de detalhe e profundidade.\n" +
            "**2️⃣ Falta de criatividade:** História genérica ou sem originalidade.\n" +
            "**3️⃣ Desconhecimento das regras:** Demonstra não ter lido as diretrizes.\n" +
            "**4️⃣ Idade:** Não atende ao requisito mínimo de 14 anos.\n" +
            "**5️⃣ Coerência:** Personagem não se encaixa no universo do servidor.\n" +
            "**6️⃣ Uso de IA:** Respostas aparentam ser geradas por inteligência artificial.", false);

        if (targetUser != null) embed.setThumbnail(targetUser.getEffectiveAvatarUrl());

        List<ActionRow> actionRows = new ArrayList<>();
        actionRows.add(ActionRow.of(
            Button.secondary("reject_predefined:" + targetUserId + ":1", "1️⃣ Respostas curtas"),
            Button.secondary("reject_predefined:" + targetUserId + ":2", "2️⃣ Falta criatividade"),
            Button.secondary("reject_predefined:" + targetUserId + ":3", "3️⃣ Sem conhecimento")
        ));
        actionRows.add(ActionRow.of(
            Button.secondary("reject_predefined:" + targetUserId + ":4", "4️⃣ Idade"),
            Button.secondary("reject_predefined:" + targetUserId + ":5", "5️⃣ Sem coerência"),
            Button.secondary("reject_predefined:" + targetUserId + ":6", "6️⃣ Uso de IA")
        ));
        actionRows.add(ActionRow.of(
            Button.primary("reject_custom:" + targetUserId, "✍️ Feedback Personalizado"),
            Button.danger("cancel_reject:" + targetUserId, "Cancelar")
        ));

        if (event.isAcknowledged()) {
            event.getHook().editOriginalEmbeds(embed.build()).setComponents(actionRows).queue();
        } else {
            event.editMessageEmbeds(embed.build()).setComponents(actionRows).queue();
        }
    }

    private static void openCustomRejectModal(ButtonInteractionEvent event, String targetUserId) {
        Modal modal = Modal.create("modal_custom_reject:" + targetUserId, "Reprovação Personalizada")
            .addActionRow(TextInput.create("custom_reason", "Escreva o motivo da reprovação", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Seja específico e construtivo...")
                .setRequired(true).setMinLength(10).setMaxLength(500).build())
            .build();
        event.replyModal(modal).queue();
    }

    private static void handleWhitelistRejectionDirect(String targetId, String reasonNumber, ButtonInteractionEvent buttonEvent) {
        WhitelistStatusInfo status = DataManager.getStatus(targetId);
        if (status != null && (status.status == WhitelistStatus.APPROVED || status.status == WhitelistStatus.REJECTED)) {
            sendAlreadyProcessed(buttonEvent, status, targetId);
            buttonEvent.getMessage().delete().queue(s -> {}, e -> {});
            ReviewManager.endReview(targetId);
            ReviewPanelManager.updatePanel(buttonEvent.getJDA());
            return;
        }
        processRejection(targetId, reasonNumber, buttonEvent.getUser(), buttonEvent);
    }

    private static void handleWhitelistRejection(String targetId, String rejectionReason, ModalInteractionEvent modalEvent) {
        WhitelistStatusInfo status = DataManager.getStatus(targetId);
        if (status != null && (status.status == WhitelistStatus.APPROVED || status.status == WhitelistStatus.REJECTED)) {
            modalEvent.replyEmbeds(EmbedUtils.createError("Já Processado",
                "Esta whitelist já foi " + (status.status == WhitelistStatus.APPROVED ? "aprovada" : "reprovada") + ".",
                modalEvent.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }
        processRejection(targetId, rejectionReason, modalEvent.getUser(), modalEvent);
    }

    private static void processRejection(String targetId, String rejectionReason, User staffUser, IReplyCallback interaction) {
        if (!interaction.isAcknowledged()) {
            if (interaction instanceof IMessageEditCallback) {
                ((IMessageEditCallback) interaction).deferEdit().queue();
            } else {
                interaction.deferReply(true).queue();
            }
        }

        try {
            String resultsChannelId = BotConfig.getResultsChannelId();
            String logChannelId = BotConfig.getLogChannelId();
            String staffChannelId = BotConfig.getStaffChannelId();

            TextChannel resultsChannel = (resultsChannelId != null && !resultsChannelId.equals("000000000000000000"))
                ? interaction.getJDA().getTextChannelById(resultsChannelId) : null;
            TextChannel logChannel = (logChannelId != null && !logChannelId.equals("000000000000000000"))
                ? interaction.getJDA().getTextChannelById(logChannelId) : null;

            String finalReason = getRejectionReasonText(rejectionReason);

            interaction.getJDA().retrieveUserById(targetId).queue(
                user -> finalizeRejection(targetId, user, finalReason, staffUser, interaction, resultsChannel, logChannel, staffChannelId),
                error -> finalizeRejection(targetId, null, finalReason, staffUser, interaction, resultsChannel, logChannel, staffChannelId)
            );
        } catch (Exception e) {
            LOGGER.error("Erro ao processar rejeição", e);
            if (!interaction.isAcknowledged()) {
                interaction.reply("❌ Erro ao processar rejeição.").setEphemeral(true).queue();
            }
        }
    }

    private static void finalizeRejection(String targetId, User user, String finalReason, User staffUser,
                                           IReplyCallback interaction, TextChannel resultsChannel, TextChannel logChannel, String staffChannelId) {
        try {
            ReviewManager.endReview(targetId);
            ReviewPanelManager.updatePanel(interaction.getJDA());

            Map<String, String> answers = DataManager.getPendingWhitelist(targetId);
            if (answers == null) {
                answers = new HashMap<>();
                answers.put("⚠️ Aviso", "As respostas originais não estão disponíveis (Cache limpo ou Bot reiniciado).");
            }

            if (logChannel != null) {
                List<MessageEmbed> logEmbeds = createLogEmbeds(user, staffUser, "Log: Whitelist Reprovada", finalReason, answers, EmbedUtils.COLOR_ERROR, interaction.getJDA().getSelfUser());
                if (logEmbeds.size() > 1) {
                    List<Button> buttons = new ArrayList<>();
                    buttons.add(Button.secondary("btn_log_prev", "⬅️ Anterior").withDisabled(true));
                    buttons.add(Button.secondary("btn_log_next", "Próxima ➡️").withDisabled(false));
                    logChannel.sendMessageEmbeds(logEmbeds.get(0)).setActionRow(buttons).queue(msg -> {
                        InteractionUtils.logCache.put(msg.getId(), logEmbeds);
                        InteractionUtils.logViewPages.put(msg.getId(), 0);
                    });
                } else {
                    logChannel.sendMessageEmbeds(logEmbeds.get(0)).queue();
                }
            }

            String staffMessageId = InteractionUtils.staffMessages.get(targetId);
            if (staffMessageId != null && staffChannelId != null) {
                TextChannel staffChannel = interaction.getJDA().getTextChannelById(staffChannelId);
                if (staffChannel != null) staffChannel.deleteMessageById(staffMessageId).queue(s -> {}, e -> {});
            }

            if (interaction instanceof ButtonInteractionEvent) {
                ((ButtonInteractionEvent) interaction).getMessage().delete().queue(s -> {}, e -> {});
            }

            DataManager.removePendingWhitelist(targetId);
            ReviewPanelManager.updatePanel(interaction.getJDA());

            InteractionUtils.staffMessages.remove(targetId);
            if (staffMessageId != null) InteractionUtils.staffViewPages.remove(staffMessageId);

            String answersJson = new com.google.gson.Gson().toJson(answers);
            WhitelistStatusInfo current = DataManager.getStatus(targetId);
            boolean terms = current != null && current.termsAccepted;
            DataManager.setStatus(targetId, WhitelistStatus.REJECTED, finalReason, null, answersJson, terms, staffUser.getId());
            DataManager.incrementStaffRejection(staffUser.getId());
            DataManager.setCooldown(targetId, 12 * 60 * 60 * 1000);

            // DM de reprovação
            if (user != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", finalReason);
                EmbedBuilder dmEmbed = MessagesConfig.buildEmbed(MessagesConfig.get().whitelist.dm_rejected, placeholders);
                dmEmbed.setFooter(interaction.getJDA().getSelfUser().getName(), interaction.getJDA().getSelfUser().getEffectiveAvatarUrl());

                int remainingAttempts = DataManager.getRemainingAttempts(targetId);
                if (remainingAttempts > 0) {
                    dmEmbed.addField("⏳ Tentativas Restantes",
                        "Você ainda tem **" + remainingAttempts + "** tentativa(s) disponível(is) agora.\nPode enviar uma nova whitelist imediatamente se desejar.", false);
                } else {
                    long nextAttempt = DataManager.getNextAttemptTime(targetId);
                    long remaining = nextAttempt - System.currentTimeMillis();
                    long hours = TimeUnit.MILLISECONDS.toHours(remaining);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
                    dmEmbed.addField("⏳ Quando tentar novamente?",
                        "Você atingiu o limite de tentativas.\nPoderá enviar novamente em **" + hours + "h " + minutes + "m**.", false);
                }
                File reprovadoFile = new File("data/reprovado.png");
                if (reprovadoFile.exists()) {
                    InteractionUtils.sendDM(user, dmEmbed.build(), FileUpload.fromData(reprovadoFile, "reprovado.png"));
                } else {
                    InteractionUtils.sendDM(user, dmEmbed.build());
                }
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("user", user != null ? user.getAsMention() : "`" + targetId + "`");
            placeholders.put("reason", finalReason);
            placeholders.put("user_avatar", user != null ? user.getEffectiveAvatarUrl() : interaction.getJDA().getSelfUser().getEffectiveAvatarUrl());

            // Delete original message
            if (interaction.isAcknowledged()) {
                interaction.getHook().deleteOriginal().queue(s -> {}, e -> {});
            } else {
                if (interaction instanceof ModalInteractionEvent) {
                    ((ModalInteractionEvent) interaction).deferEdit().queue(h -> h.deleteOriginal().queue());
                } else if (interaction instanceof ButtonInteractionEvent) {
                    ((ButtonInteractionEvent) interaction).deferEdit().queue(h -> h.deleteOriginal().queue());
                }
            }

            if (resultsChannel != null) {
                EmbedBuilder resultEmbed = MessagesConfig.buildEmbed(MessagesConfig.get().whitelist.rejected, placeholders);
                resultEmbed.setFooter(interaction.getJDA().getSelfUser().getName(), interaction.getJDA().getSelfUser().getEffectiveAvatarUrl());
                resultEmbed.setTimestamp(java.time.Instant.now());
                File reprovadoFile = new File("data/reprovado.png");
                if (reprovadoFile.exists()) {
                    FileUpload upload = FileUpload.fromData(reprovadoFile, "reprovado.png");
                    if (user != null) resultsChannel.sendMessage(user.getAsMention()).setEmbeds(resultEmbed.build()).addFiles(upload).queue();
                    else resultsChannel.sendMessage("Usuário: `" + targetId + "`").setEmbeds(resultEmbed.build()).addFiles(upload).queue();
                } else {
                    if (user != null) resultsChannel.sendMessage(user.getAsMention()).setEmbeds(resultEmbed.build()).queue();
                    else resultsChannel.sendMessage("Usuário: `" + targetId + "`").setEmbeds(resultEmbed.build()).queue();
                }
            }

            // Auto-Next
            Map<String, Map<String, String>> pending = DataManager.getAllPendingWhitelists();
            if (!pending.isEmpty()) {
                interaction.getHook().sendMessageEmbeds(new EmbedBuilder()
                    .setColor(Color.CYAN)
                    .setDescription("✅ **Reprovado com sucesso!**\nAinda existem **" + pending.size() + "** whitelists na fila.")
                    .build())
                    .addActionRow(Button.primary("btn_review_next", "➡️ Analisar Próxima"))
                    .setEphemeral(true)
                    .queue();
            }

        } catch (Exception e) {
            LOGGER.error("Erro critico em finalizeRejection", e);
        }
    }

    // ========================
    //   WHITELIST PAGE VIEW
    // ========================

    public static void sendWhitelistPage(TextChannel channel, String userId, User user, int page, String messageIdToEdit, ButtonInteractionEvent eventToEdit) {
        if (channel == null && eventToEdit == null) {
            LOGGER.error("Channel and Event are null in sendWhitelistPage for user {}", userId);
            return;
        }

        LOGGER.info("Tentando enviar whitelist de {} (Página {})", userId, page);

        Map<String, String> answers = DataManager.getPendingWhitelist(userId);
        if (answers == null) {
            LOGGER.error("Answers map is null for user {} in sendWhitelistPage", userId);
            return;
        }

        String avatarUrl = null;
        String mention = userId;
        String authorName = "Nova Aplicação de Whitelist";
        if (answers.containsKey("_ai_score")) authorName += " 🤖 " + answers.get("_ai_score");

        try {
            if (user != null) { avatarUrl = user.getEffectiveAvatarUrl(); mention = user.getAsMention(); }
            else { avatarUrl = "https://cdn.discordapp.com/embed/avatars/0.png"; mention = "<@" + userId + ">"; }
        } catch (Exception e) { avatarUrl = "https://cdn.discordapp.com/embed/avatars/0.png"; mention = "<@" + userId + ">"; }

        long timestamp = System.currentTimeMillis();
        if (answers.containsKey("_timestamp")) {
            try { timestamp = Long.parseLong(answers.get("_timestamp")); } catch (NumberFormatException ignored) {}
        }

        String candidateDisplay = mention;
        if (user != null) candidateDisplay = "**" + user.getName() + "** (" + mention + ")";

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(EmbedUtils.COLOR_PRIMARY)
            .setAuthor(authorName, null, avatarUrl)
            .setDescription(
                EmbedUtils.ICON_USER + " **Candidato:** " + candidateDisplay + (answers.containsKey("_ai_score") ? " 🤖 **" + answers.get("_ai_score") + "**" : "") + "\n" +
                EmbedUtils.ICON_ID + " **ID:** `" + userId + "`\n" +
                EmbedUtils.ICON_CALENDAR + " **Enviado:** <t:" + (timestamp / 1000) + ":R>\n\n" +
                "⚠️ **Atenção:** Clique em **🚪 Sair** para liberar a whitelist se não for finalizar. Fechar a mensagem mantém o bloqueio."
            );

        if (DataManager.isFlagged(userId)) {
            embed.setColor(EmbedUtils.COLOR_WARNING);
            embed.addField(EmbedUtils.ICON_WARNING + " ALERTA DE IDADE",
                ">>> Este usuário informou ter **menos de 14 anos**. A análise requer atenção redobrada e verificação antes da aprovação.", false);
        }

        if (answers.containsKey("_ai_analysis")) {
            embed.setColor(EmbedUtils.COLOR_ERROR);
            String title = "🤖 ALERTA DE IA";
            if (answers.containsKey("_ai_score")) title += " (" + answers.get("_ai_score") + ")";
            embed.addField(title, ">>> " + answers.get("_ai_analysis"), false);
        }

        embed.setThumbnail(avatarUrl);

        String sectionTitle = WhitelistConfig.getPageTitle(page);
        embed.addField("📂 Seção em Análise: " + sectionTitle, "", false);
        addFieldsToEmbed(embed, answers, WhitelistConfig.getQuestionsByPage(page));

        net.dv8tion.jda.api.JDA jda = (eventToEdit != null) ? eventToEdit.getJDA() : channel.getJDA();
        embed.setFooter("Página " + (page + 1) + "/3 • Use os botões para navegar e decidir.", jda.getSelfUser().getEffectiveAvatarUrl());
        embed.setTimestamp(java.time.Instant.now());

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.secondary("btn_wl_prev:" + userId, "⬅️ Anterior").withDisabled(page == 0));
        buttons.add(Button.secondary("btn_wl_next:" + userId, "Próxima ➡️").withDisabled(page == 2));
        buttons.add(Button.success("btn_whitelist_approve:" + userId, "✅ Aprovar"));
        buttons.add(Button.danger("btn_whitelist_reject:" + userId, "❌ Reprovar"));
        buttons.add(Button.secondary("btn_review_exit:" + userId, "🚪 Sair"));

        if (eventToEdit != null) {
            if (eventToEdit.isAcknowledged()) eventToEdit.getHook().editOriginalEmbeds(embed.build()).setActionRow(buttons).queue();
            else eventToEdit.editMessageEmbeds(embed.build()).setActionRow(buttons).queue();
            InteractionUtils.staffViewPages.put(eventToEdit.getMessageId(), page);
        } else if (messageIdToEdit != null) {
            channel.retrieveMessageById(messageIdToEdit).queue(msg -> {
                msg.editMessageEmbeds(embed.build()).setActionRow(buttons).queue();
                InteractionUtils.staffViewPages.put(msg.getId(), page);
                InteractionUtils.staffMessages.put(userId, msg.getId());
                DataManager.updatePendingWhitelistMessageId(userId, msg.getId());
            });
        } else {
            channel.sendMessageEmbeds(embed.build()).setActionRow(buttons).queue(msg -> {
                InteractionUtils.staffViewPages.put(msg.getId(), page);
                InteractionUtils.staffMessages.put(userId, msg.getId());
                DataManager.updatePendingWhitelistMessageId(userId, msg.getId());
            });
        }
    }

    public static void sendWhitelistPage(TextChannel channel, String userId, User user, int page, String messageIdToEdit) {
        sendWhitelistPage(channel, userId, user, page, messageIdToEdit, null);
    }

    // ========================
    //      UTILITIES
    // ========================

    private static void sendAlreadyProcessed(ButtonInteractionEvent event, WhitelistStatusInfo status, String targetId) {
        EmbedBuilder errorEmbed = EmbedUtils.createError("Já Processado",
            "Esta whitelist já foi " + (status.status == WhitelistStatus.APPROVED ? "aprovada" : "reprovada") + ".",
            event.getJDA().getSelfUser());

        Map<String, Map<String, String>> pending = DataManager.getAllPendingWhitelists();
        List<Button> buttons = new ArrayList<>();
        if (!pending.isEmpty()) buttons.add(Button.primary("btn_review_next", "➡️ Analisar Próxima"));

        if (event.isAcknowledged()) {
            var req = event.getHook().editOriginalEmbeds(errorEmbed.build());
            if (!buttons.isEmpty()) req.setActionRow(buttons); else req.setComponents();
            req.queue();
        } else {
            var req = event.editMessageEmbeds(errorEmbed.build());
            if (!buttons.isEmpty()) req.setActionRow(buttons); else req.setComponents();
            req.queue();
        }
        ReviewManager.endReview(targetId);
        ReviewPanelManager.updatePanel(event.getJDA());
    }

    static String getRejectionReasonText(String reason) {
        String trimmed = reason.trim();
        switch (trimmed) {
            case "1": return "❌ Respostas muito curtas - Precisamos de mais detalhes para avaliar sua criatividade e escrita.";
            case "2": return "❌ Falta de criatividade - Sua história pareceu genérica. Tente criar algo mais único e pessoal.";
            case "3": return "❌ Desconhecimento das regras - Algumas respostas indicam que você não leu ou não entendeu nossas regras.";
            case "4": return "❌ Não atende aos requisitos de idade mínima - Nosso servidor possui requisitos de idade para garantir a maturidade adequada do roleplay.";
            case "5": return "❌ Personagem sem coerência com o servidor - Certifique-se de que seu personagem se encaixa no universo e temática do Midgard RPG.";
            case "6": return "❌ Uso de IA detectado - Suas respostas aparentam ter sido geradas por inteligência artificial. A whitelist deve ser preenchida com suas próprias palavras e criatividade.";
            default:
                if (trimmed.contains("Apague este texto")) {
                    String[] parts = trimmed.split("Apague este texto.*:", 2);
                    if (parts.length > 1 && !parts[1].trim().isEmpty()) return "❌ " + parts[1].trim();
                    return "❌ Não se adequou aos requisitos do servidor.";
                }
                return "❌ " + trimmed;
        }
    }

    static List<MessageEmbed> createLogEmbeds(User user, User staff, String title, String reason, Map<String, String> answers, Color color, net.dv8tion.jda.api.entities.SelfUser selfUser) {
        List<MessageEmbed> embeds = new ArrayList<>();

        StringBuilder description = new StringBuilder();
        description.append(EmbedUtils.ICON_USER).append(" **Candidato:** ").append(user != null ? user.getAsMention() : "`Unknown`").append("\n");
        description.append(EmbedUtils.ICON_STAFF).append(" **Staff:** ").append(staff.getAsMention()).append("\n");
        description.append(EmbedUtils.ICON_CALENDAR).append(" **Data:** <t:").append(System.currentTimeMillis() / 1000).append(":F>\n");
        description.append("**").append(EmbedUtils.ICON_SCROLL).append(" Motivo:** ").append(reason);

        if (answers.containsKey("⚠️ Aviso")) description.append("\n\n**⚠️ Aviso:** ").append(answers.get("⚠️ Aviso"));
        if (answers.containsKey("_ai_analysis")) description.append("\n\n**🤖 ALERTA DE IA:**\n").append(answers.get("_ai_analysis"));

        String suspiciousWarnings = analyzeAnswers(answers);
        if (!suspiciousWarnings.isEmpty()) description.append("\n\n**⚠️ Alerta de Conteúdo Suspeito:**\n").append(suspiciousWarnings);

        EmbedBuilder firstEmbed = new EmbedBuilder().setTitle(title).setColor(color).setDescription(description.toString());

        Map<String, String> allQuestions = new LinkedHashMap<>();
        allQuestions.putAll(WhitelistConfig.getPart1Questions());
        allQuestions.putAll(WhitelistConfig.getPart2Questions());
        allQuestions.putAll(WhitelistConfig.getPart3Questions());

        int totalPages = 1 + (int) Math.ceil(allQuestions.size() / 10.0);
        if (user != null) firstEmbed.setThumbnail(user.getEffectiveAvatarUrl());
        if (selfUser != null) firstEmbed.setFooter("Página 1/" + totalPages + " • Use os botões para ver as respostas", selfUser.getEffectiveAvatarUrl());
        embeds.add(firstEmbed.build());

        EmbedBuilder currentEmbed = new EmbedBuilder().setColor(color);
        int fieldsInCurrent = 0, charsInCurrent = 0, pageIndex = 2;

        for (Map.Entry<String, String> entry : allQuestions.entrySet()) {
            String question = entry.getValue();
            String questionId = entry.getKey();
            String answer = answers.getOrDefault(entry.getKey(), "*Sem resposta*");
            if (answer.length() > 1000) answer = answer.substring(0, 997) + "...";

            if (fieldsInCurrent >= 10 || charsInCurrent + answer.length() > 2000) {
                if (selfUser != null) currentEmbed.setFooter("Página " + pageIndex + "/" + totalPages, selfUser.getEffectiveAvatarUrl());
                embeds.add(currentEmbed.build());
                currentEmbed = new EmbedBuilder().setColor(color);
                fieldsInCurrent = 0; charsInCurrent = 0; pageIndex++;
            }

            answer = answer.replace("`", "'").replace("*", "").replace("_", "").replace("~", "").replace("|", "");
            if (answers.containsKey("_ai_score") && (questionId.equals("q14_lore") || questionId.equals("q10_concepts"))) {
                question = "🤖 (" + answers.get("_ai_score") + ") " + question;
            }

            currentEmbed.addField("❓ " + question, "📝 " + answer, false);
            fieldsInCurrent++; charsInCurrent += answer.length() + question.length();
        }

        if (fieldsInCurrent > 0) {
            if (selfUser != null) currentEmbed.setFooter("Página " + pageIndex + "/" + totalPages, selfUser.getEffectiveAvatarUrl());
            embeds.add(currentEmbed.build());
        }

        return embeds;
    }

    static String analyzeAnswers(Map<String, String> answers) {
        StringBuilder warnings = new StringBuilder();
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String answer = entry.getValue().toLowerCase();
            for (String word : InteractionUtils.SUSPICIOUS_WORDS) {
                if (answer.contains(word)) {
                    warnings.append("• 🚫 **Palavra Proibida:** \"").append(word).append("\" em `").append(entry.getKey()).append("`\n");
                }
            }
        }
        return warnings.toString();
    }

    static void addFieldsToEmbed(EmbedBuilder embed, Map<String, String> answers, Map<String, String> questions) {
        String aiScore = answers.get("_ai_score");
        for (Map.Entry<String, String> entry : questions.entrySet()) {
            String questionId = entry.getKey();
            String questionText = entry.getValue();
            String answer = answers.getOrDefault(questionId, "*Sem resposta*");

            if (aiScore != null && (questionId.equals("q14_lore") || questionId.equals("q10_concepts"))) {
                questionText = "🤖 (" + aiScore + ") " + questionText;
            }

            answer = answer.replace("`", "'").replace("|", "");

            String prefix = "📝 ";
            int maxFirstChunk = 1024 - prefix.length();

            if (answer.length() <= maxFirstChunk) {
                embed.addField("❓ " + questionText, prefix + answer, false);
            } else {
                String firstChunk = answer.substring(0, maxFirstChunk);
                embed.addField("❓ " + questionText, prefix + firstChunk, false);
                String remaining = answer.substring(maxFirstChunk);
                int length = remaining.length();
                for (int i = 0; i < length; i += 1024) {
                    String chunk = remaining.substring(i, Math.min(length, i + 1024));
                    embed.addField("➡️ (Continuação)", chunk, false);
                }
            }
        }
    }
}
