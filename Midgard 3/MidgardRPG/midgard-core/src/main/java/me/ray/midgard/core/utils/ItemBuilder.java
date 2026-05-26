package me.ray.midgard.core.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        if (material == null || material == Material.AIR) {
            throw new IllegalArgumentException("Cannot build items for AIR or null material");
        }
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }
    
    // ==================== NEXO INTEGRATION ====================
    
    /**
     * Creates an ItemBuilder from a Nexo custom item.
     * Falls back to the specified material if Nexo is not available or item not found.
     * 
     * @param nexoId The Nexo item ID (e.g., "ruby", "custom_sword")
     * @param fallbackMaterial The fallback material if Nexo item is not found
     * @return ItemBuilder instance
     */
    public static ItemBuilder fromNexo(String nexoId, Material fallbackMaterial) {
        ItemStack nexoItem = me.ray.midgard.core.integration.NexoUtils.getCustomItem(nexoId);
        if (nexoItem != null) {
            return new ItemBuilder(nexoItem);
        }
        return new ItemBuilder(fallbackMaterial);
    }
    
    /**
     * Creates an ItemBuilder from a Nexo custom item.
     * Falls back to PAPER if Nexo is not available or item not found.
     * 
     * @param nexoId The Nexo item ID
     * @return ItemBuilder instance
     */
    public static ItemBuilder fromNexo(String nexoId) {
        return fromNexo(nexoId, Material.PAPER);
    }
    
    /**
     * Checks if a Nexo item exists with the given ID.
     * 
     * @param nexoId The Nexo item ID to check
     * @return true if the Nexo item exists
     */
    public static boolean nexoItemExists(String nexoId) {
        return me.ray.midgard.core.integration.NexoUtils.isAvailable() 
            && me.ray.midgard.core.integration.NexoUtils.getCustomItem(nexoId) != null;
    }
    
    /**
     * Creates an ItemBuilder, trying Nexo first, then vanilla material.
     * Useful for items that may or may not be custom.
     * 
     * @param nexoIdOrMaterial Either a Nexo item ID or a Material name
     * @return ItemBuilder instance
     */
    public static ItemBuilder smart(String nexoIdOrMaterial) {
        // Try Nexo first
        if (me.ray.midgard.core.integration.NexoUtils.isAvailable()) {
            ItemStack nexoItem = me.ray.midgard.core.integration.NexoUtils.getCustomItem(nexoIdOrMaterial);
            if (nexoItem != null) {
                return new ItemBuilder(nexoItem);
            }
        }
        
        // Try vanilla material
        try {
            Material material = Material.valueOf(nexoIdOrMaterial.toUpperCase());
            return new ItemBuilder(material);
        } catch (IllegalArgumentException e) {
            // Not a valid material, return paper as fallback
            return new ItemBuilder(Material.PAPER);
        }
    }
    
    // ==================== END NEXO INTEGRATION ====================

    public ItemBuilder name(Component name) {
        meta.displayName(name);
        return this;
    }

    public ItemBuilder lore(Component... lore) {
        meta.lore(Arrays.asList(lore));
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    public ItemBuilder lore(java.util.Collection<String> lore) {
        List<Component> components = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                components.add(me.ray.midgard.core.text.MessageUtils.parse(line));
            }
        }
        meta.lore(components);
        return this;
    }

    public ItemBuilder setName(String name) {
        meta.displayName(me.ray.midgard.core.text.MessageUtils.parse(name));
        return this;
    }

    public ItemBuilder addLore(String line) {
        List<Component> lore = meta.lore();
        if (lore == null) { lore = new ArrayList<>(); }
        lore.add(me.ray.midgard.core.text.MessageUtils.parse(line));
        meta.lore(lore);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }
    
    @SuppressWarnings("deprecation")
    public ItemBuilder texture(String texture) {
        if (texture == null || texture.isEmpty()) { return this; }
        if (meta instanceof SkullMeta skullMeta) {
            if (texture.length() > 20) {
                // Base64 texture - extract URL and apply via PlayerProfile
                try {
                    String decoded = new String(java.util.Base64.getDecoder().decode(texture));
                    int urlStart = decoded.indexOf("url\" : \"");
                    if (urlStart == -1) { urlStart = decoded.indexOf("url\": \""); }
                    if (urlStart == -1) { urlStart = decoded.indexOf("url\":\""); }
                    
                    if (urlStart != -1) {
                        urlStart = decoded.indexOf("\"", urlStart + 4) + 1;
                        int urlEnd = decoded.indexOf("\"", urlStart);
                        String url = decoded.substring(urlStart, urlEnd);
                        
                        org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(java.util.UUID.randomUUID(), "MidgardSkull");
                        org.bukkit.profile.PlayerTextures textures = profile.getTextures();
                        textures.setSkin(java.net.URI.create(url).toURL());
                        profile.setTextures(textures);
                        skullMeta.setOwnerProfile(profile);
                    }
                } catch (Exception e) {
                    me.ray.midgard.core.debug.MidgardLogger.warn("Failed to apply skull texture: " + e.getMessage());
                }
            } else {
                // Player name
                skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(texture));
            }
            item.setItemMeta(skullMeta);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    @SuppressWarnings("deprecation")
    public ItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder glowIf(boolean condition) {
        if (condition) {
            return glow();
        }
        return this;
    }

    public ItemBuilder color(Color color) {
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(color);
        }
        return this;
    }

    public ItemBuilder skullOwner(OfflinePlayer player) {
        if (meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwningPlayer(player);
        }
        return this;
    }



    public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        meta.getPersistentDataContainer().set(key, type, value);
        return this;
    }

    public ItemBuilder editMeta(Consumer<ItemMeta> metaConsumer) {
        metaConsumer.accept(meta);
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder maxStackSize(int maxStackSize) {
        // Only available in Paper 1.20.5+ / 1.21+
        // Using try-catch/reflection or direct call if we are sure about environment
        try {
            meta.setMaxStackSize(maxStackSize);
        } catch (NoSuchMethodError e) {
            // Fallback or ignore if running on older version
            // me.ray.midgard.core.debug.MidgardLogger.warn("setMaxStackSize not supported on this server version.");
        }
        return this;
    }

    public ItemBuilder addLoreLine(Component line) {
        List<Component> currentLore = meta.lore();
        if (currentLore == null) {
            currentLore = new ArrayList<>();
        }
        currentLore.add(line);
        meta.lore(currentLore);
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        return addLoreLine(me.ray.midgard.core.text.MessageUtils.parse(line));
    }

    /**
     * Adiciona lore multi-linha suportando formato com \n
     * Espaços vazios são preservados
     */
    public ItemBuilder setLoreMultiline(String multilineText) {
        if (multilineText == null || multilineText.isEmpty()) { return this; }
        
        List<Component> lore = meta.lore();
        if (lore == null) { lore = new ArrayList<>(); }
        
        String[] lines = multilineText.split("\n");
        for (String line : lines) {
            if (line.isEmpty()) {
                lore.add(Component.empty());
            } else {
                lore.add(me.ray.midgard.core.text.MessageUtils.parse("<italic:false>" + line));
            }
        }
        
        meta.lore(lore);
        return this;
    }

    public ItemBuilder replaceLorePlaceholder(String placeholder, String replacement) {
        List<Component> currentLore = meta.lore();
        if (currentLore == null) { return this; }
        
        List<Component> newLore = new ArrayList<>();
        for (Component line : currentLore) {
            String serialized = me.ray.midgard.core.text.MessageUtils.serialize(line);
            serialized = serialized.replace(placeholder, replacement);
            newLore.add(me.ray.midgard.core.text.MessageUtils.parse(serialized));
        }
        meta.lore(newLore);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
