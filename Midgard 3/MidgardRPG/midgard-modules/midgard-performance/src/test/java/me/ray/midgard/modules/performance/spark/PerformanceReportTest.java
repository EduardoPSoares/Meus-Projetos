package me.ray.midgard.modules.performance.spark;

import me.ray.midgard.modules.performance.spark.PerformanceReport.*;
import me.ray.midgard.modules.performance.spark.SparkPerformanceManager.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PerformanceReport")
class PerformanceReportTest {

    // ========== FullReport ==========

    @Nested
    @DisplayName("FullReport")
    class FullReportTests {

        @Test
        @DisplayName("should return green for score >= 90")
        void shouldReturnGreenForHighScore() {
            var report = createReport(95);
            assertEquals("<green>", report.getScoreColor());
        }

        @Test
        @DisplayName("should return yellow for score >= 75 and < 90")
        void shouldReturnYellowForMediumScore() {
            assertEquals("<yellow>", createReport(80).getScoreColor());
            assertEquals("<yellow>", createReport(75).getScoreColor());
        }

        @Test
        @DisplayName("should return gold for score >= 50 and < 75")
        void shouldReturnGoldForLowScore() {
            assertEquals("<gold>", createReport(60).getScoreColor());
            assertEquals("<gold>", createReport(50).getScoreColor());
        }

        @Test
        @DisplayName("should return red for score < 50")
        void shouldReturnRedForVeryLowScore() {
            assertEquals("<red>", createReport(49).getScoreColor());
            assertEquals("<red>", createReport(0).getScoreColor());
        }

        @Test
        @DisplayName("should return S+ grade for score >= 95")
        void shouldReturnSPlusForScore95() {
            assertEquals("S+", createReport(95).getScoreGrade());
            assertEquals("S+", createReport(100).getScoreGrade());
        }

        @Test
        @DisplayName("should return S grade for score >= 90 and < 95")
        void shouldReturnSForScore90() {
            assertEquals("S", createReport(90).getScoreGrade());
            assertEquals("S", createReport(94).getScoreGrade());
        }

        @Test
        @DisplayName("should return A+ grade for score >= 85 and < 90")
        void shouldReturnAPlusForScore85() {
            assertEquals("A+", createReport(85).getScoreGrade());
            assertEquals("A+", createReport(89).getScoreGrade());
        }

        @Test
        @DisplayName("should return A grade for score >= 80 and < 85")
        void shouldReturnAForScore80() {
            assertEquals("A", createReport(80).getScoreGrade());
            assertEquals("A", createReport(84).getScoreGrade());
        }

        @Test
        @DisplayName("should return B+ grade for score >= 75")
        void shouldReturnBPlusForScore75() {
            assertEquals("B+", createReport(75).getScoreGrade());
            assertEquals("B+", createReport(79).getScoreGrade());
        }

        @Test
        @DisplayName("should return B grade for score >= 70")
        void shouldReturnBForScore70() {
            assertEquals("B", createReport(70).getScoreGrade());
            assertEquals("B", createReport(74).getScoreGrade());
        }

        @Test
        @DisplayName("should return C grade for score >= 60")
        void shouldReturnCForScore60() {
            assertEquals("C", createReport(60).getScoreGrade());
            assertEquals("C", createReport(69).getScoreGrade());
        }

        @Test
        @DisplayName("should return D grade for score >= 50")
        void shouldReturnDForScore50() {
            assertEquals("D", createReport(50).getScoreGrade());
            assertEquals("D", createReport(59).getScoreGrade());
        }

        @Test
        @DisplayName("should return F grade for score < 50")
        void shouldReturnFForLowScore() {
            assertEquals("F", createReport(49).getScoreGrade());
            assertEquals("F", createReport(0).getScoreGrade());
        }

        private FullReport createReport(int score) {
            return new FullReport("2024-01-01 12:00:00", true,
                    ServerMetrics.unavailable(), null, null,
                    List.of(), List.of(), score);
        }
    }

    // ========== QuickReport ==========

    @Nested
    @DisplayName("QuickReport")
    class QuickReportTests {

        @Test
        @DisplayName("unavailable() should have negative TPS")
        void unavailableShouldHaveNegativeTPS() {
            var report = QuickReport.unavailable();
            assertTrue(report.tps() < 0);
        }

        @Test
        @DisplayName("unavailable() should not be available")
        void unavailableShouldNotBeAvailable() {
            var report = QuickReport.unavailable();
            assertFalse(report.isAvailable());
        }

        @Test
        @DisplayName("should be available when TPS >= 0")
        void shouldBeAvailableWhenTPSNonNegative() {
            var report = new QuickReport(20.0, 15.0, 50.0, 30.0,
                    HealthLevel.EXCELLENT, 0, "2024-01-01 12:00:00");
            assertTrue(report.isAvailable());
        }

        @Test
        @DisplayName("should store all fields correctly")
        void shouldStoreFieldsCorrectly() {
            var report = new QuickReport(19.5, 12.0, 65.0, 40.0,
                    HealthLevel.GOOD, 1, "2024-01-01 12:00:00");
            assertEquals(19.5, report.tps());
            assertEquals(12.0, report.mspt());
            assertEquals(65.0, report.memoryPercent());
            assertEquals(40.0, report.cpuPercent());
            assertEquals(HealthLevel.GOOD, report.health());
            assertEquals(1, report.criticalIssues());
        }

        @Test
        @DisplayName("unavailable() should have UNKNOWN health level")
        void unavailableShouldHaveUnknownHealth() {
            assertEquals(HealthLevel.UNKNOWN, QuickReport.unavailable().health());
        }
    }

    // ========== Issue Record ==========

    @Nested
    @DisplayName("Issue Record")
    class IssueTests {

        @Test
        @DisplayName("should store all fields correctly")
        void shouldStoreFieldsCorrectly() {
            var issue = new Issue(IssueCategory.TPS, HealthLevel.CRITICAL,
                    "TPS Baixo", "TPS em 5.0", "Verifique entidades");
            assertEquals(IssueCategory.TPS, issue.category());
            assertEquals(HealthLevel.CRITICAL, issue.level());
            assertEquals("TPS Baixo", issue.title());
            assertEquals("TPS em 5.0", issue.description());
            assertEquals("Verifique entidades", issue.suggestion());
        }
    }

    // ========== Recommendation Record ==========

    @Nested
    @DisplayName("Recommendation Record")
    class RecommendationTests {

        @Test
        @DisplayName("should store all fields correctly")
        void shouldStoreFieldsCorrectly() {
            var rec = new Recommendation(RecommendationPriority.HIGH,
                    "Otimizar TPS", List.of("Passo 1", "Passo 2"));
            assertEquals(RecommendationPriority.HIGH, rec.priority());
            assertEquals("Otimizar TPS", rec.title());
            assertEquals(2, rec.steps().size());
        }
    }

    // ========== IssueCategory Enum ==========

    @Nested
    @DisplayName("IssueCategory Enum")
    class IssueCategoryTests {

        @Test
        @DisplayName("should have 9 categories")
        void shouldHaveNineCategories() {
            assertEquals(9, IssueCategory.values().length);
        }

        @Test
        @DisplayName("should have non-empty icons")
        void shouldHaveNonEmptyIcons() {
            for (IssueCategory cat : IssueCategory.values()) {
                assertNotNull(cat.getIcon());
                assertFalse(cat.getIcon().isEmpty());
            }
        }

        @Test
        @DisplayName("should have non-empty labels")
        void shouldHaveNonEmptyLabels() {
            for (IssueCategory cat : IssueCategory.values()) {
                assertNotNull(cat.getLabel());
                assertFalse(cat.getLabel().isEmpty());
            }
        }

        @Test
        @DisplayName("should have expected category names")
        void shouldHaveExpectedNames() {
            assertNotNull(IssueCategory.valueOf("TPS"));
            assertNotNull(IssueCategory.valueOf("MSPT"));
            assertNotNull(IssueCategory.valueOf("MEMORY"));
            assertNotNull(IssueCategory.valueOf("CPU"));
            assertNotNull(IssueCategory.valueOf("GC"));
            assertNotNull(IssueCategory.valueOf("MODULE"));
            assertNotNull(IssueCategory.valueOf("EVENT"));
            assertNotNull(IssueCategory.valueOf("COMMAND"));
            assertNotNull(IssueCategory.valueOf("OPERATION"));
        }
    }

    // ========== RecommendationPriority Enum ==========

    @Nested
    @DisplayName("RecommendationPriority Enum")
    class RecommendationPriorityTests {

        @Test
        @DisplayName("should have 3 priorities")
        void shouldHaveThreePriorities() {
            assertEquals(3, RecommendationPriority.values().length);
        }

        @Test
        @DisplayName("should have correct colors")
        void shouldHaveCorrectColors() {
            assertEquals("<red>", RecommendationPriority.HIGH.getColor());
            assertEquals("<yellow>", RecommendationPriority.MEDIUM.getColor());
            assertEquals("<gray>", RecommendationPriority.LOW.getColor());
        }

        @Test
        @DisplayName("should have non-empty labels")
        void shouldHaveNonEmptyLabels() {
            for (RecommendationPriority p : RecommendationPriority.values()) {
                assertNotNull(p.getLabel());
                assertFalse(p.getLabel().isEmpty());
            }
        }
    }
}
