package com.midgard.fooddecay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineInventoryDecaySupportTest {

    @Test
    void snapshotsCurrentResidualCoolingBeforePausingOffline() {
        OfflineInventoryDecaySupport.ResidualCoolingSnapshot snapshot =
                OfflineInventoryDecaySupport.snapshotResidualCooling(1_000L, 0.40, 4_000L, 10_000L);

        assertTrue(snapshot.active());
        assertEquals(0.58, snapshot.storedMultiplier(), 0.0001);
    }

    @Test
    void dropsResidualCoolingWhenWarmingWindowAlreadyFinished() {
        OfflineInventoryDecaySupport.ResidualCoolingSnapshot snapshot =
                OfflineInventoryDecaySupport.snapshotResidualCooling(1_000L, 0.40, 12_000L, 10_000L);

        assertFalse(snapshot.active());
        assertEquals(1.0, snapshot.storedMultiplier(), 0.0001);
    }
}
