package me.ray.midgard.core.skill;

import me.ray.midgard.core.profile.MidgardProfile;

/**
 * Interface para provedores de habilidades (Skills/Spells).
 * Permite que módulos de classes desbloqueiem habilidades sem dependência direta.
 */
public interface SkillProvider {

    /**
     * Verifica se o jogador tem a habilidade desbloqueada.
     *
     * @param profile Perfil do jogador.
     * @param skillId ID da habilidade.
     * @return true se tiver, false caso contrário.
     */
    boolean hasSkill(MidgardProfile profile, String skillId);

    /**
     * Desbloqueia a habilidade para o jogador.
     *
     * @param profile Perfil do jogador.
     * @param skillId ID da habilidade.
     */
    void unlockSkill(MidgardProfile profile, String skillId);

    /**
     * Obtém o nome de exibição da habilidade.
     *
     * @param skillId ID da habilidade.
     * @return Nome da habilidade ou o ID se não encontrado.
     */
    String getSkillName(String skillId);

    /**
     * Obtém o nível atual da habilidade para o jogador.
     *
     * @param profile Perfil do jogador.
     * @param skillId ID da habilidade.
     * @return Nível atual (0 se não desbloqueado, ou 1+ se desbloqueado).
     */
    int getSkillLevel(MidgardProfile profile, String skillId);

    /**
     * Define o nível da habilidade para o jogador.
     *
     * @param profile Perfil do jogador.
     * @param skillId ID da habilidade.
     * @param level Novo nível.
     */
    void setSkillLevel(MidgardProfile profile, String skillId, int level);

    /**
     * Remove todas as habilidades do jogador (usado ao trocar de classe).
     *
     * @param profile Perfil do jogador.
     */
    default void removeAllSkills(MidgardProfile profile) {}
}
