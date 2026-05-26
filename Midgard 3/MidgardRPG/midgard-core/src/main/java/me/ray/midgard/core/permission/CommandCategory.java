package me.ray.midgard.core.permission;

/**
 * Categorias de comandos para organização e controle de permissões.
 */
public enum CommandCategory {
    /**
     * Comandos administrativos - apenas para administradores
     */
    ADMIN,
    
    /**
     * Comandos de moderador - para moderadores e administradores
     */
    MODERATOR,
    
    /**
     * Comandos de jogador - disponíveis para todos os jogadores
     */
    PLAYER
}