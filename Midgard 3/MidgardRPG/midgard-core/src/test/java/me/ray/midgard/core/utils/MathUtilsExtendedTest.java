package me.ray.midgard.core.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class MathUtilsExtendedTest {

    // ========== format() ==========

    @ParameterizedTest
    @CsvSource({
        "0.0, 0",
        "1.0, 1",
        "99.9, 99.9",
        "100.0, 100",
        "999.99, 999.99",
        "1000.0, 1k",
        "1500.0, 1.5k",
        "2000.0, 2k",
        "10000.0, 10k",
        "100000.0, 100k",
        "1000000.0, 1M",
        "1500000.0, 1.5M",
        "1000000000.0, 1B",
        "1000000000000.0, 1T"
    })
    void shouldFormatNumbers(double input, String expected) {
        assertEquals(expected, MathUtils.format(input));
    }

    @Test
    void shouldFormatNegativeNumbers() {
        assertEquals("-1k", MathUtils.format(-1000.0));
        assertEquals("-1.5M", MathUtils.format(-1_500_000.0));
    }

    // ========== round() ==========

    @Test
    void shouldRoundToDecimalPlaces() {
        assertEquals(10.5, MathUtils.round(10.49, 1));
        assertEquals(10.56, MathUtils.round(10.555, 2));
        assertEquals(10.0, MathUtils.round(10.001, 1));
        assertEquals(10.0, MathUtils.round(10.0, 0));
    }

    @Test
    void shouldRejectNegativeDecimalPlaces() {
        assertThrows(IllegalArgumentException.class, () -> MathUtils.round(10.0, -1));
    }

    // ========== randomRange() ==========

    @Test
    void shouldReturnValueInRange() {
        for (int i = 0; i < 200; i++) {
            int val = MathUtils.randomRange(5, 15);
            assertTrue(val >= 5 && val <= 15, "Value " + val + " out of range [5, 15]");
        }
    }

    @Test
    void shouldReturnExactValueWhenMinEqualsMax() {
        for (int i = 0; i < 50; i++) {
            assertEquals(7, MathUtils.randomRange(7, 7));
        }
    }

    // ========== chance() ==========

    @Test
    void shouldAlwaysFailWithZeroChance() {
        for (int i = 0; i < 100; i++) {
            assertFalse(MathUtils.chance(0));
        }
    }

    @Test
    void shouldAlwaysPassWith100Chance() {
        for (int i = 0; i < 100; i++) {
            assertTrue(MathUtils.chance(100));
        }
    }

    @Test
    void shouldReturnMixedResultsForFiftyPercent() {
        int trueCount = 0;
        int total = 10000;
        for (int i = 0; i < total; i++) {
            if (MathUtils.chance(50)) {
                trueCount++;
            }
        }
        // Estatisticamente, deve ficar entre 40% e 60%
        assertTrue(trueCount > total * 0.4 && trueCount < total * 0.6,
            "50% chance retornou " + trueCount + "/" + total + " trues");
    }
}
