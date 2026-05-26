package de.maxhenkel.voicechat.range.gui;

import de.maxhenkel.voicechat.Voicechat;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RangeGlobalMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";
    private static final int ITEMS_PER_PAGE = 36;
    private static final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public static String getTitle() {
        return Voicechat.MESSAGES.gui_global_titulo;
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        Set<UUID> globalPlayers = Voicechat.playerRangeManager.getGlobalPlayers();
        List<UUID> sortedPlayers = new ArrayList<>(globalPlayers);
        sortedPlayers.sort(Comparator
                .comparing(RangeGlobalMenu::resolvePlayerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(UUID::toString));
        int totalPages = Math.max(1, (int) Math.ceil((double) sortedPlayers.size() / ITEMS_PER_PAGE));
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
            infoMeta.setDisplayName(Voicechat.MESSAGES.gui_global_info_titulo);
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_menu.info.line1", "&7 > &fLista quem pode ser ouvido globalmente."),
                    Voicechat.MESSAGES.text("gui.global_menu.info.line2", "&7 > &fJogadores nesta lista ignoram a distancia"),
                    Voicechat.MESSAGES.text("gui.global_menu.info.line3", "&7   &fnormal de proximidade ao transmitir voz."),
                    "",
                    String.format(Voicechat.MESSAGES.gui_global_info_total, globalPlayers.size()),
                    Voicechat.MESSAGES.format("gui.global_menu.info.page", "&7 > &ePagina&8 - &f%s/%s", page + 1, totalPages),
                    "",
                    Voicechat.MESSAGES.gui_global_info_desc,
                    "",
                    S
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        if (globalPlayers.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(Voicechat.MESSAGES.gui_global_vazio);
                emptyMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.global_menu.empty.line1", "&7 > &fNenhum jogador possui voz global agora."),
                        Voicechat.MESSAGES.text("gui.global_menu.empty.line2", "&7 > &fUse o botao inferior para adicionar"),
                        Voicechat.MESSAGES.text("gui.global_menu.empty.line3", "&7   &fum jogador online a lista."),
                        "",
                        S
                ));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        } else {
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, sortedPlayers.size());
            int slot = 9;
            for (int i = start; i < end; i++) {
                UUID uuid = sortedPlayers.get(i);

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setOwningPlayer(offlinePlayer);
                    String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString().substring(0, 8);
                    skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.global_menu.item_name", "&6* &f%s", name));
                    skullMeta.setLore(Arrays.asList(
                            "",
                            S,
                            "",
                            Voicechat.MESSAGES.gui_global_jogador_desc,
                            Voicechat.MESSAGES.format("gui.global_menu.uuid_line", "&7 > &eUUID&8 - &7%s...", uuid.toString().substring(0, 8)),
                            "",
                            Voicechat.MESSAGES.text("gui.global_menu.player_line1", "&7 > &fAo remover, o jogador volta a"),
                            Voicechat.MESSAGES.text("gui.global_menu.player_line2", "&7   &fdepender do alcance normal de voz."),
                            "",
                            S,
                            Voicechat.MESSAGES.gui_global_clique_remover
                    ));
                    head.setItemMeta(skullMeta);
                }
                inv.setItem(slot, head);
                slot++;
            }
        }

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    Voicechat.MESSAGES.text("gui.global_menu.nav.prev_name", "&e< Anterior"),
                    Voicechat.MESSAGES.format("gui.global_menu.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }

        inv.setItem(49, createItem(Material.EMERALD,
                Voicechat.MESSAGES.gui_global_adicionar,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.global_menu.add.line1", "&7 > &fAbre a lista de jogadores online que ainda"),
                Voicechat.MESSAGES.text("gui.global_menu.add.line2", "&7   &fnao possuem voz global configurada."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.global_menu.add.action", "&a > &eClique para selecionar")));

        inv.setItem(45, createItem(Material.ARROW,
                Voicechat.MESSAGES.text("gui.global_menu.back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.gui_global_voltar_lista));

        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    Voicechat.MESSAGES.text("gui.global_menu.nav.next_name", "&eProxima >"),
                    Voicechat.MESSAGES.format("gui.global_menu.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
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

    private static String resolvePlayerName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
    }
}
