package me.ray.midgard.modules.performance.monitor;

import me.ray.midgard.modules.performance.monitor.SparkIntegration.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SparkIntegration Records")
class SparkIntegrationRecordsTest {

    // ========== SparkTPS ==========

    @Nested
    @DisplayName("SparkTPS")
    class SparkTPSTests {

        @Test
        @DisplayName("should return green for TPS >= 19")
        void shouldReturnGreenForHighTPS() {
            var tps = new SparkTPS(20, 20, 20, 20, 20);
            assertEquals("<green>", tps.getColor(20.0));
            assertEquals("<green>", tps.getColor(19.0));
        }

        @Test
        @DisplayName("should return yellow for TPS >= 15 and < 19")
        void shouldReturnYellowForMediumTPS() {
            var tps = new SparkTPS(17, 17, 17, 17, 17);
            assertEquals("<yellow>", tps.getColor(18.0));
            assertEquals("<yellow>", tps.getColor(15.0));
        }

        @Test
        @DisplayName("should return gold for TPS >= 10 and < 15")
        void shouldReturnGoldForLowTPS() {
            var tps = new SparkTPS(12, 12, 12, 12, 12);
            assertEquals("<gold>", tps.getColor(14.0));
            assertEquals("<gold>", tps.getColor(10.0));
        }

        @Test
        @DisplayName("should return red for TPS < 10")
        void shouldReturnRedForVeryLowTPS() {
            var tps = new SparkTPS(5, 5, 5, 5, 5);
            assertEquals("<red>", tps.getColor(9.9));
            assertEquals("<red>", tps.getColor(0));
        }

        @Test
        @DisplayName("should format TPS with 2 decimal places")
        void shouldFormatTPSWithTwoDecimalPlaces() {
            var tps = new SparkTPS(19.85, 19, 18, 17, 16);
            assertEquals(String.format("%.2f", 19.85), tps.format(19.85));
            assertEquals(String.format("%.2f", 20.0), tps.format(20.0));
        }

        @Test
        @DisplayName("should store all windows correctly")
        void shouldStoreAllWindows() {
            var tps = new SparkTPS(20.0, 19.5, 19.0, 18.5, 18.0);
            assertEquals(20.0, tps.last5s());
            assertEquals(19.5, tps.last10s());
            assertEquals(19.0, tps.last1m());
            assertEquals(18.5, tps.last5m());
            assertEquals(18.0, tps.last15m());
        }
    }

    // ========== SparkMSPT ==========

    @Nested
    @DisplayName("SparkMSPT")
    class SparkMSPTTests {

        @Test
        @DisplayName("should return green for MSPT <= 30")
        void shouldReturnGreenForLowMSPT() {
            var mspt = new SparkMSPT(5, 10, 15, 20, 5, 10, 15, 20);
            assertEquals("<green>", mspt.getColor(30));
            assertEquals("<green>", mspt.getColor(10));
        }

        @Test
        @DisplayName("should return yellow for MSPT > 30 and <= 40")
        void shouldReturnYellowForMediumMSPT() {
            var mspt = new SparkMSPT(5, 35, 38, 40, 5, 35, 38, 40);
            assertEquals("<yellow>", mspt.getColor(35));
            assertEquals("<yellow>", mspt.getColor(40));
        }

        @Test
        @DisplayName("should return gold for MSPT > 40 and <= 50")
        void shouldReturnGoldForHighMSPT() {
            var mspt = new SparkMSPT(5, 45, 48, 50, 5, 45, 48, 50);
            assertEquals("<gold>", mspt.getColor(45));
            assertEquals("<gold>", mspt.getColor(50));
        }

        @Test
        @DisplayName("should return red for MSPT > 50")
        void shouldReturnRedForVeryHighMSPT() {
            var mspt = new SparkMSPT(5, 60, 80, 100, 5, 60, 80, 100);
            assertEquals("<red>", mspt.getColor(60));
        }

        @Test
        @DisplayName("should format MSPT with 1 decimal place")
        void shouldFormatMSPTWithOneDecimalPlace() {
            var mspt = new SparkMSPT(5, 12.34, 15, 20, 5, 12, 15, 20);
            assertEquals(String.format("%.1f", 12.34), mspt.format(12.34));
        }

        @Test
        @DisplayName("should store 10s and 1m windows correctly")
        void shouldStoreWindowsCorrectly() {
            var mspt = new SparkMSPT(1, 2, 3, 4, 5, 6, 7, 8);
            assertEquals(1, mspt.min10s());
            assertEquals(2, mspt.median10s());
            assertEquals(3, mspt.p95_10s());
            assertEquals(4, mspt.max10s());
            assertEquals(5, mspt.min1m());
            assertEquals(6, mspt.median1m());
            assertEquals(7, mspt.p95_1m());
            assertEquals(8, mspt.max1m());
        }
    }

    // ========== SparkCPU ==========

    @Nested
    @DisplayName("SparkCPU")
    class SparkCPUTests {

        @Test
        @DisplayName("should return green for CPU <= 50")
        void shouldReturnGreenForLowCPU() {
            var cpu = new SparkCPU(0.3, 0.4, 0.5, 0.3, 0.4, 0.5);
            assertEquals("<green>", cpu.getColor(50));
            assertEquals("<green>", cpu.getColor(30));
        }

        @Test
        @DisplayName("should return yellow for CPU > 50 and <= 70")
        void shouldReturnYellowForMediumCPU() {
            var cpu = new SparkCPU(0.6, 0.65, 0.7, 0.6, 0.65, 0.7);
            assertEquals("<yellow>", cpu.getColor(60));
            assertEquals("<yellow>", cpu.getColor(70));
        }

        @Test
        @DisplayName("should return gold for CPU > 70 and <= 90")
        void shouldReturnGoldForHighCPU() {
            var cpu = new SparkCPU(0.8, 0.85, 0.9, 0.8, 0.85, 0.9);
            assertEquals("<gold>", cpu.getColor(80));
            assertEquals("<gold>", cpu.getColor(90));
        }

        @Test
        @DisplayName("should return red for CPU > 90")
        void shouldReturnRedForVeryHighCPU() {
            var cpu = new SparkCPU(0.95, 0.95, 0.95, 0.95, 0.95, 0.95);
            assertEquals("<red>", cpu.getColor(95));
        }

        @Test
        @DisplayName("should format percent correctly")
        void shouldFormatPercentCorrectly() {
            var cpu = new SparkCPU(0.5, 0.6, 0.7, 0.3, 0.4, 0.5);
            assertEquals(String.format("%.1f%%", 50.0), cpu.formatPercent(0.5));
            assertEquals(String.format("%.1f%%", 75.5), cpu.formatPercent(0.755));
        }

        @Test
        @DisplayName("should store all windows correctly")
        void shouldStoreAllWindows() {
            var cpu = new SparkCPU(0.1, 0.2, 0.3, 0.4, 0.5, 0.6);
            assertEquals(0.1, cpu.process10s());
            assertEquals(0.2, cpu.process1m());
            assertEquals(0.3, cpu.process15m());
            assertEquals(0.4, cpu.system10s());
            assertEquals(0.5, cpu.system1m());
            assertEquals(0.6, cpu.system15m());
        }
    }

    // ========== SparkGC ==========

    @Nested
    @DisplayName("SparkGC")
    class SparkGCTests {

        @Test
        @DisplayName("should format time in ms for < 1000ms")
        void shouldFormatTimeInMs() {
            var gc = new SparkGC(10, 500, 30.0);
            assertEquals("500ms", gc.formatTime());
        }

        @Test
        @DisplayName("should format time in seconds for >= 1000ms")
        void shouldFormatTimeInSeconds() {
            var gc = new SparkGC(100, 2500, 15.0);
            assertEquals(String.format("%.1fs", 2.5), gc.formatTime());
        }

        @Test
        @DisplayName("should format 0ms correctly")
        void shouldFormatZeroMs() {
            var gc = new SparkGC(0, 0, 0);
            assertEquals("0ms", gc.formatTime());
        }

        @Test
        @DisplayName("should format exactly 1000ms as seconds")
        void shouldFormat1000AsSeconds() {
            var gc = new SparkGC(50, 1000, 20.0);
            assertEquals(String.format("%.1fs", 1.0), gc.formatTime());
        }

        @Test
        @DisplayName("should store all fields correctly")
        void shouldStoreAllFields() {
            var gc = new SparkGC(42, 1234, 56.7);
            assertEquals(42, gc.totalCollections());
            assertEquals(1234, gc.totalTimeMs());
            assertEquals(56.7, gc.avgFrequency());
        }
    }
}
