package me.ray.midgard.modules.item.model;

import org.bukkit.configuration.ConfigurationSection;

public class MidgardItemDurability {

    private final MidgardItemImpl item;
    private final ConfigurationSection base;

    private int maxCustomDurability;
    private int maxVanillaDurability;
    private boolean lostWhenBroken;
    private boolean hideDurabilityBar;
    private boolean unbreakable;
    private String repairReference;

    public MidgardItemDurability(MidgardItemImpl item, ConfigurationSection base) {
        this.item = item;
        this.base = base;
        load();
    }

    private void load() {
        this.maxCustomDurability = base.getInt("max-custom-durability", 0);
        this.maxVanillaDurability = base.getInt("max-vanilla-durability", 0);
        this.lostWhenBroken = base.getBoolean("lost-when-broken", false);
        this.hideDurabilityBar = base.getBoolean("hide-durability-bar", false);
        this.unbreakable = base.getBoolean("unbreakable", false);
        this.repairReference = base.getString("repair-reference", "");
    }

    public int getMaxCustomDurability() { return maxCustomDurability; }
    public void setMaxCustomDurability(int val) { this.maxCustomDurability = val; base.set("max-custom-durability", val); item.save(); }

    public int getMaxVanillaDurability() { return maxVanillaDurability; }
    public void setMaxVanillaDurability(int val) { this.maxVanillaDurability = val; base.set("max-vanilla-durability", val); item.save(); }

    public boolean isLostWhenBroken() { return lostWhenBroken; }
    public void setLostWhenBroken(boolean val) { this.lostWhenBroken = val; base.set("lost-when-broken", val); item.save(); }

    public boolean isHideDurabilityBar() { return hideDurabilityBar; }
    public void setHideDurabilityBar(boolean val) { this.hideDurabilityBar = val; base.set("hide-durability-bar", val); item.save(); }

    public boolean isUnbreakable() { return unbreakable; }
    public void setUnbreakable(boolean val) { this.unbreakable = val; base.set("unbreakable", val); item.save(); }

    public String getRepairReference() { return repairReference; }
    public void setRepairReference(String val) { this.repairReference = val; base.set("repair-reference", val); item.save(); }
}
