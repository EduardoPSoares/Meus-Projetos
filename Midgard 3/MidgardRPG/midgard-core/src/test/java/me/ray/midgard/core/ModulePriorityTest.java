package me.ray.midgard.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModulePriorityTest {

    @Test
    void shouldHaveFiveValues() {
        assertEquals(5, ModulePriority.values().length);
    }

    @Test
    void shouldHaveCorrectValues() {
        assertEquals(0, ModulePriority.LOWEST.getValue());
        assertEquals(1, ModulePriority.LOW.getValue());
        assertEquals(2, ModulePriority.NORMAL.getValue());
        assertEquals(3, ModulePriority.HIGH.getValue());
        assertEquals(4, ModulePriority.HIGHEST.getValue());
    }

    @Test
    void shouldBeOrderedByValue() {
        assertTrue(ModulePriority.LOWEST.getValue() < ModulePriority.LOW.getValue());
        assertTrue(ModulePriority.LOW.getValue() < ModulePriority.NORMAL.getValue());
        assertTrue(ModulePriority.NORMAL.getValue() < ModulePriority.HIGH.getValue());
        assertTrue(ModulePriority.HIGH.getValue() < ModulePriority.HIGHEST.getValue());
    }
}
