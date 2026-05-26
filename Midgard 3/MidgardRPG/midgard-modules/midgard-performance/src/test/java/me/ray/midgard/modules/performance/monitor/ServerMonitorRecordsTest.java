package me.ray.midgard.modules.performance.monitor;

import me.ray.midgard.modules.performance.monitor.ServerMonitor.*;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServerMonitor Records")
class ServerMonitorRecordsTest {

    @Nested
    @DisplayName("MemoryInfo")
    class MemoryInfoTests {

        @Test
        @DisplayName("should calculate usedPercent correctly")
        void shouldCalculateUsedPercent() {
            // 500MB used out of 1000MB max
            long max = 1000L * 1024 * 1024;
            long used = 500L * 1024 * 1024;
            long free = 200L * 1024 * 1024;
            long total = used + free;

            var info = new MemoryInfo(max, total, used, free);
            assertEquals(50.0, info.usedPercent(), 0.1);
        }

        @Test
        @DisplayName("should calculate usedPercent at 100%")
        void shouldCalculateFullUsage() {
            long max = 1000L * 1024 * 1024;
            var info = new MemoryInfo(max, max, max, 0);
            assertEquals(100.0, info.usedPercent(), 0.1);
        }

        @Test
        @DisplayName("should format usedMB correctly")
        void shouldFormatUsedMB() {
            long bytes = 512L * 1024 * 1024; // 512 MB
            var info = new MemoryInfo(bytes * 2, bytes * 2, bytes, bytes);
            assertEquals(String.format("%.1f", 512.0), info.usedMB());
        }

        @Test
        @DisplayName("should format maxMB correctly")
        void shouldFormatMaxMB() {
            long max = 2048L * 1024 * 1024; // 2048 MB
            var info = new MemoryInfo(max, max, max / 2, max / 2);
            assertEquals(String.format("%.1f", 2048.0), info.maxMB());
        }

        @Test
        @DisplayName("should format freeMB correctly")
        void shouldFormatFreeMB() {
            long free = 256L * 1024 * 1024; // 256 MB
            var info = new MemoryInfo(free * 4, free * 4, free * 3, free);
            assertEquals(String.format("%.1f", 256.0), info.freeMB());
        }

        @Test
        @DisplayName("should return green for usage < 60%")
        void shouldReturnGreenForLowUsage() {
            long max = 1000L * 1024 * 1024;
            long used = 500L * 1024 * 1024; // 50%
            var info = new MemoryInfo(max, max, used, max - used);
            assertEquals("<green>", info.getColor());
        }

        @Test
        @DisplayName("should return yellow for usage >= 60% and < 80%")
        void shouldReturnYellowForMediumUsage() {
            long max = 1000L * 1024 * 1024;
            long used = 700L * 1024 * 1024; // 70%
            var info = new MemoryInfo(max, max, used, max - used);
            assertEquals("<yellow>", info.getColor());
        }

        @Test
        @DisplayName("should return gold for usage >= 80% and < 90%")
        void shouldReturnGoldForHighUsage() {
            long max = 1000L * 1024 * 1024;
            long used = 850L * 1024 * 1024; // 85%
            var info = new MemoryInfo(max, max, used, max - used);
            assertEquals("<gold>", info.getColor());
        }

        @Test
        @DisplayName("should return red for usage >= 90%")
        void shouldReturnRedForCriticalUsage() {
            long max = 1000L * 1024 * 1024;
            long used = 950L * 1024 * 1024; // 95%
            var info = new MemoryInfo(max, max, used, max - used);
            assertEquals("<red>", info.getColor());
        }
    }

    @Nested
    @DisplayName("EntityStats")
    class EntityStatsTests {

        @Test
        @DisplayName("should return top entities sorted by count")
        void shouldReturnTopEntitiesSorted() {
            Map<EntityType, Integer> byType = new EnumMap<>(EntityType.class);
            byType.put(EntityType.ZOMBIE, 50);
            byType.put(EntityType.CREEPER, 30);
            byType.put(EntityType.SKELETON, 100);
            byType.put(EntityType.PIG, 10);

            var stats = new EntityStats(190, 180, 10, 0, 0, byType);
            String top = stats.getTopEntities(2);

            assertTrue(top.startsWith("skeleton:100"), "Should start with skeleton, was: " + top);
            assertTrue(top.contains("zombie:50"), "Should contain zombie");
            assertFalse(top.contains("pig:10"), "Should not contain pig (limit 2)");
        }

        @Test
        @DisplayName("should return 'none' for empty byType map")
        void shouldReturnNoneForEmptyMap() {
            var stats = new EntityStats(0, 0, 0, 0, 0, Map.of());
            assertEquals("none", stats.getTopEntities(5));
        }

        @Test
        @DisplayName("should limit results by count parameter")
        void shouldLimitByCount() {
            Map<EntityType, Integer> byType = new EnumMap<>(EntityType.class);
            byType.put(EntityType.ZOMBIE, 50);
            byType.put(EntityType.CREEPER, 30);
            byType.put(EntityType.SKELETON, 100);

            var stats = new EntityStats(180, 180, 0, 0, 0, byType);
            String top1 = stats.getTopEntities(1);

            assertEquals("skeleton:100", top1);
        }

        @Test
        @DisplayName("should store all counts correctly")
        void shouldStoreCountsCorrectly() {
            var stats = new EntityStats(100, 40, 30, 20, 10, Map.of());
            assertEquals(100, stats.total());
            assertEquals(40, stats.hostile());
            assertEquals(30, stats.passive());
            assertEquals(20, stats.players());
            assertEquals(10, stats.other());
        }
    }

    @Nested
    @DisplayName("ChunkStats")
    class ChunkStatsTests {

        @Test
        @DisplayName("should store chunk data correctly")
        void shouldStoreChunkData() {
            var stats = new ChunkStats(256, 10, 200);
            assertEquals(256, stats.loaded());
            assertEquals(10, stats.forceLoaded());
            assertEquals(200, stats.ticketing());
        }
    }

    @Nested
    @DisplayName("PlayerStats")
    class PlayerStatsTests {

        @Test
        @DisplayName("should store player data correctly")
        void shouldStorePlayerData() {
            var stats = new PlayerStats(20, 100, 45, 10, 200);
            assertEquals(20, stats.online());
            assertEquals(100, stats.max());
            assertEquals(45, stats.avgPing());
            assertEquals(10, stats.minPing());
            assertEquals(200, stats.maxPing());
        }

        @Test
        @DisplayName("should return green for ping < 50")
        void shouldReturnGreenForLowPing() {
            var stats = new PlayerStats(1, 100, 30, 30, 30);
            assertEquals("<green>", stats.getPingColor(0));
            assertEquals("<green>", stats.getPingColor(49));
        }

        @Test
        @DisplayName("should return yellow for ping >= 50 and < 100")
        void shouldReturnYellowForMediumPing() {
            var stats = new PlayerStats(1, 100, 75, 75, 75);
            assertEquals("<yellow>", stats.getPingColor(50));
            assertEquals("<yellow>", stats.getPingColor(99));
        }

        @Test
        @DisplayName("should return gold for ping >= 100 and < 200")
        void shouldReturnGoldForHighPing() {
            var stats = new PlayerStats(1, 100, 150, 150, 150);
            assertEquals("<gold>", stats.getPingColor(100));
            assertEquals("<gold>", stats.getPingColor(199));
        }

        @Test
        @DisplayName("should return red for ping >= 200")
        void shouldReturnRedForVeryHighPing() {
            var stats = new PlayerStats(1, 100, 250, 250, 250);
            assertEquals("<red>", stats.getPingColor(200));
            assertEquals("<red>", stats.getPingColor(500));
        }
    }
}
