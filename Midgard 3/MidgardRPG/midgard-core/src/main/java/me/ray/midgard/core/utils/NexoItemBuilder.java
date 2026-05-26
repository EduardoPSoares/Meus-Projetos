package me.ray.midgard.core.utils;

import me.ray.midgard.core.integration.NexoUtils;
import me.ray.midgard.core.text.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialized ItemBuilder for Nexo items with enhanced support for:
 * - Custom Nexo items
 * - Glyph-prefixed names and lore
 * - Smart fallback to vanilla materials
 * 
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Create from Nexo item
 * ItemStack item = NexoItemBuilder.of("custom_ruby")
 *     .name("<red>Ruby Gem")
 *     .glyphPrefix("gem_icon")  // Adds glyph before name
 *     .build();
 * 
 * // Smart creation (Nexo or vanilla)
 * ItemStack item = NexoItemBuilder.smart("DIAMOND")
 *     .name("<aqua>Shiny Diamond")
 *     .build();
 * 
 * // With glyph in lore
 * ItemStack item = NexoItemBuilder.of("custom_sword")
 *     .name("<gold>Legendary Sword")
 *     .loreWithGlyphs(
 *         ":attack_icon: <white>+50 Attack",
 *         ":defense_icon: <white>+20 Defense"
 *     )
 *     .build();
 * }</pre>
 */
public class NexoItemBuilder {
    
    private ItemStack item;
    private ItemMeta meta;
    private String glyphPrefix;
    private boolean parseGlyphsInLore = true;
    
    private NexoItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }
    
    private NexoItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = this.item.getItemMeta();
    }
    
    // ==================== STATIC FACTORY METHODS ====================
    
    /**
     * Creates a NexoItemBuilder from a Nexo custom item.
     * 
     * @param nexoId The Nexo item ID
     * @return NexoItemBuilder instance
     */
    public static NexoItemBuilder of(String nexoId) {
        return of(nexoId, Material.PAPER);
    }
    
    /**
     * Creates a NexoItemBuilder from a Nexo custom item with fallback.
     * 
     * @param nexoId The Nexo item ID
     * @param fallback Fallback material if Nexo item not found
     * @return NexoItemBuilder instance
     */
    public static NexoItemBuilder of(String nexoId, Material fallback) {
        if (nexoId == null || nexoId.isEmpty()) {
            return new NexoItemBuilder(fallback);
        }
        
        ItemStack nexoItem = NexoUtils.getCustomItem(nexoId);
        if (nexoItem != null) {
            return new NexoItemBuilder(nexoItem);
        }
        return new NexoItemBuilder(fallback);
    }
    
    /**
     * Creates a NexoItemBuilder from a vanilla material.
     * 
     * @param material The vanilla material
     * @return NexoItemBuilder instance
     */
    public static NexoItemBuilder material(Material material) {
        return new NexoItemBuilder(material);
    }
    
    /**
     * Smart creation - tries Nexo first, then vanilla material.
     * 
     * @param nexoIdOrMaterial Nexo ID or Material name
     * @return NexoItemBuilder instance
     */
    public static NexoItemBuilder smart(String nexoIdOrMaterial) {
        if (nexoIdOrMaterial == null || nexoIdOrMaterial.isEmpty()) {
            return new NexoItemBuilder(Material.PAPER);
        }
        
        // Try Nexo first
        if (NexoUtils.isAvailable()) {
            ItemStack nexoItem = NexoUtils.getCustomItem(nexoIdOrMaterial);
            if (nexoItem != null) {
                return new NexoItemBuilder(nexoItem);
            }
        }
        
        // Try vanilla material
        try {
            Material material = Material.valueOf(nexoIdOrMaterial.toUpperCase().replace(" ", "_"));
            return new NexoItemBuilder(material);
        } catch (IllegalArgumentException e) {
            return new NexoItemBuilder(Material.PAPER);
        }
    }
    
    /**
     * Creates from an existing ItemStack.
     * 
     * @param item The item to copy
     * @return NexoItemBuilder instance
     */
    public static NexoItemBuilder from(ItemStack item) {
        if (item == null) {
            return new NexoItemBuilder(Material.PAPER);
        }
        return new NexoItemBuilder(item);
    }
    
    // ==================== NEXO-SPECIFIC METHODS ====================
    
    /**
     * Sets a glyph to prefix the item name.
     * The glyph will appear before the display name.
     * 
     * @param glyphId The Nexo glyph ID
     * @return this builder
     */
    public NexoItemBuilder glyphPrefix(String glyphId) {
        this.glyphPrefix = glyphId;
        return this;
    }
    
    /**
     * Enables or disables automatic glyph parsing in lore.
     * When enabled, :glyph_id: and %glyph_glyph_id% placeholders are replaced.
     * 
     * @param parse Whether to parse glyphs
     * @return this builder
     */
    public NexoItemBuilder parseGlyphsInLore(boolean parse) {
        this.parseGlyphsInLore = parse;
        return this;
    }
    
    /**
     * Sets lore with automatic glyph parsing.
     * Supports :glyph_id: and %glyph_glyph_id% placeholders.
     * 
     * @param lines Lore lines with glyph placeholders
     * @return this builder
     */
    public NexoItemBuilder loreWithGlyphs(String... lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) {
            String parsed = NexoUtils.parseGlyphs(line);
            lore.add(MessageUtils.parse("<italic:false>" + parsed));
        }
        meta.lore(lore);
        return this;
    }
    
    /**
     * Sets lore with automatic glyph parsing.
     * 
     * @param lines Lore lines
     * @return this builder
     */
    public NexoItemBuilder loreWithGlyphs(List<String> lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) {
            String parsed = NexoUtils.parseGlyphs(line);
            lore.add(MessageUtils.parse("<italic:false>" + parsed));
        }
        meta.lore(lore);
        return this;
    }
    
    /**
     * Adds a lore line with a glyph prefix.
     * 
     * @param glyphId The glyph to prefix
     * @param text The text after the glyph
     * @return this builder
     */
    public NexoItemBuilder addLoreWithGlyph(String glyphId, String text) {
        List<Component> lore = meta.lore();
        if (lore == null) { lore = new ArrayList<>(); }
        
        String glyphChar = NexoUtils.getGlyphChar(glyphId);
        String line = (glyphChar != null ? glyphChar + " " : "") + text;
        lore.add(MessageUtils.parse("<italic:false>" + line));
        
        meta.lore(lore);
        return this;
    }
    
    /**
     * Sets the name with a glyph prefix.
     * 
     * @param glyphId The glyph to prefix
     * @param name The name after the glyph
     * @return this builder
     */
    public NexoItemBuilder nameWithGlyph(String glyphId, String name) {
        String glyphChar = NexoUtils.getGlyphChar(glyphId);
        String finalName = (glyphChar != null ? glyphChar + " " : "") + name;
        meta.displayName(MessageUtils.parse(finalName));
        return this;
    }
    
    // ==================== STANDARD ITEM BUILDER METHODS ====================
    
    /**
     * Sets the display name.
     * Glyph prefix will be applied if set.
     * 
     * @param name The display name (MiniMessage format)
     * @return this builder
     */
    public NexoItemBuilder name(String name) {
        String finalName = name;
        if (glyphPrefix != null) {
            String glyphChar = NexoUtils.getGlyphChar(glyphPrefix);
            if (glyphChar != null) {
                finalName = glyphChar + " " + name;
            }
        }
        meta.displayName(MessageUtils.parse(finalName));
        return this;
    }
    
    /**
     * Sets the display name as Component.
     * 
     * @param name The display name component
     * @return this builder
     */
    public NexoItemBuilder name(Component name) {
        if (glyphPrefix != null) {
            Component glyphComponent = NexoUtils.getGlyphComponent(glyphPrefix);
            if (glyphComponent != null) {
                meta.displayName(glyphComponent.append(Component.text(" ")).append(name));
                return this;
            }
        }
        meta.displayName(name);
        return this;
    }
    
    /**
     * Sets the lore.
     * 
     * @param lines Lore lines (MiniMessage format)
     * @return this builder
     */
    public NexoItemBuilder lore(String... lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) {
            String parsed = parseGlyphsInLore ? NexoUtils.parseGlyphs(line) : line;
            lore.add(MessageUtils.parse("<italic:false>" + parsed));
        }
        meta.lore(lore);
        return this;
    }
    
    /**
     * Sets the lore from a list.
     * 
     * @param lines Lore lines
     * @return this builder
     */
    public NexoItemBuilder lore(List<String> lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) {
            String parsed = parseGlyphsInLore ? NexoUtils.parseGlyphs(line) : line;
            lore.add(MessageUtils.parse("<italic:false>" + parsed));
        }
        meta.lore(lore);
        return this;
    }
    
    /**
     * Adds a lore line.
     * 
     * @param line The line to add
     * @return this builder
     */
    public NexoItemBuilder addLore(String line) {
        List<Component> lore = meta.lore();
        if (lore == null) { lore = new ArrayList<>(); }
        
        String parsed = parseGlyphsInLore ? NexoUtils.parseGlyphs(line) : line;
        lore.add(MessageUtils.parse("<italic:false>" + parsed));
        meta.lore(lore);
        return this;
    }
    
    /**
     * Adds an empty lore line.
     * 
     * @return this builder
     */
    public NexoItemBuilder addEmptyLore() {
        List<Component> lore = meta.lore();
        if (lore == null) { lore = new ArrayList<>(); }
        lore.add(Component.empty());
        meta.lore(lore);
        return this;
    }
    
    /**
     * Sets the amount.
     * 
     * @param amount Item amount
     * @return this builder
     */
    public NexoItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }
    
    /**
     * Adds glow effect (hidden enchantment).
     * 
     * @return this builder
     */
    public NexoItemBuilder glow() {
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }
    
    /**
     * Adds glow effect if condition is true.
     * 
     * @param condition Whether to add glow
     * @return this builder
     */
    public NexoItemBuilder glowIf(boolean condition) {
        if (condition) { glow(); }
        return this;
    }
    
    /**
     * Adds item flags.
     * 
     * @param flags Flags to add
     * @return this builder
     */
    public NexoItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }
    
    /**
     * Hides all item attributes and flags.
     * 
     * @return this builder
     */
    public NexoItemBuilder hideAll() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }
    
    /**
     * Sets custom model data.
     * 
     * @param data Custom model data value
     * @return this builder
     */
    @SuppressWarnings("deprecation")
    public NexoItemBuilder customModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }
    
    /**
     * Sets the item as unbreakable.
     * 
     * @param unbreakable Whether unbreakable
     * @return this builder
     */
    public NexoItemBuilder unbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }
    
    /**
     * Checks if the player has permission for this item's glyphs.
     * 
     * @param player The player to check
     * @return true if player has permission for all glyphs used
     */
    public boolean hasGlyphPermission(Player player) {
        if (glyphPrefix == null) { return true; }
        return NexoUtils.hasGlyphPermission(player, glyphPrefix);
    }
    
    /**
     * Builds the final ItemStack.
     * 
     * @return The built ItemStack
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Builds and returns a copy.
     * 
     * @return A cloned ItemStack
     */
    public ItemStack buildCopy() {
        item.setItemMeta(meta);
        return item.clone();
    }
}
