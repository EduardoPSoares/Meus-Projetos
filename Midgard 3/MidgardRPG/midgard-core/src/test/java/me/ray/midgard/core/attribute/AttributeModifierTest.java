package me.ray.midgard.core.attribute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributeModifierTest {

    @Test
    void shouldCreateModifierWithCorrectFields() {
        AttributeModifier mod = new AttributeModifier("Helmet", 10.0, AttributeOperation.ADD_NUMBER);

        assertEquals("Helmet", mod.getName());
        assertEquals(10.0, mod.getAmount());
        assertEquals(AttributeOperation.ADD_NUMBER, mod.getOperation());
        assertNotNull(mod.getUuid());
    }

    @Test
    void shouldGenerateUniqueUUIDs() {
        AttributeModifier mod1 = new AttributeModifier("A", 1, AttributeOperation.ADD_NUMBER);
        AttributeModifier mod2 = new AttributeModifier("A", 1, AttributeOperation.ADD_NUMBER);

        assertNotEquals(mod1.getUuid(), mod2.getUuid());
    }

    @Test
    void shouldSupportAllOperations() {
        assertNotNull(new AttributeModifier("a", 1, AttributeOperation.ADD_NUMBER));
        assertNotNull(new AttributeModifier("b", 0.5, AttributeOperation.MULTIPLY_SCALAR));
        assertNotNull(new AttributeModifier("c", 0.2, AttributeOperation.MULTIPLY_PERCENTAGE_ADDITIVE));
    }

    @Test
    void shouldSupportNegativeAmount() {
        AttributeModifier mod = new AttributeModifier("Curse", -15.0, AttributeOperation.ADD_NUMBER);
        assertEquals(-15.0, mod.getAmount());
    }
}
