package midgardvanish.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ViewerDataManager {

    private final File file;
    private FileConfiguration config;
    // Map: vanished player UUID -> set of viewer UUIDs that can see them
    private final Map<UUID, Set<UUID>> viewerMap = new ConcurrentHashMap<>();

    public ViewerDataManager(JavaPlugin plugin) {
        file = new File(plugin.getDataFolder(), "viewers.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        viewerMap.clear();
        ConfigurationSection section = config.getConfigurationSection("viewers");
        if (section != null) {
            for (String vanishedKey : section.getKeys(false)) {
                try {
                    UUID vanishedUUID = UUID.fromString(vanishedKey);
                    Set<UUID> viewers = ConcurrentHashMap.newKeySet();
                    for (String viewerStr : section.getStringList(vanishedKey)) {
                        try {
                            viewers.add(UUID.fromString(viewerStr));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    if (!viewers.isEmpty()) {
                        viewerMap.put(vanishedUUID, viewers);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        config.set("viewers", null);
        for (Map.Entry<UUID, Set<UUID>> entry : viewerMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                config.set("viewers." + entry.getKey().toString(),
                        entry.getValue().stream().map(UUID::toString).toList());
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if viewer can see a specific vanished player.
     */
    public boolean isViewer(UUID viewerUUID, UUID vanishedUUID) {
        Set<UUID> viewers = viewerMap.get(vanishedUUID);
        return viewers != null && viewers.contains(viewerUUID);
    }

    /**
     * Check if viewer can see any vanished player.
     */
    public boolean canSeeAny(UUID viewerUUID) {
        for (Set<UUID> viewers : viewerMap.values()) {
            if (viewers.contains(viewerUUID)) return true;
        }
        return false;
    }

    public void addViewer(UUID vanishedUUID, UUID viewerUUID) {
        viewerMap.computeIfAbsent(vanishedUUID, k -> ConcurrentHashMap.newKeySet()).add(viewerUUID);
        save();
    }

    public void removeViewer(UUID vanishedUUID, UUID viewerUUID) {
        Set<UUID> viewers = viewerMap.get(vanishedUUID);
        if (viewers != null) {
            viewers.remove(viewerUUID);
            if (viewers.isEmpty()) {
                viewerMap.remove(vanishedUUID);
            }
        }
        save();
    }

    /**
     * Get all viewers for a specific vanished player.
     */
    public Set<UUID> getViewers(UUID vanishedUUID) {
        Set<UUID> viewers = viewerMap.get(vanishedUUID);
        return viewers != null ? Set.copyOf(viewers) : Set.of();
    }
}
