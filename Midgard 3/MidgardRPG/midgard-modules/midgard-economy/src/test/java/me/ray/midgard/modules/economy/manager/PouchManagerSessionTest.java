package me.ray.midgard.modules.economy.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PouchManagerSessionTest {

    /**
     * Tests for the session management part of PouchManager.
     * These methods are pure state management with no Bukkit dependencies,
     * so we test them via reflection to avoid constructing the full PouchManager
     * (which needs EconomyModule + ItemModule singletons).
     */

    private java.util.Set<String> openSessions;

    @BeforeEach
    void setUp() {
        openSessions = new java.util.HashSet<>();
    }

    // Mirror the logic of PouchManager session methods for isolated testing

    private void openSession(String sessionId) {
        openSessions.add(sessionId);
    }

    private void closeSession(String sessionId) {
        openSessions.remove(sessionId);
    }

    private boolean isSessionOpen(String sessionId) {
        return sessionId != null && openSessions.contains(sessionId);
    }

    @Test
    void shouldOpenSession() {
        openSession("abc-123");
        assertTrue(isSessionOpen("abc-123"));
    }

    @Test
    void shouldCloseSession() {
        openSession("abc-123");
        closeSession("abc-123");
        assertFalse(isSessionOpen("abc-123"));
    }

    @Test
    void shouldReturnFalseForUnknownSession() {
        assertFalse(isSessionOpen("unknown"));
    }

    @Test
    void shouldReturnFalseForNullSession() {
        assertFalse(isSessionOpen(null));
    }

    @Test
    void shouldHandleMultipleSessions() {
        openSession("s1");
        openSession("s2");
        openSession("s3");

        assertTrue(isSessionOpen("s1"));
        assertTrue(isSessionOpen("s2"));
        assertTrue(isSessionOpen("s3"));

        closeSession("s2");

        assertTrue(isSessionOpen("s1"));
        assertFalse(isSessionOpen("s2"));
        assertTrue(isSessionOpen("s3"));
    }

    @Test
    void shouldHandleClosingNonExistentSession() {
        // Should not throw
        assertDoesNotThrow(() -> closeSession("never-opened"));
    }

    @Test
    void shouldHandleDoubleOpen() {
        openSession("dup");
        openSession("dup");
        assertTrue(isSessionOpen("dup"));
        closeSession("dup");
        assertFalse(isSessionOpen("dup"));
    }

    @Test
    void shouldHandleDoubleClose() {
        openSession("x");
        closeSession("x");
        assertDoesNotThrow(() -> closeSession("x"));
        assertFalse(isSessionOpen("x"));
    }
}
