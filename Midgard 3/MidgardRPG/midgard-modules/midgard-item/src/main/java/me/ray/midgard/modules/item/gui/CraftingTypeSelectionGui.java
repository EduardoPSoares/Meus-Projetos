package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.model.MidgardRecipe;
import me.ray.midgard.modules.item.model.MidgardRecipe.RecipeType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class CraftingTypeSelectionGui extends BaseGui {

    private final MidgardItem item;
    private final BaseGui parent;

    public CraftingTypeSelectionGui(Player player, ItemModule module, MidgardItem item, BaseGui parent) {
        super(player, 6, MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.type_selection.title"));
        this.item = item;
        this.parent = parent;
    }

    @Override
    public void initializeItems() {
        // Background filler removed per user request


        // Slot 2: Chest - Get Item
        inventory.setItem(2, new ItemBuilder(Material.CHEST)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.get_item.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.get_item.lore"))
                .build());

        // Slot 4: Item Display
        ItemStack displayItem = item.build();
        inventory.setItem(4, displayItem);

        // 19: Smithing Table
        inventory.setItem(19, new ItemBuilder(Material.SMITHING_TABLE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.smithing"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 20: Note Block - Super Shaped
        inventory.setItem(20, new ItemBuilder(Material.NOTE_BLOCK)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.super_shaped"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 21: Smoker - Smoker
        inventory.setItem(21, new ItemBuilder(Material.SMOKER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.smoker"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 22: Furnace - Furnace
        inventory.setItem(22, new ItemBuilder(Material.FURNACE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.furnace"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 23: Crafting Table - Shaped
        inventory.setItem(23, new ItemBuilder(Material.CRAFTING_TABLE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.shaped"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 24: Jukebox - Mega Shaped
        inventory.setItem(24, new ItemBuilder(Material.JUKEBOX)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.mega_shaped"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 25: Campfire - Campfire
        inventory.setItem(25, new ItemBuilder(Material.CAMPFIRE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.campfire"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 28: Oak Log - Shapeless
        inventory.setItem(28, new ItemBuilder(Material.OAK_LOG)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.shapeless"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 29: Blast Furnace - Blast
        inventory.setItem(29, new ItemBuilder(Material.BLAST_FURNACE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.blast_furnace"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 30: Anvil - Forge
        inventory.setItem(30, new ItemBuilder(Material.ANVIL)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.forge"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // 31: Blast Furnace - Smelting (Smeltery)
        inventory.setItem(31, new ItemBuilder(Material.SOUL_CAMPFIRE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.type_selection.types.smelting"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.type_selection.click_to_manage"))
                .build());

        // Back button
        inventory.setItem(45, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.back")).build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45) {
            parent.open();
            return;
        }

        if (slot == 2) {
            // Give item
            ItemStack result = item.build();
            int amount = item.getRecipes().isEmpty() ? 1 : item.getRecipes().get(0).getOutputAmount();
            result.setAmount(amount);
            player.getInventory().addItem(result);
            player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.common.item_added")));
            return;
        }

        RecipeType type = null;
        switch (slot) {
            case 19: type = RecipeType.SMITHING; break;
            case 20: type = RecipeType.SUPER_SHAPED; break;
            case 21: type = RecipeType.SMOKER; break;
            case 22: type = RecipeType.FURNACE; break;
            case 23: type = RecipeType.SHAPED; break;
            case 24: type = RecipeType.MEGA_SHAPED; break;
            case 25: type = RecipeType.CAMPFIRE; break;
            case 28: type = RecipeType.SHAPELESS; break;
            case 29: type = RecipeType.BLAST_FURNACE; break;
            case 30: type = RecipeType.FORGE; break;
            case 31: type = RecipeType.SMELTING; break;
        }

        if (type != null) {
            MidgardRecipe recipe = new MidgardRecipe(type);
            item.addRecipe(recipe); // Saves automatically
            player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.configuration.created")));
            
            // Just go back to the list instead of opening the editor
            parent.open();
        }
    }
    
}
