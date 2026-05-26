package com.midgard.fooddecay;

import org.bukkit.inventory.ItemStack;

public final class FermentationInventorySync {

    private FermentationInventorySync() {
    }

    public static PourPlan planAfterPour(int originalAmount, boolean vanillaConversion) {
        if (!vanillaConversion) {
            return new PourPlan(false, 1, false);
        }

        int remaining = originalAmount - 1;
        if (remaining > 0) {
            return new PourPlan(true, remaining, true);
        }

        return new PourPlan(false, 1, false);
    }

    public static HandUpdate planAfterPour(ItemStack originalHand, ItemStack resultContainer, boolean vanillaConversion) {
        PourPlan plan = planAfterPour(originalHand.getAmount(), vanillaConversion);
        ItemStack mainHand = plan.keepOriginalTypeInMainHand() ? originalHand.clone() : resultContainer.clone();
        mainHand.setAmount(plan.mainHandAmount());
        ItemStack extraItem = plan.addExtraResultItem() ? resultContainer.clone() : null;

        return new HandUpdate(mainHand, extraItem);
    }

    public record PourPlan(boolean keepOriginalTypeInMainHand, int mainHandAmount, boolean addExtraResultItem) {
    }

    public record HandUpdate(ItemStack mainHand, ItemStack extraItem) {
    }
}
