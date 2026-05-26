package de.maxhenkel.voicechat.zone;

import de.maxhenkel.voicechat.Voicechat;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalZoneSettings {

    private final File file;
    private boolean voiceEnabled;
    private final Set<UUID> allowedPlayers;
    private final Set<UUID> mutedPlayers;
    private float rangeMultiplier;
    private boolean listenOnly;
    private boolean stageMode;
    private final Set<UUID> speakers;
    private long globalCooldownMaxTalkTimeSec;
    private long globalCooldownSec;

    public GlobalZoneSettings() {
        this.file = new File(Voicechat.INSTANCE.getDataFolder(), "global-zone.yml");
        this.voiceEnabled = true;
        this.allowedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.mutedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.rangeMultiplier = 1.0f;
        this.listenOnly = false;
        this.stageMode = false;
        this.speakers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.globalCooldownMaxTalkTimeSec = 0L;
        this.globalCooldownSec = 0L;
        load();
    }

    public void load() {
        allowedPlayers.clear();
        mutedPlayers.clear();
        speakers.clear();
        if (!file.exists()) {
            voiceEnabled = true;
            rangeMultiplier = 1.0f;
            listenOnly = false;
            stageMode = false;
            globalCooldownMaxTalkTimeSec = 0L;
            globalCooldownSec = 0L;
            Voicechat.applyGlobalCooldownSettings();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        voiceEnabled = config.getBoolean("voiceEnabled", true);
        rangeMultiplier = (float) config.getDouble("rangeMultiplier", 1.0);
        listenOnly = config.getBoolean("listenOnly", false);
        stageMode = config.getBoolean("stageMode", false);
        globalCooldownMaxTalkTimeSec = config.getLong("cooldownMaxTalkTime", 0L);
        globalCooldownSec = config.getLong("cooldownDuration", 0L);

        for (String s : config.getStringList("allowedPlayers")) {
            try { allowedPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        for (String s : config.getStringList("mutedPlayers")) {
            try { mutedPlayers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        for (String s : config.getStringList("speakers")) {
            try { speakers.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        Voicechat.applyGlobalCooldownSettings();
        Voicechat.LOGGER.info("Loaded global zone settings");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("voiceEnabled", voiceEnabled);
        config.set("rangeMultiplier", (double) rangeMultiplier);
        config.set("listenOnly", listenOnly);
        config.set("stageMode", stageMode);
        config.set("cooldownMaxTalkTime", globalCooldownMaxTalkTimeSec);
        config.set("cooldownDuration", globalCooldownSec);

        List<String> allowed = new ArrayList<>();
        for (UUID uuid : allowedPlayers) allowed.add(uuid.toString());
        allowed.sort(String.CASE_INSENSITIVE_ORDER);
        config.set("allowedPlayers", allowed);

        List<String> muted = new ArrayList<>();
        for (UUID uuid : mutedPlayers) muted.add(uuid.toString());
        muted.sort(String.CASE_INSENSITIVE_ORDER);
        config.set("mutedPlayers", muted);

        List<String> speakerList = new ArrayList<>();
        for (UUID uuid : speakers) speakerList.add(uuid.toString());
        speakerList.sort(String.CASE_INSENSITIVE_ORDER);
        config.set("speakers", speakerList);

        try {
            config.save(file);
        } catch (IOException e) {
            Voicechat.LOGGER.error("Failed to save global zone settings", e);
        }
    }

    // === Voice Enabled ===
    public boolean isVoiceEnabled() { return voiceEnabled; }
    public void setVoiceEnabled(boolean enabled) { this.voiceEnabled = enabled; }

    // === Allowed Players ===
    public Set<UUID> getAllowedPlayers() { return allowedPlayers; }
    public void addAllowedPlayer(UUID uuid) { allowedPlayers.add(uuid); }
    public void removeAllowedPlayer(UUID uuid) { allowedPlayers.remove(uuid); }
    public boolean isAllowedPlayer(UUID uuid) { return allowedPlayers.contains(uuid); }

    // === Muted Players ===
    public Set<UUID> getMutedPlayers() { return mutedPlayers; }
    public void addMutedPlayer(UUID uuid) { mutedPlayers.add(uuid); }
    public void removeMutedPlayer(UUID uuid) { mutedPlayers.remove(uuid); }
    public boolean isMutedPlayer(UUID uuid) { return mutedPlayers.contains(uuid); }

    // === Range Multiplier ===
    public float getRangeMultiplier() { return rangeMultiplier; }
    public void setRangeMultiplier(float multiplier) { this.rangeMultiplier = multiplier; }

    // === Listen Only ===
    public boolean isListenOnly() { return listenOnly; }
    public void setListenOnly(boolean listenOnly) { this.listenOnly = listenOnly; }

    // === Stage Mode ===
    public boolean isStageMode() { return stageMode; }
    public void setStageMode(boolean stageMode) { this.stageMode = stageMode; }

    // === Speakers ===
    public Set<UUID> getSpeakers() { return speakers; }
    public void addSpeaker(UUID uuid) { speakers.add(uuid); }
    public void removeSpeaker(UUID uuid) { speakers.remove(uuid); }
    public boolean isSpeaker(UUID uuid) { return speakers.contains(uuid); }

    public long getGlobalCooldownMaxTalkTimeSec() {
        return globalCooldownMaxTalkTimeSec;
    }

    public long getGlobalCooldownSec() {
        return globalCooldownSec;
    }

    public void setGlobalCooldownSettings(long maxTalkTimeSec, long cooldownSec) {
        this.globalCooldownMaxTalkTimeSec = maxTalkTimeSec;
        this.globalCooldownSec = cooldownSec;
    }

    /**
     * Checks if a player's voice should be blocked by global zone settings.
     * Called when the player is NOT inside any sub-zone.
     */
    public boolean isBlocked(UUID playerUuid) {
        if (mutedPlayers.contains(playerUuid)) {
            return true;
        }
        if (listenOnly) {
            return true;
        }
        if (stageMode) {
            return !speakers.contains(playerUuid);
        }
        if (!voiceEnabled) {
            return !allowedPlayers.contains(playerUuid);
        }
        return false;
    }
}
