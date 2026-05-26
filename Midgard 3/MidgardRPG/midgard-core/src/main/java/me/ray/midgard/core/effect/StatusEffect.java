package me.ray.midgard.core.effect;

import me.ray.midgard.core.attribute.AttributeModifier;

import java.util.ArrayList;
import java.util.List;

public class StatusEffect {

    private final String id;
    private String displayName;
    private EffectType type;
    private long defaultDuration; // in ticks
    private long tickInterval; // in ticks
    
    private final List<EffectModifier> attributeModifiers = new ArrayList<>();
    
    // We can use String lists for script actions
    private final List<String> onStartActions = new ArrayList<>();
    private final List<String> onTickActions = new ArrayList<>();
    private final List<String> onEndActions = new ArrayList<>();

    public StatusEffect(String id, EffectType type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public EffectType getType() {
        return type;
    }

    public long getDefaultDuration() {
        return defaultDuration;
    }

    public void setDefaultDuration(long defaultDuration) {
        this.defaultDuration = defaultDuration;
    }

    public long getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(long tickInterval) {
        this.tickInterval = tickInterval;
    }

    public List<EffectModifier> getAttributeModifiers() {
        return attributeModifiers;
    }
    
    public void addAttributeModifier(String attributeId, AttributeModifier modifier) {
        attributeModifiers.add(new EffectModifier(attributeId, modifier));
    }

    public static class EffectModifier {
        private final String attributeId;
        private final AttributeModifier modifier;

        public EffectModifier(String attributeId, AttributeModifier modifier) {
            this.attributeId = attributeId;
            this.modifier = modifier;
        }

        public String getAttributeId() {
            return attributeId;
        }

        public AttributeModifier getModifier() {
            return modifier;
        }
    }

    public List<String> getOnStartActions() {
        return onStartActions;
    }

    public List<String> getOnTickActions() {
        return onTickActions;
    }

    public List<String> getOnEndActions() {
        return onEndActions;
    }
    
    public enum EffectType {
        BUFF,
        DEBUFF,
        NEUTRAL
    }
}
