package me.ray.midgard.modules.item.listener;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.manager.ItemManager;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.classes.ClassData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

import java.util.List;

public class ItemRestrictionListener implements Listener {

    private final ItemModule module;

    public ItemRestrictionListener(ItemModule module) {
        this.module = module;
    }

    private MidgardItem getMidgardItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) { return null; }
        try {
            ItemManager itemManager = module.getItemManager();
            if (itemManager == null) { return null; }
            
            String id = itemManager.getItemId(itemStack);
            if (id == null) { return null; }
            return itemManager.getMidgardItem(id);
        } catch (Exception e) {
            MidgardLogger.warn("Erro ao identificar MidgardItem: " + e.getMessage());
            return null;
        }
    }

    private boolean checkLevelRequirement(Player player, MidgardItem item) {
        if (item == null) { return true; }
        
        int requiredLevel = item.getRequiredLevel();
        if (requiredLevel <= 0) { return true; }

        if (!MidgardCore.isLoaded()) {
             // If core is not ready, block usage for safety
             return false;
        }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            // Fail-safe: Block usage if profile is not loaded to prevent exploiting sync issues
            return false;
        }

        ClassData classData = profile.getData(ClassData.class);
        int playerLevel = (classData != null) ? classData.getLevel() : 1;

        if (playerLevel < requiredLevel) {
            MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.common.level_requirement", "%level%", String.valueOf(requiredLevel)));
            return false;
        }
        return true;
    }

    private boolean checkClassRequirement(Player player, MidgardItem item) {
        if (item == null) { return true; }

        List<String> requiredClasses = item.getRequiredClasses();
        if (requiredClasses == null || requiredClasses.isEmpty()) { return true; }

        if (!MidgardCore.isLoaded()) {
            return false;
        }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return false;
        }

        ClassData classData = profile.getData(ClassData.class);
        String playerClass = (classData != null) ? classData.getClassName() : null;

        if (playerClass == null || !requiredClasses.contains(playerClass)) {
            MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.common.class_requirement"));
            return false;
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) { return; }
        // Only process once per click to avoid duplicate messages
        if (event.getHand() != null && event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) { return; }

        MidgardItem item = getMidgardItem(event.getItem());
        
        if (item != null) {
            if (item.isDisableInteraction()) {
                event.setCancelled(true);
                return;
            }
            if (!checkLevelRequirement(event.getPlayer(), item)) {
                event.setCancelled(true);
                return;
            }
            if (!checkClassRequirement(event.getPlayer(), item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) { return; }
        Player player = (Player) event.getDamager();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        MidgardItem item = getMidgardItem(itemStack);
        
        if (item != null && !checkLevelRequirement(player, item)) {
            event.setCancelled(true);
            return;
        }
        if (item != null && !checkClassRequirement(player, item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        MidgardItem item = getMidgardItem(itemStack);
        
        if (item != null && !checkLevelRequirement(player, item)) {
            event.setCancelled(true);
            return;
        }
        if (item != null && !checkClassRequirement(player, item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraftPrepare(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        boolean isMidgardRecipe = false;

        if (recipe instanceof Keyed) {
            NamespacedKey key = ((Keyed) recipe).getKey();
            if (key.getNamespace().equalsIgnoreCase("midgard_item")) {
                isMidgardRecipe = true;
            }
        }

        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            MidgardItem item = getMidgardItem(ingredient);
            if (item != null) {
                // Se for um item do Midgard, só permite se for uma receita do Midgard
                if (!isMidgardRecipe) {
                    event.getInventory().setResult(null);
                    return;
                }
                
                if (item.isDisableCrafting()) {
                    event.getInventory().setResult(null);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);

        MidgardItem item1 = getMidgardItem(first);
        if (item1 != null && item1.isDisableRepairing()) {
            event.setResult(null);
            return;
        }

        MidgardItem item2 = getMidgardItem(second);
        if (item2 != null && item2.isDisableRepairing()) {
            event.setResult(null);
        }
    }



    @EventHandler
    public void onSmithingPrepare(PrepareSmithingEvent event) {
        // Smithing inventory slots: 0=template, 1=base, 2=addition (in 1.20+)
        // But PrepareSmithingEvent might not give easy access to slots depending on version.
        // It has getInventory().
        
        for (ItemStack content : event.getInventory().getContents()) {
             MidgardItem item = getMidgardItem(content);
             if (item != null && item.isDisableSmithing()) {
                 event.setResult(null);
                 return;
             }
        }
    }

    @EventHandler
    public void onEnchantPrepare(PrepareItemEnchantEvent event) {
        MidgardItem item = getMidgardItem(event.getItem());
        if (item != null && item.isDisableEnchanting()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        MidgardItem item = getMidgardItem(event.getItemDrop().getItemStack());
        if (item != null && item.isDisableItemDropping()) {
            event.setCancelled(true);
            MessageUtils.send(event.getPlayer(), MidgardCore.getLanguageManager().getMessage("item.restriction.drop"));
        }
    }
}
