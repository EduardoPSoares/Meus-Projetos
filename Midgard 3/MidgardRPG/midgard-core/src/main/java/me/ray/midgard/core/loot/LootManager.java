package me.ray.midgard.core.loot;

import me.ray.midgard.core.integration.NexoUtils;
import me.ray.midgard.core.registry.Registry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final Registry<String, LootTable> tableRegistry = new Registry<>();

    public LootManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerTable(LootTable table) {
        tableRegistry.register(table.getId(), table);
    }

    public LootTable getTable(String id) {
        return tableRegistry.get(id).orElse(null);
    }

    public List<ItemStack> rollLoot(LootTable table, LootContext context) {
        List<ItemStack> drops = new ArrayList<>();
        if (table == null) {
            return drops;
        }

        int minR = Math.max(0, table.getMinRolls());
        int maxR = Math.max(minR, table.getMaxRolls());
        int rolls = (minR == maxR) ? minR : ThreadLocalRandom.current().nextInt(minR, maxR + 1);

        for (int i = 0; i < rolls; i++) {
            for (LootEntry entry : table.getEntries()) {
                if (entry.canDrop(context)) {
                    processEntry(entry, context, drops);
                }
            }
        }
        return drops;
    }

    private void processEntry(LootEntry entry, LootContext context, List<ItemStack> drops) {
        int amount = entry.rollAmount();
        if (amount <= 0) {
            return;
        }

        switch (entry.getType()) {
            case ITEM:
                ItemStack item = null;
                
                // Try cache first
                if (entry.getCachedObject() instanceof ItemStack) {
                    item = ((ItemStack) entry.getCachedObject()).clone();
                } else if (entry.getCachedObject() instanceof Material) {
                    item = new ItemStack((Material) entry.getCachedObject());
                } else {
                    // Resolve and cache
                    if (entry.getValue().contains(":")) {
                        // Nexo or other plugin
                        item = NexoUtils.getCustomItem(entry.getValue());
                        if (item != null) entry.setCachedObject(item.clone());
                    } else {
                        // Vanilla
                        Material mat = Material.matchMaterial(entry.getValue());
                        if (mat != null) {
                            item = new ItemStack(mat);
                            entry.setCachedObject(mat);
                        }
                    }
                }
                
                if (item != null) {
                    item.setAmount(amount);
                    drops.add(item);
                }
                break;
                
            case MONEY:
                if (context.getPlayer().isPresent()) {
                    // Assuming default currency for now
                    // We need an instance of VaultIntegration or use a static helper if available.
                    // Since VaultIntegration is an instance in Core, we should probably access it via a Service or Manager.
                    // For now, let's assume we can get the provider from Bukkit Services if we didn't inject it.
                    // Or better, let's make a static helper in Core for simple Vault access if we want to keep LootManager simple.
                    // But wait, VaultIntegration implements EconomyProvider.
                    // Let's just use the Vault API directly here for simplicity or fix the call.
                    
                    // The error was: deposit(UUID, String, double) but we passed (Player, double).
                    // We need to pass UUID and a currency string (e.g. "VAULT").
                    
                    me.ray.midgard.core.economy.EconomyProvider provider = me.ray.midgard.core.MidgardCore.getEconomyProvider();
                    if (provider != null) {
                        try {
                            double value = Double.parseDouble(entry.getValue());
                            provider.deposit(context.getPlayer().get().getUniqueId(), "VAULT", value * amount);
                        } catch (NumberFormatException e) {
                            me.ray.midgard.core.utils.ConsoleUtils.warn("LootManager: Valor de dinheiro inválido '" + entry.getValue() + "'");
                        }
                    }
                }
                break;
                
            case EXPERIENCE:
                if (context.getPlayer().isPresent()) {
                    try {
                        int exp = Integer.parseInt(entry.getValue());
                        context.getPlayer().get().giveExp(exp * amount);
                    } catch (NumberFormatException e) {
                         me.ray.midgard.core.utils.ConsoleUtils.warn("LootManager: Valor de experiência inválido '" + entry.getValue() + "'");
                    }
                }
                break;
                
            case COMMAND:
                if (context.getPlayer().isPresent()) {
                    String cmd = entry.getValue().replace("%player%", context.getPlayer().get().getName());
                    cmd = cmd.replace("\n", "").replace("\r", "").trim();
                    if (!cmd.isEmpty()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    }
                }
                break;
        }
    }
}
