package de.maxhenkel.voicechat.gui;

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

public class StageSpeakersMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final Map<UUID, Integer> speakerPages = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> addSpeakerPages = new ConcurrentHashMap<>();
    private static final String S = ChatColor.DARK_GRAY + "-------------------------";

    public static String getTitlePrefix() {
        return de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.title_prefix", "&5Speakers: ");
    }

    public static String getAddTitlePrefix() {
        return de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_title_prefix", "&5Adicionar Speaker: ");
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

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(de.maxhenkel.voicechat.Voicechat.MESSAGES.format(
                    "gui.stage.info_name",
                    "&d&l* Speakers - %s",
                    zone.getName()
            ));
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.info.line1", "&7 > &fLista de jogadores autorizados a falar"),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.info.line2", "&7   &fquando o stage mode desta zona estiver ativo."),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.info.line3", "&7 > &fSem speaker, o jogador apenas escuta"),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.info.line4", "&7   &fe nao transmite voz dentro do palco."),
                    "",
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.format("gui.stage.info.count", "&7 > &eSpeakers&8 - &f%s", zone.getSpeakers().size()),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.format(
                            "gui.stage.info.status",
                            "&7 > &eStage Mode&8 - %s",
                            zone.isStageMode()
                                    ? de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.status_enabled", "&aAtivado")
                                    : de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.status_disabled", "&cDesativado")
                    ),
                    "",
                    S
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        List<UUID> allSpeakers = new ArrayList<>(zone.getSpeakers());
        allSpeakers.sort(Comparator.comparing(StageSpeakersMenu::resolvePlayerName, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil((double) allSpeakers.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        speakerPages.put(player.getUniqueId(), page);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allSpeakers.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            UUID speakerUuid = allSpeakers.get(i);
            OfflinePlayer target = Bukkit.getOfflinePlayer(speakerUuid);
            String name = resolvePlayerName(speakerUuid);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(target);
                skullMeta.setDisplayName(de.maxhenkel.voicechat.Voicechat.MESSAGES.format("gui.stage.item_name", "&6* &f%s", name));
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.item_status", "&7 > &eStatus&8 - &aSpeaker ativo"),
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.item.line1", "&7 > &fEste jogador pode transmitir voz"),
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.item.line2", "&7   &fenquanto o palco estiver habilitado."),
                        "",
                        S,
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.item.action", "&c > &eClique para remover do palco")
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot, head);
            slot++;
        }

        if (allSpeakers.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.empty_name", "&7Nenhum speaker configurado"));
                emptyMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.empty.line1", "&7 > &fAinda nao existe ninguem autorizado"),
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.empty.line2", "&7   &fa falar neste palco."),
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.empty.line3", "&7 > &fUse o botao inferior para adicionar"),
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.empty.line4", "&7   &fjogadores online a lista."),
                        "",
                        S
                ));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        }

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.nav.prev_name", "&e< Anterior"),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.format("gui.stage.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.nav.next_name", "&eProxima >"),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.format("gui.stage.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
            ));
        }

        inv.setItem(49, createButton(Material.EMERALD,
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_button.name", "&a* &fAdicionar Speaker"),
                "",
                S,
                "",
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_button.line1", "&7 > &fAbre a selecao de jogadores online"),
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_button.line2", "&7   &fque ainda nao sao speakers desta zona."),
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_button.line3", "&7 > &fUse quando quiser montar ou expandir"),
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_button.line4", "&7   &fquem pode falar no palco."),
                "",
                S,
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_button.action", "&a > &eClique para selecionar")));

        inv.setItem(45, createButton(Material.ARROW,
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.back_name", "&c< &fVoltar"),
                "",
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.back_lore", "&7 > &fRetorna para as configuracoes da zona")));

        player.openInventory(inv);
    }

    public static void openAddSpeaker(Player player, RestrictedZone zone) {
        openAddSpeaker(player, zone, 0);
    }

    public static void openAddSpeaker(Player player, RestrictedZone zone, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, getAddTitlePrefix() + zone.getName());

        ItemStack glass = createGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }

        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_info_name", "&d&l* Adicionar Speaker"));
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_info.line1", "&7 > &fEscolha um jogador online para liberar"),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_info.line2", "&7   &ffala dentro do stage mode da zona."),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_info.line3", "&7 > &fA permissao vale imediatamente apos"),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_info.line4", "&7   &fo clique e pode ser removida depois."),
                    "",
                    S
            ));
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(4, infoItem);

        List<Player> filtered = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!zone.isSpeaker(online.getUniqueId())) {
                filtered.add(online);
            }
        }
        filtered.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        addSpeakerPages.put(player.getUniqueId(), page);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filtered.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            Player online = filtered.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(online);
                skullMeta.setDisplayName(de.maxhenkel.voicechat.Voicechat.MESSAGES.format("gui.stage.add_item_name", "&6* &f%s", online.getName()));
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_item.line1", "&7 > &fAo adicionar, este jogador passa"),
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_item.line2", "&7   &fa ter permissao de fala no palco."),
                        "",
                        S,
                        de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_item.action", "&a > &eClique para adicionar")
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot, head);
            slot++;
        }

        if (page > 0) {
            inv.setItem(47, createNavButton(
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_nav.prev_name", "&e< Anterior"),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.format("gui.stage.add_nav.prev_lore", "&7 > &fVolta para a pagina %s", page)
            ));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createNavButton(
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_nav.next_name", "&eProxima >"),
                    de.maxhenkel.voicechat.Voicechat.MESSAGES.format("gui.stage.add_nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)
            ));
        }

        inv.setItem(45, createButton(Material.ARROW,
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_back_name", "&c< &fVoltar"),
                "",
                de.maxhenkel.voicechat.Voicechat.MESSAGES.text("gui.stage.add_back_lore", "&7 > &fRetorna para a lista de speakers")));

        player.openInventory(inv);
    }

    public static int getSpeakerPage(UUID playerUuid) {
        return speakerPages.getOrDefault(playerUuid, 0);
    }

    public static int getAddSpeakerPage(UUID playerUuid) {
        return addSpeakerPages.getOrDefault(playerUuid, 0);
    }

    private static String resolvePlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
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

    private static ItemStack createButton(Material material, String name, String... lore) {
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
}
