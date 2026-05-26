package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.commands.handlers.InteractionUtils;
import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.features.whitelist.ReviewManager;
import com.midgardbot.features.whitelist.WhitelistConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReviewCommand implements ISlashCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewCommand.class);

    @Override
    public String getName() {
        return "analisar";
    }

    @Override
    public String getDescription() {
        return "Analisa a whitelist pendente mais antiga (Modo Fila)";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_REVIEW";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Permissão gerenciada pelo InteractionManager via getPermissionKey()
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_REVIEW").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
             event.reply("❌ Sem permissão.").setEphemeral(true).queue();
             return;
        }

        // Limpa locks expirados antes de buscar
        ReviewManager.cleanExpiredLocks();

        Map<String, Map<String, String>> allPending = DataManager.getAllPendingWhitelists();
        if (allPending.isEmpty()) {
            event.reply("✅ Não há whitelists pendentes para análise no momento.").setEphemeral(true).queue();
            return;
        }

        // Sort by timestamp (oldest first)
        List<Map.Entry<String, Map<String, String>>> sortedPending = allPending.entrySet().stream()
            .sorted((e1, e2) -> {
                long t1 = Long.MAX_VALUE;
                long t2 = Long.MAX_VALUE;
                try {
                    if (e1.getValue().containsKey("_timestamp")) t1 = Long.parseLong(e1.getValue().get("_timestamp"));
                    if (e2.getValue().containsKey("_timestamp")) t2 = Long.parseLong(e2.getValue().get("_timestamp"));
                } catch (Exception ex) { LOGGER.debug("Erro ao parsear timestamp para ordenação de whitelist", ex); }
                return Long.compare(t1, t2);
            })
            .collect(Collectors.toList());

        // Encontra a primeira whitelist que NÃO está em análise
        Map.Entry<String, Map<String, String>> entry = null;
        
        // 1. Prioridade: Verifica se o staff já tem alguma review travada para ele (Resume)
        for (Map.Entry<String, Map<String, String>> e : sortedPending) {
             String reviewerId = ReviewManager.getReviewer(e.getKey());
             if (reviewerId != null && reviewerId.equals(event.getUser().getId())) {
                 entry = e;
                 break;
             }
        }

        // 2. Se não tiver nenhuma pendente dele, procura a próxima livre na fila
        if (entry == null) {
            for (Map.Entry<String, Map<String, String>> e : sortedPending) {
                // Verificação extra: Se já foi aprovado/reprovado, remove e pula
                WhitelistStatusInfo info = DataManager.getStatus(e.getKey());
                if (info != null && (info.status == WhitelistStatus.APPROVED || 
                                     info.status == WhitelistStatus.REJECTED)) {
                    DataManager.removePendingWhitelist(e.getKey());
                    continue;
                }

                if (!ReviewManager.isUnderReview(e.getKey())) {
                    entry = e;
                    break;
                }
            }
        }

        if (entry == null) {
            event.reply("⚠️ Todas as whitelists pendentes já estão sendo analisadas por outros staffs.").setEphemeral(true).queue();
            return;
        }

        String userId = entry.getKey();
        Map<String, String> answers = entry.getValue();

        // Inicia o Lock
        ReviewManager.startReview(userId, event.getUser().getId());
        
        // Atualiza o painel para mostrar que alguém começou a analisar
        com.midgardbot.features.whitelist.ReviewPanelManager.updatePanel(event.getJDA());

        // Apaga a mensagem pública do chat (se existir)
        if (answers.containsKey("_staff_message_id")) {
            String msgId = answers.get("_staff_message_id");
            String staffChannelId = BotConfig.getStaffChannelId();
            TextChannel staffChannel = (staffChannelId != null) ? event.getJDA().getTextChannelById(staffChannelId) : null;
            
            if (staffChannel != null) {
                staffChannel.deleteMessageById(msgId).queue(s -> {}, e -> {});
            }
        }

        event.deferReply(true).queue();

        event.getJDA().retrieveUserById(userId).queue(user -> {
            sendReviewEmbed(event, userId, user, answers, allPending.size());
        }, failure -> {
            sendReviewEmbed(event, userId, null, answers, allPending.size());
        });
    }

    public void sendReviewEmbed(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String userId, User user, Map<String, String> answers, int totalPending) {
        String avatarUrl = (user != null) ? user.getEffectiveAvatarUrl() : "https://cdn.discordapp.com/embed/avatars/0.png";
        String mention = (user != null) ? user.getAsMention() : "<@" + userId + ">";
        String authorName = "Modo de Análise 🧐";
        if (answers.containsKey("_ai_score")) {
            authorName += " • IA: " + answers.get("_ai_score");
        }

        long timestamp = System.currentTimeMillis();
        if (answers.containsKey("_timestamp")) {
            try { timestamp = Long.parseLong(answers.get("_timestamp")); } catch (Exception e) { LOGGER.debug("Erro ao parsear timestamp da whitelist", e); }
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(EmbedUtils.COLOR_PRIMARY)
            .setAuthor(authorName, null, avatarUrl)
            .setDescription(
                EmbedUtils.ICON_USER + " **Candidato:** " + (user != null ? "**" + user.getName() + "** (" + mention + ")" : mention) + "\n" +
                EmbedUtils.ICON_ID + " **ID:** `" + userId + "`\n" +
                EmbedUtils.ICON_CALENDAR + " **Enviado:** <t:" + (timestamp / 1000) + ":R>\n" +
                "📊 **Fila:** " + totalPending + " pendentes\n\n" +
                "⚠️ **Atenção:** Clique em **🚪 Sair** para liberar a whitelist se não for finalizar. Fechar a mensagem mantém o bloqueio."
            );

        if (DataManager.isFlagged(userId)) {
            embed.setColor(EmbedUtils.COLOR_WARNING);
            embed.addField(EmbedUtils.ICON_WARNING + " ALERTA DE IDADE", "Usuário informou ter -14 anos.", false);
        }

        if (answers.containsKey("_ai_analysis")) {
            embed.setColor(EmbedUtils.COLOR_ERROR);
            embed.addField("🤖 Análise da IA", ">>> " + answers.get("_ai_analysis"), false);
        }

        // Show Page 1 (Part 1) initially
        embed.addField("📂 Parte 1: " + WhitelistConfig.getPageTitle(0), "", false);
        
        Map<String, String> questions = WhitelistConfig.getQuestionsByPage(0);
        for (Map.Entry<String, String> q : questions.entrySet()) {
            String answer = answers.getOrDefault(q.getKey(), "*Sem resposta*");
            if (answer.length() > 1000) answer = answer.substring(0, 997) + "...";
            embed.addField(q.getValue(), answer, false);
        }

        embed.setFooter("Página 1/3 • Use os botões para navegar.", event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        List<Button> buttons = new ArrayList<>();
        buttons.add(Button.secondary("btn_wl_prev:" + userId, "⬅️ Anterior").withDisabled(true));
        buttons.add(Button.secondary("btn_wl_next:" + userId, "Próxima ➡️").withDisabled(false));
        buttons.add(Button.success("btn_whitelist_approve:" + userId, "✅ Aprovar"));
        buttons.add(Button.danger("btn_whitelist_reject:" + userId, "❌ Reprovar"));
        buttons.add(Button.secondary("btn_review_exit:" + userId, "🚪 Sair"));
        
        // Se já foi deferido (acknowledged), usa o hook
        if (event.isAcknowledged()) {
            event.getHook().sendMessageEmbeds(embed.build())
                .addActionRow(buttons)
                .setEphemeral(true)
                .queue(msg -> {
                    InteractionUtils.registerStaffView(msg.getId(), 0);
                });
        } else {
            // Se não, responde diretamente
            event.replyEmbeds(embed.build())
                .addActionRow(buttons)
                .setEphemeral(true)
                .queue(msg -> {
                    msg.retrieveOriginal().queue(original -> {
                         InteractionUtils.registerStaffView(original.getId(), 0);
                    });
                });
        }
    }
}
