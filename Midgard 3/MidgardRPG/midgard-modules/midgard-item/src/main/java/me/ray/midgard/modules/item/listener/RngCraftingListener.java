package me.ray.midgard.modules.item.listener;

import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.utils.ItemPDC;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class RngCraftingListener implements Listener {

    private final ItemModule module;

    public RngCraftingListener(ItemModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof Keyed)) { return; }

        NamespacedKey key = ((Keyed) recipe).getKey();
        if (!key.getNamespace().equalsIgnoreCase("midgard_item")) { return; }

        // Get the static result item to check ID
        ItemStack result = event.getInventory().getResult();
        if (result == null) { return; }

        String id = ItemPDC.getMidgardId(result);
        if (id == null) { return; }

        MidgardItem item = module.getItemManager().getMidgardItem(id);
        if (item == null) { return; }

        // Block shift-click: each Midgard item needs unique RNG stats per craft.
        // Shift-click batch crafting would produce a stack with only one set of rolled stats.
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        // Generate a new item with fresh RNG stats
        ItemStack newItem = item.build();
        newItem.setAmount(result.getAmount());

        // Update both the result slot and current item to ensure the player receives
        // the freshly rolled item regardless of server implementation
        event.getInventory().setResult(newItem);
        event.setCurrentItem(newItem);
    }
}
