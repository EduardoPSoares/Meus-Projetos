package me.ray.midgard.modules.races.model;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import java.util.HashMap;
import java.util.Map;

public class ConfiguredTrait {
    private final String id;
    private final RaceTrait trait;
    private final TraitTrigger trigger;
    private final int minLevel;
    private final Map<String, Object> config;
    private final boolean selectable;
    private final String exclusionGroup;
    private final TraitCondition condition;

    public ConfiguredTrait(String id, RaceTrait trait, TraitTrigger trigger, int minLevel, Map<String, Object> config) {
        this(id, trait, trigger, minLevel, config, false, null, TraitCondition.ALWAYS);
    }

    public ConfiguredTrait(String id, RaceTrait trait, TraitTrigger trigger, int minLevel, Map<String, Object> config, boolean selectable, String exclusionGroup) {
        this(id, trait, trigger, minLevel, config, selectable, exclusionGroup, TraitCondition.ALWAYS);
    }

    public ConfiguredTrait(String id, RaceTrait trait, TraitTrigger trigger, int minLevel, Map<String, Object> config, boolean selectable, String exclusionGroup, TraitCondition condition) {
        this.id = id;
        this.trait = trait;
        this.trigger = trigger;
        this.minLevel = minLevel;
        this.config = config;
        this.selectable = selectable;
        this.exclusionGroup = exclusionGroup;
        this.condition = condition;
    }

    public String getId() { return id; }
    public RaceTrait getTrait() { return trait; }
    public TraitTrigger getTrigger() { return trigger; }
    public int getMinLevel() { return minLevel; }
    public Map<String, Object> getConfig() { return config != null ? config : Map.of(); }
    public boolean isSelectable() { return selectable; }
    public String getExclusionGroup() { return exclusionGroup; }
    public TraitCondition getCondition() { return condition; }
}
