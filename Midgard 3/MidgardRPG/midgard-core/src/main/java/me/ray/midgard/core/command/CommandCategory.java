package me.ray.midgard.core.command;

/**
 * Categorias de comandos para organização e controle de permissões.
 */
public enum CommandCategory {
    
    /**
     * Comandos administrativos - Acessíveis apenas com permissão midgard.admin
     * Exemplo: /rpg admin reload
     */
    ADMIN,
    
    /**
     * Comandos de jogador - Acessíveis a todos os jogadores
     * Exemplo: /spell, /skill, /character
     */
    PLAYER,
    
    /**
     * Comandos de moderador - Permissões intermediárias
     * Exemplo: /vanish, /fly (quando não admin)
     */
    MODERATOR
}