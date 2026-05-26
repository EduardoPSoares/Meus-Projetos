package me.ray.midgard.modules.mythicmobs.drops;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.loot.LootContext;
import me.ray.midgard.core.loot.LootTable;
import me.ray.midgard.core.profile.MidgardProfile;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class MidgardLootDrop implements IItemDrop {

    private final String tableId;

    public MidgardLootDrop(MythicLineConfig config) {
        this.tableId = config.getString(new String[]{"id", "table", "t"}, "");
    }

    @Override
    public AbstractItemStack getDrop(DropMetadata metadata, double amount) {
        // This method expects a SINGLE item stack, but LootTables return a List<ItemStack>.
        // MythicMobs API for custom drops is a bit restrictive here if we want to return multiple items.
        // However, we can try to return the first item, or use a trick.
        // A better approach for Loot Tables is to implement a MECHANIC that drops the items, 
        // instead of a DROP, because Drops are expected to be single items (or stacks of same item).
        // BUT, if we implement it as a Drop, we can return null here and handle the drop manually?
        // No, getDrop is called to generate the item to be dropped by MythicMobs.
        
        // Let's try to implement it as a Mechanic instead? 
        // "midgard-drop-loot{table=xyz}"
        // But the user asked for "Drops".
        
        // If we use it as a Drop, we can only return ONE ItemStack.
        // So this is only useful if the LootTable guarantees one item or we pick one random.
        
        LootTable table = MidgardCore.getLootManager().getTable(tableId);
        if (table == null) {
            return null;
        }
        
        Player killer = null;
        if (metadata.getCause().isPresent()) {
            org.bukkit.entity.Entity entity = BukkitAdapter.adapt(metadata.getCause().get());
            if (entity instanceof Player) {
                killer = (Player) entity;
            }
        }
        
        MidgardProfile profile = null;
        if (killer != null) {
            profile = MidgardCore.getProfileManager().getProfile(killer.getUniqueId());
        }
        
        org.bukkit.Location loc = null;
        if (metadata.getDropper().isPresent()) {
             loc = BukkitAdapter.adapt(metadata.getDropper().get().getLocation());
        }
        
        LootContext context = new LootContext(profile, loc, 1.0); // Luck 1.0 default
        List<ItemStack> items = MidgardCore.getLootManager().rollLoot(table, context);
        
        if (items.isEmpty()) {
            return null;
        }
        
        // Return the first item. 
        // Limitation: If the table drops multiple different items, only the first is returned.
        return BukkitAdapter.adapt(items.get(0));
    }
}
