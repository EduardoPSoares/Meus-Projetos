package me.ray.midgard.modules.races.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI Admin de Raças - Painel completo
 * Design: Hypixel/Wynncraft Premium Style
 * Layout: 6 linhas (54 slots) - Painel administrativo com lista de jogadores
 */
public class RaceAdminGui extends BaseGui {

    private final RacesModule module;
    private int page = 0;

    // Layout 6 linhas (54 slots)
    // Linha 0: Decoração + ícone título
    private static final int SLOT_TITLE = 4;

    // Linhas 1-3: Grid de jogadores online (21 slots)
    private static final List<Integer> PLAYER_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );
    private static final int PLAYERS_PER_PAGE = PLAYER_SLOTS.size();

    // Linha 4: Controles
    private static final int SLOT_STATS = 37;
    private static final int SLOT_PREV = 39;
    private static final int SLOT_PAGE_INFO = 40;
    private static final int SLOT_NEXT = 41;
    private static final int SLOT_RELOAD = 43;

    // Linha 5: Navegação
    private static final int SLOT_BACK = 45;
    private static final int SLOT_CLOSE = 53;

    public RaceAdminGui(Player player) {
        super(player, 6, getTitle());
        this.module = RacesModule.getInstance();
    }

    private static String getTitle() {
        return RacesModule.getInstance().getGuiMessage("admin.title");
    }

    private String gui(String key) {
        return module.getGuiMessage("admin." + key);
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // Decoração
        var pane = RaceGuiTheme.createDarkPane();
        RaceGuiTheme.fillSlots(inventory, pane, 0, 1, 2, 3, 5, 6, 7, 8);
        RaceGuiTheme.fillSlots(inventory, pane, 9, 17, 18, 26, 27, 35);
        RaceGuiTheme.fillSlots(inventory, pane, 36, 38, 42, 44);
        RaceGuiTheme.fillSlots(inventory, pane, 46, 47, 48, 49, 50, 51, 52);

        // Ícone título
        inventory.setItem(SLOT_TITLE, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(gui("title_icon.name"))
                .setLoreMultiline(gui("title_icon.lore"))
                .glow()
                .build());

        // Grid de jogadores online
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int start = page * PLAYERS_PER_PAGE;
        int end = Math.min(start + PLAYERS_PER_PAGE, onlinePlayers.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex < PLAYER_SLOTS.size()) {
                inventory.setItem(PLAYER_SLOTS.get(slotIndex), createPlayerItem(onlinePlayers.get(i)));
            }
        }

        // Estatísticas
        inventory.setItem(SLOT_STATS, createStatsItem());

        // Paginação
        int totalPages = Math.max(1, (int) Math.ceil((double) onlinePlayers.size() / PLAYERS_PER_PAGE));

        if (page > 0) {
            inventory.setItem(SLOT_PREV, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.previous_page"))
                    .build());
        }

        inventory.setItem(SLOT_PAGE_INFO, new ItemBuilder(Material.PAPER)
                .setName(module.getGuiMessage("general.page_info")
                        .replace("%current%", String.valueOf(page + 1))
                        .replace("%total%", String.valueOf(totalPages)))
                .build());

        if ((page + 1) * PLAYERS_PER_PAGE < onlinePlayers.size()) {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.next_page"))
                    .build());
        }

        // Reload
        inventory.setItem(SLOT_RELOAD, createReloadItem());

        // Voltar / Fechar
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .setName(module.getGuiMessage("general.back"))
                .build());
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(module.getGuiMessage("general.close"))
                .build());
    }

    private ItemStack createPlayerItem(Player target) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(target);
        RaceData raceData = (profile != null) ? profile.getData(RaceData.class) : null;

        String raceName = module.getGuiMessage("general.no_race_name");
        int level = 0;
        double xp = 0;

        if (raceData != null && raceData.hasRace()) {
            Race race = module.getRaceManager().getRace(raceData.getRaceId());
            if (race != null) {
                raceName = race.getDisplayName();
            }
            level = raceData.getLevel();
            xp = raceData.getExperience();
        }

        return new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(target)
                .setName(gui("player_item.name").replace("%player%", target.getName()))
                .setLoreMultiline(gui("player_item.lore")
                        .replace("%race%", raceName)
                        .replace("%level%", String.valueOf(level))
                        .replace("%xp%", String.format("%.0f", xp)))
                .build();
    }

    private ItemStack createReloadItem() {
        return new ItemBuilder(Material.REDSTONE)
                .setName(gui("reload.name"))
                .setLoreMultiline(gui("reload.lore"))
                .glow()
                .build();
    }

    private ItemStack createStatsItem() {
        long totalRaces = module.getRaceManager().getRaces().size();
        long baseRaces = module.getRaceManager().getRaces().stream()
                .filter(r -> !r.isSubRace())
                .count();
        long subRaces = totalRaces - baseRaces;

        long onlinePlayers = Bukkit.getOnlinePlayers().size();
        long playersWithRace = Bukkit.getOnlinePlayers().stream()
                .filter(p -> {
                    var prof = MidgardCore.getProfileManager().getProfile(p);
                    if (prof == null) { return false; }
                    var rd = prof.getData(RaceData.class);
                    return rd != null && rd.hasRace();
                })
                .count();

        return new ItemBuilder(Material.BOOK)
                .setName(gui("stats.name"))
                .setLoreMultiline(gui("stats.lore")
                        .replace("%total%", String.valueOf(totalRaces))
                        .replace("%base%", String.valueOf(baseRaces))
                        .replace("%sub%", String.valueOf(subRaces))
                        .replace("%online%", String.valueOf(onlinePlayers))
                        .replace("%with_race%", String.valueOf(playersWithRace)))
                .build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event == null) { return; }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) { return; }
        if (!player.equals(event.getWhoClicked())) { return; }

        if (!player.hasPermission("midgard.admin.race")) {
            me.ray.midgard.core.text.MessageUtils.send(player, module.getMessage("command.no_permission"));
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) { return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) { return; }

        try {
            // Verificar se clicou em um jogador
            if (PLAYER_SLOTS.contains(slot)) {
                int index = PLAYER_SLOTS.indexOf(slot) + (page * PLAYERS_PER_PAGE);
                List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                if (index >= 0 && index < onlinePlayers.size()) {
                    Player target = onlinePlayers.get(index);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new RaceAdminPlayerGui(player, target, this).open();
                    return;
                }
            }

            switch (slot) {
                case SLOT_RELOAD -> {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    module.reloadConfig();
                    me.ray.midgard.core.text.MessageUtils.send(player,
                            module.getMessage("command.reload_success"));
                    initializeItems();
                }
                case SLOT_PREV -> {
                    if (page > 0) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                        page--;
                        initializeItems();
                    }
                }
                case SLOT_NEXT -> {
                    List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                    if ((page + 1) * PLAYERS_PER_PAGE < onlinePlayers.size()) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                        page++;
                        initializeItems();
                    }
                }
                case SLOT_BACK -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new RaceMainMenuGui(player).open();
                }
                case SLOT_CLOSE -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    player.closeInventory();
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceAdminGui para %s no slot %d",
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }
}
