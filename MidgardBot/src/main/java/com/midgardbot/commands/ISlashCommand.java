package com.midgardbot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

/**
 * Interface para Comandos Slash (/).
 * Define a estrutura que todos os comandos slash devem seguir.
 */
public interface ISlashCommand {
    // Interface for Slash Commands
    String getName();
    
    String getDescription();
    
    void handle(SlashCommandInteractionEvent event);
    
    default List<OptionData> getOptions() {
        return List.of();
    }

    default List<net.dv8tion.jda.api.interactions.commands.build.SubcommandData> getSubcommands() {
        return List.of();
    }

    /**
     * Retorna a chave de permissão necessária para executar este comando.
     * Se retornar null, o comando é público (ou usa permissões padrão do Discord).
     * @return Chave da permissão (ex: "PERM_BAN") ou null
     */
    default String getPermissionKey() {
        return null;
    }

    /**
     * Indica se este comando deve estar disponível no servidor de staffs.
     * Por padrão, comandos só ficam disponíveis no servidor principal.
     * @return true se o comando também deve ser registrado no servidor de staffs
     */
    default boolean allowedInStaffGuild() {
        return false;
    }
}