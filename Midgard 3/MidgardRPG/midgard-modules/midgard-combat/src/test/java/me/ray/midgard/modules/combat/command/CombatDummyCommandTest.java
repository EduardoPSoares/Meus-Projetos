package me.ray.midgard.modules.combat.command;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CombatDummyCommandTest {

    @Test
    void shouldHaveCorrectName() {
        CombatDummyCommand cmd = new CombatDummyCommand();
        assertEquals("dummy", cmd.getName());
    }

    @Test
    void shouldHaveCorrectPermission() {
        CombatDummyCommand cmd = new CombatDummyCommand();
        assertEquals("midgard.admin.dummy", cmd.getPermission());
    }

    @Test
    void shouldBePlayerOnly() {
        CombatDummyCommand cmd = new CombatDummyCommand();
        assertTrue(cmd.isPlayerOnly());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldContainAllExpectedDamageTypes() throws Exception {
        Field field = CombatDummyCommand.class.getDeclaredField("DAMAGE_TYPES");
        field.setAccessible(true);
        List<String> damageTypes = (List<String>) field.get(null);

        assertNotNull(damageTypes);
        assertFalse(damageTypes.isEmpty());

        // Dano base
        assertTrue(damageTypes.contains("ATTACK_DAMAGE"));
        assertTrue(damageTypes.contains("WEAPON_DAMAGE"));
        assertTrue(damageTypes.contains("PHYSICAL_DAMAGE"));
        assertTrue(damageTypes.contains("MAGIC_DAMAGE"));
        assertTrue(damageTypes.contains("PROJECTILE_DAMAGE"));
        assertTrue(damageTypes.contains("SKILL_DAMAGE"));
        assertTrue(damageTypes.contains("UNDEAD_DAMAGE"));

        // Elementais
        assertTrue(damageTypes.contains("FIRE_DAMAGE"));
        assertTrue(damageTypes.contains("ICE_DAMAGE"));
        assertTrue(damageTypes.contains("LIGHT_DAMAGE"));
        assertTrue(damageTypes.contains("DARKNESS_DAMAGE"));
        assertTrue(damageTypes.contains("DIVINE_DAMAGE"));
        assertTrue(damageTypes.contains("EARTH_DAMAGE"));
        assertTrue(damageTypes.contains("THUNDER_DAMAGE"));
        assertTrue(damageTypes.contains("WATER_DAMAGE"));
        assertTrue(damageTypes.contains("AIR_DAMAGE"));

        assertEquals(16, damageTypes.size());
    }
}
