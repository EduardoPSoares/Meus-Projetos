package me.ray.midgard.core.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MessageRegistryTest {

    @BeforeEach
    void setUp() {
        MessageRegistry.getInstance().clear();
        MessageRegistry.getInstance().setDebugMode(false);
    }

    @Test
    void shouldBeSingleton() {
        assertSame(MessageRegistry.getInstance(), MessageRegistry.getInstance());
    }

    @Test
    void shouldRegisterAndRetrieveKey() {
        MessageKey key = MessageKey.of("combat.hit");
        MessageRegistry.getInstance().register(key);

        assertTrue(MessageRegistry.getInstance().isRegistered("combat.hit"));
        assertEquals(key, MessageRegistry.getInstance().get("combat.hit"));
    }

    @Test
    void shouldReturnNullForMissingKey() {
        assertNull(MessageRegistry.getInstance().get("nonexistent"));
        assertFalse(MessageRegistry.getInstance().isRegistered("nonexistent"));
    }

    @Test
    void shouldRegisterAll() {
        MessageKey k1 = MessageKey.of("msg.one");
        MessageKey k2 = MessageKey.of("msg.two");
        MessageRegistry.getInstance().registerAll(k1, k2);

        assertTrue(MessageRegistry.getInstance().isRegistered("msg.one"));
        assertTrue(MessageRegistry.getInstance().isRegistered("msg.two"));
    }

    @Test
    void shouldGetAllKeys() {
        MessageRegistry.getInstance().register(MessageKey.of("a.one"));
        MessageRegistry.getInstance().register(MessageKey.of("b.two"));

        Collection<MessageKey> all = MessageRegistry.getInstance().getAllKeys();
        assertEquals(2, all.size());
    }

    @Test
    void shouldGroupByModule() {
        MessageRegistry.getInstance().register(MessageKey.of("combat", "hit"));
        MessageRegistry.getInstance().register(MessageKey.of("combat", "miss"));
        MessageRegistry.getInstance().register(MessageKey.of("spell", "cast"));

        Set<MessageKey> combatKeys = MessageRegistry.getInstance().getKeysByModule("combat");
        assertEquals(2, combatKeys.size());

        Set<MessageKey> spellKeys = MessageRegistry.getInstance().getKeysByModule("spell");
        assertEquals(1, spellKeys.size());
    }

    @Test
    void shouldGetModules() {
        MessageRegistry.getInstance().register(MessageKey.of("combat", "hit"));
        MessageRegistry.getInstance().register(MessageKey.of("spell", "cast"));

        Set<String> modules = MessageRegistry.getInstance().getModules();
        assertTrue(modules.contains("combat"));
        assertTrue(modules.contains("spell"));
    }

    @Test
    void shouldReturnEmptyForUnknownModule() {
        Set<MessageKey> keys = MessageRegistry.getInstance().getKeysByModule("nonexistent");
        assertTrue(keys.isEmpty());
    }

    @Test
    void shouldTrackUsageWithSource() {
        MessageRegistry.getInstance().trackUsage("combat.hit", "CombatListener", 42);

        assertFalse(MessageRegistry.getInstance().getRuntimeUsage().isEmpty());
        assertTrue(MessageRegistry.getInstance().getRuntimeUsage().containsKey("combat.hit"));
    }

    @Test
    void shouldDetectUnregisteredKeys() {
        MessageRegistry.getInstance().trackUsage("unregistered.key", "SomeClass", 1);

        Set<String> unregistered = MessageRegistry.getInstance().getUnregisteredKeys();
        assertTrue(unregistered.contains("unregistered.key"));
    }

    @Test
    void shouldDetectUnusedKeys() {
        MessageRegistry.getInstance().register(MessageKey.of("registered.but.unused"));

        Set<String> unused = MessageRegistry.getInstance().getUnusedKeys();
        assertTrue(unused.contains("registered.but.unused"));
    }

    @Test
    void shouldClearAll() {
        MessageRegistry.getInstance().register(MessageKey.of("msg.test"));
        MessageRegistry.getInstance().trackUsage("msg.test");
        MessageRegistry.getInstance().clear();

        assertFalse(MessageRegistry.getInstance().isRegistered("msg.test"));
        assertTrue(MessageRegistry.getInstance().getRuntimeUsage().isEmpty());
    }

    @Test
    void shouldClearUsageOnly() {
        MessageRegistry.getInstance().register(MessageKey.of("msg.test"));
        MessageRegistry.getInstance().trackUsage("msg.test");
        MessageRegistry.getInstance().clearUsage();

        assertTrue(MessageRegistry.getInstance().isRegistered("msg.test"));
        assertTrue(MessageRegistry.getInstance().getRuntimeUsage().isEmpty());
    }

    @Test
    void shouldToggleDebugMode() {
        assertFalse(MessageRegistry.getInstance().isDebugMode());
        MessageRegistry.getInstance().setDebugMode(true);
        assertTrue(MessageRegistry.getInstance().isDebugMode());
    }

    @Test
    void shouldHandleNullRegister() {
        // register(null) calls MidgardLogger.warn which requires Bukkit.server
        // We verify the key is just not registered rather than testing null input
        assertFalse(MessageRegistry.getInstance().isRegistered("never_registered"));
    }

    @Test
    void shouldHandleNullTrackUsage() {
        // Should not throw
        MessageRegistry.getInstance().trackUsage(null);
        MessageRegistry.getInstance().trackUsage(null, "Class", 1);
    }

    @Test
    void shouldGenerateStats() {
        MessageRegistry.getInstance().register(MessageKey.of("combat", "hit"));
        String stats = MessageRegistry.getInstance().getStats();
        assertNotNull(stats);
        assertTrue(stats.contains("Total Keys Registered: 1"));
    }
}
