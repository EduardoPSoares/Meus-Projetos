package de.maxhenkel.voicechat.zone.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.util.MessageFormatUtil;
import de.maxhenkel.voicechat.voice.common.Utils;
import de.maxhenkel.voicechat.zone.RestrictedZone;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneListMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";
    private static final int ITEMS_PER_PAGE = 35;
    private static final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public static String getTitle() {
        return Voicechat.MESSAGES.gui_zonas_titulo;
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        Collection<RestrictedZone> zones = Voicechat.restrictedZoneManager.getZones();
        List<RestrictedZone> sortedZones = new ArrayList<>(zones);
        sortedZones.sort(Comparator.comparing(RestrictedZone::getName, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil((double) sortedZones.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        pages.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, getTitle());

        ItemStack glass = createGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Voicechat.MESSAGES.gui_zonas_info_titulo);
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_list.info.line1", "&7 > &fGerencie todas as areas com regra propria."),
                    Voicechat.MESSAGES.text("gui.zone_list.info.line2", "&7 > &fCada zona pode controlar voz, speakers,"),
                    Voicechat.MESSAGES.text("gui.zone_list.info.line3", "&7   &fcooldown local, range e restricoes."),
                    "",
                    String.format(Voicechat.MESSAGES.gui_zonas_info_total, zones.size()),
                    Voicechat.MESSAGES.format("gui.zone_list.info.page", "&7 > &ePagina&8 - &f%s/%s", page + 1, totalPages),
                    "",
                    Voicechat.MESSAGES.text("gui.zone_list.info.line4", "&7 > &fClique em uma zona para abrir a configuracao."),
                    Voicechat.MESSAGES.text("gui.zone_list.info.line5", "&7 > &fUse Shift + clique para remover a zona."),
                    "",
                    S
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        ItemStack globalRegion = new ItemStack(Material.FILLED_MAP);
        ItemMeta globalMeta = globalRegion.getItemMeta();
        if (globalMeta != null) {
            globalMeta.setDisplayName(Voicechat.MESSAGES.text("gui.zone_list.global_name", "&b* &fRegiao Global"));
            String rangeStr = MessageFormatUtil.blocks((int) Utils.getDefaultDistance());
            String cooldownStr = Voicechat.globalZoneSettings != null
                    && Voicechat.globalZoneSettings.getGlobalCooldownMaxTalkTimeSec() > 0
                    && Voicechat.globalZoneSettings.getGlobalCooldownSec() > 0
                    ? MessageFormatUtil.cooldownCompact(
                            Voicechat.globalZoneSettings.getGlobalCooldownMaxTalkTimeSec(),
                            Voicechat.globalZoneSettings.getGlobalCooldownSec()
                    )
                    : Voicechat.MESSAGES.text("gui.zone_list.global_cooldown_disabled", "&7Desativado");
            List<String> globalLore = new ArrayList<>();
            globalLore.add("");
            globalLore.add(S);
            globalLore.add("");
            globalLore.add(Voicechat.MESSAGES.text("gui.zone_list.global_world", "&7 > &eMundo&8 - &fTodos"));
            globalLore.add(Voicechat.MESSAGES.text("gui.zone_list.global_scope", "&7 > &eEscopo&8 - &fTodo o servidor"));
            globalLore.add("");
            globalLore.add(Voicechat.MESSAGES.text("gui.zone_list.global_line1", "&7 > &fEstas regras valem quando o jogador"));
            globalLore.add(Voicechat.MESSAGES.text("gui.zone_list.global_line2", "&7   &fnao esta dentro de uma sub-zona."));
            globalLore.add("");
            globalLore.add(Voicechat.MESSAGES.format("gui.zone_list.global_range", "&7 > &eRange&8 - &f%s", rangeStr));
            globalLore.add(Voicechat.MESSAGES.format("gui.zone_list.global_cooldown", "&7 > &eCooldown&8 - &f%s", cooldownStr));
            globalLore.add("");
            globalLore.add(S);
            globalLore.add(Voicechat.MESSAGES.text("gui.zone_list.global_action", "&a > &eClique para gerenciar"));
            globalMeta.setLore(globalLore);
            globalRegion.setItemMeta(globalMeta);
        }
        inv.setItem(9, globalRegion);

        if (zones.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(Voicechat.MESSAGES.gui_zonas_vazio);
                emptyMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.zone_list.empty.line1", "&7 > &fNenhuma sub-zona foi criada ainda."),
                        Voicechat.MESSAGES.text("gui.zone_list.empty.line2", "&7 > &fCrie uma com o comando /mvoice create"),
                        Voicechat.MESSAGES.text("gui.zone_list.empty.line3", "&7   &f<nome> e depois abra este menu."),
                        "",
                        S
                ));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        } else {
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, sortedZones.size());
            int slot = 10;
            for (int i = start; i < end; i++) {
                RestrictedZone zone = sortedZones.get(i);

                ItemStack item = new ItemStack(Material.FILLED_MAP);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(Voicechat.MESSAGES.format("gui.zone_list.item_name", "&6* &f%s", zone.getName()));
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    lore.add(S);
                    lore.add("");
                    lore.add(String.format(Voicechat.MESSAGES.gui_zona_mundo, zone.getWorld()));
                    lore.add(String.format(Voicechat.MESSAGES.gui_zona_de, zone.getMinX() + ", " + zone.getMinY() + ", " + zone.getMinZ()));
                    lore.add(String.format(Voicechat.MESSAGES.gui_zona_ate, zone.getMaxX() + ", " + zone.getMaxY() + ", " + zone.getMaxZ()));
                    lore.add("");
                    lore.add(Voicechat.MESSAGES.format(
                            "gui.zone_list.voice_status",
                            "&7 > &eVoz&8 - %s",
                            zone.isVoiceEnabled()
                                    ? Voicechat.MESSAGES.text("gui.zone_list.voice_enabled", "&aAtivada")
                                    : Voicechat.MESSAGES.text("gui.zone_list.voice_disabled", "&cDesativada")
                    ));
                    if (zone.isListenOnly()) {
                        lore.add(Voicechat.MESSAGES.text("gui.zone_list.listen_only", "&7 > &eModo&8 - &bSomente escuta"));
                    }
                    if (zone.isStageMode()) {
                        lore.add(Voicechat.MESSAGES.format("gui.zone_list.stage", "&7 > &eStage&8 - &f%s speakers", zone.getSpeakers().size()));
                    }
                    if (zone.hasCustomRange()) {
                        lore.add(Voicechat.MESSAGES.format("gui.zone_list.range", "&7 > &eRange&8 - &f%s blocos", (int) zone.getCustomRange()));
                    }
                    if (zone.getRangeMultiplier() != 1.0f) {
                        lore.add(Voicechat.MESSAGES.format("gui.zone_list.multiplier", "&7 > &eMultiplicador&8 - &f%s", String.format("%.1fx", zone.getRangeMultiplier())));
                    }
                    if (zone.hasZoneCooldown()) {
                        lore.add(Voicechat.MESSAGES.format(
                                "gui.zone_list.cooldown",
                                "&7 > &eCooldown&8 - &f%s/%s",
                                MessageFormatUtil.seconds(zone.getZoneCooldownMaxTalkTimeSec()),
                                MessageFormatUtil.seconds(zone.getZoneCooldownSec())
                        ));
                    }
                    if (zone.isTemporary()) {
                        long remaining = zone.getExpiresAt() - System.currentTimeMillis();
                        lore.add(Voicechat.MESSAGES.format(
                                "gui.zone_list.duration",
                                "&7 > &eDuracao&8 - %s",
                                remaining > 0
                                        ? Voicechat.MESSAGES.format("gui.zone_list.duration_value", "&f%s", MessageFormatUtil.duration(remaining))
                                        : Voicechat.MESSAGES.text("gui.zone_list.expired", "&4Expirada")
                        ));
                    }
                    lore.add(String.format(Voicechat.MESSAGES.gui_zona_permitidos, zone.getAllowedPlayers().size()));
                    lore.add(String.format(Voicechat.MESSAGES.gui_zona_mutados, zone.getMutedPlayers().size()));
                    lore.add("");
                    lore.add(S);
                    lore.add(Voicechat.MESSAGES.text("gui.zone_list.item_action", "&a > &eClique&8 - &fAbrir configuracao"));
                    lore.add(Voicechat.MESSAGES.text("gui.zone_list.item_remove_action", "&c > &eShift + clique&8 - &fRemover zona"));
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
                slot++;
            }
        }

        inv.setItem(45, createItem(Material.ARROW,
                Voicechat.MESSAGES.text("gui.zone_list.back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.zone_list.back_lore", "&7 > &fRetorna ao painel administrativo")));

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    Voicechat.MESSAGES.text("gui.zone_list.nav.prev_name", "&e< Anterior"),
                    Voicechat.MESSAGES.format("gui.zone_list.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }

        inv.setItem(49, createItem(Material.EMERALD,
                Voicechat.MESSAGES.gui_zona_recarregar,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.zone_list.reload.line1", "&7 > &fReler zonas e configuracoes do disco."),
                Voicechat.MESSAGES.text("gui.zone_list.reload.line2", "&7 > &fUse depois de editar arquivos manualmente"),
                Voicechat.MESSAGES.text("gui.zone_list.reload.line3", "&7   &fou para sincronizar tudo novamente."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.zone_list.reload.action", "&a > &eClique para recarregar")));

        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    Voicechat.MESSAGES.text("gui.zone_list.nav.next_name", "&eProxima >"),
                    Voicechat.MESSAGES.format("gui.zone_list.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
            ));
        }

        player.openInventory(inv);
    }

    public static int getPage(UUID playerUuid) {
        return pages.getOrDefault(playerUuid, 0);
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createGlass() {
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private static ItemStack createNavButton(String name, String lore) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList("", lore));
            item.setItemMeta(meta);
        }
        return item;
    }

}
