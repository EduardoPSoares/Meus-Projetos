package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.listener.ChatInputListener;
import me.ray.midgard.modules.item.model.MidgardItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PotionEffectSelectionGui extends PaginatedGui<PotionEffectType> {

    private final MidgardItem item;
    private final ItemEditionGui parent;

    public PotionEffectSelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.potion_effect_selection.title"), new ArrayList<>());
        this.item = item;
        this.parent = parent;
        
        // Load all potion effects
        List<PotionEffectType> effects = new ArrayList<>();
        Registry<PotionEffectType> registry = getPotionEffectRegistry();
        if (registry != null) {
            for (PotionEffectType type : registry) {
                if (type != null) {
                    effects.add(type);
                }
            }
        }
        effects.sort(Comparator.comparing(this::getEffectKey));
        this.items = effects;
    }

    private int getEffectLevel(PotionEffectType type) {
        for (String effectStr : item.getPermanentEffects()) {
            String[] parts = effectStr.split(":");
            if (parts.length >= 1 && parts[0].equalsIgnoreCase(getEffectKey(type))) {
                if (parts.length >= 2) {
                    try {
                        return Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
                return 0;
            }
        }
        return -1; // Not present
    }

    private void updateEffect(PotionEffectType type, int level) {
        List<String> effects = new ArrayList<>(item.getPermanentEffects());
        
        // Remove existing if present
        effects.removeIf(s -> s.split(":")[0].equalsIgnoreCase(getEffectKey(type)));
        
        if (level >= 0) {
            effects.add(getEffectKey(type) + ":" + level);
        }
        
        item.setPermanentEffects(effects);
        item.save();
    }

    @Override
    public ItemStack createItem(PotionEffectType type) {
        int currentLevel = getEffectLevel(type);
        boolean active = currentLevel >= 0;
        
        Material mat = active ? Material.POTION : Material.GLASS_BOTTLE;
        
        ItemBuilder builder = new ItemBuilder(mat)
                .name(MidgardCore.getLanguageManager().getMessage("item.gui.potion_effect_selection.item.name", "%s", getEffectKey(type)));

        if (active) {
            builder.enchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            builder.flags(org.bukkit.inventory.ItemFlag.values());
            
            List<String> loreLines = MidgardCore.getLanguageManager().getStringList("item.gui.potion_effect_selection.item.lore");
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageUtils.parse(line.replace("%level%", String.valueOf(currentLevel))));
            }
            builder.lore(lore);
        } else {
            builder.lore(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.potion_effect_selection.item.click_to_add")));
        }
        
        return builder.build();
    }

    private String getEffectKey(PotionEffectType type) {
        return type.getKey().getKey();
    }

    @SuppressWarnings("deprecation")
    private Registry<PotionEffectType> getPotionEffectRegistry() {
        return Bukkit.getRegistry(PotionEffectType.class);
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

        // Handle item click
        if (slot < 45) {
            int index = page * 45 + slot;
            if (index < items.size()) {
                PotionEffectType type = items.get(index);
                int currentLevel = getEffectLevel(type);
                
                if (event.isRightClick()) {
                    if (currentLevel >= 0) {
                        updateEffect(type, -1); // Remove
                        player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.potion_effect_selection.messages.removed", "%s", getEffectKey(type)));
                        initializeItems();
                    }
                } else {
                    // Edit/Add
                    player.closeInventory();
                    player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.potion_effect_selection.messages.prompt_level"));
                    
                    ChatInputListener.requestInput(player, (input) -> {
                        try {
                            int level = Integer.parseInt(input);
                            if (level < 0) {
                                level = 0;
                            }
                            
                            updateEffect(type, level);
                            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.potion_effect_selection.messages.added", "%s", getEffectKey(type)));
                            this.open();
                        } catch (NumberFormatException e) {
                            player.sendMessage(MidgardCore.getLanguageManager().getMessage("item.gui.editor.invalid-number"));
                            this.open();
                        }
                    });
                }
            }
        }
    }

    @Override
    public void addMenuBorder() {
        if (page > 0) {
            inventory.setItem(45, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.gui.potion_effect_selection.buttons.previous_page")).build());
        }
        
        if ((page + 1) * 45 < items.size()) {
            inventory.setItem(53, new ItemBuilder(Material.ARROW).name(MidgardCore.getLanguageManager().getMessage("item.gui.potion_effect_selection.buttons.next_page")).build());
        }

        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name(MidgardCore.getLanguageManager().getMessage("item.gui.potion_effect_selection.buttons.back")).build());
    }
    
    @Override
    public void initializeItems() {
        inventory.clear();
        addMenuBorder();
        
        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, items.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(i - startIndex, createItem(items.get(i)));
        }
    }
}
