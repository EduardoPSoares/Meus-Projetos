package me.ray.midgard.core.loot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LootTypeTest {

    @Test
    void shouldHaveFourValues() {
        assertEquals(4, LootType.values().length);
    }

    @Test
    void shouldContainAllExpectedTypes() {
        assertNotNull(LootType.valueOf("ITEM"));
        assertNotNull(LootType.valueOf("COMMAND"));
        assertNotNull(LootType.valueOf("MONEY"));
        assertNotNull(LootType.valueOf("EXPERIENCE"));
    }
}
