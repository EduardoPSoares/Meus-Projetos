package me.ray.midgard.modules.item.model;

import org.bukkit.configuration.ConfigurationSection;

public class MidgardItemRestrictions {

    private final MidgardItemImpl item;
    private final ConfigurationSection base;

    private boolean disableInteraction;
    private boolean disableCrafting;
    private boolean disableSmelting;
    private boolean disableRepairing;
    private boolean disableEnchanting;
    private boolean disableSmithing;
    private boolean disableItemDropping;
    private boolean disableDropOnDeath;
    private boolean unstackable;
    private int maxStackSize;

    public MidgardItemRestrictions(MidgardItemImpl item, ConfigurationSection base) {
        this.item = item;
        this.base = base;
        load();
    }

    private void load() {
        this.disableInteraction = base.getBoolean("disable-interaction", false);
        this.disableCrafting = base.getBoolean("disable-crafting", false);
        this.disableSmelting = base.getBoolean("disable-smelting", false);
        this.disableRepairing = base.getBoolean("disable-repairing", false);
        this.disableEnchanting = base.getBoolean("disable-enchanting", false);
        this.disableSmithing = base.getBoolean("disable-smithing", false);
        this.disableItemDropping = base.getBoolean("disable-item-dropping", false);
        this.disableDropOnDeath = base.getBoolean("disable-drop-on-death", false);
        this.unstackable = base.getBoolean("unstackable", false);
        this.maxStackSize = base.getInt("max-stack-size", 64);
    }

    public boolean isDisableInteraction() { return disableInteraction; }
    public void setDisableInteraction(boolean val) { this.disableInteraction = val; base.set("disable-interaction", val); item.save(); }

    public boolean isDisableCrafting() { return disableCrafting; }
    public void setDisableCrafting(boolean val) { this.disableCrafting = val; base.set("disable-crafting", val); item.save(); }

    public boolean isDisableSmelting() { return disableSmelting; }
    public void setDisableSmelting(boolean val) { this.disableSmelting = val; base.set("disable-smelting", val); item.save(); }

    public boolean isDisableRepairing() { return disableRepairing; }
    public void setDisableRepairing(boolean val) { this.disableRepairing = val; base.set("disable-repairing", val); item.save(); }

    public boolean isDisableEnchanting() { return disableEnchanting; }
    public void setDisableEnchanting(boolean val) { this.disableEnchanting = val; base.set("disable-enchanting", val); item.save(); }

    public boolean isDisableSmithing() { return disableSmithing; }
    public void setDisableSmithing(boolean val) { this.disableSmithing = val; base.set("disable-smithing", val); item.save(); }

    public boolean isDisableItemDropping() { return disableItemDropping; }
    public void setDisableItemDropping(boolean val) { this.disableItemDropping = val; base.set("disable-item-dropping", val); item.save(); }

    public boolean isDisableDropOnDeath() { return disableDropOnDeath; }
    public void setDisableDropOnDeath(boolean val) { this.disableDropOnDeath = val; base.set("disable-drop-on-death", val); item.save(); }

    public boolean isUnstackable() { return unstackable; }
    public void setUnstackable(boolean val) { this.unstackable = val; base.set("unstackable", val); item.save(); }

    public int getMaxStackSize() { return maxStackSize; }
    public void setMaxStackSize(int val) { this.maxStackSize = val; base.set("max-stack-size", val); item.save(); }
}
