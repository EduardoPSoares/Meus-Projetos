package me.ray.midgard.modules.item.model;

import org.bukkit.configuration.ConfigurationSection;

public class MidgardItemDisplay {

    private final MidgardItemImpl item;
    private final ConfigurationSection base;

    private String displayedType;
    private String itemModel;
    private int customModelData;
    private String customModelDataStrings;
    private String customModelDataFloats;
    private String loreFormat;
    private String customTooltip;
    private String vanillaTooltipStyle;
    private boolean hideTooltip;
    private String itemParticles;
    private String cameraOverlay;
    private String trimMaterial;
    private String trimPattern;
    private boolean hideArmorTrim;

    public MidgardItemDisplay(MidgardItemImpl item, ConfigurationSection base) {
        this.item = item;
        this.base = base;
        load();
    }

    private void load() {
        this.displayedType = base.getString("displayed-type", "");
        this.itemModel = base.getString("item-model", "");
        this.customModelData = base.getInt("custom-model-data", 0);
        this.customModelDataStrings = base.getString("custom-model-data-strings", "");
        this.customModelDataFloats = base.getString("custom-model-data-floats", "");
        this.loreFormat = base.getString("lore-format", "");
        this.customTooltip = base.getString("custom-tooltip", "");
        this.vanillaTooltipStyle = base.getString("vanilla-tooltip-style", "");
        this.hideTooltip = base.getBoolean("hide-tooltip", false);
        this.itemParticles = base.getString("item-particles", "");
        this.cameraOverlay = base.getString("camera-overlay", "");
        this.trimMaterial = base.getString("trim-material", "");
        this.trimPattern = base.getString("trim-pattern", "");
        this.hideArmorTrim = base.getBoolean("hide-armor-trim", false);
    }

    public String getDisplayedType() { return displayedType; }
    public void setDisplayedType(String displayedType) {
        this.displayedType = displayedType;
        base.set("displayed-type", displayedType);
        item.save();
    }

    public String getItemModel() { return itemModel; }
    public void setItemModel(String itemModel) {
        this.itemModel = itemModel;
        base.set("item-model", itemModel);
        item.save();
    }

    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int val) { this.customModelData = val; base.set("custom-model-data", val); item.save(); }

    public String getCustomModelDataStrings() { return customModelDataStrings; }
    public void setCustomModelDataStrings(String val) { this.customModelDataStrings = val; base.set("custom-model-data-strings", val); item.save(); }

    public String getCustomModelDataFloats() { return customModelDataFloats; }
    public void setCustomModelDataFloats(String val) { this.customModelDataFloats = val; base.set("custom-model-data-floats", val); item.save(); }

    public String getLoreFormat() { return loreFormat; }
    public void setLoreFormat(String val) { this.loreFormat = val; base.set("lore-format", val); item.save(); }

    public String getCustomTooltip() { return customTooltip; }
    public void setCustomTooltip(String val) { this.customTooltip = val; base.set("custom-tooltip", val); item.save(); }

    public String getVanillaTooltipStyle() { return vanillaTooltipStyle; }
    public void setVanillaTooltipStyle(String val) { this.vanillaTooltipStyle = val; base.set("vanilla-tooltip-style", val); item.save(); }

    public boolean isHideTooltip() { return hideTooltip; }
    public void setHideTooltip(boolean val) { this.hideTooltip = val; base.set("hide-tooltip", val); item.save(); }

    public String getItemParticles() { return itemParticles; }
    public void setItemParticles(String val) { this.itemParticles = val; base.set("item-particles", val); item.save(); }

    public String getCameraOverlay() { return cameraOverlay; }
    public void setCameraOverlay(String val) { this.cameraOverlay = val; base.set("camera-overlay", val); item.save(); }

    public String getTrimMaterial() { return trimMaterial; }
    public void setTrimMaterial(String val) { this.trimMaterial = val; base.set("trim-material", val); item.save(); }

    public String getTrimPattern() { return trimPattern; }
    public void setTrimPattern(String val) { this.trimPattern = val; base.set("trim-pattern", val); item.save(); }

    public boolean isHideArmorTrim() { return hideArmorTrim; }
    public void setHideArmorTrim(boolean val) { this.hideArmorTrim = val; base.set("hide-armor-trim", val); item.save(); }
}
