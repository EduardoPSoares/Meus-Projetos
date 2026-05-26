package de.maxhenkel.voicechat.zone;

import de.maxhenkel.voicechat.Voicechat;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class ZoneNotificationListener implements Listener {

    private final Map<UUID, String> playerCurrentZone = new ConcurrentHashMap<>();

    private int expirationCheckCounter = 0;

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkPlayerZone(player);
                }
                expirationCheckCounter++;
                if (expirationCheckCounter >= 60) {
                    expirationCheckCounter = 0;
                    Voicechat.restrictedZoneManager.removeExpiredZones();
                }
            }
        }.runTaskTimer(Voicechat.INSTANCE, 20L, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerCurrentZone.remove(uuid);
        if (Voicechat.zoneCooldownTracker != null) {
            Voicechat.zoneCooldownTracker.clearPlayer(uuid);
        }
        if (Voicechat.voiceCooldownManager != null) {
            Voicechat.voiceCooldownManager.clearPlayer(uuid);
        }
    }

    private void checkPlayerZone(Player player) {
        RestrictedZone currentZoneObj = Voicechat.restrictedZoneManager.getZoneAt(player.getLocation());
        String currentZone = currentZoneObj != null ? currentZoneObj.getName() : null;

        String previousZone = playerCurrentZone.get(player.getUniqueId());

        // Reset zone cooldown when leaving a zone
        if (previousZone != null && !previousZone.equals(currentZone)) {
            if (Voicechat.zoneCooldownTracker != null) {
                Voicechat.zoneCooldownTracker.resetTransmission(previousZone, player.getUniqueId());
            }
        }

        // Track current zone
        if (currentZone != null) {
            playerCurrentZone.put(player.getUniqueId(), currentZone);
        } else {
            playerCurrentZone.remove(player.getUniqueId());
        }

        // Global players bypass zone restrictions — never show blocked message
        boolean isGlobal = Voicechat.playerRangeManager != null
                && Voicechat.playerRangeManager.isGlobalPlayer(player.getUniqueId());
        if (isGlobal) {
            return;
        }

        // Only show action bar when the player CANNOT speak
        boolean blocked = Voicechat.restrictedZoneManager.isVoiceBlocked(player);
        if (blocked) {
            String reason = getBlockReason(player, currentZoneObj);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(reason));
        }
    }

    private String getBlockReason(Player player, RestrictedZone zone) {
        UUID uuid = player.getUniqueId();
        if (zone != null) {
            String name = zone.getName();
            if (zone.isStageMode() && !zone.isSpeaker(uuid)) {
                return Voicechat.MESSAGES.format(
                        "zone.block_reason.stage",
                        "&cX &7%s &8| &dStage mode &c(ouvinte)",
                        name
                );
            }
            if (zone.isListenOnly()) {
                return Voicechat.MESSAGES.format(
                        "zone.block_reason.listen_only",
                        "&cX &7%s &8| &bSomente escuta",
                        name
                );
            }
            if (zone.isMutedPlayer(uuid)) {
                return Voicechat.MESSAGES.format(
                        "zone.block_reason.muted",
                        "&cX &7%s &8| &cVoce esta mutado",
                        name
                );
            }
            if (!zone.isVoiceEnabled()) {
                return Voicechat.MESSAGES.format(
                        "zone.block_reason.voice_disabled",
                        "&cX &7%s &8| &cVoz desativada",
                        name
                );
            }
        } else if (Voicechat.globalZoneSettings != null) {
            GlobalZoneSettings g = Voicechat.globalZoneSettings;
            if (g.isStageMode() && !g.isSpeaker(uuid)) {
                return Voicechat.MESSAGES.text(
                        "zone.block_reason.global_stage",
                        "&cX &dStage mode global &c(ouvinte)"
                );
            }
            if (g.isListenOnly()) {
                return Voicechat.MESSAGES.text(
                        "zone.block_reason.global_listen_only",
                        "&cX &bSomente escuta (global)"
                );
            }
            if (g.isMutedPlayer(uuid)) {
                return Voicechat.MESSAGES.text(
                        "zone.block_reason.global_muted",
                        "&cX &cVoce esta mutado (global)"
                );
            }
            if (!g.isVoiceEnabled()) {
                return Voicechat.MESSAGES.text(
                        "zone.block_reason.global_voice_disabled",
                        "&cX &cVoz desativada (global)"
                );
            }
        }
        return Voicechat.MESSAGES.text("zone.block_reason.default", "&cX &cVoz bloqueada");
    }

    public String getCurrentZone(UUID playerUuid) {
        return playerCurrentZone.get(playerUuid);
    }

}
