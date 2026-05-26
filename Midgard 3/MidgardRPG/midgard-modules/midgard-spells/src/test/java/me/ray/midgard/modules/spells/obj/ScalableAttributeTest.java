package me.ray.midgard.modules.spells.obj;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ScalableAttributeTest {

    @Test
    @DisplayName("calculate: level 1 deve retornar base")
    void shouldReturnBase_atLevel1() {
        ScalableAttribute attr = new ScalableAttribute(10.0, 2.0);
        assertEquals(10.0, attr.calculate(1), 0.001);
    }

    @Test
    @DisplayName("calculate: level 2 deve retornar base + perLevel")
    void shouldReturnBasePlusPerLevel_atLevel2() {
        ScalableAttribute attr = new ScalableAttribute(10.0, 2.0);
        assertEquals(12.0, attr.calculate(2), 0.001);
    }

    @Test
    @DisplayName("calculate: level 5")
    void shouldScaleCorrectly_atLevel5() {
        // base + perLevel * (5-1) = 10 + 2*4 = 18
        ScalableAttribute attr = new ScalableAttribute(10.0, 2.0);
        assertEquals(18.0, attr.calculate(5), 0.001);
    }

    @Test
    @DisplayName("calculate: level 10")
    void shouldScaleCorrectly_atLevel10() {
        // base + perLevel * (10-1) = 10 + 2*9 = 28
        ScalableAttribute attr = new ScalableAttribute(10.0, 2.0);
        assertEquals(28.0, attr.calculate(10), 0.001);
    }

    @Test
    @DisplayName("calculate: perLevel negativo (diminui com nível)")
    void shouldHandleNegativePerLevel() {
        // 20.0 + (-1.5) * (5-1) = 20 - 6 = 14
        ScalableAttribute attr = new ScalableAttribute(20.0, -1.5);
        assertEquals(14.0, attr.calculate(5), 0.001);
    }

    @Test
    @DisplayName("calculate: nunca retorna negativo (clamp a 0)")
    void shouldClampToZero() {
        // 5.0 + (-10.0) * (3-1) = 5 - 20 = -15 → clamped to 0
        ScalableAttribute attr = new ScalableAttribute(5.0, -10.0);
        assertEquals(0.0, attr.calculate(3), 0.001);
    }

    @Test
    @DisplayName("calculate: level 0 deve usar max(0, level-1) = 0")
    void shouldHandleLevelZero() {
        ScalableAttribute attr = new ScalableAttribute(10.0, 5.0);
        assertEquals(10.0, attr.calculate(0), 0.001);
    }

    @Test
    @DisplayName("of: cria attribute sem escalonamento")
    void shouldCreateStaticAttribute() {
        ScalableAttribute attr = ScalableAttribute.of(15.0);
        assertEquals(15.0, attr.base());
        assertEquals(0.0, attr.perLevel());
        assertEquals(15.0, attr.calculate(1));
        assertEquals(15.0, attr.calculate(10));
    }

    @Test
    @DisplayName("Record: base e perLevel acessíveis como campos")
    void shouldExposeRecordFields() {
        ScalableAttribute attr = new ScalableAttribute(8.0, 1.5);
        assertEquals(8.0, attr.base());
        assertEquals(1.5, attr.perLevel());
    }
}
