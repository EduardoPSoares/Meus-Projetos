package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.gui.editors.impl.*;
import me.ray.midgard.modules.item.gui.editors.impl.ForgeRecipeEditorGui;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.model.MidgardRecipe;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class RecipeConfigurationGui extends PaginatedGui<MidgardRecipe> {

    private final ItemModule module;
    private final MidgardItem item;
    private final BaseGui parent;

    public RecipeConfigurationGui(Player player, ItemModule module, MidgardItem item, BaseGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.configuration.title"), new ArrayList<>());
        this.module = module;
        this.item = item;
        this.parent = parent;
        this.items = item.getRecipes(); // Load recipes
    }

    @Override
    public void initializeItems() {
        this.items = item.getRecipes(); // Refresh recipes list
        super.initializeItems();
    }

    @Override
    public ItemStack createItem(MidgardRecipe recipe) {
        String typeName = formatType(recipe.getType().name());
        
        ItemBuilder builder = new ItemBuilder(getIconForType(recipe.getType()))
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.configuration.recipe_item.name", "%type%", typeName))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.configuration.recipe_item.lore"))
                .replaceLorePlaceholder("%id%", recipe.getId())
                .replaceLorePlaceholder("%amount%", String.valueOf(recipe.getOutputAmount()));
                
        return builder.build();
    }
    
    private Material getIconForType(MidgardRecipe.RecipeType type) {
        switch (type) {
            case SHAPED: return Material.CRAFTING_TABLE;
            case SHAPELESS: return Material.OAK_LOG;
            case FURNACE: return Material.FURNACE;
            case BLAST_FURNACE: return Material.BLAST_FURNACE;
            case SMOKER: return Material.SMOKER;
            case CAMPFIRE: return Material.CAMPFIRE;
            case SMITHING: return Material.SMITHING_TABLE;
            case MEGA_SHAPED: return Material.JUKEBOX;
            case SUPER_SHAPED: return Material.NOTE_BLOCK;
            case STONE_CUTTING: return Material.STONECUTTER;
            case FORGE: return Material.ANVIL;
            case SMELTING: return Material.SOUL_CAMPFIRE;
            default: return Material.PAPER;
        }
    }

    private String formatType(String type) {
        if (type == null) {
            return "";
        }
        String lower = type.toLowerCase().replace("_", " ");
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    @Override
    public void addMenuBorder() {
        super.addMenuBorder();
        
        // Back button (Overwrites Close button from super)
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.back"))
                .build());
                
        // Add New Recipe Button (Emerald)
        inventory.setItem(50, new ItemBuilder(Material.EMERALD)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.configuration.new_recipe.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.configuration.new_recipe.lore"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 49) {
            parent.open();
            return;
        }
        
        if (slot == 50) {
            new CraftingTypeSelectionGui(player, module, item, this).open();
            return;
        }

        // Handle pagination clicks
        int row = slot / 9;
        int col = slot % 9;
        
        if (row >= 1 && row <= 3 && col >= 1 && col <= 7) {
            int relativeIndex = (row - 1) * 7 + (col - 1);
            int index = relativeIndex + (page * maxItemsPerPage);

            if (index < items.size()) {
                MidgardRecipe recipe = items.get(index);
                
                if (event.getClick().isRightClick()) {
                    // Delete
                    item.removeRecipe(recipe.getId());
                    player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.configuration.removed")));
                    new RecipeConfigurationGui(player, module, item, parent).open(); // Reload
                } else {
                    // Edit
                    openEditor(recipe);
                }
                return;
            }
        }
        
        super.onClick(event);
    }
    
    private void openEditor(MidgardRecipe recipe) {
        String type = recipe.getType().name();
        String recipeId = recipe.getId();
        
        if ("SMITHING".equalsIgnoreCase(type)) {
            new SmithingRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("SUPER_SHAPED".equalsIgnoreCase(type)) {
            new SuperShapedRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("SMOKER".equalsIgnoreCase(type)) {
            new SmokerRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("FURNACE".equalsIgnoreCase(type)) {
            new FurnaceRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("SHAPED".equalsIgnoreCase(type)) {
            new ShapedRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("MEGA_SHAPED".equalsIgnoreCase(type)) {
            new MegaShapedRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("CAMPFIRE".equalsIgnoreCase(type)) {
            new CampfireRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("SHAPELESS".equalsIgnoreCase(type)) {
            new ShapelessRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("BLAST_FURNACE".equalsIgnoreCase(type)) {
            new BlastFurnaceRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("FORGE".equalsIgnoreCase(type)) {
            new ForgeRecipeEditorGui(player, module, item, this, recipeId).open();
        } else if ("SMELTING".equalsIgnoreCase(type)) {
            new SmeltingRecipeEditorGui(player, module, item, this, recipeId).open();
        } else {
            player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.configuration.editing_wip")));
        }
    }
}
