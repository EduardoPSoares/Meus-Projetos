package com.midgardbot.commands;

import java.util.List;

/**
 * Interface para Comandos de Texto (Prefixo).
 * Define a estrutura para comandos antigos baseados em mensagens (ex: !ping).
 */
public interface ICommand {
    void handle(CommandContext ctx);

    String getName();

    String getHelp();

    default List<String> getAliases() {
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
}