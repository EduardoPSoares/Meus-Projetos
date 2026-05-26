package me.ray.midgard.modules.races.listener;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.api.RaceXpSource;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;

public class RaceXpListener implements Listener {

    private final RacesModule module;

    public RaceXpListener(RacesModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        try {
            Player killer = event.getEntity().getKiller();
            if (killer == null) { return; }

            double base = getBaseXp(RaceXpSource.COMBAT);
            if (base <= 0) { return; }

            module.getLevelManager().addExperience(killer, base, RaceXpSource.COMBAT);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de combate", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            Material type = event.getBlock().getType();
            RaceXpSource source = classifyBlock(type);
            if (source == null) { return; }

            double base = getBaseXp(source);
            if (base <= 0) { return; }

            module.getLevelManager().addExperience(event.getPlayer(), base, source);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de bloco para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        try {
            double base = getBaseXp(RaceXpSource.BUILDING);
            if (base <= 0) { return; }

            module.getLevelManager().addExperience(event.getPlayer(), base, RaceXpSource.BUILDING);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de construção para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        try {
            if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) { return; }

            double base = getBaseXp(RaceXpSource.FISHING);
            if (base <= 0) { return; }

            module.getLevelManager().addExperience(event.getPlayer(), base, RaceXpSource.FISHING);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de pesca para %s", event.getPlayer().getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player player)) { return; }

            double base = getBaseXp(RaceXpSource.CRAFTING);
            if (base <= 0) { return; }

            int amount = event.getRecipe().getResult().getAmount();
            if (event.isShiftClick()) {
                amount = calculateShiftClickAmount(event);
            }
            module.getLevelManager().addExperience(player, base * amount, RaceXpSource.CRAFTING);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar XP de craft", e);
        }
    }

    private RaceXpSource classifyBlock(Material type) {
        if (Tag.MINEABLE_PICKAXE.isTagged(type) && type.name().contains("ORE")) {
            return RaceXpSource.MINING;
        }
        if (Tag.LOGS.isTagged(type)) {
            return RaceXpSource.WOODCUTTING;
        }
        if (Tag.CROPS.isTagged(type) || type == Material.MELON || type == Material.PUMPKIN
                || type == Material.SUGAR_CANE || type == Material.CACTUS || type == Material.COCOA
                || type == Material.SWEET_BERRY_BUSH || type == Material.NETHER_WART) {
            return RaceXpSource.HARVESTING;
        }
        return null;
    }

    private double getBaseXp(RaceXpSource source) {
        String key = "xp-sources." + source.name().toLowerCase().replace('_', '-');
        return module.getConfig().getDouble(key, 0.0);
    }

    private int calculateShiftClickAmount(CraftItemEvent event) {
        int resultAmount = event.getRecipe().getResult().getAmount();
        int minIngredient = Integer.MAX_VALUE;
        for (org.bukkit.inventory.ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && !item.getType().isAir()) {
                minIngredient = Math.min(minIngredient, item.getAmount());
            }
        }
        if (minIngredient == Integer.MAX_VALUE) { return resultAmount; }
        return resultAmount * minIngredient;
    }
}
