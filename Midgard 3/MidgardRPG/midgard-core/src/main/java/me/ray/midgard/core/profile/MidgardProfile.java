package me.ray.midgard.core.profile;

import com.google.gson.JsonElement; // Import added
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representa o perfil de um jogador no MidgardRPG.
 * Armazena dados de todos os módulos de forma thread-safe.
 */
public class MidgardProfile {

    private final UUID uuid;
    private final String name;
    private final Map<Class<? extends ModuleData>, ModuleData> moduleData = new ConcurrentHashMap<>();
    private final Map<String, JsonElement> unknownData = new ConcurrentHashMap<>();

    public MidgardProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
    
    // ...existing code...

    public Map<String, JsonElement> getUnknownData() {
        return java.util.Collections.unmodifiableMap(unknownData);
    }

    public void addUnknownData(String className, JsonElement json) {
        this.unknownData.put(className, json);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isOnline() {
        return getPlayer() != null;
    }

    public <T extends ModuleData> T getData(Class<T> clazz) {
        return clazz.cast(moduleData.get(clazz));
    }

    public <T extends ModuleData> T getOrCreateData(Class<T> clazz) {
        return clazz.cast(moduleData.computeIfAbsent(clazz, c -> {
            try {
                return c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate module data: " + c.getName(), e);
            }
        }));
    }

    public void setData(ModuleData data) {
        moduleData.put(data.getClass(), data);
    }
    
    public boolean hasData(Class<? extends ModuleData> clazz) {
        return moduleData.containsKey(clazz);
    }

    /**
     * Returns a snapshot of the module data map for serialization.
     * Thread-safe: returns a copy to avoid ConcurrentModificationException.
     */
    public Map<Class<? extends ModuleData>, ModuleData> getModuleDataSnapshot() {
        return new java.util.HashMap<>(moduleData);
    }
}
