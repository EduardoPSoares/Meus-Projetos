package me.ray.midgard.modules.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RPGDamageCategoryTest {

    @Test
    void shouldHaveExactly12Categories() {
        assertEquals(12, RPGDamageCategory.values().length);
    }

    @Test
    void shouldContainAllExpectedCategories() {
        assertNotNull(RPGDamageCategory.valueOf("AOE"));
        assertNotNull(RPGDamageCategory.valueOf("GLOBAL"));
        assertNotNull(RPGDamageCategory.valueOf("PHYSICAL"));
        assertNotNull(RPGDamageCategory.valueOf("PROJECTILE"));
        assertNotNull(RPGDamageCategory.valueOf("MAGICAL"));
        assertNotNull(RPGDamageCategory.valueOf("ENVIRONMENTAL"));
        assertNotNull(RPGDamageCategory.valueOf("ARMED"));
        assertNotNull(RPGDamageCategory.valueOf("UNARMED"));
        assertNotNull(RPGDamageCategory.valueOf("EXPLOSION"));
        assertNotNull(RPGDamageCategory.valueOf("DOT"));
        assertNotNull(RPGDamageCategory.valueOf("SKILL"));
        assertNotNull(RPGDamageCategory.valueOf("MINION"));
    }

    @Test
    void shouldThrowForInvalidCategory() {
        assertThrows(IllegalArgumentException.class, () -> RPGDamageCategory.valueOf("FIRE"));
    }

    @Test
    void shouldPreserveOrdinalOrder() {
        RPGDamageCategory[] values = RPGDamageCategory.values();
        assertEquals(RPGDamageCategory.AOE, values[0]);
        assertEquals(RPGDamageCategory.GLOBAL, values[1]);
        assertEquals(RPGDamageCategory.PHYSICAL, values[2]);
        assertEquals(RPGDamageCategory.PROJECTILE, values[3]);
        assertEquals(RPGDamageCategory.MAGICAL, values[4]);
        assertEquals(RPGDamageCategory.ENVIRONMENTAL, values[5]);
    }

    @Test
    void shouldHaveConsistentNameAndToString() {
        for (RPGDamageCategory cat : RPGDamageCategory.values()) {
            assertEquals(cat.name(), cat.toString());
        }
    }
}
