package me.ray.midgard.core.effect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EffectRegistryTest {

    @BeforeEach
    void setUp() {
        EffectRegistry.getInstance().clear();
    }

    @Test
    void shouldBeSingleton() {
        assertSame(EffectRegistry.getInstance(), EffectRegistry.getInstance());
    }

    @Test
    void shouldRegisterAndRetrieveEffect() {
        StatusEffect effect = new StatusEffect("poison", StatusEffect.EffectType.DEBUFF);
        EffectRegistry.getInstance().register("poison", effect);

        Optional<StatusEffect> result = EffectRegistry.getInstance().getEffect("poison");
        assertTrue(result.isPresent());
        assertEquals("poison", result.get().getId());
    }

    @Test
    void shouldReturnEmptyForMissingEffect() {
        assertFalse(EffectRegistry.getInstance().getEffect("nonexistent").isPresent());
    }

    @Test
    void shouldClearEffects() {
        EffectRegistry.getInstance().register("a", new StatusEffect("a", StatusEffect.EffectType.BUFF));
        EffectRegistry.getInstance().register("b", new StatusEffect("b", StatusEffect.EffectType.DEBUFF));
        assertEquals(2, EffectRegistry.getInstance().size());

        EffectRegistry.getInstance().clear();
        assertEquals(0, EffectRegistry.getInstance().size());
    }
}
