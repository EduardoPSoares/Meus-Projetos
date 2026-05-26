package me.ray.midgard.core.effect;

import me.ray.midgard.core.profile.MidgardProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ActiveEffectTest {

    @Mock
    StatusEffect effect;

    @Mock
    MidgardProfile profile;

    @Test
    void testDurationTick() {
        // Effect with 3 ticks duration
        ActiveEffect active = new ActiveEffect(effect, 3, UUID.randomUUID());

        // Tick 1: Remaining 2 -> Not expired
        assertFalse(active.tick(profile));

        // Tick 2: Remaining 1 -> Not expired
        assertFalse(active.tick(profile));

        // Tick 3: Remaining 0 -> Expired
        assertTrue(active.tick(profile));
    }
}
