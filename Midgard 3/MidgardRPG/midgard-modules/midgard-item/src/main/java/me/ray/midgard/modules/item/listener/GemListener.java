package me.ray.midgard.modules.item.listener;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.socket.SocketData;
import me.ray.midgard.modules.item.utils.ItemPDC;
import me.ray.midgard.modules.item.utils.LoreFormatter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GemListener implements Listener {

    @EventHandler
    public void onGemApply(InventoryClickEvent event) {
        if (event.isCancelled()) { return; }
        if (!(event.getWhoClicked() instanceof Player player)) { return; }

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        
        if (cursor.getType() == Material.AIR) { return; }
        if (current == null || current.getType() == Material.AIR) { return; }
        
        if (ItemModule.getInstance() == null || ItemModule.getInstance().getItemManager() == null) { return; }

        String gemId = ItemModule.getInstance().getItemManager().getItemId(cursor);
        String targetId = ItemModule.getInstance().getItemManager().getItemId(current);
        
        if (gemId == null || targetId == null) { return; }
        
        MidgardItem gemItem = ItemModule.getInstance().getItemManager().getMidgardItem(gemId);
        if (gemItem == null || !gemItem.getCategoryId().equalsIgnoreCase("GEM")) { return; }
        
        // Prevent socketing gems into other gems or consumables
        MidgardItem targetItem = ItemModule.getInstance().getItemManager().getMidgardItem(targetId);
        if (targetItem != null && targetItem.getCategoryId().equalsIgnoreCase("GEM")) { return; }
        if (targetItem != null && targetItem.getCategoryId().equalsIgnoreCase("CONSUMABLE")) { return; }
        
        // Check socket compatibility
        // Assuming Gem Tier defines the socket color/type
        String gemType = gemItem.getTier(); 
        if (gemType == null || gemType.isEmpty()) { gemType = "ANY"; } // Default or universal
        
        SocketData socketData = SocketData.fromItem(current);
        if (!socketData.hasEmptySocket(gemType) && !socketData.hasEmptySocket("ANY")) { return; }
        
        // Apply gem
        if (socketData.applyGem(gemType, gemId)) {
            event.setCancelled(true);
            
            // Read gem stats BEFORE consuming
            ItemMeta gemMeta = cursor.getItemMeta();
            
            // Consume gem
            if (cursor.getAmount() > 1) {
                cursor.setAmount(cursor.getAmount() - 1);
            } else {
                event.getView().setCursor(null);
            }
            
            // Update target item
            ItemMeta meta = current.getItemMeta();
            
            // Add stats from the gem's actual PDC values (not re-rolled from template)
            for (ItemStat stat : ItemStat.values()) {
                if (!ItemPDC.hasStat(gemMeta, stat)) { continue; }
                double value = ItemPDC.getStat(gemMeta, stat);
                if (value != 0) {
                    double currentVal = ItemPDC.getStat(meta, stat);
                    ItemPDC.setStat(meta, stat, currentVal + value);
                }
            }
            
            current.setItemMeta(meta);
            socketData.save(current);
            
            // Update Lore
            ItemMeta updatedMeta = current.getItemMeta();
            List<Component> lore = LoreFormatter.formatLore(current);
            updatedMeta.lore(lore);
            current.setItemMeta(updatedMeta);
            
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
            MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("item.gem.applied_success"));
        }
    }
}
