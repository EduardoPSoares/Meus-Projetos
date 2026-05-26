package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Comando para avaliar um staff.
 */
public class StaffFeedbackCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "avaliarstaff";
    }

    @Override
    public String getDescription() {
        return "Envie uma avaliação para um membro da staff.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "staff", "Staff a ser avaliado", true),
            new OptionData(OptionType.INTEGER, "nota", "Nota de 1 a 5", true),
            new OptionData(OptionType.STRING, "comentario", "Comentário (opcional)", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_FEEDBACK";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        User staff = event.getOption("staff").getAsUser();
        int nota = event.getOption("nota").getAsInt();
        String comentario = event.getOption("comentario") != null ? event.getOption("comentario").getAsString() : "";
        User autor = event.getUser();

        if (nota < 1 || nota > 5) {
            event.replyEmbeds(EmbedUtils.createError("Nota inválida", "A nota deve ser entre 1 e 5.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }
        if (staff.isBot()) {
            event.replyEmbeds(EmbedUtils.createError("Inválido", "Você não pode avaliar bots.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }
        if (staff.getId().equals(autor.getId())) {
            event.replyEmbeds(EmbedUtils.createError("Inválido", "Você não pode se autoavaliar.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        DataManager.addStaffFeedback(staff.getId(), autor.getId(), nota, comentario);
        // Atualiza embed no canal de feedback
        com.midgardbot.features.StaffFeedbackEmbedUpdater.forceUpdate();
        event.replyEmbeds(EmbedUtils.createSuccess("Avaliação registrada!", "Sua avaliação foi enviada com sucesso para o staff <@" + staff.getId() + ">.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
    }
}
