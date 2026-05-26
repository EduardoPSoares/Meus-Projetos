package me.ray.midgard.core.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CooldownManagerExtendedTest {

    private CooldownManager manager;
    private UUID player;

    @BeforeEach
    void setUp() {
        manager = new CooldownManager();
        player = UUID.randomUUID();
    }

    @Test
    void shouldNotBeOnCooldownInitially() {
        assertFalse(manager.isOnCooldown(player, "skill"));
        assertEquals(0, manager.getRemainingMillis(player, "skill"));
    }

    @Test
    void shouldBeOnCooldownAfterSet() {
        manager.setCooldown(player, "skill", Duration.ofSeconds(5));
        assertTrue(manager.isOnCooldown(player, "skill"));
    }

    @Test
    void shouldReturnPositiveRemaining() {
        manager.setCooldown(player, "skill", Duration.ofSeconds(5));
        long remaining = manager.getRemainingMillis(player, "skill");
        assertTrue(remaining > 0 && remaining <= 5000);
    }

    @Test
    void shouldExpireAfterDuration() throws InterruptedException {
        manager.setCooldown(player, "skill", Duration.ofMillis(50));
        assertTrue(manager.isOnCooldown(player, "skill"));
        Thread.sleep(100);
        assertFalse(manager.isOnCooldown(player, "skill"));
    }

    @Test
    void shouldTrackDifferentKeysIndependently() {
        manager.setCooldown(player, "skill_a", Duration.ofSeconds(5));
        assertTrue(manager.isOnCooldown(player, "skill_a"));
        assertFalse(manager.isOnCooldown(player, "skill_b"));
    }

    @Test
    void shouldTrackDifferentPlayersIndependently() {
        UUID player2 = UUID.randomUUID();
        manager.setCooldown(player, "skill", Duration.ofSeconds(5));
        assertTrue(manager.isOnCooldown(player, "skill"));
        assertFalse(manager.isOnCooldown(player2, "skill"));
    }

    @Test
    void shouldFormatRemainingSeconds() {
        manager.setCooldown(player, "skill", Duration.ofSeconds(5));
        String formatted = manager.getRemainingFormatted(player, "skill");
        assertNotNull(formatted);
        assertTrue(formatted.endsWith("s"));
        // Locale-dependent: may use . or , as decimal separator
        assertTrue(formatted.matches(".*[.,].*"));
    }

    @Test
    void shouldFormatZeroWhenNoCooldown() {
        String formatted = manager.getRemainingFormatted(player, "none");
        // Locale-dependent: may use , or . as decimal separator
        assertTrue(formatted.equals("0.0s") || formatted.equals("0,0s"));
    }

    @Test
    void shouldCleanupPlayer() {
        manager.setCooldown(player, "skill_a", Duration.ofSeconds(10));
        manager.setCooldown(player, "skill_b", Duration.ofSeconds(10));
        assertTrue(manager.isOnCooldown(player, "skill_a"));

        manager.cleanupPlayer(player);
        assertFalse(manager.isOnCooldown(player, "skill_a"));
        assertFalse(manager.isOnCooldown(player, "skill_b"));
    }

    @Test
    void shouldOverwriteCooldownOnSameKey() {
        manager.setCooldown(player, "skill", Duration.ofMillis(100));
        manager.setCooldown(player, "skill", Duration.ofSeconds(10));
        assertTrue(manager.isOnCooldown(player, "skill"));
        assertTrue(manager.getRemainingMillis(player, "skill") > 1000);
    }

    @Test
    void shouldReturnZeroRemainingForUnknownPlayer() {
        assertEquals(0, manager.getRemainingMillis(UUID.randomUUID(), "skill"));
    }

    @Test
    void shouldReturnZeroRemainingForUnknownKey() {
        manager.setCooldown(player, "other", Duration.ofSeconds(5));
        assertEquals(0, manager.getRemainingMillis(player, "unknown"));
    }
}
