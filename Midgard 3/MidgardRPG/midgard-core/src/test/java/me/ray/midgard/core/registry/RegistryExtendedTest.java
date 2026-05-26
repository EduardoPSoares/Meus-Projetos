package me.ray.midgard.core.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegistryExtendedTest {

    private Registry<String, Integer> registry;

    @BeforeEach
    void setUp() {
        registry = new Registry<>();
    }

    @Test
    void shouldStartEmpty() {
        assertEquals(0, registry.size());
        assertFalse(registry.contains("key"));
    }

    @Test
    void shouldRegisterAndGet() {
        registry.register("one", 1);
        assertEquals(Optional.of(1), registry.get("one"));
    }

    @Test
    void shouldReturnEmptyForMissing() {
        assertEquals(Optional.empty(), registry.get("missing"));
    }

    @Test
    void shouldCheckContains() {
        registry.register("key", 42);
        assertTrue(registry.contains("key"));
        assertFalse(registry.contains("other"));
    }

    @Test
    void shouldReturnSize() {
        registry.register("a", 1);
        registry.register("b", 2);
        registry.register("c", 3);
        assertEquals(3, registry.size());
    }

    @Test
    void shouldGetAllValues() {
        registry.register("a", 1);
        registry.register("b", 2);
        Collection<Integer> values = registry.getAll();
        assertEquals(2, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    void shouldGetAllKeys() {
        registry.register("x", 10);
        registry.register("y", 20);
        Collection<String> keys = registry.getKeys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("x"));
        assertTrue(keys.contains("y"));
    }

    @Test
    void shouldClear() {
        registry.register("a", 1);
        registry.register("b", 2);
        registry.clear();
        assertEquals(0, registry.size());
        assertFalse(registry.contains("a"));
    }

    @Test
    void shouldOverwriteExistingKey() {
        registry.register("key", 1);
        registry.register("key", 2);
        assertEquals(Optional.of(2), registry.get("key"));
        assertEquals(1, registry.size());
    }

    @Test
    void shouldWorkWithDifferentTypes() {
        Registry<Integer, String> intReg = new Registry<>();
        intReg.register(1, "one");
        intReg.register(2, "two");
        assertEquals(Optional.of("one"), intReg.get(1));
    }
}
