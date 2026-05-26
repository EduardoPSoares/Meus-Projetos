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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RangeListMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";
    private static final int ITEMS_PER_PAGE = 36;
    private static final Map<UUID, Integer> pages = new ConcurrentHashMap<>();

    public static String getTitle() {
        return Voicechat.MESSAGES.gui_range_titulo;
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        Map<UUID, Float> allRanges = Voicechat.playerRangeManager.getAllRanges();
        List<Map.Entry<UUID, Float>> entries = new ArrayList<>(allRanges.entrySet());
        entries.sort(Comparator
                .comparing((Map.Entry<UUID, Float> entry) -> resolvePlayerName(entry.getKey()), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.getKey().toString()));
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        pages.put(player.getUniqueId(), page);
        float defaultRange = de.maxhenkel.voicechat.voice.common.Utils.getDefaultDistance();
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
            infoMeta.setDisplayName(Voicechat.MESSAGES.gui_range_info_titulo);
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.range_list.info.line1", "&7 > &fGerencia alcances individuais de voz."),
                    Voicechat.MESSAGES.text("gui.range_list.info.line2", "&7 > &fUse quando um jogador precisa falar"),
                    Voicechat.MESSAGES.text("gui.range_list.info.line3", "&7   &fmais longe ou mais perto que o padrao."),
                    "",
                    String.format(Voicechat.MESSAGES.gui_range_info_total, allRanges.size()),
                    String.format(Voicechat.MESSAGES.gui_range_range_padrao, String.valueOf(defaultRange)),
                    Voicechat.MESSAGES.format("gui.range_list.info.page", "&7 > &ePagina&8 - &f%s/%s", page + 1, totalPages),
                    "",
                    Voicechat.MESSAGES.text("gui.range_list.info.line4", "&7 > &fA voz global fica em um menu separado."),
                    "",
                    S
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        if (allRanges.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(Voicechat.MESSAGES.gui_range_vazio);
                emptyMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.range_list.empty.line1", "&7 > &fNao existem jogadores com range customizado."),
                        Voicechat.MESSAGES.text("gui.range_list.empty.line2", "&7 > &fUse o botao inferior para escolher um"),
                        Voicechat.MESSAGES.text("gui.range_list.empty.line3", "&7   &fjogador e criar a configuracao."),
                        "",
                        S
                ));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        } else {
            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, entries.size());
            int slot = 9;
            for (int i = start; i < end; i++) {
                Map.Entry<UUID, Float> entry = entries.get(i);

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setOwningPlayer(offlinePlayer);
                    String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : entry.getKey().toString().substring(0, 8);
                    skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.range_list.item_name", "&6* &f%s", name));
                    skullMeta.setLore(Arrays.asList(
                            "",
                            S,
                            "",
                            String.format(Voicechat.MESSAGES.gui_range_jogador_range, String.valueOf(entry.getValue())),
                            Voicechat.MESSAGES.format("gui.range_list.uuid_line", "&7 > &eUUID&8 - &7%s...", entry.getKey().toString().substring(0, 8)),
                            "",
                            Voicechat.MESSAGES.text("gui.range_list.player_line1", "&7 > &fClique para abrir a tela de"),
                            Voicechat.MESSAGES.text("gui.range_list.player_line2", "&7   &fdistancias deste jogador."),
                            "",
                            S,
                            Voicechat.MESSAGES.gui_range_clique_gerenciar
                    ));
                    head.setItemMeta(skullMeta);
                }
                inv.setItem(slot, head);
                slot++;
            }
        }

        inv.setItem(45, createItem(Material.ARROW,
                Voicechat.MESSAGES.text("gui.range_list.back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.range_list.back_lore", "&7 > &fRetorna ao painel administrativo")));

        inv.setItem(46, createItem(Material.EMERALD,
                Voicechat.MESSAGES.gui_range_definir,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.range_list.define.line1", "&7 > &fEscolhe um jogador online para definir"),
                Voicechat.MESSAGES.text("gui.range_list.define.line2", "&7   &fou alterar o range personalizado dele."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.range_list.define.action", "&a > &eClique para selecionar")));

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    Voicechat.MESSAGES.text("gui.range_list.nav.prev_name", "&e< Anterior"),
                    Voicechat.MESSAGES.format("gui.range_list.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }

        int globalCount = Voicechat.playerRangeManager.getGlobalPlayers().size();
        inv.setItem(49, createItem(Material.ENDER_EYE,
                Voicechat.MESSAGES.gui_global_botao,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.range_list.global.line1", "&7 > &fAbre a lista de jogadores com voz global,"),
                Voicechat.MESSAGES.text("gui.range_list.global.line2", "&7   &fque ignoram a distancia normal."),
                String.format(Voicechat.MESSAGES.gui_global_botao_desc, globalCount),
                "",
                S,
                Voicechat.MESSAGES.text("gui.range_list.global.action", "&a > &eClique para gerenciar")));

        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    Voicechat.MESSAGES.text("gui.range_list.nav.next_name", "&eProxima >"),
                    Voicechat.MESSAGES.format("gui.range_list.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
            ));
        }

        inv.setItem(52, createItem(Material.GLOWSTONE_DUST,
                Voicechat.MESSAGES.gui_range_recarregar,
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.range_list.reload.line1", "&7 > &fReler a configuracao de ranges do disco."),
                Voicechat.MESSAGES.text("gui.range_list.reload.line2", "&7 > &fUse depois de editar arquivos manualmente"),
                Voicechat.MESSAGES.text("gui.range_list.reload.line3", "&7   &fou quando quiser sincronizar o estado."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.range_list.reload.action", "&a > &eClique para recarregar")));

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
