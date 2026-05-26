package me.ray.midgard.modules.item.model;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

public class MidgardItemRequirements {

    private final MidgardItemImpl item;
    private final ConfigurationSection base;

    private String permission;
    private final List<String> requiredClasses;
    private int requiredLevel;
    private final List<String> requiredBiomes;
    private final List<String> compatibleTypes;
    private final List<String> compatibleIds;
    private final List<String> compatibleMaterials;

    public MidgardItemRequirements(MidgardItemImpl item, ConfigurationSection base) {
        this.item = item;
        this.base = base;
        
        this.permission = base.getString("permission", "");
        // Backward compat: read new list format, fallback to old single-string format
        List<String> classes = base.getStringList("required-classes");
        if (classes.isEmpty()) {
            String oldClass = base.getString("required-class", "");
            if (oldClass != null && !oldClass.isEmpty()) {
                classes = new java.util.ArrayList<>();
                classes.add(oldClass);
            }
        }
        this.requiredClasses = new java.util.ArrayList<>(classes);
        this.requiredLevel = base.getInt("required-level", 0);
        this.requiredBiomes = base.getStringList("required-biomes");
        this.compatibleTypes = base.getStringList("compatible-types");
        this.compatibleIds = base.getStringList("compatible-ids");
        this.compatibleMaterials = base.getStringList("compatible-materials");
    }

    public String getPermission() { return permission; }
    public void setPermission(String val) { this.permission = val; base.set("permission", val); item.save(); }

    public List<String> getRequiredClasses() { return requiredClasses; }
    public void setRequiredClasses(List<String> list) { this.requiredClasses.clear(); this.requiredClasses.addAll(list); base.set("required-classes", list); base.set("required-class", null); item.save(); }

    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int val) { this.requiredLevel = val; base.set("required-level", val); item.save(); }

    public List<String> getRequiredBiomes() { return requiredBiomes; }
    public void setRequiredBiomes(List<String> list) { this.requiredBiomes.clear(); this.requiredBiomes.addAll(list); base.set("required-biomes", list); item.save(); }

    public List<String> getCompatibleTypes() { return compatibleTypes; }
    public void setCompatibleTypes(List<String> list) { this.compatibleTypes.clear(); this.compatibleTypes.addAll(list); base.set("compatible-types", list); item.save(); }

    public List<String> getCompatibleIds() { return compatibleIds; }
    public void setCompatibleIds(List<String> list) { this.compatibleIds.clear(); this.compatibleIds.addAll(list); base.set("compatible-ids", list); item.save(); }

    public List<String> getCompatibleMaterials() { return compatibleMaterials; }
    public void setCompatibleMaterials(List<String> list) { this.compatibleMaterials.clear(); this.compatibleMaterials.addAll(list); base.set("compatible-materials", list); item.save(); }
}
