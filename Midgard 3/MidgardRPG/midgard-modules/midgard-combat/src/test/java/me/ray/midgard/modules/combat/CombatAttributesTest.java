package me.ray.midgard.modules.combat;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CombatAttributesTest {

    // --- Constant Values ---

    @Test
    void shouldHaveCorrectPrimaryAttributes() {
        assertEquals("max_health", CombatAttributes.MAX_HEALTH);
        assertEquals("max_mana", CombatAttributes.MAX_MANA);
        assertEquals("max_stamina", CombatAttributes.MAX_STAMINA);
        assertEquals("strength", CombatAttributes.STRENGTH);
        assertEquals("intelligence", CombatAttributes.INTELLIGENCE);
        assertEquals("dexterity", CombatAttributes.DEXTERITY);
        assertEquals("agility", CombatAttributes.AGILITY);
        assertEquals("vitality", CombatAttributes.VITALITY);
        assertEquals("defense", CombatAttributes.DEFENSE);
    }

    @Test
    void shouldHaveCorrectRegenAttributes() {
        assertEquals("health_regen", CombatAttributes.HEALTH_REGEN);
        assertEquals("mana_regen", CombatAttributes.MANA_REGEN);
        assertEquals("stamina_regen", CombatAttributes.STAMINA_REGEN);
        assertEquals("health_regen_amp", CombatAttributes.HEALTH_REGEN_AMP);
        assertEquals("mana_regen_amp", CombatAttributes.MANA_REGEN_AMP);
        assertEquals("stamina_regen_amp", CombatAttributes.STAMINA_REGEN_AMP);
    }

    @Test
    void shouldHaveCorrectCriticalAttributes() {
        assertEquals("critical_chance", CombatAttributes.CRITICAL_CHANCE);
        assertEquals("critical_damage", CombatAttributes.CRITICAL_DAMAGE);
        assertEquals("critical_resistance", CombatAttributes.CRITICAL_RESISTANCE);
        assertEquals("skill_critical_chance", CombatAttributes.SKILL_CRITICAL_CHANCE);
        assertEquals("skill_critical_damage", CombatAttributes.SKILL_CRITICAL_DAMAGE);
    }

    @Test
    void shouldHaveCorrectPenetrationAttributes() {
        assertEquals("armor_penetration", CombatAttributes.ARMOR_PENETRATION);
        assertEquals("armor_penetration_flat", CombatAttributes.ARMOR_PENETRATION_FLAT);
        assertEquals("magic_penetration", CombatAttributes.MAGIC_PENETRATION);
        assertEquals("magic_penetration_flat", CombatAttributes.MAGIC_PENETRATION_FLAT);
    }

    @Test
    void shouldHaveCorrectDamageReductionAttributes() {
        assertEquals("damage_reduction", CombatAttributes.DAMAGE_REDUCTION);
        assertEquals("fall_damage_reduction", CombatAttributes.FALL_DAMAGE_REDUCTION);
        assertEquals("projectile_damage_reduction", CombatAttributes.PROJECTILE_DAMAGE_REDUCTION);
        assertEquals("physical_damage_reduction", CombatAttributes.PHYSICAL_DAMAGE_REDUCTION);
        assertEquals("magic_damage_reduction", CombatAttributes.MAGIC_DAMAGE_REDUCTION);
        assertEquals("pve_damage_reduction", CombatAttributes.PVE_DAMAGE_REDUCTION);
        assertEquals("pvp_damage_reduction", CombatAttributes.PVP_DAMAGE_REDUCTION);
        assertEquals("dot_damage_reduction", CombatAttributes.DOT_DAMAGE_REDUCTION);
        assertEquals("skill_damage_reduction", CombatAttributes.SKILL_DAMAGE_REDUCTION);
        assertEquals("minion_damage_reduction", CombatAttributes.MINION_DAMAGE_REDUCTION);
    }

    @Test
    void shouldHaveCorrectMechanicsAttributes() {
        assertEquals("life_steal", CombatAttributes.LIFE_STEAL);
        assertEquals("thorns", CombatAttributes.THORNS);
        assertEquals("block_power", CombatAttributes.BLOCK_POWER);
        assertEquals("block_rating", CombatAttributes.BLOCK_RATING);
        assertEquals("dodge_rating", CombatAttributes.DODGE_RATING);
        assertEquals("parry_rating", CombatAttributes.PARRY_RATING);
        assertEquals("accuracy", CombatAttributes.ACCURACY);
        assertEquals("spell_vampirism", CombatAttributes.SPELL_VAMPIRISM);
        assertEquals("mana_steal", CombatAttributes.MANA_STEAL);
    }

    @Test
    void shouldHaveCorrectDamageAttributes() {
        assertEquals("physical_damage", CombatAttributes.PHYSICAL_DAMAGE);
        assertEquals("magic_damage", CombatAttributes.MAGIC_DAMAGE);
        assertEquals("weapon_damage", CombatAttributes.WEAPON_DAMAGE);
        assertEquals("skill_damage", CombatAttributes.SKILL_DAMAGE);
        assertEquals("projectile_damage", CombatAttributes.PROJECTILE_DAMAGE);
        assertEquals("undead_damage", CombatAttributes.UNDEAD_DAMAGE);
        assertEquals("skill_damage_bonus", CombatAttributes.SKILL_DAMAGE_BONUS);
        assertEquals("minion_damage", CombatAttributes.MINION_DAMAGE);
    }

    @Test
    void shouldHaveCorrectBonusAttributes() {
        assertEquals("xp_bonus", CombatAttributes.XP_BONUS);
        assertEquals("loot_bonus", CombatAttributes.LOOT_BONUS);
    }

    // --- Elemental Damage Attributes ---

    @Test
    void shouldHaveAll9ElementalDamageAttributes() {
        assertEquals("fire_damage", CombatAttributes.FIRE_DAMAGE);
        assertEquals("ice_damage", CombatAttributes.ICE_DAMAGE);
        assertEquals("light_damage", CombatAttributes.LIGHT_DAMAGE);
        assertEquals("darkness_damage", CombatAttributes.DARKNESS_DAMAGE);
        assertEquals("divine_damage", CombatAttributes.DIVINE_DAMAGE);
        assertEquals("earth_damage", CombatAttributes.EARTH_DAMAGE);
        assertEquals("thunder_damage", CombatAttributes.THUNDER_DAMAGE);
        assertEquals("water_damage", CombatAttributes.WATER_DAMAGE);
        assertEquals("air_damage", CombatAttributes.AIR_DAMAGE);
    }

    @Test
    void shouldHaveAll9ElementalDefenseAttributes() {
        assertEquals("fire_defense", CombatAttributes.FIRE_DEFENSE);
        assertEquals("ice_defense", CombatAttributes.ICE_DEFENSE);
        assertEquals("light_defense", CombatAttributes.LIGHT_DEFENSE);
        assertEquals("darkness_defense", CombatAttributes.DARKNESS_DEFENSE);
        assertEquals("divine_defense", CombatAttributes.DIVINE_DEFENSE);
        assertEquals("earth_defense", CombatAttributes.EARTH_DEFENSE);
        assertEquals("thunder_defense", CombatAttributes.THUNDER_DEFENSE);
        assertEquals("water_defense", CombatAttributes.WATER_DEFENSE);
        assertEquals("air_defense", CombatAttributes.AIR_DEFENSE);
    }

    // --- ELEMENTAL_MAP ---

    @Test
    void shouldHaveExactly9ElementalMapEntries() {
        assertEquals(9, CombatAttributes.ELEMENTAL_MAP.size());
    }

    @Test
    void shouldMapEachDamageToCorrectDefense() {
        Map<String, String> map = CombatAttributes.ELEMENTAL_MAP;
        assertEquals(CombatAttributes.FIRE_DEFENSE, map.get(CombatAttributes.FIRE_DAMAGE));
        assertEquals(CombatAttributes.ICE_DEFENSE, map.get(CombatAttributes.ICE_DAMAGE));
        assertEquals(CombatAttributes.LIGHT_DEFENSE, map.get(CombatAttributes.LIGHT_DAMAGE));
        assertEquals(CombatAttributes.DARKNESS_DEFENSE, map.get(CombatAttributes.DARKNESS_DAMAGE));
        assertEquals(CombatAttributes.DIVINE_DEFENSE, map.get(CombatAttributes.DIVINE_DAMAGE));
        assertEquals(CombatAttributes.EARTH_DEFENSE, map.get(CombatAttributes.EARTH_DAMAGE));
        assertEquals(CombatAttributes.THUNDER_DEFENSE, map.get(CombatAttributes.THUNDER_DAMAGE));
        assertEquals(CombatAttributes.WATER_DEFENSE, map.get(CombatAttributes.WATER_DAMAGE));
        assertEquals(CombatAttributes.AIR_DEFENSE, map.get(CombatAttributes.AIR_DAMAGE));
    }

    @Test
    void shouldNotContainNonElementalKeysInMap() {
        assertFalse(CombatAttributes.ELEMENTAL_MAP.containsKey(CombatAttributes.PHYSICAL_DAMAGE));
        assertFalse(CombatAttributes.ELEMENTAL_MAP.containsKey(CombatAttributes.MAGIC_DAMAGE));
        assertFalse(CombatAttributes.ELEMENTAL_MAP.containsKey(CombatAttributes.DEFENSE));
    }

    @Test
    void shouldHaveAllMapValuesEndingWithDefense() {
        for (String value : CombatAttributes.ELEMENTAL_MAP.values()) {
            assertTrue(value.endsWith("_defense"), "Expected value to end with _defense: " + value);
        }
    }

    @Test
    void shouldHaveAllMapKeysEndingWithDamage() {
        for (String key : CombatAttributes.ELEMENTAL_MAP.keySet()) {
            assertTrue(key.endsWith("_damage"), "Expected key to end with _damage: " + key);
        }
    }
}
