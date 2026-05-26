package de.maxhenkel.voicechat.zone.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.zone.GlobalZoneSettings;
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

public class GlobalPlayerListMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final Map<UUID, Integer> pages = new ConcurrentHashMap<>();
    private static final String S = ChatColor.DARK_GRAY + "-------------------------";

    public static String getAllowedTitle() {
        return Voicechat.MESSAGES.text("gui.global_players.allowed_title", "&5Global Permitidos");
    }

    public static String getMutedTitle() {
        return Voicechat.MESSAGES.text("gui.global_players.muted_title", "&5Global Mutados");
    }

    public static String getSpeakersTitle() {
        return Voicechat.MESSAGES.text("gui.global_players.speakers_title", "&5Global Speakers");
    }

    public static String getAddAllowedTitle() {
        return Voicechat.MESSAGES.text("gui.global_players.add_allowed_title", "&5Adicionar Permitido Global");
    }

    public static String getAddMutedTitle() {
        return Voicechat.MESSAGES.text("gui.global_players.add_muted_title", "&5Adicionar Mutado Global");
    }

    public static String getAddSpeakerTitle() {
        return Voicechat.MESSAGES.text("gui.global_players.add_speaker_title", "&5Adicionar Speaker Global");
    }

    public static void openAllowed(Player player) {
        openAllowed(player, 0);
    }

    public static void openAllowed(Player player, int page) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        openList(player, getAllowedTitle(), new ArrayList<>(g.getAllowedPlayers()), page,
                Voicechat.MESSAGES.gui_zona_jogadores_permitidos,
                Voicechat.MESSAGES.text("gui.global_players.allowed_desc1", "&7 > &fLista quem pode falar mesmo se a regra"),
                Voicechat.MESSAGES.text("gui.global_players.allowed_desc2", "&7   &fglobal estiver bloqueada."),
                Voicechat.MESSAGES.gui_permitidos_clique_remover,
                Voicechat.MESSAGES.text("gui.global_players.allowed_prefix", "&6* "),
                Voicechat.MESSAGES.gui_permitidos_adicionar,
                Material.EMERALD);
    }

    public static void openMuted(Player player) {
        openMuted(player, 0);
    }

    public static void openMuted(Player player, int page) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        openList(player, getMutedTitle(), new ArrayList<>(g.getMutedPlayers()), page,
                Voicechat.MESSAGES.gui_zona_jogadores_mutados,
                Voicechat.MESSAGES.text("gui.global_players.muted_desc1", "&7 > &fLista quem nao pode falar pela regra"),
                Voicechat.MESSAGES.text("gui.global_players.muted_desc2", "&7   &fglobal em lugar nenhum."),
                Voicechat.MESSAGES.gui_mutados_clique_desmutar,
                Voicechat.MESSAGES.text("gui.global_players.muted_prefix", "&c* "),
                Voicechat.MESSAGES.gui_mutados_mutar,
                Material.REDSTONE);
    }

    public static void openSpeakers(Player player) {
        openSpeakers(player, 0);
    }

    public static void openSpeakers(Player player, int page) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        openList(player, getSpeakersTitle(), new ArrayList<>(g.getSpeakers()), page,
                Voicechat.MESSAGES.text("gui.global_players.speakers_name", "&6* &fSpeakers Globais"),
                Voicechat.MESSAGES.text("gui.global_players.speakers_desc1", "&7 > &fLista quem pode falar com o stage"),
                Voicechat.MESSAGES.text("gui.global_players.speakers_desc2", "&7   &fglobal ativado."),
                Voicechat.MESSAGES.text("gui.global_players.speakers_remove", "&c > &eClique para remover"),
                Voicechat.MESSAGES.text("gui.global_players.speakers_prefix", "&6* "),
                Voicechat.MESSAGES.text("gui.global_players.add_speaker_name", "&a* &fAdicionar Speaker"),
                Material.EMERALD);
    }

    private static void openList(Player player, String title, List<UUID> playerList, int page,
                                 String headerName, String desc1, String desc2, String clickLore,
                                 String namePrefix, String addButtonName, Material addMaterial) {
        playerList.sort(Comparator.comparing(GlobalPlayerListMenu::resolvePlayerName, String.CASE_INSENSITIVE_ORDER));
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
            headerMeta.setDisplayName(headerName);
            headerMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    desc1,
                    desc2,
                        "",
                        Voicechat.MESSAGES.format("gui.global_players.current_count", "&7 > &eAtualmente&8 - &f%s", playerList.size()),
                        "",
                        S
                ));
            header.setItemMeta(headerMeta);
        }
        inv.setItem(4, header);

        int totalPages = Math.max(1, (int) Math.ceil((double) playerList.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        pages.put(player.getUniqueId(), page);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, playerList.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            UUID uuid = playerList.get(i);
            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(target);
                String name = resolvePlayerName(uuid);
                skullMeta.setDisplayName(namePrefix + ChatColor.WHITE + name);
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.format("gui.global_players.uuid_line", "&7 > &eUUID&8 - &7%s...", uuid.toString().substring(0, 8)),
                        Voicechat.MESSAGES.text("gui.global_players.item_line1", "&7 > &fAo remover, a regra global deixa"),
                        Voicechat.MESSAGES.text("gui.global_players.item_line2", "&7   &fde considerar esta excecao."),
                        "",
                        S,
                        clickLore
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot, head);
            slot++;
        }

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    Voicechat.MESSAGES.text("gui.global_players.nav.prev_name", "&e< Anterior"),
                    Voicechat.MESSAGES.format("gui.global_players.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    Voicechat.MESSAGES.text("gui.global_players.nav.next_name", "&eProxima >"),
                    Voicechat.MESSAGES.format("gui.global_players.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
            ));
        }

        ItemStack addBtn = new ItemStack(addMaterial);
        ItemMeta addMeta = addBtn.getItemMeta();
        if (addMeta != null) {
            addMeta.setDisplayName(addButtonName);
            addMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.global_players.add_button.line1", "&7 > &fAbre a selecao de jogadores online"),
                    Voicechat.MESSAGES.text("gui.global_players.add_button.line2", "&7   &fque ainda nao estao nesta lista."),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.global_players.add_button.action", "&a > &eClique para selecionar")
            ));
            addBtn.setItemMeta(addMeta);
        }
        inv.setItem(49, addBtn);

        inv.setItem(45, createBackButton(Voicechat.MESSAGES.text("gui.global_players.back_config_lore", "&7 > &fVoltar para config. global")));

        player.openInventory(inv);
    }

    public static void openAddAllowed(Player player) {
        openAddAllowed(player, 0);
    }

    public static void openAddAllowed(Player player, int page) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        openAdd(player, getAddAllowedTitle(), page, uuid -> !g.isAllowedPlayer(uuid),
                Voicechat.MESSAGES.gui_permitidos_adicionar,
                Voicechat.MESSAGES.text("gui.global_players.add_allowed_desc", "&7 > &fSelecione quem sera liberado globalmente."),
                Voicechat.MESSAGES.gui_add_clique_permitir);
    }

    public static void openAddMuted(Player player) {
        openAddMuted(player, 0);
    }

    public static void openAddMuted(Player player, int page) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        openAdd(player, getAddMutedTitle(), page, uuid -> !g.isMutedPlayer(uuid),
                Voicechat.MESSAGES.gui_mutados_mutar,
                Voicechat.MESSAGES.text("gui.global_players.add_muted_desc", "&7 > &fSelecione quem sera bloqueado globalmente."),
                Voicechat.MESSAGES.gui_add_clique_mutar);
    }

    public static void openAddSpeaker(Player player) {
        openAddSpeaker(player, 0);
    }

    public static void openAddSpeaker(Player player, int page) {
        GlobalZoneSettings g = Voicechat.globalZoneSettings;
        openAdd(player, getAddSpeakerTitle(), page, uuid -> !g.isSpeaker(uuid),
                Voicechat.MESSAGES.text("gui.global_players.add_speaker_header", "&a* &fAdicionar Speaker"),
                Voicechat.MESSAGES.text("gui.global_players.add_speaker_desc", "&7 > &fSelecione quem podera falar com stage global."),
                Voicechat.MESSAGES.text("gui.global_players.add_speaker_action", "&a > &eClique para adicionar"));
    }

    private static void openAdd(Player player, String title, int page, java.util.function.Predicate<UUID> filter,
                                String headerName, String description, String clickLore) {
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
            headerMeta.setDisplayName(headerName);
            headerMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    description,
                    Voicechat.MESSAGES.text("gui.global_players.add_info_apply", "&7 > &fA alteracao e aplicada no momento do clique."),
                    "",
                    S
            ));
            header.setItemMeta(headerMeta);
        }
        inv.setItem(4, header);

        List<Player> filtered = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) {
                continue;
            }
            if (filter.test(online.getUniqueId())) {
                filtered.add(online);
            }
        }
        filtered.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        pages.put(player.getUniqueId(), page);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filtered.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            Player online = filtered.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(online);
                skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.global_players.add_item_name", "&6* &f%s", online.getName()));
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.global_players.add_item_line", "&7 > &fEste jogador sera adicionado a lista global."),
                        "",
                        S,
                        clickLore
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot, head);
            slot++;
        }

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    Voicechat.MESSAGES.text("gui.global_players.add_nav.prev_name", "&e< Anterior"),
                    Voicechat.MESSAGES.format("gui.global_players.add_nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    Voicechat.MESSAGES.text("gui.global_players.add_nav.next_name", "&eProxima >"),
                    Voicechat.MESSAGES.format("gui.global_players.add_nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
            ));
        }

        inv.setItem(45, createBackButton(Voicechat.MESSAGES.text("gui.global_players.back_list_lore", "&7 > &fVoltar para lista")));

        player.openInventory(inv);
    }

    public static int getPage(UUID playerUuid) {
        return pages.getOrDefault(playerUuid, 0);
    }

    private static String resolvePlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
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

    private static ItemStack createBackButton(String lore) {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(Voicechat.MESSAGES.text("gui.global_players.back_name", "&c< &fVoltar"));
            backMeta.setLore(Arrays.asList("", lore));
            back.setItemMeta(backMeta);
        }
        return back;
    }
}
