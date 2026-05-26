package me.ray.midgard.core.attribute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AttributeRegistryTest {

    @BeforeEach
    void setUp() {
        AttributeRegistry.getInstance().clear();
    }

    @Test
    void shouldRegisterAndRetrieveAttribute() {
        Attribute attr = new Attribute("str", "Força", 10, 0, 100);
        AttributeRegistry.getInstance().register("str", attr);

        Optional<Attribute> result = AttributeRegistry.getInstance().getAttribute("str");
        assertTrue(result.isPresent());
        assertEquals("str", result.get().getId());
    }

    @Test
    void shouldReturnEmptyForMissingAttribute() {
        Optional<Attribute> result = AttributeRegistry.getInstance().getAttribute("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldBeSingleton() {
        assertSame(AttributeRegistry.getInstance(), AttributeRegistry.getInstance());
    }

    @Test
    void shouldSupportMultipleAttributes() {
        AttributeRegistry.getInstance().register("str", new Attribute("str", "Força", 10, 0, 100));
        AttributeRegistry.getInstance().register("dex", new Attribute("dex", "Destreza", 5, 0, 50));
        AttributeRegistry.getInstance().register("int", new Attribute("int", "Inteligência", 8, 0, 80));

        assertEquals(3, AttributeRegistry.getInstance().size());
        assertTrue(AttributeRegistry.getInstance().contains("str"));
        assertTrue(AttributeRegistry.getInstance().contains("dex"));
        assertTrue(AttributeRegistry.getInstance().contains("int"));
    }

    @Test
    void shouldClearAllAttributes() {
        AttributeRegistry.getInstance().register("str", new Attribute("str", "Força", 10, 0, 100));
        AttributeRegistry.getInstance().clear();
        assertEquals(0, AttributeRegistry.getInstance().size());
    }
}
