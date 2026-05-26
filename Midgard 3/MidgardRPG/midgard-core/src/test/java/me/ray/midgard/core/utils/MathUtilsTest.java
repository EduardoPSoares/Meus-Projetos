package me.ray.midgard.core.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MathUtilsTest {

    @Test
    void testFormat() {
        // Debug outputs
        System.out.println("100.0 -> " + MathUtils.format(100.0));
        System.out.println("999.99 -> " + MathUtils.format(999.99));
        System.out.println("1000.0 -> " + MathUtils.format(1000.0));
        System.out.println("1500.0 -> " + MathUtils.format(1500.0));
        System.out.println("-1000.0 -> " + MathUtils.format(-1000.0));

        assertEquals("100", MathUtils.format(100.0));
        assertEquals("999.99", MathUtils.format(999.99));
        assertEquals("1k", MathUtils.format(1000.0));
        assertEquals("1.5k", MathUtils.format(1500.0));
        assertEquals("1M", MathUtils.format(1_000_000.0));
        assertEquals("1.5M", MathUtils.format(1_500_000.0));
        assertEquals("-1k", MathUtils.format(-1000.0));
    }

    @Test
    void testRound() {
        assertEquals(10.5, MathUtils.round(10.49, 1));
        assertEquals(10.56, MathUtils.round(10.555, 2));
        assertEquals(10.0, MathUtils.round(10.001, 1));
    }

    @Test
    void testRandomRange() {
        for (int i = 0; i < 100; i++) {
            int val = MathUtils.randomRange(1, 10);
            assertTrue(val >= 1 && val <= 10, "Value " + val + " out of range");
        }
    }
}
