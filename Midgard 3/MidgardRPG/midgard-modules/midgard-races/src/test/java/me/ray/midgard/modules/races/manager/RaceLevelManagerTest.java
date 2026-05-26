package me.ray.midgard.modules.races.manager;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RaceLevelManagerTest {

    @Test
    public void testExperienceCalculation() {
        double base = 100.0;
        double mult = 1.25;

        // Level 1: 100 * 1.25^0 = 100
        assertEquals(100.0, RaceLevelManager.calculateRequiredXp(1, base, mult), 0.01);

        // Level 2: 100 * 1.25^1 = 125
        assertEquals(125.0, RaceLevelManager.calculateRequiredXp(2, base, mult), 0.01);

        // Level 3: 100 * 1.25^2 = 156.25
        assertEquals(156.25, RaceLevelManager.calculateRequiredXp(3, base, mult), 0.01);
    }
}
