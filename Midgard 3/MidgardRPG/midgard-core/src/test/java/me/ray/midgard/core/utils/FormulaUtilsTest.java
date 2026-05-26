package me.ray.midgard.core.utils;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormulaUtilsTest {

    @Test
    void testBasicArithmetic() {
        assertEquals(15.0, FormulaUtils.eval("10 + 5"), 0.001);
        assertEquals(5.0, FormulaUtils.eval("10 - 5"), 0.001);
        assertEquals(50.0, FormulaUtils.eval("10 * 5"), 0.001);
        assertEquals(2.0, FormulaUtils.eval("10 / 5"), 0.001);
    }

    @Test
    void testPrecedence() {
        // Multiplicação deve acontecer antes da adição
        assertEquals(20.0, FormulaUtils.eval("10 + 2 * 5"), 0.001);
        // Parênteses devem alterar a precedência
        assertEquals(60.0, FormulaUtils.eval("(10 + 2) * 5"), 0.001);
    }

    @Test
    void testVariables() {
        Map<String, Double> vars = Map.of(
            "str", 10.0,
            "dex", 5.0
        );
        
        // str * 2 + dex
        assertEquals(25.0, FormulaUtils.evaluate("str * 2 + dex", vars), 0.001);
    }

    @Test
    void testFunctions() {
        assertEquals(4.0, FormulaUtils.eval("sqrt(16)"), 0.001);
        assertEquals(1.0, FormulaUtils.eval("sin(90)"), 0.001); // Assumindo input em graus conforme código
    }

    @Test
    void testComplexFormula() {
        // Exemplo de fórmula de dano: (base + str) * multiplier
        String formula = "(base + str) * multi";
        Map<String, Double> vars = Map.of(
            "base", 100.0,
            "str", 50.0,
            "multi", 1.5
        );
        
        assertEquals(225.0, FormulaUtils.evaluate(formula, vars), 0.001);
    }
}
