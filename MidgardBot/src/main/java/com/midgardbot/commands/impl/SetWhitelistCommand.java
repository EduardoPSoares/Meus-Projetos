package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class SetWhitelistCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "setwhitelist";
    }

    @Override
    public String getDescription() {
        return "Altera o status da whitelist de um jogador.";
    }

    @Override
    public List<OptionData> getOptions() {
        OptionData statusOption = new OptionData(OptionType.STRING, "status", "O novo status da whitelist", true)
                .addChoice("Aprovado", "APPROVED")
                .addChoice("Pendente", "PENDING")
                .addChoice("Reprovado", "REJECTED")
                .addChoice("Em Revisão", "REVIEWING")
                .addChoice("Precisa Revisão", "NEEDS_REVIEW")
                .addChoice("Sinalizada", "FLAGGED")
                .addChoice("Prioritária", "PRIORITY")
                .addChoice("Excepcional", "EXCELLENT");

        return List.of(
                new OptionData(OptionType.USER, "usuario", "O usuário para alterar o status", true),
                statusOption,
                new OptionData(OptionType.STRING, "motivo", "Motivo da alteração (opcional)", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_WHITELIST_MANAGE";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        User target = event.getOption("usuario").getAsUser();
        String statusStr = event.getOption("status").getAsString();
        OptionMapping reasonOption = event.getOption("motivo");
        String reason = reasonOption != null ? reasonOption.getAsString() : "Alterado manualmente por " + event.getUser().getName();

        WhitelistStatus status;
        try {
            status = WhitelistStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            event.reply("❌ Status inválido.").setEphemeral(true).queue();
            return;
        }

        // Recupera informações atuais para manter nickname e respostas se existirem
        WhitelistStatusInfo currentInfo = DataManager.getStatus(target.getId());
        String nickname = currentInfo != null ? currentInfo.nickname : target.getName();
        String answers = currentInfo != null ? currentInfo.answers : "{}";
        boolean termsAccepted = currentInfo != null && currentInfo.termsAccepted;

        // Atualiza o status
        DataManager.setStatus(target.getId(), status, reason, nickname, answers, termsAccepted, event.getUser().getId());

        event.replyEmbeds(EmbedUtils.createSuccess(
                "Status Atualizado",
                String.format("O status da whitelist de %s foi alterado para **%s**.\n**Motivo:** %s",
                        target.getAsMention(), status.label, reason),
                event.getJDA().getSelfUser()
        ).build()).queue();
    }
}
