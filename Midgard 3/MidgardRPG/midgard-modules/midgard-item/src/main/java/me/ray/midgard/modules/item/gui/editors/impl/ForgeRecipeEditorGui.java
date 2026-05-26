package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.model.MidgardRecipe;
import me.ray.midgard.modules.item.gui.IngredientSelectionGui;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Editor GUI for FORGE recipe type.
 * Allows admins to configure forge-specific recipe properties:
 * - Primary metal ingredient (slot 0)
 * - Secondary materials (slots 1-4)
 * - Output amount
 * - Difficulty level (1-10)
 * - Minimum forge profession level
 * - Required forge tier
 * - Forge recipe ID link
 * - Hidden from book toggle
 */
public class ForgeRecipeEditorGui extends BaseGui {

    private static String msg(String key) {
        return MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.forge." + key);
    }

    private final ItemModule module;
    private final MidgardItem item;
    private final BaseGui parent;
    private final MidgardRecipe recipe;

    // Layout constants
    private static final int SLOT_GET_ITEM = 2;
    private static final int SLOT_DISPLAY = 4;
    private static final int SLOT_BACK = 6;
    private static final int SLOT_OUTPUT_AMOUNT = 19;
    private static final int SLOT_HIDE_BOOK = 20;
    private static final int SLOT_DIFFICULTY = 21;
    private static final int SLOT_MIN_LEVEL = 22;
    private static final int SLOT_FORGE_TIER = 23;
    private static final int SLOT_FORGE_RECIPE_ID = 24;
    private static final int SLOT_PRIMARY_METAL = 38;
    private static final int SLOT_SECONDARY_1 = 39;
    private static final int SLOT_SECONDARY_2 = 40;
    private static final int SLOT_SECONDARY_3 = 41;
    private static final int SLOT_SECONDARY_4 = 42;

    public ForgeRecipeEditorGui(Player player, ItemModule module, MidgardItem item, BaseGui parent, String recipeId) {
        super(player, 6, msg("title"));
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

        // Get Item button
        inventory.setItem(SLOT_GET_ITEM, new ItemBuilder(Material.CHEST)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.get_item.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.get_item.lore"))
                .build());

        // Display Item
        inventory.setItem(SLOT_DISPLAY, item.build());

        // Back
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.BARRIER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.back"))
                .build());

        // Output Amount
        inventory.setItem(SLOT_OUTPUT_AMOUNT, new ItemBuilder(Material.LEATHER)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.output_amount.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.output_amount.lore"))
                .amount(Math.max(1, recipe.getOutputAmount()))
                .build());

        // Hide from Book
        boolean hidden = recipe.isHiddenFromBook();
        inventory.setItem(SLOT_HIDE_BOOK, new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.hide_book.name"))
                .lore(MidgardCore.getLanguageManager().getMessageList("item.gui.crafting_gui.editor.common.hide_book.lore"))
                .addLoreLine("")
                .addLoreLine(hidden ? MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.hide_book.hidden")
                        : MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.common.hide_book.shown"))
                .build());

        // Difficulty (1-10)
        int difficulty = recipe.getForgeDifficulty();
        inventory.setItem(SLOT_DIFFICULTY, new ItemBuilder(Material.IRON_SWORD)
                .setName(msg("difficulty_name"))
                .addLoreLine(msg("difficulty_lore1"))
                .addLoreLine(msg("difficulty_lore2"))
                .addLoreLine(msg("difficulty_lore3"))
                .addLoreLine("")
                .addLoreLine(msg("difficulty_current") + (difficulty > 0 ? difficulty : 1) + "/10")
                .addLoreLine("")
                .addLoreLine(msg("click_edit"))
                .addLoreLine(msg("click_reset"))
                .amount(Math.max(1, difficulty))
                .build());

        // Min Level
        int minLevel = recipe.getForgeMinLevel();
        inventory.setItem(SLOT_MIN_LEVEL, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(msg("min_level_name"))
                .addLoreLine(msg("min_level_lore1"))
                .addLoreLine(msg("min_level_lore2"))
                .addLoreLine("")
                .addLoreLine(msg("min_level_current") + minLevel)
                .addLoreLine("")
                .addLoreLine(msg("click_edit"))
                .addLoreLine(msg("click_reset"))
                .build());

        // Forge Tier
        String tier = recipe.getForgeTier() != null ? recipe.getForgeTier() : "BASIC";
        inventory.setItem(SLOT_FORGE_TIER, new ItemBuilder(Material.SMITHING_TABLE)
                .setName(msg("tier_name"))
                .addLoreLine(msg("tier_lore1"))
                .addLoreLine(msg("tier_lore2"))
                .addLoreLine("")
                .addLoreLine(msg("tier_current") + tier)
                .addLoreLine("")
                .addLoreLine(msg("tier_click"))
                .addLoreLine(msg("tier_cycle"))
                .build());

        // Forge Recipe ID
        String forgeRecipeId = recipe.getForgeRecipeId() != null ? recipe.getForgeRecipeId() : msg("none");
        inventory.setItem(SLOT_FORGE_RECIPE_ID, new ItemBuilder(Material.NAME_TAG)
                .setName(msg("recipe_id_name"))
                .addLoreLine(msg("recipe_id_lore1"))
                .addLoreLine(msg("recipe_id_lore2"))
                .addLoreLine("")
                .addLoreLine(msg("recipe_id_current") + forgeRecipeId)
                .addLoreLine("")
                .addLoreLine(msg("recipe_id_click"))
                .build());

        // Separator
        ItemStack separator = new ItemBuilder(Material.IRON_BARS)
                .setName(msg("separator"))
                .build();
        inventory.setItem(29, separator);
        inventory.setItem(30, separator);
        inventory.setItem(31, separator);
        inventory.setItem(32, separator);
        inventory.setItem(33, separator);

        // Primary Metal (slot 0)
        displayIngredient(SLOT_PRIMARY_METAL, 0, msg("primary_metal"), true);

        // Secondary Materials (slots 1-4)
        displayIngredient(SLOT_SECONDARY_1, 1, msg("secondary_1"), false);
        displayIngredient(SLOT_SECONDARY_2, 2, msg("secondary_2"), false);
        displayIngredient(SLOT_SECONDARY_3, 3, msg("secondary_3"), false);
        displayIngredient(SLOT_SECONDARY_4, 4, msg("secondary_4"), false);
    }

    private void displayIngredient(int guiSlot, int ingredientSlot, String label, boolean primary) {
        Map<Integer, String> ingredients = recipe.getIngredients();
        String ingredient = ingredients.get(ingredientSlot);

        ItemStack displayItem;
        if (ingredient != null) {
            MidgardItem ingItem = module.getItemManager().getItem(ingredient);
            if (ingItem != null) {
                displayItem = new ItemBuilder(ingItem.build())
                        .setName(label)
                        .addLoreLine("")
                        .addLoreLine(msg("ingredient_current") + ingredient)
                        .addLoreLine("")
                        .addLoreLine(msg("click_swap"))
                        .addLoreLine(msg("click_remove"))
                        .build();
            } else {
                try {
                    Material mat = Material.valueOf(ingredient);
                    displayItem = new ItemBuilder(mat)
                            .setName(label)
                            .addLoreLine("")
                            .addLoreLine(msg("ingredient_current") + ingredient)
                            .addLoreLine("")
                            .addLoreLine(msg("click_swap"))
                            .addLoreLine(msg("click_remove"))
                            .build();
                } catch (IllegalArgumentException e) {
                    displayItem = new ItemBuilder(Material.BARRIER)
                            .setName(label + " " + msg("invalid_suffix"))
                            .addLoreLine(msg("item_not_found") + ingredient)
                            .build();
                }
            }
        } else {
            displayItem = new ItemBuilder(primary ? Material.IRON_INGOT : Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                    .setName(label)
                    .addLoreLine(primary ? msg("required") : msg("optional"))
                    .addLoreLine("")
                    .addLoreLine(msg("click_set"))
                    .build();
        }

        inventory.setItem(guiSlot, displayItem);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == SLOT_BACK) {
            parent.open();
            return;
        }

        if (slot == SLOT_GET_ITEM) {
            ItemStack result = item.build();
            result.setAmount(recipe.getOutputAmount());
            player.getInventory().addItem(result);
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.crafting_gui.editor.common.item_added"));
            return;
        }

        if (slot == SLOT_OUTPUT_AMOUNT) {
            if (event.getClick() == ClickType.RIGHT) {
                recipe.setOutputAmount(1);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new IntegerEditor(player, val -> {
                    recipe.setOutputAmount(val);
                    item.updateRecipe(recipe);
                    open();
                }, msg("prompt_output_amount")).open();
            }
            return;
        }

        if (slot == SLOT_HIDE_BOOK) {
            recipe.setHiddenFromBook(!recipe.isHiddenFromBook());
            item.updateRecipe(recipe);
            initializeItems();
            return;
        }

        if (slot == SLOT_DIFFICULTY) {
            if (event.getClick() == ClickType.RIGHT) {
                recipe.setForgeDifficulty(1);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new IntegerEditor(player, val -> {
                    recipe.setForgeDifficulty(Math.max(1, Math.min(10, val)));
                    item.updateRecipe(recipe);
                    open();
                }, msg("prompt_difficulty")).open();
            }
            return;
        }

        if (slot == SLOT_MIN_LEVEL) {
            if (event.getClick() == ClickType.RIGHT) {
                recipe.setForgeMinLevel(1);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new IntegerEditor(player, val -> {
                    recipe.setForgeMinLevel(Math.max(0, val));
                    item.updateRecipe(recipe);
                    open();
                }, msg("prompt_min_level")).open();
            }
            return;
        }

        if (slot == SLOT_FORGE_TIER) {
            // Cycle through tiers
            String current = recipe.getForgeTier() != null ? recipe.getForgeTier() : "BASIC";
            String next = switch (current) {
                case "BASIC" -> "ADVANCED";
                case "ADVANCED" -> "EXPERT";
                case "EXPERT" -> "MASTER";
                default -> "BASIC";
            };
            recipe.setForgeTier(next);
            item.updateRecipe(recipe);
            initializeItems();
            return;
        }

        if (slot == SLOT_FORGE_RECIPE_ID) {
            player.closeInventory();
            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.enter-prompt", "%s", msg("prompt_recipe_id")));
            ChatInputListener.requestInput(player, input -> {
                recipe.setForgeRecipeId(input);
                item.updateRecipe(recipe);
                open();
            });
            return;
        }

        // Ingredient slots
        int ingredientSlot = -1;
        if (slot == SLOT_PRIMARY_METAL) {
            ingredientSlot = 0;
        } else if (slot == SLOT_SECONDARY_1) {
            ingredientSlot = 1;
        } else if (slot == SLOT_SECONDARY_2) {
            ingredientSlot = 2;
        } else if (slot == SLOT_SECONDARY_3) {
            ingredientSlot = 3;
        } else if (slot == SLOT_SECONDARY_4) {
            ingredientSlot = 4;
        }

        if (ingredientSlot >= 0) {
            final int ingSlot = ingredientSlot;
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                ItemStack cursor = event.getCursor();
                String ingredient = module.getItemManager().getItemId(cursor);
                if (ingredient == null) {
                    ingredient = cursor.getType().name();
                }
                recipe.setIngredient(ingSlot, ingredient);
                item.updateRecipe(recipe);
                initializeItems();
            } else if (event.getClick() == ClickType.RIGHT) {
                recipe.setIngredient(ingSlot, null);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new IngredientSelectionGui(player, module, this, selectedIngredient -> {
                    recipe.setIngredient(ingSlot, selectedIngredient);
                    item.updateRecipe(recipe);
                    open();
                }).open();
            }
        }
    }
}
