package me.ray.midgard.modules.mythicmobs.drops;

import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.inventory.ItemStack;

public class MidgardItemDrop implements IItemDrop {

    private final String itemId;

    public MidgardItemDrop(MythicLineConfig config) {
        this.itemId = config.getString(new String[]{"id", "item"}, "SWORD");
    }

    @Override
    public AbstractItemStack getDrop(DropMetadata metadata, double amount) {
        if (ItemModule.getInstance() == null || ItemModule.getInstance().getItemManager() == null) {
            return null;
        }
        MidgardItem midgardItem = ItemModule.getInstance().getItemManager().getMidgardItem(itemId);
        
        if (midgardItem == null) {
            return null;
        }
        
        ItemStack itemStack = midgardItem.build();
        itemStack.setAmount((int) amount);
        
        return BukkitAdapter.adapt(itemStack);
    }
}
