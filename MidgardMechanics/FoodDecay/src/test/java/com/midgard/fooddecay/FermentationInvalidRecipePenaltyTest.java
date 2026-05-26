package com.midgard.fooddecay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FermentationInvalidRecipePenaltyTest {

    @Test
    void appliesConfiguredLossToBottleSizedPour() {
        FermentationInvalidRecipePenalty.Outcome outcome =
                FermentationInvalidRecipePenalty.apply(250, 250);

        assertEquals(50, outcome.lostAmountMb());
        assertEquals(200, outcome.remainingAmountMb());
    }

    @Test
    void appliesConfiguredLossToBucketSizedPour() {
        FermentationInvalidRecipePenalty.Outcome outcome =
                FermentationInvalidRecipePenalty.apply(1000, 1000);

        assertEquals(200, outcome.lostAmountMb());
        assertEquals(800, outcome.remainingAmountMb());
    }

    @Test
    void neverRemovesMoreThanCurrentlyStored() {
        FermentationInvalidRecipePenalty.Outcome outcome =
                FermentationInvalidRecipePenalty.apply(35, 250);

        assertEquals(35, outcome.lostAmountMb());
        assertEquals(0, outcome.remainingAmountMb());
    }

    @Test
    void ignoresEmptyInputs() {
        FermentationInvalidRecipePenalty.Outcome outcome =
                FermentationInvalidRecipePenalty.apply(0, 250);

        assertEquals(0, outcome.lostAmountMb());
        assertEquals(0, outcome.remainingAmountMb());
    }
}
