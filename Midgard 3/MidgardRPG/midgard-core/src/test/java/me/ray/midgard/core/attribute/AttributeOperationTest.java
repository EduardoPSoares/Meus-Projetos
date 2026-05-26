package me.ray.midgard.core.attribute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributeOperationTest {

    @Test
    void shouldHaveExactlyThreeValues() {
        assertEquals(3, AttributeOperation.values().length);
    }

    @Test
    void shouldContainExpectedValues() {
        assertNotNull(AttributeOperation.valueOf("ADD_NUMBER"));
        assertNotNull(AttributeOperation.valueOf("MULTIPLY_SCALAR"));
        assertNotNull(AttributeOperation.valueOf("MULTIPLY_PERCENTAGE_ADDITIVE"));
    }
}
