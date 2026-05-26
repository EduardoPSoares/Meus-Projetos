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

import java.util.Map;

public class CampfireRecipeEditorGui extends BaseGui {

    private final ItemModule module;
    private final MidgardItem item;
    private final BaseGui parent;
    private final MidgardRecipe recipe;

    public CampfireRecipeEditorGui(Player player, ItemModule module, MidgardItem item, BaseGui parent, String recipeId) {
        super(player, 6, "key:item.gui.crafting_gui.editor.titles.campfire");
        this.module = module;
        this.item = item;
        this.parent = parent;
        this.recipe = item.getRecipe(recipeId);
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

        // Slot 19: Choose Output Amount
        inventory.setItem(19, new ItemBuilder(Material.LEATHER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.output_amount.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.output_amount.lore"))
                .amount(Math.max(1, recipe.getOutputAmount()))
                .build());

        // Slot 20: Hide from Crafting Book
        boolean hidden = recipe.isHiddenFromBook();
        inventory.setItem(20, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.hide_book.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.hide_book.lore"))
                .addLoreLine("")
                .addLoreLine(hidden ? MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.hide_book.hidden") : MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.hide_book.shown"))
                .build());

        // Slot 21: Experience
        inventory.setItem(21, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.experience.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.experience.lore"))
                .addLoreLine("")
                .addLoreLine(MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.current-value").replace("%value%", String.valueOf(recipe.getExperience())))
                .build());

        // Slot 22: Duration
        inventory.setItem(22, new ItemBuilder(Material.CLOCK)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.duration.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.duration.lore"))
                .addLoreLine("")
                .addLoreLine(MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.current-value").replace("%value%", String.valueOf(recipe.getCookTime())))
                .build());

        // Slots 23, 24, 25: Separator
        ItemStack separator = new ItemBuilder(Material.IRON_BARS)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.separator"))
                .build();
        inventory.setItem(23, separator);
        inventory.setItem(24, separator);
        inventory.setItem(25, separator);

        // Slot 40: Input Item
        Map<Integer, String> ingredients = recipe.getIngredients();
        String ingredient = ingredients.get(0); // Slot 0 for campfire input
        ItemStack inputItem;
        if (ingredient != null) {
            MidgardItem ingItem = module.getItemManager().getItem(ingredient);
            if (ingItem != null) {
                inputItem = ingItem.build();
            } else {
                try {
                    inputItem = new ItemStack(Material.valueOf(ingredient));
                } catch (IllegalArgumentException e) {
                    inputItem = new ItemBuilder(Material.BARRIER).name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.invalid_ingredient").replace("%s", ingredient))).build();
                }
            }
        } else {
            inputItem = new ItemBuilder(Material.BARRIER)
                    .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.no_item"))
                    .build();
        }
        inventory.setItem(40, inputItem);
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

        if (slot == 19) {
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

        if (slot == 20) {
            recipe.setHiddenFromBook(!recipe.isHiddenFromBook());
            item.updateRecipe(recipe);
            initializeItems();
            return;
        }

        if (slot == 21) {
            if (event.getClick() == ClickType.RIGHT) {
                recipe.setExperience(0.35);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new DoubleEditor(player, val -> {
                    recipe.setExperience(val);
                    item.updateRecipe(recipe);
                    open();
                }, MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.prompts.experience")).open();
            }
            return;
        }

        if (slot == 22) {
            if (event.getClick() == ClickType.RIGHT) {
                recipe.setCookTime(200);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new IntegerEditor(player, val -> {
                    recipe.setCookTime(val);
                    item.updateRecipe(recipe);
                    open();
                }, MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.prompts.duration")).open();
            }
            return;
        }

        if (slot == 40) {
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                ItemStack cursor = event.getCursor();
                String ingredient = module.getItemManager().getItemId(cursor);
                if (ingredient == null) {
                    ingredient = cursor.getType().name();
                }
                recipe.setIngredient(0, ingredient);
                item.updateRecipe(recipe);
                initializeItems();
            } else if (event.getClick() == ClickType.RIGHT) {
                recipe.setIngredient(0, null);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new IngredientSelectionGui(player, module, this, selectedIngredient -> {
                    recipe.setIngredient(0, selectedIngredient);
                    item.updateRecipe(recipe);
                    open();
                }).open();
            }
            return;
        }
    }
}
