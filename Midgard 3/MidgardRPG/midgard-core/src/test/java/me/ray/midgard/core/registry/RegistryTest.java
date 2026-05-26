package me.ray.midgard.core.registry;

import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RegistryTest {

    @Test
    void testRegistryFlow() {
        Registry<String, Integer> registry = new Registry<>();
        
        // Test Register & Get
        registry.register("one", 1);
        assertTrue(registry.contains("one"));
        assertEquals(Optional.of(1), registry.get("one"));
        
        // Test Missing
        assertFalse(registry.contains("two"));
        assertEquals(Optional.empty(), registry.get("two"));
        
        // Test Size
        assertEquals(1, registry.size());
        
        // Test Clear
        registry.clear();
        assertEquals(0, registry.size());
        assertFalse(registry.contains("one"));
    }
}
