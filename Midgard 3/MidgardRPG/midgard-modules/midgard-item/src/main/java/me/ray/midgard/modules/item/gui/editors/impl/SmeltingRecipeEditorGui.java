package me.ray.midgard.modules.item.gui.editors.impl;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.model.MidgardRecipe;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Editor GUI for SMELTING recipe type.
 * Allows admins to configure smeltery-based crafting:
 * - Metal ingredients (MoltenMetal name → mb amount)
 * - Minimum smeltery temperature
 * - Output amount
 * - Hidden from alloy book
 */
public class SmeltingRecipeEditorGui extends BaseGui {

    private final ItemModule module;
    private final MidgardItem item;
    private final BaseGui parent;
    private final MidgardRecipe recipe;

    // Todos os metais disponíveis no sistema de smeltery
    private static final String[] ALL_METALS = {
            "IRON", "GOLD", "COPPER", "NETHERITE_SCRAP", "EMERALD",
            "DIAMOND", "AMETHYST", "QUARTZ", "LAPIS", "REDSTONE",
            "BRONZE", "STEEL", "MANYULLYN", "OBSIDIAN_ALLOY",
            "ROSE_GOLD", "ELECTRUM", "KNIGHTSLIME"
    };

    // Map de metal → Material para ícone visual
    private static final Map<String, Material> METAL_ICONS = Map.ofEntries(
            Map.entry("IRON", Material.IRON_INGOT),
            Map.entry("GOLD", Material.GOLD_INGOT),
            Map.entry("COPPER", Material.COPPER_INGOT),
            Map.entry("NETHERITE_SCRAP", Material.NETHERITE_SCRAP),
            Map.entry("EMERALD", Material.EMERALD),
            Map.entry("DIAMOND", Material.DIAMOND),
            Map.entry("AMETHYST", Material.AMETHYST_SHARD),
            Map.entry("QUARTZ", Material.QUARTZ),
            Map.entry("LAPIS", Material.LAPIS_LAZULI),
            Map.entry("REDSTONE", Material.REDSTONE),
            Map.entry("BRONZE", Material.COPPER_INGOT),
            Map.entry("STEEL", Material.IRON_INGOT),
            Map.entry("MANYULLYN", Material.NETHERITE_INGOT),
            Map.entry("OBSIDIAN_ALLOY", Material.OBSIDIAN),
            Map.entry("ROSE_GOLD", Material.GOLD_INGOT),
            Map.entry("ELECTRUM", Material.GOLD_INGOT),
            Map.entry("KNIGHTSLIME", Material.SLIME_BALL)
    );

    // Helper methods for i18n
    private String msg(String key) {
        return MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.smelting." + key);
    }

    private static String smsg(String key) {
        return MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.smelting." + key);
    }

    private static String getMetalName(String key) {
        String msg = MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.editor.smelting.metal." + key);
        return msg != null ? msg : key;
    }

    // Layout
    private static final int SLOT_GET_ITEM = 2;
    private static final int SLOT_DISPLAY = 4;
    private static final int SLOT_BACK = 6;
    private static final int SLOT_OUTPUT_AMOUNT = 19;
    private static final int SLOT_HIDE_BOOK = 20;
    private static final int SLOT_MIN_TEMP = 21;
    private static final int SLOT_ADD_METAL = 22;

    // Slots para metais ingredientes (row 4-5, cols 1-7)
    private static final int[] METAL_SLOTS = {37, 38, 39, 40, 41, 42, 43};

    // Mapeamento slot → metal key para cliques
    private final Map<Integer, String> slotToMetal = new HashMap<>();

    public SmeltingRecipeEditorGui(Player player, ItemModule module, MidgardItem item, BaseGui parent, String recipeId) {
        super(player, 6, smsg("title"));
        this.module = module;
        this.item = item;
        this.parent = parent;
        this.recipe = item.getRecipe(recipeId);
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        slotToMetal.clear();

        if (recipe == null) {
            player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.crafting_gui.error.recipe_not_found")));
            parent.open();
            return;
        }

        // Background
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(MessageUtils.parse(" ")).build();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // Get Item
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

        // Min Temperature
        int minTemp = recipe.getSmeltingMinTemperature();
        inventory.setItem(SLOT_MIN_TEMP, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(msg("min_temp_name"))
                .addLoreLine(msg("min_temp_lore1"))
                .addLoreLine(msg("min_temp_lore2"))
                .addLoreLine("")
                .addLoreLine(msg("min_temp_current") + minTemp + "°C")
                .addLoreLine("")
                .addLoreLine(msg("click_edit"))
                .addLoreLine(msg("click_reset_temp"))
                .build());

        // Add Metal Button
        inventory.setItem(SLOT_ADD_METAL, new ItemBuilder(Material.EMERALD)
                .setName(msg("add_metal_name"))
                .addLoreLine(msg("add_metal_lore1"))
                .addLoreLine(msg("add_metal_lore2"))
                .addLoreLine("")
                .addLoreLine(msg("add_metal_click"))
                .build());

        // Separator
        ItemStack separator = new ItemBuilder(Material.IRON_BARS)
                .setName(msg("separator"))
                .build();
        for (int i = 28; i <= 34; i++) {
            inventory.setItem(i, separator);
        }

        // Display current metals
        displayMetals();
    }

    private void displayMetals() {
        Map<String, Integer> metals = recipe.getSmeltingMetals();
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(metals.entrySet());

        for (int i = 0; i < METAL_SLOTS.length; i++) {
            int slot = METAL_SLOTS[i];
            if (i < entries.size()) {
                var entry = entries.get(i);
                String metalKey = entry.getKey();
                int amount = entry.getValue();

                Material icon = METAL_ICONS.getOrDefault(metalKey, Material.IRON_NUGGET);
                String name = getMetalName(metalKey);

                inventory.setItem(slot, new ItemBuilder(icon)
                        .setName(name)
                        .addLoreLine("")
                        .addLoreLine(msg("metal_amount") + amount + "mb")
                        .addLoreLine("")
                        .addLoreLine(msg("click_edit_amount"))
                        .addLoreLine(msg("click_remove"))
                        .build());
                slotToMetal.put(slot, metalKey);
            } else {
                inventory.setItem(slot, new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .setName(msg("empty_slot"))
                        .addLoreLine(msg("empty_slot_hint"))
                        .build());
            }
        }
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
                    recipe.setOutputAmount(Math.max(1, val));
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

        if (slot == SLOT_MIN_TEMP) {
            if (event.getClick() == ClickType.RIGHT) {
                recipe.setSmeltingMinTemperature(800);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                new IntegerEditor(player, val -> {
                    recipe.setSmeltingMinTemperature(Math.max(0, val));
                    item.updateRecipe(recipe);
                    open();
                }, msg("prompt_min_temp")).open();
            }
            return;
        }

        if (slot == SLOT_ADD_METAL) {
            openMetalSelectionMenu();
            return;
        }

        // Metal ingredient slots
        String metalKey = slotToMetal.get(slot);
        if (metalKey != null) {
            if (event.getClick() == ClickType.RIGHT) {
                // Remove
                recipe.setSmeltingMetal(metalKey, 0);
                item.updateRecipe(recipe);
                initializeItems();
            } else {
                // Edit amount
                player.closeInventory();
                player.sendMessage(MessageUtils.parse(msg("prompt_metal_amount") + metalKey));
                ChatInputListener.requestInput(player, input -> {
                    try {
                        int val = Integer.parseInt(input);
                        recipe.setSmeltingMetal(metalKey, Math.max(1, val));
                        item.updateRecipe(recipe);
                    } catch (NumberFormatException e) {
                        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-number"));
                    }
                    open();
                });
            }
        }
    }

    private void openMetalSelectionMenu() {
        new MetalSelectionGui(player, this).open();
    }

    /**
     * Chamado pelo MetalSelectionGui quando um metal é selecionado.
     */
    void onMetalSelected(String metalKey) {
        player.closeInventory();
        player.sendMessage(MessageUtils.parse(msg("prompt_metal_amount") + getMetalName(metalKey)));
        ChatInputListener.requestInput(player, input -> {
            try {
                int val = Integer.parseInt(input);
                recipe.setSmeltingMetal(metalKey, Math.max(1, val));
                item.updateRecipe(recipe);
            } catch (NumberFormatException e) {
                player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-number"));
            }
            open();
        });
    }

    // ── Inner class: Metal Selection GUI ──

    static class MetalSelectionGui extends BaseGui {

        private final SmeltingRecipeEditorGui parentEditor;
        private final Map<Integer, String> slotMetal = new HashMap<>();

        MetalSelectionGui(Player player, SmeltingRecipeEditorGui parentEditor) {
            super(player, 4, smsg("metal_selection_title"));
            this.parentEditor = parentEditor;
        }

        @Override
        public void initializeItems() {
            ItemStack filler = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(MessageUtils.parse(" ")).build();
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }

            // Display all metals in center
            int[] slots = {10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30};

            for (int i = 0; i < ALL_METALS.length && i < slots.length; i++) {
                String metalKey = ALL_METALS[i];
                Material icon = METAL_ICONS.getOrDefault(metalKey, Material.IRON_NUGGET);
                String name = getMetalName(metalKey);

                inventory.setItem(slots[i], new ItemBuilder(icon)
                        .setName(name)
                        .addLoreLine("")
                        .addLoreLine(smsg("click_select"))
                        .build());
                slotMetal.put(slots[i], metalKey);
            }

            // Back
            inventory.setItem(31, new ItemBuilder(Material.ARROW)
                    .setName(smsg("back_arrow"))
                    .build());
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            if (slot == 31) {
                parentEditor.open();
                return;
            }

            String metal = slotMetal.get(slot);
            if (metal != null) {
                parentEditor.onMetalSelected(metal);
            }
        }
    }
}
