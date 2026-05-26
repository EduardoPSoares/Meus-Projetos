package me.ray.midgard.modules.professions.blacksmith.forge.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class MaterialGradeTest {

    @Test
    @DisplayName("deve haver exatamente 5 grades")
    void shouldHaveExactly5Grades() {
        assertEquals(5, MaterialGrade.values().length);
    }

    @Test
    @DisplayName("IMPURE deve ter o menor multiplicador (0.4)")
    void shouldHaveLowestMultiplierForImpure() {
        assertEquals(0.4, MaterialGrade.IMPURE.getQualityMultiplier(), 0.001);
    }

    @Test
    @DisplayName("PRISTINE deve ter o maior multiplicador (1.0)")
    void shouldHaveHighestMultiplierForPristine() {
        assertEquals(1.0, MaterialGrade.PRISTINE.getQualityMultiplier(), 0.001);
    }

    @Test
    @DisplayName("multiplicadores devem crescer com cada grade")
    void shouldHaveIncreasingMultipliers() {
        MaterialGrade[] grades = MaterialGrade.values();
        for (int i = 1; i < grades.length; i++) {
            assertTrue(grades[i].getQualityMultiplier() > grades[i - 1].getQualityMultiplier(),
                    grades[i].name() + " deve ter multiplicador maior que " + grades[i - 1].name());
        }
    }

    @Test
    @DisplayName("getDefault deve retornar REFINED")
    void shouldReturnRefinedAsDefault() {
        assertEquals(MaterialGrade.REFINED, MaterialGrade.getDefault());
    }

    @Test
    @DisplayName("fromString deve retornar grade correta (case insensitive)")
    void shouldParseCaseInsensitive() {
        assertEquals(MaterialGrade.PURE, MaterialGrade.fromString("pure"));
        assertEquals(MaterialGrade.PURE, MaterialGrade.fromString("PURE"));
        assertEquals(MaterialGrade.CRUDE, MaterialGrade.fromString("crude"));
    }

    @Test
    @DisplayName("fromString deve retornar REFINED para string inválida")
    void shouldReturnRefinedForInvalidString() {
        assertEquals(MaterialGrade.REFINED, MaterialGrade.fromString("invalid"));
        assertEquals(MaterialGrade.REFINED, MaterialGrade.fromString("xyz"));
    }

    @ParameterizedTest
    @EnumSource(MaterialGrade.class)
    @DisplayName("displayName não deve ser nulo para nenhuma grade")
    void shouldHaveDisplayNameForAllGrades(MaterialGrade grade) {
        assertNotNull(grade.getDisplayName());
        assertFalse(grade.getDisplayName().isEmpty());
    }

    @Test
    @DisplayName("REFINED deve ter multiplicador 0.8")
    void shouldHaveCorrectRefinedMultiplier() {
        assertEquals(0.8, MaterialGrade.REFINED.getQualityMultiplier(), 0.001);
    }

    @Test
    @DisplayName("CRUDE deve ter multiplicador 0.6")
    void shouldHaveCorrectCrudeMultiplier() {
        assertEquals(0.6, MaterialGrade.CRUDE.getQualityMultiplier(), 0.001);
    }

    @Test
    @DisplayName("PURE deve ter multiplicador 0.95")
    void shouldHaveCorrectPureMultiplier() {
        assertEquals(0.95, MaterialGrade.PURE.getQualityMultiplier(), 0.001);
    }
}
