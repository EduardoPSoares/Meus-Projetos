package com.midgard.fooddecay;

public final class FermentationInvalidRecipePenalty {

    private static final double LOSS_RATIO = 0.20;
    private static final int MINIMUM_LOSS_MB = 50;

    private FermentationInvalidRecipePenalty() {
    }

    public record Outcome(int lostAmountMb, int remainingAmountMb) {
    }

    public static Outcome apply(int currentAmountMb, int recentPourAmountMb) {
        if (currentAmountMb <= 0 || recentPourAmountMb <= 0) {
            return new Outcome(0, Math.max(0, currentAmountMb));
        }

        int loss = (int) Math.ceil(recentPourAmountMb * LOSS_RATIO);
        loss = Math.max(MINIMUM_LOSS_MB, loss);
        loss = Math.min(loss, currentAmountMb);

        return new Outcome(loss, currentAmountMb - loss);
    }

    public static int getLossPercent() {
        return (int) Math.round(LOSS_RATIO * 100.0);
    }

    public static int getMinimumLossMb() {
        return MINIMUM_LOSS_MB;
    }
}
