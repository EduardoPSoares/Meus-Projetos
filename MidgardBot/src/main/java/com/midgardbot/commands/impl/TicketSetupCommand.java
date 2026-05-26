package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.MessagesConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.List;

/**
 * Comando de Setup de Tickets.
 * Cria o painel de suporte onde os usuarios podem abrir tickets selecionando a categoria desejada.
 */
public class TicketSetupCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "setup-ticket";
    }

    @Override
    public String getDescription() {
        return "Inicializa o portal de atendimento ao cliente (Tickets).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_TICKET_SETUP";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Fallback para ADMINISTRATOR se nao configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_TICKET_SETUP").isEmpty()
            && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            EmbedBuilder errorEmbed = MessagesConfig.buildEmbed(MessagesConfig.get().general.access_denied, null);
            errorEmbed.setFooter(event.getJDA().getSelfUser().getName(), event.getJDA().getSelfUser().getEffectiveAvatarUrl());
            event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
            return;
        }

        if (!(event.getChannel() instanceof TextChannel textChannel)) {
            event.replyEmbeds(EmbedUtils.createError(
                "Canal Invalido",
                "O setup de tickets precisa ser executado em um canal de texto normal. Nao use forum, midia ou topico.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        MessagesConfig.TicketSection ticketConfig = MessagesConfig.get().ticket;

        StringSelectMenu menu = StringSelectMenu.create("ticket_selection")
            .setPlaceholder("\uD83D\uDCC2 Selecione o Departamento...")
            .addOption(ticketConfig.support_label, "ticket_support", ticketConfig.support_desc, net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83C\uDD98"))
            .addOption(ticketConfig.report_label, "ticket_report", ticketConfig.report_desc, net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83D\uDEA8"))
            .addOption(ticketConfig.bug_label, "ticket_bug", ticketConfig.bug_desc, net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83D\uDC1B"))
            .addOption(ticketConfig.lore_label, "ticket_lore", ticketConfig.lore_desc, net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83D\uDCDC"))
            .build();

        EmbedBuilder setupEmbed = MessagesConfig.buildEmbed(ticketConfig.setup, null);
        setupEmbed.setFooter(event.getJDA().getSelfUser().getName(), event.getJDA().getSelfUser().getEffectiveAvatarUrl());

        event.deferReply(true).queue();
        textChannel.sendMessageEmbeds(setupEmbed.build())
            .addActionRow(menu)
            .queue(
                success -> event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                    "Sistema configurado",
                    "O portal de tickets foi configurado com sucesso neste canal.",
                    event.getJDA().getSelfUser()
                ).build()).queue(),
                error -> event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                    "Falha ao configurar tickets",
                    "O Discord recusou o envio do painel neste canal. Verifique se ele e um canal de texto comum e se o bot pode enviar mensagens.",
                    event.getJDA().getSelfUser()
                ).build()).queue()
            );
    }
}
