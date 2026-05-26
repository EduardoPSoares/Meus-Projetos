package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.model.ItemCategory;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.file.FileConfiguration;
import me.ray.midgard.core.utils.GuiConfigUtils;

import java.util.ArrayList;
import java.util.List;

public class TypeBrowserGui extends PaginatedGui<ItemCategory> {

    private final ItemModule module;
    private final NamespacedKey categoryIdKey;
    private final java.util.function.Consumer<MidgardItem> onSelect;
    @SuppressWarnings("unused")
    private final me.ray.midgard.core.gui.BaseGui parentGui;

    public TypeBrowserGui(Player player, ItemModule module) {
        this(player, module, null, null);
    }

    public TypeBrowserGui(Player player, ItemModule module, java.util.function.Consumer<MidgardItem> onSelect, me.ray.midgard.core.gui.BaseGui parentGui) {
        super(player, module.getConfig().getString("guis.type_browser.title", "Categorias"), getSortedCategories(module));
        this.module = module;
        this.categoryIdKey = new NamespacedKey(module.getPlugin(), "category_id");
        this.onSelect = onSelect;
        this.parentGui = parentGui;
    }

    private static List<ItemCategory> getSortedCategories(ItemModule module) {
        List<ItemCategory> categories = new ArrayList<>(module.getCategoryManager().getCategories());
        categories.sort((c1, c2) -> {
            int p1 = c1.getPage();
            int p2 = c2.getPage();
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
            
            int s1 = c1.getSlot();
            int s2 = c2.getSlot();
            if (s1 != -1 && s2 != -1) {
                return Integer.compare(s1, s2);
            } else if (s1 != -1) {
                return -1; // s1 comes first
            } else if (s2 != -1) {
                return 1; // s2 comes first
            } else {
                return c1.getName().compareTo(c2.getName()); // Alphabetical for others
            }
        });
        return categories;
    }

    @Override
    public ItemStack createItem(ItemCategory category) {
        int itemCount = module.getItemManager().getItemsByCategory(category.getId()).size();
        String name = category.getName();

        // Resolve display item if exists
        ItemStack iconStack = new ItemStack(category.getIcon());
        int modelData = category.getModelData();
        
        if (category.getDisplayItemId() != null) {
             me.ray.midgard.modules.item.model.MidgardItem displayItem = module.getItemManager().getMidgardItem(category.getDisplayItemId());
             if (displayItem != null) {
                 iconStack = displayItem.build();
                 // We might want to keep the category name override?
                 // Usually categories have their own names in the config.
             }
        }

        // Ensure green color logic using MiniMessage
        String finalName = "<green>" + name;

        String rawLore = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.browser.category-lore");
        if (rawLore == null) {
            rawLore = "<gray>There are <yellow>%count% <gray>items in this type.";
        }
        
        rawLore = rawLore.replace("%count%", String.valueOf(itemCount));
        
        // DEBUG: Print count
        // me.ray.midgard.core.debug.MidgardLogger.info("Category " + name + " has " + itemCount + " items. Setting amount to " + Math.max(1, Math.min(64, itemCount)));

        return new ItemBuilder(iconStack)
                .name(me.ray.midgard.core.text.MessageUtils.parse(finalName))
                .lore(
                        me.ray.midgard.core.text.MessageUtils.parse(rawLore)
                )
                .amount(Math.max(1, Math.min(64, itemCount)))
                .maxStackSize(99) // Force max stack size to allow showing count > 1 on unstackable items
                .flags(ItemFlag.values())
                .customModelData(modelData) // Re-apply model data if category overrides it, or if it's default
                .pdc(categoryIdKey, PersistentDataType.STRING, category.getId())
                .build();
    }
    
    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        FileConfiguration config = module.getConfig();
        String path = "guis.type_browser.items.";
        
        int closeSlot = GuiConfigUtils.getSlot(config, path + "close");
        if (closeSlot == -1) {
            closeSlot = 49;
        }
        
        int prevSlot = GuiConfigUtils.getSlot(config, path + "previous_page");
        if (prevSlot == -1) {
            prevSlot = 18;
        }
        
        int nextSlot = GuiConfigUtils.getSlot(config, path + "next_page");
        if (nextSlot == -1) {
            nextSlot = 26;
        }

        if (slot == closeSlot) {
            event.getWhoClicked().closeInventory();
            return;
        } else if (slot == prevSlot && page > 0) {
            page--;
            initializeItems();
            return;
        } else if (slot == nextSlot && index + 1 < items.size()) {
            page++;
            initializeItems();
            return;
        }
        
        if (event.getCurrentItem() == null) {
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked.getItemMeta() == null) {
            return;
        }
        
        if (clicked.getItemMeta().getPersistentDataContainer().has(categoryIdKey, PersistentDataType.STRING)) {
            String categoryId = clicked.getItemMeta().getPersistentDataContainer().get(categoryIdKey, PersistentDataType.STRING);
            
            List<MidgardItem> items = module.getItemManager().getItemsByCategory(categoryId);
            new ItemBrowserGui(player, module, categoryId, items, onSelect, this).open();
        }
    }

    @Override
    public void addMenuBorder() {
        FileConfiguration config = module.getConfig();
        String path = "guis.type_browser.items.";
        
        // Navigation
        if (page > 0) {
            int prevSlot = GuiConfigUtils.getSlot(config, path + "previous_page");
            if (prevSlot == -1) {
                prevSlot = 18;
            }
            
            ItemStack prevStack = GuiConfigUtils.getItem(config, path + "previous_page", me.ray.midgard.core.MidgardCore.getLanguageManager());
            if (prevStack == null) {
                prevStack = new ItemBuilder(org.bukkit.Material.ARROW).name(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.previous-page")).build();
            }
            inventory.setItem(prevSlot, prevStack);
        }

        // Close
        int closeSlot = GuiConfigUtils.getSlot(config, path + "close");
        if (closeSlot == -1) {
            closeSlot = 49;
        }
        ItemStack closeStack = GuiConfigUtils.getItem(config, path + "close", me.ray.midgard.core.MidgardCore.getLanguageManager());
        if (closeStack == null) {
            closeStack = new ItemBuilder(org.bukkit.Material.BARRIER).name(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.close")).build();
        }
        inventory.setItem(closeSlot, closeStack);

        // Next Page
        if (index + 1 < items.size()) {
            int nextSlot = GuiConfigUtils.getSlot(config, path + "next_page");
            if (nextSlot == -1) {
                nextSlot = 26;
            }
            
            ItemStack nextStack = GuiConfigUtils.getItem(config, path + "next_page", me.ray.midgard.core.MidgardCore.getLanguageManager());
            if (nextStack == null) {
                nextStack = new ItemBuilder(org.bukkit.Material.ARROW).name(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.next-page")).build();
            }
            inventory.setItem(nextSlot, nextStack);
        }
    }
}
