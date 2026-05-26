package me.ray.midgard.modules.combat;

/**
 * Enum que representa as categorias principais de dano no sistema de RPG.
 * Cada categoria agrupa fontes de dano do Minecraft para facilitar a lógica de mitigação e efeitos.
 *
 * <ul>
 *   <li><b>GLOBAL</b>: Dano "verdadeiro" ou genérico, ignora defesa e resistência. Ex: KILL, GENERIC_KILL, CUSTOM.</li>
 *   <li><b>PHYSICAL</b>: Dano físico direto, como ataques de espada, machado, mobs, ou impacto de projéteis físicos.</li>
 *   <li><b>PROJECTILE</b>: Dano causado por projéteis (flechas, tridentes, bolas de fogo, etc). Pode ser combinado com outras categorias.</li>
 *   <li><b>MAGICAL</b>: Dano de fonte mágica, como poções, magias, wither, thorns, freeze, etc.</li>
 *   <li><b>ENVIRONMENTAL</b>: Dano causado pelo ambiente, como fogo, queda, sufocação, lava, cacto, explosão, etc.</li>
 * </ul>
 *
 * Categorias podem ser combinadas para representar fontes mistas (ex: Fireball = MAGICAL + PHYSICAL + PROJECTILE).
 */
public enum RPGDamageCategory {
    /**
     * Dano em área (Area of Effect).
     * Exemplos: Poções de splash, bafo de dragão, explosões.
     */
    AOE,

    /**
     * Dano global ou verdadeiro, ignora qualquer mitigação.
     * Exemplos: KILL, GENERIC_KILL, CUSTOM.
     */
    GLOBAL,

    /**
     * Dano físico direto, como ataques corpo a corpo, ataques de mobs, ou impacto de projéteis físicos.
     * Exemplos: ENTITY_ATTACK, MACE_SMASH, PLAYER_ATTACK, STING.
     */
    PHYSICAL,

    /**
     * Dano causado por projéteis, podendo ser físico, mágico ou misto.
     * Exemplos: Arrow, Trident, Fireball, WitherSkull, Spit, Wind Charge.
     */
    PROJECTILE,

    /**
     * Dano de fonte mágica, incluindo poções, magias, wither, thorns, freeze, etc.
     * Exemplos: MAGIC, POISON, WITHER, DRAGON_BREATH, THORNS, FREEZE, ENDER_PEARL.
     */
    MAGICAL,

    /**
     * Dano causado pelo ambiente, como fogo, queda, sufocação, lava, cacto, explosão, etc.
     * Exemplos: FIRE, LAVA, FALL, SUFFOCATION, EXPLOSION, HOT_FLOOR, CAMPFIRE.
     */
    ENVIRONMENTAL,

    /**
     * Dano causado por um ataque armado (com item na mão).
     */
    ARMED,

    /**
     * Dano causado por um ataque desarmado (mão vazia).
     */
    UNARMED,

    /**
     * Dano causado por explosões.
     */
    EXPLOSION,

    /**
     * Dano ao longo do tempo (Damage Over Time).
     * Exemplos: Veneno, Withering, Queimadura, Sangramento.
     */
    DOT,

    /**
     * Dano causado por uma habilidade/skill do jogador (via módulo de spells ou MythicMobs).
     */
    SKILL,

    /**
     * Dano causado por um minion/invocação do jogador (Golem, Skeleton summon, etc.).
     * O dano é atribuído ao jogador invocador.
     */
    MINION
}
