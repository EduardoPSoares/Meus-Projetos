package de.maxhenkel.voicechat.cooldown;

import de.maxhenkel.voicechat.Voicechat;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceCooldownManager {

    private static final long SILENCE_RESET_MS = 750L;

    private final Map<UUID, Long> lastTransmissionEnd = new ConcurrentHashMap<>();
    private final Map<UUID, Long> continuousTransmissionStart = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPacketTime = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> notifiedCooldown = new ConcurrentHashMap<>();

    // Max continuous talk time in ms (0 = unlimited)
    private long maxTalkTimeMs = 0;
    // Cooldown after max talk time in ms
    private long cooldownMs = 0;

    public VoiceCooldownManager() {
        loadConfig();
    }

    public void loadConfig() {
        // Read from voicechat-server.properties or use defaults
        // For now, configurable via reload
        // Defaults: disabled (0 = no limit)
    }

    public void setMaxTalkTime(long seconds) {
        this.maxTalkTimeMs = seconds * 1000;
    }

    public void setSettings(long maxTalkTimeSec, long cooldownSec) {
        this.maxTalkTimeMs = maxTalkTimeSec * 1000;
        this.cooldownMs = cooldownSec * 1000;
    }

    public long getMaxTalkTimeMs() {
        return maxTalkTimeMs;
    }

    public void setCooldown(long seconds) {
        this.cooldownMs = seconds * 1000;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public boolean isEnabled() {
        return maxTalkTimeMs > 0 && cooldownMs > 0;
    }

    public void recordTransmission(UUID playerUuid) {
        if (!isEnabled()) return;

        long now = System.currentTimeMillis();
        Long lastPacket = lastPacketTime.get(playerUuid);
        if (lastPacket == null || now - lastPacket > SILENCE_RESET_MS) {
            continuousTransmissionStart.put(playerUuid, now);
        } else {
            continuousTransmissionStart.putIfAbsent(playerUuid, now);
        }
        lastPacketTime.put(playerUuid, now);
    }

    public boolean isOnCooldown(UUID playerUuid) {
        if (!isEnabled()) return false;

        long now = System.currentTimeMillis();

        // Check if player is in cooldown period
        Long lastEnd = lastTransmissionEnd.get(playerUuid);
        if (lastEnd != null) {
            long elapsed = now - lastEnd;
            if (elapsed < cooldownMs) {
                // Still on cooldown - notify player
                if (!Boolean.TRUE.equals(notifiedCooldown.get(playerUuid))) {
                    notifiedCooldown.put(playerUuid, true);
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player != null) {
                        long remaining = (cooldownMs - elapsed) / 1000;
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.RED + String.format(Voicechat.MESSAGES.cooldown_ativo, remaining)));
                    }
                }
                return true;
            } else {
                // Cooldown expired
                lastTransmissionEnd.remove(playerUuid);
                notifiedCooldown.remove(playerUuid);
            }
        }

        Long lastPacket = lastPacketTime.get(playerUuid);
        if (lastPacket != null && now - lastPacket > SILENCE_RESET_MS) {
            continuousTransmissionStart.remove(playerUuid);
            lastPacketTime.remove(playerUuid);
        }

        // Check if player exceeded max talk time
        Long startTime = continuousTransmissionStart.get(playerUuid);
        if (startTime != null) {
            long talkDuration = now - startTime;
            if (talkDuration >= maxTalkTimeMs) {
                // Exceeded max talk time - start cooldown
                lastTransmissionEnd.put(playerUuid, now);
                continuousTransmissionStart.remove(playerUuid);
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatColor.RED + Voicechat.MESSAGES.cooldown_iniciado));
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Called when a player stops transmitting (silence detected).
     * Resets the continuous transmission timer.
     */
    public void resetTransmission(UUID playerUuid) {
        continuousTransmissionStart.remove(playerUuid);
        lastPacketTime.remove(playerUuid);
    }

    /**
     * Get remaining cooldown in seconds, or 0 if not on cooldown.
     */
    public long getRemainingCooldown(UUID playerUuid) {
        if (!isEnabled()) return 0;
        Long lastEnd = lastTransmissionEnd.get(playerUuid);
        if (lastEnd == null) return 0;
        long remaining = cooldownMs - (System.currentTimeMillis() - lastEnd);
        return remaining > 0 ? remaining / 1000 : 0;
    }

    /**
     * Get remaining talk time in seconds, or -1 if unlimited.
     */
    public long getRemainingTalkTime(UUID playerUuid) {
        if (!isEnabled()) return -1;
        Long lastPacket = lastPacketTime.get(playerUuid);
        if (lastPacket == null || System.currentTimeMillis() - lastPacket > SILENCE_RESET_MS) {
            return maxTalkTimeMs / 1000;
        }
        Long startTime = continuousTransmissionStart.get(playerUuid);
        if (startTime == null) return maxTalkTimeMs / 1000;
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = maxTalkTimeMs - elapsed;
        return remaining > 0 ? remaining / 1000 : 0;
    }

    public void clearPlayer(UUID playerUuid) {
        lastTransmissionEnd.remove(playerUuid);
        continuousTransmissionStart.remove(playerUuid);
        lastPacketTime.remove(playerUuid);
        notifiedCooldown.remove(playerUuid);
    }
}
