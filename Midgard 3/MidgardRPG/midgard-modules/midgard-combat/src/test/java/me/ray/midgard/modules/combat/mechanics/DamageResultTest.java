package me.ray.midgard.modules.combat.mechanics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DamageResultTest {

    @Test
    void shouldStoreAndReturnDamage() {
        DamageResult result = new DamageResult(150.5, false, "Physical");
        assertEquals(150.5, result.getDamage());
    }

    @Test
    void shouldStoreAndReturnCriticalFlag() {
        DamageResult critical = new DamageResult(100.0, true, "Physical");
        assertTrue(critical.isCritical());

        DamageResult normal = new DamageResult(100.0, false, "Physical");
        assertFalse(normal.isCritical());
    }

    @Test
    void shouldStoreAndReturnDamageKey() {
        DamageResult result = new DamageResult(50.0, false, "Magical");
        assertEquals("Magical", result.getDamageKey());
    }

    @Test
    void shouldHandleZeroDamage() {
        DamageResult result = new DamageResult(0.0, false, "Physical");
        assertEquals(0.0, result.getDamage());
    }

    @Test
    void shouldHandleNegativeDamage() {
        DamageResult result = new DamageResult(-10.0, false, "Physical");
        assertEquals(-10.0, result.getDamage());
    }

    @Test
    void shouldHandleNullDamageKey() {
        DamageResult result = new DamageResult(100.0, false, null);
        assertNull(result.getDamageKey());
    }

    @Test
    void shouldHandleCombinedDamageKey() {
        DamageResult result = new DamageResult(100.0, true, "Projectile+Physical");
        assertEquals("Projectile+Physical", result.getDamageKey());
    }

    @Test
    void shouldHandleElementalDamageKey() {
        DamageResult result = new DamageResult(30.0, false, "fire_damage");
        assertEquals("fire_damage", result.getDamageKey());
    }

    @Test
    void shouldHandleVeryLargeDamage() {
        DamageResult result = new DamageResult(999999.99, true, "Physical");
        assertEquals(999999.99, result.getDamage());
    }
}
