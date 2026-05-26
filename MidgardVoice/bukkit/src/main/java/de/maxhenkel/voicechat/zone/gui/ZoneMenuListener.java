package de.maxhenkel.voicechat.zone.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.gui.MenuViewHelper;
import de.maxhenkel.voicechat.zone.GlobalZoneSettings;
import de.maxhenkel.voicechat.zone.RestrictedZone;
import de.maxhenkel.voicechat.gui.StageSpeakersMenu;
import de.maxhenkel.voicechat.util.MessageFormatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class ZoneMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (title.equals(ZoneListMenu.getTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleZoneListClick(player, clicked, event.getSlot(), event.getClick());
        } else if (title.equals(GlobalSettingsMenu.getTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleGlobalSettingsClick(player, event.getSlot(), event.getClick());
        } else if (title.startsWith(ZoneSettingsMenu.getTitlePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            String zoneName = title.substring(ZoneSettingsMenu.getTitlePrefix().length());
            handleZoneSettingsClick(player, zoneName, event.getSlot(), event.getClick());
        } else if (title.startsWith(ZoneAllowedPlayersMenu.getTitlePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            String zoneName = title.substring(ZoneAllowedPlayersMenu.getTitlePrefix().length());
            handleAllowedPlayersClick(player, zoneName, clicked, event.getSlot());
        } else if (title.startsWith(ZoneMutedPlayersMenu.getTitlePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            String zoneName = title.substring(ZoneMutedPlayersMenu.getTitlePrefix().length());
            handleMutedPlayersClick(player, zoneName, clicked, event.getSlot());
        } else if (title.startsWith(ZoneAddPlayerMenu.getTitleAllowedPrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            String zoneName = title.substring(ZoneAddPlayerMenu.getTitleAllowedPrefix().length());
            handleAddAllowedPlayerClick(player, zoneName, clicked, event.getSlot());
        } else if (title.startsWith(ZoneAddPlayerMenu.getTitleMutePrefix())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            String zoneName = title.substring(ZoneAddPlayerMenu.getTitleMutePrefix().length());
            handleAddMutedPlayerClick(player, zoneName, clicked, event.getSlot());
        } else if (title.equals(GlobalPlayerListMenu.getAllowedTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleGlobalAllowedClick(player, clicked, event.getSlot());
        } else if (title.equals(GlobalPlayerListMenu.getMutedTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleGlobalMutedClick(player, clicked, event.getSlot());
        } else if (title.equals(GlobalPlayerListMenu.getSpeakersTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleGlobalSpeakersClick(player, clicked, event.getSlot());
        } else if (title.equals(GlobalPlayerListMenu.getAddAllowedTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleGlobalAddAllowedClick(player, clicked, event.getSlot());
        } else if (title.equals(GlobalPlayerListMenu.getAddMutedTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleGlobalAddMutedClick(player, clicked, event.getSlot());
        } else if (title.equals(GlobalPlayerListMenu.getAddSpeakerTitle())) {
            event.setCancelled(true);
            if (isDecoration(clicked)) return;
            handleGlobalAddSpeakerClick(player, clicked, event.getSlot());
        }
    }

    private boolean isDecoration(ItemStack item) {
        Material type = item.getType();
        return type == Material.PURPLE_STAINED_GLASS_PANE
                || type == Material.GRAY_STAINED_GLASS_PANE
                || type == Material.NETHER_STAR
                || type == Material.BOOK;
    }

    private void handleZoneListClick(Player player, ItemStack clicked, int slot, ClickType clickType) {
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            de.maxhenkel.voicechat.gui.AdminHubMenu.open(player);
            return;
        }

        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = ZoneListMenu.getPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            ZoneListMenu.open(player, newPage);
            return;
        }

        // Regiao Global no slot 9
        if (slot == 9 && clicked.getType() == Material.FILLED_MAP) {
            String name = MenuViewHelper.stripDecorativePrefix(ChatColor.stripColor(clicked.getItemMeta().getDisplayName()));
            if (name.equals(globalRegionName())) {
                GlobalSettingsMenu.open(player);
                return;
            }
        }

        if (slot == 49 && clicked.getType() == Material.EMERALD) {
            Voicechat.restrictedZoneManager.load();
            if (Voicechat.globalZoneSettings != null) Voicechat.globalZoneSettings.load();
            player.sendMessage(Voicechat.MESSAGES.gui_zona_recarregadas);
            int currentPage = ZoneListMenu.getPage(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(Voicechat.INSTANCE, () -> ZoneListMenu.open(player, currentPage), 1L);
            return;
        }

        if (clicked.getType() == Material.FILLED_MAP && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String zoneName = MenuViewHelper.stripDecorativePrefix(ChatColor.stripColor(clicked.getItemMeta().getDisplayName()));
            // Ignorar a regiao global (tratada acima)
            if (zoneName.equals(globalRegionName())) {
                return;
            }

            if (clickType == ClickType.SHIFT_LEFT) {
                // Deletar zona com shift+click
                if (Voicechat.restrictedZoneManager.getZone(zoneName) != null) {
                    Voicechat.restrictedZoneManager.removeZone(zoneName);
                    Voicechat.activityLogger.logZoneDeleted(player.getName(), zoneName);
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_deletada, zoneName));
                    if (Voicechat.zoneCooldownTracker != null) {
                        Voicechat.zoneCooldownTracker.clearZone(zoneName);
                    }
                    ZoneListMenu.open(player, ZoneListMenu.getPage(player.getUniqueId()));
                } else {
                    player.sendMessage(Voicechat.MESSAGES.gui_zona_nao_encontrada);
                }
                return;
            }

            RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
            if (zone != null) {
                ZoneSettingsMenu.open(player, zone);
            } else {
                player.sendMessage(Voicechat.MESSAGES.gui_zona_nao_encontrada);
            }
        }
    }

    private static final float[] RANGE_VALUES = {-1f, 16f, 32f, 48f, 64f, 96f, 128f};
    private static final float[] MULTIPLIER_VALUES = {0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 5.0f};
    // Cooldown presets: {maxTalkTimeSec, cooldownSec}
    private static final long[][] COOLDOWN_PRESETS = {
            {0, 0},       // Disabled
            {30, 10},
            {60, 15},
            {60, 30},
            {120, 30},
            {120, 60},
            {180, 60},
            {300, 120}
    };

    private void handleZoneSettingsClick(Player player, String zoneName, int slot, ClickType clickType) {
        RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
        if (zone == null) {
            player.sendMessage(Voicechat.MESSAGES.gui_zona_nao_existe_mais);
            ZoneListMenu.open(player, ZoneListMenu.getPage(player.getUniqueId()));
            return;
        }

        switch (slot) {
            case 20:
                zone.setVoiceEnabled(!zone.isVoiceEnabled());
                Voicechat.restrictedZoneManager.save();
                Voicechat.activityLogger.logZoneVoiceToggled(player.getName(), zoneName, zone.isVoiceEnabled());
                if (zone.isVoiceEnabled()) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_voz_ativada_msg, zoneName));
                } else {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_voz_desativada_msg, zoneName));
                }
                ZoneSettingsMenu.open(player, zone);
                break;
            case 22:
                ZoneAllowedPlayersMenu.open(player, zone);
                break;
            case 24:
                ZoneMutedPlayersMenu.open(player, zone);
                break;
            case 29: {
                float current = zone.getCustomRange();
                int idx = 0;
                for (int i = 0; i < RANGE_VALUES.length; i++) {
                    if (RANGE_VALUES[i] == current) { idx = i; break; }
                }
                idx = (idx + 1) % RANGE_VALUES.length;
                zone.setCustomRange(RANGE_VALUES[idx]);
                Voicechat.restrictedZoneManager.save();
                String val = RANGE_VALUES[idx] < 0 ? Voicechat.MESSAGES.gui_zona_range_padrao_valor : MessageFormatUtil.blocks((int) RANGE_VALUES[idx]);
                Voicechat.activityLogger.log(player.getName() + " alterou range da zona " + zoneName + " para " + val);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_range_alterado, zoneName, val));
                ZoneSettingsMenu.open(player, zone);
                break;
            }
            case 31: {
                float current = zone.getRangeMultiplier();
                int idx = 0;
                for (int i = 0; i < MULTIPLIER_VALUES.length; i++) {
                    if (MULTIPLIER_VALUES[i] == current) { idx = i; break; }
                }
                idx = (idx + 1) % MULTIPLIER_VALUES.length;
                zone.setRangeMultiplier(MULTIPLIER_VALUES[idx]);
                Voicechat.restrictedZoneManager.save();
                String val = String.format("%.1fx", MULTIPLIER_VALUES[idx]);
                Voicechat.activityLogger.log(player.getName() + " alterou multiplicador da zona " + zoneName + " para " + val);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_multiplicador_alterado, zoneName, val));
                ZoneSettingsMenu.open(player, zone);
                break;
            }
            case 33:
                zone.setListenOnly(!zone.isListenOnly());
                Voicechat.restrictedZoneManager.save();
                Voicechat.activityLogger.log(player.getName() + (zone.isListenOnly() ? " ativou" : " desativou") + " somente-escuta na zona " + zoneName);
                if (zone.isListenOnly()) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_escuta_ativada_msg, zoneName));
                } else {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_escuta_desativada_msg, zoneName));
                }
                ZoneSettingsMenu.open(player, zone);
                break;
            case 38:
                zone.setStageMode(!zone.isStageMode());
                Voicechat.restrictedZoneManager.save();
                Voicechat.activityLogger.log(player.getName() + (zone.isStageMode() ? " ativou" : " desativou") + " stage mode na zona " + zoneName);
                if (zone.isStageMode()) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_stage_ativado_msg, zoneName));
                } else {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_stage_desativado_msg, zoneName));
                }
                ZoneSettingsMenu.open(player, zone);
                break;
            case 39:
                if (zone.isStageMode()) {
                    StageSpeakersMenu.open(player, zone);
                }
                break;
            case 40: {
                if (clickType == ClickType.SHIFT_LEFT) {
                    // Custom cooldown via chat
                    player.closeInventory();
                    for (String line : Voicechat.MESSAGES.textList(
                            "gui.zone.chat_input.cooldown_prompt_lines",
                            "&eDigite o tempo de fala e cooldown em segundos separados por espaco.",
                            "&eExemplo: &f45 15&e (45s fala, 15s cooldown)",
                            "&eDigite &f0&e para desativar, ou &fcancelar&e para voltar."
                    )) {
                        player.sendMessage(line);
                    }
                    ChatInputListener.awaitZoneCooldownInput(player.getUniqueId(), zoneName);
                    break;
                }
                long curTalk = zone.getZoneCooldownMaxTalkTimeSec();
                long curCd = zone.getZoneCooldownSec();
                int idx = 0;
                for (int i = 0; i < COOLDOWN_PRESETS.length; i++) {
                    if (COOLDOWN_PRESETS[i][0] == curTalk && COOLDOWN_PRESETS[i][1] == curCd) {
                        idx = i;
                        break;
                    }
                }
                idx = (idx + 1) % COOLDOWN_PRESETS.length;
                zone.setZoneCooldownMaxTalkTimeSec(COOLDOWN_PRESETS[idx][0]);
                zone.setZoneCooldownSec(COOLDOWN_PRESETS[idx][1]);
                Voicechat.restrictedZoneManager.save();
                String msg;
                if (COOLDOWN_PRESETS[idx][0] == 0) {
                    msg = ChatColor.stripColor(Voicechat.MESSAGES.text("gui.zone.cooldown.disabled_label", "Desativado"));
                } else {
                    msg = ChatColor.stripColor(Voicechat.MESSAGES.format(
                            "gui.zone.cooldown.summary",
                            "fala=%ss, espera=%ss",
                            COOLDOWN_PRESETS[idx][0],
                            COOLDOWN_PRESETS[idx][1]
                    ));
                }
                Voicechat.activityLogger.log(player.getName() + " alterou cooldown da zona " + zoneName + " para " + msg);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_cooldown_alterado, zoneName,
                        COOLDOWN_PRESETS[idx][0], COOLDOWN_PRESETS[idx][1]));
                ZoneSettingsMenu.open(player, zone);
                break;
            }
            case 49:
                teleportToZone(player, zone);
                break;
            case 42:
                if (Voicechat.zoneParticleVisualizer != null) {
                    Voicechat.zoneParticleVisualizer.toggleZone(player, zoneName);
                    boolean viewing = Voicechat.zoneParticleVisualizer.isViewingZone(player.getUniqueId(), zoneName);
                    player.sendMessage(Voicechat.MESSAGES.format(
                            viewing ? "gui.zone.particles.enabled" : "gui.zone.particles.disabled",
                            viewing
                                    ? "&d[MidgardVoice] &aParticulas da zona '%s' ATIVADAS"
                                    : "&d[MidgardVoice] &cParticulas da zona '%s' DESATIVADAS",
                            zoneName
                    ));
                }
                ZoneSettingsMenu.open(player, zone);
                break;
            case 45:
                ZoneListMenu.open(player, ZoneListMenu.getPage(player.getUniqueId()));
                break;
        }
    }

    private static final double[] GLOBAL_RANGE_VALUES = {16D, 24D, 32D, 48D, 64D, 96D, 128D, 256D};

    private void handleGlobalSettingsClick(Player player, int slot, ClickType clickType) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        if (g == null) return;

        switch (slot) {
            case 20: {
                // Voice toggle
                g.setVoiceEnabled(!g.isVoiceEnabled());
                g.save();
                Voicechat.activityLogger.log(player.getName() + (g.isVoiceEnabled() ? " ativou" : " desativou") + " voz global");
                if (g.isVoiceEnabled()) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_voz_ativada_msg, globalRegionName()));
                } else {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_voz_desativada_msg, globalRegionName()));
                }
                GlobalSettingsMenu.open(player);
                break;
            }
            case 22:
                GlobalPlayerListMenu.openAllowed(player);
                break;
            case 24:
                GlobalPlayerListMenu.openMuted(player);
                break;
            case 29: {
                // Range global
                double current = Voicechat.SERVER_CONFIG.voiceChatDistance.get();
                int idx = 0;
                for (int i = 0; i < GLOBAL_RANGE_VALUES.length; i++) {
                    if (GLOBAL_RANGE_VALUES[i] == current) { idx = i; break; }
                }
                idx = (idx + 1) % GLOBAL_RANGE_VALUES.length;
                Voicechat.SERVER_CONFIG.voiceChatDistance.set(GLOBAL_RANGE_VALUES[idx]);
                Voicechat.SERVER_CONFIG.voiceChatDistance.save();
                String val = MessageFormatUtil.blocks((int) GLOBAL_RANGE_VALUES[idx]);
                Voicechat.activityLogger.log(player.getName() + " alterou range global para " + val);
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.global.range.changed",
                        "&aRange global alterado para: &f%s",
                        val
                ));
                GlobalSettingsMenu.open(player);
                break;
            }
            case 31: {
                // Range multiplier
                float current = g.getRangeMultiplier();
                int idx = 0;
                for (int i = 0; i < MULTIPLIER_VALUES.length; i++) {
                    if (MULTIPLIER_VALUES[i] == current) { idx = i; break; }
                }
                idx = (idx + 1) % MULTIPLIER_VALUES.length;
                g.setRangeMultiplier(MULTIPLIER_VALUES[idx]);
                g.save();
                String val = String.format("%.1fx", MULTIPLIER_VALUES[idx]);
                Voicechat.activityLogger.log(player.getName() + " alterou multiplicador global para " + val);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_multiplicador_alterado, globalRegionName(), val));
                GlobalSettingsMenu.open(player);
                break;
            }
            case 33: {
                // Listen only
                g.setListenOnly(!g.isListenOnly());
                g.save();
                Voicechat.activityLogger.log(player.getName() + (g.isListenOnly() ? " ativou" : " desativou") + " somente-escuta global");
                if (g.isListenOnly()) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_escuta_ativada_msg, globalRegionName()));
                } else {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_escuta_desativada_msg, globalRegionName()));
                }
                GlobalSettingsMenu.open(player);
                break;
            }
            case 38: {
                // Stage mode
                g.setStageMode(!g.isStageMode());
                g.save();
                Voicechat.activityLogger.log(player.getName() + (g.isStageMode() ? " ativou" : " desativou") + " stage mode global");
                if (g.isStageMode()) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_stage_ativado_msg, globalRegionName()));
                } else {
                    player.sendMessage(String.format(Voicechat.MESSAGES.gui_zona_stage_desativado_msg, globalRegionName()));
                }
                GlobalSettingsMenu.open(player);
                break;
            }
            case 39: {
                // Manage speakers
                if (g.isStageMode()) {
                    GlobalPlayerListMenu.openSpeakers(player);
                }
                break;
            }
            case 40: {
                // Cooldown global
                if (Voicechat.voiceCooldownManager == null) break;
                long curTalk = Voicechat.voiceCooldownManager.getMaxTalkTimeMs() / 1000;
                long curCd = Voicechat.voiceCooldownManager.getCooldownMs() / 1000;
                int idx = 0;
                for (int i = 0; i < COOLDOWN_PRESETS.length; i++) {
                    if (COOLDOWN_PRESETS[i][0] == curTalk && COOLDOWN_PRESETS[i][1] == curCd) {
                        idx = i;
                        break;
                    }
                }
                idx = (idx + 1) % COOLDOWN_PRESETS.length;
                Voicechat.voiceCooldownManager.setMaxTalkTime(COOLDOWN_PRESETS[idx][0]);
                Voicechat.voiceCooldownManager.setCooldown(COOLDOWN_PRESETS[idx][1]);
                Voicechat.persistGlobalCooldownSettings();
                String msg;
                if (COOLDOWN_PRESETS[idx][0] == 0) {
                    msg = ChatColor.stripColor(Voicechat.MESSAGES.text("gui.global.cooldown.disabled_label", "Desativado"));
                } else {
                    msg = ChatColor.stripColor(Voicechat.MESSAGES.format(
                            "gui.global.cooldown.summary",
                            "fala=%ss, espera=%ss",
                            COOLDOWN_PRESETS[idx][0],
                            COOLDOWN_PRESETS[idx][1]
                    ));
                }
                Voicechat.activityLogger.log(player.getName() + " alterou cooldown global para " + msg);
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.global.cooldown.changed",
                        "&aCooldown global alterado para: &f%s",
                        msg
                ));
                GlobalSettingsMenu.open(player);
                break;
            }
            case 45:
                ZoneListMenu.open(player, ZoneListMenu.getPage(player.getUniqueId()));
                break;
        }
    }

    private void handleAllowedPlayersClick(Player player, String zoneName, ItemStack clicked, int slot) {
        RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
        if (zone == null) {
            player.sendMessage(Voicechat.MESSAGES.gui_zona_nao_existe_mais);
            ZoneListMenu.open(player, ZoneListMenu.getPage(player.getUniqueId()));
            return;
        }

        if (slot == 45 && clicked.getType() == Material.ARROW) {
            ZoneSettingsMenu.open(player, zone);
            return;
        }

        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = ZoneAllowedPlayersMenu.getPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            ZoneAllowedPlayersMenu.open(player, zone, newPage);
            return;
        }

        if (slot == 49 && clicked.getType() == Material.EMERALD) {
            ZoneAddPlayerMenu.openForAllowed(player, zone);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                zone.removeAllowedPlayer(targetUuid);
                Voicechat.restrictedZoneManager.save();
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_permitidos_removido, targetName));
                Voicechat.activityLogger.logZonePlayerRemoved(player.getName(), targetName, zoneName, "permitidos");
                ZoneAllowedPlayersMenu.open(player, zone, ZoneAllowedPlayersMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handleMutedPlayersClick(Player player, String zoneName, ItemStack clicked, int slot) {
        RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
        if (zone == null) {
            player.sendMessage(Voicechat.MESSAGES.gui_zona_nao_existe_mais);
            ZoneListMenu.open(player, ZoneListMenu.getPage(player.getUniqueId()));
            return;
        }

        if (slot == 45 && clicked.getType() == Material.ARROW) {
            ZoneSettingsMenu.open(player, zone);
            return;
        }

        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = ZoneMutedPlayersMenu.getPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            ZoneMutedPlayersMenu.open(player, zone, newPage);
            return;
        }

        if (slot == 49 && clicked.getType() == Material.REDSTONE) {
            ZoneAddPlayerMenu.openForMuted(player, zone);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                zone.removeMutedPlayer(targetUuid);
                Voicechat.restrictedZoneManager.save();
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_mutados_desmutado, targetName));
                Voicechat.activityLogger.logZonePlayerRemoved(player.getName(), targetName, zoneName, "mutados");
                ZoneMutedPlayersMenu.open(player, zone, ZoneMutedPlayersMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handleAddAllowedPlayerClick(Player player, String zoneName, ItemStack clicked, int slot) {
        RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
        if (zone == null) {
            player.sendMessage(Voicechat.MESSAGES.gui_zona_nao_existe_mais);
            ZoneListMenu.open(player, ZoneListMenu.getPage(player.getUniqueId()));
            return;
        }

        if (slot == 45 && clicked.getType() == Material.ARROW) {
            ZoneAllowedPlayersMenu.open(player, zone, ZoneAllowedPlayersMenu.getPage(player.getUniqueId()));
            return;
        }

        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = ZoneAddPlayerMenu.getPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            ZoneAddPlayerMenu.openForAllowed(player, zone, newPage);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                zone.addAllowedPlayer(targetUuid);
                Voicechat.restrictedZoneManager.save();
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_permitidos_adicionado, targetName));
                Voicechat.activityLogger.logZonePlayerAdded(player.getName(), targetName, zoneName, "permitidos");
                ZoneAllowedPlayersMenu.open(player, zone, ZoneAllowedPlayersMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handleAddMutedPlayerClick(Player player, String zoneName, ItemStack clicked, int slot) {
        RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
        if (zone == null) {
            player.sendMessage(Voicechat.MESSAGES.gui_zona_nao_existe_mais);
            ZoneListMenu.open(player, ZoneListMenu.getPage(player.getUniqueId()));
            return;
        }

        if (slot == 45 && clicked.getType() == Material.ARROW) {
            ZoneMutedPlayersMenu.open(player, zone, ZoneMutedPlayersMenu.getPage(player.getUniqueId()));
            return;
        }

        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int currentPage = ZoneAddPlayerMenu.getPage(player.getUniqueId());
            int newPage = slot == 47 ? currentPage - 1 : currentPage + 1;
            ZoneAddPlayerMenu.openForMuted(player, zone, newPage);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID targetUuid = getPlayerUuidFromSkull(clicked);
            if (targetUuid != null) {
                zone.addMutedPlayer(targetUuid);
                Voicechat.restrictedZoneManager.save();
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                String targetName = target.getName() != null ? target.getName() : targetUuid.toString().substring(0, 8);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_mutados_mutado, targetName));
                Voicechat.activityLogger.logZonePlayerAdded(player.getName(), targetName, zoneName, "mutados");
                ZoneMutedPlayersMenu.open(player, zone, ZoneMutedPlayersMenu.getPage(player.getUniqueId()));
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

    // === Global Player List Handlers ===

    private void handleGlobalAllowedClick(Player player, ItemStack clicked, int slot) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        if (slot == 45 && clicked.getType() == Material.ARROW) { GlobalSettingsMenu.open(player); return; }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int p = GlobalPlayerListMenu.getPage(player.getUniqueId());
            GlobalPlayerListMenu.openAllowed(player, slot == 47 ? p - 1 : p + 1); return;
        }
        if (slot == 49 && clicked.getType() == Material.EMERALD) { GlobalPlayerListMenu.openAddAllowed(player); return; }
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID uuid = getPlayerUuidFromSkull(clicked);
            if (uuid != null) {
                g.removeAllowedPlayer(uuid);
                g.save();
                OfflinePlayer t = Bukkit.getOfflinePlayer(uuid);
                String name = t.getName() != null ? t.getName() : uuid.toString().substring(0, 8);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_permitidos_removido, name));
                Voicechat.activityLogger.log(player.getName() + " removeu " + name + " dos permitidos globais");
                GlobalPlayerListMenu.openAllowed(player, GlobalPlayerListMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handleGlobalMutedClick(Player player, ItemStack clicked, int slot) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        if (slot == 45 && clicked.getType() == Material.ARROW) { GlobalSettingsMenu.open(player); return; }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int p = GlobalPlayerListMenu.getPage(player.getUniqueId());
            GlobalPlayerListMenu.openMuted(player, slot == 47 ? p - 1 : p + 1); return;
        }
        if (slot == 49 && clicked.getType() == Material.REDSTONE) { GlobalPlayerListMenu.openAddMuted(player); return; }
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID uuid = getPlayerUuidFromSkull(clicked);
            if (uuid != null) {
                g.removeMutedPlayer(uuid);
                g.save();
                OfflinePlayer t = Bukkit.getOfflinePlayer(uuid);
                String name = t.getName() != null ? t.getName() : uuid.toString().substring(0, 8);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_mutados_desmutado, name));
                Voicechat.activityLogger.log(player.getName() + " desmutou " + name + " na global");
                GlobalPlayerListMenu.openMuted(player, GlobalPlayerListMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handleGlobalSpeakersClick(Player player, ItemStack clicked, int slot) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        if (slot == 45 && clicked.getType() == Material.ARROW) { GlobalSettingsMenu.open(player); return; }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int p = GlobalPlayerListMenu.getPage(player.getUniqueId());
            GlobalPlayerListMenu.openSpeakers(player, slot == 47 ? p - 1 : p + 1); return;
        }
        if (slot == 49 && clicked.getType() == Material.EMERALD) { GlobalPlayerListMenu.openAddSpeaker(player); return; }
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID uuid = getPlayerUuidFromSkull(clicked);
            if (uuid != null) {
                g.removeSpeaker(uuid);
                g.save();
                OfflinePlayer t = Bukkit.getOfflinePlayer(uuid);
                String name = t.getName() != null ? t.getName() : uuid.toString().substring(0, 8);
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.global.speakers.removed",
                        "&a%s removido dos speakers globais",
                        name
                ));
                Voicechat.activityLogger.log(player.getName() + " removeu " + name + " dos speakers globais");
                GlobalPlayerListMenu.openSpeakers(player, GlobalPlayerListMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handleGlobalAddAllowedClick(Player player, ItemStack clicked, int slot) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        if (slot == 45 && clicked.getType() == Material.ARROW) { GlobalPlayerListMenu.openAllowed(player, GlobalPlayerListMenu.getPage(player.getUniqueId())); return; }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int p = GlobalPlayerListMenu.getPage(player.getUniqueId());
            GlobalPlayerListMenu.openAddAllowed(player, slot == 47 ? p - 1 : p + 1); return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID uuid = getPlayerUuidFromSkull(clicked);
            if (uuid != null) {
                g.addAllowedPlayer(uuid);
                g.save();
                OfflinePlayer t = Bukkit.getOfflinePlayer(uuid);
                String name = t.getName() != null ? t.getName() : uuid.toString().substring(0, 8);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_permitidos_adicionado, name));
                Voicechat.activityLogger.log(player.getName() + " adicionou " + name + " aos permitidos globais");
                GlobalPlayerListMenu.openAllowed(player, GlobalPlayerListMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handleGlobalAddMutedClick(Player player, ItemStack clicked, int slot) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        if (slot == 45 && clicked.getType() == Material.ARROW) { GlobalPlayerListMenu.openMuted(player, GlobalPlayerListMenu.getPage(player.getUniqueId())); return; }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int p = GlobalPlayerListMenu.getPage(player.getUniqueId());
            GlobalPlayerListMenu.openAddMuted(player, slot == 47 ? p - 1 : p + 1); return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID uuid = getPlayerUuidFromSkull(clicked);
            if (uuid != null) {
                g.addMutedPlayer(uuid);
                g.save();
                OfflinePlayer t = Bukkit.getOfflinePlayer(uuid);
                String name = t.getName() != null ? t.getName() : uuid.toString().substring(0, 8);
                player.sendMessage(String.format(Voicechat.MESSAGES.gui_mutados_mutado, name));
                Voicechat.activityLogger.log(player.getName() + " mutou " + name + " na global");
                GlobalPlayerListMenu.openMuted(player, GlobalPlayerListMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private void handleGlobalAddSpeakerClick(Player player, ItemStack clicked, int slot) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        if (slot == 45 && clicked.getType() == Material.ARROW) { GlobalPlayerListMenu.openSpeakers(player, GlobalPlayerListMenu.getPage(player.getUniqueId())); return; }
        if ((slot == 47 || slot == 51) && clicked.getType() == Material.SPECTRAL_ARROW) {
            int p = GlobalPlayerListMenu.getPage(player.getUniqueId());
            GlobalPlayerListMenu.openAddSpeaker(player, slot == 47 ? p - 1 : p + 1); return;
        }
        if (clicked.getType() == Material.PLAYER_HEAD) {
            UUID uuid = getPlayerUuidFromSkull(clicked);
            if (uuid != null) {
                g.addSpeaker(uuid);
                g.save();
                OfflinePlayer t = Bukkit.getOfflinePlayer(uuid);
                String name = t.getName() != null ? t.getName() : uuid.toString().substring(0, 8);
                player.sendMessage(Voicechat.MESSAGES.format(
                        "gui.global.speakers.added",
                        "&a%s adicionado como speaker global",
                        name
                ));
                Voicechat.activityLogger.log(player.getName() + " adicionou " + name + " como speaker global");
                GlobalPlayerListMenu.openSpeakers(player, GlobalPlayerListMenu.getPage(player.getUniqueId()));
            }
        }
    }

    private String globalRegionName() {
        return ChatColor.stripColor(Voicechat.MESSAGES.text("gui.global_region.name", "Regiao Global"));
    }

    private void teleportToZone(Player player, RestrictedZone zone) {
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) {
            player.sendMessage(Voicechat.MESSAGES.format(
                    "gui.zone.teleport.world_missing",
                    "&cO mundo da zona '%s' nao esta carregado.",
                    zone.getName()
            ));
            return;
        }

        Location target = findZoneTeleportLocation(player, zone, world);
        if (target == null) {
            player.sendMessage(Voicechat.MESSAGES.format(
                    "gui.zone.teleport.failed",
                    "&cNao foi possivel encontrar um ponto seguro para a zona '%s'.",
                    zone.getName()
            ));
            return;
        }

        player.closeInventory();
        if (player.teleport(target)) {
            player.sendMessage(Voicechat.MESSAGES.format(
                    "gui.zone.teleport.success",
                    "&aTeleportado para a zona '%s'.",
                    zone.getName()
            ));
            Voicechat.activityLogger.log(player.getName() + " teleportou-se para a zona " + zone.getName());
            return;
        }

        player.sendMessage(Voicechat.MESSAGES.format(
                "gui.zone.teleport.failed",
                "&cNao foi possivel encontrar um ponto seguro para a zona '%s'.",
                zone.getName()
        ));
    }

    private Location findZoneTeleportLocation(Player player, RestrictedZone zone, World world) {
        int centerX = (zone.getMinX() + zone.getMaxX()) / 2;
        int centerZ = (zone.getMinZ() + zone.getMaxZ()) / 2;
        int maxRadius = Math.min(6, Math.max(
                (zone.getMaxX() - zone.getMinX()) / 2,
                (zone.getMaxZ() - zone.getMinZ()) / 2
        ));

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = clamp(centerX + dx, zone.getMinX(), zone.getMaxX());
                    int z = clamp(centerZ + dz, zone.getMinZ(), zone.getMaxZ());
                    Location location = findSafeLocationInColumn(player, zone, world, x, z);
                    if (location != null) {
                        return location;
                    }
                }
            }
        }

        int fallbackX = clamp(centerX, zone.getMinX(), zone.getMaxX());
        int fallbackZ = clamp(centerZ, zone.getMinZ(), zone.getMaxZ());
        int highestY = world.getHighestBlockYAt(fallbackX, fallbackZ) + 1;
        return withPlayerFacing(player, new Location(world, fallbackX + 0.5D, highestY, fallbackZ + 0.5D));
    }

    private Location findSafeLocationInColumn(Player player, RestrictedZone zone, World world, int x, int z) {
        int minY = Math.max(1, zone.getMinY());
        for (int y = zone.getMaxY(); y >= minY; y--) {
            if (isSafeStandLocation(world, x, y, z)) {
                return withPlayerFacing(player, new Location(world, x + 0.5D, y, z + 0.5D));
            }
        }
        return null;
    }

    private boolean isSafeStandLocation(World world, int x, int y, int z) {
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block below = world.getBlockAt(x, y - 1, z);
        return feet.isEmpty() && head.isEmpty() && below.getType().isSolid();
    }

    private Location withPlayerFacing(Player player, Location location) {
        Location current = player.getLocation();
        location.setYaw(current.getYaw());
        location.setPitch(current.getPitch());
        return location;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
