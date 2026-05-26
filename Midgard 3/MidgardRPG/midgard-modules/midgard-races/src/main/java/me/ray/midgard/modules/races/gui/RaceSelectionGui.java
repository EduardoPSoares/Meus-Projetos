package me.ray.midgard.modules.races.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * GUI de Seleção de Raças
 * Design: Hypixel/Wynncraft Premium Style
 * Layout: 5 linhas (45 slots) - Grid de raças
 */
public class RaceSelectionGui extends BaseGui {

    private final RacesModule module;
    private final BaseGui parentGui;
    private final MidgardProfile profile;
    private final RaceData raceData;

    private int page = 0;
    private final List<Race> races;
    private static final List<Integer> ITEM_SLOTS = RaceGuiTheme.gridSlots5Rows();
    private static final int ITEMS_PER_PAGE = ITEM_SLOTS.size();

    // Layout 5 linhas (45 slots)
    private static final int SLOT_BACK = 36;
    private static final int SLOT_PREV = 39;
    private static final int SLOT_INFO = 40;
    private static final int SLOT_NEXT = 41;
    private static final int SLOT_CLOSE = 44;

    public RaceSelectionGui(Player player, BaseGui parentGui) {
        super(player, 5, getTitle());
        this.module = RacesModule.getInstance();
        this.parentGui = parentGui;
        this.profile = MidgardCore.getProfileManager().getProfile(player);
        this.raceData = (profile != null) ? profile.getData(RaceData.class) : null;
        this.races = loadAvailableRaces();
    }

    private static String getTitle() {
        return RacesModule.getInstance().getGuiMessage("selection.title");
    }

    private String gui(String key) {
        return module.getGuiMessage("selection." + key);
    }

    private List<Race> loadAvailableRaces() {
        return module.getRaceManager().getRaces().stream()
                .filter(r -> !r.isSubRace())
                .sorted(Comparator.comparing(Race::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // Decoração - apenas linha inferior
        var pane = RaceGuiTheme.createDarkPane();
        RaceGuiTheme.fillBottomRow(inventory, pane);

        // Grid de raças
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, races.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex < ITEM_SLOTS.size()) {
                inventory.setItem(ITEM_SLOTS.get(slotIndex), createRaceItem(races.get(i)));
            }
        }

        // Navegação
        addNavigation();
    }

    private ItemStack createRaceItem(Race race) {
        boolean isCurrent = raceData != null && raceData.hasRace() 
                && raceData.getRaceId().equals(race.getId());

        // Descrição
        String description = getShortDescription(race);
        
        // Atributos
        StringBuilder attributes = new StringBuilder();
        if (race.getAttributes() == null || race.getAttributes().isEmpty()) {
            attributes.append(gui("race_item.no_attributes"));
        } else {
            for (Map.Entry<String, Double> entry : race.getAttributes().entrySet()) {
                String attrName = module.getAttributeName(entry.getKey());
                double value = entry.getValue();
                String color = RaceGuiTheme.getValueColor(value);
                String sign = RaceGuiTheme.getValueSign(value);
                
                attributes.append(gui("race_item.attribute_format")
                        .replace("%color%", color)
                        .replace("%sign%", sign)
                        .replace("%value%", RaceGuiTheme.formatValue(value))
                        .replace("%name%", attrName))
                        .append("\n");
            }
        }

        // Habilidades
        int traitCount = (race.getTraits() != null) ? race.getTraits().size() : 0;

        // Status
        String status = isCurrent 
                ? gui("race_item.status_current")
                : gui("race_item.status_available");

        // Montar lore
        String lore = gui("race_item.lore_template")
                .replace("%description%", description)
                .replace("%attributes%", attributes.toString().trim())
                .replace("%abilities%", String.valueOf(traitCount))
                .replace("%status%", status);

        ItemBuilder builder = new ItemBuilder(race.getIcon())
                .setName(gui("race_item.name").replace("%race%", race.getDisplayName()))
                .setLoreMultiline(lore);

        if (isCurrent) {
            builder.glow();
        }

        return builder.build();
    }

    private String getShortDescription(Race race) {
        if (race.getDescription() == null || race.getDescription().isEmpty()) {
            return module.getGuiMessage("general.no_description");
        }
        
        int maxLines = Math.min(2, race.getDescription().size());
        StringBuilder sb = new StringBuilder("<gray>");
        for (int i = 0; i < maxLines; i++) {
            if (i > 0) { sb.append("\n<gray>"); }
            sb.append(race.getDescription().get(i));
        }
        
        return sb.toString();
    }

    private void addNavigation() {
        int totalPages = Math.max(1, (int) Math.ceil((double) races.size() / ITEMS_PER_PAGE));
        
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
        inventory.setItem(SLOT_INFO, new ItemBuilder(Material.PAPER)
                .setName(module.getGuiMessage("general.page_info")
                        .replace("%current%", String.valueOf(page + 1))
                        .replace("%total%", String.valueOf(totalPages)))
                .build());

        // Próxima página
        if ((page + 1) * ITEMS_PER_PAGE < races.size()) {
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
            // Navegação
            if (slot == SLOT_BACK && parentGui != null) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                parentGui.open();
                return;
            }

            if (slot == SLOT_PREV && page > 0) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                page--;
                initializeItems();
                return;
            }

            if (slot == SLOT_NEXT && (page + 1) * ITEMS_PER_PAGE < races.size()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                page++;
                initializeItems();
                return;
            }

            if (slot == SLOT_CLOSE) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                player.closeInventory();
                return;
            }

            // Clique em raça
            int slotIndex = ITEM_SLOTS.indexOf(slot);
            if (slotIndex >= 0) {
                int index = page * ITEMS_PER_PAGE + slotIndex;
                if (index >= 0 && index < races.size()) {
                    Race race = races.get(index);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new RacePreviewGui(player, race, this).open();
                }
            }

        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceSelectionGui para %s no slot %d", 
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }
}
