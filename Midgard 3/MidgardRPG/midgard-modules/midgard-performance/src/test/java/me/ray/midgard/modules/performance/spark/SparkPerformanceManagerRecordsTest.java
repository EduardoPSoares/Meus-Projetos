package me.ray.midgard.modules.performance.spark;

import me.ray.midgard.modules.performance.spark.SparkPerformanceManager.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SparkPerformanceManager Records")
class SparkPerformanceManagerRecordsTest {

    // ========== ServerMetrics ==========

    @Nested
    @DisplayName("ServerMetrics")
    class ServerMetricsTests {

        @Test
        @DisplayName("should be available when timestamp > 0")
        void shouldBeAvailableWithPositiveTimestamp() {
            var metrics = new ServerMetrics(
                    TPSMetrics.unavailable(), MSPTMetrics.unavailable(),
                    CPUMetrics.unavailable(), GCMetrics.unavailable(),
                    MemoryMetrics.unavailable(), System.currentTimeMillis());
            assertTrue(metrics.available());
        }

        @Test
        @DisplayName("should not be available when timestamp is 0")
        void shouldNotBeAvailableWithZeroTimestamp() {
            var metrics = ServerMetrics.unavailable();
            assertFalse(metrics.available());
            assertEquals(0, metrics.timestamp());
        }

        @Test
        @DisplayName("unavailable() should create metrics with all sub-metrics unavailable")
        void unavailableShouldHaveAllSubMetricsUnavailable() {
            var metrics = ServerMetrics.unavailable();
            assertFalse(metrics.tps().available());
            assertFalse(metrics.mspt().available());
            assertFalse(metrics.cpu().available());
            assertFalse(metrics.gc().available());
            assertFalse(metrics.memory().available());
        }
    }

    // ========== TPSMetrics ==========

    @Nested
    @DisplayName("TPSMetrics")
    class TPSMetricsTests {

        @Test
        @DisplayName("should calculate average of all TPS windows")
        void shouldCalculateAverage() {
            var tps = new TPSMetrics(20.0, 19.0, 18.0, 17.0, 16.0, true);
            assertEquals(18.0, tps.average(), 0.01);
        }

        @Test
        @DisplayName("should return green for TPS >= 19.0")
        void shouldReturnGreenForHighTPS() {
            var tps = new TPSMetrics(20, 20, 20, 20, 20, true);
            assertEquals("<green>", tps.getColor(20.0));
            assertEquals("<green>", tps.getColor(19.0));
        }

        @Test
        @DisplayName("should return yellow for TPS >= 17.0 and < 19.0")
        void shouldReturnYellowForMediumTPS() {
            var tps = new TPSMetrics(18, 18, 18, 18, 18, true);
            assertEquals("<yellow>", tps.getColor(18.0));
            assertEquals("<yellow>", tps.getColor(17.0));
        }

        @Test
        @DisplayName("should return gold for TPS >= 15.0 and < 17.0")
        void shouldReturnGoldForLowTPS() {
            var tps = new TPSMetrics(16, 16, 16, 16, 16, true);
            assertEquals("<gold>", tps.getColor(16.0));
            assertEquals("<gold>", tps.getColor(15.0));
        }

        @Test
        @DisplayName("should return red for TPS < 15.0")
        void shouldReturnRedForVeryLowTPS() {
            var tps = new TPSMetrics(10, 10, 10, 10, 10, true);
            assertEquals("<red>", tps.getColor(14.9));
            assertEquals("<red>", tps.getColor(5.0));
        }

        @Test
        @DisplayName("unavailable() should be marked as not available")
        void unavailableShouldBeNotAvailable() {
            var tps = TPSMetrics.unavailable();
            assertFalse(tps.available());
            assertEquals(0, tps.last5s());
        }
    }

    // ========== MSPTMetrics / MSPTWindow ==========

    @Nested
    @DisplayName("MSPTMetrics")
    class MSPTMetricsTests {

        @Test
        @DisplayName("unavailable() should be not available with empty windows")
        void unavailableShouldBeNotAvailable() {
            var mspt = MSPTMetrics.unavailable();
            assertFalse(mspt.available());
        }

        @Test
        @DisplayName("should store windows correctly")
        void shouldStoreWindows() {
            var w10s = new MSPTWindow(5.0, 10.0, 15.0, 20.0);
            var w1m = new MSPTWindow(6.0, 11.0, 16.0, 21.0);
            var mspt = new MSPTMetrics(w10s, w1m, true);

            assertTrue(mspt.available());
            assertEquals(10.0, mspt.last10s().median());
            assertEquals(11.0, mspt.last1m().median());
        }
    }

    @Nested
    @DisplayName("MSPTWindow")
    class MSPTWindowTests {

        @Test
        @DisplayName("should return green for MSPT <= 30")
        void shouldReturnGreenForLowMSPT() {
            var w = new MSPTWindow(5, 10, 20, 30);
            assertEquals("<green>", w.getColor(30));
            assertEquals("<green>", w.getColor(10));
        }

        @Test
        @DisplayName("should return yellow for MSPT > 30 and <= 40")
        void shouldReturnYellowForMediumMSPT() {
            var w = new MSPTWindow(5, 35, 38, 40);
            assertEquals("<yellow>", w.getColor(35));
            assertEquals("<yellow>", w.getColor(40));
        }

        @Test
        @DisplayName("should return gold for MSPT > 40 and <= 50")
        void shouldReturnGoldForHighMSPT() {
            var w = new MSPTWindow(5, 45, 48, 50);
            assertEquals("<gold>", w.getColor(45));
            assertEquals("<gold>", w.getColor(50));
        }

        @Test
        @DisplayName("should return red for MSPT > 50")
        void shouldReturnRedForVeryHighMSPT() {
            var w = new MSPTWindow(5, 55, 80, 100);
            assertEquals("<red>", w.getColor(55));
            assertEquals("<red>", w.getColor(100));
        }

        @Test
        @DisplayName("empty() should have all zeros")
        void emptyShouldHaveAllZeros() {
            var w = MSPTWindow.empty();
            assertEquals(0, w.min());
            assertEquals(0, w.median());
            assertEquals(0, w.p95());
            assertEquals(0, w.max());
        }
    }

    // ========== CPUMetrics / CPUWindow ==========

    @Nested
    @DisplayName("CPUWindow")
    class CPUWindowTests {

        @Test
        @DisplayName("should return green for CPU <= 0.50")
        void shouldReturnGreenForLowCPU() {
            var w = new CPUWindow(0.3, 0.4, 0.5);
            assertEquals("<green>", w.getColor(0.50));
            assertEquals("<green>", w.getColor(0.30));
        }

        @Test
        @DisplayName("should return yellow for CPU > 0.50 and <= 0.70")
        void shouldReturnYellowForMediumCPU() {
            var w = new CPUWindow(0.6, 0.65, 0.7);
            assertEquals("<yellow>", w.getColor(0.60));
            assertEquals("<yellow>", w.getColor(0.70));
        }

        @Test
        @DisplayName("should return gold for CPU > 0.70 and <= 0.85")
        void shouldReturnGoldForHighCPU() {
            var w = new CPUWindow(0.8, 0.82, 0.85);
            assertEquals("<gold>", w.getColor(0.80));
            assertEquals("<gold>", w.getColor(0.85));
        }

        @Test
        @DisplayName("should return red for CPU > 0.85")
        void shouldReturnRedForVeryHighCPU() {
            var w = new CPUWindow(0.9, 0.95, 0.99);
            assertEquals("<red>", w.getColor(0.90));
            assertEquals("<red>", w.getColor(0.99));
        }

        @Test
        @DisplayName("should format percent correctly")
        void shouldFormatPercent() {
            var w = new CPUWindow(0.5, 0.6, 0.7);
            assertEquals(String.format("%.1f%%", 50.0), w.formatPercent(0.5));
            assertEquals(String.format("%.1f%%", 75.5), w.formatPercent(0.755));
        }

        @Test
        @DisplayName("empty() should have all zeros")
        void emptyShouldHaveAllZeros() {
            var w = CPUWindow.empty();
            assertEquals(0, w.seconds10());
            assertEquals(0, w.minutes1());
            assertEquals(0, w.minutes15());
        }
    }

    @Nested
    @DisplayName("CPUMetrics")
    class CPUMetricsTests {

        @Test
        @DisplayName("unavailable() should be not available")
        void unavailableShouldBeNotAvailable() {
            var cpu = CPUMetrics.unavailable();
            assertFalse(cpu.available());
        }

        @Test
        @DisplayName("should store process and system windows")
        void shouldStoreWindows() {
            var proc = new CPUWindow(0.3, 0.4, 0.5);
            var sys = new CPUWindow(0.5, 0.6, 0.7);
            var cpu = new CPUMetrics(proc, sys, true);
            assertTrue(cpu.available());
            assertEquals(0.3, cpu.process().seconds10());
            assertEquals(0.5, cpu.system().seconds10());
        }
    }

    // ========== GCMetrics ==========

    @Nested
    @DisplayName("GCMetrics")
    class GCMetricsTests {

        @Test
        @DisplayName("should format time in ms for < 1000ms")
        void shouldFormatTimeInMs() {
            var gc = new GCMetrics(10, 500, 50.0, 30000, Map.of(), true);
            assertEquals("500ms", gc.formatTime());
        }

        @Test
        @DisplayName("should format time in seconds for >= 1000ms")
        void shouldFormatTimeInSeconds() {
            var gc = new GCMetrics(100, 2500, 25.0, 15000, Map.of(), true);
            assertEquals(String.format("%.1fs", 2.5), gc.formatTime());
        }

        @Test
        @DisplayName("unavailable() should be not available")
        void unavailableShouldBeNotAvailable() {
            var gc = GCMetrics.unavailable();
            assertFalse(gc.available());
            assertEquals(0, gc.totalCollections());
        }
    }

    // ========== MemoryMetrics ==========

    @Nested
    @DisplayName("MemoryMetrics")
    class MemoryMetricsTests {

        @Test
        @DisplayName("should calculate usedPercent correctly")
        void shouldCalculateUsedPercent() {
            long max = 1024L * 1024 * 1024; // 1GB
            long used = max / 2; // 50%
            var mem = new MemoryMetrics(max, max, used, max - used, true);
            assertEquals(50.0, mem.usedPercent(), 0.1);
        }

        @Test
        @DisplayName("should return 0 when max is 0")
        void shouldReturnZeroWhenMaxIsZero() {
            var mem = new MemoryMetrics(0, 0, 0, 0, true);
            assertEquals(0, mem.usedPercent());
        }

        @Test
        @DisplayName("should calculate MB values correctly")
        void shouldCalculateMBValues() {
            long mb = 1024L * 1024;
            var mem = new MemoryMetrics(1000 * mb, 800 * mb, 500 * mb, 300 * mb, true);
            assertEquals(500, mem.usedMB());
            assertEquals(300, mem.freeMB());
            assertEquals(1000, mem.maxMB());
            assertEquals(800, mem.totalMB());
        }

        @Test
        @DisplayName("should return green for usage <= 60%")
        void shouldReturnGreenForLowUsage() {
            long max = 1000L * 1024 * 1024;
            long used = 500L * 1024 * 1024;
            var mem = new MemoryMetrics(max, max, used, max - used, true);
            assertEquals("<green>", mem.getColor());
        }

        @Test
        @DisplayName("should return yellow for usage > 60% and <= 75%")
        void shouldReturnYellowForMediumUsage() {
            long max = 1000L * 1024 * 1024;
            long used = 700L * 1024 * 1024;
            var mem = new MemoryMetrics(max, max, used, max - used, true);
            assertEquals("<yellow>", mem.getColor());
        }

        @Test
        @DisplayName("should return gold for usage > 75% and <= 85%")
        void shouldReturnGoldForHighUsage() {
            long max = 1000L * 1024 * 1024;
            long used = 800L * 1024 * 1024;
            var mem = new MemoryMetrics(max, max, used, max - used, true);
            assertEquals("<gold>", mem.getColor());
        }

        @Test
        @DisplayName("should return red for usage > 85%")
        void shouldReturnRedForCriticalUsage() {
            long max = 1000L * 1024 * 1024;
            long used = 900L * 1024 * 1024;
            var mem = new MemoryMetrics(max, max, used, max - used, true);
            assertEquals("<red>", mem.getColor());
        }

        @Test
        @DisplayName("unavailable() should be not available")
        void unavailableShouldBeNotAvailable() {
            var mem = MemoryMetrics.unavailable();
            assertFalse(mem.available());
        }
    }

    // ========== HealthIssue ==========

    @Nested
    @DisplayName("HealthIssue")
    class HealthIssueTests {

        @Test
        @DisplayName("healthy() should create EXCELLENT level")
        void healthyShouldBeExcellent() {
            var issue = HealthIssue.healthy("TPS", "20 TPS");
            assertEquals(HealthLevel.EXCELLENT, issue.level());
            assertEquals("TPS", issue.category());
            assertEquals("20 TPS", issue.message());
        }

        @Test
        @DisplayName("good() should create GOOD level")
        void goodShouldBeGood() {
            var issue = HealthIssue.good("CPU", "40% usage");
            assertEquals(HealthLevel.GOOD, issue.level());
        }

        @Test
        @DisplayName("warning() should create WARNING level")
        void warningShouldBeWarning() {
            var issue = HealthIssue.warning("Memory", "80% used");
            assertEquals(HealthLevel.WARNING, issue.level());
        }

        @Test
        @DisplayName("critical() should create CRITICAL level")
        void criticalShouldBeCritical() {
            var issue = HealthIssue.critical("TPS", "5 TPS");
            assertEquals(HealthLevel.CRITICAL, issue.level());
        }

        @Test
        @DisplayName("severe() should create SEVERE level")
        void severeShouldBeSevere() {
            var issue = HealthIssue.severe("Memory", "99% used");
            assertEquals(HealthLevel.SEVERE, issue.level());
        }

        @Test
        @DisplayName("unknown() should create UNKNOWN level")
        void unknownShouldBeUnknown() {
            var issue = HealthIssue.unknown("GC");
            assertEquals(HealthLevel.UNKNOWN, issue.level());
            assertEquals("GC", issue.category());
        }

        @Test
        @DisplayName("getIcon() should delegate to HealthLevel")
        void getIconShouldDelegateToLevel() {
            var issue = HealthIssue.healthy("TPS", "ok");
            assertEquals(HealthLevel.EXCELLENT.getIcon(), issue.getIcon());
        }

        @Test
        @DisplayName("getColor() should delegate to HealthLevel")
        void getColorShouldDelegateToLevel() {
            var issue = HealthIssue.critical("TPS", "bad");
            assertEquals(HealthLevel.CRITICAL.getColor(), issue.getColor());
        }
    }

    // ========== HealthLevel ==========

    @Nested
    @DisplayName("HealthLevel Enum")
    class HealthLevelTests {

        @Test
        @DisplayName("should have correct colors for each level")
        void shouldHaveCorrectColors() {
            assertEquals("<green>", HealthLevel.EXCELLENT.getColor());
            assertEquals("<yellow>", HealthLevel.GOOD.getColor());
            assertEquals("<gold>", HealthLevel.WARNING.getColor());
            assertEquals("<red>", HealthLevel.CRITICAL.getColor());
            assertEquals("<dark_red>", HealthLevel.SEVERE.getColor());
            assertEquals("<gray>", HealthLevel.UNKNOWN.getColor());
        }

        @Test
        @DisplayName("should have non-empty icons")
        void shouldHaveNonEmptyIcons() {
            for (HealthLevel level : HealthLevel.values()) {
                assertNotNull(level.getIcon());
                assertFalse(level.getIcon().isEmpty());
            }
        }

        @Test
        @DisplayName("should have non-empty labels")
        void shouldHaveNonEmptyLabels() {
            for (HealthLevel level : HealthLevel.values()) {
                assertNotNull(level.getLabel());
                assertFalse(level.getLabel().isEmpty());
            }
        }

        @Test
        @DisplayName("should have 6 levels")
        void shouldHaveSixLevels() {
            assertEquals(6, HealthLevel.values().length);
        }
    }

    // ========== HealthDiagnosis ==========

    @Nested
    @DisplayName("HealthDiagnosis")
    class HealthDiagnosisTests {

        @Test
        @DisplayName("should store all health issues and overall level")
        void shouldStoreAllIssues() {
            var diag = new HealthDiagnosis(
                    HealthIssue.healthy("TPS", "ok"),
                    HealthIssue.good("MSPT", "ok"),
                    HealthIssue.warning("CPU", "high"),
                    HealthIssue.critical("GC", "frequent"),
                    HealthIssue.severe("Memory", "full"),
                    HealthLevel.CRITICAL
            );

            assertEquals(HealthLevel.EXCELLENT, diag.tps().level());
            assertEquals(HealthLevel.GOOD, diag.mspt().level());
            assertEquals(HealthLevel.WARNING, diag.cpu().level());
            assertEquals(HealthLevel.CRITICAL, diag.gc().level());
            assertEquals(HealthLevel.SEVERE, diag.memory().level());
            assertEquals(HealthLevel.CRITICAL, diag.overallHealth());
        }
    }
}
