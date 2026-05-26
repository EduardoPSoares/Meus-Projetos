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

import java.util.Map;

/**
 * GUI de Detalhes da Raça Atual do Jogador
 * Design: Hypixel/Wynncraft Premium Style
 * Layout: 4 linhas (36 slots) - Compacto
 */
public class RaceDetailGui extends BaseGui {

    private final RacesModule module;
    private final Race race;
    private final BaseGui parentGui;
    private final MidgardProfile profile;
    private final RaceData raceData;

    // Layout 4 linhas (36 slots)
    private static final int SLOT_RACE_ICON = 13;
    private static final int SLOT_ATTRIBUTES = 20;
    private static final int SLOT_LORE = 24;
    private static final int SLOT_BACK = 27;
    private static final int SLOT_CLOSE = 35;

    public RaceDetailGui(Player player, Race race, BaseGui parentGui) {
        super(player, 4, getTitle(race));
        if (race == null) {
            throw new IllegalArgumentException("race cannot be null");
        }
        this.module = RacesModule.getInstance();
        this.race = race;
        this.parentGui = parentGui;
        this.profile = MidgardCore.getProfileManager().getProfile(player);
        this.raceData = (profile != null) ? profile.getData(RaceData.class) : null;
    }

    private static String getTitle(Race race) {
        return RacesModule.getInstance().getGuiMessage("detail.title")
                .replace("%race%", race != null ? race.getDisplayName() : "?");
    }

    private String gui(String key) {
        return module.getGuiMessage("detail." + key);
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // Decoração
        var pane = RaceGuiTheme.createDarkPane();
        RaceGuiTheme.fillBottomRow(inventory, pane);
        RaceGuiTheme.fillSlots(inventory, pane, 0, 1, 2, 3, 4, 5, 6, 7, 8);

        // Ícone central
        inventory.setItem(SLOT_RACE_ICON, createRaceIcon());

        // Atributos e História
        inventory.setItem(SLOT_ATTRIBUTES, createAttributesItem());
        inventory.setItem(SLOT_LORE, createLoreItem());

        // Navegação
        if (parentGui != null) {
            inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.back"))
                    .build());
        }

        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(module.getGuiMessage("general.close"))
                .build());
    }

    private ItemStack createRaceIcon() {
        if (raceData == null || !raceData.hasRace()) {
            return new ItemStack(Material.BARRIER);
        }

        int level = raceData.getLevel();
        double xp = raceData.getExperience();
        double required = module.getLevelManager().getRequiredExperience(level);
        double percent = (required > 0) ? (xp / required) * 100 : 100;
        String progressBar = RaceGuiTheme.progressBar(percent, 12);

        return new ItemBuilder(race.getIcon())
                .setName(gui("race_icon.name").replace("%race%", race.getDisplayName()))
                .setLoreMultiline(gui("race_icon.lore")
                        .replace("%level%", String.valueOf(level))
                        .replace("%xp%", String.format("%.0f", xp))
                        .replace("%required%", String.format("%.0f", required))
                        .replace("%percent%", String.format("%.1f", percent))
                        .replace("%bar%", progressBar))
                .glow()
                .build();
    }

    private ItemStack createAttributesItem() {
        StringBuilder lore = new StringBuilder(gui("attributes.lore_header"));
        lore.append("\n");

        if (race.getAttributes() == null || race.getAttributes().isEmpty()) {
            lore.append(gui("attributes.no_attributes"));
        } else {
            for (Map.Entry<String, Double> entry : race.getAttributes().entrySet()) {
                String attrName = module.getAttributeName(entry.getKey());
                double value = entry.getValue();

                // Calcular valor total com per-level
                if (race.getPerLevelAttributes() != null 
                        && race.getPerLevelAttributes().containsKey(entry.getKey())) {
                    double perLevel = race.getPerLevelAttributes().get(entry.getKey());
                    int level = (raceData != null) ? raceData.getLevel() : 1;
                    value += perLevel * (level - 1);
                }

                String color = RaceGuiTheme.getValueColor(value);
                String sign = RaceGuiTheme.getValueSign(value);

                lore.append(gui("attributes.attribute_format")
                        .replace("%color%", color)
                        .replace("%sign%", sign)
                        .replace("%value%", RaceGuiTheme.formatValue(value))
                        .replace("%name%", attrName))
                        .append("\n");
            }
        }

        return new ItemBuilder(Material.DIAMOND)
                .setName(gui("attributes.name"))
                .setLoreMultiline(lore.toString())
                .build();
    }

    private ItemStack createLoreItem() {
        String description = (race.getDescription() != null && !race.getDescription().isEmpty())
                ? "<gray>" + String.join("\n<gray>", race.getDescription())
                : gui("lore.no_lore");

        return new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(gui("lore.name"))
                .setLoreMultiline(gui("lore.lore_header") + "\n" + description)
                .build();
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
                case SLOT_CLOSE -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    player.closeInventory();
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceDetailGui para %s no slot %d",
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }
}
