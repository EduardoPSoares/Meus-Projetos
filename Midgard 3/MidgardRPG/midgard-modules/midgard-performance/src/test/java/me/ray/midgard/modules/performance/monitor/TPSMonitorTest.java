package me.ray.midgard.modules.performance.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TPSMonitor")
class TPSMonitorTest {

    private TPSMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new TPSMonitor();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should start with TPS 20.0")
        void shouldStartWith20TPS() {
            assertEquals(20.0, monitor.getCurrentTPS());
            assertEquals(20.0, monitor.getAverageTPS());
            assertEquals(20.0, monitor.getMinTPS());
            assertEquals(20.0, monitor.getMaxTPS());
        }
    }

    @Nested
    @DisplayName("run()")
    class RunTests {

        @Test
        @DisplayName("should calculate TPS from elapsed time - perfect 50ms tick")
        void shouldCalculateTPSFromPerfectTick() throws Exception {
            // Simulate that lastTick was 50ms ago (perfect tick = 20 TPS)
            setLastTick(System.nanoTime() - 50_000_000L);
            monitor.run();

            double tps = monitor.getCurrentTPS();
            // Should be approximately 20.0 (clamped to max 20)
            assertTrue(tps >= 19.0 && tps <= 20.0,
                    "TPS should be ~20.0 for a perfect tick, was: " + tps);
        }

        @Test
        @DisplayName("should calculate lower TPS for slow tick")
        void shouldCalculateLowerTPSForSlowTick() throws Exception {
            // Simulate that lastTick was 100ms ago (100ms per tick = 10 TPS)
            setLastTick(System.nanoTime() - 100_000_000L);
            monitor.run();

            double tps = monitor.getCurrentTPS();
            assertTrue(tps >= 9.0 && tps <= 11.0,
                    "TPS should be ~10.0 for 100ms tick, was: " + tps);
        }

        @Test
        @DisplayName("should clamp TPS between 0 and 20")
        void shouldClampTPS() throws Exception {
            // Very fast tick (10ms) would give 100 TPS, but should clamp to 20
            setLastTick(System.nanoTime() - 10_000_000L);
            monitor.run();

            assertTrue(monitor.getCurrentTPS() <= 20.0,
                    "TPS should not exceed 20.0");
            assertTrue(monitor.getCurrentTPS() >= 0.0,
                    "TPS should not be negative");
        }

        @Test
        @DisplayName("should accumulate history up to SAMPLE_SIZE")
        void shouldAccumulateHistory() throws Exception {
            for (int i = 0; i < 5; i++) {
                setLastTick(System.nanoTime() - 50_000_000L);
                monitor.run();
            }

            // After 5 runs, average should be reasonable
            double avg = monitor.getAverageTPS();
            assertTrue(avg > 0 && avg <= 20.0,
                    "Average TPS should be in valid range, was: " + avg);
        }

        @Test
        @DisplayName("should track min and max TPS")
        void shouldTrackMinAndMax() throws Exception {
            // First run - fast tick (high TPS, clamped to 20)
            setLastTick(System.nanoTime() - 25_000_000L);
            monitor.run();

            // Second run - slow tick (lower TPS)
            setLastTick(System.nanoTime() - 200_000_000L);
            monitor.run();

            assertTrue(monitor.getMinTPS() < monitor.getMaxTPS(),
                    "Min TPS should be less than Max TPS");
        }
    }

    @Nested
    @DisplayName("getTPSColor()")
    class TPSColorTests {

        @Test
        @DisplayName("should return green for TPS >= 19")
        void shouldReturnGreenForHighTPS() {
            assertEquals("<green>", monitor.getTPSColor(20.0));
            assertEquals("<green>", monitor.getTPSColor(19.0));
            assertEquals("<green>", monitor.getTPSColor(19.5));
        }

        @Test
        @DisplayName("should return yellow for TPS >= 15 and < 19")
        void shouldReturnYellowForMediumTPS() {
            assertEquals("<yellow>", monitor.getTPSColor(18.9));
            assertEquals("<yellow>", monitor.getTPSColor(15.0));
            assertEquals("<yellow>", monitor.getTPSColor(17.0));
        }

        @Test
        @DisplayName("should return gold for TPS >= 10 and < 15")
        void shouldReturnGoldForLowTPS() {
            assertEquals("<gold>", monitor.getTPSColor(14.9));
            assertEquals("<gold>", monitor.getTPSColor(10.0));
            assertEquals("<gold>", monitor.getTPSColor(12.0));
        }

        @Test
        @DisplayName("should return red for TPS < 10")
        void shouldReturnRedForVeryLowTPS() {
            assertEquals("<red>", monitor.getTPSColor(9.9));
            assertEquals("<red>", monitor.getTPSColor(5.0));
            assertEquals("<red>", monitor.getTPSColor(0.0));
        }
    }

    @Nested
    @DisplayName("reset()")
    class ResetTests {

        @Test
        @DisplayName("should reset all values to defaults")
        void shouldResetToDefaults() throws Exception {
            // Populate some data first
            setLastTick(System.nanoTime() - 100_000_000L);
            monitor.run();
            setLastTick(System.nanoTime() - 200_000_000L);
            monitor.run();

            // Reset
            monitor.reset();

            assertEquals(20.0, monitor.getCurrentTPS());
            assertEquals(20.0, monitor.getAverageTPS());
            assertEquals(20.0, monitor.getMinTPS());
            assertEquals(20.0, monitor.getMaxTPS());
        }

        @Test
        @DisplayName("should work correctly after reset and new runs")
        void shouldWorkAfterReset() throws Exception {
            // First run
            setLastTick(System.nanoTime() - 100_000_000L);
            monitor.run();

            monitor.reset();

            // New run after reset
            setLastTick(System.nanoTime() - 50_000_000L);
            monitor.run();

            double tps = monitor.getCurrentTPS();
            assertTrue(tps > 15.0 && tps <= 20.0,
                    "TPS should be reasonable after reset and new run, was: " + tps);
        }
    }

    // ========== Helper ==========

    private void setLastTick(long value) throws Exception {
        Field field = TPSMonitor.class.getDeclaredField("lastTick");
        field.setAccessible(true);
        field.set(monitor, value);
    }
}
