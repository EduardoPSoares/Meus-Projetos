package de.maxhenkel.voicechat.range.gui;

import de.maxhenkel.voicechat.Voicechat;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RangeGlobalAddMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";
    private static final int ITEMS_PER_PAGE = 36;
    private static final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public static String getTitle() {
        return Voicechat.MESSAGES.gui_global_selecionar_titulo;
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, getTitle());

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
            headerMeta.setDisplayName(Voicechat.MESSAGES.gui_global_adicionar);
            headerMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_add.line1", "&7 > &fEscolha quem podera transmitir para"),
                    Voicechat.MESSAGES.text("gui.global_add.line2", "&7   &ftodo o servidor ignorando distancia."),
                    Voicechat.MESSAGES.text("gui.global_add.line3", "&7 > &fA alteracao passa a valer logo apos o clique."),
                    "",
                    S
            ));
            header.setItemMeta(headerMeta);
        }
        inv.setItem(4, header);

        List<Player> selectablePlayers = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (Voicechat.playerRangeManager.isGlobalPlayer(online.getUniqueId())) {
                continue;
            }
            selectablePlayers.add(online);
        }
        selectablePlayers.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int) Math.ceil((double) selectablePlayers.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        pages.put(player.getUniqueId(), page);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, selectablePlayers.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            Player online = selectablePlayers.get(i);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(online);
                skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.global_add.item_name", "&6* &f%s", online.getName()));
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.global_add.player_line1", "&7 > &fAo adicionar, este jogador podera"),
                        Voicechat.MESSAGES.text("gui.global_add.player_line2", "&7   &fser ouvido em qualquer lugar do servidor."),
                        "",
                        S,
                        Voicechat.MESSAGES.gui_global_clique_adicionar
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot, head);
            slot++;
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(Voicechat.MESSAGES.text("gui.global_add.back_name", "&c< &fVoltar"));
            backMeta.setLore(Arrays.asList(
                    "",
                    Voicechat.MESSAGES.text("gui.global_add.back_lore", "&7 > &fRetorna para a lista global")));
            back.setItemMeta(backMeta);
        }
        inv.setItem(45, back);

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    Voicechat.MESSAGES.text("gui.global_add.nav.prev_name", "&e< Anterior"),
                    Voicechat.MESSAGES.format("gui.global_add.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    Voicechat.MESSAGES.text("gui.global_add.nav.next_name", "&eProxima >"),
                    Voicechat.MESSAGES.format("gui.global_add.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
            ));
        }

        player.openInventory(inv);
    }

    public static int getPage(UUID playerUuid) {
        return pages.getOrDefault(playerUuid, 0);
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
