package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;

public class CleanTicketsCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "cleantickets";
    }

    @Override
    public String getDescription() {
        return "Limpa tickets 'fantasmas' (pending) do banco de dados.";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    // Método opcional para permissões, assumindo que InteractionManager verifica ADMIN
    public String getPermissionKey() {
        return null; // Apenas ADMIN por padrão
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                "Sem Permissão",
                "Apenas administradores podem usar este comando.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        int deletedCount = DataManager.cleanupGhostTickets();

        if (deletedCount >= 0) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                "Limpeza Concluída",
                "Foram removidos **" + deletedCount + "** tickets fantasmas (pending) do banco de dados.",
                event.getJDA().getSelfUser()
            ).build()).queue();
        } else {
            event.getHook().sendMessageEmbeds(EmbedUtils.createError(
                "Erro na Limpeza",
                "Ocorreu um erro ao tentar limpar os tickets. Verifique o console.",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }
}
