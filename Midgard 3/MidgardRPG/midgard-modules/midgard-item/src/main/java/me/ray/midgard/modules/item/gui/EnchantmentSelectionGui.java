package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class EnchantmentSelectionGui extends PaginatedGui<Enchantment> {

    private final MidgardItem item;
    private final ItemEditionGui parent;
    private final LanguageManager lang;
    private final Map<Enchantment, Integer> currentEnchants;

    public EnchantmentSelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.enchantment_selection.title"), new ArrayList<>());
        this.item = item;
        this.parent = parent;
        this.lang = MidgardCore.getLanguageManager();
        this.currentEnchants = parseEnchantments(item.getEnchantments());
        
        // Load all registered enchantments
        List<Enchantment> allEnchants = new ArrayList<>();
        Registry<Enchantment> registry = getEnchantmentRegistry();
        if (registry != null) {
            for (Enchantment enchant : registry) {
                allEnchants.add(enchant);
            }
        }
        
        // Sort by key
        allEnchants.sort(Comparator.comparing(e -> e.getKey().getKey()));
        this.items = allEnchants;
    }

    private Map<Enchantment, Integer> parseEnchantments(String serialized) {
        Map<Enchantment, Integer> map = new HashMap<>();
        if (serialized == null || serialized.isEmpty() || serialized.equals("None")) {
            return map;
        }

        // Format: minecraft:sharpness:5,minecraft:unbreaking:3
        String[] parts = serialized.split(",");
        for (String part : parts) {
            try {
                String[] data = part.split(":");
                if (data.length < 2) {
                    continue;
                }
                
                String namespace = "minecraft";
                String key = data[0];
                int level = 1;

                if (data.length == 3) {
                    namespace = data[0];
                    key = data[1];
                    level = Integer.parseInt(data[2]);
                } else if (data.length == 2) {
                    // Try to guess if first part is namespace or key
                    if (data[1].matches("\\d+")) {
                        key = data[0];
                        level = Integer.parseInt(data[1]);
                    } else {
                        namespace = data[0];
                        key = data[1];
                    }
                }

                Registry<Enchantment> registry = getEnchantmentRegistry();
                Enchantment enchant = registry != null ? registry.get(NamespacedKey.fromString(namespace + ":" + key)) : null;
                if (enchant != null) {
                    map.put(enchant, level);
                }
            } catch (Exception e) {
                // Ignore malformed
            }
        }
        return map;
    }

    private String serializeEnchantments() {
        if (currentEnchants.isEmpty()) {
            return "None";
        }
        
        return currentEnchants.entrySet().stream()
                .map(entry -> entry.getKey().getKey().toString() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    @SuppressWarnings("deprecation")
    private Registry<Enchantment> getEnchantmentRegistry() {
        return Bukkit.getRegistry(Enchantment.class);
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        
        // 45 items per page
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, items.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(i - startIndex, createItem(items.get(i)));
        }
        
        addMenuBorder();
    }

    @Override
    public void addMenuBorder() {
        // Navigation buttons
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).name(MessageUtils.parse(lang.getRawMessage("item.gui.enchantment_selection.buttons.previous_page"))).build());
        }
        
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(MessageUtils.parse(lang.getRawMessage("item.gui.enchantment_selection.buttons.back"))).build());
        
        if ((page + 1) * 45 < items.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name(MessageUtils.parse(lang.getRawMessage("item.gui.enchantment_selection.buttons.next_page"))).build());
        }
    }

    @Override
    public ItemStack createItem(Enchantment enchant) {
        boolean hasEnchant = currentEnchants.containsKey(enchant);
        int level = currentEnchants.getOrDefault(enchant, 0);
        
        Material mat = hasEnchant ? Material.ENCHANTED_BOOK : Material.BOOK;
        ItemBuilder builder = new ItemBuilder(mat);
        
        // Name
        String name = enchant.getKey().getKey().toUpperCase().replace("_", " ");
        if (hasEnchant) {
            builder.name(MessageUtils.parse("<green>" + name));
            builder.enchant(enchant, level);
        } else {
            builder.name(MessageUtils.parse("<gray>" + name));
        }

        // Lore
        List<String> lore = new ArrayList<>();
        List<String> configLore = lang.getStringList("item.gui.enchantment_selection.item_lore");
        
        for (String line : configLore) {
            lore.add(line.replace("%level%", hasEnchant ? String.valueOf(level) : "0")
                        .replace("%id%", enchant.getKey().toString()));
        }
        
        if (hasEnchant) {
            lore.add("");
            lore.add(lang.getRawMessage("item.gui.enchantment_selection.active"));
        }
        
        builder.lore(lore.stream().map(MessageUtils::parse).collect(Collectors.toList()));
        
        return builder.build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        
        // Handle navigation
        if (slot == 45 && page > 0) {
            page--;
            initializeItems();
            return;
        } else if (slot == 53 && (page + 1) * 45 < items.size()) {
            page++;
            initializeItems();
            return;
        } else if (slot == 49) {
            parent.open();
            return;
        }

        if (slot >= 0 && slot < 45) {
            int index = page * 45 + slot;
            if (index >= items.size()) {
                return;
            }
            
            Enchantment clickedEnchant = items.get(index);
            
            if (event.isRightClick()) {
                // Remove
                if (currentEnchants.containsKey(clickedEnchant)) {
                    currentEnchants.remove(clickedEnchant);
                    item.setEnchantments(serializeEnchantments());
                    item.save();
                    player.sendMessage(MessageUtils.parse(lang.getRawMessage("item.gui.enchantment_selection.removed")
                            .replace("%s", clickedEnchant.getKey().getKey())));
                    initializeItems();
                }
            } else {
                // Add/Edit Level
                player.closeInventory();
                player.sendMessage(MessageUtils.parse(lang.getRawMessage("item.gui.enchantment_selection.prompt_level")
                        .replace("%s", clickedEnchant.getKey().getKey())));
                
                ChatInputListener.requestInput(player, (input) -> {
                    try {
                        int level = Integer.parseInt(input);
                        if (level < 1) {
                            level = 1;
                        }
                        if (level > 255) {
                            level = 255;
                        }
                        
                        currentEnchants.put(clickedEnchant, level);
                        item.setEnchantments(serializeEnchantments());
                        item.save();
                        
                        player.sendMessage(MessageUtils.parse(lang.getRawMessage("item.gui.enchantment_selection.added")
                                .replace("%s", clickedEnchant.getKey().getKey())
                                .replace("%d", String.valueOf(level))));
                        
                        this.open();
                    } catch (NumberFormatException e) {
                        player.sendMessage(MessageUtils.parse(lang.getRawMessage("item.gui.editor.invalid-number")));
                        this.open();
                    }
                });
            }
        }
    }
}
