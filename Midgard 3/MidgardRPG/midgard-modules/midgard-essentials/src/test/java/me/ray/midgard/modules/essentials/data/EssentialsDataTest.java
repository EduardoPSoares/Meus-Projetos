package me.ray.midgard.modules.essentials.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EssentialsDataTest {

    private EssentialsData data;

    @BeforeEach
    void setUp() {
        data = new EssentialsData();
    }

    // --- Defaults ---

    @Test
    void shouldDefaultVanishedToFalse() {
        assertFalse(data.isVanished());
    }

    @Test
    void shouldDefaultLastLoginToCurrentTime() {
        long before = System.currentTimeMillis();
        EssentialsData fresh = new EssentialsData();
        long after = System.currentTimeMillis();
        assertTrue(fresh.getLastLogin() >= before && fresh.getLastLogin() <= after);
    }

    @Test
    void shouldDefaultLastKnownNameToEmpty() {
        assertEquals("", data.getLastKnownName());
    }

    // --- Vanished ---

    @Test
    void shouldSetVanished() {
        data.setVanished(true);
        assertTrue(data.isVanished());
    }

    @Test
    void shouldToggleVanished() {
        data.setVanished(true);
        assertTrue(data.isVanished());
        data.setVanished(false);
        assertFalse(data.isVanished());
    }

    // --- LastLogin ---

    @Test
    void shouldSetLastLogin() {
        long ts = 1234567890L;
        data.setLastLogin(ts);
        assertEquals(ts, data.getLastLogin());
    }

    @Test
    void shouldSetLastLoginToZero() {
        data.setLastLogin(0L);
        assertEquals(0L, data.getLastLogin());
    }

    // --- LastKnownName ---

    @Test
    void shouldSetLastKnownName() {
        data.setLastKnownName("Steve");
        assertEquals("Steve", data.getLastKnownName());
    }

    @Test
    void shouldAllowNullName() {
        data.setLastKnownName(null);
        assertNull(data.getLastKnownName());
    }

    @Test
    void shouldAllowEmptyName() {
        data.setLastKnownName("");
        assertEquals("", data.getLastKnownName());
    }

    // --- ModuleData interface ---

    @Test
    void shouldImplementModuleData() {
        assertInstanceOf(me.ray.midgard.core.profile.ModuleData.class, data);
    }
}
