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
 * GUI de Preview/Detalhes de uma Raça
 * Design: Hypixel/Wynncraft Premium Style
 * Layout: 5 linhas (45 slots)
 */
public class RacePreviewGui extends BaseGui {

    private final RacesModule module;
    private final Race race;
    private final BaseGui parentGui;
    private final MidgardProfile profile;
    private final RaceData raceData;

    // Layout 5 linhas (45 slots)
    // Linha 1: Decoração
    // Linha 2: Ícone central da raça
    // Linha 3: Atributos e Habilidades
    // Linha 4: Botão de confirmar
    // Linha 5: Navegação
    private static final int SLOT_RACE_ICON = 13;
    private static final int SLOT_ATTRIBUTES = 20;
    private static final int SLOT_ABILITIES = 24;
    private static final int SLOT_CONFIRM = 31;
    private static final int SLOT_BACK = 36;
    private static final int SLOT_CLOSE = 44;

    public RacePreviewGui(Player player, Race race, BaseGui parentGui) {
        super(player, 5, getTitle(race));
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
        return RacesModule.getInstance().getGuiMessage("preview.title")
                .replace("%race%", race != null ? race.getDisplayName() : "?");
    }

    private String gui(String key) {
        return module.getGuiMessage("preview." + key);
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

        // Informações
        inventory.setItem(SLOT_ATTRIBUTES, createAttributesItem());
        inventory.setItem(SLOT_ABILITIES, createAbilitiesItem());

        // Botão confirmar
        inventory.setItem(SLOT_CONFIRM, createConfirmButton());

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
        String description = getDescription();

        return new ItemBuilder(race.getIcon())
                .setName(gui("race_icon.name").replace("%race%", race.getDisplayName()))
                .setLoreMultiline(gui("race_icon.lore").replace("%description%", description))
                .glow()
                .build();
    }

    private ItemStack createAttributesItem() {
        StringBuilder lore = new StringBuilder(gui("attributes.lore_header"));
        lore.append("\n");

        // Atributos base
        if (race.getAttributes() == null || race.getAttributes().isEmpty()) {
            lore.append(gui("attributes.no_attributes")).append("\n");
        } else {
            for (Map.Entry<String, Double> entry : race.getAttributes().entrySet()) {
                String attrName = module.getAttributeName(entry.getKey());
                double value = entry.getValue();
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

        // Atributos por nível
        if (race.getPerLevelAttributes() != null && !race.getPerLevelAttributes().isEmpty()) {
            lore.append(gui("attributes.lore_per_level")).append("\n");

            for (Map.Entry<String, Double> entry : race.getPerLevelAttributes().entrySet()) {
                String attrName = module.getAttributeName(entry.getKey());
                double value = entry.getValue();
                String sign = value >= 0 ? "+" : "";

                lore.append(gui("attributes.attribute_format")
                        .replace("%color%", "yellow")
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

    private ItemStack createAbilitiesItem() {
        int count = (race.getTraits() != null) ? race.getTraits().size() : 0;

        return new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName(gui("abilities.name"))
                .setLoreMultiline(gui("abilities.lore").replace("%count%", String.valueOf(count)))
                .build();
    }

    private ItemStack createConfirmButton() {
        boolean isCurrent = raceData != null && raceData.hasRace()
                && raceData.getRaceId().equals(race.getId());

        if (isCurrent) {
            return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .setName(gui("confirm.name_current"))
                    .setLoreMultiline(gui("confirm.lore_current"))
                    .build();
        } else {
            return new ItemBuilder(Material.EMERALD)
                    .setName(gui("confirm.name_new"))
                    .setLoreMultiline(gui("confirm.lore_new").replace("%race%", race.getDisplayName()))
                    .glow()
                    .build();
        }
    }

    private String getDescription() {
        if (race.getDescription() == null || race.getDescription().isEmpty()) {
            return module.getGuiMessage("general.no_description");
        }
        return "<gray>" + String.join("\n<gray>", race.getDescription());
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
                case SLOT_CONFIRM -> {
                    boolean isCurrent = raceData != null && raceData.hasRace()
                            && raceData.getRaceId().equals(race.getId());

                    if (!isCurrent) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                        new RaceConfirmationGui(player, race, this).open();
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RacePreviewGui para %s no slot %d",
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }
}
