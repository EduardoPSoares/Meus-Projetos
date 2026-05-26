package me.ray.midgard.loader.listener;

import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.ItemStat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemMechanicsListener implements Listener {

    public ItemMechanicsListener(JavaPlugin plugin) {
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (isTwoHanded(event.getMainHandItem()) || isTwoHanded(event.getOffHandItem())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MessageUtils.parse("&cVocê não pode usar itens na segunda mão enquanto usa uma arma de duas mãos!"));
            MidgardLogger.debug(DebugCategory.ITEMS, "Jogador %s tentou trocar de mão com arma de duas mãos.", event.getPlayer().getName());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            
            // Simple check: if putting item in offhand slot
            if (event.getSlot() == 40) { // Offhand slot
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (isTwoHanded(mainHand)) {
                    event.setCancelled(true);
                    player.sendMessage(MessageUtils.parse("&cVocê não pode equipar itens na segunda mão enquanto usa uma arma de duas mãos!"));
                }
            }
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newMainHand = player.getInventory().getItem(event.getNewSlot());
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (isTwoHanded(newMainHand) && offHand != null && offHand.getType() != Material.AIR) {
            // Drop offhand item or move to inventory
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(offHand);
                player.getInventory().setItemInOffHand(null);
                player.sendMessage(MessageUtils.parse("&eItem da segunda mão movido para o inventário (Arma de Duas Mãos)."));
            } else {
                me.ray.midgard.core.utils.Task.sync(player.getLocation(), () -> 
                    player.getWorld().dropItemNaturally(player.getLocation(), offHand)
                );
                player.getInventory().setItemInOffHand(null);
                player.sendMessage(MessageUtils.parse("&eItem da segunda mão dropado (Arma de Duas Mãos)."));
            }
            MidgardLogger.debug(DebugCategory.ITEMS, "Item off-hand removido de %s por conflito Two-Handed.", player.getName());
        }
        
        checkRequirements(player, newMainHand);
    }
    
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!checkRequirements(player, item)) {
                event.setCancelled(true);
                player.sendMessage(MessageUtils.parse("&cVocê não tem os requisitos para usar este item!"));
                MidgardLogger.debug(DebugCategory.ITEMS, "Dano cancelado: %s não tem requisitos para usar item.", player.getName());
            }
        }
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!checkRequirements(player, item)) {
            event.setCancelled(true);
            player.sendMessage(MessageUtils.parse("&cVocê não tem os requisitos para usar este item!"));
        }
    }
    
    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        double bonusExp = 0;
        
        for (ItemStack item : player.getInventory().getArmorContents()) {
            bonusExp += getStat(item, ItemStat.ADDITIONAL_EXPERIENCE);
        }
        bonusExp += getStat(player.getInventory().getItemInMainHand(), ItemStat.ADDITIONAL_EXPERIENCE);
        bonusExp += getStat(player.getInventory().getItemInOffHand(), ItemStat.ADDITIONAL_EXPERIENCE);
        
        if (bonusExp > 0) {
            int newAmount = (int) (event.getAmount() * (1 + (bonusExp / 100.0)));
            event.setAmount(newAmount);
        }
    }

    private boolean isTwoHanded(ItemStack item) {
        return getStat(item, ItemStat.TWO_HANDED) > 0;
    }

    private boolean checkRequirements(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return true;
        
        double requiredLevel = getStat(item, ItemStat.REQUIRED_LEVEL);
        if (requiredLevel > 0) {
            // Assuming MidgardProfile has level, or using vanilla exp level
            // For now using vanilla level as placeholder or profile level if available
            //   
            // For now, let's assume vanilla level for simplicity or check if ClassesModule is loaded
            
            if (player.getLevel() < requiredLevel) {
                return false;
            }
        }
        
        return true;
    }

    private double getStat(ItemStack item, ItemStat stat) {
        if (item == null || !item.hasItemMeta()) return 0;
        if (ItemModule.getInstance() == null) return 0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        String statKey = "midgard_stat_" + stat.name().toLowerCase();
        NamespacedKey key = new NamespacedKey(ItemModule.getInstance().getPlugin(), statKey);
        
        if (pdc.has(key, PersistentDataType.DOUBLE)) {
            return pdc.get(key, PersistentDataType.DOUBLE);
        }
        return 0;
    }
}
