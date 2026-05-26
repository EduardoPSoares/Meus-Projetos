package me.ray.midgard.core.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillRegistryTest {

    @BeforeEach
    void setUp() {
        SkillRegistry.getInstance().clear();
    }

    @Test
    void shouldBeSingleton() {
        assertSame(SkillRegistry.getInstance(), SkillRegistry.getInstance());
    }

    @Test
    void shouldReturnEmptyForMissingSkill() {
        assertTrue(SkillRegistry.getInstance().getSkill("nonexistent").isEmpty());
    }

    @Test
    void shouldTrackSize() {
        assertEquals(0, SkillRegistry.getInstance().size());
    }
}
