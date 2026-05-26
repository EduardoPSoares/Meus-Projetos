package me.ray.midgard.core.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillTypeTest {

    @Test
    void shouldHaveThreeValues() {
        assertEquals(3, SkillType.values().length);
    }

    @Test
    void shouldContainExpectedValues() {
        assertNotNull(SkillType.valueOf("ACTIVE"));
        assertNotNull(SkillType.valueOf("PASSIVE"));
        assertNotNull(SkillType.valueOf("TOGGLE"));
    }
}
