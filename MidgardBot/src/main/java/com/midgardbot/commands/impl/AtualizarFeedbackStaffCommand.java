package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.features.StaffFeedbackEmbedUpdater;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Comando para forçar atualização do embed de avaliações de staff (admin).
 */
public class AtualizarFeedbackStaffCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "atualizarfeedbackstaff";
    }

    @Override
    public String getDescription() {
        return "Atualiza manualmente o embed de avaliações dos staffs (admin).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_FEEDBACK_UPDATE";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_FEEDBACK_UPDATE").isEmpty() && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError("Acesso negado", "Apenas administradores podem usar este comando.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }
        StaffFeedbackEmbedUpdater.forceUpdate();
        event.replyEmbeds(EmbedUtils.createSuccess("Embed atualizado!", "O painel de avaliações dos staffs foi atualizado.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
    }
}
