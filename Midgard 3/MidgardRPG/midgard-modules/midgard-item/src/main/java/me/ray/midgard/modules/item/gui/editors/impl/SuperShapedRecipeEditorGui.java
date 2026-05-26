package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.model.MidgardRecipe;
import me.ray.midgard.modules.item.gui.IngredientSelectionGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SuperShapedRecipeEditorGui extends BaseGui {

    private final ItemModule module;
    private final MidgardItem item;
    private final BaseGui parent;
    private final MidgardRecipe recipe;
    private final Map<Integer, Integer> slotMap = new HashMap<>();

    public SuperShapedRecipeEditorGui(Player player, ItemModule module, MidgardItem item, BaseGui parent, String recipeId) {
        super(player, 6, "key:item.gui.crafting_gui.editor.titles.super_shaped");
        this.module = module;
        this.item = item;
        this.parent = parent;
        this.recipe = item.getRecipe(recipeId);

        // Map 5x5 Grid Slots to Recipe Slots (1-25)
        int[] gridSlots = {
                11, 12, 13, 14, 15,
                20, 21, 22, 23, 24,
                29, 30, 31, 32, 33,
                38, 39, 40, 41, 42,
                47, 48, 49, 50, 51
        };
        
        for (int i = 0; i < gridSlots.length; i++) {
            slotMap.put(gridSlots[i], i + 1);
        }
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        if (recipe == null) {
            player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.error.recipe_not_found")));
            parent.open();
            return;
        }

        // Background filler
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(MessageUtils.parse(" ")).build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // Slot 1: Choose Output Amount
        inventory.setItem(1, new ItemBuilder(Material.LEATHER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.output_amount.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.output_amount.lore"))
                .amount(Math.max(1, recipe.getOutputAmount()))
                .build());

        // Slot 2: Output Mode
        boolean isShaped = recipe.isShaped();
        inventory.setItem(2, new ItemBuilder(Material.CRAFTING_TABLE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.output_mode.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.output_mode.lore"))
                .addLoreLine("")
                .addLoreLine(isShaped ? MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.output_mode.shaped") : MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.output_mode.shapeless"))
                .build());

        // Slot 3: Hide from Book
        boolean hidden = recipe.isHiddenFromBook();
        inventory.setItem(3, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.hide_book.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.hide_book.lore"))
                .addLoreLine("")
                .addLoreLine(hidden ? MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.hide_book.hidden") : MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.hide_book.shown"))
                .build());

        // Slots 4-7: Separator
        ItemStack separator = new ItemBuilder(Material.IRON_BARS)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.separator"))
                .build();
        for (int i = 4; i <= 7; i++) {
            inventory.setItem(i, separator);
        }

        // Back button
        inventory.setItem(0, new ItemBuilder(Material.ARROW)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.back"))
                .build());

        // 5x5 Grid
        Map<Integer, String> ingredients = recipe.getIngredients();
        ItemStack noItem = new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.no_item"))
                .build();

        for (Map.Entry<Integer, Integer> entry : slotMap.entrySet()) {
            int guiSlot = entry.getKey();
            int recipeSlot = entry.getValue();
            String ingredient = ingredients.get(recipeSlot);
            
            if (ingredient != null) {
                MidgardItem ingItem = module.getItemManager().getItem(ingredient);
                if (ingItem != null) {
                    inventory.setItem(guiSlot, ingItem.build());
                } else {
                    try {
                        inventory.setItem(guiSlot, new ItemStack(Material.valueOf(ingredient)));
                    } catch (IllegalArgumentException e) {
                        inventory.setItem(guiSlot, new ItemBuilder(Material.BARRIER).name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.invalid_ingredient").replace("%s", ingredient))).build());
                    }
                }
            } else {
                inventory.setItem(guiSlot, noItem);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 0) {
            parent.open();
            return;
        }

        if (slot == 1) {
            if (event.getClick() == ClickType.RIGHT) {
                recipe.setOutputAmount(1);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new IntegerEditor(player, val -> {
                    recipe.setOutputAmount(val);
                    item.updateRecipe(recipe);
                    open();
                }, MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.prompts.output_amount")).open();
            }
            return;
        }

        if (slot == 2) {
            recipe.setShaped(!recipe.isShaped());
            item.updateRecipe(recipe);
            initializeItems();
            return;
        }

        if (slot == 3) {
            recipe.setHiddenFromBook(!recipe.isHiddenFromBook());
            item.updateRecipe(recipe);
            initializeItems();
            return;
        }

        if (slotMap.containsKey(slot)) {
            int recipeSlot = slotMap.get(slot);
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                ItemStack cursor = event.getCursor();
                String ingredient = module.getItemManager().getItemId(cursor);
                if (ingredient == null) {
                    ingredient = cursor.getType().name();
                }
                recipe.setIngredient(recipeSlot, ingredient);
                item.updateRecipe(recipe);
                initializeItems();
            } else if (event.getClick() == ClickType.RIGHT) {
                recipe.setIngredient(recipeSlot, null);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new IngredientSelectionGui(player, module, this, selectedIngredient -> {
                    recipe.setIngredient(recipeSlot, selectedIngredient);
                    item.updateRecipe(recipe);
                    open();
                }).open();
            }
            return;
        }
    }
}
