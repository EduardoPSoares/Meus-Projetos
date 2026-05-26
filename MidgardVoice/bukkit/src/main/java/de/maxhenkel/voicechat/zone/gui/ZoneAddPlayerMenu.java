package de.maxhenkel.voicechat.zone.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.zone.RestrictedZone;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneAddPlayerMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();
    private static final String S = ChatColor.DARK_GRAY + "-------------------------";

    public static String getTitleAllowedPrefix() {
        return Voicechat.MESSAGES.gui_add_permitido_prefixo;
    }

    public static String getTitleMutePrefix() {
        return Voicechat.MESSAGES.gui_add_mutado_prefixo;
    }

    public static void openForAllowed(Player player, RestrictedZone zone) {
        open(player, zone, getTitleAllowedPrefix() + zone.getName(), true, 0);
    }

    public static void openForAllowed(Player player, RestrictedZone zone, int page) {
        open(player, zone, getTitleAllowedPrefix() + zone.getName(), true, page);
    }

    public static void openForMuted(Player player, RestrictedZone zone) {
        open(player, zone, getTitleMutePrefix() + zone.getName(), false, 0);
    }

    public static void openForMuted(Player player, RestrictedZone zone, int page) {
        open(player, zone, getTitleMutePrefix() + zone.getName(), false, page);
    }

    private static void open(Player player, RestrictedZone zone, String title, boolean forAllowed, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack glass = createGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        ItemStack header = new ItemStack(Material.BOOK);
        ItemMeta headerMeta = header.getItemMeta();
        if (headerMeta != null) {
            headerMeta.setDisplayName(forAllowed ? Voicechat.MESSAGES.gui_permitidos_adicionar : Voicechat.MESSAGES.gui_mutados_mutar);
            headerMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text(
                            forAllowed ? "gui.zone_add.allowed_desc" : "gui.zone_add.muted_desc",
                            forAllowed
                                    ? "&7 > &fEscolha um jogador para liberar fala nesta zona."
                                    : "&7 > &fEscolha um jogador para bloquear fala nesta zona."
                    ),
                    Voicechat.MESSAGES.text("gui.zone_add.apply_line", "&7 > &fA alteracao vale imediatamente apos o clique."),
                    "",
                    S
            ));
            header.setItemMeta(headerMeta);
        }
        inv.setItem(4, header);

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        List<Player> filtered = new ArrayList<>();
        for (Player online : onlinePlayers) {
            if (online.equals(player)) {
                continue;
            }
            if (forAllowed && zone.isAllowedPlayer(online.getUniqueId())) {
                continue;
            }
            if (!forAllowed && zone.isMutedPlayer(online.getUniqueId())) {
                continue;
            }
            filtered.add(online);
        }
        filtered.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), page);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filtered.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            Player online = filtered.get(i);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(online);
                skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.zone_add.item_name", "&6* &f%s", online.getName()));
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text(
                                forAllowed ? "gui.zone_add.allowed_player_line" : "gui.zone_add.muted_player_line",
                                forAllowed
                                        ? "&7 > &fEste jogador sera colocado na lista de permitidos."
                                        : "&7 > &fEste jogador sera colocado na lista de mutados."
                        ),
                        "",
                        S,
                        (forAllowed ? Voicechat.MESSAGES.gui_add_clique_permitir : Voicechat.MESSAGES.gui_add_clique_mutar)
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot, head);
            slot++;
        }

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    Voicechat.MESSAGES.text("gui.zone_add.nav.prev_name", "&e< Anterior"),
                    Voicechat.MESSAGES.format("gui.zone_add.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    Voicechat.MESSAGES.text("gui.zone_add.nav.next_name", "&eProxima >"),
                    Voicechat.MESSAGES.format("gui.zone_add.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
            ));
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(Voicechat.MESSAGES.text("gui.zone_add.back_name", "&c< &fVoltar"));
            backMeta.setLore(Arrays.asList(
                    "",
                    Voicechat.MESSAGES.text("gui.zone_add.back_lore", "&7 > &fRetorna para a lista anterior")));
            back.setItemMeta(backMeta);
        }
        inv.setItem(45, back);

        player.openInventory(inv);
    }

    public static int getPage(UUID playerUuid) {
        return playerPages.getOrDefault(playerUuid, 0);
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

    private static ItemStack createGlass() {
        ItemStack glass = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }
}
