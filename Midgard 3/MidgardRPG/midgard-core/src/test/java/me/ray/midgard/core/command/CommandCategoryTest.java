package me.ray.midgard.core.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandCategoryTest {

    @Test
    void shouldHaveThreeCategories() {
        assertEquals(3, CommandCategory.values().length);
    }

    @Test
    void shouldContainExpectedValues() {
        assertNotNull(CommandCategory.valueOf("ADMIN"));
        assertNotNull(CommandCategory.valueOf("PLAYER"));
        assertNotNull(CommandCategory.valueOf("MODERATOR"));
    }
}
