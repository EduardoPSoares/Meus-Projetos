package de.maxhenkel.voicechat.zone;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RestrictedZone {

    private final String name;
    private final String world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private boolean voiceEnabled;
    private final Set<UUID> allowedPlayers;
    private final Set<UUID> mutedPlayers;
    private float customRange = -1;
    private float rangeMultiplier = 1.0f;
    private boolean listenOnly = false;
    private long expiresAt = -1;
    private boolean stageMode = false;
    private final Set<UUID> speakers = new HashSet<>();
    private long zoneCooldownMaxTalkTimeSec = 0; // 0 = disabled
    private long zoneCooldownSec = 0;

    public RestrictedZone(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.voiceEnabled = false;
        this.allowedPlayers = new HashSet<>();
        this.mutedPlayers = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public boolean isVoiceEnabled() {
        return voiceEnabled;
    }

    public void setVoiceEnabled(boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
    }

    public Set<UUID> getAllowedPlayers() {
        return allowedPlayers;
    }

    public void addAllowedPlayer(UUID uuid) {
        allowedPlayers.add(uuid);
    }

    public void removeAllowedPlayer(UUID uuid) {
        allowedPlayers.remove(uuid);
    }

    public boolean isAllowedPlayer(UUID uuid) {
        return allowedPlayers.contains(uuid);
    }

    public Set<UUID> getMutedPlayers() {
        return mutedPlayers;
    }

    public void addMutedPlayer(UUID uuid) {
        mutedPlayers.add(uuid);
    }

    public void removeMutedPlayer(UUID uuid) {
        mutedPlayers.remove(uuid);
    }

    public boolean isMutedPlayer(UUID uuid) {
        return mutedPlayers.contains(uuid);
    }

    public boolean contains(Location location) {
        World w = location.getWorld();
        if (w == null || !w.getName().equals(world)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean contains(RestrictedZone other) {
        if (other == null || !world.equals(other.world)) {
            return false;
        }
        return minX <= other.minX && maxX >= other.maxX
                && minY <= other.minY && maxY >= other.maxY
                && minZ <= other.minZ && maxZ >= other.maxZ;
    }

    /**
     * Checks if a player's voice should be blocked in this zone.
     * - If stage mode is on, only speakers can talk.
     * - If the player is individually muted, always blocked.
     * - If voice is enabled, not blocked (unless individually muted).
     * - If voice is disabled, blocked unless the player is in the allowed list.
     */
    public boolean isBlocked(UUID playerUuid) {
        if (mutedPlayers.contains(playerUuid)) {
            return true;
        }
        if (stageMode) {
            return !speakers.contains(playerUuid);
        }
        if (voiceEnabled) {
            return false;
        }
        return !allowedPlayers.contains(playerUuid);
    }

    // === Custom Range ===

    public float getCustomRange() {
        return customRange;
    }

    public void setCustomRange(float customRange) {
        this.customRange = customRange;
    }

    public boolean hasCustomRange() {
        return customRange > 0;
    }

    // === Range Multiplier (amplification) ===

    public float getRangeMultiplier() {
        return rangeMultiplier;
    }

    public void setRangeMultiplier(float rangeMultiplier) {
        this.rangeMultiplier = rangeMultiplier;
    }

    // === Listen Only ===

    public boolean isListenOnly() {
        return listenOnly;
    }

    public void setListenOnly(boolean listenOnly) {
        this.listenOnly = listenOnly;
    }

    // === Temporary Zone ===

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isTemporary() {
        return expiresAt > 0;
    }

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() >= expiresAt;
    }

    // === Stage Mode ===

    public boolean isStageMode() {
        return stageMode;
    }

    public void setStageMode(boolean stageMode) {
        this.stageMode = stageMode;
    }

    public Set<UUID> getSpeakers() {
        return speakers;
    }

    public void addSpeaker(UUID uuid) {
        speakers.add(uuid);
    }

    public void removeSpeaker(UUID uuid) {
        speakers.remove(uuid);
    }

    public boolean isSpeaker(UUID uuid) {
        return speakers.contains(uuid);
    }

    /**
     * Returns the volume (number of blocks) of this zone.
     */
    public long getVolume() {
        return (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    // === Zone Cooldown ===

    public long getZoneCooldownMaxTalkTimeSec() {
        return zoneCooldownMaxTalkTimeSec;
    }

    public void setZoneCooldownMaxTalkTimeSec(long seconds) {
        this.zoneCooldownMaxTalkTimeSec = seconds;
    }

    public long getZoneCooldownSec() {
        return zoneCooldownSec;
    }

    public void setZoneCooldownSec(long seconds) {
        this.zoneCooldownSec = seconds;
    }

    public boolean hasZoneCooldown() {
        return zoneCooldownMaxTalkTimeSec > 0 && zoneCooldownSec > 0;
    }

}
