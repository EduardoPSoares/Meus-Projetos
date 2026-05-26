package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.features.intimacao.IntimacaoManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class IntimarCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "intimar";
    }

    @Override
    public String getDescription() {
        return "Intima um usuário para uma audiência.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário a ser intimado", true),
            new OptionData(OptionType.STRING, "motivo", "O motivo da intimação", true),
            new OptionData(OptionType.STRING, "data_audiencia", "Data da audiência (ex: 01/03/2026 às 15:00)", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_INTIMAR";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        Member targetMember = event.getOption("usuario").getAsMember();
        User targetUser = event.getOption("usuario").getAsUser();
        String motivo = event.getOption("motivo").getAsString();
        String dataAudiencia = event.getOption("data_audiencia").getAsString();

        if (targetMember == null) {
            event.replyEmbeds(
                EmbedUtils.createError("Erro", "O usuário precisa ser membro do servidor.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        // Não pode intimar bots
        if (targetUser.isBot()) {
            event.replyEmbeds(
                EmbedUtils.createError("Erro", "Não é possível intimar um bot.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        String categoryId = BotConfig.get("INTIMACAO_CATEGORY_ID");
        if (categoryId == null || categoryId.isEmpty()) {
            event.replyEmbeds(
                EmbedUtils.createError("Erro de Configuração", "A categoria de intimações não está configurada.\nDefina `INTIMACAO_CATEGORY_ID` no config.env.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        IntimacaoManager.criarIntimacao(
            event.getGuild(),
            event.getUser(),
            targetUser,
            targetMember,
            motivo,
            dataAudiencia,
            event.getHook()
        );
    }
}
