package de.maxhenkel.voicechat.zone.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.zone.RestrictedZone;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneMutedPlayersMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();
    private static final String S = ChatColor.DARK_GRAY + "-------------------------";

    public static String getTitlePrefix() {
        return Voicechat.MESSAGES.gui_mutados_prefixo;
    }

    public static void open(Player player, RestrictedZone zone) {
        open(player, zone, 0);
    }

    public static void open(Player player, RestrictedZone zone, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, getTitlePrefix() + zone.getName());

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
            headerMeta.setDisplayName(Voicechat.MESSAGES.gui_zona_jogadores_mutados);
            headerMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_muted.line1", "&7 > &fLista quem fica impedido de transmitir"),
                    Voicechat.MESSAGES.text("gui.zone_muted.line2", "&7   &fvoz nesta zona, independentemente do resto."),
                    Voicechat.MESSAGES.format("gui.zone_muted.current_count", "&7 > &eAtualmente&8 - &f%s", zone.getMutedPlayers().size()),
                    "",
                    S
            ));
            header.setItemMeta(headerMeta);
        }
        inv.setItem(4, header);

        List<UUID> allPlayers = new ArrayList<>(zone.getMutedPlayers());
        allPlayers.sort(Comparator.comparing(ZoneMutedPlayersMenu::resolvePlayerName, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil((double) allPlayers.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPages.put(player.getUniqueId(), page);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allPlayers.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            UUID uuid = allPlayers.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(offlinePlayer);
                String name = resolvePlayerName(uuid);
                skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.zone_muted.item_name", "&c* &f%s", name));
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.format("gui.zone_muted.uuid_line", "&7 > &eUUID&8 - &7%s...", uuid.toString().substring(0, 8)),
                        Voicechat.MESSAGES.text("gui.zone_muted.player_line1", "&7 > &fEste jogador nao consegue falar"),
                        Voicechat.MESSAGES.text("gui.zone_muted.player_line2", "&7   &fenquanto estiver dentro desta zona."),
                        "",
                        S,
                        Voicechat.MESSAGES.gui_mutados_clique_desmutar
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot, head);
            slot++;
        }

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    Voicechat.MESSAGES.text("gui.zone_muted.nav.prev_name", "&e< Anterior"),
                    Voicechat.MESSAGES.format("gui.zone_muted.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    Voicechat.MESSAGES.text("gui.zone_muted.nav.next_name", "&eProxima >"),
                    Voicechat.MESSAGES.format("gui.zone_muted.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
            ));
        }

        ItemStack addPlayer = new ItemStack(Material.REDSTONE);
        ItemMeta addMeta = addPlayer.getItemMeta();
        if (addMeta != null) {
            addMeta.setDisplayName(Voicechat.MESSAGES.gui_mutados_mutar);
            addMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.zone_muted.add.line1", "&7 > &fAbre a lista de jogadores online que"),
                    Voicechat.MESSAGES.text("gui.zone_muted.add.line2", "&7   &fainda nao estao bloqueados aqui."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.zone_muted.add.action", "&c > &eClique para selecionar")
            ));
            addPlayer.setItemMeta(addMeta);
        }
        inv.setItem(49, addPlayer);

        inv.setItem(45, createBackButton(Voicechat.MESSAGES.text("gui.zone_muted.back_lore", "&7 > &fVoltar para config. da zona")));

        player.openInventory(inv);
    }

    public static int getPage(UUID playerUuid) {
        return playerPages.getOrDefault(playerUuid, 0);
    }

    private static String resolvePlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
    }

    private static ItemStack createBackButton(String lore) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(Voicechat.MESSAGES.text("gui.zone_muted.back_name", "&c< &fVoltar"));
            backMeta.setLore(Arrays.asList("", lore));
            back.setItemMeta(backMeta);
        }
        return back;
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
