package me.ray.midgard.modules.races.model;

import org.bukkit.inventory.ItemStack;
import me.ray.midgard.modules.races.api.RaceXpSource;
import java.util.List;
import java.util.Map;

public class Race {

    private final String id;
    private final String displayName;
    private final String parentRace;
    private final int minLevel;
    private final int slot;
    private final int treeSlot;
    private final ItemStack icon;
    private final List<String> description;
    private final Map<String, Double> attributes;
    private final Map<String, Double> perLevelAttributes;
    private final Map<String, Double> dayAttributes;
    private final Map<String, Double> nightAttributes;
    private final Map<String, Double> dayPerLevelAttributes;
    private final Map<String, Double> nightPerLevelAttributes;
    private final List<ConfiguredTrait> traits;
    private final List<String> permissions;
    private final List<String> onSelectCommands;
    private final List<String> onRemoveCommands;
    private final List<EvolutionRequirement> evolutionRequirements;
    private final String exclusionBranch;
    private final boolean allowDevolution;
    private final Map<RaceXpSource, Double> xpMultipliers;

    public Race(String id, String displayName, String parentRace, int minLevel, int slot, int treeSlot,
                ItemStack icon, List<String> description, Map<String, Double> attributes,
                Map<String, Double> perLevelAttributes, Map<String, Double> dayAttributes,
                Map<String, Double> nightAttributes, Map<String, Double> dayPerLevelAttributes,
                Map<String, Double> nightPerLevelAttributes, List<ConfiguredTrait> traits,
                List<String> permissions, List<String> onSelectCommands, List<String> onRemoveCommands,
                List<EvolutionRequirement> evolutionRequirements, String exclusionBranch, boolean allowDevolution,
                Map<RaceXpSource, Double> xpMultipliers) {
        this.id = id;
        this.displayName = displayName;
        this.parentRace = parentRace;
        this.minLevel = minLevel;
        this.slot = slot;
        this.treeSlot = treeSlot;
        this.icon = icon;
        this.description = description;
        this.attributes = attributes;
        this.perLevelAttributes = perLevelAttributes;
        this.dayAttributes = dayAttributes;
        this.nightAttributes = nightAttributes;
        this.dayPerLevelAttributes = dayPerLevelAttributes;
        this.nightPerLevelAttributes = nightPerLevelAttributes;
        this.traits = traits;
        this.permissions = permissions;
        this.onSelectCommands = onSelectCommands;
        this.onRemoveCommands = onRemoveCommands;
        this.evolutionRequirements = evolutionRequirements;
        this.exclusionBranch = exclusionBranch;
        this.allowDevolution = allowDevolution;
        this.xpMultipliers = xpMultipliers == null || xpMultipliers.isEmpty() ? Map.of() : Map.copyOf(xpMultipliers);
    }

    public Race(String id, String displayName, String parentRace, int minLevel, int slot, int treeSlot,
                ItemStack icon, List<String> description, Map<String, Double> attributes,
                Map<String, Double> perLevelAttributes, List<ConfiguredTrait> traits,
                List<String> permissions, List<String> onSelectCommands, List<String> onRemoveCommands,
                List<EvolutionRequirement> evolutionRequirements, String exclusionBranch, boolean allowDevolution) {
        this(id, displayName, parentRace, minLevel, slot, treeSlot, icon, description, attributes,
                perLevelAttributes, Map.of(), Map.of(), Map.of(), Map.of(),
                traits, permissions, onSelectCommands, onRemoveCommands,
                evolutionRequirements, exclusionBranch, allowDevolution, Map.of());
    }

    public Race(String id, String displayName, String parentRace, int minLevel, int slot, int treeSlot,
                ItemStack icon, List<String> description, Map<String, Double> attributes,
                Map<String, Double> perLevelAttributes, List<ConfiguredTrait> traits,
                List<String> permissions, List<String> onSelectCommands, List<String> onRemoveCommands) {
        this(id, displayName, parentRace, minLevel, slot, treeSlot, icon, description, attributes,
                perLevelAttributes, traits, permissions, onSelectCommands, onRemoveCommands,
                List.of(), null, true);
    }


    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getParentRace() {
        return parentRace;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getSlot() {
        return slot;
    }

    public int getTreeSlot() {
        return treeSlot;
    }

    public boolean isSubRace() {
        return parentRace != null && !parentRace.isEmpty();
    }

    public ItemStack getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    public Map<String, Double> getAttributes() {
        return attributes;
    }

    public Map<String, Double> getPerLevelAttributes() {
        return perLevelAttributes;
    }

    public Map<String, Double> getDayAttributes() {
        return dayAttributes;
    }

    public Map<String, Double> getNightAttributes() {
        return nightAttributes;
    }

    public Map<String, Double> getDayPerLevelAttributes() {
        return dayPerLevelAttributes;
    }

    public Map<String, Double> getNightPerLevelAttributes() {
        return nightPerLevelAttributes;
    }

    public boolean hasTimeAttributes() {
        return (dayAttributes != null && !dayAttributes.isEmpty())
                || (nightAttributes != null && !nightAttributes.isEmpty())
                || (dayPerLevelAttributes != null && !dayPerLevelAttributes.isEmpty())
                || (nightPerLevelAttributes != null && !nightPerLevelAttributes.isEmpty());
    }

    public List<ConfiguredTrait> getTraits() {
        return traits;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public List<String> getOnSelectCommands() {
        return onSelectCommands;
    }

    public List<String> getOnRemoveCommands() {
        return onRemoveCommands;
    }

    public List<EvolutionRequirement> getEvolutionRequirements() {
        return evolutionRequirements;
    }

    public String getExclusionBranch() {
        return exclusionBranch;
    }

    public boolean isAllowDevolution() {
        return allowDevolution;
    }

    public Map<RaceXpSource, Double> getXpMultipliers() {
        return xpMultipliers;
    }

    public double getXpMultiplier(RaceXpSource source) {
        if (xpMultipliers == null || xpMultipliers.isEmpty()) { return 1.0; }
        return xpMultipliers.getOrDefault(source, 1.0);
    }

    public boolean hasEvolutionRequirements() {
        return evolutionRequirements != null && !evolutionRequirements.isEmpty();
    }
}
