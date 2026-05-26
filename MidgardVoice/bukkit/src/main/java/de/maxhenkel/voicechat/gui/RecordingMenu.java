package de.maxhenkel.voicechat.gui;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.recording.VoiceRecording;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RecordingMenu {

    private static final ConcurrentHashMap<UUID, Integer> activePages = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> savedPages = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> startPages = new ConcurrentHashMap<>();
    private static final int ACTIVE_ITEMS_PER_PAGE = 27;
    private static final int SAVED_ITEMS_PER_PAGE = 36;
    private static final int START_ITEMS_PER_PAGE = 36;

    private static final String S = ChatColor.DARK_GRAY + "-------------------------";

    public static String getTitle() {
        return Voicechat.MESSAGES.text("gui.recording.title", "&5Gravacoes de Audio");
    }

    public static String getSavedTitle() {
        return Voicechat.MESSAGES.text("gui.recording.saved_title", "&5Gravacoes Salvas");
    }

    public static String getStartTitle() {
        return Voicechat.MESSAGES.text("gui.recording.start_title", "&5Iniciar Gravacao");
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        List<VoiceRecording> activeRecordings = new ArrayList<>(Voicechat.voiceRecordingManager.getActiveRecordings().values());
        activeRecordings.sort(Comparator.comparing(VoiceRecording::getTargetName, String.CASE_INSENSITIVE_ORDER));
        List<String> savedRecordings = Voicechat.voiceRecordingManager.getSavedRecordings();

        int totalPages = Math.max(1, (activeRecordings.size() + ACTIVE_ITEMS_PER_PAGE - 1) / ACTIVE_ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        activePages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, getTitle());
        fillFrame(inv);

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Voicechat.MESSAGES.text("gui.recording.info_name", "&d&l* Gravacoes de Audio"));
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.recording.info.line1", "&7 > &fRegistra a voz de jogadores em arquivos locais."),
                    Voicechat.MESSAGES.text("gui.recording.info.line2", "&7 > &fUse para auditoria, investigacao, provas"),
                    Voicechat.MESSAGES.text("gui.recording.info.line3", "&7   &fadministrativas e acompanhamento de eventos."),
                    "",
                    Voicechat.MESSAGES.text("gui.recording.info.line4", "&7 > &fGravacoes ativas capturam novos frames."),
                    Voicechat.MESSAGES.text("gui.recording.info.line5", "&7 > &fGravacoes salvas podem ser revisadas ou apagadas."),
                    "",
                    S,
                    Voicechat.MESSAGES.format("gui.recording.info.active_count", "&7 > &eAtivas&8 - &f%s", activeRecordings.size()),
                    Voicechat.MESSAGES.format("gui.recording.info.saved_count", "&7 > &eSalvas&8 - &f%s", savedRecordings.size()),
                    Voicechat.MESSAGES.format("gui.recording.info.page", "&7 > &ePagina&8 - &f%s/%s", page + 1, totalPages)
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        int startIndex = page * ACTIVE_ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ACTIVE_ITEMS_PER_PAGE, activeRecordings.size());
        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            VoiceRecording rec = activeRecordings.get(i);
            ItemStack item = new ItemStack(Material.MUSIC_DISC_CAT);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Voicechat.MESSAGES.format("gui.recording.active_item.name", "&c* &f%s", rec.getTargetName()));
                meta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.recording.active_item.status", "&7 > &eStatus&8 - &cGravando agora"),
                        Voicechat.MESSAGES.format("gui.recording.active_item.duration", "&7 > &eDuracao&8 - &f%s", rec.getFormattedDuration()),
                        Voicechat.MESSAGES.format("gui.recording.active_item.frames", "&7 > &eFrames&8 - &f%s", rec.getFrameCount()),
                        Voicechat.MESSAGES.format("gui.recording.active_item.by", "&7 > &eIniciada por&8 - &f%s", rec.getRecordedByName()),
                        "",
                        Voicechat.MESSAGES.text("gui.recording.active_item.line1", "&7 > &fAo parar, o arquivo e finalizado e"),
                        Voicechat.MESSAGES.text("gui.recording.active_item.line2", "&7   &ffica disponivel na lista de gravacoes salvas."),
                        "",
                        S,
                        Voicechat.MESSAGES.text("gui.recording.active_item.action", "&c > &eClique para parar e salvar")
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        if (activeRecordings.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(Voicechat.MESSAGES.text("gui.recording.empty_active_name", "&7Nenhuma gravacao ativa"));
                emptyMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.recording.empty_active.line1", "&7 > &fNenhum jogador esta sendo gravado agora."),
                        Voicechat.MESSAGES.text("gui.recording.empty_active.line2", "&7 > &fUse o botao inferior para escolher"),
                        Voicechat.MESSAGES.text("gui.recording.empty_active.line3", "&7   &fum alvo e iniciar a captura."),
                        "",
                        S
                ));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        }

        if (page > 0) {
            inv.setItem(47, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.recording.nav.prev_name", "&e< Pagina Anterior"),
                    "",
                    Voicechat.MESSAGES.format("gui.recording.nav.prev_lore", "&7 > &fVolta para a pagina %s", page)));
        }

        inv.setItem(49, createButton(Material.EMERALD,
                Voicechat.MESSAGES.text("gui.recording.start_button.name", "&a* &fIniciar Gravacao"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.recording.start_button.line1", "&7 > &fAbre a lista de jogadores online elegiveis."),
                Voicechat.MESSAGES.text("gui.recording.start_button.line2", "&7 > &fA partir dai a voz do alvo passa a ser"),
                Voicechat.MESSAGES.text("gui.recording.start_button.line3", "&7   &fgravada ate voce encerrar manualmente."),
                "",
                S,
                Voicechat.MESSAGES.text("gui.recording.start_button.action", "&a > &eClique para selecionar um alvo")));

        inv.setItem(51, createButton(Material.CHEST,
                Voicechat.MESSAGES.text("gui.recording.saved_button.name", "&6* &fGravacoes Salvas"),
                "",
                S,
                "",
                Voicechat.MESSAGES.text("gui.recording.saved_button.line1", "&7 > &fAbre o arquivo historico de gravacoes."),
                Voicechat.MESSAGES.text("gui.recording.saved_button.line2", "&7 > &fLa voce pode revisar detalhes do id e"),
                Voicechat.MESSAGES.text("gui.recording.saved_button.line3", "&7   &fapagar registros que nao precisa manter."),
                "",
                Voicechat.MESSAGES.format("gui.recording.saved_button.count", "&7 > &eDisponiveis&8 - &f%s", savedRecordings.size()),
                "",
                S,
                Voicechat.MESSAGES.text("gui.recording.saved_button.action", "&a > &eClique para abrir")));

        if (page < totalPages - 1) {
            inv.setItem(53, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.recording.nav.next_name", "&eProxima Pagina >"),
                    "",
                    Voicechat.MESSAGES.format("gui.recording.nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)));
        }

        inv.setItem(45, createButton(Material.ARROW,
                Voicechat.MESSAGES.text("gui.recording.back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.recording.back_lore", "&7 > &fRetorna ao painel administrativo")));

        player.openInventory(inv);
    }

    public static void openSaved(Player player, int page) {
        List<String> saved = Voicechat.voiceRecordingManager.getSavedRecordings();
        int totalPages = Math.max(1, (saved.size() + SAVED_ITEMS_PER_PAGE - 1) / SAVED_ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        savedPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, getSavedTitle());
        fillFrame(inv);

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Voicechat.MESSAGES.text("gui.recording.saved_info_name", "&d&l* Gravacoes Salvas"));
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.recording.saved_info.line1", "&7 > &fLista de arquivos ja finalizados."),
                    Voicechat.MESSAGES.text("gui.recording.saved_info.line2", "&7 > &fCada entrada representa um registro salvo"),
                    Voicechat.MESSAGES.text("gui.recording.saved_info.line3", "&7   &fque pode ser consultado ou removido."),
                    "",
                    Voicechat.MESSAGES.format("gui.recording.saved_info.total", "&7 > &eTotal&8 - &f%s", saved.size()),
                    Voicechat.MESSAGES.format("gui.recording.saved_info.page", "&7 > &ePagina&8 - &f%s/%s", page + 1, totalPages),
                    "",
                    S
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        int startIndex = page * SAVED_ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + SAVED_ITEMS_PER_PAGE, saved.size());
        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            String recId = saved.get(i);
            ItemStack item = new ItemStack(Material.DISC_FRAGMENT_5);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Voicechat.MESSAGES.format("gui.recording.saved_item.name", "&6* &f%s", recId));
                meta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.recording.saved_item.line1", "&7 > &fEntrada salva no historico de gravacoes."),
                        Voicechat.MESSAGES.text("gui.recording.saved_item.line2", "&7 > &fUse o clique esquerdo para inspecionar"),
                        Voicechat.MESSAGES.text("gui.recording.saved_item.line3", "&7   &fdados da gravacao e o direito para excluir."),
                        "",
                        S,
                        Voicechat.MESSAGES.text("gui.recording.saved_item.left_click", "&a > &eClique esquerdo&8 - &fVer detalhes"),
                        Voicechat.MESSAGES.text("gui.recording.saved_item.right_click", "&c > &eClique direito&8 - &fApagar registro")
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        if (saved.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(Voicechat.MESSAGES.text("gui.recording.empty_saved_name", "&7Nenhuma gravacao salva"));
                emptyMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.recording.empty_saved.line1", "&7 > &fAinda nao existe historico finalizado."),
                        Voicechat.MESSAGES.text("gui.recording.empty_saved.line2", "&7 > &fAs gravacoes encerradas vao aparecer"),
                        Voicechat.MESSAGES.text("gui.recording.empty_saved.line3", "&7   &fnesta tela automaticamente."),
                        "",
                        S
                ));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        }

        if (page > 0) {
            inv.setItem(47, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.recording.saved_nav.prev_name", "&e< Pagina Anterior"),
                    "",
                    Voicechat.MESSAGES.format("gui.recording.saved_nav.prev_lore", "&7 > &fVolta para a pagina %s", page)));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.recording.saved_nav.next_name", "&eProxima Pagina >"),
                    "",
                    Voicechat.MESSAGES.format("gui.recording.saved_nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)));
        }

        inv.setItem(45, createButton(Material.ARROW,
                Voicechat.MESSAGES.text("gui.recording.saved_back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.recording.saved_back_lore", "&7 > &fRetorna ao menu de gravacoes")));

        player.openInventory(inv);
    }

    public static int getSavedPage(UUID playerUuid) {
        return savedPages.getOrDefault(playerUuid, 0);
    }

    public static void openStartRecording(Player player) {
        openStartRecording(player, 0);
    }

    public static void openStartRecording(Player player, int page) {
        List<Player> selectablePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        selectablePlayers.removeIf(online -> Voicechat.voiceRecordingManager.isRecording(online.getUniqueId()));
        selectablePlayers.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (selectablePlayers.size() + START_ITEMS_PER_PAGE - 1) / START_ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        startPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, getStartTitle());
        fillFrame(inv);

        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(Voicechat.MESSAGES.text("gui.recording.start_info_name", "&d&l* Iniciar Gravacao"));
            infoMeta.setLore(Arrays.asList(
                    "",
                    S,
                    "",
                    Voicechat.MESSAGES.text("gui.recording.start_info.line1", "&7 > &fEscolha o jogador cuja voz sera capturada."),
                    Voicechat.MESSAGES.text("gui.recording.start_info.line2", "&7 > &fSo aparecem alvos online que ainda nao"),
                    Voicechat.MESSAGES.text("gui.recording.start_info.line3", "&7   &festao em outra gravacao ativa."),
                    "",
                    Voicechat.MESSAGES.format("gui.recording.start_info.page", "&7 > &ePagina&8 - &f%s/%s", page + 1, totalPages),
                    "",
                    S,
                    Voicechat.MESSAGES.text("gui.recording.start_info.action", "&a > &eClique em uma cabeca para iniciar")
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        int startIndex = page * START_ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + START_ITEMS_PER_PAGE, selectablePlayers.size());
        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            Player online = selectablePlayers.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(online);
                skullMeta.setDisplayName(Voicechat.MESSAGES.format("gui.recording.start_item.name", "&6* &f%s", online.getName()));
                skullMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.recording.start_item.line1", "&7 > &fAo clicar, uma nova captura sera criada"),
                        Voicechat.MESSAGES.text("gui.recording.start_item.line2", "&7   &fpara este jogador imediatamente."),
                        "",
                        S,
                        Voicechat.MESSAGES.text("gui.recording.start_item.action", "&a > &eClique para iniciar a gravacao")
                ));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot++, head);
        }

        if (selectablePlayers.isEmpty()) {
            ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(Voicechat.MESSAGES.text("gui.recording.start_empty_name", "&7Nenhum alvo disponivel"));
                emptyMeta.setLore(Arrays.asList(
                        "",
                        S,
                        "",
                        Voicechat.MESSAGES.text("gui.recording.start_empty.line1", "&7 > &fTodos os jogadores elegiveis ja estao"),
                        Voicechat.MESSAGES.text("gui.recording.start_empty.line2", "&7   &fsendo gravados ou nao ha ninguem online."),
                        "",
                        S
                ));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        }

        if (page > 0) {
            inv.setItem(47, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.recording.start_nav.prev_name", "&e< Pagina Anterior"),
                    "",
                    Voicechat.MESSAGES.format("gui.recording.start_nav.prev_lore", "&7 > &fVolta para a pagina %s", page)));
        }
        if (page < totalPages - 1) {
            inv.setItem(51, createButton(Material.SPECTRAL_ARROW,
                    Voicechat.MESSAGES.text("gui.recording.start_nav.next_name", "&eProxima Pagina >"),
                    "",
                    Voicechat.MESSAGES.format("gui.recording.start_nav.next_lore", "&7 > &fAvanca para a pagina %s", page + 2)));
        }

        inv.setItem(45, createButton(Material.ARROW,
                Voicechat.MESSAGES.text("gui.recording.start_back_name", "&c< &fVoltar"),
                "",
                Voicechat.MESSAGES.text("gui.recording.start_back_lore", "&7 > &fRetorna ao menu de gravacoes")));

        player.openInventory(inv);
    }

    public static int getActivePage(UUID playerUuid) {
        return activePages.getOrDefault(playerUuid, 0);
    }

    public static int getStartPage(UUID playerUuid) {
        return startPages.getOrDefault(playerUuid, 0);
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
