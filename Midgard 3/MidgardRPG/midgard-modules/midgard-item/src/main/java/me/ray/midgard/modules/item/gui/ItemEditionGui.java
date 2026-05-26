package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.utils.GuiConfigUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.gui.editors.impl.*;
import me.ray.midgard.modules.classes.ClassesModule;
import me.ray.midgard.modules.classes.RPGClass;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.ray.midgard.core.integration.NexoUtils;

@SuppressWarnings("deprecation")
public class ItemEditionGui extends PaginatedGui<ItemStat> {

    private final ItemModule module;
    private final MidgardItem item;
    private List<ItemStat> allStats;
    private final LanguageManager lang;

    private final int[] modifierSlots = {19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private int currentPageCounter;
    private int currentSlotIndex;
    private int maxPages;

    public ItemEditionGui(Player player, ItemModule module, MidgardItem item) {
        super(player, MessageUtils.serialize(MessageUtils.parse((module.getItemEditionLoader().getTitle() != null ? module.getItemEditionLoader().getTitle() : "Item Edition: %id%").replace("%id%", item.getId()))), new ArrayList<>());
        this.lang = MidgardCore.getLanguageManager();
        this.module = module;
        this.item = item;
        this.allStats = loadStats();
        this.items = allStats;
        
        // Calculate max pages based on custom slots
        this.maxPages = (int) Math.ceil((double) allStats.size() / modifierSlots.length);
        if (this.maxPages == 0) {
            this.maxPages = 1;
        }

        // Register session so sub-menus can find this editor
        EditorSession.registerEditor(player, module, item, this);
    }

    public ItemEditionGui(Player player, ItemModule module, MidgardItem item, int page) {
        this(player, module, item);
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public ItemStack createItem(ItemStat stat) {
        return stat.getIcon(player, item);
    }

    @Override
    public void initializeItems() {
        // Reload stats to reflect any changes made in submenus
        this.allStats = loadStats();
        this.items = allStats;
        this.maxPages = (int) Math.ceil((double) allStats.size() / modifierSlots.length);
        if (this.maxPages == 0) {
            this.maxPages = 1;
        }

        inventory.clear();
        
        int targetPage = page + 1;
        
        List<ItemStat> pageStats = allStats.stream()
                .filter(s -> s.getPage() == targetPage)
                .collect(Collectors.toList());

        for (ItemStat stat : pageStats) {
            inventory.setItem(stat.getSlot(), createItem(stat));
        }

        addMenuBorder();
    }

    @Override
    public void addMenuBorder() {
        FileConfiguration config = module.getConfig();
        String path = "guis.item_edition.items.";

        // Slot 2: Get Item
        int getItemSlot = GuiConfigUtils.getSlot(config, path + "get_item");
        if (getItemSlot == -1) {
            getItemSlot = 2;
        }
        
        ItemStack getItemStack = GuiConfigUtils.getItem(config, path + "get_item", lang);
        if (getItemStack == null) {
            ItemEditionMessagesLoader.StatConfig getItemConf = module.getItemEditionLoader().getButtonConfig("get_item");
            if (getItemConf != null) {
                getItemStack = new ItemBuilder(Material.EMERALD)
                    .name(MessageUtils.parse(getItemConf.getName()))
                    .lore(getItemConf.getLore().stream()
                            .map(MessageUtils::parse)
                            .collect(Collectors.toList()))
                    .build();
            } else {
                getItemStack = new ItemBuilder(Material.EMERALD)
                    .name(MessageUtils.parse("<green>Get Item"))
                    .build();
            }
        }
        inventory.setItem(getItemSlot, getItemStack);
        
        // Slot 4: Display Item
        int displaySlot = GuiConfigUtils.getSlot(config, path + "display_item");
        if (displaySlot == -1) {
            displaySlot = 4;
        }
        inventory.setItem(displaySlot, item.build());
        
        // Slot 6: Close
        int closeSlot = GuiConfigUtils.getSlot(config, path + "close");
        if (closeSlot == -1) {
            closeSlot = 6;
        }
        
        ItemStack closeStack = GuiConfigUtils.getItem(config, path + "close", lang);
        if (closeStack == null) {
            ItemEditionMessagesLoader.StatConfig closeConf = module.getItemEditionLoader().getButtonConfig("close");
            if (closeConf != null) {
                closeStack = new ItemBuilder(Material.BARRIER)
                    .name(MessageUtils.parse(closeConf.getName()))
                    .lore(closeConf.getLore().stream()
                            .map(MessageUtils::parse)
                            .collect(Collectors.toList()))
                    .build();
            } else {
                closeStack = new ItemBuilder(Material.BARRIER)
                    .name(MessageUtils.parse("<red>Close"))
                    .build();
            }
        }
        inventory.setItem(closeSlot, closeStack);

        // Navigation
        if (page > 0) {
            int prevSlot = GuiConfigUtils.getSlot(config, path + "previous_page");
            if (prevSlot == -1) {
                prevSlot = 27;
            }
            
            ItemStack prevStack = GuiConfigUtils.getItem(config, path + "previous_page", lang);
            if (prevStack == null) {
                prevStack = new ItemBuilder(Material.ARROW).name(lang.getMessage("item.gui.browser.previous-page")).build();
            }
            inventory.setItem(prevSlot, prevStack);
        }
        
        // Check if there are more pages
        int maxPages = (int) Math.ceil((double) allStats.size() / modifierSlots.length);
        if (page < maxPages - 1) {
            int nextSlot = GuiConfigUtils.getSlot(config, path + "next_page");
            if (nextSlot == -1) {
                nextSlot = 35;
            }
            
            ItemStack nextStack = GuiConfigUtils.getItem(config, path + "next_page", lang);
            if (nextStack == null) {
                nextStack = new ItemBuilder(Material.ARROW).name(lang.getMessage("item.gui.browser.next-page")).build();
            }
            inventory.setItem(nextSlot, nextStack);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        FileConfiguration config = module.getConfig();
        String path = "guis.item_edition.items.";
        
        int prevSlot = GuiConfigUtils.getSlot(config, path + "previous_page");
        if (prevSlot == -1) {
            prevSlot = 27;
        }
        
        int nextSlot = GuiConfigUtils.getSlot(config, path + "next_page");
        if (nextSlot == -1) {
            nextSlot = 35;
        }
        
        int closeSlot = GuiConfigUtils.getSlot(config, path + "close");
        if (closeSlot == -1) {
            closeSlot = 6;
        }
        
        int getItemSlot = GuiConfigUtils.getSlot(config, path + "get_item");
        if (getItemSlot == -1) {
            getItemSlot = 2;
        }

        if (slot >= inventory.getSize()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                // Nexo Integration
                if (NexoUtils.isCustomItem(clicked)) {
                    String nexoId = NexoUtils.getCustomItemId(clicked);
                    ItemStack nexoItem = NexoUtils.getCustomItem(nexoId);
                    if (nexoItem != null) {
                        item.setMaterial(nexoItem.getType());
                        if (nexoItem.hasItemMeta() && nexoItem.getItemMeta().hasCustomModelData()) {
                            item.setCustomModelData(nexoItem.getItemMeta().getCustomModelData());
                        } else {
                            item.setCustomModelData(0);
                        }
                        item.save();
                        initializeItems();
                        player.sendMessage(MessageUtils.parse(getSafeMessage("item.gui.item_edition.messages.material-updated", "<green>✔</green> <gray>ᴍᴀᴛᴇʀɪᴀʟ ᴀᴛᴜᴀʟɪᴢᴀᴅᴏ ᴠɪᴀ ɪɴᴠᴇɴᴛᴀʀɪᴏ!</gray>")));
                        return;
                    }
                }

                item.setMaterial(clicked.getType());
                if (clicked.hasItemMeta() && clicked.getItemMeta().hasCustomModelData()) {
                    item.setCustomModelData(clicked.getItemMeta().getCustomModelData());
                } else {
                    item.setCustomModelData(0);
                }
                item.save();
                initializeItems();
                player.sendMessage(MessageUtils.parse(lang.getRawMessage("item.gui.item_edition.messages.material-updated")));
            }
            return;
        }

        if (slot == prevSlot && page > 0) {
            page--;
            initializeItems();
        } else if (slot == closeSlot) {
            EditorSession.end(player);
            event.getWhoClicked().closeInventory();
        } else if (slot == nextSlot) {
            int maxPages = (int) Math.ceil((double) allStats.size() / modifierSlots.length);
            if (page < maxPages - 1) {
                page++;
                initializeItems();
            }
        } else if (slot == getItemSlot) {
            // Get Item
            player.getInventory().addItem(item.build());
            String msg = getSafeMessage("item.command.received", "Received item %s")
                    .replace("%s", item.getId())
                    .replace("%amount%", "1");
            player.sendMessage(MessageUtils.parse(msg));
        } else {
            // Handle stat click
            int targetPage = page + 1;
            ItemStat clickedStat = allStats.stream()
                    .filter(s -> s.getPage() == targetPage && s.getSlot() == slot)
                    .findFirst()
                    .orElse(null);
            
            if (clickedStat != null) {
                if (clickedStat.getEditor() != null) {
                    clickedStat.getEditor().edit(player, module, item, this, event.getClick());
                } else {
                    String itemName = MessageUtils.serialize(clickedStat.getIcon(player, item).getItemMeta().displayName());
                    String msg = module.getItemEditionLoader().getMessage("messages.editing").replace("%s", itemName);
                    player.sendMessage(MessageUtils.parse(msg));
                    player.sendMessage(MessageUtils.parse(module.getItemEditionLoader().getMessage("messages.no-editor")));
                }
            }
        }
    }

    private String getSafeMessage(String key, String def) {
        // Compatibility wrapper for refactoring
        if (key.startsWith("item.gui.item_edition.")) {
            String shortKey = key.replace("item.gui.item_edition.", "");
            return module.getItemEditionLoader().getMessage(shortKey);
        }
        try {
            String msg = lang.getRawMessage(key);
            return msg != null ? msg : def;
        } catch (Exception e) {
            return def;
        }
    }

    private ItemStat addStat(List<ItemStat> stats, Material mat, String key, Object valueObj) {
        if (currentSlotIndex >= modifierSlots.length) {
            currentPageCounter++;
            currentSlotIndex = 0;
        }

        ItemEditionMessagesLoader.StatConfig conf = module.getItemEditionLoader().getStatConfig(key);
        String name;
        List<String> lore;
        
        if (conf != null) {
            name = MessageUtils.serialize(MessageUtils.parse(conf.getName()));
            lore = conf.getLore();
        } else {
            name = MessageUtils.serialize(MessageUtils.parse("<yellow>" + key));
            lore = new ArrayList<>();
        }
        
        String valueStr;
        String sizeStr = "0";

        if (valueObj == null) {
            valueStr = lang.getRawMessage("item.gui.item_edition.values.none");
        } else if (valueObj instanceof Boolean) {
            valueStr = ((Boolean) valueObj) ? lang.getRawMessage("item.gui.item_edition.values.enabled") : lang.getRawMessage("item.gui.item_edition.values.disabled");
        } else if (valueObj instanceof java.util.Collection) {
            java.util.Collection<?> col = (java.util.Collection<?>) valueObj;
            sizeStr = String.valueOf(col.size());
            valueStr = lang.getRawMessage("item.gui.item_edition.values.list");
        } else if (valueObj instanceof java.util.Map) {
            java.util.Map<?,?> map = (java.util.Map<?,?>) valueObj;
            sizeStr = String.valueOf(map.size());
            valueStr = lang.getRawMessage("item.gui.item_edition.values.map");
        } else if (valueObj instanceof String) {
            valueStr = (String) valueObj;
            if (valueStr.isEmpty()) {
                valueStr = lang.getRawMessage("item.gui.item_edition.values.none");
            } else if ((key.equals("enchantments") || key.equals("custom_model_data_strings") || key.equals("custom_model_data_floats")) && !valueStr.equals("None") && !valueStr.isEmpty()) {
                sizeStr = String.valueOf(valueStr.split(",").length);
            } else if (key.equals("nbt_tags")) {
                sizeStr = "1";
                valueStr = lang.getRawMessage("item.gui.item_edition.values.present");
            } else if (key.equals("item_set") || key.equals("tier")) {
                // Keep the ID as valueStr
            }
        } else {
            valueStr = String.valueOf(valueObj);
        }

        List<String> finalLore = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                String l = line.replace("%value%", valueStr)
                               .replace("%size%", sizeStr)
                               .replace("%particle%", valueStr);
                finalLore.add(MessageUtils.serialize(MessageUtils.parse(l)));
            }
        }
        
        ItemStat stat = new ItemStat(key, currentPageCounter, modifierSlots[currentSlotIndex++], mat, name, finalLore.toArray(new String[0]));
        stats.add(stat);
        return stat;
    }

    private List<ItemStat> loadStats() {
        List<ItemStat> stats = new ArrayList<>();
        currentPageCounter = 1;
        currentSlotIndex = 0;

        // Slot 19: Revision ID (Item Frame)
        addStat(stats, Material.ITEM_FRAME, "revision_id", String.valueOf(item.getRevisionId()))
            .setEditor((player, module, item, gui, click) -> new RevisionConfigurationGui(player, module, item, gui).open());

        // Slot 20: Material (Grass Block)
        addStat(stats, Material.GRASS_BLOCK, "material", item.getMaterial().name())
            .setEditor((p, module, item, gui, click) -> {
                ItemStack cursor = p.getItemOnCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    // Nexo Integration
                    if (NexoUtils.isCustomItem(cursor)) {
                        String nexoId = NexoUtils.getCustomItemId(cursor);
                        ItemStack nexoItem = NexoUtils.getCustomItem(nexoId);
                        if (nexoItem != null) {
                            item.setMaterial(nexoItem.getType());
                            if (nexoItem.hasItemMeta() && nexoItem.getItemMeta().hasCustomModelData()) {
                                item.setCustomModelData(nexoItem.getItemMeta().getCustomModelData());
                            } else {
                                item.setCustomModelData(0);
                            }
                            item.save();
                            gui.initializeItems();
                            p.sendMessage(MessageUtils.parse(getSafeMessage("item.gui.item_edition.messages.material-updated", "<green>✔</green> <gray>ᴍᴀᴛᴇʀɪᴀʟ ᴀᴛᴜᴀʟɪᴢᴀᴅᴏ ᴠɪᴀ ɪɴᴠᴇɴᴛᴀʀɪᴏ!</gray>")));
                            return;
                        }
                    }

                    item.setMaterial(cursor.getType());
                    if (cursor.hasItemMeta() && cursor.getItemMeta().hasCustomModelData()) {
                        item.setCustomModelData(cursor.getItemMeta().getCustomModelData());
                    }
                    item.save();
                    gui.initializeItems();
                    p.sendMessage(MessageUtils.parse(getSafeMessage("item.gui.item_edition.messages.material-updated", "<green>✔</green> <gray>ᴍᴀᴛᴇʀɪᴀʟ ᴀᴛᴜᴀʟɪᴢᴀᴅᴏ ᴠɪᴀ ɪɴᴠᴇɴᴛᴀʀɪᴏ!</gray>")));
                } else {
                    new MaterialSelectionGui(p, module, item, gui).open();
                }
            });

        // Slot 21: Base Item Damage (Fishing Rod)
        addStat(stats, Material.FISHING_ROD, "base_item_damage", String.valueOf(item.getBaseItemDamage()))
            .setEditor(new IntegerEditor((i, v) -> i.setBaseItemDamage(v), MidgardItem::getBaseItemDamage, lang.getRawMessage("item.gui.editor.prompt.base-item-damage")));

        // Slot 22: Custom Model Data (Painting)
        addStat(stats, Material.PAINTING, "custom_model_data", String.valueOf(item.getCustomModelData()))
            .setEditor(new IntegerEditor((i, v) -> i.setCustomModelData(v), MidgardItem::getCustomModelData, lang.getRawMessage("item.gui.editor.prompt.custom-model-data")));

        // Slot 23: Custom Model Data Strings (Painting)
        addStat(stats, Material.PAINTING, "custom_model_data_strings", item.getCustomModelDataStrings())
            .setEditor(new StringEditor(MidgardItem::setCustomModelDataStrings, MidgardItem::getCustomModelDataStrings, lang.getRawMessage("item.gui.editor.prompt.custom-model-data-strings")));

        // Slot 24: Custom Model Data Floats (Painting)
        addStat(stats, Material.PAINTING, "custom_model_data_floats", item.getCustomModelDataFloats())
            .setEditor(new StringEditor(MidgardItem::setCustomModelDataFloats, MidgardItem::getCustomModelDataFloats, lang.getRawMessage("item.gui.editor.prompt.custom-model-data-floats")));

        // Slot 25: Item Model (Painting)
        addStat(stats, Material.PAINTING, "item_model", item.getItemModel())
            .setEditor(new StringEditor(MidgardItem::setItemModel, MidgardItem::getItemModel, lang.getRawMessage("item.gui.editor.prompt.item-model")));

        // Slot 28: Equippable Slot (Leather Leggings)
        addStat(stats, Material.LEATHER_LEGGINGS, "equippable_slot", item.getEquippableSlot())
            .setEditor(new ActionEditor((p, i) -> new EquippableSlotSelectionGui(p, module, i, this).open()));

        // Slot 29: Equippable Model (Leather Chestplate)
        addStat(stats, Material.LEATHER_CHESTPLATE, "equippable_model", item.getEquippableModel())
            .setEditor(new ActionEditor((p, i) -> new EquippableModelSelectionGui(p, module, i, this).open()));

        // Slot 30: Maximum Custom Durability (Shears)
        addStat(stats, Material.SHEARS, "max_custom_durability", String.valueOf(item.getMaxCustomDurability()))
            .setEditor(new IntegerEditor((i, v) -> i.setMaxCustomDurability(v), MidgardItem::getMaxCustomDurability, lang.getRawMessage("item.gui.editor.prompt.max-custom-durability")));

        // Slot 31: Maximum Vanilla Durability (Damaged Anvil)
        addStat(stats, Material.DAMAGED_ANVIL, "max_vanilla_durability", String.valueOf(item.getMaxVanillaDurability()))
            .setEditor(new IntegerEditor((i, v) -> i.setMaxVanillaDurability(v), MidgardItem::getMaxVanillaDurability, lang.getRawMessage("item.gui.editor.prompt.max-vanilla-durability")));

        // Slot 32: Lost when Broken? (Shears)
        addStat(stats, Material.SHEARS, "lost_when_broken", item.isLostWhenBroken())
            .setEditor(new BooleanEditor((i, v) -> i.setLostWhenBroken(v), MidgardItem::isLostWhenBroken));

        // Slot 33: Display Name (Name Tag)
        addStat(stats, Material.NAME_TAG, "display_name", item.getDisplayName())
            .setEditor(new StringEditor(MidgardItem::setDisplayName, MidgardItem::getDisplayName, lang.getRawMessage("item.gui.editor.prompt.display-name")));

        // Slot 34: Lore (Writable Book)
        addStat(stats, Material.WRITABLE_BOOK, "lore", item.getLore())
            .setEditor(new ListEditor(MidgardItem::setLore, MidgardItem::getLore, lang.getRawMessage("item.gui.editor.prompt.lore")));

        // Slot 37: NBT Tags (Name Tag)
        addStat(stats, Material.NAME_TAG, "nbt_tags", item.getNbtTags())
            .setEditor(new ActionEditor((p, i) -> new NbtEditorGui(p, module, i, this).open()));

        // Slot 38: Max Stack Size (Chest)
        addStat(stats, Material.CHEST, "max_stack_size", String.valueOf(item.getMaxStackSize()))
            .setEditor(new IntegerEditor((i, v) -> i.setMaxStackSize(v), MidgardItem::getMaxStackSize, lang.getRawMessage("item.gui.editor.prompt.max-stack-size")));

        // Slot 39: Lore Format (Map)
        addStat(stats, Material.MAP, "lore_format", item.getLoreFormat())
            .setEditor(new StringEditor(MidgardItem::setLoreFormat, MidgardItem::getLoreFormat, lang.getRawMessage("item.gui.editor.prompt.lore-format")));

        // Slot 40: Custom Tooltip (Birch Sign)
        addStat(stats, Material.BIRCH_SIGN, "custom_tooltip", item.getCustomTooltip())
            .setEditor(new StringEditor(MidgardItem::setCustomTooltip, MidgardItem::getCustomTooltip, lang.getRawMessage("item.gui.editor.prompt.custom-tooltip")));

        // Slot 41: Vanilla Tooltip Style (Acacia Sign)
        addStat(stats, Material.ACACIA_SIGN, "vanilla_tooltip_style", item.getVanillaTooltipStyle())
            .setEditor(new StringEditor(MidgardItem::setVanillaTooltipStyle, MidgardItem::getVanillaTooltipStyle, lang.getRawMessage("item.gui.editor.prompt.vanilla-tooltip-style")));

        // Slot 42: Displayed Type (Oak Sign)
        addStat(stats, Material.OAK_SIGN, "displayed_type", item.getDisplayedType())
            .setEditor(new StringEditor(MidgardItem::setDisplayedType, MidgardItem::getDisplayedType, lang.getRawMessage("item.gui.editor.prompt.displayed-type")));

        // Slot 43: Enchantments (Enchanted Book)
        addStat(stats, Material.ENCHANTED_BOOK, "enchantments", item.getEnchantments())
            .setEditor(new ActionEditor((p, i) -> new EnchantmentSelectionGui(p, module, i, this).open()));

        // Damage Types (Iron Sword)
        List<String> activeDamageTypes = new ArrayList<>();
        List<me.ray.midgard.modules.item.model.ItemStat> damageStats = java.util.List.of(
            me.ray.midgard.modules.item.model.ItemStat.ATTACK_DAMAGE,
            me.ray.midgard.modules.item.model.ItemStat.MAGIC_DAMAGE,
            me.ray.midgard.modules.item.model.ItemStat.PROJECTILE_DAMAGE,
            me.ray.midgard.modules.item.model.ItemStat.SKILL_DAMAGE,
            me.ray.midgard.modules.item.model.ItemStat.UNDEAD_DAMAGE,
            me.ray.midgard.modules.item.model.ItemStat.FIRE_DAMAGE,
            me.ray.midgard.modules.item.model.ItemStat.ICE_DAMAGE,
            me.ray.midgard.modules.item.model.ItemStat.LIGHT_DAMAGE,
            me.ray.midgard.modules.item.model.ItemStat.DARKNESS_DAMAGE,
            me.ray.midgard.modules.item.model.ItemStat.DIVINE_DAMAGE
        );
        for (me.ray.midgard.modules.item.model.ItemStat stat : damageStats) {
            if (item.getStat(stat) > 0) {
                activeDamageTypes.add(stat.name());
            }
        }

        addStat(stats, Material.IRON_SWORD, "damage_types", activeDamageTypes)
            .setEditor(new ActionEditor((p, i) -> new DamageTypeEditorGui(p, module, i, this).open()));

        // Slot 19: Hide Enchantments (Book)
        addStat(stats, Material.BOOK, "hide_enchantments", item.isHideEnchantments())
            .setEditor(new BooleanEditor((i, v) -> i.setHideEnchantments(v), MidgardItem::isHideEnchantments));

        // Slot 20: Hide Tooltip (Acacia Sign)
        addStat(stats, Material.ACACIA_SIGN, "hide_tooltip", item.isHideTooltip())
            .setEditor(new BooleanEditor((i, v) -> i.setHideTooltip(v), MidgardItem::isHideTooltip));

        // Slot 21: Permission (Oak Sign)
        addStat(stats, Material.OAK_SIGN, "permission", item.getPermission())
            .setEditor(new StringEditor(MidgardItem::setPermission, MidgardItem::getPermission, lang.getRawMessage("item.gui.editor.prompt.permission")));

        // Slot 22: Item Particles (Pink Stained Glass)
        addStat(stats, Material.PINK_STAINED_GLASS, "item_particles", item.getItemParticles())
            .setEditor(new ActionEditor((p, i) -> new ParticleEditorGui(p, module, i, this).open()));

        // Slot 23: Disable Interaction (Grass Block)
        addStat(stats, Material.GRASS_BLOCK, "disable_interaction", item.isDisableInteraction())
            .setEditor(new BooleanEditor((i, v) -> i.setDisableInteraction(v), MidgardItem::isDisableInteraction));

        // Slot 24: Disable Crafting (Crafting Table)
        addStat(stats, Material.CRAFTING_TABLE, "disable_crafting", item.isDisableCrafting())
            .setEditor(new BooleanEditor((i, v) -> i.setDisableCrafting(v), MidgardItem::isDisableCrafting));

        // Slot 25: Disable Smelting (Furnace)
        addStat(stats, Material.FURNACE, "disable_smelting", item.isDisableSmelting())
            .setEditor(new BooleanEditor((i, v) -> i.setDisableSmelting(v), MidgardItem::isDisableSmelting));

        // Slot 28: Disable Smithing (Damaged Anvil)
        addStat(stats, Material.DAMAGED_ANVIL, "disable_smithing", item.isDisableSmithing())
            .setEditor(new BooleanEditor((i, v) -> i.setDisableSmithing(v), MidgardItem::isDisableSmithing));

        // Slot 29: Disable Enchanting (Enchanting Table)
        addStat(stats, Material.ENCHANTING_TABLE, "disable_enchanting", item.isDisableEnchanting())
            .setEditor(new BooleanEditor((i, v) -> i.setDisableEnchanting(v), MidgardItem::isDisableEnchanting));

        // Slot 30: Disable Repairing (Anvil)
        addStat(stats, Material.ANVIL, "disable_repairing", item.isDisableRepairing())
            .setEditor(new BooleanEditor((i, v) -> i.setDisableRepairing(v), MidgardItem::isDisableRepairing));

        // Slot 31: Disable Item Dropping (Lava Bucket)
        addStat(stats, Material.LAVA_BUCKET, "disable_item_dropping", item.isDisableItemDropping())
            .setEditor(new BooleanEditor((i, v) -> i.setDisableItemDropping(v), MidgardItem::isDisableItemDropping));

        // Slot 32: Required Level (Experience Bottle)
        addStat(stats, Material.EXPERIENCE_BOTTLE, "required_level", String.valueOf(item.getRequiredLevel()))
            .setEditor(new IntegerEditor((i, v) -> i.setRequiredLevel(v), MidgardItem::getRequiredLevel, lang.getRawMessage("item.gui.editor.prompt.required-level")));

        // Slot 33: Required Classes (Writable Book)
        List<String> reqClassIds = item.getRequiredClasses();
        String reqClassNames;
        if (reqClassIds == null || reqClassIds.isEmpty()) {
            reqClassNames = lang.getRawMessage("item.gui.item_edition.values.none");
        } else if (ClassesModule.getInstance() != null) {
            reqClassNames = reqClassIds.stream()
                .map(id -> {
                    RPGClass c = ClassesModule.getInstance().getClassManager().getClass(id);
                    return c != null ? c.getDisplayName() : id;
                })
                .collect(java.util.stream.Collectors.joining(", "));
        } else {
            reqClassNames = String.join(", ", reqClassIds);
        }
        addStat(stats, Material.WRITABLE_BOOK, "required_class", reqClassNames)
            .setEditor(new ActionEditor((p, i) -> new ClassRequirementSelectionGui(p, module, i, this).open()));

        // Slot 34: Critical Strike Chance (Nether Star)
        addStat(stats, Material.NETHER_STAR, "critical_strike_chance", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.CRITICAL_STRIKE_CHANCE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.CRITICAL_STRIKE_CHANCE));

        // Slot 37: Critical Strike Power (Nether Star)
        addStat(stats, Material.NETHER_STAR, "critical_strike_power", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.CRITICAL_STRIKE_POWER)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.CRITICAL_STRIKE_POWER));

        // Slot 38: Skill Critical Strike Chance (Nether Star)
        addStat(stats, Material.NETHER_STAR, "skill_critical_strike_chance", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SKILL_CRITICAL_STRIKE_CHANCE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SKILL_CRITICAL_STRIKE_CHANCE));

        // Slot 39: Skill Critical Strike Power (Nether Star)
        addStat(stats, Material.NETHER_STAR, "skill_critical_strike_power", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SKILL_CRITICAL_STRIKE_POWER)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SKILL_CRITICAL_STRIKE_POWER));

        // Slot 40: Block Power (Iron Helmet)
        addStat(stats, Material.IRON_HELMET, "block_power", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.BLOCK_POWER)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.BLOCK_POWER));

        // Slot 41: Block Rating (Iron Helmet)
        addStat(stats, Material.IRON_HELMET, "block_rating", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.BLOCK_RATING)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.BLOCK_RATING));

        // Slot 42: Block Cooldown Reduction (Iron Helmet)
        addStat(stats, Material.IRON_HELMET, "block_cooldown_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.BLOCK_COOLDOWN_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.BLOCK_COOLDOWN_REDUCTION));

        // Slot 43: Dodge Rating (Feather)
        addStat(stats, Material.FEATHER, "dodge_rating", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.DODGE_RATING)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.DODGE_RATING));

        // Page 3
        // Slot 19: Dodge Cooldown Reduction (Feather)
        addStat(stats, Material.FEATHER, "dodge_cooldown_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.DODGE_COOLDOWN_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.DODGE_COOLDOWN_REDUCTION));

        // Slot 20: Parry Rating (Bucket)
        addStat(stats, Material.BUCKET, "parry_rating", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.PARRY_RATING)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.PARRY_RATING));

        // Slot 21: Parry Cooldown Reduction (Bucket)
        addStat(stats, Material.BUCKET, "parry_cooldown_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.PARRY_COOLDOWN_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.PARRY_COOLDOWN_REDUCTION));

        // Slot 22: Cooldown Reduction (Book)
        addStat(stats, Material.BOOK, "cooldown_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.COOLDOWN_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.COOLDOWN_REDUCTION));

        // Slot 23: Attack Speed (Golden Sword)
        addStat(stats, Material.GOLDEN_SWORD, "attack_speed", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ATTACK_SPEED)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ATTACK_SPEED));

        // Two Handed (Shield)
        addStat(stats, Material.SHIELD, "two_handed", item.getStat(me.ray.midgard.modules.item.model.ItemStat.TWO_HANDED) > 0)
            .setEditor(new BooleanEditor(
                (i, val) -> i.setStat(me.ray.midgard.modules.item.model.ItemStat.TWO_HANDED, val ? 1.0 : 0.0),
                i -> i.getStat(me.ray.midgard.modules.item.model.ItemStat.TWO_HANDED) > 0
            ));

        // Slot 24: Skill Damage (Book)
        addStat(stats, Material.BOOK, "skill_damage", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SKILL_DAMAGE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SKILL_DAMAGE));

        // Slot 25: Projectile Damage (Arrow)
        addStat(stats, Material.ARROW, "projectile_damage", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.PROJECTILE_DAMAGE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.PROJECTILE_DAMAGE));

        // Slot 28: Magic Damage (Magma Cream)
        addStat(stats, Material.MAGMA_CREAM, "magic_damage", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAGIC_DAMAGE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAGIC_DAMAGE));

        // Slot 29: Defense (Shield)
        addStat(stats, Material.SHIELD, "defense", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.DEFENSE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.DEFENSE));

        // Slot 31: Damage Reduction (Iron Chestplate)
        addStat(stats, Material.IRON_CHESTPLATE, "damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.DAMAGE_REDUCTION));

        // Slot 32: Fall Damage Reduction (Feather)
        addStat(stats, Material.FEATHER, "fall_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.FALL_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.FALL_DAMAGE_REDUCTION));

        // Slot 33: Projectile Damage Reduction (Snowball)
        addStat(stats, Material.SNOWBALL, "projectile_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.PROJECTILE_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.PROJECTILE_DAMAGE_REDUCTION));

        // Slot 34: Physical Damage Reduction (Leather Chestplate)
        addStat(stats, Material.LEATHER_CHESTPLATE, "physical_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.PHYSICAL_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.PHYSICAL_DAMAGE_REDUCTION));

        // Slot 35: Fire Damage Reduction (Blaze Powder)
        addStat(stats, Material.BLAZE_POWDER, "fire_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.FIRE_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.FIRE_DAMAGE_REDUCTION));

        // Slot 38: Magic Damage Reduction (Potion)
        addStat(stats, Material.POTION, "magic_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAGIC_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAGIC_DAMAGE_REDUCTION));

        // Slot 39: Pve Damage Reduction (Porkchop)
        addStat(stats, Material.PORKCHOP, "pve_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.PVE_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.PVE_DAMAGE_REDUCTION));

        // Slot 40: Pvp Damage Reduction (Skeleton Skull)
        addStat(stats, Material.SKELETON_SKULL, "pvp_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.PVP_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.PVP_DAMAGE_REDUCTION));

        // Slot 41: Undead Damage (Skeleton Skull)
        addStat(stats, Material.SKELETON_SKULL, "undead_damage", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.UNDEAD_DAMAGE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.UNDEAD_DAMAGE));

        // Slot 42: Life Steal (Redstone)
        addStat(stats, Material.REDSTONE, "lifesteal", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.LIFESTEAL)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.LIFESTEAL));

        // Slot 43: Spell Vampirism (Redstone)
        addStat(stats, Material.REDSTONE, "spell_vampirism", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SPELL_VAMPIRISM)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SPELL_VAMPIRISM));

        // Page 4
        // Slot 19: Unbreakable (Anvil)
        addStat(stats, Material.ANVIL, "unbreakable", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.UNBREAKABLE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.UNBREAKABLE));

        // Slot 20: Item Tier (Diamond)
        addStat(stats, Material.DIAMOND, "tier", item.getTier())
            .setEditor(new ActionEditor((p, i) -> new TierSelectionGui(p, module, i, this).open()));

        // Slot 21: Item Set (Leather Chestplate)
        addStat(stats, Material.LEATHER_CHESTPLATE, "item_set", item.getItemSet())
            .setEditor(new ActionEditor((p, i) -> new SetSelectionGui(p, module, i, this).open()));

        // Slot 22: Armor (Golden Chestplate)
        addStat(stats, Material.GOLDEN_CHESTPLATE, "armor", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ARMOR)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ARMOR));

        // Slot 23: Armor Toughness (Diamond Chestplate)
        addStat(stats, Material.DIAMOND_CHESTPLATE, "armor_toughness", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ARMOR_TOUGHNESS)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ARMOR_TOUGHNESS));

        // Slot 24: Max Health (Golden Apple)
        addStat(stats, Material.GOLDEN_APPLE, "max_health", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAX_HEALTH)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAX_HEALTH));

        // Slot 25: Unstackable (Chest Minecart)
        addStat(stats, Material.CHEST_MINECART, "unstackable", item.isUnstackable())
            .setEditor(new BooleanEditor((i, v) -> i.setUnstackable(v), MidgardItem::isUnstackable));

        // Slot 28: Max Mana (Lapis Lazuli)
        addStat(stats, Material.LAPIS_LAZULI, "max_mana", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAX_MANA)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAX_MANA));

        // Slot 29: Knockback Resistance (Chainmail Chestplate)
        addStat(stats, Material.CHAINMAIL_CHESTPLATE, "knockback_resistance", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.KNOCKBACK_RESISTANCE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.KNOCKBACK_RESISTANCE));

        // Slot 30: Movement Speed (Leather Boots)
        addStat(stats, Material.LEATHER_BOOTS, "movement_speed", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MOVEMENT_SPEED)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MOVEMENT_SPEED));

        // Slot 31: Required Biomes (Jungle Sapling)
        addStat(stats, Material.JUNGLE_SAPLING, "required_biomes", item.getRequiredBiomes())
            .setEditor(new ActionEditor((p, i) -> new BiomeSelectionGui(p, module, i, this).open()));

        // Slot 32: Disable Drop on Death (Bone)
        addStat(stats, Material.BONE, "disable_drop_on_death", item.isDisableDropOnDeath())
            .setEditor(new BooleanEditor((i, v) -> i.setDisableDropOnDeath(v), MidgardItem::isDisableDropOnDeath));

        // Slot 33: Hide Durability Bar (Damaged Anvil)
        addStat(stats, Material.DAMAGED_ANVIL, "hide_durability_bar", item.isHideDurabilityBar())
            .setEditor(new BooleanEditor((i, v) -> i.setHideDurabilityBar(v), MidgardItem::isHideDurabilityBar));

        // Slot 34: Camera Overlay (Glass)
        addStat(stats, Material.GLASS, "camera_overlay", item.getCameraOverlay())
            .setEditor(new StringEditor(MidgardItem::setCameraOverlay, MidgardItem::getCameraOverlay, lang.getRawMessage("item.gui.editor.prompt.camera-overlay")));

        // Slot 37: Max Absorption (Enchanted Golden Apple)
        addStat(stats, Material.ENCHANTED_GOLDEN_APPLE, "max_absorption", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAX_ABSORPTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAX_ABSORPTION));

        // Slot 38: Mining Speed (Iron Pickaxe)
        addStat(stats, Material.IRON_PICKAXE, "mining_speed", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MINING_SPEED)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MINING_SPEED));

        // Slot 39: Block Interaction Range (Spyglass)
        addStat(stats, Material.SPYGLASS, "block_interaction_range", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.BLOCK_INTERACTION_RANGE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.BLOCK_INTERACTION_RANGE));

        // Slot 40: Entity Interaction Range (Spyglass)
        addStat(stats, Material.SPYGLASS, "entity_interaction_range", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ENTITY_INTERACTION_RANGE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ENTITY_INTERACTION_RANGE));

        // Slot 41: Fall Damage Multiplier (Damaged Anvil)
        addStat(stats, Material.DAMAGED_ANVIL, "fall_damage_multiplier", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.FALL_DAMAGE_MULTIPLIER)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.FALL_DAMAGE_MULTIPLIER));

        // Slot 42: Gravity (Stone)
        addStat(stats, Material.STONE, "gravity", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.GRAVITY)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.GRAVITY));

        // Slot 43: Jump Strength (Saddle)
        addStat(stats, Material.SADDLE, "jump_strength", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.JUMP_STRENGTH)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.JUMP_STRENGTH));

        // Page 5
        // Slot 19: Safe Fall Distance (Bed)
        addStat(stats, Material.RED_BED, "safe_fall_distance", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SAFE_FALL_DISTANCE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SAFE_FALL_DISTANCE));

        // Slot 20: Scale (Stone)
        addStat(stats, Material.STONE, "scale", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SCALE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SCALE));

        // Slot 21: Step Height (Stone Slab)
        addStat(stats, Material.STONE_SLAB, "step_height", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.STEP_HEIGHT)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.STEP_HEIGHT));

        // Slot 22: Burning Time (Fire Charge)
        addStat(stats, Material.FIRE_CHARGE, "burning_time", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.BURNING_TIME)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.BURNING_TIME));

        // Slot 23: Explosion Knockback Resistance (Obsidian)
        addStat(stats, Material.OBSIDIAN, "explosion_knockback_resistance", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.EXPLOSION_KNOCKBACK_RESISTANCE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.EXPLOSION_KNOCKBACK_RESISTANCE));

        // Slot 24: Mining Efficiency (Iron Pickaxe)
        addStat(stats, Material.IRON_PICKAXE, "mining_efficiency", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MINING_EFFICIENCY)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MINING_EFFICIENCY));

        // Slot 25: Movement Efficiency (Leather Boots)
        addStat(stats, Material.LEATHER_BOOTS, "movement_efficiency", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MOVEMENT_EFFICIENCY)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MOVEMENT_EFFICIENCY));

        // Slot 28: Bonus Oxygen (Conduit)
        addStat(stats, Material.CONDUIT, "bonus_oxygen", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.BONUS_OXYGEN)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.BONUS_OXYGEN));

        // Slot 29: Sneaking Speed (Leather Boots)
        addStat(stats, Material.LEATHER_BOOTS, "sneaking_speed", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SNEAKING_SPEED)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SNEAKING_SPEED));

        // Slot 30: Submerged Mining Speed (Water Bucket)
        addStat(stats, Material.WATER_BUCKET, "submerged_mining_speed", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SUBMERGED_MINING_SPEED)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SUBMERGED_MINING_SPEED));

        // Slot 31: Sweeping Damage Ratio (Light Gray Dye)
        addStat(stats, Material.LIGHT_GRAY_DYE, "sweeping_damage_ratio", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SWEEPING_DAMAGE_RATIO)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SWEEPING_DAMAGE_RATIO));

        // Slot 32: Water Movement Efficiency (Water Bucket)
        addStat(stats, Material.WATER_BUCKET, "water_movement_efficiency", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.WATER_MOVEMENT_EFFICIENCY)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.WATER_MOVEMENT_EFFICIENCY));

        // Slot 33: Permanent Effects (Potion)
        addStat(stats, Material.POTION, "permanent_effects", item.getPermanentEffects())
            .setEditor(new ActionEditor((p, i) -> new PotionEffectSelectionGui(p, module, i, this).open()));

        // Slot 34: Granted Permissions (Name Tag)
        addStat(stats, Material.NAME_TAG, "granted_permissions", item.getGrantedPermissions())
            .setEditor(new ListEditor(MidgardItem::setGrantedPermissions, MidgardItem::getGrantedPermissions, lang.getRawMessage("item.gui.editor.prompt.granted-permissions")));

        // Slot 37: Item Cooldown (Cooked Chicken)
        addStat(stats, Material.COOKED_CHICKEN, "item_cooldown", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ITEM_COOLDOWN)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ITEM_COOLDOWN));

        // Slot 38: Cooldown Reference (Chicken)
        addStat(stats, Material.CHICKEN, "cooldown_reference", item.getCooldownReference())
            .setEditor(new StringEditor(MidgardItem::setCooldownReference, MidgardItem::getCooldownReference, lang.getRawMessage("item.gui.editor.prompt.cooldown-reference")));

        // Slot 39: Success Rate (Emerald)
        addStat(stats, Material.EMERALD, "success_rate", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.SUCCESS_RATE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.SUCCESS_RATE));

        // Slot 40: Crafting (Crafting Table)
        addStat(stats, Material.CRAFTING_TABLE, "crafting", item.getRecipes())
            .setEditor(new me.ray.midgard.modules.item.gui.editors.impl.CraftingEditor());

        // Slot 41: Crafting Recipe Permission (Oak Sign)
        addStat(stats, Material.OAK_SIGN, "crafting_recipe_permission", item.getCraftingRecipePermission())
            .setEditor(new StringEditor(MidgardItem::setCraftingRecipePermission, MidgardItem::getCraftingRecipePermission, lang.getRawMessage("item.gui.editor.prompt.crafting-recipe-permission")));

        // Slot 42: Custom Sounds (Jukebox)
        addStat(stats, Material.JUKEBOX, "custom_sounds", item.getCustomSounds())
            .setEditor(new ActionEditor((p, i) -> new SoundEditorGui(p, module, i, this).open()));

        // Slot 43: Commands (Command Block Minecart)
        addStat(stats, Material.COMMAND_BLOCK_MINECART, "commands", item.getCommands())
            .setEditor(new ListEditor(MidgardItem::setCommands, MidgardItem::getCommands, lang.getRawMessage("item.gui.editor.prompt.commands")));

        // Page 6
        // Slot 19: Compatible Types (Command Block)
        addStat(stats, Material.COMMAND_BLOCK, "compatible_types", item.getCompatibleTypes())
            .setEditor(new ListEditor(MidgardItem::setCompatibleTypes, MidgardItem::getCompatibleTypes, lang.getRawMessage("item.gui.editor.prompt.compatible-types")));

        // Slot 20: Compatible IDs (Command Block)
        addStat(stats, Material.COMMAND_BLOCK, "compatible_ids", item.getCompatibleIds())
            .setEditor(new ListEditor(MidgardItem::setCompatibleIds, MidgardItem::getCompatibleIds, lang.getRawMessage("item.gui.editor.prompt.compatible-ids")));

        // Slot 21: Repair Reference (Anvil)
        addStat(stats, Material.ANVIL, "repair_reference", item.getRepairReference())
            .setEditor(new StringEditor(MidgardItem::setRepairReference, MidgardItem::getRepairReference, lang.getRawMessage("item.gui.editor.prompt.repair-reference")));

        // Slot 22: Amphibian (Water Bucket)
        addStat(stats, Material.WATER_BUCKET, "amphibian", item.isAmphibian())
            .setEditor(new BooleanEditor((i, v) -> i.setAmphibian(v), MidgardItem::isAmphibian));

        // Slot 23: Item Abilities (Blaze Powder)
        addStat(stats, Material.BLAZE_POWDER, "item_abilities", item.getItemAbilities())
            .setEditor(new ActionEditor((p, i) -> new AbilitySelectionGui(p, module, i, this).open()));

        // Slot 24: Gem Sockets (Emerald)
        addStat(stats, Material.EMERALD, "gem_sockets", item.getGemSockets())
            .setEditor(new ActionEditor((p, i) -> new SocketConfigurationGui(p, module, i, this).open()));

        // Slot 25: Compatible Materials (List)
        addStat(stats, Material.CHEST, "compatible_materials", item.getCompatibleMaterials())
            .setEditor(new ActionEditor((p, i) -> new MaterialListSelectionGui(p, module, i, this).open()));
            
        // Slot 28: Trim Material (Leather Chestplate)
        addStat(stats, Material.GOLD_INGOT, "trim_material", item.getTrimMaterial())
             .setEditor(new ActionEditor((p, i) -> new TrimEditorGui(p, module, i, this).open()));
             
        // Slot 36: Trim Pattern (Leather Chestplate)
        addStat(stats, Material.FILLED_MAP, "trim_pattern", item.getTrimPattern())
             .setEditor(new ActionEditor((p, i) -> new TrimEditorGui(p, module, i, this).open()));

        // Slot 29: Hide Armor Trim (Leather Chestplate)
        addStat(stats, Material.LEATHER_CHESTPLATE, "hide_armor_trim", item.isHideArmorTrim())
            .setEditor(new BooleanEditor((i, v) -> i.setHideArmorTrim(v), MidgardItem::isHideArmorTrim));

        // Slot 30: Browser Index (Ghast Tear)
        addStat(stats, Material.GHAST_TEAR, "browser_index", String.valueOf(item.getBrowserIndex()))
            .setEditor(new IntegerEditor((i, v) -> i.setBrowserIndex(v), MidgardItem::getBrowserIndex, lang.getRawMessage("item.gui.editor.prompt.browser-index")));

        // Slot 31: MyLuck (Paper)
        addStat(stats, Material.PAPER, "myluck", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MYLUCK)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MYLUCK));

        // Slot 32: Health Regeneration (Bread)
        addStat(stats, Material.BREAD, "health_regeneration", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.HEALTH_REGENERATION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.HEALTH_REGENERATION));

        // Slot 33: Max Health Regeneration (Bread)
        addStat(stats, Material.BREAD, "max_health_regeneration", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAX_HEALTH_REGENERATION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAX_HEALTH_REGENERATION));

        // Slot 34: Mana Regeneration (Lapis Lazuli)
        addStat(stats, Material.LAPIS_LAZULI, "mana_regeneration", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MANA_REGENERATION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MANA_REGENERATION));

        // Slot 37: Max Mana Regeneration (Lapis Lazuli)
        addStat(stats, Material.LAPIS_LAZULI, "max_mana_regeneration", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAX_MANA_REGENERATION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAX_MANA_REGENERATION));

        // Slot 38: Stamina Regeneration (Light Blue Dye)
        addStat(stats, Material.LIGHT_BLUE_DYE, "stamina_regeneration", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.STAMINA_REGENERATION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.STAMINA_REGENERATION));

        // Slot 39: Max Stamina Regeneration (Light Blue Dye)
        addStat(stats, Material.LIGHT_BLUE_DYE, "max_stamina_regeneration", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAX_STAMINA_REGENERATION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAX_STAMINA_REGENERATION));

        // Slot 40: Max Stamina (Light Blue Dye)
        addStat(stats, Material.LIGHT_BLUE_DYE, "max_stamina", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAX_STAMINA)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAX_STAMINA));

        // Slot 41: Max Stellium (Ender Eye)
        addStat(stats, Material.ENDER_EYE, "max_stellium", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAX_STELLIUM)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAX_STELLIUM));

        // Slot 42: Additional Experience (Experience Bottle)
        addStat(stats, Material.EXPERIENCE_BOTTLE, "additional_experience", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ADDITIONAL_EXPERIENCE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ADDITIONAL_EXPERIENCE));

        // Slot 43: Disable Advanced Enchants (Enchanted Book)
        addStat(stats, Material.ENCHANTED_BOOK, "disable_advanced_enchants", item.isDisableAdvancedEnchants())
            .setEditor(new BooleanEditor((i, v) -> i.setDisableAdvancedEnchants(v), MidgardItem::isDisableAdvancedEnchants));

        // New RPG Stats
        // Strength (Iron Sword)
        addStat(stats, Material.IRON_SWORD, "strength", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.STRENGTH)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.STRENGTH));

        // Intelligence (Book)
        addStat(stats, Material.BOOK, "intelligence", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.INTELLIGENCE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.INTELLIGENCE));

        // Dexterity (Feather)
        addStat(stats, Material.FEATHER, "dexterity", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.DEXTERITY)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.DEXTERITY));

        // Accuracy (Spyglass)
        addStat(stats, Material.SPYGLASS, "accuracy", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ACCURACY)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ACCURACY));

        // Critical Resistance (Shield)
        addStat(stats, Material.SHIELD, "critical_resistance", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.CRITICAL_RESISTANCE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.CRITICAL_RESISTANCE));

        // Thorns (Cactus)
        addStat(stats, Material.CACTUS, "thorns", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.THORNS)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.THORNS));

        // Magic Resistance (Potion)
        addStat(stats, Material.POTION, "magic_resistance", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAGIC_RESISTANCE)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAGIC_RESISTANCE));

        // Armor Penetration (Iron Axe)
        addStat(stats, Material.IRON_AXE, "armor_penetration", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ARMOR_PENETRATION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ARMOR_PENETRATION));

        // Armor Penetration Flat (Iron Axe)
        addStat(stats, Material.IRON_AXE, "armor_penetration_flat", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ARMOR_PENETRATION_FLAT)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ARMOR_PENETRATION_FLAT));

        // Magic Penetration (Blaze Rod)
        addStat(stats, Material.BLAZE_ROD, "magic_penetration", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAGIC_PENETRATION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAGIC_PENETRATION));

        // Magic Penetration Flat (Blaze Rod)
        addStat(stats, Material.BLAZE_ROD, "magic_penetration_flat", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.MAGIC_PENETRATION_FLAT)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.MAGIC_PENETRATION_FLAT));

        // Ice Damage Reduction (Ice)
        addStat(stats, Material.ICE, "ice_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.ICE_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.ICE_DAMAGE_REDUCTION));

        // Light Damage Reduction (Glowstone)
        addStat(stats, Material.GLOWSTONE, "light_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.LIGHT_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.LIGHT_DAMAGE_REDUCTION));

        // Darkness Damage Reduction (Coal)
        addStat(stats, Material.COAL, "darkness_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.DARKNESS_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.DARKNESS_DAMAGE_REDUCTION));

        // Divine Damage Reduction (Gold Ingot)
        addStat(stats, Material.GOLD_INGOT, "divine_damage_reduction", String.valueOf(item.getStat(me.ray.midgard.modules.item.model.ItemStat.DIVINE_DAMAGE_REDUCTION)))
            .setEditor(new RpgStatEditor(me.ray.midgard.modules.item.model.ItemStat.DIVINE_DAMAGE_REDUCTION));
            
        return stats;
    }
}
