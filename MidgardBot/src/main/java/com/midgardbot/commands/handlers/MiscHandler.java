package com.midgardbot.commands.handlers;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.StaffStats;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handler para interações diversas que não pertencem a Tickets, WhitelistWizard ou WhitelistReview.
 * Inclui: link de conta, status do servidor, reset de dados, estatísticas, guia e help.
 */
public final class MiscHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiscHandler.class);

    private MiscHandler() {}

    // ========================
    //   BUTTON INTERACTIONS
    // ========================

    public static boolean handleButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("btn_stats_prev") || id.equals("btn_stats_next")) { handleStatsPagination(event); return true; }
        if (id.equals("btn_link_account")) { handleLinkAccount(event); return true; }
        if (id.equals("status_refresh")) { handleStatusRefresh(event); return true; }
        if (id.equals("status_notify")) { handleStatusNotify(event); return true; }
        if (id.equals("reset_whitelist_confirm")) { handleResetWhitelistConfirm(event); return true; }
        if (id.equals("reset_whitelist_cancel")) { handleResetWhitelistCancel(event); return true; }
        if (id.equals("btn_java")) { handleJava(event); return true; }
        if (id.equals("btn_bedrock")) { handleBedrock(event); return true; }
        if (id.equals("btn_console")) { handleConsole(event); return true; }

        return false;
    }

    // ========================
    //  SELECT MENU INTERACTIONS
    // ========================

    public static boolean handleSelectMenu(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("help_menu")) {
            handleHelpMenu(event);
            return true;
        }
        return false;
    }

    // ========================
    //    MODAL INTERACTIONS
    // ========================

    public static boolean handleModal(ModalInteractionEvent event) {
        if (event.getModalId().equals("modal_link_account")) {
            handleLinkAccountModal(event);
            return true;
        }
        return false;
    }

    // ========================
    //   BUTTON HANDLERS
    // ========================

    private static void handleStatsPagination(ButtonInteractionEvent event) {
        String messageId = event.getMessageId();
        int currentPage = InteractionUtils.staffStatsPages.getOrDefault(messageId, 0);

        if (event.getComponentId().equals("btn_stats_prev")) currentPage--;
        else currentPage++;

        Map<String, StaffStats> stats = DataManager.getStaffStats();
        java.util.List<Map.Entry<String, StaffStats>> filteredStats = com.midgardbot.commands.impl.StaffStatsCommand.filterActiveStaff(stats, event.getGuild());

        int totalPages = (int) Math.ceil((double) filteredStats.size() / com.midgardbot.commands.impl.StaffStatsCommand.ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage < 0) currentPage = 0;
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        InteractionUtils.staffStatsPages.put(messageId, currentPage);

        MessageEmbed newEmbed = com.midgardbot.commands.impl.StaffStatsCommand.generateEmbed(currentPage, event.getJDA().getSelfUser(), filteredStats);

        event.editMessageEmbeds(newEmbed)
            .setActionRow(
                Button.secondary("btn_stats_prev", "◀️ Anterior").withDisabled(currentPage == 0),
                Button.secondary("btn_stats_next", "Próximo ▶️").withDisabled(currentPage >= totalPages - 1)
            )
            .queue();
    }

    private static void handleLinkAccount(ButtonInteractionEvent event) {
        if (com.midgardbot.features.link.LinkManager.isLinked(event.getUser().getId())) {
            event.reply("✅ Sua conta já está vinculada!").setEphemeral(true).queue();
            return;
        }

        net.dv8tion.jda.api.interactions.components.text.TextInput codeInput =
            net.dv8tion.jda.api.interactions.components.text.TextInput.create("code", "Código do Minecraft", net.dv8tion.jda.api.interactions.components.text.TextInputStyle.SHORT)
                .setPlaceholder("Digite o código de 6 caracteres (ex: JKBND9)")
                .setMinLength(4).setMaxLength(6).setRequired(true).build();

        net.dv8tion.jda.api.interactions.modals.Modal modal = net.dv8tion.jda.api.interactions.modals.Modal.create("modal_link_account", "Vincular Conta")
            .addActionRow(codeInput).build();

        event.replyModal(modal).queue();
    }

    private static void handleStatusRefresh(ButtonInteractionEvent event) {
        com.midgardbot.features.ServerStatusMonitor.forceUpdate();
        event.reply("🔄 Status atualizado!").setEphemeral(true).queue();
    }

    private static void handleStatusNotify(ButtonInteractionEvent event) {
        String roleId = BotConfig.getNotificationRoleId();
        if (roleId == null || roleId.isEmpty()) {
            event.reply("❌ O cargo de notificações não foi configurado pelo administrador.").setEphemeral(true).queue();
            return;
        }

        net.dv8tion.jda.api.entities.Role role = event.getGuild().getRoleById(roleId);
        if (role == null) {
            event.reply("❌ Cargo de notificações não encontrado no servidor.").setEphemeral(true).queue();
            return;
        }

        if (event.getMember().getRoles().contains(role)) {
            event.getGuild().removeRoleFromMember(event.getMember(), role).queue(
                success -> event.reply("🔕 Notificações desativadas. Você não será mais avisado sobre mudanças no servidor.").setEphemeral(true).queue(),
                error -> event.reply("❌ Erro ao remover cargo: " + error.getMessage()).setEphemeral(true).queue()
            );
        } else {
            event.getGuild().addRoleToMember(event.getMember(), role).queue(
                success -> event.reply("🔔 Notificações ativadas! Você será avisado sobre mudanças no servidor.").setEphemeral(true).queue(),
                error -> event.reply("❌ Erro ao adicionar cargo: " + error.getMessage()).setEphemeral(true).queue()
            );
        }
    }

    private static void handleResetWhitelistConfirm(ButtonInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ Acesso negado.").setEphemeral(true).queue();
            return;
        }

        DataManager.clearAllWhitelistData();

        event.editMessageEmbeds(EmbedUtils.createSuccess("Limpeza Concluída",
            "Todos os dados de whitelist foram apagados com sucesso.\nUm backup foi criado antes da operação.",
            event.getJDA().getSelfUser()).build())
            .setComponents().queue();
    }

    private static void handleResetWhitelistCancel(ButtonInteractionEvent event) {
        event.editMessageEmbeds(EmbedUtils.createDefault("Operação Cancelada",
            "Nenhum dado foi alterado.", event.getJDA().getSelfUser()).build())
            .setComponents().queue();
    }

    private static void handleJava(ButtonInteractionEvent event) {
        event.replyEmbeds(EmbedUtils.createDefault(
            EmbedUtils.ICON_PC + " Conexão Java Edition",
            "**IP:** `jogar.midgard.com`\n**Versão:** 1.20.x ou superior",
            event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
    }

    private static void handleBedrock(ButtonInteractionEvent event) {
        event.replyEmbeds(EmbedUtils.createDefault(
            EmbedUtils.ICON_BEDROCK + " Conexão Bedrock Edition",
            "**IP:** `bedrock.midgard.com`\n**Porta:** `19132`",
            event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
    }

    private static void handleConsole(ButtonInteractionEvent event) {
        event.replyEmbeds(EmbedUtils.createDefault(
            EmbedUtils.ICON_CONSOLE + " Conexão Console Edition",
            "**IP:** `bedrock.midgard.com`\n**Porta:** `19132`\n\n*Dica: Use apps como BedrockTogether para conectar em servidores customizados no seu console.*",
            event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
    }

    // ========================
    //  SELECT MENU HANDLERS
    // ========================

    private static void handleHelpMenu(StringSelectInteractionEvent event) {
        try {
            String selected = event.getValues().get(0);
            EmbedBuilder embed = new EmbedBuilder();

            switch (selected) {
                case "cat_general":
                    embed = EmbedUtils.createDefault("📝 Comandos Gerais", "Comandos disponíveis para todos os jogadores.", event.getJDA().getSelfUser());
                    embed.addField("`/help`", "Mostra este menu de ajuda", false);
                    embed.addField("`/ping`", "Verifica a latência do bot", false);
                    embed.addField("`/status`", "Verifica o status da sua whitelist", false);
                    embed.addField("`/guia`", "Mostra o guia do servidor", false);
                    break;
                case "cat_whitelist":
                    embed = EmbedUtils.createRpg("🛡️ Comandos de Whitelist", "Comandos para gerenciar sua entrada no servidor.", event.getJDA().getSelfUser());
                    embed.addField("`/setup-whitelist`", "Cria o painel de whitelist (Admin)", false);
                    embed.addField("`/status`", "Verifica se você foi aprovado", false);
                    break;
                case "cat_admin":
                    embed = EmbedUtils.createWarning("👮 Comandos de Admin", "Comandos restritos para a equipe.", event.getJDA().getSelfUser());
                    embed.addField("`/limit`", "Gerencia limites de tentativas", false);
                    embed.addField("`/unflag`", "Remove restrições de um usuário", false);
                    embed.addField("`/setup-whitelist`", "Configura o canal de whitelist", false);
                    embed.addField("`/maintenance`", "Ativa/Desativa modo manutenção", false);
                    embed.addField("`/blacklist`", "Gerencia usuários bloqueados", false);
                    break;
            }

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        } catch (Exception e) {
            LOGGER.error("Erro ao exibir menu de ajuda", e);
            event.reply("❌ Erro ao exibir menu de ajuda.").setEphemeral(true).queue();
        }
    }

    // ========================
    //    MODAL HANDLERS
    // ========================

    private static void handleLinkAccountModal(ModalInteractionEvent event) {
        String code = event.getValue("code").getAsString();
        boolean success = com.midgardbot.features.link.LinkManager.linkAccount(code, event.getUser().getId());

        if (success) {
            event.replyEmbeds(EmbedUtils.createSuccess("Conta Vinculada!",
                "Sua conta do Minecraft foi vinculada com sucesso ao Discord.\nVocê já pode entrar no servidor!",
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS));
            LOGGER.info("Usuario " + event.getUser().getName() + " vinculou conta com codigo " + code);
        } else {
            event.replyEmbeds(EmbedUtils.createError("Falha na Vinculação",
                "Código inválido ou expirado.\nTente entrar no servidor novamente para gerar um novo código.",
                event.getJDA().getSelfUser()).build()).setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(10, java.util.concurrent.TimeUnit.SECONDS));
        }
    }
}
