package de.maxhenkel.voicechat.range;

import de.maxhenkel.voicechat.Voicechat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRangeManager {

    private final Map<UUID, Float> playerRanges;
    private final Map<UUID, Float> playerVolumes;
    private final Map<UUID, Integer> playerPriorities;
    private final Set<UUID> globalPlayers;
    private final File file;
    private int maxGlobalPlayers = -1;

    public PlayerRangeManager() {
        this.playerRanges = new ConcurrentHashMap<>();
        this.playerVolumes = new ConcurrentHashMap<>();
        this.playerPriorities = new ConcurrentHashMap<>();
        this.globalPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.file = new File(Voicechat.INSTANCE.getDataFolder(), "player-ranges.yml");
        load();
    }

    public void load() {
        playerRanges.clear();
        playerVolumes.clear();
        playerPriorities.clear();
        globalPlayers.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("ranges");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    float range = (float) section.getDouble(key);
                    if (range > 0) {
                        playerRanges.put(uuid, range);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        ConfigurationSection volumeSection = config.getConfigurationSection("volumes");
        if (volumeSection != null) {
            for (String key : volumeSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    float volume = (float) volumeSection.getDouble(key);
                    if (volume > 0) {
                        playerVolumes.put(uuid, volume);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        ConfigurationSection prioritySection = config.getConfigurationSection("priorities");
        if (prioritySection != null) {
            for (String key : prioritySection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int priority = prioritySection.getInt(key);
                    if (priority != 0) {
                        playerPriorities.put(uuid, priority);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        List<String> globalList = config.getStringList("global");
        for (String uuidStr : globalList) {
            try {
                globalPlayers.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
            }
        }
        maxGlobalPlayers = config.getInt("max-global-players", -1);
        Voicechat.LOGGER.info("Loaded {} range(s), {} volume(s), {} priority(ies), {} global(s)", playerRanges.size(), playerVolumes.size(), playerPriorities.size(), globalPlayers.size());
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("ranges");
        List<UUID> sortedRangePlayers = new ArrayList<>(playerRanges.keySet());
        sortedRangePlayers.sort(Comparator.comparing(UUID::toString));
        for (UUID uuid : sortedRangePlayers) {
            section.set(uuid.toString(), (double) playerRanges.get(uuid));
        }
        ConfigurationSection volumeSection = config.createSection("volumes");
        List<UUID> sortedVolumePlayers = new ArrayList<>(playerVolumes.keySet());
        sortedVolumePlayers.sort(Comparator.comparing(UUID::toString));
        for (UUID uuid : sortedVolumePlayers) {
            volumeSection.set(uuid.toString(), (double) playerVolumes.get(uuid));
        }
        ConfigurationSection prioritySection = config.createSection("priorities");
        List<UUID> sortedPriorityPlayers = new ArrayList<>(playerPriorities.keySet());
        sortedPriorityPlayers.sort(Comparator.comparing(UUID::toString));
        for (UUID uuid : sortedPriorityPlayers) {
            prioritySection.set(uuid.toString(), playerPriorities.get(uuid));
        }
        List<String> globalList = new ArrayList<>();
        for (UUID uuid : globalPlayers) {
            globalList.add(uuid.toString());
        }
        globalList.sort(String.CASE_INSENSITIVE_ORDER);
        config.set("global", globalList);
        config.set("max-global-players", maxGlobalPlayers);
        try {
            config.save(file);
        } catch (IOException e) {
            Voicechat.LOGGER.error("Failed to save player ranges", e);
        }
    }

    public void setRange(UUID playerUuid, float range) {
        playerRanges.put(playerUuid, range);
        save();
    }

    public boolean removeRange(UUID playerUuid) {
        if (playerRanges.remove(playerUuid) != null) {
            save();
            return true;
        }
        return false;
    }

    public Float getRange(UUID playerUuid) {
        return playerRanges.get(playerUuid);
    }

    public boolean hasCustomRange(UUID playerUuid) {
        return playerRanges.containsKey(playerUuid);
    }

    public Map<UUID, Float> getAllRanges() {
        return Collections.unmodifiableMap(playerRanges);
    }

    // === Global Players ===

    public boolean addGlobalPlayer(UUID playerUuid) {
        if (globalPlayers.add(playerUuid)) {
            save();
            return true;
        }
        return false;
    }

    public boolean removeGlobalPlayer(UUID playerUuid) {
        if (globalPlayers.remove(playerUuid)) {
            save();
            return true;
        }
        return false;
    }

    public boolean isGlobalPlayer(UUID playerUuid) {
        return globalPlayers.contains(playerUuid);
    }

    public Set<UUID> getGlobalPlayers() {
        return Collections.unmodifiableSet(globalPlayers);
    }

    public int getMaxGlobalPlayers() {
        return maxGlobalPlayers;
    }

    public boolean isGlobalLimitReached() {
        return maxGlobalPlayers > 0 && globalPlayers.size() >= maxGlobalPlayers;
    }

    // === Player Volume ===

    public void setVolume(UUID playerUuid, float volume) {
        playerVolumes.put(playerUuid, volume);
        save();
    }

    public boolean removeVolume(UUID playerUuid) {
        if (playerVolumes.remove(playerUuid) != null) {
            save();
            return true;
        }
        return false;
    }

    public Float getVolume(UUID playerUuid) {
        return playerVolumes.get(playerUuid);
    }

    public boolean hasCustomVolume(UUID playerUuid) {
        return playerVolumes.containsKey(playerUuid);
    }

    public Map<UUID, Float> getAllVolumes() {
        return Collections.unmodifiableMap(playerVolumes);
    }

    // === Audio Priority ===

    public void setPriority(UUID playerUuid, int priority) {
        if (priority == 0) {
            playerPriorities.remove(playerUuid);
        } else {
            playerPriorities.put(playerUuid, priority);
        }
        save();
    }

    public int getPriority(UUID playerUuid) {
        return playerPriorities.getOrDefault(playerUuid, 0);
    }

    public boolean removePriority(UUID playerUuid) {
        if (playerPriorities.remove(playerUuid) != null) {
            save();
            return true;
        }
        return false;
    }

    public Map<UUID, Integer> getAllPriorities() {
        return Collections.unmodifiableMap(playerPriorities);
    }

}
