package com.midgard.fooddecay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FermentationInventorySyncTest {

    @Test
    void keepsRemainingVanillaStackAndReturnsContainerSeparately() {
        FermentationInventorySync.PourPlan plan = FermentationInventorySync.planAfterPour(3, true);

        assertTrue(plan.keepOriginalTypeInMainHand());
        assertEquals(2, plan.mainHandAmount());
        assertTrue(plan.addExtraResultItem());
    }

    @Test
    void replacesLastVanillaContainerInHand() {
        FermentationInventorySync.PourPlan plan = FermentationInventorySync.planAfterPour(1, true);

        assertFalse(plan.keepOriginalTypeInMainHand());
        assertEquals(1, plan.mainHandAmount());
        assertFalse(plan.addExtraResultItem());
    }

    @Test
    void replacesHandDirectlyForNonVanillaConversions() {
        FermentationInventorySync.PourPlan plan = FermentationInventorySync.planAfterPour(1, false);

        assertFalse(plan.keepOriginalTypeInMainHand());
        assertEquals(1, plan.mainHandAmount());
        assertFalse(plan.addExtraResultItem());
    }
}
