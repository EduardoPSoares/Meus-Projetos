package me.ray.midgard.modules.item.socket;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SocketEntryTest {

    @Test
    void shouldStoreTypeAndGemId() {
        SocketEntry entry = new SocketEntry("WEAPON", "ruby_gem");
        assertEquals("WEAPON", entry.getType());
        assertEquals("ruby_gem", entry.getGemId());
    }

    @Test
    void shouldBeEmpty_whenGemIdIsNull() {
        SocketEntry entry = new SocketEntry("ARMOR", null);
        assertTrue(entry.isEmpty());
    }

    @Test
    void shouldNotBeEmpty_whenGemIdIsSet() {
        SocketEntry entry = new SocketEntry("WEAPON", "diamond_gem");
        assertFalse(entry.isEmpty());
    }

    @Test
    void shouldUpdateGemId() {
        SocketEntry entry = new SocketEntry("WEAPON", null);
        assertTrue(entry.isEmpty());

        entry.setGemId("emerald_gem");
        assertFalse(entry.isEmpty());
        assertEquals("emerald_gem", entry.getGemId());
    }

    @Test
    void shouldClearGemId() {
        SocketEntry entry = new SocketEntry("WEAPON", "ruby_gem");
        entry.setGemId(null);
        assertTrue(entry.isEmpty());
    }
}
