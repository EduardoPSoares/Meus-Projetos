package me.ray.midgard.core.effect;

import me.ray.midgard.core.profile.MidgardProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ActiveEffectExtendedTest {

    @Mock
    MidgardProfile profile;

    @Test
    void shouldStoreFieldsCorrectly() {
        StatusEffect effect = new StatusEffect("regen", StatusEffect.EffectType.BUFF);
        UUID applier = UUID.randomUUID();
        ActiveEffect active = new ActiveEffect(effect, 100, applier);

        assertSame(effect, active.getEffect());
        assertEquals(100, active.getRemainingDuration());
        assertEquals(applier, active.getApplierId());
    }

    @Test
    void shouldDecrementDurationOnTick() {
        StatusEffect effect = new StatusEffect("test", StatusEffect.EffectType.NEUTRAL);
        ActiveEffect active = new ActiveEffect(effect, 5, UUID.randomUUID());

        assertFalse(active.tick(profile)); // 4 remaining
        assertFalse(active.tick(profile)); // 3
        assertFalse(active.tick(profile)); // 2
        assertFalse(active.tick(profile)); // 1
        assertTrue(active.tick(profile));  // 0 -> expired
    }

    @Test
    void shouldExpireImmediatelyWithDurationOne() {
        StatusEffect effect = new StatusEffect("flash", StatusEffect.EffectType.BUFF);
        ActiveEffect active = new ActiveEffect(effect, 1, UUID.randomUUID());
        assertTrue(active.tick(profile));
    }

    @Test
    void shouldExpireImmediatelyWithDurationZero() {
        StatusEffect effect = new StatusEffect("flash", StatusEffect.EffectType.BUFF);
        ActiveEffect active = new ActiveEffect(effect, 0, UUID.randomUUID());
        // Duration starts at 0, after decrement it's -1, which is <= 0
        assertTrue(active.tick(profile));
    }

    @Test
    void shouldDecrementRemainingDuration() {
        StatusEffect effect = new StatusEffect("test", StatusEffect.EffectType.NEUTRAL);
        ActiveEffect active = new ActiveEffect(effect, 10, UUID.randomUUID());

        active.tick(profile);
        assertEquals(9, active.getRemainingDuration());

        active.tick(profile);
        assertEquals(8, active.getRemainingDuration());
    }
}
