package me.ray.midgard.modules.item.model;

import me.ray.midgard.modules.item.ability.AbilityTrigger;
import me.ray.midgard.modules.item.ability.SpellBinding;
import me.ray.midgard.modules.item.utils.StatRange;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MidgardItemMechanics {

    private final MidgardItemImpl item;
    private final ConfigurationSection base;

    private final Map<ItemStat, StatRange> stats;
    private String enchantments;
    private boolean hideEnchantments;
    private boolean disableAdvancedEnchants;
    private String nbtTags;
    private String itemSet;
    private String cooldownReference;
    private String craftingRecipePermission;
    private boolean amphibian;
    private final List<String> permanentEffects;
    private final List<String> grantedPermissions;
    private final List<String> customSounds;
    private final List<String> commands;
    private final List<String> itemAbilities;
    private final List<SpellBinding> spellBindings;
    private final List<String> gemSockets;
    private int baseItemDamage;
    private String equippableSlot;
    private String equippableModel;

    public MidgardItemMechanics(MidgardItemImpl item, ConfigurationSection base) {
        this.item = item;
        this.base = base;
        
        this.stats = new HashMap<>();
        for (ItemStat stat : ItemStat.values()) {
            if (base.contains(stat.getPath())) {
                if (base.isBoolean(stat.getPath())) {
                    double val = base.getBoolean(stat.getPath()) ? 1.0 : 0.0;
                    this.stats.put(stat, new StatRange(val, val));
                } else if (base.isString(stat.getPath())) {
                    this.stats.put(stat, StatRange.parse(base.getString(stat.getPath())));
                } else if (base.isDouble(stat.getPath()) || base.isInt(stat.getPath())) {
                    double val = base.getDouble(stat.getPath());
                    this.stats.put(stat, new StatRange(val, val));
                }
            }
        }

        this.enchantments = base.getString("enchantments", "");
        this.hideEnchantments = base.getBoolean("hide-enchantments", false);
        this.disableAdvancedEnchants = base.getBoolean("disable-advanced-enchants", false);
        this.nbtTags = base.getString("nbt-tags", "");
        this.itemSet = base.getString("item-set", "");
        this.cooldownReference = base.getString("cooldown-reference", "");
        this.craftingRecipePermission = base.getString("crafting-recipe-permission", "");
        this.amphibian = base.getBoolean("amphibian", false);
        this.baseItemDamage = base.getInt("base-item-damage", 0);
        this.equippableSlot = base.getString("equippable-slot", "");
        this.equippableModel = base.getString("equippable-model", "");

        this.permanentEffects = base.getStringList("permanent-effects");
        this.grantedPermissions = base.getStringList("granted-permissions");
        this.customSounds = base.getStringList("custom-sounds");
        this.commands = base.getStringList("commands");
        this.itemAbilities = base.getStringList("abilities");
        this.gemSockets = base.getStringList("gem-sockets");
        
        // Load spell bindings from config
        this.spellBindings = new ArrayList<>();
        if (base.contains("spell-bindings")) {
            if (base.isConfigurationSection("spell-bindings")) {
                // Format 1: Named sections
                // spell-bindings:
                //   ability1:
                //     spell: fireball
                //     trigger: LEFT_CLICK
                ConfigurationSection bindingsSection = base.getConfigurationSection("spell-bindings");
                for (String key : bindingsSection.getKeys(false)) {
                    ConfigurationSection bindingConfig = bindingsSection.getConfigurationSection(key);
                    if (bindingConfig != null) {
                        SpellBinding binding = SpellBinding.fromConfig(bindingConfig);
                        if (binding != null) {
                            this.spellBindings.add(binding);
                        }
                    }
                }
            } else if (base.isList("spell-bindings")) {
                // Format 2: List of maps (from importer)
                // spell-bindings:
                //   - spell: fireball
                //     trigger: LEFT_CLICK
                try {
                    @SuppressWarnings("unchecked")
                    java.util.List<java.util.Map<String, Object>> bindingList = 
                        (java.util.List<java.util.Map<String, Object>>) base.getList("spell-bindings");
                    if (bindingList != null) {
                        for (java.util.Map<String, Object> map : bindingList) {
                            SpellBinding binding = SpellBinding.fromMap(map);
                            if (binding != null) {
                                this.spellBindings.add(binding);
                            }
                        }
                    }
                } catch (ClassCastException e) {
                    me.ray.midgard.core.debug.MidgardLogger.warn("Invalid spell-bindings format for item " + item.getId() + " (expected list of maps)");
                }
            }
        }
    }

    public Map<ItemStat, StatRange> getStats() { return stats; }
    public void setStat(ItemStat stat, double value) {
        stats.put(stat, new StatRange(value, value));
        base.set(stat.getPath(), value);
        item.save();
    }
    public void setStat(ItemStat stat, String value) {
        stats.put(stat, StatRange.parse(value));
        base.set(stat.getPath(), value);
        item.save();
    }
    public double getStat(ItemStat stat) { 
        StatRange range = stats.get(stat);
        return range != null ? range.getMax() : 0.0; 
    }
    public StatRange getStatRange(ItemStat stat) {
        return stats.get(stat);
    }

    public String getEnchantments() { return enchantments; }
    public void setEnchantments(String val) { this.enchantments = val; base.set("enchantments", val); item.save(); }

    public boolean isHideEnchantments() { return hideEnchantments; }
    public void setHideEnchantments(boolean val) { this.hideEnchantments = val; base.set("hide-enchantments", val); item.save(); }

    public boolean isDisableAdvancedEnchants() { return disableAdvancedEnchants; }
    public void setDisableAdvancedEnchants(boolean val) { this.disableAdvancedEnchants = val; base.set("disable-advanced-enchants", val); item.save(); }

    public String getNbtTags() { return nbtTags; }
    public void setNbtTags(String val) { this.nbtTags = val; base.set("nbt-tags", val); item.save(); }

    public String getItemSet() { return itemSet; }
    public void setItemSet(String val) { this.itemSet = val; base.set("item-set", val); item.save(); }

    public String getCooldownReference() { return cooldownReference; }
    public void setCooldownReference(String val) { this.cooldownReference = val; base.set("cooldown-reference", val); item.save(); }

    public String getCraftingRecipePermission() { return craftingRecipePermission; }
    public void setCraftingRecipePermission(String val) { this.craftingRecipePermission = val; base.set("crafting-recipe-permission", val); item.save(); }

    public boolean isAmphibian() { return amphibian; }
    public void setAmphibian(boolean val) { this.amphibian = val; base.set("amphibian", val); item.save(); }

    public int getBaseItemDamage() { return baseItemDamage; }
    public void setBaseItemDamage(int baseItemDamage) { this.baseItemDamage = baseItemDamage; base.set("base-item-damage", baseItemDamage); item.save(); }

    public String getEquippableSlot() { return equippableSlot; }
    public void setEquippableSlot(String equippableSlot) { this.equippableSlot = equippableSlot; base.set("equippable-slot", equippableSlot); item.save(); }

    public String getEquippableModel() { return equippableModel; }
    public void setEquippableModel(String equippableModel) { this.equippableModel = equippableModel; base.set("equippable-model", equippableModel); item.save(); }

    public List<String> getPermanentEffects() { return permanentEffects; }
    public void setPermanentEffects(List<String> list) { this.permanentEffects.clear(); this.permanentEffects.addAll(list); base.set("permanent-effects", list); item.save(); }

    public List<String> getGrantedPermissions() { return grantedPermissions; }
    public void setGrantedPermissions(List<String> list) { this.grantedPermissions.clear(); this.grantedPermissions.addAll(list); base.set("granted-permissions", list); item.save(); }

    public List<String> getCustomSounds() { return customSounds; }
    public void setCustomSounds(List<String> list) { this.customSounds.clear(); this.customSounds.addAll(list); base.set("custom-sounds", list); item.save(); }

    public List<String> getCommands() { return commands; }
    public void setCommands(List<String> list) { this.commands.clear(); this.commands.addAll(list); base.set("commands", list); item.save(); }

    public List<String> getItemAbilities() { return itemAbilities; }
    public void setItemAbilities(List<String> list) { this.itemAbilities.clear(); this.itemAbilities.addAll(list); base.set("abilities", list); item.save(); }

    public List<String> getGemSockets() { return gemSockets; }
    public void setGemSockets(List<String> list) { this.gemSockets.clear(); this.gemSockets.addAll(list); base.set("gem-sockets", list); item.save(); }

    public List<SpellBinding> getSpellBindings() { return spellBindings; }
    
    public List<SpellBinding> getSpellBindingsForTrigger(AbilityTrigger trigger) {
        List<SpellBinding> result = new ArrayList<>();
        for (SpellBinding binding : spellBindings) {
            if (binding.getTrigger() == trigger) {
                result.add(binding);
            }
        }
        return result;
    }
    
    public boolean hasSpellBindings() {
        return !spellBindings.isEmpty();
    }
}
