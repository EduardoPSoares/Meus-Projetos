package me.ray.midgard.core.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.integration.NexoUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public abstract class BaseGui implements InventoryHolder {

    protected Inventory inventory;
    protected Player player;

    /**
     * Creates a GUI with a simple text title.
     */
    public BaseGui(Player player, int rows, String title) {
        this(player, rows, title, null);
    }
    
    /**
     * Creates a GUI with a Nexo glyph background and text overlay.
     * 
     * @param player The player to show the GUI to
     * @param rows Number of rows (1-6)
     * @param title The text title to display
     * @param nexoGlyphId The Nexo glyph ID for the menu background (null for no glyph)
     */
    public BaseGui(Player player, int rows, String title, String nexoGlyphId) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null");
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("Rows must be between 1 and 6");

        this.player = player;
        Component titleComponent = createTitle(title, nexoGlyphId);
        
        try {
            this.inventory = Bukkit.createInventory(this, rows * 9, titleComponent);
        } catch (Exception e) {
             MidgardCore.getInstance().getLogger().severe("Failed to create inventory for " + this.getClass().getSimpleName());
             this.inventory = Bukkit.createInventory(this, 9, Component.text("Error")); // Fallback
        }
    }
    
    /**
     * Creates a GUI with a pre-built Component title.
     * Useful when you want full control over the title formatting.
     */
    public BaseGui(Player player, int rows, Component title) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null");
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("Rows must be between 1 and 6");

        this.player = player;
        
        try {
            this.inventory = Bukkit.createInventory(this, rows * 9, title);
        } catch (Exception e) {
             MidgardCore.getInstance().getLogger().severe("Failed to create inventory for " + this.getClass().getSimpleName());
             this.inventory = Bukkit.createInventory(this, 9, Component.text("Error"));
        }
    }
    
    /**
     * Creates the title component, optionally with a Nexo glyph background.
     */
    private Component createTitle(String title, String nexoGlyphId) {
        String finalTitle = title != null ? title : "Interface";
        
        // Handle language key lookup
        try {
            if (title != null && title.startsWith("key:")) {
                String key = title.substring(4);
                if (MidgardCore.getLanguageManager() != null) {
                    finalTitle = MidgardCore.getLanguageManager().getRawMessage(key);
                } else {
                    finalTitle = key;
                }
            }
        } catch (Exception e) {
            MidgardCore.getInstance().getLogger().warning("Error parsing title key: " + title);
            finalTitle = "Error";
        }
        
        // If a Nexo glyph is specified and Nexo is available, create menu with glyph background
        if (nexoGlyphId != null && !nexoGlyphId.isEmpty() && NexoUtils.isAvailable()) {
            String glyphTitle = NexoUtils.createMenuTitle(nexoGlyphId, finalTitle);
            if (glyphTitle != null && !glyphTitle.isEmpty()) {
                return me.ray.midgard.core.text.MessageUtils.parse(glyphTitle);
            }
        }
        
        // Parse title with possible inline glyph placeholders like :glyph_id: or %glyph_glyph_id%
        String parsedTitle = NexoUtils.parseGlyphs(finalTitle);
        return me.ray.midgard.core.text.MessageUtils.parse(parsedTitle);
    }
    
    /**
     * Creates a menu title with only a Nexo glyph (no text).
     * Useful for fully custom GUI backgrounds.
     * 
     * @param nexoGlyphId The Nexo glyph ID
     * @return Component with only the glyph
     */
    protected static Component createGlyphOnlyTitle(String nexoGlyphId) {
        if (NexoUtils.isAvailable()) {
            String glyphChar = NexoUtils.getGlyphChar(nexoGlyphId);
            if (glyphChar != null && !glyphChar.isEmpty()) {
                return me.ray.midgard.core.text.MessageUtils.parse(glyphChar);
            }
        }
        return Component.text("Menu");
    }

    public abstract void initializeItems();

    public void open() {
        initializeItems();
        player.openInventory(inventory);
    }
    
    /**
     * @deprecated Use open() instead.
     */
    @Deprecated
    public void open(Player player) {
        if (this.player != null && !this.player.equals(player)) {
            // Warn or handle mismatch?
        }
        open();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // Event hooks that implementing classes can override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true); // Default behavior: prevent taking items
    }

    public void onDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    public void onOpen(InventoryOpenEvent event) {}

    public void onClose(InventoryCloseEvent event) {}
}
