package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class AntiFakeBypassCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "antifakebypass";
    }

    @Override
    public String getDescription() {
        return "Gerencia a lista de bypass do Anti-Fake (contas novas).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "acao", "Ação a realizar (add/remove)", true)
                .addChoice("Adicionar", "add")
                .addChoice("Remover", "remove"),
            new OptionData(OptionType.STRING, "id", "ID do usuário do Discord", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_ADMIN";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Validação de Permissão Administrativa
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_ADMIN").isEmpty() && 
            !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError(
                "⛔ Acesso Negado",
                "Você precisa de permissão de Administrador para usar este comando.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        String action = event.getOption("acao").getAsString();
        String userId = event.getOption("id").getAsString();

        if (action.equalsIgnoreCase("add")) {
            if (DataManager.isAntiFakeBypass(userId)) {
                event.replyEmbeds(EmbedUtils.createError("Erro", "Este ID já está na lista de bypass.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
                return;
            }
            DataManager.addAntiFakeBypass(userId);
            event.replyEmbeds(EmbedUtils.createSuccess("Sucesso", "ID " + userId + " adicionado ao bypass do Anti-Fake.", event.getJDA().getSelfUser()).build()).queue();
        } else if (action.equalsIgnoreCase("remove")) {
            if (!DataManager.isAntiFakeBypass(userId)) {
                event.replyEmbeds(EmbedUtils.createError("Erro", "Este ID não está na lista de bypass.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
                return;
            }
            DataManager.removeAntiFakeBypass(userId);
            event.replyEmbeds(EmbedUtils.createSuccess("Sucesso", "ID " + userId + " removido do bypass do Anti-Fake.", event.getJDA().getSelfUser()).build()).queue();
        }
    }
}
