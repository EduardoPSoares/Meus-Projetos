package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class ToggleWhitelistCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "toggle-whitelist";
    }

    @Override
    public String getDescription() {
        return "Ativa ou desativa o sistema de whitelist.";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_WHITELIST_TOGGLE";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.BOOLEAN, "ativar", "Ativar (true) ou Desativar (false) o sistema de whitelist.", true)
        );
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean enable = event.getOption("ativar").getAsBoolean();
        
        DataManager.setWhitelistEnabled(enable);
        
        String status = enable ? "ativado" : "desativado";
        String emoji = enable ? "✅" : "⛔";
        
        event.replyEmbeds(EmbedUtils.createSuccess(
            "Sistema de Whitelist",
            emoji + " O sistema de whitelist foi **" + status + "** com sucesso.",
            event.getJDA().getSelfUser()
        ).build()).queue();
    }
}
