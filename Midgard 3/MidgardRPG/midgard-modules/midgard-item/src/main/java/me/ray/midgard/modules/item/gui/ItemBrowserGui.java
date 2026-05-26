package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.model.MidgardItemImpl;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.persistence.PersistentDataType;
import me.ray.midgard.core.utils.GuiConfigUtils;

import java.util.List;

public class ItemBrowserGui extends PaginatedGui<MidgardItem> {

    private final ItemModule module;
    private final String categoryId;
    private final NamespacedKey itemIdKey;
    private final java.util.function.Consumer<MidgardItem> onSelect;
    private final me.ray.midgard.core.gui.BaseGui parentGui;
    private boolean isDeleteMode = false;

    public ItemBrowserGui(Player player, ItemModule module, String categoryId, List<MidgardItem> items) {
        this(player, module, categoryId, items, null, null);
    }

    public ItemBrowserGui(Player player, ItemModule module, String categoryId, List<MidgardItem> items, java.util.function.Consumer<MidgardItem> onSelect, me.ray.midgard.core.gui.BaseGui parentGui) {
        super(player, module.getConfig().getString("guis.item_browser.title", "Navegador de Itens") + ": " + categoryId, items);
        this.module = module;
        this.categoryId = categoryId;
        this.itemIdKey = new NamespacedKey(module.getPlugin(), "item_id");
        this.onSelect = onSelect;
        this.parentGui = parentGui;
    }

    public String getCategoryId() {
        return categoryId;
    }

    @Override
    public ItemStack createItem(MidgardItem item) {
        ItemStack stack = item.build();
        ItemBuilder builder = new ItemBuilder(stack)
                .addLoreLine("");
        
        if (isDeleteMode) {
            builder.addLoreLine(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.click-to-delete-item"));
        } else {
            builder.addLoreLine(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.click-to-get"))
                   .addLoreLine(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.shift-click-to-edit"));
        }
        
        return builder.pdc(itemIdKey, PersistentDataType.STRING, item.getId())
                .build();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void initializeItems() {
        super.initializeItems();

        // Filler removed per user request


        if (onSelect == null) {
            FileConfiguration config = module.getConfig();
            String path = "guis.item_browser.items.";
            
            // Slot 47: Delete Item
            int deleteSlot = GuiConfigUtils.getSlot(config, path + "delete_mode");
            if (deleteSlot == -1) {
                deleteSlot = 47;
            }
            
            ItemStack deleteStack = GuiConfigUtils.getItem(config, path + "delete_mode", me.ray.midgard.core.MidgardCore.getLanguageManager());
            if (deleteStack == null) {
                ItemBuilder deleteBtn = new ItemBuilder(isDeleteMode ? org.bukkit.Material.TNT : org.bukkit.Material.RED_DYE)
                    .name(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.buttons.delete.name"));
                deleteStack = deleteBtn.build();
            } else {
                if (isDeleteMode) {
                    deleteStack.setType(org.bukkit.Material.TNT);
                }
            }
            
            ItemBuilder deleteBuilder = new ItemBuilder(deleteStack);
            if (isDeleteMode) {
                deleteBuilder.addLoreLine(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.buttons.delete.mode-active"));
                deleteBuilder.addLoreLine(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.buttons.delete.click-to-delete"));
                deleteBuilder.addLoreLine(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.buttons.delete.click-to-cancel"));
                deleteBuilder.glow();
            } else {
                deleteBuilder.addLoreLine(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.buttons.delete.click-to-enable"));
            }
            inventory.setItem(deleteSlot, deleteBuilder.build());

            // Slot 51: Create New Item
            int createSlot = GuiConfigUtils.getSlot(config, path + "create_item");
            if (createSlot == -1) {
                createSlot = 51;
            }
            
            ItemStack createStack = GuiConfigUtils.getItem(config, path + "create_item", me.ray.midgard.core.MidgardCore.getLanguageManager());
            if (createStack == null) {
                createStack = new ItemBuilder(org.bukkit.Material.EMERALD)
                    .name(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.buttons.create.name"))
                    .build();
            }
            inventory.setItem(createSlot, createStack);
        }

        // Slot 49: Back to Categories
        inventory.setItem(49, new ItemBuilder(org.bukkit.Material.OAK_DOOR)
                .name(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.buttons.back.name"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        
        FileConfiguration config = module.getConfig();
        String path = "guis.item_browser.items.";
        
        int closeSlot = GuiConfigUtils.getSlot(config, path + "close");
        if (closeSlot == -1) {
            closeSlot = 49;
        }
        
        int deleteSlot = GuiConfigUtils.getSlot(config, path + "delete_mode");
        if (deleteSlot == -1) {
            deleteSlot = 47;
        }
        
        int createSlot = GuiConfigUtils.getSlot(config, path + "create_item");
        if (createSlot == -1) {
            createSlot = 51;
        }

        int prevSlot = GuiConfigUtils.getSlot(config, path + "previous_page");
        if (prevSlot == -1) {
            prevSlot = 18;
        }

        int nextSlot = GuiConfigUtils.getSlot(config, path + "next_page");
        if (nextSlot == -1) {
            nextSlot = 26;
        }

        // Handle navigation buttons
        if (slot == prevSlot && page > 0) {
            event.setCancelled(true);
            page--;
            initializeItems();
            return;
        } else if (slot == nextSlot && index + 1 < items.size()) {
            event.setCancelled(true);
            page++;
            initializeItems();
            return;
        }

        // Handle custom buttons before super to avoid conflicts (e.g. super closes on 49)
        if (slot == closeSlot) {
            event.setCancelled(true);
            if (parentGui != null) {
                parentGui.open();
            } else {
                new TypeBrowserGui(player, module).open();
            }
            return;
        } else if (onSelect == null && slot == deleteSlot) {
            event.setCancelled(true);
            if (!player.hasPermission("midgard.admin.item.delete")) {
                player.sendMessage(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.no-permission"));
                return;
            }
            isDeleteMode = !isDeleteMode;
            initializeItems(); // Refresh UI
            return;
        } else if (onSelect == null && slot == createSlot) {
            event.setCancelled(true);
            if (!player.hasPermission("midgard.admin.item.create")) {
                player.sendMessage(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.no-permission"));
                return;
            }
            player.closeInventory();
            player.sendMessage(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.messages.enter-id"));
            player.sendMessage(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.gui.browser.messages.cancel-abort"));

            ChatInputListener.requestInput(player, (input) -> {
                if (module.getItemManager().getItemStack(input) != null) {
                    String msg = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.browser.messages.id-exists").replace("%s", input);
                    player.sendMessage(me.ray.midgard.core.text.MessageUtils.parse(msg));
                    new ItemBrowserGui(player, module, categoryId, module.getItemManager().getItemsByCategory(categoryId)).open();
                } else {
                    String msg = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.browser.messages.creating-item").replace("%s", input);
                    player.sendMessage(me.ray.midgard.core.text.MessageUtils.parse(msg));
                    
                    // Create item in memory (no file — persisted to DB via save())
                    org.bukkit.configuration.file.YamlConfiguration root = new org.bukkit.configuration.file.YamlConfiguration();
                    org.bukkit.configuration.ConfigurationSection section = root.createSection(input);
                    
                    // Inherit from Category
                    me.ray.midgard.modules.item.model.ItemCategory category = module.getCategoryManager().getCategory(categoryId);
                    if (category != null) {
                        section.set("base.material", category.getIcon().name());
                        if (category.getModelData() != 0) {
                            section.set("base.display.custom-model-data", category.getModelData());
                        }
                    } else {
                        section.set("base.material", "STONE");
                    }
                    
                    section.set("base.name", input);
                    section.set("type", categoryId);

                    MidgardItem newItem = new MidgardItemImpl(input, section, categoryId);
                    module.getItemManager().registerItem(newItem);
                    newItem.save(); // Persiste no banco de dados
                    
                    // Update Category Display Item
                    module.getCategoryManager().updateCategoryDisplayItem(categoryId, newItem.getId());
                    
                    // Open Edition GUI
                    new ItemEditionGui(player, module, newItem).open();
                }
            });
            return;
        }

        // super.onClick(event);
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) {
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked.getItemMeta() == null) {
            return;
        }
        
        if (clicked.getItemMeta().getPersistentDataContainer().has(itemIdKey, PersistentDataType.STRING)) {
            String itemId = clicked.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
            
            if (isDeleteMode) {
                MidgardItem mItem = module.getItemManager().getMidgardItem(itemId);
                if (mItem != null) {
                    new DeleteConfirmationGui(player, module, mItem, this).open();
                }
                return;
            }

            if (onSelect != null) {
                MidgardItem mItem = module.getItemManager().getMidgardItem(itemId);
                if (mItem != null) {
                    onSelect.accept(mItem);
                }
                return;
            }
            
            if (event.isShiftClick()) {
                if (!player.hasPermission("midgard.admin.item.edit")) {
                    player.sendMessage(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.no-permission"));
                    return;
                }
                MidgardItem mItem = module.getItemManager().getMidgardItem(itemId);
                if (mItem != null) {
                    new ItemEditionGui(player, module, mItem).open();
                } else {
                    String msg = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.browser.messages.edit-error").replace("%s", itemId);
                    player.sendMessage(me.ray.midgard.core.text.MessageUtils.parse(msg));
                }
            } else {
                if (!player.hasPermission("midgard.admin.item.give")) {
                    player.sendMessage(me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("item.command.no-permission"));
                    return;
                }
                ItemStack item = module.getItemManager().getItemStack(itemId);
                if (item != null) {
                    player.getInventory().addItem(item);
                    String msg = me.ray.midgard.core.MidgardCore.getLanguageManager().getRawMessage("item.gui.browser.messages.received")
                            .replace("%s", itemId)
                            .replace("%amount%", String.valueOf(item.getAmount()));
                    player.sendMessage(me.ray.midgard.core.text.MessageUtils.parse(msg));
                }
            }
        }
    }

    @Override
    public void addMenuBorder() {
        FileConfiguration config = module.getConfig();
        String path = "guis.item_browser.items.";
        
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
