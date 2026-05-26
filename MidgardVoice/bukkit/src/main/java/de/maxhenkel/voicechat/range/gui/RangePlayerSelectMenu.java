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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RangePlayerSelectMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";
    private static final int ITEMS_PER_PAGE = 36;
    private static final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public static String getTitlePrefix() {
        return Voicechat.MESSAGES.gui_range_selecionar_prefixo;
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, getTitlePrefix());

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
            headerMeta.setDisplayName(Voicechat.MESSAGES.gui_range_selecionar_titulo);
            headerMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.range.select.line1", "&7 > &fEscolha o jogador que recebera um"),
                    Voicechat.MESSAGES.text("gui.range.select.line2", "&7   &frange de voz personalizado."),
                    Voicechat.MESSAGES.text("gui.range.select.line3", "&7 > &fDepois do clique, voce abrira a tela"),
                    Voicechat.MESSAGES.text("gui.range.select.line4", "&7   &fcom os alcances disponiveis."),
                    "",
                    S
            ));
            header.setItemMeta(headerMeta);
        }
        inv.setItem(4, header);

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil((double) onlinePlayers.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        pages.put(player.getUniqueId(), page);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, onlinePlayers.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            Player online = onlinePlayers.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(online);
                skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.range.select.item_name", "&6* &f%s", online.getName()));

                Float currentRange = Voicechat.playerRangeManager.getRange(online.getUniqueId());
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        currentRange != null
                                ? String.format(Voicechat.MESSAGES.gui_range_jogador_range, String.valueOf(currentRange))
                                : Voicechat.MESSAGES.gui_range_sem_custom_gui,
                        Voicechat.MESSAGES.text("gui.range.select.player_action", "&7 > &fClique para abrir a selecao de distancia."),
                        "",
                        S,
                        Voicechat.MESSAGES.gui_range_clique_selecionar
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot, head);
            slot++;
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(Voicechat.MESSAGES.text("gui.range.select.back_name", "&c< &fVoltar"));
            backMeta.setLore(Arrays.asList(
                    "",
                    Voicechat.MESSAGES.text("gui.range.select.back_lore", "&7 > &fRetorna para a lista de ranges")));
            back.setItemMeta(backMeta);
        }
        inv.setItem(45, back);

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    Voicechat.MESSAGES.text("gui.range.select.nav.prev_name", "&e< Anterior"),
                    Voicechat.MESSAGES.format("gui.range.select.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    Voicechat.MESSAGES.text("gui.range.select.nav.next_name", "&eProxima >"),
                    Voicechat.MESSAGES.format("gui.range.select.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
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
