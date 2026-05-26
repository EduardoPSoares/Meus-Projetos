package me.ray.midgard.modules.item.model;

import org.bukkit.configuration.ConfigurationSection;

public class MidgardItemUpdaterOptions {

    private final MidgardItemImpl item;
    private final ConfigurationSection base;

    private boolean keepLore;
    private boolean keepName;
    private boolean keepEnchantments;
    private boolean keepExternalSH;
    private boolean keepUpgrades;
    private boolean keepGemStones;
    private boolean keepSoulbind;

    public MidgardItemUpdaterOptions(MidgardItemImpl item, ConfigurationSection base) {
        this.item = item;
        this.base = base;
        load();
    }

    private void load() {
        this.keepLore = base.getBoolean("keep-lore", false);
        this.keepName = base.getBoolean("keep-name", true);
        this.keepEnchantments = base.getBoolean("keep-enchantments", true);
        this.keepExternalSH = base.getBoolean("keep-external-sh", true);
        this.keepUpgrades = base.getBoolean("keep-upgrades", true);
        this.keepGemStones = base.getBoolean("keep-gem-stones", true);
        this.keepSoulbind = base.getBoolean("keep-soulbind", true);
    }

    public boolean isKeepLore() { return keepLore; }
    public void setKeepLore(boolean val) { this.keepLore = val; base.set("keep-lore", val); item.save(); }

    public boolean isKeepName() { return keepName; }
    public void setKeepName(boolean val) { this.keepName = val; base.set("keep-name", val); item.save(); }

    public boolean isKeepEnchantments() { return keepEnchantments; }
    public void setKeepEnchantments(boolean val) { this.keepEnchantments = val; base.set("keep-enchantments", val); item.save(); }

    public boolean isKeepExternalSH() { return keepExternalSH; }
    public void setKeepExternalSH(boolean val) { this.keepExternalSH = val; base.set("keep-external-sh", val); item.save(); }

    public boolean isKeepUpgrades() { return keepUpgrades; }
    public void setKeepUpgrades(boolean val) { this.keepUpgrades = val; base.set("keep-upgrades", val); item.save(); }

    public boolean isKeepGemStones() { return keepGemStones; }
    public void setKeepGemStones(boolean val) { this.keepGemStones = val; base.set("keep-gem-stones", val); item.save(); }

    public boolean isKeepSoulbind() { return keepSoulbind; }
    public void setKeepSoulbind(boolean val) { this.keepSoulbind = val; base.set("keep-soulbind", val); item.save(); }
}
