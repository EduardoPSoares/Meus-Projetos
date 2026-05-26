package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.features.intimacao.IntimacaoManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class RetirarIntimacaoCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "retirar-intimacao";
    }

    @Override
    public String getDescription() {
        return "Retira a intimação ativa de um usuário.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário cuja intimação será retirada", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_INTIMAR";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        User targetUser = event.getOption("usuario").getAsUser();

        IntimacaoManager.IntimacaoData data = IntimacaoManager.getIntimacao(targetUser.getId());
        if (data == null) {
            event.replyEmbeds(
                EmbedUtils.createError("Sem Intimação", "Este usuário não possui uma intimação ativa.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        boolean sucesso = IntimacaoManager.retirarIntimacao(targetUser.getId(), event.getUser(), event.getJDA());

        if (sucesso) {
            event.getHook().sendMessageEmbeds(
                EmbedUtils.createSuccess("Intimação Retirada", "A intimação de " + targetUser.getAsMention() + " foi retirada com sucesso.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
        } else {
            event.getHook().sendMessageEmbeds(
                EmbedUtils.createError("Erro", "Não foi possível retirar a intimação.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
        }
    }
}
