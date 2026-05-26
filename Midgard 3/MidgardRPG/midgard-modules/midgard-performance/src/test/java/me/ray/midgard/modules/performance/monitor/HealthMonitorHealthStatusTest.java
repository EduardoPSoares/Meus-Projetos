package me.ray.midgard.modules.performance.monitor;

import me.ray.midgard.modules.performance.monitor.HealthMonitor.HealthStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HealthMonitor.HealthStatus")
class HealthMonitorHealthStatusTest {

    @Nested
    @DisplayName("activeAlerts()")
    class ActiveAlertsTests {

        @Test
        @DisplayName("should return 0 when no alerts active")
        void shouldReturnZeroWhenNoAlerts() {
            var status = new HealthStatus(false, false, false, false, 15.0, 85.0, 1000, 500);
            assertEquals(0, status.activeAlerts());
        }

        @Test
        @DisplayName("should return 1 when single alert active")
        void shouldReturnOneForSingleAlert() {
            var status = new HealthStatus(true, false, false, false, 15.0, 85.0, 1000, 500);
            assertEquals(1, status.activeAlerts());
        }

        @Test
        @DisplayName("should return 2 for two alerts")
        void shouldReturnTwoForTwoAlerts() {
            var status = new HealthStatus(true, true, false, false, 15.0, 85.0, 1000, 500);
            assertEquals(2, status.activeAlerts());
        }

        @Test
        @DisplayName("should return 3 for three alerts")
        void shouldReturnThreeForThreeAlerts() {
            var status = new HealthStatus(true, true, true, false, 15.0, 85.0, 1000, 500);
            assertEquals(3, status.activeAlerts());
        }

        @Test
        @DisplayName("should return 4 when all alerts active")
        void shouldReturnFourWhenAllAlerts() {
            var status = new HealthStatus(true, true, true, true, 15.0, 85.0, 1000, 500);
            assertEquals(4, status.activeAlerts());
        }

        @Test
        @DisplayName("should count individual alert types correctly")
        void shouldCountIndividualAlerts() {
            // Only entity alert
            assertEquals(1, new HealthStatus(false, false, true, false, 15.0, 85.0, 1000, 500).activeAlerts());
            // Only chunk alert
            assertEquals(1, new HealthStatus(false, false, false, true, 15.0, 85.0, 1000, 500).activeAlerts());
            // Memory + chunk
            assertEquals(2, new HealthStatus(false, true, false, true, 15.0, 85.0, 1000, 500).activeAlerts());
        }
    }

    @Nested
    @DisplayName("getOverallStatus()")
    class OverallStatusTests {

        @Test
        @DisplayName("should return HEALTHY when no alerts")
        void shouldReturnHealthyWhenNoAlerts() {
            var status = new HealthStatus(false, false, false, false, 15.0, 85.0, 1000, 500);
            assertEquals("HEALTHY", status.getOverallStatus());
        }

        @Test
        @DisplayName("should return WARNING when 1 alert active")
        void shouldReturnWarningForOneAlert() {
            var status = new HealthStatus(true, false, false, false, 15.0, 85.0, 1000, 500);
            assertEquals("WARNING", status.getOverallStatus());
        }

        @Test
        @DisplayName("should return WARNING when 2 alerts active")
        void shouldReturnWarningForTwoAlerts() {
            var status = new HealthStatus(true, true, false, false, 15.0, 85.0, 1000, 500);
            assertEquals("WARNING", status.getOverallStatus());
        }

        @Test
        @DisplayName("should return CRITICAL when 3 alerts active")
        void shouldReturnCriticalForThreeAlerts() {
            var status = new HealthStatus(true, true, true, false, 15.0, 85.0, 1000, 500);
            assertEquals("CRITICAL", status.getOverallStatus());
        }

        @Test
        @DisplayName("should return CRITICAL when all 4 alerts active")
        void shouldReturnCriticalForFourAlerts() {
            var status = new HealthStatus(true, true, true, true, 15.0, 85.0, 1000, 500);
            assertEquals("CRITICAL", status.getOverallStatus());
        }
    }

    @Nested
    @DisplayName("Record Fields")
    class RecordFieldTests {

        @Test
        @DisplayName("should store threshold values correctly")
        void shouldStoreThresholds() {
            var status = new HealthStatus(false, false, false, false, 10.0, 90.0, 2000, 750);
            assertEquals(10.0, status.tpsThreshold());
            assertEquals(90.0, status.memoryThreshold());
            assertEquals(2000, status.entityThreshold());
            assertEquals(750, status.chunkThreshold());
        }

        @Test
        @DisplayName("should store alert flags correctly")
        void shouldStoreAlertFlags() {
            var status = new HealthStatus(true, false, true, false, 15.0, 85.0, 1000, 500);
            assertTrue(status.tpsAlert());
            assertFalse(status.memoryAlert());
            assertTrue(status.entityAlert());
            assertFalse(status.chunkAlert());
        }
    }
}
