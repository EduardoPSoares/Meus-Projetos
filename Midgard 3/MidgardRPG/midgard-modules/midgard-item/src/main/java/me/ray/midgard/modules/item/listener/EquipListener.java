package me.ray.midgard.modules.item.listener;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.classes.ClassData;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import me.ray.midgard.modules.item.manager.AttributeUpdater;
import me.ray.midgard.modules.item.task.EquipmentUpdateTask;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;

import java.util.List;

public class EquipListener implements Listener {

    private final ItemModule module;

    public EquipListener(ItemModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        AttributeUpdater.updateAttributes(event.getPlayer());
        EquipmentUpdateTask.refreshHash(event.getPlayer());
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        EquipmentUpdateTask.clearCache(uuid);
        // Clean up ability cooldowns and passive ability timers to prevent memory leaks
        if (module.getItemAbilityListener() != null) {
            module.getItemAbilityListener().cleanupPlayer(uuid);
        }
        if (module.getPassiveAbilityTask() != null) {
            module.getPassiveAbilityTask().cleanupPlayer(uuid);
        }
        // Clean up editor session to prevent memory leak if player disconnects with editor open
        me.ray.midgard.modules.item.gui.EditorSession.end(event.getPlayer());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        // Update attributes immediately using the new slot
        AttributeUpdater.updateAttributes(event.getPlayer(), event.getNewSlot());
        EquipmentUpdateTask.refreshHash(event.getPlayer());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        me.ray.midgard.core.utils.Task.sync(event.getPlayer(), () -> {
            AttributeUpdater.updateAttributes(event.getPlayer());
            EquipmentUpdateTask.refreshHash(event.getPlayer());
        });
    }
    
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        me.ray.midgard.core.utils.Task.sync(event.getPlayer(), () -> {
            AttributeUpdater.updateAttributes(event.getPlayer());
            EquipmentUpdateTask.refreshHash(event.getPlayer());
        });
    }

    private boolean checkRequirements(Player player, ItemStack itemStack) {
        return checkRequirements(player, itemStack, true);
    }

    private boolean checkRequirements(Player player, ItemStack itemStack, boolean sendMessage) {
        String id = module.getItemManager().getItemId(itemStack);
        if (id == null) { return true; }
        
        MidgardItem item = module.getItemManager().getMidgardItem(id);
        if (item == null) { return true; }

        int requiredLevel = item.getRequiredLevel();
        List<String> requiredClasses = item.getRequiredClasses();
        boolean hasLevelReq = requiredLevel > 0;
        boolean hasClassReq = requiredClasses != null && !requiredClasses.isEmpty();

        if (!hasLevelReq && !hasClassReq) { return true; }

        if (!MidgardCore.isLoaded()) { return false; }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) { return false; }

        ClassData classData = profile.getData(ClassData.class);

        if (hasLevelReq) {
            int playerLevel = (classData != null) ? classData.getLevel() : 1;
            if (playerLevel < requiredLevel) {
                if (sendMessage) {
                    MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.common.level_requirement", "%level%", String.valueOf(requiredLevel)));
                }
                return false;
            }
        }

        if (hasClassReq) {
            String playerClass = (classData != null) ? classData.getClassName() : null;
            if (playerClass == null || !requiredClasses.contains(playerClass)) {
                if (sendMessage) {
                    MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.common.class_requirement"));
                }
                return false;
            }
        }

        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) { return; }
        if (!(event.getWhoClicked() instanceof Player player)) { return; }
        if (event.getClickedInventory() == null) { return; }
        if (event.getClickedInventory().getType() != InventoryType.PLAYER) { return; }

        int slot = event.getSlot();

        // --- Check 1: Direct interaction with armor slots (36=Boots, 37=Legs, 38=Chest, 39=Head, 40=OffHand) ---
        if (slot >= 36 && slot <= 40) {
            // Placing cursor item into armor slot
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                if (!checkRequirements(player, cursorItem)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // Hotbar swap (pressing number key while hovering armor slot)
            if (event.getHotbarButton() >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                if (hotbarItem != null && hotbarItem.getType() != Material.AIR) {
                    if (!checkRequirements(player, hotbarItem)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // --- Check 2: Shift-click to auto-equip ---
        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) { return; }

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) { return; }

        if (!checkRequirements(player, item)) {
            event.setCancelled(true);
            return;
        }

        NamespacedKey key = me.ray.midgard.modules.item.utils.ItemPDC.key("midgard_equippable_slot");
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) { return; }

        String slotName = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        try {
            EquipmentSlot targetSlot = EquipmentSlot.valueOf(slotName.toUpperCase());
            PlayerInventory inv = player.getInventory();
            
            int targetSlotIndex = -1;
            switch (targetSlot) {
                case HEAD: targetSlotIndex = 39; break;
                case CHEST: targetSlotIndex = 38; break;
                case LEGS: targetSlotIndex = 37; break;
                case FEET: targetSlotIndex = 36; break;
                case OFF_HAND: targetSlotIndex = 40; break;
                default: return; // HAND or invalid
            }

            // Check if item is already in that slot (clicking the armor slot itself)
            if (event.getSlot() == targetSlotIndex) { return; }

            ItemStack currentItemInSlot = inv.getItem(targetSlotIndex);
            if (currentItemInSlot != null && currentItemInSlot.getType() != Material.AIR) {
                return; 
            }

            // Move item to slot
            event.setCancelled(true);
            
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                ItemStack toEquip = item.clone();
                toEquip.setAmount(1);
                inv.setItem(targetSlotIndex, toEquip);
            } else {
                inv.setItem(targetSlotIndex, item);
                event.setCurrentItem(null);
            }
            
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);

            me.ray.midgard.core.utils.Task.sync(player, () -> {
                AttributeUpdater.updateAttributes(player);
                EquipmentUpdateTask.refreshHash(player);
            });

        } catch (IllegalArgumentException e) {
            // Ignore
        }
    }

    @EventHandler
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        if (!(event.getTargetEntity() instanceof Player player)) { return; }
        if (!checkRequirements(player, event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) { return; }
        // Only process once per click (main hand only) to avoid duplicate messages
        if (event.getHand() != EquipmentSlot.HAND) { return; }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() == Material.AIR) { return; }
        if (!checkRequirements(player, item)) {
            event.setCancelled(true);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            player.updateInventory();
            return;
        }

        if (!item.hasItemMeta()) { return; }

        NamespacedKey key = me.ray.midgard.modules.item.utils.ItemPDC.key("midgard_equippable_slot");
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return;
        }

        String slotName = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        try {
            EquipmentSlot targetSlot = EquipmentSlot.valueOf(slotName.toUpperCase());
            
            // Ignore if target is HAND (Main Hand) as we are already holding it there
            if (targetSlot == EquipmentSlot.HAND) { return; }

            event.setCancelled(true); // Cancel default interaction
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            
            PlayerInventory inv = player.getInventory();
            ItemStack currentItemInSlot = inv.getItem(targetSlot);
            
            // Clone item to set in slot (to be safe)
            ItemStack toEquip = item.clone();
            toEquip.setAmount(1); // Usually equip 1
            
            // Handle stack reduction if amount > 1
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                // If we have a current item in slot, we can't easily swap if we are holding a stack.
                // Usually you can't equip from a stack if the slot is occupied.
                if (currentItemInSlot != null && currentItemInSlot.getType() != Material.AIR) {
                    MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.equip.stack_occupied"));
                    return; 
                }
                inv.setItem(targetSlot, toEquip);
            } else {
                // Simple swap
                inv.setItem(targetSlot, toEquip);
                if (event.getHand() == EquipmentSlot.HAND) {
                    inv.setItemInMainHand(currentItemInSlot);
                } else {
                    inv.setItemInOffHand(currentItemInSlot);
                }
            }
            
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
            
            // Update attributes (Folia-compatible)
            me.ray.midgard.core.utils.Task.sync(player, () -> {
                AttributeUpdater.updateAttributes(player);
                EquipmentUpdateTask.refreshHash(player);
            });
            
        } catch (IllegalArgumentException e) {
            MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.equip.invalid_slot", "%s", slotName));
        }
    }
}
