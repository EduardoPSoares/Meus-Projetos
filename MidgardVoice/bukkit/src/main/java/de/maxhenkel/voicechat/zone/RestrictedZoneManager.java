package de.maxhenkel.voicechat.zone;

import de.maxhenkel.voicechat.Voicechat;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RestrictedZoneManager {

    public static final Permission ZONE_BYPASS_PERMISSION = new Permission(Voicechat.MODID + ".zone.bypass", PermissionDefault.FALSE);
    public static final Permission ZONE_ADMIN_PERMISSION = new Permission(Voicechat.MODID + ".zone.admin", PermissionDefault.OP);

    private final Map<String, RestrictedZone> zones;
    private final File file;

    public RestrictedZoneManager() {
        this.zones = new ConcurrentHashMap<>();
        this.file = new File(Voicechat.INSTANCE.getDataFolder(), "restricted-zones.yml");
        load();
    }

    public void load() {
        zones.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("zones");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection zoneSection = section.getConfigurationSection(key);
            if (zoneSection == null) {
                continue;
            }
            String world = zoneSection.getString("world", "world");
            int x1 = zoneSection.getInt("x1");
            int y1 = zoneSection.getInt("y1");
            int z1 = zoneSection.getInt("z1");
            int x2 = zoneSection.getInt("x2");
            int y2 = zoneSection.getInt("y2");
            int z2 = zoneSection.getInt("z2");
            RestrictedZone zone = new RestrictedZone(key, world, x1, y1, z1, x2, y2, z2);
            zone.setVoiceEnabled(zoneSection.getBoolean("voiceEnabled", false));
            zone.setCustomRange((float) zoneSection.getDouble("customRange", -1));
            zone.setRangeMultiplier((float) zoneSection.getDouble("rangeMultiplier", 1.0));
            zone.setListenOnly(zoneSection.getBoolean("listenOnly", false));
            zone.setExpiresAt(zoneSection.getLong("expiresAt", -1));
            zone.setStageMode(zoneSection.getBoolean("stageMode", false));
            zone.setZoneCooldownMaxTalkTimeSec(zoneSection.getLong("cooldownMaxTalkTime", 0));
            zone.setZoneCooldownSec(zoneSection.getLong("cooldownDuration", 0));

            List<String> allowed = zoneSection.getStringList("allowedPlayers");
            for (String uuidStr : allowed) {
                try {
                    zone.addAllowedPlayer(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                }
            }

            List<String> muted = zoneSection.getStringList("mutedPlayers");
            for (String uuidStr : muted) {
                try {
                    zone.addMutedPlayer(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                }
            }

            List<String> speakersList = zoneSection.getStringList("speakers");
            for (String uuidStr : speakersList) {
                try {
                    zone.addSpeaker(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                }
            }

            zones.put(key, zone);
        }
        Voicechat.LOGGER.info("Loaded {} restricted voice zone(s)", zones.size());
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("zones");
        List<String> zoneNames = new ArrayList<>(zones.keySet());
        zoneNames.sort(String.CASE_INSENSITIVE_ORDER);
        for (String zoneName : zoneNames) {
            RestrictedZone zone = zones.get(zoneName);
            if (zone == null) {
                continue;
            }
            ConfigurationSection zoneSection = section.createSection(zoneName);
            zoneSection.set("world", zone.getWorld());
            zoneSection.set("x1", zone.getMinX());
            zoneSection.set("y1", zone.getMinY());
            zoneSection.set("z1", zone.getMinZ());
            zoneSection.set("x2", zone.getMaxX());
            zoneSection.set("y2", zone.getMaxY());
            zoneSection.set("z2", zone.getMaxZ());
            zoneSection.set("voiceEnabled", zone.isVoiceEnabled());
            zoneSection.set("customRange", (double) zone.getCustomRange());
            zoneSection.set("rangeMultiplier", (double) zone.getRangeMultiplier());
            zoneSection.set("listenOnly", zone.isListenOnly());
            zoneSection.set("expiresAt", zone.getExpiresAt());
            zoneSection.set("stageMode", zone.isStageMode());
            zoneSection.set("cooldownMaxTalkTime", zone.getZoneCooldownMaxTalkTimeSec());
            zoneSection.set("cooldownDuration", zone.getZoneCooldownSec());

            List<String> allowed = new ArrayList<>();
            for (UUID uuid : zone.getAllowedPlayers()) {
                allowed.add(uuid.toString());
            }
            allowed.sort(String.CASE_INSENSITIVE_ORDER);
            zoneSection.set("allowedPlayers", allowed);

            List<String> muted = new ArrayList<>();
            for (UUID uuid : zone.getMutedPlayers()) {
                muted.add(uuid.toString());
            }
            muted.sort(String.CASE_INSENSITIVE_ORDER);
            zoneSection.set("mutedPlayers", muted);

            List<String> speakersList = new ArrayList<>();
            for (UUID uuid : zone.getSpeakers()) {
                speakersList.add(uuid.toString());
            }
            speakersList.sort(String.CASE_INSENSITIVE_ORDER);
            zoneSection.set("speakers", speakersList);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            Voicechat.LOGGER.error("Failed to save restricted zones", e);
        }
    }

    public boolean addZone(RestrictedZone zone) {
        if (zones.containsKey(zone.getName())) {
            return false;
        }
        zones.put(zone.getName(), zone);
        save();
        return true;
    }

    public boolean removeZone(String name) {
        if (zones.remove(name) != null) {
            save();
            return true;
        }
        return false;
    }

    public Collection<RestrictedZone> getZones() {
        return zones.values();
    }

    public RestrictedZone getZone(String name) {
        return zones.get(name);
    }

    /**
     * Checks if the player is in a restricted zone and does NOT have the bypass permission.
     * Takes into account zone voice toggle, allowed players, and muted players.
     *
     * @param player the player to check
     * @return true if voice should be blocked for this player
     */
    public boolean isVoiceBlocked(Player player) {
        if (player.hasPermission(ZONE_BYPASS_PERMISSION)) {
            return false;
        }
        RestrictedZone zone = getZoneAt(player.getLocation());
        if (zone == null) {
            // No sub-zone — check global zone settings
            if (Voicechat.globalZoneSettings != null) {
                return Voicechat.globalZoneSettings.isBlocked(player.getUniqueId());
            }
            return false;
        }
        if (zone.isListenOnly()) {
            return true;
        }
        if (zone.isStageMode()) {
            return !zone.isSpeaker(player.getUniqueId());
        }
        return zone.isBlocked(player.getUniqueId());
    }

    /**
     * Returns the highest-priority zone at the given location.
     * More specific sub-regions automatically take priority over their parent zones.
     */
    public RestrictedZone getZoneAt(Location location) {
        List<RestrictedZone> matches = getZonesAt(location);
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * Returns all zones that contain the given location, sorted from most specific to least specific.
     */
    public List<RestrictedZone> getZonesAt(Location location) {
        List<RestrictedZone> result = new ArrayList<>();
        for (RestrictedZone zone : zones.values()) {
            if (zone.contains(location)) {
                result.add(zone);
            }
        }
        result.sort((a, b) -> compareZones(a, b, result));
        return result;
    }

    private int compareZones(RestrictedZone a, RestrictedZone b, Collection<RestrictedZone> candidates) {
        int depthComparison = Integer.compare(getContainmentDepth(b, candidates), getContainmentDepth(a, candidates));
        if (depthComparison != 0) {
            return depthComparison;
        }

        int volumeComparison = Long.compare(a.getVolume(), b.getVolume());
        if (volumeComparison != 0) {
            return volumeComparison;
        }

        return a.getName().compareToIgnoreCase(b.getName());
    }

    private int getContainmentDepth(RestrictedZone target, Collection<RestrictedZone> candidates) {
        int depth = 0;
        for (RestrictedZone zone : candidates) {
            if (zone != target && zone.contains(target)) {
                depth++;
            }
        }
        return depth;
    }

    public int removeExpiredZones() {
        List<String> expired = new ArrayList<>();
        for (Map.Entry<String, RestrictedZone> entry : zones.entrySet()) {
            if (entry.getValue().isExpired()) {
                expired.add(entry.getKey());
            }
        }
        for (String name : expired) {
            zones.remove(name);
        }
        if (!expired.isEmpty()) {
            save();
            Voicechat.LOGGER.info("Removed {} expired zone(s)", expired.size());
        }
        return expired.size();
    }

}
