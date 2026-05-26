package me.ray.midgard.modules.item.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class StatRangeTest {

    @Nested
    class ConstructorAndGetters {

        @Test
        void shouldStoreMinAndMax() {
            StatRange range = new StatRange(10.0, 20.0);
            assertEquals(10.0, range.getMin());
            assertEquals(20.0, range.getMax());
        }

        @Test
        void shouldHandleEqualMinMax() {
            StatRange range = new StatRange(5.0, 5.0);
            assertEquals(5.0, range.getMin());
            assertEquals(5.0, range.getMax());
        }

        @Test
        void shouldHandleNegativeValues() {
            StatRange range = new StatRange(-10.0, -5.0);
            assertEquals(-10.0, range.getMin());
            assertEquals(-5.0, range.getMax());
        }

        @Test
        void shouldHandleZeroValues() {
            StatRange range = new StatRange(0.0, 0.0);
            assertEquals(0.0, range.getMin());
            assertEquals(0.0, range.getMax());
        }
    }

    @Nested
    class GetRandom {

        @Test
        void shouldReturnMin_whenMinEqualsMax() {
            StatRange range = new StatRange(7.5, 7.5);
            assertEquals(7.5, range.getRandom());
        }

        @Test
        void shouldReturnMin_whenMinGreaterThanMax() {
            StatRange range = new StatRange(10.0, 5.0);
            assertEquals(10.0, range.getRandom());
        }

        @RepeatedTest(20)
        void shouldReturnValueWithinRange() {
            StatRange range = new StatRange(1.0, 100.0);
            double val = range.getRandom();
            assertTrue(val >= 1.0 && val <= 100.0,
                    "Random value " + val + " should be between 1.0 and 100.0");
        }

        @RepeatedTest(10)
        void shouldRoundToTwoDecimalPlaces() {
            StatRange range = new StatRange(1.0, 100.0);
            double val = range.getRandom();
            double rounded = Math.round(val * 100.0) / 100.0;
            assertEquals(rounded, val, 0.0001);
        }
    }

    @Nested
    class ToStringMethod {

        @Test
        void shouldReturnSingleValue_whenMinEqualsMax() {
            StatRange range = new StatRange(5.0, 5.0);
            assertEquals("5.0", range.toString());
        }

        @Test
        void shouldReturnRange_whenMinDiffersFromMax() {
            StatRange range = new StatRange(5.0, 10.0);
            assertEquals("5.0 > 10.0", range.toString());
        }

        @Test
        void shouldHandleIntegerLikeValues() {
            StatRange range = new StatRange(0.0, 0.0);
            assertEquals("0.0", range.toString());
        }
    }

    @Nested
    class Parse {

        @Test
        void shouldParseSingleValue() {
            StatRange range = StatRange.parse("42");
            assertEquals(42.0, range.getMin());
            assertEquals(42.0, range.getMax());
        }

        @Test
        void shouldParseSingleDecimalValue() {
            StatRange range = StatRange.parse("3.14");
            assertEquals(3.14, range.getMin());
            assertEquals(3.14, range.getMax());
        }

        @ParameterizedTest
        @CsvSource({
            "'10 > 20', 10.0, 20.0",
            "'5.5 > 15.5', 5.5, 15.5",
            "'20 > 10', 10.0, 20.0"
        })
        void shouldParseGreaterThanFormat(String input, double expectedMin, double expectedMax) {
            StatRange range = StatRange.parse(input);
            assertEquals(expectedMin, range.getMin());
            assertEquals(expectedMax, range.getMax());
        }

        @ParameterizedTest
        @CsvSource({
            "'10 -> 20', 10.0, 20.0",
            "'5.5 -> 15.5', 5.5, 15.5",
            "'20 -> 10', 10.0, 20.0"
        })
        void shouldParseArrowFormat(String input, double expectedMin, double expectedMax) {
            StatRange range = StatRange.parse(input);
            assertEquals(expectedMin, range.getMin());
            assertEquals(expectedMax, range.getMax());
        }

        @Test
        void shouldParseDashFormat() {
            StatRange range = StatRange.parse("10-20");
            assertEquals(10.0, range.getMin());
            assertEquals(20.0, range.getMax());
        }

        @Test
        void shouldParseDashFormatWithDecimals() {
            StatRange range = StatRange.parse("1.5-3.5");
            assertEquals(1.5, range.getMin());
            assertEquals(3.5, range.getMax());
        }

        @Test
        void shouldParseDashFormatAndSortMinMax() {
            StatRange range = StatRange.parse("30-10");
            assertEquals(10.0, range.getMin());
            assertEquals(30.0, range.getMax());
        }

        @Test
        void shouldParseNegativeNumbersInDashFormat() {
            StatRange range = StatRange.parse("-5-10");
            assertEquals(-5.0, range.getMin());
            assertEquals(10.0, range.getMax());
        }

        @Test
        void shouldReturnZero_forInvalidInput() {
            StatRange range = StatRange.parse("abc");
            assertEquals(0.0, range.getMin());
            assertEquals(0.0, range.getMax());
        }

        @Test
        void shouldReturnZero_forEmptyString() {
            StatRange range = StatRange.parse("");
            assertEquals(0.0, range.getMin());
            assertEquals(0.0, range.getMax());
        }

        @Test
        void shouldHandleWhitespace() {
            StatRange range = StatRange.parse("  42  ");
            assertEquals(42.0, range.getMin());
            assertEquals(42.0, range.getMax());
        }

        @Test
        void shouldHandleWhitespaceInGreaterThanFormat() {
            StatRange range = StatRange.parse("  10 > 20  ");
            assertEquals(10.0, range.getMin());
            assertEquals(20.0, range.getMax());
        }

        @Test
        void shouldHandleWhitespaceInDashFormat() {
            StatRange range = StatRange.parse("  10 - 20  ");
            assertEquals(10.0, range.getMin());
            assertEquals(20.0, range.getMax());
        }
    }
}
