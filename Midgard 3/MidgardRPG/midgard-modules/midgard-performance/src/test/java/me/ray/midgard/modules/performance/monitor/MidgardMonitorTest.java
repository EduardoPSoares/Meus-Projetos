package me.ray.midgard.modules.performance.monitor;

import me.ray.midgard.modules.performance.monitor.MidgardMonitor.ModuleStats;
import me.ray.midgard.modules.performance.monitor.MidgardMonitor.OperationStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MidgardMonitor")
class MidgardMonitorTest {

    @BeforeEach
    void setUp() {
        MidgardMonitor.clearTracking();
    }

    @Nested
    @DisplayName("Tracking Methods")
    class TrackingTests {

        @Test
        @DisplayName("should track events correctly")
        void shouldTrackEvents() {
            MidgardMonitor.trackEvent("PlayerDamageEvent");
            MidgardMonitor.trackEvent("PlayerDamageEvent");
            MidgardMonitor.trackEvent("PlayerJoinEvent");

            Map<String, Long> counts = MidgardMonitor.getEventCounts();
            assertEquals(2L, counts.get("PlayerDamageEvent"));
            assertEquals(1L, counts.get("PlayerJoinEvent"));
        }

        @Test
        @DisplayName("should track GUI opens correctly")
        void shouldTrackGuiOpens() {
            MidgardMonitor.trackGuiOpen("InventoryGui");
            MidgardMonitor.trackGuiOpen("InventoryGui");
            MidgardMonitor.trackGuiOpen("ShopGui");

            Map<String, Long> opens = MidgardMonitor.getGuiOpens();
            assertEquals(2L, opens.get("InventoryGui"));
            assertEquals(1L, opens.get("ShopGui"));
        }

        @Test
        @DisplayName("should track commands correctly")
        void shouldTrackCommands() {
            MidgardMonitor.trackCommand("/rpg");
            MidgardMonitor.trackCommand("/rpg");
            MidgardMonitor.trackCommand("/perf");

            Map<String, Long> cmds = MidgardMonitor.getCommandExecutions();
            assertEquals(2L, cmds.get("/rpg"));
            assertEquals(1L, cmds.get("/perf"));
        }

        @Test
        @DisplayName("should track database queries correctly")
        void shouldTrackDatabaseQueries() {
            MidgardMonitor.trackDatabaseQuery("SELECT");
            MidgardMonitor.trackDatabaseQuery("SELECT");
            MidgardMonitor.trackDatabaseQuery("INSERT");

            Map<String, Long> queries = MidgardMonitor.getDatabaseQueries();
            assertEquals(2L, queries.get("SELECT"));
            assertEquals(1L, queries.get("INSERT"));
        }

        @Test
        @DisplayName("should return empty maps initially")
        void shouldReturnEmptyMapsInitially() {
            assertTrue(MidgardMonitor.getEventCounts().isEmpty());
            assertTrue(MidgardMonitor.getGuiOpens().isEmpty());
            assertTrue(MidgardMonitor.getCommandExecutions().isEmpty());
            assertTrue(MidgardMonitor.getDatabaseQueries().isEmpty());
        }
    }

    @Nested
    @DisplayName("clearTracking()")
    class ClearTrackingTests {

        @Test
        @DisplayName("should clear all tracking data")
        void shouldClearAllTracking() {
            MidgardMonitor.trackEvent("evt");
            MidgardMonitor.trackGuiOpen("gui");
            MidgardMonitor.trackCommand("cmd");
            MidgardMonitor.trackDatabaseQuery("query");

            MidgardMonitor.clearTracking();

            assertTrue(MidgardMonitor.getEventCounts().isEmpty());
            assertTrue(MidgardMonitor.getGuiOpens().isEmpty());
            assertTrue(MidgardMonitor.getCommandExecutions().isEmpty());
            assertTrue(MidgardMonitor.getDatabaseQueries().isEmpty());
        }
    }

    @Nested
    @DisplayName("Returned Maps are Copies")
    class DefensiveCopyTests {

        @Test
        @DisplayName("should return a defensive copy of event counts")
        void shouldReturnDefensiveCopyOfEventCounts() {
            MidgardMonitor.trackEvent("evt");

            Map<String, Long> copy = MidgardMonitor.getEventCounts();
            copy.clear(); // Modify the returned map

            // Original should be unchanged
            assertFalse(MidgardMonitor.getEventCounts().isEmpty());
        }

        @Test
        @DisplayName("should return a defensive copy of gui opens")
        void shouldReturnDefensiveCopyOfGuiOpens() {
            MidgardMonitor.trackGuiOpen("gui");

            Map<String, Long> copy = MidgardMonitor.getGuiOpens();
            copy.clear();

            assertFalse(MidgardMonitor.getGuiOpens().isEmpty());
        }
    }

    @Nested
    @DisplayName("ModuleStats Record")
    class ModuleStatsTests {

        @Test
        @DisplayName("should return green check for enabled module")
        void shouldReturnGreenCheckForEnabled() {
            var stats = new ModuleStats(true, 50, 200, 10);
            assertEquals("<green>✔", stats.getStatus());
        }

        @Test
        @DisplayName("should return red X for disabled module")
        void shouldReturnRedXForDisabled() {
            var stats = new ModuleStats(false, 0, 0, 0);
            assertEquals("<red>✘", stats.getStatus());
        }

        @Test
        @DisplayName("should return green for fast enable time (< 100ms)")
        void shouldReturnGreenForFastEnable() {
            var stats = new ModuleStats(true, 50, 0, 0);
            assertEquals("<green>", stats.getTimeColor());
        }

        @Test
        @DisplayName("should return yellow for moderate enable time (100-499ms)")
        void shouldReturnYellowForModerateEnable() {
            var stats = new ModuleStats(true, 250, 0, 0);
            assertEquals("<yellow>", stats.getTimeColor());
        }

        @Test
        @DisplayName("should return red for slow enable time (>= 500ms)")
        void shouldReturnRedForSlowEnable() {
            var stats = new ModuleStats(true, 600, 0, 0);
            assertEquals("<red>", stats.getTimeColor());
        }

        @Test
        @DisplayName("should store all fields correctly")
        void shouldStoreFieldsCorrectly() {
            var stats = new ModuleStats(true, 100, 500, 25);
            assertTrue(stats.enabled());
            assertEquals(100, stats.enableTime());
            assertEquals(500, stats.totalProfiledTime());
            assertEquals(25, stats.totalOperations());
        }
    }

    @Nested
    @DisplayName("OperationStats Record")
    class OperationStatsTests {

        @Test
        @DisplayName("should return green for maxTime < 10ms")
        void shouldReturnGreenForFastOp() {
            var stats = new OperationStats(5, 3, 100);
            assertEquals("<green>", stats.getTimeColor());
        }

        @Test
        @DisplayName("should return yellow for maxTime 10-49ms")
        void shouldReturnYellowForModerateOp() {
            var stats = new OperationStats(25, 10, 50);
            assertEquals("<yellow>", stats.getTimeColor());
        }

        @Test
        @DisplayName("should return gold for maxTime 50-99ms")
        void shouldReturnGoldForSlowOp() {
            var stats = new OperationStats(75, 50, 20);
            assertEquals("<gold>", stats.getTimeColor());
        }

        @Test
        @DisplayName("should return red for maxTime >= 100ms")
        void shouldReturnRedForVerySlowOp() {
            var stats = new OperationStats(150, 100, 5);
            assertEquals("<red>", stats.getTimeColor());
        }

        @Test
        @DisplayName("should store all fields correctly")
        void shouldStoreFieldsCorrectly() {
            var stats = new OperationStats(200, 100, 42);
            assertEquals(200, stats.maxTime());
            assertEquals(100, stats.lastTime());
            assertEquals(42, stats.count());
        }
    }
}
