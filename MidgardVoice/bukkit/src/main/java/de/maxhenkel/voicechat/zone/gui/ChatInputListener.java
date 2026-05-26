package de.maxhenkel.voicechat.zone.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.zone.RestrictedZone;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatInputListener implements Listener {

    private static final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

    public static void awaitZoneCooldownInput(UUID playerUuid, String zoneName) {
        pendingInputs.put(playerUuid, new PendingInput(zoneName));
    }

    public static boolean hasPendingInput(UUID playerUuid) {
        return pendingInputs.containsKey(playerUuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        PendingInput pending = pendingInputs.remove(event.getPlayer().getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();
        Player player = event.getPlayer();

        // Process on main thread
        Bukkit.getScheduler().runTask(Voicechat.INSTANCE, () -> {
            if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel")) {
                player.sendMessage(Voicechat.MESSAGES.text("gui.zone.chat_input.cancelled", "&eOperacao cancelada."));
                RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(pending.zoneName);
                if (zone != null) {
                    ZoneSettingsMenu.open(player, zone);
                }
                return;
            }

            if (msg.equals("0")) {
                RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(pending.zoneName);
                if (zone != null) {
                    zone.setZoneCooldownMaxTalkTimeSec(0);
                    zone.setZoneCooldownSec(0);
                    Voicechat.restrictedZoneManager.save();
                    player.sendMessage(String.format(
                            Voicechat.MESSAGES.text("gui.zone.chat_input.cooldown_disabled", "&aCooldown da zona '%s' desativado."),
                            pending.zoneName
                    ));
                    Voicechat.activityLogger.log(player.getName() + " desativou cooldown da zona " + pending.zoneName);
                    ZoneSettingsMenu.open(player, zone);
                }
                return;
            }

            String[] parts = msg.split("\\s+");
            if (parts.length != 2) {
                for (String line : Voicechat.MESSAGES.textList(
                        "gui.zone.chat_input.invalid_format_lines",
                        "&cFormato invalido! Use: <tempo_fala> <cooldown>",
                        "&cExemplo: 45 15 (45s fala, 15s espera)",
                        "&cOu digite 'cancelar' para voltar.")) {
                    player.sendMessage(line);
                }
                pendingInputs.put(player.getUniqueId(), pending);
                return;
            }

            try {
                long talkTime = Long.parseLong(parts[0]);
                long cooldown = Long.parseLong(parts[1]);
                if (talkTime <= 0 || cooldown <= 0) {
                    player.sendMessage(Voicechat.MESSAGES.text("gui.zone.chat_input.positive_values", "&cValores devem ser positivos! Use 0 para desativar."));
                    pendingInputs.put(player.getUniqueId(), pending);
                    return;
                }
                if (talkTime > 3600 || cooldown > 3600) {
                    player.sendMessage(Voicechat.MESSAGES.text("gui.zone.chat_input.max_values", "&cValores maximos: 3600 segundos (1 hora)."));
                    pendingInputs.put(player.getUniqueId(), pending);
                    return;
                }

                RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(pending.zoneName);
                if (zone != null) {
                    zone.setZoneCooldownMaxTalkTimeSec(talkTime);
                    zone.setZoneCooldownSec(cooldown);
                    Voicechat.restrictedZoneManager.save();
                    player.sendMessage(String.format(
                            Voicechat.MESSAGES.text("gui.zone.chat_input.cooldown_defined", "&aCooldown da zona '%s' definido: fala=%ss, espera=%ss"),
                            pending.zoneName,
                            talkTime,
                            cooldown
                    ));
                    Voicechat.activityLogger.log(player.getName() + " definiu cooldown custom da zona " + pending.zoneName + ": fala=" + talkTime + "s, espera=" + cooldown + "s");
                    ZoneSettingsMenu.open(player, zone);
                } else {
                    player.sendMessage(String.format(
                            Voicechat.MESSAGES.text("gui.zone.chat_input.zone_not_found", "&cZona '%s' nao encontrada."),
                            pending.zoneName
                    ));
                }
            } catch (NumberFormatException e) {
                for (String line : Voicechat.MESSAGES.textList(
                        "gui.zone.chat_input.invalid_numbers_lines",
                        "&cValores invalidos! Use apenas numeros.",
                        "&cExemplo: 45 15")) {
                    player.sendMessage(line);
                }
                pendingInputs.put(player.getUniqueId(), pending);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    private static class PendingInput {
        final String zoneName;

        PendingInput(String zoneName) {
            this.zoneName = zoneName;
        }
    }
}
