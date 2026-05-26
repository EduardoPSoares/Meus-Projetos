package me.ray.midgard.modules.races.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI Admin para escolher raça de um jogador
 * Layout: 5 linhas (45 slots) - Grid de raças disponíveis
 */
public class RaceAdminSetRaceGui extends BaseGui {

    private final RacesModule module;
    private final Player target;
    private final BaseGui parentGui;

    private int page = 0;
    private final List<Race> races;
    private static final List<Integer> RACE_SLOTS = RaceGuiTheme.gridSlots5Rows();
    private static final int RACES_PER_PAGE = RACE_SLOTS.size();

    private static final int SLOT_INFO = 4;
    private static final int SLOT_BACK = 36;
    private static final int SLOT_PREV = 39;
    private static final int SLOT_PAGE_INFO = 40;
    private static final int SLOT_NEXT = 41;
    private static final int SLOT_CLOSE = 44;

    public RaceAdminSetRaceGui(Player admin, Player target, BaseGui parentGui) {
        super(admin, 5, getTitle(target));
        this.module = RacesModule.getInstance();
        this.target = target;
        this.parentGui = parentGui;
        this.races = new ArrayList<>(module.getRaceManager().getRaces());
    }

    private static String getTitle(Player target) {
        return RacesModule.getInstance().getGuiMessage("admin_set_race.title")
                .replace("%player%", target.getName());
    }

    private String gui(String key) {
        return module.getGuiMessage("admin_set_race." + key);
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // Atualizar lista de raças a cada abertura
        races.clear();
        races.addAll(module.getRaceManager().getRaces());

        var pane = RaceGuiTheme.createDarkPane();
        RaceGuiTheme.fillBottomRow(inventory, pane);

        // Info no topo
        inventory.setItem(SLOT_INFO, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(gui("info.name").replace("%player%", target.getName()))
                .setLoreMultiline(gui("info.lore")
                        .replace("%total%", String.valueOf(races.size())))
                .glow()
                .build());

        // Grid de raças
        int start = page * RACES_PER_PAGE;
        int end = Math.min(start + RACES_PER_PAGE, races.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex < RACE_SLOTS.size()) {
                Race race = races.get(i);
                inventory.setItem(RACE_SLOTS.get(slotIndex), createRaceItem(race));
            }
        }

        // Navegação
        int totalPages = Math.max(1, (int) Math.ceil((double) races.size() / RACES_PER_PAGE));

        if (parentGui != null) {
            inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.back"))
                    .build());
        }

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

        if ((page + 1) * RACES_PER_PAGE < races.size()) {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.next_page"))
                    .build());
        }

        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(module.getGuiMessage("general.close"))
                .build());
    }

    private ItemStack createRaceItem(Race race) {
        String subRaceTag = race.isSubRace() ? gui("race_item.sub_tag") : "";
        return new ItemBuilder(race.getIcon())
                .setName(gui("race_item.name")
                        .replace("%race%", race.getDisplayName())
                        .replace("%sub%", subRaceTag))
                .setLoreMultiline(gui("race_item.lore")
                        .replace("%id%", race.getId())
                        .replace("%abilities%", String.valueOf(
                                race.getTraits() != null ? race.getTraits().size() : 0))
                        .replace("%attributes%", String.valueOf(
                                race.getAttributes() != null ? race.getAttributes().size() : 0)))
                .build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event == null) { return; }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) { return; }
        if (!player.equals(event.getWhoClicked())) { return; }

        if (!player.hasPermission("midgard.admin.race")) {
            MessageUtils.send(player, module.getMessage("command.no_permission"));
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) { return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) { return; }

        try {
            // Clique em raça
            if (RACE_SLOTS.contains(slot)) {
                int index = RACE_SLOTS.indexOf(slot) + (page * RACES_PER_PAGE);
                if (index >= 0 && index < races.size()) {
                    Race race = races.get(index);
                    module.getRaceManager().setRace(target, race, true);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                    MessageUtils.send(player, module.getMessage("command.set_success")
                            .replace("%player%", target.getName())
                            .replace("%race%", race.getDisplayName()));
                    // Voltar ao painel do jogador
                    if (parentGui != null) {
                        parentGui.open();
                    } else {
                        player.closeInventory();
                    }
                    return;
                }
            }

            switch (slot) {
                case SLOT_PREV -> {
                    if (page > 0) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                        page--;
                        initializeItems();
                    }
                }
                case SLOT_NEXT -> {
                    if ((page + 1) * RACES_PER_PAGE < races.size()) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                        page++;
                        initializeItems();
                    }
                }
                case SLOT_BACK -> {
                    if (parentGui != null) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        parentGui.open();
                    }
                }
                case SLOT_CLOSE -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    player.closeInventory();
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceAdminSetRaceGui para %s no slot %d",
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }
}
