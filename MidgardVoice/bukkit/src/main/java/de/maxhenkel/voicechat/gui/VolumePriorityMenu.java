package de.maxhenkel.voicechat.gui;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VolumePriorityMenu {

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";
    private static final int ITEMS_PER_PAGE = 36;
    private static final int SELECT_ITEMS_PER_PAGE = 36;
    private static final Map<UUID, Integer> pages = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> volumeSelectPages = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> prioritySelectPages = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> selectedVolumeTargets = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> selectedPriorityTargets = new ConcurrentHashMap<>();

    public static String getTitle() {
        return Voicechat.MESSAGES.text("gui.volume_priority.title", "&5Volume e Prioridade");
    }

    public static String getVolumeSelectTitlePrefix() {
        return Voicechat.MESSAGES.text("gui.volume_priority.volume_title_prefix", "&5Volume: ");
    }

    public static String getPrioritySelectTitlePrefix() {
        return Voicechat.MESSAGES.text("gui.volume_priority.priority_title_prefix", "&5Prioridade: ");
    }

    public static String getSelectVolumeTitle() {
        return Voicechat.MESSAGES.text("gui.volume_priority.select_volume_title", "&5Selecionar Jogador - Volume");
    }

    public static String getSelectPriorityTitle() {
        return Voicechat.MESSAGES.text("gui.volume_priority.select_priority_title", "&5Selecionar Jogador - Prioridade");
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, getTitle());
        fillFrame(inv);

        Map<UUID, Float> volumes = Voicechat.playerRangeManager.getAllVolumes();
        Map<UUID, Integer> priorities = Voicechat.playerRangeManager.getAllPriorities();

        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(volumes.keySet());
        allPlayers.addAll(priorities.keySet());

        List<UUID> sortedPlayers = new ArrayList<>(allPlayers);
        sortedPlayers.sort(Comparator.comparing(VolumePriorityMenu::resolvePlayerName, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (sortedPlayers.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        pages.put(player.getUniqueId(), page);

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Voicechat.MESSAGES.text("gui.volume_priority.info_name", "&d&l* Volume e Prioridade"));
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.volume_priority.info.line1", "&7 > &fAjusta como cada jogador e ouvido."),
                    Voicechat.MESSAGES.text("gui.volume_priority.info.line2", "&7 > &fVolume altera a intensidade do audio."),
                    Voicechat.MESSAGES.text("gui.volume_priority.info.line3", "&7 > &fPrioridade define quem prevalece quando"),
                    Voicechat.MESSAGES.text("gui.volume_priority.info.line4", "&7   &fo receptor recebe falas concorrentes."),
                    "",
                    Voicechat.MESSAGES.format("gui.volume_priority.info.volumes", "&7 > &eVolumes customizados&8 - &f%s", volumes.size()),
                    Voicechat.MESSAGES.format("gui.volume_priority.info.priorities", "&7 > &ePrioridades customizadas&8 - &f%s", priorities.size()),
                    Voicechat.MESSAGES.format("gui.volume_priority.info.page", "&7 > &ePagina&8 - &f%s/%s", page + 1, totalPages),
                    "",
                    S
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, sortedPlayers.size());
        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            UUID uuid = sortedPlayers.get(i);
            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
            String name = resolvePlayerName(uuid);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(target);
                skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.volume_priority.item_name", "&6* &f%s", name));

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(S);
                lore.add("");
                Float vol = Voicechat.playerRangeManager.getVolume(uuid);
                lore.add(Voicechat.MESSAGES.format(
                        "gui.volume_priority.item.volume",
                        "&7 > &eVolume&8 - %s",
                        vol != null
                                ? Voicechat.MESSAGES.format("gui.volume_priority.item.volume_value", "&f%s", String.format("%.2fx", vol))
                                : Voicechat.MESSAGES.text("gui.volume_priority.item.default", "&7Padrao")
                ));
                int pri = Voicechat.playerRangeManager.getPriority(uuid);
                lore.add(Voicechat.MESSAGES.format(
                        "gui.volume_priority.item.priority",
                        "&7 > &ePrioridade&8 - %s",
                        pri > 0
                                ? Voicechat.MESSAGES.format("gui.volume_priority.item.priority_value", "&f%s", pri)
                                : Voicechat.MESSAGES.text("gui.volume_priority.item.priority_default", "&70")
                ));
                lore.add("");
                lore.add(Voicechat.MESSAGES.text("gui.volume_priority.item.line1", "&7 > &fVolume mais alto deixa a voz mais forte."));
                lore.add(Voicechat.MESSAGES.text("gui.volume_priority.item.line2", "&7 > &fPrioridade mais alta ganha disputa de audio."));
                lore.add("");
                lore.add(S);
                lore.add(Voicechat.MESSAGES.text("gui.volume_priority.item.left_click", "&a > &eClique esquerdo&8 - &fEditar volume"));
                lore.add(Voicechat.MESSAGES.text("gui.volume_priority.item.right_click", "&b > &eClique direito&8 - &fEditar prioridade"));
                lore.add(Voicechat.MESSAGES.text("gui.volume_priority.item.shift_left", "&c > &eShift + esquerdo&8 - &fRemover overrides"));
                skullMeta.setLore(lore);
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot++, head);
        }

        if (sortedPlayers.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(Voicechat.MESSAGES.text("gui.volume_priority.empty_name", "&7Nenhum jogador configurado"));
                emptyMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.volume_priority.empty.line1", "&7 > &fAinda nao existem overrides salvos."),
                        Voicechat.MESSAGES.text("gui.volume_priority.empty.line2", "&7 > &fUse os botoes inferiores para escolher"),
                        Voicechat.MESSAGES.text("gui.volume_priority.empty.line3", "&7   &fum jogador e criar a configuracao."),
                        "",
                        S
                ));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        }

        if (page > 0) {
            inv.setItem(47, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.volume_priority.nav.prev_name", "&e< Pagina Anterior"),
                    "",
                    Voicechat.MESSAGES.format("gui.volume_priority.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)));
        }

        inv.setItem(49, createButton(Material.BELL,
                Voicechat.MESSAGES.text("gui.volume_priority.volume_button.name", "&e* &fDefinir Volume"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.volume_priority.volume_button.line1", "&7 > &fEscolhe um jogador online e abre a"),
                Voicechat.MESSAGES.text("gui.volume_priority.volume_button.line2", "&7   &ftela de multiplicador de volume."),
                Voicechat.MESSAGES.text("gui.volume_priority.volume_button.line3", "&7 > &fUse para destacar ou reduzir alguem"),
                Voicechat.MESSAGES.text("gui.volume_priority.volume_button.line4", "&7   &fsem mexer no range de voz dele."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.volume_priority.volume_button.action", "&a > &eClique para selecionar")));

        inv.setItem(51, createButton(Material.DIAMOND,
                Voicechat.MESSAGES.text("gui.volume_priority.priority_button.name", "&b* &fDefinir Prioridade"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.volume_priority.priority_button.line1", "&7 > &fEscolhe um jogador online e abre a"),
                Voicechat.MESSAGES.text("gui.volume_priority.priority_button.line2", "&7   &ftela de prioridade de transmissao."),
                Voicechat.MESSAGES.text("gui.volume_priority.priority_button.line3", "&7 > &fUse quando uma voz precisa ter"),
                Voicechat.MESSAGES.text("gui.volume_priority.priority_button.line4", "&7   &fprecedencia sobre outras falas."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.volume_priority.priority_button.action", "&a > &eClique para selecionar")));

        if (page < totalPages - 1) {
            inv.setItem(53, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.volume_priority.nav.next_name", "&eProxima Pagina >"),
                    "",
                    Voicechat.MESSAGES.format("gui.volume_priority.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)));
        }

        inv.setItem(45, createButton(Material.ARROW,
                Voicechat.MESSAGES.text("gui.volume_priority.back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.volume_priority.back_lore", "&7 > &fRetorna ao painel administrativo")));

        player.openInventory(inv);
    }

    public static void openVolumeSelect(Player player, UUID targetUuid) {
        selectedVolumeTargets.put(player.getUniqueId(), targetUuid);
        String targetName = resolvePlayerName(targetUuid);
        Inventory inv = Bukkit.createInventory(null, 27, getVolumeSelectTitlePrefix() + targetName);

        ItemStack glass = createGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, glass);
        }

        float[] values = {0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f, 5.0f};
        String[] labels = {"0.25x", "0.5x", "0.75x", "1.0x", "1.5x", "2.0x", "3.0x", "5.0x"};
        Float currentVol = Voicechat.playerRangeManager.getVolume(targetUuid);

        for (int i = 0; i < values.length; i++) {
            boolean selected = currentVol != null && Math.abs(currentVol - values[i]) < 0.01f;
            ItemStack item = new ItemStack(selected ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Voicechat.MESSAGES.format(
                        selected ? "gui.volume_priority.volume_option.selected_name" : "gui.volume_priority.volume_option.name",
                        selected ? "&a* &f%s" : "&e* &f%s",
                        labels[i]
                ));
                meta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.volume_priority.volume_option.line1", "&7 > &fMultiplicador aplicado ao volume final."),
                        Voicechat.MESSAGES.text(
                                selected ? "gui.volume_priority.volume_option.selected_line" : "gui.volume_priority.volume_option.action_line",
                                selected ? "&7 > &fEste e o valor ativo no momento." : "&7 > &fClique para usar este nivel."
                        ),
                        "",
                        S
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(9 + i, item);
        }

        inv.setItem(17, createButton(Material.BARRIER,
                Voicechat.MESSAGES.text("gui.volume_priority.volume_reset.name", "&c* &fRemover Volume"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.volume_priority.volume_reset.line1", "&7 > &fApaga o override e devolve o jogador"),
                Voicechat.MESSAGES.text("gui.volume_priority.volume_reset.line2", "&7   &fpara o comportamento de volume padrao."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.volume_priority.volume_reset.action", "&c > &eClique para resetar")));

        inv.setItem(18, createButton(Material.ARROW,
                Voicechat.MESSAGES.text("gui.volume_priority.select_back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.volume_priority.select_back_lore", "&7 > &fRetorna para a lista anterior")));

        player.openInventory(inv);
    }

    public static void openPrioritySelect(Player player, UUID targetUuid) {
        selectedPriorityTargets.put(player.getUniqueId(), targetUuid);
        String targetName = resolvePlayerName(targetUuid);
        Inventory inv = Bukkit.createInventory(null, 27, getPrioritySelectTitlePrefix() + targetName);

        ItemStack glass = createGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, glass);
        }

        int[] values = {0, 1, 5, 10, 25, 50, 75, 100};
        int currentPri = Voicechat.playerRangeManager.getPriority(targetUuid);

        for (int i = 0; i < values.length; i++) {
            boolean selected = currentPri == values[i];
            ItemStack item = new ItemStack(selected ? Material.LIME_STAINED_GLASS_PANE : Material.LIGHT_BLUE_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Voicechat.MESSAGES.format(
                        selected ? "gui.volume_priority.priority_option.selected_name" : "gui.volume_priority.priority_option.name",
                        selected ? "&a* &fPrioridade %s" : "&b* &fPrioridade %s",
                        values[i]
                ));
                meta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.volume_priority.priority_option.line1", "&7 > &fQuanto maior o valor, maior a chance"),
                        Voicechat.MESSAGES.text("gui.volume_priority.priority_option.line2", "&7   &fdesta voz prevalecer em disputa de audio."),
                        Voicechat.MESSAGES.text(
                                selected ? "gui.volume_priority.priority_option.selected_line" : "gui.volume_priority.priority_option.action_line",
                                selected ? "&7 > &fEste e o valor ativo no momento." : "&7 > &fClique para definir este nivel."
                        ),
                        "",
                        S
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(9 + i, item);
        }

        inv.setItem(18, createButton(Material.ARROW,
                Voicechat.MESSAGES.text("gui.volume_priority.select_back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.volume_priority.select_back_lore", "&7 > &fRetorna para a lista anterior")));

        player.openInventory(inv);
    }

    public static void openSelectPlayerForVolume(Player player) {
        openSelectPlayerForVolume(player, 0);
    }

    public static void openSelectPlayerForVolume(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, getSelectVolumeTitle());
        List<Player> onlinePlayers = getSortedOnlinePlayers();
        int totalPages = Math.max(1, (onlinePlayers.size() + SELECT_ITEMS_PER_PAGE - 1) / SELECT_ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        volumeSelectPages.put(player.getUniqueId(), page);
        fillPlayerSelect(
                inv,
                Voicechat.MESSAGES.text("gui.volume_priority.select_volume_header", "&e* &fSelecionar Jogador - Volume"),
                Voicechat.MESSAGES.text("gui.volume_priority.select_volume_desc", "&7 > &fEscolha quem recebera um ajuste de volume."),
                onlinePlayers,
                page,
                totalPages
        );
        applySelectFooter(inv, page, totalPages);
        player.openInventory(inv);
    }

    public static void openSelectPlayerForPriority(Player player) {
        openSelectPlayerForPriority(player, 0);
    }

    public static void openSelectPlayerForPriority(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, getSelectPriorityTitle());
        List<Player> onlinePlayers = getSortedOnlinePlayers();
        int totalPages = Math.max(1, (onlinePlayers.size() + SELECT_ITEMS_PER_PAGE - 1) / SELECT_ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        prioritySelectPages.put(player.getUniqueId(), page);
        fillPlayerSelect(
                inv,
                Voicechat.MESSAGES.text("gui.volume_priority.select_priority_header", "&b* &fSelecionar Jogador - Prioridade"),
                Voicechat.MESSAGES.text("gui.volume_priority.select_priority_desc", "&7 > &fEscolha quem recebera uma prioridade de audio."),
                onlinePlayers,
                page,
                totalPages
        );
        applySelectFooter(inv, page, totalPages);
        player.openInventory(inv);
    }

    public static int getPage(UUID playerUuid) {
        return pages.getOrDefault(playerUuid, 0);
    }

    public static int getVolumeSelectPage(UUID playerUuid) {
        return volumeSelectPages.getOrDefault(playerUuid, 0);
    }

    public static int getPrioritySelectPage(UUID playerUuid) {
        return prioritySelectPages.getOrDefault(playerUuid, 0);
    }

    public static UUID getSelectedVolumeTarget(UUID playerUuid) {
        return selectedVolumeTargets.get(playerUuid);
    }

    public static UUID getSelectedPriorityTarget(UUID playerUuid) {
        return selectedPriorityTargets.get(playerUuid);
    }

    private static void fillPlayerSelect(Inventory inv, String headerName, String description, List<Player> onlinePlayers, int page, int totalPages) {
        fillFrame(inv);

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(headerName);
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    description,
                    Voicechat.MESSAGES.text("gui.volume_priority.select_info.line1", "&7 > &fA configuracao sera aplicada apenas ao jogador"),
                    Voicechat.MESSAGES.text("gui.volume_priority.select_info.line2", "&7   &fselecionado nesta tela."),
                    "",
                    Voicechat.MESSAGES.format("gui.volume_priority.select_info.page", "&7 > &ePagina&8 - &f%s/%s", page + 1, totalPages),
                    "",
                    S
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        int startIndex = page * SELECT_ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + SELECT_ITEMS_PER_PAGE, onlinePlayers.size());
        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            Player online = onlinePlayers.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(online);
                skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.volume_priority.select_item.name", "&6* &f%s", online.getName()));
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.volume_priority.select_item.line1", "&7 > &fAbre a tela de definicao para este jogador."),
                        "",
                        S,
                        Voicechat.MESSAGES.text("gui.volume_priority.select_item.action", "&a > &eClique para selecionar")
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot++, head);
        }

        if (onlinePlayers.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(Voicechat.MESSAGES.text("gui.volume_priority.select_empty_name", "&7Nenhum jogador online"));
                emptyMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.volume_priority.select_empty.line1", "&7 > &fNao ha jogadores online disponiveis"),
                        Voicechat.MESSAGES.text("gui.volume_priority.select_empty.line2", "&7   &fpara criar uma configuracao agora."),
                        "",
                        S
                ));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        }
    }

    private static void applySelectFooter(Inventory inv, int page, int totalPages) {
        inv.setItem(45, createButton(Material.ARROW,
                Voicechat.MESSAGES.text("gui.volume_priority.select_back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.volume_priority.select_back_lore", "&7 > &fRetorna para a lista anterior")));
        if (page > 0) {
            inv.setItem(47, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.volume_priority.select_nav.prev_name", "&e< Pagina Anterior"),
                    "",
                    Voicechat.MESSAGES.format("gui.volume_priority.select_nav.prev_lore", "&7 > &fVolta para a pagina %s", page)));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.volume_priority.select_nav.next_name", "&eProxima Pagina >"),
                    "",
                    Voicechat.MESSAGES.format("gui.volume_priority.select_nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)));
        }
    }

    private static void fillFrame(Inventory inv) {
        ItemStack glass = createGlass();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, glass);
        }
    }

    private static List<Player> getSortedOnlinePlayers() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        return players;
    }

    private static String resolvePlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() != null ? player.getName() : uuid.toString().substring(0, 8);
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
