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
import java.util.List;
import java.util.Map;

public class ShapedRecipeEditorGui extends BaseGui {

    private final ItemModule module;
    private final MidgardItem item;
    private final BaseGui parent;
    private final MidgardRecipe recipe;
    private final Map<Integer, Integer> slotMap = new HashMap<>();

    public ShapedRecipeEditorGui(Player player, ItemModule module, MidgardItem item, BaseGui parent, String recipeId) {
        super(player, 6, "key:item.gui.crafting_gui.editor.titles.shaped");
        this.module = module;
        this.item = item;
        this.parent = parent;
        this.recipe = item.getRecipe(recipeId);
        
        // Map GUI slots to Recipe slots (1-9)
        slotMap.put(30, 1);
        slotMap.put(31, 2);
        slotMap.put(32, 3);
        slotMap.put(39, 4);
        slotMap.put(40, 5);
        slotMap.put(41, 6);
        slotMap.put(48, 7);
        slotMap.put(49, 8);
        slotMap.put(50, 9);
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

        // Slot 2: Get Item
        inventory.setItem(2, new ItemBuilder(Material.CHEST)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.get_item.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.get_item.lore"))
                .build());

        // Slot 4: Display Item
        inventory.setItem(4, item.build());

        // Slot 6: Back
        inventory.setItem(6, new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.back"))
                .build());

        // Slot 10: Choose Output Amount
        List<String> amountLore = MidgardCore.getLanguageManager().getStringList("item.gui.crafting_gui.editor.common.output_amount.lore");
        amountLore.replaceAll(l -> l.replace("%s", String.valueOf(recipe.getOutputAmount())));
        inventory.setItem(10, new ItemBuilder(Material.LEATHER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.output_amount.name"))
                .lore(amountLore)
                .amount(Math.max(1, recipe.getOutputAmount()))
                .build());

        // Slot 11: Switch Output Mode
        boolean isShaped = recipe.isShaped();
        List<String> modeLore = MidgardCore.getLanguageManager().getStringList("item.gui.crafting_gui.editor.common.output_mode.lore");
        modeLore.replaceAll(l -> l.replace("%s", isShaped ? MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.output_mode.shaped") : MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.output_mode.shapeless")));
        inventory.setItem(11, new ItemBuilder(Material.CRAFTING_TABLE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.output_mode.name"))
                .lore(modeLore)
                .build());

        // Slot 12: Hide from Crafting Book
        boolean hidden = recipe.isHiddenFromBook();
        List<String> hideLore = MidgardCore.getLanguageManager().getStringList("item.gui.crafting_gui.editor.common.hide_book.lore");
        hideLore.replaceAll(l -> l.replace("%s", hidden ? MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.hide_book.hidden") : MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.hide_book.shown")));
        inventory.setItem(12, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.hide_book.name"))
                .lore(hideLore)
                .build());

        // Separators
        ItemStack separator = new ItemBuilder(Material.IRON_BARS).name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.separator")).build();
        for (int i = 13; i <= 17; i++) {
            inventory.setItem(i, separator);
        }

        // 3x3 Grid
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

        if (slot == 6) {
            parent.open();
            return;
        }
        
        if (slot == 2) {
             ItemStack result = item.build();
             result.setAmount(recipe.getOutputAmount());
             player.getInventory().addItem(result);
             player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.item_added"));
             return;
        }

        if (slot == 10) {
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

        if (slot == 11) {
            recipe.setShaped(!recipe.isShaped());
            item.updateRecipe(recipe);
            initializeItems();
            return;
        }

        if (slot == 12) {
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
                new IngredientSelectionGui(player, module, this, (ingredient) -> {
                    recipe.setIngredient(recipeSlot, ingredient);
                    item.updateRecipe(recipe);
                    open();
                }).open();
            }
            return;
        }
    }
}
