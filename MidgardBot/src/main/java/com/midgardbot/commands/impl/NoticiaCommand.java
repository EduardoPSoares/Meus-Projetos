package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.commands.handlers.NewsHandler;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Comando /noticia - Abre o painel interativo de gerenciamento de notícias do Launcher.
 * Todo o fluxo é feito via embeds, botões, select menus e modais.
 */
public class NoticiaCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "noticia";
    }

    @Override
    public String getDescription() {
        return "Abre o painel de gerenciamento de notícias do Launcher.";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_NOTICIA";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        NewsHandler.sendMainPanel(event);
    }
}
