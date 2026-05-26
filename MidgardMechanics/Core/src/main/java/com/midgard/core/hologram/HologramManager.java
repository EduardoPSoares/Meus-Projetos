package com.midgard.core.hologram;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages named holograms.
 */
public class HologramManager {

    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();

    public void register(String id, Hologram hologram) {
        Hologram existing = holograms.put(id, hologram);
        if (existing != null) {
            existing.destroy();
        }
    }

    public Hologram get(String id) {
        return holograms.get(id);
    }

    public void remove(String id) {
        Hologram hologram = holograms.remove(id);
        if (hologram != null) {
            hologram.destroy();
        }
    }

    public void destroyAll() {
        holograms.values().forEach(Hologram::destroy);
        holograms.clear();
    }

    public Map<String, Hologram> getAll() {
        return Collections.unmodifiableMap(holograms);
    }
}
