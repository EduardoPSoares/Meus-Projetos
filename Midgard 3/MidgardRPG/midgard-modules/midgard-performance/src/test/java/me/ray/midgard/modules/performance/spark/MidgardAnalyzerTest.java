package me.ray.midgard.modules.performance.spark;

import me.ray.midgard.modules.performance.spark.MidgardAnalyzer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MidgardAnalyzer")
class MidgardAnalyzerTest {

    @BeforeEach
    void setUp() {
        MidgardAnalyzer.init();
    }

    @Nested
    @DisplayName("Singleton")
    class SingletonTests {

        @Test
        @DisplayName("init() should create an instance")
        void initShouldCreateInstance() {
            assertNotNull(MidgardAnalyzer.getInstance());
        }

        @Test
        @DisplayName("getInstance() should return same instance")
        void getInstanceShouldReturnSameInstance() {
            var a = MidgardAnalyzer.getInstance();
            var b = MidgardAnalyzer.getInstance();
            assertSame(a, b);
        }
    }

    @Nested
    @DisplayName("Tracking Methods")
    class TrackingTests {

        @Test
        @DisplayName("trackEvent() should not throw")
        void trackEventShouldNotThrow() {
            var analyzer = MidgardAnalyzer.getInstance();
            assertDoesNotThrow(() -> analyzer.trackEvent("TestEvent", 5_000_000L));
        }

        @Test
        @DisplayName("trackCommand() should not throw")
        void trackCommandShouldNotThrow() {
            var analyzer = MidgardAnalyzer.getInstance();
            assertDoesNotThrow(() -> analyzer.trackCommand("testcmd", 10_000_000L));
        }

        @Test
        @DisplayName("trackGUI() should not throw")
        void trackGUIShouldNotThrow() {
            var analyzer = MidgardAnalyzer.getInstance();
            assertDoesNotThrow(() -> analyzer.trackGUI("TestGui", 3_000_000L));
        }

        @Test
        @DisplayName("trackOperation() should not throw")
        void trackOperationShouldNotThrow() {
            var analyzer = MidgardAnalyzer.getInstance();
            assertDoesNotThrow(() -> analyzer.trackOperation("some_op", 1_000_000L));
        }

        @Test
        @DisplayName("clearTracking() should not throw after tracking")
        void clearTrackingShouldNotThrow() {
            var analyzer = MidgardAnalyzer.getInstance();
            analyzer.trackEvent("evt", 1_000_000L);
            analyzer.trackCommand("cmd", 2_000_000L);
            assertDoesNotThrow(analyzer::clearTracking);
        }
    }

    // ========== Severity Enum ==========

    @Nested
    @DisplayName("Severity Enum")
    class SeverityTests {

        @Test
        @DisplayName("should have 6 severity levels")
        void shouldHaveSixLevels() {
            assertEquals(6, Severity.values().length);
        }

        @Test
        @DisplayName("should have correct colors")
        void shouldHaveCorrectColors() {
            assertEquals("<green>", Severity.EXCELLENT.getColor());
            assertEquals("<yellow>", Severity.GOOD.getColor());
            assertEquals("<gold>", Severity.MODERATE.getColor());
            assertEquals("<gold>", Severity.WARNING.getColor());
            assertEquals("<red>", Severity.CRITICAL.getColor());
            assertEquals("<dark_red>", Severity.SEVERE.getColor());
        }

        @Test
        @DisplayName("should have non-empty icons")
        void shouldHaveNonEmptyIcons() {
            for (Severity s : Severity.values()) {
                assertNotNull(s.getIcon());
                assertFalse(s.getIcon().isEmpty());
            }
        }
    }

    // ========== Records ==========

    @Nested
    @DisplayName("MemoryAnalysis Record")
    class MemoryAnalysisTests {

        @Test
        @DisplayName("should calculate heapUsedPercent correctly")
        void shouldCalculateHeapUsedPercent() {
            long max = 1024L * 1024 * 1024; // 1GB
            long used = max / 2; // 50%
            var mem = new MemoryAnalysis(used, max, 0, 0, 0, 5, 10, 3);
            assertEquals(50.0, mem.heapUsedPercent(), 0.1);
        }

        @Test
        @DisplayName("should return 0% when heapMax is 0")
        void shouldReturnZeroWhenMaxIsZero() {
            var mem = new MemoryAnalysis(0, 0, 0, 0, 0, 0, 0, 0);
            assertEquals(0, mem.heapUsedPercent());
        }

        @Test
        @DisplayName("should calculate MB values correctly")
        void shouldCalculateMBValues() {
            long mb = 1024L * 1024;
            var mem = new MemoryAnalysis(500 * mb, 1000 * mb, 0, 0, 0, 0, 0, 0);
            assertEquals(500, mem.heapUsedMB());
            assertEquals(1000, mem.heapMaxMB());
        }

        @Test
        @DisplayName("should store all fields correctly")
        void shouldStoreAllFields() {
            var mem = new MemoryAnalysis(100, 200, 50, 20, 10, 5, 8, 3);
            assertEquals(100, mem.heapUsed());
            assertEquals(200, mem.heapMax());
            assertEquals(50, mem.estimatedModuleMemory());
            assertEquals(20, mem.estimatedCommandMemory());
            assertEquals(10, mem.estimatedCacheMemory());
            assertEquals(5, mem.moduleCount());
            assertEquals(8, mem.commandCount());
            assertEquals(3, mem.trackerCount());
        }
    }

    @Nested
    @DisplayName("AnalysisSnapshot Record")
    class AnalysisSnapshotTests {

        @Test
        @DisplayName("should store all analysis data")
        void shouldStoreAllData() {
            var snapshot = new AnalysisSnapshot(
                    List.of(),
                    new EventAnalysis(0, 0, List.of(), List.of()),
                    new CommandAnalysis(0, 0, List.of(), List.of()),
                    new ProfilerAnalysis(0, 0, 0, List.of(), List.of()),
                    new MemoryAnalysis(0, 0, 0, 0, 0, 0, 0, 0),
                    12345L
            );
            assertNotNull(snapshot.modules());
            assertNotNull(snapshot.events());
            assertNotNull(snapshot.commands());
            assertNotNull(snapshot.profiler());
            assertNotNull(snapshot.memory());
            assertEquals(12345L, snapshot.timestamp());
        }
    }

    @Nested
    @DisplayName("ModuleAnalysis Record")
    class ModuleAnalysisTests {

        @Test
        @DisplayName("should store all module analysis data")
        void shouldStoreAllData() {
            var mod = new ModuleAnalysis("Combat", true, 200, 500, 10, 5, List.of(), Severity.GOOD);
            assertEquals("Combat", mod.name());
            assertTrue(mod.enabled());
            assertEquals(200, mod.enableTime());
            assertEquals(500, mod.totalTime());
            assertEquals(10, mod.totalOperations());
            assertEquals(5, mod.listenerCount());
            assertEquals(Severity.GOOD, mod.health());
        }
    }

    @Nested
    @DisplayName("OperationAnalysis Record")
    class OperationAnalysisTests {

        @Test
        @DisplayName("should store operation analysis data")
        void shouldStoreData() {
            var op = new OperationAnalysis("damage_calc", 50, 100, Severity.WARNING);
            assertEquals("damage_calc", op.name());
            assertEquals(50, op.maxTime());
            assertEquals(100, op.count());
            assertEquals(Severity.WARNING, op.severity());
        }
    }

    @Nested
    @DisplayName("EventAnalysis Record")
    class EventAnalysisTests {

        @Test
        @DisplayName("should store event analysis data")
        void shouldStoreData() {
            var listeners = List.of(
                    new RegisteredListenerInfo("PlayerJoinEvent", "MyListener", "NORMAL", 5, 10, Severity.EXCELLENT)
            );
            var evt = new EventAnalysis(1, 1, listeners, listeners);
            assertEquals(1, evt.totalListeners());
            assertEquals(1, evt.uniqueEvents());
        }
    }

    @Nested
    @DisplayName("RegisteredListenerInfo Record")
    class RegisteredListenerInfoTests {

        @Test
        @DisplayName("should store listener info correctly")
        void shouldStoreData() {
            var info = new RegisteredListenerInfo("PlayerDamageEvent", "CombatListener", "HIGH", 25, 50, Severity.GOOD);
            assertEquals("PlayerDamageEvent", info.eventName());
            assertEquals("CombatListener", info.listenerClass());
            assertEquals("HIGH", info.priority());
            assertEquals(25, info.maxTime());
            assertEquals(50, info.executions());
            assertEquals(Severity.GOOD, info.severity());
        }
    }

    @Nested
    @DisplayName("CommandAnalysis Record")
    class CommandAnalysisTests {

        @Test
        @DisplayName("should store command analysis data")
        void shouldStoreData() {
            var cmds = List.of(new CommandInfo("rpg", 30, 100, 30, Severity.MODERATE));
            var analysis = new CommandAnalysis(1, 100, cmds, cmds);
            assertEquals(1, analysis.totalCommands());
            assertEquals(100, analysis.totalExecutions());
        }
    }

    @Nested
    @DisplayName("CommandInfo Record")
    class CommandInfoTests {

        @Test
        @DisplayName("should store command info correctly")
        void shouldStoreData() {
            var cmd = new CommandInfo("perf", 45, 200, 20, Severity.MODERATE);
            assertEquals("perf", cmd.name());
            assertEquals(45, cmd.maxTime());
            assertEquals(200, cmd.executions());
            assertEquals(20, cmd.avgTime());
            assertEquals(Severity.MODERATE, cmd.severity());
        }
    }

    @Nested
    @DisplayName("ProfilerAnalysis Record")
    class ProfilerAnalysisTests {

        @Test
        @DisplayName("should store profiler analysis data")
        void shouldStoreData() {
            var analysis = new ProfilerAnalysis(10, 500, 2000, List.of(), List.of());
            assertEquals(10, analysis.trackedOperations());
            assertEquals(500, analysis.totalExecutions());
            assertEquals(2000, analysis.totalTime());
        }
    }

    @Nested
    @DisplayName("ProfiledOperation Record")
    class ProfiledOperationTests {

        @Test
        @DisplayName("should store profiled operation data")
        void shouldStoreData() {
            var op = new ProfiledOperation("combat:damage", 100, 50, 25, Severity.CRITICAL);
            assertEquals("combat:damage", op.name());
            assertEquals(100, op.maxTime());
            assertEquals(50, op.lastTime());
            assertEquals(25, op.count());
            assertEquals(Severity.CRITICAL, op.severity());
        }
    }
}
