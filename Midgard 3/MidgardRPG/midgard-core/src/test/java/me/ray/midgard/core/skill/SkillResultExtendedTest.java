package me.ray.midgard.core.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillResultExtendedTest {

    @Test
    void shouldHaveSevenValues() {
        assertEquals(7, SkillResult.values().length);
    }

    @Test
    void shouldContainAllExpectedValues() {
        assertNotNull(SkillResult.valueOf("SUCCESS"));
        assertNotNull(SkillResult.valueOf("FAILURE"));
        assertNotNull(SkillResult.valueOf("ON_COOLDOWN"));
        assertNotNull(SkillResult.valueOf("NO_MANA"));
        assertNotNull(SkillResult.valueOf("NO_TARGET"));
        assertNotNull(SkillResult.valueOf("INVALID_TARGET"));
        assertNotNull(SkillResult.valueOf("CANCELLED"));
    }
}
