package me.ray.midgard.core.effect;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EffectDataTest {

    @Test
    void shouldStartEmpty() {
        EffectData data = new EffectData();
        assertTrue(data.getActiveEffects().isEmpty());
    }

    @Test
    void shouldAddEffect() {
        EffectData data = new EffectData();
        StatusEffect effect = new StatusEffect("regen", StatusEffect.EffectType.BUFF);
        ActiveEffect active = new ActiveEffect(effect, 100, UUID.randomUUID());

        data.addEffect(active);
        assertEquals(1, data.getActiveEffects().size());
        assertSame(active, data.getActiveEffects().get(0));
    }

    @Test
    void shouldRemoveEffect() {
        EffectData data = new EffectData();
        StatusEffect effect = new StatusEffect("regen", StatusEffect.EffectType.BUFF);
        ActiveEffect active = new ActiveEffect(effect, 100, UUID.randomUUID());

        data.addEffect(active);
        data.removeEffect(active);
        assertTrue(data.getActiveEffects().isEmpty());
    }

    @Test
    void shouldSupportMultipleEffects() {
        EffectData data = new EffectData();
        data.addEffect(new ActiveEffect(new StatusEffect("a", StatusEffect.EffectType.BUFF), 10, UUID.randomUUID()));
        data.addEffect(new ActiveEffect(new StatusEffect("b", StatusEffect.EffectType.DEBUFF), 20, UUID.randomUUID()));
        data.addEffect(new ActiveEffect(new StatusEffect("c", StatusEffect.EffectType.NEUTRAL), 30, UUID.randomUUID()));

        assertEquals(3, data.getActiveEffects().size());
    }
}
