package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Trait que dá chance de drop extra ao quebrar blocos.
 * Config:
 *   chance: 0.15 (15% de chance de drop extra)
 *   multiplier: 2.0 (multiplicador dos drops)
 *   blocks: ["DIAMOND_ORE", "IRON_ORE", ...] (blocos que ativam, ou vazio para todos)
 */
public class HarvestTrait implements RaceTrait {

    @Override
    public String getId() {
        return "harvest";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger != TraitTrigger.ON_BLOCK_BREAK) { return; }

        if (!(context.get("event") instanceof BlockBreakEvent event)) { return; }

        Block block = event.getBlock();

        // Verificar lista de blocos permitidos
        if (config.get("blocks") instanceof List<?> blockList && !blockList.isEmpty()) {
            boolean match = false;
            for (Object b : blockList) {
                if (block.getType().name().equalsIgnoreCase(b.toString())) {
                    match = true;
                    break;
                }
            }
            if (!match) { return; }
        }

        double chance = 0.15;
        if (config.get("chance") instanceof Number n) { chance = n.doubleValue(); }

        if (ThreadLocalRandom.current().nextDouble() > chance) { return; }

        double multiplier = 2.0;
        if (config.get("multiplier") instanceof Number n) { multiplier = n.doubleValue(); }

        // Calcular drops extras baseados nos drops normais do bloco
        Collection<ItemStack> drops = block.getDrops(player.getInventory().getItemInMainHand(), player);
        int extraMultiplier = (int) multiplier - 1;
        if (extraMultiplier < 1) { extraMultiplier = 1; }

        for (ItemStack drop : drops) {
            ItemStack extra = drop.clone();
            extra.setAmount(drop.getAmount() * extraMultiplier);
            block.getWorld().dropItemNaturally(block.getLocation(), extra);
        }
    }
}
