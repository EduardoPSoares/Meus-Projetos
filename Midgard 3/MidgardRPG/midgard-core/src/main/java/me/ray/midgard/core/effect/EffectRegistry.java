package me.ray.midgard.core.effect;

import me.ray.midgard.core.registry.Registry;

public class EffectRegistry extends Registry<String, StatusEffect> {

    private static final EffectRegistry INSTANCE = new EffectRegistry();

    public static EffectRegistry getInstance() {
        return INSTANCE;
    }

    private EffectRegistry() {}
    
    public java.util.Optional<StatusEffect> getEffect(String id) {
        return get(id);
    }
}
