package com.midgardbot.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    void constants_haveExpectedValues() {
        assertEquals("1.0.2", Constants.BOT_VERSION);
        assertEquals("MidgardBot", Constants.BOT_NAME);
    }

    @Test
    void timeConstants_arePositive() {
        assertTrue(Constants.RATE_LIMIT_COOLDOWN_MS > 0);
        assertTrue(Constants.DEBOUNCE_INTERVAL_MS > 0);
        assertTrue(Constants.CACHE_CLEANUP_INTERVAL_MS > 0);
        assertTrue(Constants.WHITELIST_REFILL_MS > 0);
    }

    @Test
    void networkConstants_haveValidDefaults() {
        assertTrue(Constants.DEFAULT_SOCKET_PORT > 0 && Constants.DEFAULT_SOCKET_PORT < 65536);
        assertTrue(Constants.DEFAULT_LOBBY_PORT > 0 && Constants.DEFAULT_LOBBY_PORT < 65536);
        assertTrue(Constants.DEFAULT_RPG_PORT > 0 && Constants.DEFAULT_RPG_PORT < 65536);
        assertTrue(Constants.DEFAULT_RCON_PORT > 0 && Constants.DEFAULT_RCON_PORT < 65536);
        assertNotNull(Constants.DEFAULT_SERVER_IP);
    }

    @Test
    void colorConstants_areValidRGB() {
        // Cores devem estar no range 0x000000 a 0xFFFFFF
        assertTrue(Constants.COLOR_SUCCESS >= 0 && Constants.COLOR_SUCCESS <= 0xFFFFFF);
        assertTrue(Constants.COLOR_ERROR >= 0 && Constants.COLOR_ERROR <= 0xFFFFFF);
        assertTrue(Constants.COLOR_WARNING >= 0 && Constants.COLOR_WARNING <= 0xFFFFFF);
        assertTrue(Constants.COLOR_INFO >= 0 && Constants.COLOR_INFO <= 0xFFFFFF);
    }

    @Test
    void databaseConstants_areValid() {
        assertEquals(3306, Constants.DEFAULT_DB_PORT);
        assertTrue(Constants.HIKARI_MAX_POOL_SIZE_MYSQL > 0);
        assertEquals(1, Constants.HIKARI_MAX_POOL_SIZE_SQLITE);
        assertNotNull(Constants.SQLITE_DB_PATH);
    }
}
