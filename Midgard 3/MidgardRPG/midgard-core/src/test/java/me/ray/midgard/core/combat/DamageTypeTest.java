package me.ray.midgard.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DamageTypeTest {

    @Test
    void shouldHaveNineValues() {
        assertEquals(9, DamageType.values().length);
    }

    @Test
    void shouldContainAllExpectedTypes() {
        assertNotNull(DamageType.valueOf("PHYSICAL"));
        assertNotNull(DamageType.valueOf("MAGICAL"));
        assertNotNull(DamageType.valueOf("TRUE"));
        assertNotNull(DamageType.valueOf("PROJECTILE"));
        assertNotNull(DamageType.valueOf("FIRE"));
        assertNotNull(DamageType.valueOf("ICE"));
        assertNotNull(DamageType.valueOf("POISON"));
        assertNotNull(DamageType.valueOf("LIGHTNING"));
        assertNotNull(DamageType.valueOf("VOID"));
    }

    @Test
    void shouldThrowForInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> DamageType.valueOf("INVALID"));
    }
}
