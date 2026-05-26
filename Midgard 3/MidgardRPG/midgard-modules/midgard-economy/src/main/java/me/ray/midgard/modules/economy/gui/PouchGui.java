package me.ray.midgard.modules.economy.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.economy.EconomyModule;
import me.ray.midgard.modules.economy.manager.PouchManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;



public class PouchGui extends BaseGui {

    private final EconomyModule module;
    private final String sessionId;
    // We don't store ItemStack pouch permanently because it might change/move. 
    // We find it by sessionId when needed.

    public PouchGui(Player player, ItemStack pouch, String sessionId) {
        // Size is dynamic based on pouch tier
        super(player,
                EconomyModule.getInstance().getPouchManager().getInventorySize(pouch) / 9,
                EconomyModule.getInstance().getMessage("pouch.gui.title"));
        this.module = EconomyModule.getInstance();
        this.sessionId = sessionId;
        // Register session as open
        this.module.getPouchManager().openSession(sessionId);
    }

    @Override
    public void open() {
        if (findPouch() == null) {
            MessageUtils.send(player, module.getMessage("pouch.error.not_found"));
            return;
        }
        super.open();
    }

    @Override
    public void initializeItems() {
        ItemStack pouch = findPouch();
        if (pouch == null) {
            // Should be caught by open(), but double check
            player.closeInventory();
            return;
        }

        PouchManager pm = module.getPouchManager();
        ItemStack[] contents = pm.getContents(pouch);
        
        // Fill inventory
        for (int i = 0; i < contents.length; i++) {
            if (i >= inventory.getSize()) {
                break;
            }
            if (contents[i] != null) {
                inventory.setItem(i, contents[i]);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        // Allow interaction but filter items
        
        // Security: Prevent moving the pouch itself
        if (isInteractingWithPouch(event)) {
            event.setCancelled(true);
            return;
        }

        // Filter: Only allow emeralds into the TOP inventory
        if (event.getClickedInventory() == inventory || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            
            ItemStack cursor = event.getCursor();
            ItemStack current = event.getCurrentItem();
            
            // If putting item into GUI (Cursor is not empty and clicked top inventory)
            if (event.getClickedInventory() == inventory && cursor != null && cursor.getType() != Material.AIR) {
                if (!isAllowedItem(cursor)) {
                    // Specific message for Pouch inside Pouch
                    if (module.getPouchManager().isPouch(cursor)) {
                        MessageUtils.send(player, module.getMessage("pouch.error.inception"));
                    }
                    event.setCancelled(true);
                    return;
                }
                
                // Value Capacity Check for Place/Swap
                if (!checkCapacityForPlace(event)) {
                    event.setCancelled(true);
                    MessageUtils.send(player, module.getMessage("pouch.error.capacity_exceeded"));
                    return;
                }
            }
            
            // If Shift-Clicking from Bottom to Top
            if (event.getClickedInventory() != inventory && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (current != null && !isAllowedItem(current)) {
                    // Specific message for Pouch inside Pouch
                    if (module.getPouchManager().isPouch(current)) {
                        MessageUtils.send(player, module.getMessage("pouch.error.inception"));
                    }
                    event.setCancelled(true);
                    return;
                }
                
                // Value Capacity Check for Shift-Click
                if (!checkCapacityForShift(event)) {
                    event.setCancelled(true);
                    MessageUtils.send(player, module.getMessage("pouch.error.capacity_exceeded"));
                    return;
                }
            }
        }
        
        // Handle Hotbar Swap (Number Key)
        if (event.getClick() == ClickType.NUMBER_KEY && event.getClickedInventory() == inventory) {
             ItemStack swappedIn = player.getInventory().getItem(event.getHotbarButton());
             if (swappedIn != null && swappedIn.getType() != Material.AIR) {
                 if (!isAllowedItem(swappedIn)) {
                     if (module.getPouchManager().isPouch(swappedIn)) {
                         MessageUtils.send(player, module.getMessage("pouch.error.inception"));
                     }
                     event.setCancelled(true);
                     return;
                 }
                 
                 // Capacity Check for Hotbar Swap
                 if (!checkCapacityForHotbarSwap(event)) {
                     event.setCancelled(true);
                     MessageUtils.send(player, module.getMessage("pouch.error.capacity_exceeded"));
                     return;
                 }
             }
        }
    }
    
    @Override
    public void onDrag(InventoryDragEvent event) {
        // Prevent dragging invalid items into top inventory
        boolean involvesTop = event.getRawSlots().stream().anyMatch(slot -> slot < inventory.getSize());
        if (involvesTop) {
            ItemStack dragged = event.getOldCursor();
            if (!isAllowedItem(dragged)) {
                // Specific message for Pouch inside Pouch
                if (module.getPouchManager().isPouch(dragged)) {
                    MessageUtils.send(player, module.getMessage("pouch.error.inception"));
                }
                event.setCancelled(true);
                return;
            }
            
            // Value Capacity Check for Drag
            // This is complex to calculate per-slot distribution, so we do a simple global check
            // assuming all dragged items go in.
            if (!checkCapacityForDrag(event)) {
                event.setCancelled(true);
                MessageUtils.send(player, module.getMessage("pouch.error.capacity_exceeded"));
            }
        }
    }

    private boolean checkCapacityForPlace(InventoryClickEvent event) {
        ItemStack pouch = findPouch();
        if (pouch == null) {
            return false;
        }
        
        PouchManager pm = module.getPouchManager();
        int capacity = pm.getCapacity(pouch);
        
        // Current balance in GUI (not yet saved to pouch, so calculate from GUI contents)
        int currentBalance = calculateGuiBalance();
        
        // Item being added
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR) {
            return true; // Taking item out or nothing
        }
        
        // If clicking on existing item (Swap or Add)
        ItemStack clicked = event.getCurrentItem();
        int clickedValue = (clicked != null) ? pm.getItemValue(clicked) : 0;
        
        // Net change: +CursorValue - ClickedValue (if we swap/pickup)
        // Note: Standard click puts cursor, picks up current.
        // Right click adds 1.
        
        int change = 0;
        if (event.isLeftClick()) {
            change = pm.getItemValue(cursor) - clickedValue;
            // Left click on similar item: Merges.
            // Left click on different item: Swaps.
            // Left click on empty: Places.
            // All cases are covered by: +Cursor - Clicked.
            // (If merge, clicked remains but we add cursor amount. Wait.
            // If merge: Gui balance increases by cursor value. 
            // My formula: +Cursor - Clicked.
            // Example: Slot=10, Cursor=5. Merge. Result=15.
            // Formula: +5 - 10 = -5. WRONG.
            
            if (clicked != null && clicked.isSimilar(cursor)) {
                 // Merging
                 // We add cursor amount (up to max stack).
                 int space = clicked.getMaxStackSize() - clicked.getAmount();
                 int toAdd = Math.min(cursor.getAmount(), space);
                 change = (pm.getItemValue(cursor) / cursor.getAmount()) * toAdd;
            } else {
                 // Swapping or Placing
                 change = pm.getItemValue(cursor) - clickedValue;
            }
        } else if (event.isRightClick()) {
            // Right click logic
            if (clicked != null && !clicked.isSimilar(cursor) && cursor.getType() != Material.AIR) {
                // Different items: SWAP stacks
                change = pm.getItemValue(cursor) - clickedValue;
            } else {
                // Same items or clicking empty: Place ONE
                if (cursor.getAmount() > 0) {
                     change = pm.getItemValue(cursor) / cursor.getAmount(); // Value of 1 item
                }
            }
        }
        
        return (currentBalance + change) <= capacity;
    }

    private boolean checkCapacityForShift(InventoryClickEvent event) {
        ItemStack pouch = findPouch();
        if (pouch == null) {
            return false;
        }
        
        PouchManager pm = module.getPouchManager();
        int capacity = pm.getCapacity(pouch);
        int currentBalance = calculateGuiBalance();
        
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return true;
        }
        
        // Shift click adds the whole item (or as much as fits physically)
        // We check if adding the WHOLE item would exceed value limit.
        // If it fits partially physically, it might still exceed value limit.
        // But for simplicity, we check full item value.
        // Refinement: We could calculate exactly how much fits physically and check value of that.
        // But preventing the action if it would exceed is safer.
        
        int valueToAdd = pm.getItemValue(item);
        return (currentBalance + valueToAdd) <= capacity;
    }
    
    private boolean checkCapacityForHotbarSwap(InventoryClickEvent event) {
        ItemStack pouch = findPouch();
        if (pouch == null) {
            return false;
        }
        
        PouchManager pm = module.getPouchManager();
        int capacity = pm.getCapacity(pouch);
        int currentBalance = calculateGuiBalance();
        
        ItemStack swappedIn = player.getInventory().getItem(event.getHotbarButton());
        ItemStack swappedOut = event.getCurrentItem();
        
        int inValue = (swappedIn != null) ? pm.getItemValue(swappedIn) : 0;
        int outValue = (swappedOut != null) ? pm.getItemValue(swappedOut) : 0;
        
        return (currentBalance - outValue + inValue) <= capacity;
    }
    
    private boolean checkCapacityForDrag(InventoryDragEvent event) {
        ItemStack pouch = findPouch();
        if (pouch == null) {
            return false;
        }
        
        PouchManager pm = module.getPouchManager();
        int capacity = pm.getCapacity(pouch);
        int currentBalance = calculateGuiBalance();
        
        // Drag adds items to slots.
        // We calculate total value added.
        ItemStack dragged = event.getOldCursor(); // The item being dragged
        if (dragged == null) {
            return true;
        }
        
        // Logic: New cursor will be OldCursor - Distributed.
        // But we want to know what is ADDED to inventory.
        // The event has newItems map.
        
        int valueAdded = 0;
        for (int slot : event.getRawSlots()) {
            if (slot < inventory.getSize()) { // Only top inventory
                ItemStack newStack = event.getNewItems().get(slot);
                ItemStack oldStack = inventory.getItem(slot);
                
                int newVal = (newStack != null) ? pm.getItemValue(newStack) : 0;
                int oldVal = (oldStack != null) ? pm.getItemValue(oldStack) : 0;
                
                valueAdded += (newVal - oldVal);
            }
        }
        
        return (currentBalance + valueAdded) <= capacity;
    }

    private int calculateGuiBalance() {
        PouchManager pm = module.getPouchManager();
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                total += pm.getItemValue(item);
            }
        }
        return total;
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        ItemStack pouch = findPouch();
        if (pouch == null) {
            // Unregister session since we can't save
            module.getPouchManager().closeSession(sessionId);
            MessageUtils.send(player, module.getMessage("pouch.error.not_found_save"));
            // Drop items to ground to prevent loss
            for (ItemStack item : inventory.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    player.getWorld().dropItem(player.getLocation(), item);
                }
            }
            return;
        }

        PouchManager pm = module.getPouchManager();
        pm.setContents(pouch, inventory.getContents());
        // Unregister session AFTER saving to prevent race with pickup listener
        pm.closeSession(sessionId);
        player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, 1f, 1f);
    }

    private boolean isAllowedItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true;
        }
        
        // Prevent Pouch Inception (Infinite NBT Nesting)
        if (module.getPouchManager().isPouch(item)) {
            return false;
        }
        
        var cm = module.getCurrencyManager();
        if (cm == null) {
            return false;
        }
        return cm.getValue(item) > 0;
    }

    private boolean isInteractingWithPouch(InventoryClickEvent event) {
        // Check if the item being moved/clicked is the pouch itself
        
        // Current Item (Clicked)
        if (isPouch(event.getCurrentItem())) {
            return true;
        }
        
        // Cursor Item (Holding)
        if (isPouch(event.getCursor())) {
            return true;
        }
        
        // Hotbar Swap (Number keys)
        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (isPouch(hotbarItem)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isPouch(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        PouchManager pm = module.getPouchManager();
        if (!pm.isPouch(item)) {
            return false;
        }
        
        String id = pm.getSessionId(item);
        return id != null && id.equals(this.sessionId);
    }

    private ItemStack findPouch() {
        // Check Hands first
        ItemStack main = player.getInventory().getItemInMainHand();
        if (checkId(main)) {
            return main;
        }
        
        ItemStack off = player.getInventory().getItemInOffHand();
        if (checkId(off)) {
            return off;
        }
        
        // Check full inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (checkId(item)) {
                return item;
            }
        }
        
        return null;
    }
    
    private boolean checkId(ItemStack item) {
        PouchManager pm = module.getPouchManager();
        if (!pm.isPouch(item)) {
            return false;
        }
        String id = pm.getSessionId(item);
        return id != null && id.equals(this.sessionId);
    }
}
