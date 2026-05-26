package de.maxhenkel.voicechat.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.recording.VoiceRecording;
import de.maxhenkel.voicechat.util.MessageFormatUtil;
import de.maxhenkel.voicechat.zone.RestrictedZone;
import de.maxhenkel.voicechat.zone.gui.ZoneListMenu;
import de.maxhenkel.voicechat.zone.gui.ZoneSettingsMenu;
import de.maxhenkel.voicechat.range.gui.RangeGlobalMenu;
import de.maxhenkel.voicechat.range.gui.RangeListMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.UUID;

public class AdminHubListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (title.equals(AdminHubMenu.getTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleHubClick(player, event.getSlot());
        } else if (title.equals(RecordingMenu.getTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleRecordingClick(player, clicked, event.getSlot());
        } else if (title.equals(RecordingMenu.getSavedTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleSavedRecordingClick(player, clicked, event.getSlot(), event.getClick());
        } else if (title.equals(RecordingMenu.getStartTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleStartRecordingClick(player, clicked, event.getSlot());
        } else if (title.equals(VolumePriorityMenu.getTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleVolumePriorityClick(player, clicked, event.getSlot(), event.getClick());
        } else if (title.startsWith(VolumePriorityMenu.getVolumeSelectTitlePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleVolumeSelectClick(player, event.getSlot(), clicked);
        } else if (title.startsWith(VolumePriorityMenu.getPrioritySelectTitlePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handlePrioritySelectClick(player, event.getSlot(), clicked);
        } else if (title.equals(VolumePriorityMenu.getSelectVolumeTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleSelectPlayerForVolume(player, clicked, event.getSlot());
        } else if (title.equals(VolumePriorityMenu.getSelectPriorityTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleSelectPlayerForPriority(player, clicked, event.getSlot());
        } else if (title.equals(CooldownMenu.getTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleCooldownClick(player, event.getSlot());
        } else if (title.startsWith(StageSpeakersMenu.getTitlePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            String zoneName = title.substring(StageSpeakersMenu.getTitlePrefix().length());
            handleSpeakersClick(player, zoneName, clicked, event.getSlot());
        } else if (title.startsWith(StageSpeakersMenu.getAddTitlePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            String zoneName = title.substring(StageSpeakersMenu.getAddTitlePrefix().length());
            handleAddSpeakerClick(player, zoneName, clicked, event.getSlot());
        }
    }

    private boolean isDecoration(ItemStack item) {
        Material type = item.getType();
        return type == Material.PURPLE_STAINED_GLASS_PANE
                || type == Material.GRAY_STAINED_GLASS_PANE
                || type == Material.NETHER_STAR;
    }

    // === Admin Hub ===
    private void handleHubClick(Player player, int slot) {
        switch (slot) {
            case 10: // Zonas
                ZoneListMenu.open(player);
                break;
            case 12: // Range
                RangeListMenu.open(player);
                break;
            case 14: // Volume & Prioridade
                VolumePriorityMenu.open(player);
                break;
            case 16: // Voz Global
                RangeGlobalMenu.open(player);
                break;
            case 28: // Gravacoes
                RecordingMenu.open(player);
                break;
            case 30: // Cooldown
                CooldownMenu.open(player);
                break;
            case 32: // Particulas
                if (Voicechat.zoneParticleVisualizer != null) {
                    Voicechat.zoneParticleVisualizer.toggleAll(player);
                    boolean viewing = Voicechat.zoneParticleVisualizer.isViewing(player.getUniqueId());
                    player.sendMessage(Voicechat.MESSAGES.text(
                            viewing ? "gui.admin.particles.enabled" : "gui.admin.particles.disabled",
                            viewing
                                    ? "&d[MidgardVoice] &aVisualizacao de zonas ATIVADA"
                                    : "&d[MidgardVoice] &cVisualizacao de zonas DESATIVADA"
                    ));
                    Voicechat.activityLogger.log(player.getName() + (viewing ? " ativou" : " desativou") + " visualizacao de particulas");
                }
                AdminHubMenu.open(player);
                break;
            case 34: // Recarregar tudo
                try {
                    Voicechat.ReloadResult result = Voicechat.reloadRuntimeState();
                    if (result.isPortChanged()) {
                        player.sendMessage(Voicechat.MESSAGES.format(
                                "gui.admin.reload.success_port",
                                "&aTodas as configuracoes foram recarregadas. Porta: &f%s&7 -> &f%s",
                                result.getOldPort(),
                                result.getNewPort()
                        ));
                    } else {
                        player.sendMessage(Voicechat.MESSAGES.text(
                                "gui.admin.reload.success",
                                "&aTodas as configuracoes foram recarregadas!"
                        ));
                    }
                    Voicechat.activityLogger.log(player.getName() + " recarregou todas as configuracoes via painel admin");
                } catch (Exception e) {
                    player.sendMessage(Voicechat.MESSAGES.format(
                            "gui.admin.reload.error",
                            "&cFalha ao recarregar o plugin: %s",
                            e.getMessage()
                    ));
                    Voicechat.LOGGER.error("Failed to reload voice chat runtime from admin menu", e);
                }
                AdminHubMenu.open(player);
                break;
        }
    }

    // === Recording Menu ===
    private void handleRecordingClick(Player player, ItemStack clicked, int slot) {
        if (slot == 45) { // Voltar
            AdminHubMenu.open(player);
            return;
        }
        if (slot == 47 && clicked.getType() == Material.SPECTRAL_ARROW) {
            RecordingMenu.open(player, RecordingMenu.getActivePage(player.getUniqueId()) - 1);
            return;
        }
        if (slot == 49 && clicked.getType() == Material.EMERALD) { // Iniciar gravacao
            RecordingMenu.openStartRecording(player);
            return;
        }
        if (slot == 51 && clicked.getType() == Material.CHEST) { // Gravacoes salvas
            RecordingMenu.openSaved(player, 0);
            return;
        }
        if (slot == 53 && clicked.getType() == Material.SPECTRAL_ARROW) {
            RecordingMenu.open(player, RecordingMenu.getActivePage(player.getUniqueId()) + 1);
            return;
        }
        // Active recording - stop it
        if (clicked.getType() == Material.MUSIC_DISC_CAT && slot >= 9 && slot <= 35) {
            String targetName = MenuViewHelper.stripDecorativePrefix(ChatColor.stripColor(clicked.getItemMeta().getDisplayName()));
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null && Voicechat.voiceRecordingManager.isRecording(target.getUniqueId())) {
                VoiceRecording rec = Voicechat.voiceRecordingManager.stopRecording(target.getUniqueId());
                if (rec != null) {
                    player.sendMessage(Voicechat.MESSAGES.format(
                            "gui.admin.recording.stopped",
                            "&aGravacao de %s finalizada!",
                            targetName
                    ));
                }
            }
            RecordingMenu.open(player, RecordingMenu.getActivePage(player.getUniqueId()));
        }
    }

    private void handleSavedRecordingClick(Player player, ItemStack clicked, int slot, ClickType clickType) {
        int currentPage = RecordingMenu.getSavedPage(player.getUniqueId());

        if (slot == 45) { // Voltar
            RecordingMenu.open(player, RecordingMenu.getActivePage(player.getUniqueId()));
            return;
        }
        if (slot == 47) { // Pagina anterior
            RecordingMenu.openSaved(player, currentPage - 1);
            return;
        }
        if (slot == 51) { // Proxima pagina
            RecordingMenu.openSaved(player, currentPage + 1);
            return;
        }
        if (clicked.getType() == Material.DISC_FRAGMENT_5 && clicked.hasItemMeta()) {
            String recId = MenuViewHelper.stripDecorativePrefix(ChatColor.stripColor(clicked.getItemMeta().getDisplayName()));
            if (clickType == ClickType.RIGHT) {
                // Delete
                if (Voicechat.voiceRecordingManager.deleteSavedRecording(recId)) {
                    player.sendMessage(Voicechat.MESSAGES.format(
                            "gui.admin.recording.saved_deleted",
                            "&aGravacao '%s' deletada!",
                            recId
                    ));
                }
                RecordingMenu.openSaved(player, currentPage);
            } else {
                // Info
                String info = Voicechat.voiceRecordingManager.getRecordingInfo(recId);
                if (info != null) {
                    player.sendMessage(Voicechat.MESSAGES.format(
                            "gui.admin.recording.info_header",
                            "&6=== Detalhes: %s ===",
                            recId
                    ));
                    player.sendMessage(info);
                } else {
                    player.sendMessage(Voicechat.MESSAGES.format(
                            "gui.admin.recording.info_fallback",
                            "&eGravacao: %s",
                            recId
                    ));
                }
            }
        }
    }

    private void handleStartRecordingClick(Player player, ItemStack clicked, int slot) {
        if (slot == 45) { // Voltar
            RecordingMenu.open(player, RecordingMenu.getActivePage(player.getUniqueId()));
            return;
        }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = RecordingMenu.getStartPage(player.getUniqueId());
            RecordingMenu.openStartRecording(player, slot == 47 ? currentPage - 1 : currentPage + 1);
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid == null) return;
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null) {
                if (Voicechat.voiceRecordingManager.isRecording(target.getUniqueId())) {
                    player.sendMessage(Voicechat.MESSAGES.format(
                            "gui.admin.recording.already_recording",
                            "&c%s ja esta sendo gravado!",
                            target.getName()
                    ));
                } else {
                    Voicechat.voiceRecordingManager.startRecording(
                            target.getUniqueId(), target.getName(),
                            player.getUniqueId(), player.getName());
                    player.sendMessage(Voicechat.MESSAGES.format(
                            "gui.admin.recording.started",
                            "&aGravacao de %s iniciada!",
                            target.getName()
                    ));
                }
                RecordingMenu.open(player, RecordingMenu.getActivePage(player.getUniqueId()));
            }
        }
    }

    // === Volume & Priority ===
    private void handleVolumePriorityClick(Player player, ItemStack clicked, int slot, ClickType clickType) {
        if (slot == 45) { // Voltar
            AdminHubMenu.open(player);
            return;
        }
        if (slot == 47 && clicked.getType() == Material.SPECTRAL_ARROW) {
            VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()) - 1);
            return;
        }
        if (slot == 49 && clicked.getType() == Material.BELL) { // Definir volume
            VolumePriorityMenu.openSelectPlayerForVolume(player);
            return;
        }
        if (slot == 51 && clicked.getType() == Material.DIAMOND) { // Definir prioridade
            VolumePriorityMenu.openSelectPlayerForPriority(player);
            return;
        }
        if (slot == 53 && clicked.getType() == Material.SPECTRAL_ARROW) {
            VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()) + 1);
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid == null) return;
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
            String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);

            if (clickType == ClickType.SHIFT_LEFT) {
                // Remove all
                Voicechat.playerRangeManager.removeVolume(targetUuid);
                Voicechat.playerRangeManager.removePriority(targetUuid);
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.admin.volume_priority.removed",
                        "&aVolume e prioridade de %s removidos!",
                        targetName
                ));
                Voicechat.activityLogger.log(player.getName() + " removeu volume e prioridade de " + targetName);
                VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            } else if (clickType == ClickType.LEFT) {
                // Edit volume
                VolumePriorityMenu.openVolumeSelect(player, targetUuid);
            } else if (clickType == ClickType.RIGHT) {
                // Edit priority
                VolumePriorityMenu.openPrioritySelect(player, targetUuid);
            }
        }
    }

    private void handleVolumeSelectClick(Player player, int slot, ItemStack clicked) {
        UUID targetUuid = VolumePriorityMenu.getSelectedVolumeTarget(player.getUniqueId());
        if (targetUuid == null) {
            player.sendMessage(Voicechat.MESSAGES.text(
                    "gui.admin.volume_priority.player_missing",
                    "&cJogador selecionado nao encontrado."
            ));
            VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);

        if (slot == 18) { // Voltar
            VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            return;
        }
        if (slot == 17) { // Reset
            if (Voicechat.playerRangeManager.removeVolume(targetUuid)) {
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.admin.volume.removed",
                        "&aVolume de %s removido!",
                        targetName
                ));
                Voicechat.activityLogger.log(player.getName() + " removeu volume customizado de " + targetName);
            }
            VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            return;
        }
        float[] values = {0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f, 5.0f};
        if (slot >= 9 && slot <= 16) {
            int idx = slot - 9;
            if (idx < values.length) {
                Voicechat.playerRangeManager.setVolume(targetUuid, values[idx]);
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.admin.volume.set",
                        "&aVolume de %s definido para %s!",
                        targetName,
                        String.format("%.2fx", values[idx])
                ));
                Voicechat.activityLogger.log(player.getName() + " definiu volume de " + targetName + " para " + String.format("%.2f", values[idx]));
                VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handlePrioritySelectClick(Player player, int slot, ItemStack clicked) {
        UUID targetUuid = VolumePriorityMenu.getSelectedPriorityTarget(player.getUniqueId());
        if (targetUuid == null) {
            player.sendMessage(Voicechat.MESSAGES.text(
                    "gui.admin.volume_priority.player_missing",
                    "&cJogador selecionado nao encontrado."
            ));
            VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);

        if (slot == 18) { // Voltar
            VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            return;
        }
        int[] values = {0, 1, 5, 10, 25, 50, 75, 100};
        if (slot >= 9 && slot <= 16) {
            int idx = slot - 9;
            if (idx < values.length) {
                Voicechat.playerRangeManager.setPriority(targetUuid, values[idx]);
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.admin.priority.set",
                        "&aPrioridade de %s definida para %s",
                        targetName,
                        values[idx]
                ));
                Voicechat.activityLogger.log(player.getName() + " definiu prioridade de " + targetName + " para " + values[idx]);
                VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handleSelectPlayerForVolume(Player player, ItemStack clicked, int slot) {
        if (slot == 45) { // Voltar
            VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            return;
        }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = VolumePriorityMenu.getVolumeSelectPage(player.getUniqueId());
            VolumePriorityMenu.openSelectPlayerForVolume(player, slot == 47 ? currentPage - 1 : currentPage + 1);
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                VolumePriorityMenu.openVolumeSelect(player, targetUuid);
            }
        }
    }

    private void handleSelectPlayerForPriority(Player player, ItemStack clicked, int slot) {
        if (slot == 45) { // Voltar
            VolumePriorityMenu.open(player, VolumePriorityMenu.getPage(player.getUniqueId()));
            return;
        }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = VolumePriorityMenu.getPrioritySelectPage(player.getUniqueId());
            VolumePriorityMenu.openSelectPlayerForPriority(player, slot == 47 ? currentPage - 1 : currentPage + 1);
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                VolumePriorityMenu.openPrioritySelect(player, targetUuid);
            }
        }
    }

    // === Cooldown ===
    private void handleCooldownClick(Player player, int slot) {
        switch (slot) {
            case 10: { // Toggle
                if (Voicechat.voiceCooldownManager.isEnabled()) {
                    Voicechat.voiceCooldownManager.setMaxTalkTime(0);
                    Voicechat.voiceCooldownManager.setCooldown(0);
                    Voicechat.persistGlobalCooldownSettings();
                    player.sendMessage(Voicechat.MESSAGES.text(
                            "gui.admin.cooldown.disabled",
                            "&aCooldown de voz desativado!"
                    ));
                    Voicechat.activityLogger.log(player.getName() + " desativou cooldown de voz via menu");
                } else {
                    Voicechat.voiceCooldownManager.setMaxTalkTime(30);
                    Voicechat.voiceCooldownManager.setCooldown(10);
                    Voicechat.persistGlobalCooldownSettings();
                    player.sendMessage(Voicechat.MESSAGES.text(
                            "gui.admin.cooldown.enabled",
                            "&aCooldown de voz ativado (30s fala, 10s espera)!"
                    ));
                    Voicechat.activityLogger.log(player.getName() + " ativou cooldown de voz via menu");
                }
                CooldownMenu.open(player);
                break;
            }
            case 12: { // Cycle talk time
                long currentSec = Voicechat.voiceCooldownManager.getMaxTalkTimeMs() / 1000;
                long next = CooldownMenu.getNextTalkValue(currentSec);
                Voicechat.voiceCooldownManager.setMaxTalkTime(next);
                if (next > 0 && Voicechat.voiceCooldownManager.getCooldownMs() == 0) {
                    Voicechat.voiceCooldownManager.setCooldown(10);
                }
                Voicechat.persistGlobalCooldownSettings();
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.admin.cooldown.talk_time_set",
                        "&aTempo de fala definido para %s",
                        next > 0 ? MessageFormatUtil.seconds(next) : Voicechat.MESSAGES.text("gui.admin.cooldown.unlimited", "ilimitado")
                ));
                Voicechat.activityLogger.log(player.getName() + " alterou tempo de fala para " + next + "s via menu");
                CooldownMenu.open(player);
                break;
            }
            case 14: { // Cycle cooldown
                long currentSec = Voicechat.voiceCooldownManager.getCooldownMs() / 1000;
                long next = CooldownMenu.getNextCooldownValue(currentSec);
                Voicechat.voiceCooldownManager.setCooldown(next);
                Voicechat.persistGlobalCooldownSettings();
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.admin.cooldown.wait_time_set",
                        "&aCooldown definido para %s",
                        next > 0 ? MessageFormatUtil.seconds(next) : Voicechat.MESSAGES.text("gui.admin.cooldown.none", "nenhum")
                ));
                Voicechat.activityLogger.log(player.getName() + " alterou cooldown para " + next + "s via menu");
                CooldownMenu.open(player);
                break;
            }
            case 36: // Voltar
                AdminHubMenu.open(player);
                break;
        }
    }

    // === Stage Speakers ===
    private void handleSpeakersClick(Player player, String zoneName, ItemStack clicked, int slot) {
        RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
        if (zone == null) {
            player.sendMessage(Voicechat.MESSAGES.text(
                    "gui.admin.zone_not_found",
                    "&cZona nao encontrada!"
            ));
            AdminHubMenu.open(player);
            return;
        }

        if (slot == 45) { // Voltar
            ZoneSettingsMenu.open(player, zone);
            return;
        }
        if (slot == 49) { // Adicionar
            StageSpeakersMenu.openAddSpeaker(player, zone);
            return;
        }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = StageSpeakersMenu.getSpeakerPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            StageSpeakersMenu.open(player, zone, newPage);
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                zone.removeSpeaker(targetUuid);
                Voicechat.restrictedZoneManager.save();
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.admin.stage.removed",
                        "&a%s removido dos speakers da zona %s",
                        targetName,
                        zoneName
                ));
                Voicechat.activityLogger.log(player.getName() + " removeu " + targetName + " dos speakers da zona " + zoneName);
                StageSpeakersMenu.open(player, zone, StageSpeakersMenu.getSpeakerPage(player.getUniqueId()));
            }
        }
    }

    private void handleAddSpeakerClick(Player player, String zoneName, ItemStack clicked, int slot) {
        RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
        if (zone == null) {
            player.sendMessage(Voicechat.MESSAGES.text(
                    "gui.admin.zone_not_found",
                    "&cZona nao encontrada!"
            ));
            AdminHubMenu.open(player);
            return;
        }

        if (slot == 45) { // Voltar
            StageSpeakersMenu.open(player, zone, StageSpeakersMenu.getSpeakerPage(player.getUniqueId()));
            return;
        }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = StageSpeakersMenu.getAddSpeakerPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            StageSpeakersMenu.openAddSpeaker(player, zone, newPage);
            return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                zone.addSpeaker(targetUuid);
                Voicechat.restrictedZoneManager.save();
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.admin.stage.added",
                        "&a%s adicionado como speaker na zona %s",
                        targetName,
                        zoneName
                ));
                Voicechat.activityLogger.log(player.getName() + " adicionou " + targetName + " como speaker na zona " + zoneName);
                StageSpeakersMenu.open(player, zone, StageSpeakersMenu.getSpeakerPage(player.getUniqueId()));
            }
        }
    }

    private UUID getPlayerUuidFromSkull(ItemStack item) {
        if (item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) return null;
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return null;
        OfflinePlayer owner = meta.getOwningPlayer();
        if (owner != null) {
            return owner.getUniqueId();
        }
        return null;
    }
}
