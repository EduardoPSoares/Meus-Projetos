package de.maxhenkel.voicechat.zone;

import de.maxhenkel.voicechat.Voicechat;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneCooldownTracker {

    private static final long SILENCE_RESET_MS = 750L;

    // Key format: "zoneName:playerUUID"
    private final Map<String, Long> lastTransmissionEnd = new ConcurrentHashMap<>();
    private final Map<String, Long> continuousTransmissionStart = new ConcurrentHashMap<>();
    private final Map<String, Long> lastPacketTime = new ConcurrentHashMap<>();
    private final Map<String, Boolean> notifiedCooldown = new ConcurrentHashMap<>();

    private static String key(String zoneName, UUID playerUuid) {
        return zoneName + ":" + playerUuid.toString();
    }

    public void recordTransmission(RestrictedZone zone, UUID playerUuid) {
        if (!zone.hasZoneCooldown()) return;
        String k = key(zone.getName(), playerUuid);
        long now = System.currentTimeMillis();
        Long lastPacket = lastPacketTime.get(k);
        if (lastPacket == null || now - lastPacket > SILENCE_RESET_MS) {
            continuousTransmissionStart.put(k, now);
        } else {
            continuousTransmissionStart.putIfAbsent(k, now);
        }
        lastPacketTime.put(k, now);
    }

    public boolean isOnCooldown(RestrictedZone zone, UUID playerUuid) {
        if (!zone.hasZoneCooldown()) return false;

        String k = key(zone.getName(), playerUuid);
        long now = System.currentTimeMillis();
        long cooldownMs = zone.getZoneCooldownSec() * 1000;
        long maxTalkMs = zone.getZoneCooldownMaxTalkTimeSec() * 1000;

        // Check if player is in cooldown period
        Long lastEnd = lastTransmissionEnd.get(k);
        if (lastEnd != null) {
            long elapsed = now - lastEnd;
            if (elapsed < cooldownMs) {
                if (!Boolean.TRUE.equals(notifiedCooldown.get(k))) {
                    notifiedCooldown.put(k, true);
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player != null) {
                        long remaining = (cooldownMs - elapsed) / 1000;
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent(ChatColor.RED + String.format(Voicechat.MESSAGES.zona_cooldown_ativo, zone.getName(), remaining)));
                    }
                }
                return true;
            } else {
                lastTransmissionEnd.remove(k);
                notifiedCooldown.remove(k);
            }
        }

        Long lastPacket = lastPacketTime.get(k);
        if (lastPacket != null && now - lastPacket > SILENCE_RESET_MS) {
            continuousTransmissionStart.remove(k);
            lastPacketTime.remove(k);
        }

        // Check if player exceeded max talk time
        Long startTime = continuousTransmissionStart.get(k);
        if (startTime != null) {
            long talkDuration = now - startTime;
            if (talkDuration >= maxTalkMs) {
                lastTransmissionEnd.put(k, now);
                continuousTransmissionStart.remove(k);
                Player player = Bukkit.getPlayer(playerUuid);
                if (player != null) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatColor.RED + String.format(Voicechat.MESSAGES.zona_cooldown_iniciado, zone.getName())));
                }
                return true;
            }
        }

        return false;
    }

    public void resetTransmission(String zoneName, UUID playerUuid) {
        String k = key(zoneName, playerUuid);
        continuousTransmissionStart.remove(k);
        lastPacketTime.remove(k);
    }

    /**
     * Cleans up all tracking data for a specific zone (e.g., when zone is deleted).
     */
    public void clearZone(String zoneName) {
        String prefix = zoneName + ":";
        lastTransmissionEnd.keySet().removeIf(k -> k.startsWith(prefix));
        continuousTransmissionStart.keySet().removeIf(k -> k.startsWith(prefix));
        lastPacketTime.keySet().removeIf(k -> k.startsWith(prefix));
        notifiedCooldown.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void clearPlayer(UUID playerUuid) {
        String suffix = ":" + playerUuid.toString();
        lastTransmissionEnd.keySet().removeIf(k -> k.endsWith(suffix));
        continuousTransmissionStart.keySet().removeIf(k -> k.endsWith(suffix));
        lastPacketTime.keySet().removeIf(k -> k.endsWith(suffix));
        notifiedCooldown.keySet().removeIf(k -> k.endsWith(suffix));
    }
}
