package me.ray.midgard.modules.item.model;

import me.ray.midgard.core.MidgardCore;

public enum ItemStat {
    ATTACK_DAMAGE("attack-damage", "Dano"),
    ATTACK_SPEED("attack-speed", "Velocidade de Ataque"),
    CRITICAL_STRIKE_CHANCE("critical-strike-chance", "Chance de Crítico"),
    CRITICAL_STRIKE_POWER("critical-strike-power", "Dano Crítico"),
    BLOCK_POWER("block-power", "Poder de Bloqueio"),
    BLOCK_RATING("block-rating", "Taxa de Bloqueio"),
    DODGE_RATING("dodge-rating", "Esquiva"),
    PARRY_RATING("parry-rating", "Aparar"),
    ARMOR("armor", "Armadura"),
    ARMOR_TOUGHNESS("armor-toughness", "Resistência de Armadura"),
    MAX_HEALTH("max-health", "Vida Máxima"),
    MAX_MANA("max-mana", "Mana Máxima"),
    MAX_STAMINA("max-stamina", "Stamina Máxima"),
    MOVEMENT_SPEED("movement-speed", "Velocidade de Movimento"),
    TWO_HANDED("two-handed", "Duas Mãos"),
    UNBREAKABLE("unbreakable", "Indestrutível"),
    REQUIRED_LEVEL("required-level", "Nível Necessário"),
    SKILL_CRITICAL_STRIKE_CHANCE("skill-critical-strike-chance", "Chance de Crítico de Habilidade"),
    SKILL_CRITICAL_STRIKE_POWER("skill-critical-strike-power", "Dano Crítico de Habilidade"),
    BLOCK_COOLDOWN_REDUCTION("block-cooldown-reduction", "Redução de Cooldown de Bloqueio"),
    DODGE_COOLDOWN_REDUCTION("dodge-cooldown-reduction", "Redução de Cooldown de Esquiva"),
    PARRY_COOLDOWN_REDUCTION("parry-cooldown-reduction", "Redução de Cooldown de Aparar"),
    COOLDOWN_REDUCTION("cooldown-reduction", "Redução de Cooldown"),
    SKILL_DAMAGE("skill-damage", "Dano de Habilidade"),
    PROJECTILE_DAMAGE("projectile-damage", "Dano de Projétil"),
    MAGIC_DAMAGE("magic-damage", "Dano Mágico"),
    UNDEAD_DAMAGE("undead-damage", "Dano contra Mortos-Vivos"),
    DAMAGE_REDUCTION("damage-reduction", "Redução de Dano"),
    DEFENSE("defense", "Defesa"),
    FALL_DAMAGE_REDUCTION("fall-damage-reduction", "Redução de Dano de Queda"),
    PROJECTILE_DAMAGE_REDUCTION("projectile-damage-reduction", "Redução de Dano de Projétil"),
    PHYSICAL_DAMAGE_REDUCTION("physical-damage-reduction", "Redução de Dano Físico"),
    MAGIC_DAMAGE_REDUCTION("magic-damage-reduction", "Redução de Dano Mágico"),
    FIRE_DAMAGE_REDUCTION("fire-damage-reduction", "Redução de Dano de Fogo"),
    LIFESTEAL("lifesteal", "Roubo de Vida"),
    PVE_DAMAGE_REDUCTION("pve-damage-reduction", "Redução de Dano PvE"),
    PVP_DAMAGE_REDUCTION("pvp-damage-reduction", "Redução de Dano PvP"),
    SPELL_VAMPIRISM("spell-vampirism", "Vampirismo de Feitiço"),
    GRAVITY("gravity", "Gravidade"),
    KNOCKBACK_RESISTANCE("knockback-resistance", "Resistência a Repulsão"),
    MAX_MANA_REGENERATION("max-mana-regeneration", "Regeneração de Mana Máxima"),
    STAMINA_REGENERATION("stamina-regeneration", "Regeneração de Stamina"),
    MAX_STAMINA_REGENERATION("max-stamina-regeneration", "Regeneração de Stamina Máxima"),
    MAX_STELLIUM("max-stellium", "Stellium Máximo"),
    MAX_ABSORPTION("max-absorption", "Absorção Máxima"),
    ADDITIONAL_EXPERIENCE("additional-experience", "Experiência Adicional"),
    HEALTH_REGENERATION("health-regeneration", "Regeneração de Vida"),
    MAX_HEALTH_REGENERATION("max-health-regeneration", "Regeneração de Vida Máxima"),
    MYLUCK("myluck", "Sorte"),
    MANA_REGENERATION("mana-regeneration", "Regeneração de Mana"),
    SUCCESS_RATE("success-rate", "Taxa de Sucesso"),
    SAFE_FALL_DISTANCE("safe-fall-distance", "Distância de Queda Segura"),
    SCALE("scale", "Escala"),
    STEP_HEIGHT("step-height", "Altura do Degrau"),
    BURNING_TIME("burning-time", "Tempo de Queima"),
    JUMP_STRENGTH("jump-strength", "Força do Pulo"),
    EXPLOSION_KNOCKBACK_RESISTANCE("explosion-knockback-resistance", "Resistência a Repulsão de Explosão"),
    MINING_EFFICIENCY("mining-efficiency", "Eficiência de Mineração"),
    MOVEMENT_EFFICIENCY("movement-efficiency", "Eficiência de Movimento"),
    BONUS_OXYGEN("bonus-oxygen", "Oxigênio Bônus"),
    SNEAKING_SPEED("sneaking-speed", "Velocidade Agachado"),
    SUBMERGED_MINING_SPEED("submerged-mining-speed", "Velocidade de Mineração Submersa"),
    SWEEPING_DAMAGE_RATIO("sweeping-damage-ratio", "Taxa de Dano de Varredura"),
    WATER_MOVEMENT_EFFICIENCY("water-movement-efficiency", "Eficiência de Movimento na Água"),
    MINING_SPEED("mining-speed", "Velocidade de Mineração"),
    BLOCK_INTERACTION_RANGE("block-interaction-range", "Alcance de Interação com Blocos"),
    ENTITY_INTERACTION_RANGE("entity-interaction-range", "Alcance de Interação com Entidades"),
    FALL_DAMAGE_MULTIPLIER("fall-damage-multiplier", "Multiplicador de Dano de Queda"),
    ITEM_COOLDOWN("item-cooldown", "Cooldown do Item"),
    
    // Elemental Damage
    FIRE_DAMAGE("fire-damage", "Dano de Fogo"),
    ICE_DAMAGE("ice-damage", "Dano de Gelo"),
    LIGHT_DAMAGE("light-damage", "Dano de Luz"),
    DARKNESS_DAMAGE("darkness-damage", "Dano de Escuridão"),
    DIVINE_DAMAGE("divine-damage", "Dano Divino"),

    // Missing RPG Stats
    STRENGTH("strength", "Força"),
    INTELLIGENCE("intelligence", "Inteligência"),
    DEXTERITY("dexterity", "Destreza"),
    
    ACCURACY("accuracy", "Precisão"),
    CRITICAL_RESISTANCE("critical-resistance", "Resistência Crítica"),
    THORNS("thorns", "Espinhos"),
    MAGIC_RESISTANCE("magic-resistance", "Resistência Mágica"),
    
    ARMOR_PENETRATION("armor-penetration", "Penetração de Armadura"),
    ARMOR_PENETRATION_FLAT("armor-penetration-flat", "Penetração de Armadura (Flat)"),
    MAGIC_PENETRATION("magic-penetration", "Penetração Mágica"),
    MAGIC_PENETRATION_FLAT("magic-penetration-flat", "Penetração Mágica (Flat)"),
    
    ICE_DAMAGE_REDUCTION("ice-damage-reduction", "Redução de Dano de Gelo"),
    LIGHT_DAMAGE_REDUCTION("light-damage-reduction", "Redução de Dano de Luz"),
    DARKNESS_DAMAGE_REDUCTION("darkness-damage-reduction", "Redução de Dano de Escuridão"),
    DIVINE_DAMAGE_REDUCTION("divine-damage-reduction", "Redução de Dano Divino");

    private final String path;
    private final String name;

    ItemStat(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        String msg = MidgardCore.getLanguageManager().getRawMessage("item.stat." + path);
        return msg != null ? msg : name;
    }

    public static ItemStat fromPath(String path) {
        for (ItemStat stat : values()) {
            if (stat.getPath().equalsIgnoreCase(path)) {
                return stat;
            }
        }
        return null;
    }
}
