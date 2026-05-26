package me.ray.midgard.modules.combat;

import me.ray.midgard.core.attribute.Attribute;
import me.ray.midgard.core.attribute.AttributeRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitária para definição e registro de atributos de combate.
 * <p>
 * Contém constantes para os nomes dos atributos e o método para registrá-los no sistema de atributos do Core.
 * Define atributos como Vida, Mana, Stamina, Força, Defesa, etc.
 */
public class CombatAttributes {

    public static final String MAX_MANA = "max_mana";
    public static final String MANA_REGEN = "mana_regen";
    public static final String MAX_STAMINA = "max_stamina";
    public static final String STAMINA_REGEN = "stamina_regen";
    public static final String MAX_HEALTH = "max_health";
    public static final String HEALTH_REGEN = "health_regen";
    public static final String HEALTH_REGEN_AMP = "health_regen_amp";
    public static final String MANA_REGEN_AMP = "mana_regen_amp";
    public static final String STAMINA_REGEN_AMP = "stamina_regen_amp";
    public static final String STRENGTH = "strength";
    public static final String INTELLIGENCE = "intelligence";
    public static final String DEXTERITY = "dexterity";
    public static final String AGILITY = "agility";
    public static final String VITALITY = "vitality";
    public static final String DEFENSE = "defense";
    public static final String CRITICAL_CHANCE = "critical_chance";
    public static final String CRITICAL_DAMAGE = "critical_damage";
    
    public static final String SPEED = "speed";
    public static final String LUCK = "luck";
    public static final String COOLDOWN_REDUCTION = "cooldown_reduction";
    public static final String LIFE_STEAL = "life_steal";
    public static final String THORNS = "thorns";
    public static final String ARMOR_PENETRATION = "armor_penetration";
    public static final String MAGIC_PENETRATION = "magic_penetration";
    public static final String ARMOR_PENETRATION_FLAT = "armor_penetration_flat";
    public static final String MAGIC_PENETRATION_FLAT = "magic_penetration_flat";
    public static final String ACCURACY = "accuracy";
    public static final String CRITICAL_RESISTANCE = "critical_resistance";
    public static final String PHYSICAL_DAMAGE = "physical_damage";
    public static final String MAGIC_DAMAGE = "magic_damage";
    public static final String MAGIC_RESISTANCE = "magic_resistance";
    public static final String ATTACK_SPEED = "attack_speed";
    
    // New attributes from ItemStat
    public static final String BLOCK_POWER = "block_power";
    public static final String BLOCK_RATING = "block_rating";
    public static final String DODGE_RATING = "dodge_rating";
    public static final String PARRY_RATING = "parry_rating";
    public static final String ARMOR = "armor";
    public static final String ARMOR_TOUGHNESS = "armor_toughness";
    public static final String SKILL_CRITICAL_CHANCE = "skill_critical_chance";
    public static final String SKILL_CRITICAL_DAMAGE = "skill_critical_damage";
    public static final String BLOCK_COOLDOWN_REDUCTION = "block_cooldown_reduction";
    public static final String DODGE_COOLDOWN_REDUCTION = "dodge_cooldown_reduction";
    public static final String PARRY_COOLDOWN_REDUCTION = "parry_cooldown_reduction";
    public static final String WEAPON_DAMAGE = "weapon_damage";
    public static final String SKILL_DAMAGE = "skill_damage";
    public static final String PROJECTILE_DAMAGE = "projectile_damage";
    public static final String UNDEAD_DAMAGE = "undead_damage";
    public static final String DAMAGE_REDUCTION = "damage_reduction";
    public static final String FALL_DAMAGE_REDUCTION = "fall_damage_reduction";
    public static final String PROJECTILE_DAMAGE_REDUCTION = "projectile_damage_reduction";
    public static final String PHYSICAL_DAMAGE_REDUCTION = "physical_damage_reduction";
    public static final String MAGIC_DAMAGE_REDUCTION = "magic_damage_reduction";
    public static final String PVE_DAMAGE_REDUCTION = "pve_damage_reduction";
    public static final String PVP_DAMAGE_REDUCTION = "pvp_damage_reduction";
    public static final String SPELL_VAMPIRISM = "spell_vampirism";
    public static final String KNOCKBACK_RESISTANCE = "knockback_resistance";
    public static final String MAX_STELLIUM = "max_stellium";
    public static final String MAX_ABSORPTION = "max_absorption";
    public static final String MAX_HEALTH_REGEN = "max_health_regen";
    public static final String MAX_MANA_REGEN = "max_mana_regen";
    public static final String MAX_STAMINA_REGEN = "max_stamina_regen";
    
    // DOT / Skill / Minion
    public static final String DOT_DAMAGE_REDUCTION = "dot_damage_reduction";
    public static final String SKILL_DAMAGE_BONUS = "skill_damage_bonus";
    public static final String SKILL_DAMAGE_REDUCTION = "skill_damage_reduction";
    public static final String MINION_DAMAGE = "minion_damage";
    public static final String MINION_DAMAGE_REDUCTION = "minion_damage_reduction";

    // Bonus Attributes
    public static final String XP_BONUS = "xp_bonus";
    public static final String LOOT_BONUS = "loot_bonus";
    public static final String MANA_STEAL = "mana_steal";

    // Elemental Attributes
    public static final String FIRE_DAMAGE = "fire_damage";
    public static final String ICE_DAMAGE = "ice_damage";
    public static final String LIGHT_DAMAGE = "light_damage";
    public static final String DARKNESS_DAMAGE = "darkness_damage";
    public static final String DIVINE_DAMAGE = "divine_damage";
    public static final String EARTH_DAMAGE = "earth_damage";
    public static final String THUNDER_DAMAGE = "thunder_damage";
    public static final String WATER_DAMAGE = "water_damage";
    public static final String AIR_DAMAGE = "air_damage";
    
    public static final String FIRE_DEFENSE = "fire_defense";
    public static final String ICE_DEFENSE = "ice_defense";
    public static final String LIGHT_DEFENSE = "light_defense";
    public static final String DARKNESS_DEFENSE = "darkness_defense";
    public static final String DIVINE_DEFENSE = "divine_defense";
    public static final String EARTH_DEFENSE = "earth_defense";
    public static final String THUNDER_DEFENSE = "thunder_defense";
    public static final String WATER_DEFENSE = "water_defense";
    public static final String AIR_DEFENSE = "air_defense";

    /** Mapa que relaciona tipos de dano elemental com seus respectivos atributos de defesa. */
    public static final Map<String, String> ELEMENTAL_MAP = new HashMap<>();

    static {
        ELEMENTAL_MAP.put(FIRE_DAMAGE, FIRE_DEFENSE);
        ELEMENTAL_MAP.put(ICE_DAMAGE, ICE_DEFENSE);
        ELEMENTAL_MAP.put(LIGHT_DAMAGE, LIGHT_DEFENSE);
        ELEMENTAL_MAP.put(DARKNESS_DAMAGE, DARKNESS_DEFENSE);
        ELEMENTAL_MAP.put(DIVINE_DAMAGE, DIVINE_DEFENSE);
        ELEMENTAL_MAP.put(EARTH_DAMAGE, EARTH_DEFENSE);
        ELEMENTAL_MAP.put(THUNDER_DAMAGE, THUNDER_DEFENSE);
        ELEMENTAL_MAP.put(WATER_DAMAGE, WATER_DEFENSE);
        ELEMENTAL_MAP.put(AIR_DAMAGE, AIR_DEFENSE);
    }

    /**
     * Registra todos os atributos de combate no AttributeRegistry.
     * Verifica se o atributo já existe antes de registrar para evitar duplicatas.
     */
    public static void register() {
        AttributeRegistry registry = AttributeRegistry.getInstance();
        
        // Elemental Registration
        if (!registry.contains(FIRE_DAMAGE)) {
            registry.register(FIRE_DAMAGE, new Attribute(FIRE_DAMAGE, attrName(FIRE_DAMAGE, "Dano de Fogo"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(ICE_DAMAGE)) {
            registry.register(ICE_DAMAGE, new Attribute(ICE_DAMAGE, attrName(ICE_DAMAGE, "Dano de Gelo"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(LIGHT_DAMAGE)) {
            registry.register(LIGHT_DAMAGE, new Attribute(LIGHT_DAMAGE, attrName(LIGHT_DAMAGE, "Dano de Luz"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(DARKNESS_DAMAGE)) {
            registry.register(DARKNESS_DAMAGE, new Attribute(DARKNESS_DAMAGE, attrName(DARKNESS_DAMAGE, "Dano de Escuridão"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(DIVINE_DAMAGE)) {
            registry.register(DIVINE_DAMAGE, new Attribute(DIVINE_DAMAGE, attrName(DIVINE_DAMAGE, "Dano Divino"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(EARTH_DAMAGE)) {
            registry.register(EARTH_DAMAGE, new Attribute(EARTH_DAMAGE, attrName(EARTH_DAMAGE, "Dano de Terra"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(THUNDER_DAMAGE)) {
            registry.register(THUNDER_DAMAGE, new Attribute(THUNDER_DAMAGE, attrName(THUNDER_DAMAGE, "Dano de Trovão"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(WATER_DAMAGE)) {
            registry.register(WATER_DAMAGE, new Attribute(WATER_DAMAGE, attrName(WATER_DAMAGE, "Dano de Água"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(AIR_DAMAGE)) {
            registry.register(AIR_DAMAGE, new Attribute(AIR_DAMAGE, attrName(AIR_DAMAGE, "Dano de Ar"), 0.0, 0.0, 10000.0));
        }
        
        if (!registry.contains(FIRE_DEFENSE)) {
            registry.register(FIRE_DEFENSE, new Attribute(FIRE_DEFENSE, attrName(FIRE_DEFENSE, "Defesa de Fogo"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(ICE_DEFENSE)) {
            registry.register(ICE_DEFENSE, new Attribute(ICE_DEFENSE, attrName(ICE_DEFENSE, "Defesa de Gelo"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(LIGHT_DEFENSE)) {
            registry.register(LIGHT_DEFENSE, new Attribute(LIGHT_DEFENSE, attrName(LIGHT_DEFENSE, "Defesa de Luz"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(DARKNESS_DEFENSE)) {
            registry.register(DARKNESS_DEFENSE, new Attribute(DARKNESS_DEFENSE, attrName(DARKNESS_DEFENSE, "Defesa de Escuridão"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(DIVINE_DEFENSE)) {
            registry.register(DIVINE_DEFENSE, new Attribute(DIVINE_DEFENSE, attrName(DIVINE_DEFENSE, "Defesa Divina"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(EARTH_DEFENSE)) {
            registry.register(EARTH_DEFENSE, new Attribute(EARTH_DEFENSE, attrName(EARTH_DEFENSE, "Defesa de Terra"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(THUNDER_DEFENSE)) {
            registry.register(THUNDER_DEFENSE, new Attribute(THUNDER_DEFENSE, attrName(THUNDER_DEFENSE, "Defesa de Trovão"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(WATER_DEFENSE)) {
            registry.register(WATER_DEFENSE, new Attribute(WATER_DEFENSE, attrName(WATER_DEFENSE, "Defesa de Água"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(AIR_DEFENSE)) {
            registry.register(AIR_DEFENSE, new Attribute(AIR_DEFENSE, attrName(AIR_DEFENSE, "Defesa de Ar"), 0.0, 0.0, 10000.0));
        }

        if (!registry.contains(MAX_HEALTH)) {
            registry.register(MAX_HEALTH, new Attribute(MAX_HEALTH, attrName(MAX_HEALTH, "Vida Máxima"), 100.0, 1.0, 100000.0));
        }

        if (!registry.contains(HEALTH_REGEN)) {
            registry.register(HEALTH_REGEN, new Attribute(HEALTH_REGEN, attrName(HEALTH_REGEN, "Regeneração de Vida"), 1.0, 0.0, 1000.0));
        }
        
        if (!registry.contains(HEALTH_REGEN_AMP)) {
            registry.register(HEALTH_REGEN_AMP, new Attribute(HEALTH_REGEN_AMP, attrName(HEALTH_REGEN_AMP, "Amp. de Regen. de Vida"), 0.0, 0.0, 500.0));
        }
        if (!registry.contains(MANA_REGEN_AMP)) {
            registry.register(MANA_REGEN_AMP, new Attribute(MANA_REGEN_AMP, attrName(MANA_REGEN_AMP, "Amp. de Regen. de Mana"), 0.0, 0.0, 500.0));
        }
        
        // Bonus Registration
        if (!registry.contains(XP_BONUS)) {
            registry.register(XP_BONUS, new Attribute(XP_BONUS, attrName(XP_BONUS, "Bônus de XP"), 0.0, 0.0, 500.0, "", "0%"));
        }
        if (!registry.contains(LOOT_BONUS)) {
            registry.register(LOOT_BONUS, new Attribute(LOOT_BONUS, attrName(LOOT_BONUS, "Bônus de Saque"), 0.0, 0.0, 500.0, "", "0%"));
        }
        if (!registry.contains(MANA_STEAL)) {
            registry.register(MANA_STEAL, new Attribute(MANA_STEAL, attrName(MANA_STEAL, "Roubo de Mana"), 0.0, 0.0, 1000.0));
        }

        if (!registry.contains(STAMINA_REGEN_AMP)) {
            registry.register(STAMINA_REGEN_AMP, new Attribute(STAMINA_REGEN_AMP, attrName(STAMINA_REGEN_AMP, "Amp. de Regen. de Estamina"), 0.0, 0.0, 500.0));
        }

        if (!registry.contains(STRENGTH)) {
            registry.register(STRENGTH, new Attribute(STRENGTH, attrName(STRENGTH, "Força"), 10.0, 0.0, 10000.0));
        }
        
        if (!registry.contains(INTELLIGENCE)) {
            registry.register(INTELLIGENCE, new Attribute(INTELLIGENCE, attrName(INTELLIGENCE, "Inteligência"), 10.0, 0.0, 10000.0));
        }
        if (!registry.contains(DEXTERITY)) {
            registry.register(DEXTERITY, new Attribute(DEXTERITY, attrName(DEXTERITY, "Destreza"), 10.0, 0.0, 10000.0));
        }
        if (!registry.contains(AGILITY)) {
            registry.register(AGILITY, new Attribute(AGILITY, attrName(AGILITY, "Agilidade"), 10.0, 0.0, 10000.0));
        }
        if (!registry.contains(VITALITY)) {
            registry.register(VITALITY, new Attribute(VITALITY, attrName(VITALITY, "Vitalidade"), 10.0, 0.0, 10000.0));
        }

        if (!registry.contains(DEFENSE)) {
            registry.register(DEFENSE, new Attribute(DEFENSE, attrName(DEFENSE, "Defesa"), 0.0, 0.0, 10000.0));
        }

        if (!registry.contains(CRITICAL_CHANCE)) {
            registry.register(CRITICAL_CHANCE, new Attribute(CRITICAL_CHANCE, attrName(CRITICAL_CHANCE, "Chance Crítica"), 5.0, 0.0, 100.0));
        }

        if (!registry.contains(CRITICAL_DAMAGE)) {
            registry.register(CRITICAL_DAMAGE, new Attribute(CRITICAL_DAMAGE, attrName(CRITICAL_DAMAGE, "Dano Crítico"), 150.0, 0.0, 1000.0));
        }

        // Register new attributes
        if (!registry.contains(BLOCK_POWER)) {
            registry.register(BLOCK_POWER, new Attribute(BLOCK_POWER, attrName(BLOCK_POWER, "Poder de Bloqueio"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(BLOCK_RATING)) {
            registry.register(BLOCK_RATING, new Attribute(BLOCK_RATING, attrName(BLOCK_RATING, "Taxa de Bloqueio"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(DODGE_RATING)) {
            registry.register(DODGE_RATING, new Attribute(DODGE_RATING, attrName(DODGE_RATING, "Taxa de Esquiva"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(PARRY_RATING)) {
            registry.register(PARRY_RATING, new Attribute(PARRY_RATING, attrName(PARRY_RATING, "Taxa de Aparar"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(ARMOR)) {
            registry.register(ARMOR, new Attribute(ARMOR, attrName(ARMOR, "Armadura"), 0.0, 0.0, 1000.0));
        }
        if (!registry.contains(ARMOR_TOUGHNESS)) {
            registry.register(ARMOR_TOUGHNESS, new Attribute(ARMOR_TOUGHNESS, attrName(ARMOR_TOUGHNESS, "Resistência de Armadura"), 0.0, 0.0, 1000.0));
        }
        if (!registry.contains(SKILL_CRITICAL_CHANCE)) {
            registry.register(SKILL_CRITICAL_CHANCE, new Attribute(SKILL_CRITICAL_CHANCE, attrName(SKILL_CRITICAL_CHANCE, "Chance Crítica de Hab."), 5.0, 0.0, 100.0));
        }
        if (!registry.contains(SKILL_CRITICAL_DAMAGE)) {
            registry.register(SKILL_CRITICAL_DAMAGE, new Attribute(SKILL_CRITICAL_DAMAGE, attrName(SKILL_CRITICAL_DAMAGE, "Dano Crítico de Hab."), 150.0, 0.0, 1000.0));
        }
        if (!registry.contains(BLOCK_COOLDOWN_REDUCTION)) {
            registry.register(BLOCK_COOLDOWN_REDUCTION, new Attribute(BLOCK_COOLDOWN_REDUCTION, attrName(BLOCK_COOLDOWN_REDUCTION, "Red. Recarga Bloqueio"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(DODGE_COOLDOWN_REDUCTION)) {
            registry.register(DODGE_COOLDOWN_REDUCTION, new Attribute(DODGE_COOLDOWN_REDUCTION, attrName(DODGE_COOLDOWN_REDUCTION, "Red. Recarga Esquiva"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(PARRY_COOLDOWN_REDUCTION)) {
            registry.register(PARRY_COOLDOWN_REDUCTION, new Attribute(PARRY_COOLDOWN_REDUCTION, attrName(PARRY_COOLDOWN_REDUCTION, "Red. Recarga Aparar"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(WEAPON_DAMAGE)) {
            registry.register(WEAPON_DAMAGE, new Attribute(WEAPON_DAMAGE, attrName(WEAPON_DAMAGE, "Dano de Arma"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(PHYSICAL_DAMAGE)) {
            registry.register(PHYSICAL_DAMAGE, new Attribute(PHYSICAL_DAMAGE, attrName(PHYSICAL_DAMAGE, "Dano Físico"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(MAGIC_DAMAGE)) {
            registry.register(MAGIC_DAMAGE, new Attribute(MAGIC_DAMAGE, attrName(MAGIC_DAMAGE, "Dano Mágico"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(MAGIC_RESISTANCE)) {
            registry.register(MAGIC_RESISTANCE, new Attribute(MAGIC_RESISTANCE, attrName(MAGIC_RESISTANCE, "Resistência Mágica"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(ATTACK_SPEED)) {
            registry.register(ATTACK_SPEED, new Attribute(ATTACK_SPEED, attrName(ATTACK_SPEED, "Velocidade de Ataque"), 4.0, 0.0, 40.0));
        }
        if (!registry.contains(SKILL_DAMAGE)) {
            registry.register(SKILL_DAMAGE, new Attribute(SKILL_DAMAGE, attrName(SKILL_DAMAGE, "Dano de Habilidade"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(PROJECTILE_DAMAGE)) {
            registry.register(PROJECTILE_DAMAGE, new Attribute(PROJECTILE_DAMAGE, attrName(PROJECTILE_DAMAGE, "Dano de Projétil"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(UNDEAD_DAMAGE)) {
            registry.register(UNDEAD_DAMAGE, new Attribute(UNDEAD_DAMAGE, attrName(UNDEAD_DAMAGE, "Dano vs Mortos-Vivos"), 0.0, 0.0, 10000.0));
        }
        if (!registry.contains(DAMAGE_REDUCTION)) {
            registry.register(DAMAGE_REDUCTION, new Attribute(DAMAGE_REDUCTION, attrName(DAMAGE_REDUCTION, "Redução de Dano"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(FALL_DAMAGE_REDUCTION)) {
            registry.register(FALL_DAMAGE_REDUCTION, new Attribute(FALL_DAMAGE_REDUCTION, attrName(FALL_DAMAGE_REDUCTION, "Red. Dano de Queda"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(PROJECTILE_DAMAGE_REDUCTION)) {
            registry.register(PROJECTILE_DAMAGE_REDUCTION, new Attribute(PROJECTILE_DAMAGE_REDUCTION, attrName(PROJECTILE_DAMAGE_REDUCTION, "Red. Dano de Projétil"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(PHYSICAL_DAMAGE_REDUCTION)) {
            registry.register(PHYSICAL_DAMAGE_REDUCTION, new Attribute(PHYSICAL_DAMAGE_REDUCTION, attrName(PHYSICAL_DAMAGE_REDUCTION, "Red. Dano Físico"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(MAGIC_DAMAGE_REDUCTION)) {
            registry.register(MAGIC_DAMAGE_REDUCTION, new Attribute(MAGIC_DAMAGE_REDUCTION, attrName(MAGIC_DAMAGE_REDUCTION, "Red. Dano Mágico"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(PVE_DAMAGE_REDUCTION)) {
            registry.register(PVE_DAMAGE_REDUCTION, new Attribute(PVE_DAMAGE_REDUCTION, attrName(PVE_DAMAGE_REDUCTION, "Red. Dano JvA"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(PVP_DAMAGE_REDUCTION)) {
            registry.register(PVP_DAMAGE_REDUCTION, new Attribute(PVP_DAMAGE_REDUCTION, attrName(PVP_DAMAGE_REDUCTION, "Red. Dano JvJ"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(SPELL_VAMPIRISM)) {
            registry.register(SPELL_VAMPIRISM, new Attribute(SPELL_VAMPIRISM, attrName(SPELL_VAMPIRISM, "Vampirismo Mágico"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(KNOCKBACK_RESISTANCE)) {
            registry.register(KNOCKBACK_RESISTANCE, new Attribute(KNOCKBACK_RESISTANCE, attrName(KNOCKBACK_RESISTANCE, "Resist. à Impulsão"), 0.0, 0.0, 1.0));
        }

        // DOT / Skill / Minion
        if (!registry.contains(DOT_DAMAGE_REDUCTION)) {
            registry.register(DOT_DAMAGE_REDUCTION, new Attribute(DOT_DAMAGE_REDUCTION, attrName(DOT_DAMAGE_REDUCTION, "Red. Dano ao Longo do Tempo"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(SKILL_DAMAGE_BONUS)) {
            registry.register(SKILL_DAMAGE_BONUS, new Attribute(SKILL_DAMAGE_BONUS, attrName(SKILL_DAMAGE_BONUS, "Bônus Dano de Hab."), 0.0, 0.0, 500.0));
        }
        if (!registry.contains(SKILL_DAMAGE_REDUCTION)) {
            registry.register(SKILL_DAMAGE_REDUCTION, new Attribute(SKILL_DAMAGE_REDUCTION, attrName(SKILL_DAMAGE_REDUCTION, "Red. Dano de Habilidade"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(MINION_DAMAGE)) {
            registry.register(MINION_DAMAGE, new Attribute(MINION_DAMAGE, attrName(MINION_DAMAGE, "Dano de Minion"), 0.0, 0.0, 500.0));
        }
        if (!registry.contains(MINION_DAMAGE_REDUCTION)) {
            registry.register(MINION_DAMAGE_REDUCTION, new Attribute(MINION_DAMAGE_REDUCTION, attrName(MINION_DAMAGE_REDUCTION, "Red. Dano de Minion"), 0.0, 0.0, 100.0));
        }

        if (!registry.contains(MAX_STELLIUM)) {
            registry.register(MAX_STELLIUM, new Attribute(MAX_STELLIUM, attrName(MAX_STELLIUM, "Stellium Máximo"), 0.0, 0.0, 1000.0));
        }
        if (!registry.contains(MAX_ABSORPTION)) {
            registry.register(MAX_ABSORPTION, new Attribute(MAX_ABSORPTION, attrName(MAX_ABSORPTION, "Absorção Máxima"), 0.0, 0.0, 1000.0));
        }
        if (!registry.contains(MAX_HEALTH_REGEN)) {
            registry.register(MAX_HEALTH_REGEN, new Attribute(MAX_HEALTH_REGEN, attrName(MAX_HEALTH_REGEN, "Regen. Vida Máxima"), 0.0, 0.0, 1000.0));
        }
        if (!registry.contains(MAX_MANA_REGEN)) {
            registry.register(MAX_MANA_REGEN, new Attribute(MAX_MANA_REGEN, attrName(MAX_MANA_REGEN, "Regen. Mana Máxima"), 0.0, 0.0, 1000.0));
        }
        if (!registry.contains(MAX_STAMINA_REGEN)) {
            registry.register(MAX_STAMINA_REGEN, new Attribute(MAX_STAMINA_REGEN, attrName(MAX_STAMINA_REGEN, "Regen. Estamina Máxima"), 0.0, 0.0, 1000.0));
        }

        if (!registry.contains(MAX_MANA)) {
            registry.register(MAX_MANA, new Attribute(MAX_MANA, attrName(MAX_MANA, "Mana Máxima"), 100.0, 0.0, 10000.0));
        }
        
        if (!registry.contains(MANA_REGEN)) {
            registry.register(MANA_REGEN, new Attribute(MANA_REGEN, attrName(MANA_REGEN, "Regeneração de Mana"), 5.0, 0.0, 1000.0));
        }

        if (!registry.contains(MAX_STAMINA)) {
            registry.register(MAX_STAMINA, new Attribute(MAX_STAMINA, attrName(MAX_STAMINA, "Estamina Máxima"), 100.0, 0.0, 10000.0));
        }

        if (!registry.contains(STAMINA_REGEN)) {
            registry.register(STAMINA_REGEN, new Attribute(STAMINA_REGEN, attrName(STAMINA_REGEN, "Regen. de Estamina"), 10.0, 0.0, 1000.0));
        }
        
        // Atributos Utilitários
        if (!registry.contains(SPEED)) {
            registry.register(SPEED, new Attribute(SPEED, attrName(SPEED, "Velocidade"), 100.0, 0.0, 500.0));
        }
        if (!registry.contains(LUCK)) {
            registry.register(LUCK, new Attribute(LUCK, attrName(LUCK, "Sorte"), 0.0, 0.0, 1000.0));
        }
        if (!registry.contains(COOLDOWN_REDUCTION)) {
            registry.register(COOLDOWN_REDUCTION, new Attribute(COOLDOWN_REDUCTION, attrName(COOLDOWN_REDUCTION, "Redução de Recarga"), 0.0, 0.0, 80.0));
        }
        if (!registry.contains(LIFE_STEAL)) {
            registry.register(LIFE_STEAL, new Attribute(LIFE_STEAL, attrName(LIFE_STEAL, "Roubo de Vida"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(THORNS)) {
            registry.register(THORNS, new Attribute(THORNS, attrName(THORNS, "Espinhos"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(ARMOR_PENETRATION)) {
            registry.register(ARMOR_PENETRATION, new Attribute(ARMOR_PENETRATION, attrName(ARMOR_PENETRATION, "Penetração de Armadura"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(ARMOR_PENETRATION_FLAT)) {
            registry.register(ARMOR_PENETRATION_FLAT, new Attribute(ARMOR_PENETRATION_FLAT, attrName(ARMOR_PENETRATION_FLAT, "Pen. Armadura (Fixa)"), 0.0, 0.0, 1000.0));
        }
        if (!registry.contains(MAGIC_PENETRATION)) {
            registry.register(MAGIC_PENETRATION, new Attribute(MAGIC_PENETRATION, attrName(MAGIC_PENETRATION, "Penetração Mágica"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(MAGIC_PENETRATION_FLAT)) {
            registry.register(MAGIC_PENETRATION_FLAT, new Attribute(MAGIC_PENETRATION_FLAT, attrName(MAGIC_PENETRATION_FLAT, "Pen. Mágica (Fixa)"), 0.0, 0.0, 1000.0));
        }
        if (!registry.contains(ACCURACY)) {
            registry.register(ACCURACY, new Attribute(ACCURACY, attrName(ACCURACY, "Precisão"), 0.0, 0.0, 100.0));
        }
        if (!registry.contains(CRITICAL_RESISTANCE)) {
            registry.register(CRITICAL_RESISTANCE, new Attribute(CRITICAL_RESISTANCE, attrName(CRITICAL_RESISTANCE, "Resistência Crítica"), 0.0, 0.0, 100.0));
        }
    }

    private static String attrName(String attrId, String fallback) {
        CombatModule mod = CombatModule.getInstance();
        if (mod != null) {
            String msg = mod.getMessage("attributes." + attrId);
            if (msg != null && !msg.equals("attributes." + attrId)) return msg;
        }
        return fallback;
    }
}
