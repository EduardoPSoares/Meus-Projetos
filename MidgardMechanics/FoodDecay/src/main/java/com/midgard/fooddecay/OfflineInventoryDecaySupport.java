package com.midgard.fooddecay;

final class OfflineInventoryDecaySupport {

    record ResidualCoolingSnapshot(boolean active, double storedMultiplier) {
    }

    private OfflineInventoryDecaySupport() {
    }

    static ResidualCoolingSnapshot snapshotResidualCooling(long removedAt, double storedMultiplier,
                                                           long now, long warmingDurationMs) {
        if (warmingDurationMs <= 0L) {
            return new ResidualCoolingSnapshot(false, 1.0);
        }

        long elapsed = Math.max(0L, now - removedAt);
        if (elapsed >= warmingDurationMs) {
            return new ResidualCoolingSnapshot(false, 1.0);
        }

        double progress = (double) elapsed / warmingDurationMs;
        double residualMultiplier = storedMultiplier + (1.0 - storedMultiplier) * progress;
        return new ResidualCoolingSnapshot(true, residualMultiplier);
    }
}
