package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.item.ItemModule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;

public class IngredientSelectionGui extends BaseGui {

    private final ItemModule module;
    private final Consumer<String> onSelect;
    private final BaseGui parent;

    public IngredientSelectionGui(Player player, ItemModule module, BaseGui parent, Consumer<String> onSelect) {
        super(player, 3, MidgardCore.getLanguageManager().getRawMessage("item.gui.ingredient_selection.title"));
        this.module = module;
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    public void initializeItems() {
        // Background filler removed per user request


        // Slot 11: Vanilla Item
        inventory.setItem(11, new ItemBuilder(Material.GRASS_BLOCK)
                .name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.ingredient_selection.minecraft_item.name")))
                .lore(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.ingredient_selection.minecraft_item.lore")))
                .build());

        // Slot 15: Midgard Item
        inventory.setItem(15, new ItemBuilder(Material.DIAMOND_SWORD)
                .name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.ingredient_selection.midgard_item.name")))
                .lore(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.ingredient_selection.midgard_item.lore")))
                .build());

        // Slot 22: Back
        inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.common.back")))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 11) {
            // Open Material Selection
            new MaterialSelectionGui(player, module, (mat) -> {
                if (onSelect != null) {
                    onSelect.accept(mat.name());
                }
                parent.open();
            }, this).open();
        } else if (slot == 15) {
            // Open Item Browser for selection
             new TypeBrowserGui(player, module, (item) -> {
                 if (onSelect != null) {
                     onSelect.accept(item.getId());
                 }
                 parent.open();
             }, this).open();
        } else if (slot == 22) {
            parent.open();
        }
    }
}
