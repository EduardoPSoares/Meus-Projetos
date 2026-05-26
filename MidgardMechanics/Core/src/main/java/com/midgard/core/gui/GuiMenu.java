package com.midgard.core.gui;

import com.midgard.core.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a custom GUI menu.
 */
public abstract class GuiMenu {

    private final String title;
    private final int rows;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickActions = new HashMap<>();
    private Consumer<InventoryClickEvent> globalClickAction;
    private Consumer<InventoryCloseEvent> closeAction;
    private boolean cancelClicks = true;

    protected Inventory inventory;

    protected GuiMenu(String title, int rows) {
        this.title = title;
        this.rows = Math.min(Math.max(rows, 1), 6);
    }

    public abstract void setup(Player player);

    public void open(Player player) {
        clickActions.clear();
        inventory = Bukkit.createInventory(null, rows * 9,
                MessageUtils.toComponent(title));
        setup(player);
        player.openInventory(inventory);
        GuiManager.registerMenu(player, this);
    }

    // --- Item Placement ---

    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        clickActions.put(slot, action);
    }

    public void fillBorder(ItemStack item) {
        int size = rows * 9;
        for (int i = 0; i < 9; i++) setItem(i, item);
        for (int i = size - 9; i < size; i++) setItem(i, item);
        for (int i = 9; i < size - 9; i += 9) {
            setItem(i, item);
            setItem(i + 8, item);
        }
    }

    public void fill(ItemStack item) {
        for (int i = 0; i < rows * 9; i++) {
            if (inventory.getItem(i) == null) {
                setItem(i, item);
            }
        }
    }

    // --- Event Handling ---

    public void handleClick(InventoryClickEvent event) {
        if (cancelClicks) event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= rows * 9) return;

        if (globalClickAction != null) {
            globalClickAction.accept(event);
        }

        Consumer<InventoryClickEvent> action = clickActions.get(slot);
        if (action != null) {
            action.accept(event);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        if (closeAction != null) {
            closeAction.accept(event);
        }
        if (event.getPlayer() instanceof Player player) {
            GuiManager.unregisterMenu(player);
        }
    }

    // --- Setters ---

    public void setGlobalClickAction(Consumer<InventoryClickEvent> action) {
        this.globalClickAction = action;
    }

    public void setCloseAction(Consumer<InventoryCloseEvent> action) {
        this.closeAction = action;
    }

    public void setCancelClicks(boolean cancelClicks) {
        this.cancelClicks = cancelClicks;
    }

    // --- Getters ---

    public String getTitle() {
        return title;
    }

    public int getRows() {
        return rows;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
