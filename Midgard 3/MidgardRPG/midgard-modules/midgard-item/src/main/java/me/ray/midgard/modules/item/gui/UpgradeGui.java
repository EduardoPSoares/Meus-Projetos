package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.gui.GuiUtils;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.manager.UpgradeManager;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.utils.ItemPDC;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import me.ray.midgard.core.utils.Task;

public class UpgradeGui extends BaseGui {

    private final int ITEM_SLOT = 10;
    private final int MATERIAL_SLOT = 16;
    private final int INFO_SLOT = 13;
    private final int CONFIRM_SLOT = 22;

    public UpgradeGui(Player player) {
        super(player, 3, msg("title"));
    }

    @Override
    public void initializeItems() {
        // Fill background
        GuiUtils.fillBorder(inventory, GuiUtils.createBorderItem());
        
        // Setup static items
        updateState();
    }
    
    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        // Allow interaction with Player Inventory
        if (slot >= inventory.getSize()) {
            // Allow move to Item or Material slot?
            // If shift-click, we need to handle it.
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack current = event.getCurrentItem();
                if (current != null && current.getType() != Material.AIR) {
                    // Try to move to Item Slot first
                    if (inventory.getItem(ITEM_SLOT) == null) {
                         inventory.setItem(ITEM_SLOT, current.clone());
                         event.setCurrentItem(null);
                         updateState();
                    } else if (inventory.getItem(MATERIAL_SLOT) == null) {
                         inventory.setItem(MATERIAL_SLOT, current.clone());
                         event.setCurrentItem(null);
                         updateState();
                    }
                }
            }
            return; // Allow normal clicks in player inventory
        }

        // Allow interaction with input slots
        if (slot == ITEM_SLOT || slot == MATERIAL_SLOT) {
            event.setCancelled(false);
            // We need to update state AFTER the event
            Task.sync(this::updateState);
            return;
        }

        event.setCancelled(true);

        if (slot == CONFIRM_SLOT) {
            handleUpgrade();
        }
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        // Allow drag only on input slots
        boolean involvesInput = event.getRawSlots().contains(ITEM_SLOT) || event.getRawSlots().contains(MATERIAL_SLOT);
        boolean involvesOthers = false;
        for (int s : event.getRawSlots()) {
            if (s != ITEM_SLOT && s != MATERIAL_SLOT && s < inventory.getSize()) {
                involvesOthers = true;
                break;
            }
        }

        if (involvesInput && !involvesOthers) {
            event.setCancelled(false);
            Task.sync(this::updateState);
        } else {
            event.setCancelled(true);
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        // Return items
        returnItem(ITEM_SLOT);
        returnItem(MATERIAL_SLOT);
    }

    private void returnItem(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item != null && item.getType() != Material.AIR) {
            player.getInventory().addItem(item).values().forEach(leftover -> 
                player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }
    }

    private void updateState() {
        ItemStack item = inventory.getItem(ITEM_SLOT);
        ItemStack material = inventory.getItem(MATERIAL_SLOT);
        
        UpgradeManager manager = me.ray.midgard.modules.item.manager.UpgradeManager.class.cast(
            ItemModule.getInstance().getUpgradeManager() // Assuming I add this
        ); 
        
        // If manager is null (not added yet), we can't do much.
        if (manager == null) {
            return;
        }

        // Info Item
        ItemBuilder infoBuilder = new ItemBuilder(Material.ANVIL).setName(msg("info_name"));
        
        if (item != null && item.getType() != Material.AIR) {
            int currentLevel = ItemPDC.getInt(item.getItemMeta(), "midgard_upgrade_level");
            int nextLevel = currentLevel + 1;
            
            if (nextLevel > manager.getMaxLevel()) {
                infoBuilder.addLoreLine(msg("max_level_reached"));
            } else {
                UpgradeManager.UpgradeLevel config = manager.getLevelConfig(nextLevel);
                if (config != null) {
                    infoBuilder.addLoreLine(msg("current_level") + currentLevel);
                    infoBuilder.addLoreLine(msg("next_level") + nextLevel);
                    infoBuilder.addLoreLine("");
                    infoBuilder.addLoreLine(msg("success_chance") + config.chance + "%");
                    if (config.breakChance > 0) {
                        infoBuilder.addLoreLine(msg("break_chance") + config.breakChance + "%");
                    }
                    if (config.downgradeChance > 0) {
                        infoBuilder.addLoreLine(msg("downgrade_chance") + config.downgradeChance + "%");
                    }
                    infoBuilder.addLoreLine("");
                    infoBuilder.addLoreLine(msg("cost") + config.amount + "x " + formatMaterial(config.material));
                    
                    // Check material
                    boolean hasMat = false;
                    if (material != null) {
                         // Simple check
                         String matId = ItemModule.getInstance().getItemManager().getItemId(material);
                         if (matId != null && matId.equalsIgnoreCase(config.material)) {
                             hasMat = true;
                         } else if (material.getType().name().equalsIgnoreCase(config.material)) {
                             hasMat = true;
                         }
                         
                         if (hasMat && material.getAmount() < config.amount) {
                             hasMat = false;
                         }
                    }
                    
                    if (hasMat) {
                        infoBuilder.addLoreLine(msg("materials_sufficient"));
                    } else {
                        infoBuilder.addLoreLine(msg("materials_insufficient"));
                    }
                }
            }
        } else {
            infoBuilder.addLoreLine(msg("place_item_hint"));
        }
        
        inventory.setItem(INFO_SLOT, infoBuilder.build());
        
        // Confirm Button
        ItemBuilder confirmBuilder;
        if (canUpgrade(item, material, manager)) {
            confirmBuilder = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName(msg("confirm_button"));
        } else {
            confirmBuilder = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName(msg("waiting_items"));
        }
        inventory.setItem(CONFIRM_SLOT, confirmBuilder.build());
    }

    private boolean canUpgrade(ItemStack item, ItemStack material, UpgradeManager manager) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        int currentLevel = ItemPDC.getInt(item.getItemMeta(), "midgard_upgrade_level");
        if (currentLevel >= manager.getMaxLevel()) {
            return false;
        }
        
        UpgradeManager.UpgradeLevel config = manager.getLevelConfig(currentLevel + 1);
        if (config == null) {
            return false;
        }
        
        if (material == null || material.getType() == Material.AIR) {
            return false;
        }
        
        String matId = ItemModule.getInstance().getItemManager().getItemId(material);
        boolean matches = false;
        if (matId != null && matId.equalsIgnoreCase(config.material)) {
            matches = true;
        } else if (material.getType().name().equalsIgnoreCase(config.material)) {
            matches = true;
        }
        
        return matches && material.getAmount() >= config.amount;
    }
    
    private void handleUpgrade() {
        ItemStack item = inventory.getItem(ITEM_SLOT);
        ItemStack material = inventory.getItem(MATERIAL_SLOT);
        UpgradeManager manager = (UpgradeManager) ItemModule.getInstance().getUpgradeManager();
        
        if (manager == null || !canUpgrade(item, material, manager)) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            return;
        }
        
        UpgradeManager.UpgradeResult result = manager.upgradeItem(player, item, material);
        
        switch (result) {
            case SUCCESS:
                MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.gui.upgrade.success_message"));
                break;
            case FAIL:
                MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.gui.upgrade.fail_message"));
                break;
            case BREAK:
                MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.gui.upgrade.break_message"));
                inventory.setItem(ITEM_SLOT, null); // Item was set to amount 0 in manager, but clear slot to be safe
                break;
            case DOWNGRADE:
                MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.gui.upgrade.downgrade_message"));
                break;
            default:
                MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.gui.upgrade.error_message"));
                break;
        }
        
        updateState();
    }

    private static String msg(String key) {
        return MidgardCore.getLanguageManager().getRawMessage("item.gui.upgrade." + key);
    }

    private String formatMaterial(String mat) {
        return mat.toLowerCase().replace("_", " ");
    }
}
