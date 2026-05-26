package me.ray.midgard.core.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormulaUtilsExtendedTest {

    // ========== Aritmética básica ==========

    @Test
    void shouldEvaluateAddition() {
        assertEquals(15.0, FormulaUtils.eval("10 + 5"), 0.001);
    }

    @Test
    void shouldEvaluateSubtraction() {
        assertEquals(5.0, FormulaUtils.eval("10 - 5"), 0.001);
    }

    @Test
    void shouldEvaluateMultiplication() {
        assertEquals(50.0, FormulaUtils.eval("10 * 5"), 0.001);
    }

    @Test
    void shouldEvaluateDivision() {
        assertEquals(2.0, FormulaUtils.eval("10 / 5"), 0.001);
    }

    @Test
    void shouldEvaluateModulus() {
        assertEquals(1.0, FormulaUtils.eval("10 % 3"), 0.001);
    }

    @Test
    void shouldEvaluateExponentiation() {
        assertEquals(8.0, FormulaUtils.eval("2 ^ 3"), 0.001);
    }

    // ========== Precedência ==========

    @Test
    void shouldRespectMultiplicationPrecedence() {
        assertEquals(20.0, FormulaUtils.eval("10 + 2 * 5"), 0.001);
    }

    @Test
    void shouldRespectParentheses() {
        assertEquals(60.0, FormulaUtils.eval("(10 + 2) * 5"), 0.001);
    }

    @Test
    void shouldHandleNestedParentheses() {
        assertEquals(30.0, FormulaUtils.eval("((2 + 3) * (4 + 2))"), 0.001);
    }

    // ========== Funções ==========

    @Test
    void shouldEvaluateSqrt() {
        assertEquals(4.0, FormulaUtils.eval("sqrt(16)"), 0.001);
    }

    @Test
    void shouldEvaluateSin() {
        assertEquals(1.0, FormulaUtils.eval("sin(90)"), 0.001);
    }

    @Test
    void shouldEvaluateCos() {
        assertEquals(1.0, FormulaUtils.eval("cos(0)"), 0.001);
    }

    @Test
    void shouldEvaluateTan() {
        assertEquals(1.0, FormulaUtils.eval("tan(45)"), 0.001);
    }

    // ========== Unários ==========

    @Test
    void shouldHandleUnaryMinus() {
        assertEquals(-5.0, FormulaUtils.eval("-5"), 0.001);
    }

    @Test
    void shouldHandleUnaryPlus() {
        assertEquals(5.0, FormulaUtils.eval("+5"), 0.001);
    }

    @Test
    void shouldHandleDoubleNegation() {
        assertEquals(5.0, FormulaUtils.eval("--5"), 0.001);
    }

    // ========== Variáveis ==========

    @Test
    void shouldEvaluateWithVariables() {
        Map<String, Double> vars = Map.of("str", 10.0, "dex", 5.0);
        assertEquals(25.0, FormulaUtils.evaluate("str * 2 + dex", vars), 0.001);
    }

    @Test
    void shouldHandleLongVariableNames() {
        Map<String, Double> vars = Map.of("strength", 100.0, "str", 50.0);
        // "strength" deve ser substituído antes de "str" para evitar colisão
        assertEquals(100.0, FormulaUtils.evaluate("strength", vars), 0.001);
    }

    @Test
    void shouldEvaluateComplexDamageFormula() {
        String formula = "(base + str) * multi";
        Map<String, Double> vars = Map.of("base", 100.0, "str", 50.0, "multi", 1.5);
        assertEquals(225.0, FormulaUtils.evaluate(formula, vars), 0.001);
    }

    // ========== Erros ==========

    @Test
    void shouldThrowOnDivisionByZero() {
        assertThrows(ArithmeticException.class, () -> FormulaUtils.eval("10 / 0"));
    }

    @Test
    void shouldThrowOnModulusByZero() {
        assertThrows(ArithmeticException.class, () -> FormulaUtils.eval("10 % 0"));
    }

    @Test
    void shouldThrowOnUnknownFunction() {
        assertThrows(RuntimeException.class, () -> FormulaUtils.eval("log(10)"));
    }

    @Test
    void shouldThrowOnInvalidExpression() {
        assertThrows(RuntimeException.class, () -> FormulaUtils.eval("10 +"));
    }

    // ========== Decimais ==========

    @Test
    void shouldHandleDecimalNumbers() {
        assertEquals(3.14, FormulaUtils.eval("3.14"), 0.001);
        assertEquals(5.5, FormulaUtils.eval("2.5 + 3.0"), 0.001);
    }
}
