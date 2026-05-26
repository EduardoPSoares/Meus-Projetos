package me.ray.midgard.core.attribute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributeInstanceExtendedTest {

    private Attribute strength;
    private AttributeInstance instance;

    @BeforeEach
    void setUp() {
        strength = new Attribute("str", "Força", 100, 0, 1000);
        instance = new AttributeInstance(strength);
    }

    @Test
    void shouldReturnBaseValueWithNoModifiers() {
        assertEquals(100.0, instance.getValue());
    }

    @Test
    void shouldApplyAddNumberModifier() {
        instance.addModifier(new AttributeModifier("Sword", 25.0, AttributeOperation.ADD_NUMBER));
        assertEquals(125.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldStackMultipleAddModifiers() {
        instance.addModifier(new AttributeModifier("Sword", 25.0, AttributeOperation.ADD_NUMBER));
        instance.addModifier(new AttributeModifier("Ring", 10.0, AttributeOperation.ADD_NUMBER));
        assertEquals(135.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldApplyMultiplyPercentageAdditive() {
        // (100) * (1 + 0.5) = 150
        instance.addModifier(new AttributeModifier("Buff", 0.5, AttributeOperation.MULTIPLY_PERCENTAGE_ADDITIVE));
        assertEquals(150.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldStackPercentageAdditives() {
        // (100) * (1 + 0.5 + 0.3) = 100 * 1.8 = 180
        instance.addModifier(new AttributeModifier("Buff1", 0.5, AttributeOperation.MULTIPLY_PERCENTAGE_ADDITIVE));
        instance.addModifier(new AttributeModifier("Buff2", 0.3, AttributeOperation.MULTIPLY_PERCENTAGE_ADDITIVE));
        assertEquals(180.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldApplyMultiplyScalar() {
        // 100 * 2.0 = 200
        instance.addModifier(new AttributeModifier("Doubler", 2.0, AttributeOperation.MULTIPLY_SCALAR));
        assertEquals(200.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldStackScalarMultipliers() {
        // 100 * 2.0 * 0.5 = 100
        instance.addModifier(new AttributeModifier("Doubler", 2.0, AttributeOperation.MULTIPLY_SCALAR));
        instance.addModifier(new AttributeModifier("Halver", 0.5, AttributeOperation.MULTIPLY_SCALAR));
        assertEquals(100.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldApplyAllOperationsInOrder() {
        // base=100, +10 = 110, *(1+0.5) = 165, *0.5 = 82.5
        instance.addModifier(new AttributeModifier("Add", 10.0, AttributeOperation.ADD_NUMBER));
        instance.addModifier(new AttributeModifier("Percent", 0.5, AttributeOperation.MULTIPLY_PERCENTAGE_ADDITIVE));
        instance.addModifier(new AttributeModifier("Scalar", 0.5, AttributeOperation.MULTIPLY_SCALAR));
        assertEquals(82.5, instance.getValue(), 0.001);
    }

    @Test
    void shouldClampToMaxValue() {
        Attribute hp = new Attribute("hp", "Vida", 50, 0, 100);
        AttributeInstance hpInst = new AttributeInstance(hp);
        hpInst.addModifier(new AttributeModifier("OverPower", 1000.0, AttributeOperation.ADD_NUMBER));
        assertEquals(100.0, hpInst.getValue());
    }

    @Test
    void shouldClampToMinValue() {
        Attribute hp = new Attribute("hp", "Vida", 50, 0, 100);
        AttributeInstance hpInst = new AttributeInstance(hp);
        hpInst.addModifier(new AttributeModifier("Drain", -200.0, AttributeOperation.ADD_NUMBER));
        assertEquals(0.0, hpInst.getValue());
    }

    @Test
    void shouldSetBaseValue() {
        instance.setBaseValue(200);
        assertEquals(200.0, instance.getValue());
    }

    @Test
    void shouldRemoveModifierByName() {
        instance.addModifier(new AttributeModifier("Sword", 25.0, AttributeOperation.ADD_NUMBER));
        assertEquals(125.0, instance.getValue(), 0.001);

        instance.removeModifier("Sword");
        assertEquals(100.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldRemoveModifierByUuid() {
        AttributeModifier mod = new AttributeModifier("Sword", 25.0, AttributeOperation.ADD_NUMBER);
        instance.addModifier(mod);
        assertEquals(125.0, instance.getValue(), 0.001);

        instance.removeModifier(mod.getUuid());
        assertEquals(100.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldRemoveModifierByReference() {
        AttributeModifier mod = new AttributeModifier("Sword", 25.0, AttributeOperation.ADD_NUMBER);
        instance.addModifier(mod);
        instance.removeModifier(mod);
        assertEquals(100.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldCheckHasModifier() {
        assertFalse(instance.hasModifier("Sword"));
        instance.addModifier(new AttributeModifier("Sword", 10, AttributeOperation.ADD_NUMBER));
        assertTrue(instance.hasModifier("Sword"));
    }

    @Test
    void shouldGetAttribute() {
        assertSame(strength, instance.getAttribute());
    }

    @Test
    void shouldRecalculateAfterModification() {
        instance.addModifier(new AttributeModifier("A", 50.0, AttributeOperation.ADD_NUMBER));
        assertEquals(150.0, instance.getValue(), 0.001);

        instance.setBaseValue(200);
        assertEquals(250.0, instance.getValue(), 0.001);
    }

    @Test
    void shouldHandleZeroScalar() {
        instance.addModifier(new AttributeModifier("Zero", 0.0, AttributeOperation.MULTIPLY_SCALAR));
        assertEquals(0.0, instance.getValue());
    }

    @Test
    void shouldHandleNegativePercentage() {
        // 100 * (1 + (-0.5)) = 100 * 0.5 = 50
        instance.addModifier(new AttributeModifier("Debuff", -0.5, AttributeOperation.MULTIPLY_PERCENTAGE_ADDITIVE));
        assertEquals(50.0, instance.getValue(), 0.001);
    }
}
