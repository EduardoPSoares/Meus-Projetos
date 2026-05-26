package me.ray.midgard.core.attribute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributeTest {

    @Test
    void shouldStoreAllFieldsFromFullConstructor() {
        Attribute attr = new Attribute("str", "Força", 10, 0, 100, "⚔", "#.#");

        assertEquals("str", attr.getId());
        assertEquals("Força", attr.getName());
        assertEquals(10.0, attr.getBaseValue());
        assertEquals(0.0, attr.getMinValue());
        assertEquals(100.0, attr.getMaxValue());
        assertEquals("⚔", attr.getIcon());
        assertEquals("#.#", attr.getFormat());
    }

    @Test
    void shouldUseDefaultsForIconAndFormat() {
        Attribute attr = new Attribute("dex", "Destreza", 5, 0, 50);

        assertEquals("", attr.getIcon());
        assertEquals("0.0", attr.getFormat());
    }

    @Test
    void shouldHandleNullIconAndFormat() {
        Attribute attr = new Attribute("int", "Inteligência", 20, 0, 200, null, null);

        assertEquals("", attr.getIcon());
        assertEquals("0.0", attr.getFormat());
    }

    @Test
    void shouldAllowNegativeBaseValue() {
        Attribute attr = new Attribute("luck", "Sorte", -5, -100, 100);
        assertEquals(-5.0, attr.getBaseValue());
    }
}
