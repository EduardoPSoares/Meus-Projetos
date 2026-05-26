package me.ray.midgard.modules.races.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.model.ConfiguredTrait;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * GUI de Habilidades Raciais
 * Design: Hypixel/Wynncraft Premium Style
 * Layout: 5 linhas (45 slots) - Grid de habilidades
 */
public class RaceAbilitiesGui extends BaseGui {

    private final RacesModule module;
    @SuppressWarnings("unused")
    private final Race race;
    private final BaseGui parentGui;
    private final MidgardProfile profile;
    private final RaceData raceData;

    private int page = 0;
    private final List<ConfiguredTrait> traits;
    private static final List<Integer> ITEM_SLOTS = RaceGuiTheme.gridSlots5Rows();
    private static final int ITEMS_PER_PAGE = ITEM_SLOTS.size();

    // Layout 5 linhas (45 slots)
    private static final int SLOT_INFO = 4;
    private static final int SLOT_BACK = 36;
    private static final int SLOT_PREV = 39;
    private static final int SLOT_PAGE_INFO = 40;
    private static final int SLOT_NEXT = 41;
    private static final int SLOT_CLOSE = 44;

    public RaceAbilitiesGui(Player player, Race race, BaseGui parentGui) {
        super(player, 5, getTitle());
        if (race == null) {
            throw new IllegalArgumentException("race cannot be null");
        }
        this.module = RacesModule.getInstance();
        this.race = race;
        this.parentGui = parentGui;
        this.profile = MidgardCore.getProfileManager().getProfile(player);
        this.raceData = (profile != null) ? profile.getData(RaceData.class) : null;
        this.traits = (race.getTraits() != null) ? race.getTraits() : List.of();
    }

    private static String getTitle() {
        return RacesModule.getInstance().getGuiMessage("abilities.title");
    }

    private String gui(String key) {
        return module.getGuiMessage("abilities." + key);
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // Decoração
        var pane = RaceGuiTheme.createDarkPane();
        RaceGuiTheme.fillBottomRow(inventory, pane);

        // Info no topo
        inventory.setItem(SLOT_INFO, createInfoItem());

        // Grid de habilidades
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, traits.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex < ITEM_SLOTS.size()) {
                inventory.setItem(ITEM_SLOTS.get(slotIndex), createAbilityItem(traits.get(i)));
            }
        }

        // Navegação
        addNavigation();
    }

    private ItemStack createInfoItem() {
        int currentLevel = (raceData != null) ? raceData.getLevel() : 1;
        int unlocked = (int) traits.stream()
                .filter(t -> t.getMinLevel() <= currentLevel)
                .count();
        int locked = traits.size() - unlocked;

        return new ItemBuilder(Material.NETHER_STAR)
                .setName(gui("info.name"))
                .setLoreMultiline(gui("info.lore")
                        .replace("%total%", String.valueOf(traits.size()))
                        .replace("%unlocked%", String.valueOf(unlocked))
                        .replace("%locked%", String.valueOf(locked)))
                .glow()
                .build();
    }

    private ItemStack createAbilityItem(ConfiguredTrait trait) {
        int currentLevel = (raceData != null) ? raceData.getLevel() : 1;
        boolean unlocked = trait.getMinLevel() <= currentLevel;

        String traitName = trait.getId();
        String description = extractDescription(trait);

        if (unlocked) {
            return new ItemBuilder(Material.ENCHANTED_BOOK)
                    .setName(gui("ability_unlocked.name").replace("%name%", traitName))
                    .setLoreMultiline(gui("ability_unlocked.lore").replace("%description%", description))
                    .glow()
                    .build();
        } else {
            return new ItemBuilder(Material.BOOK)
                    .setName(gui("ability_locked.name").replace("%name%", traitName))
                    .setLoreMultiline(gui("ability_locked.lore")
                            .replace("%required%", String.valueOf(trait.getMinLevel()))
                            .replace("%current%", String.valueOf(currentLevel))
                            .replace("%description%", description))
                    .build();
        }
    }

    private String extractDescription(ConfiguredTrait trait) {
        String fallback = gui("no_description");
        Object desc = trait.getConfig().get("description");
        if (desc instanceof List<?> list && !list.isEmpty()) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse(fallback);
        }
        return fallback;
    }

    private void addNavigation() {
        int totalPages = Math.max(1, (int) Math.ceil((double) traits.size() / ITEMS_PER_PAGE));
        
        // Voltar
        if (parentGui != null) {
            inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.back"))
                    .build());
        }

        // Página anterior
        if (page > 0) {
            inventory.setItem(SLOT_PREV, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.previous_page"))
                    .build());
        }

        // Info de página
        inventory.setItem(SLOT_PAGE_INFO, new ItemBuilder(Material.PAPER)
                .setName(module.getGuiMessage("general.page_info")
                        .replace("%current%", String.valueOf(page + 1))
                        .replace("%total%", String.valueOf(totalPages)))
                .build());

        // Próxima página
        if ((page + 1) * ITEMS_PER_PAGE < traits.size()) {
            inventory.setItem(SLOT_NEXT, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.next_page"))
                    .build());
        }

        // Fechar
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(module.getGuiMessage("general.close"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event == null) { return; }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) { return; }
        if (!player.equals(event.getWhoClicked())) { return; }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) { return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) { return; }

        try {
            switch (slot) {
                case SLOT_BACK -> {
                    if (parentGui != null) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        parentGui.open();
                    }
                }
                case SLOT_PREV -> {
                    if (page > 0) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                        page--;
                        initializeItems();
                    }
                }
                case SLOT_NEXT -> {
                    if ((page + 1) * ITEMS_PER_PAGE < traits.size()) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                        page++;
                        initializeItems();
                    }
                }
                case SLOT_CLOSE -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    player.closeInventory();
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceAbilitiesGui para %s no slot %d",
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }
}
