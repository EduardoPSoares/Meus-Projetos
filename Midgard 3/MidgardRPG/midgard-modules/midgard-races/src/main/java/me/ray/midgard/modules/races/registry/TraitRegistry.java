package me.ray.midgard.modules.races.registry;

import me.ray.midgard.modules.races.api.RaceTrait;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TraitRegistry {

    private static final TraitRegistry INSTANCE = new TraitRegistry();
    private final Map<String, RaceTrait> traits = new ConcurrentHashMap<>();

    private TraitRegistry() {}

    public static TraitRegistry getInstance() {
        return INSTANCE;
    }

    public void register(RaceTrait trait) {
        traits.put(trait.getId().toLowerCase(), trait);
    }

    public RaceTrait getTrait(String id) {
        return id != null ? traits.get(id.toLowerCase()) : null;
    }

    public Map<String, RaceTrait> getTraits() {
        return Collections.unmodifiableMap(traits);
    }
}
