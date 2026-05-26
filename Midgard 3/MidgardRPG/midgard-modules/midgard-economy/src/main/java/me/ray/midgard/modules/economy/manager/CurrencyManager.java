package me.ray.midgard.modules.economy.manager;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.economy.EconomyModule;
import me.ray.midgard.modules.item.ItemModule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.io.File;

public class CurrencyManager {

    private final Map<String, Integer> currencyValues = new HashMap<>();
    private final Map<String, ItemStack> currencyTemplates = new HashMap<>();
    private List<Map.Entry<String, Integer>> sortedCurrencies = new ArrayList<>();
    private final EconomyModule module;

    public CurrencyManager(EconomyModule module) {
        this.module = module;
        loadCurrencies(module);
    }
    
    public void reload() {
        loadCurrencies(module);
    }

    private void loadCurrencies(EconomyModule module) {
        currencyValues.clear();
        currencyTemplates.clear();
        org.bukkit.configuration.ConfigurationSection section = module.getConfig().getConfigurationSection("currencies");
        if (section == null) {
            return;
        }

        // Debug: List available items in ItemManager to diagnose loading issues
        if (ItemModule.getInstance() != null && ItemModule.getInstance().getItemManager() != null) {
             // Access via reflection or public method if available. 
             // Assuming we can't easily modify ItemManager to expose keys publicly right now if it's not open.
             // But wait, getMidgardItem(id) is public.
        }

        for (String key : section.getKeys(false)) {
            int value = section.getInt(key + ".value", 0);
            if (value > 0) {
                ItemModule itemModule = ItemModule.getInstance();
                if (itemModule == null || itemModule.getItemManager() == null) {
                    MidgardLogger.warn("ItemModule not loaded. Cannot register currency: " + key);
                    continue;
                }
                // Cache template and only register if valid
                me.ray.midgard.modules.item.model.MidgardItem mItem = itemModule.getItemManager().getItem(key);
                
                // Retry Logic: If item is null, maybe ItemManager hasn't refreshed fully or file system lag.
                // Try to force load specific file? No, ItemManager doesn't have that exposed.
                
                if (mItem != null) {
                    currencyValues.put(key, value);
                    currencyTemplates.put(key, mItem.build());
                    me.ray.midgard.core.debug.MidgardLogger.debug("Currency registered: " + key);
                } else {
                    MidgardLogger.warn("Currency item not found during load: " + key);
                    
                    // Critical Debugging
                    File expectedFile = new File(module.getPlugin().getDataFolder(), "modules/item/item/currency/" + key + ".yml");
                    if (expectedFile.exists()) {
                        MidgardLogger.warn(" -> File exists at " + expectedFile.getPath() + " but ItemManager didn't load it.");
                    } else {
                        MidgardLogger.warn(" -> File DOES NOT exist at " + expectedFile.getPath());
                    }
                }
            }
        }
        
        // Ordenar por valor decrescente para otimizar entrega (compactação)
        sortedCurrencies = new ArrayList<>(currencyValues.entrySet());
        sortedCurrencies.sort((a, b) -> b.getValue().compareTo(a.getValue()));
    }

    /**
     * Calcula o valor total de moedas no inventário do jogador.
     */
    public int getPhysicalBalance(Player player) {
        int total = 0;
        
        // Use getStorageContents() to avoid counting Armor slots
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || !item.hasItemMeta()) {
                continue;
            }
            total += getStackValue(item);
        }
        
        // Explicitly check Offhand
        total += getStackValue(player.getInventory().getItemInOffHand());
        
        return total;
    }
    
    private int getStackValue(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        ItemModule itemModule = ItemModule.getInstance();
        if (itemModule == null || itemModule.getItemManager() == null) {
            return 0;
        }
        
        String id = itemModule.getItemManager().getItemId(item);
        if (id == null) {
            return 0; // Not a MidgardItem
        }

        // Security Check: Verify if Lore matches the official template
        if (!isSafeCurrency(item, id)) {
            return 0;
        }

        return currencyValues.getOrDefault(id, 0) * item.getAmount();
    }

    /**
     * Dá ao jogador uma quantidade específica de moeda física.
     * Tenta dar na maior denominação possível.
     * O que sobrar (se inventário cheio) cai no chão.
     */
    public void givePhysicalCurrency(Player player, int amount) {
        giveCurrency(player, amount);
    }
    
    private boolean isSafeCurrency(ItemStack item, String id) {
        ItemStack template = currencyTemplates.get(id);
        
        // Fallback if not cached (should not happen if loaded correctly)
        if (template == null) {
            me.ray.midgard.modules.item.model.MidgardItem mItem = ItemModule.getInstance().getItemManager().getItem(id);
            if (mItem == null) {
                return false;
            }
            template = mItem.build();
        }
        
        ItemMeta itemMeta = item.getItemMeta();
        ItemMeta templateMeta = template.getItemMeta();
        
        // Check 1: Display Name (Anti-Renamed Items)
        if (itemMeta.hasDisplayName()) {
            if (!templateMeta.hasDisplayName()) {
                return false;
            }
            if (!Objects.equals(itemMeta.displayName(), templateMeta.displayName())) {
                return false;
            }
        } else {
             if (templateMeta.hasDisplayName()) {
                 return false;
             }
        }
        
        // Check 2: Lore
        if (!template.hasItemMeta() || !templateMeta.hasLore()) {
            return true;
        }
        if (!itemMeta.hasLore()) {
            return false;
        }
        return Objects.equals(itemMeta.lore(), templateMeta.lore());
    }
    
    public int getValue(ItemStack item) {
        return getStackValue(item);
    }

    public List<ItemStack> getCurrencyStacks(int amount) {
        List<ItemStack> result = new ArrayList<>();
        if (amount <= 0) {
            return result;
        }

        for (Map.Entry<String, Integer> entry : sortedCurrencies) {
            String id = entry.getKey();
            int value = entry.getValue();
            
            if (amount >= value) {
                // Ensure template exists before calculating
                ItemStack template = currencyTemplates.get(id);
                if (template == null) {
                    // Fallback
                    me.ray.midgard.modules.item.model.MidgardItem mItem = ItemModule.getInstance().getItemManager().getItem(id);
                    if (mItem != null) {
                        template = mItem.build();
                        // Optional: cache it for later?
                        // currencyTemplates.put(id, template); 
                    }
                }
                
                // If still null, skip this currency type
                if (template == null) {
                    MidgardLogger.warn("Skipping currency " + id + " in conversion due to missing template.");
                    continue;
                }
                
                int count = amount / value;
                
                while (count > 0) {
                    int stackSize = Math.min(count, template.getMaxStackSize());
                    ItemStack s = template.clone();
                    s.setAmount(stackSize);
                    result.add(s);
                    count -= stackSize;
                }
                
                amount %= value;
            }
            
            if (amount == 0) {
                break;
            }
        }
        return result;
    }

    public void removeCurrencyItems(Player player) {
        // Main Inventory (Storage only, no armor)
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (shouldRemove(contents[i])) {
                player.getInventory().setItem(i, null);
            }
        }
        
        // Offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (shouldRemove(offhand)) {
            player.getInventory().setItemInOffHand(null);
        }
    }
    
    private boolean shouldRemove(ItemStack item) {
        return getStackValue(item) > 0;
    }

    public void giveCurrency(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        
        // Safety Limit: Prevent server crash from excessive item drops
        // Assuming max stack is 64. 100k stacks = 6.4 million items.
        // Let's cap at 100,000 items per transaction to be safe.
        // Actually, command parser limits input, but internal calls might not.
        
        // Entrega dinâmica baseada na ordem de valor (Maior -> Menor)
        for (Map.Entry<String, Integer> entry : sortedCurrencies) {
            String id = entry.getKey();
            int value = entry.getValue();
            
            if (amount >= value) {
                // Pre-check if item is valid to avoid Money Void
                ItemStack template = currencyTemplates.get(id);
                if (template == null) {
                     me.ray.midgard.modules.item.model.MidgardItem mItem = ItemModule.getInstance().getItemManager().getItem(id);
                     if (mItem == null) {
                         // Skip invalid currency
                         MidgardLogger.warn("Skipping invalid currency " + id + " in giveCurrency");
                         continue;
                     }
                }
                
                int count = amount / value;
                
                // CRITICAL: Lag Protection
                // If we are about to give more than 5000 stacks (320k items), stop.
                if (count > 5000 * 64) {
                    MidgardLogger.warn("Aborting massive currency give to " + player.getName() + ": " + count + " items.");
                    MessageUtils.send(player, module.getMessage("currency.error.too_large"));
                    return;
                }
                
                giveItem(player, id, count);
                amount %= value;
            }
            
            if (amount == 0) {
                break;
            }
        }
    }
    
    public boolean takeCurrency(Player player, int amount) {
        if (amount <= 0) {
            return false;
        }
        int currentBalance = getPhysicalBalance(player);
        
        if (currentBalance < amount) {
            return false;
        }
        
        // Remove specific amount from inventory (smallest denominations first to minimize change)
        int remaining = amount;
        
        // Iterate from lowest value to highest to minimize change needed
        List<Map.Entry<String, Integer>> ascending = new ArrayList<>(sortedCurrencies);
        java.util.Collections.reverse(ascending);
        
        for (Map.Entry<String, Integer> entry : ascending) {
            if (remaining <= 0) {
                break;
            }
            String id = entry.getKey();
            int unitValue = entry.getValue();
            
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (int i = 0; i < contents.length; i++) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack item = contents[i];
                if (item == null || !item.hasItemMeta()) {
                    continue;
                }
                
                String itemId = ItemModule.getInstance().getItemManager().getItemId(item);
                if (!id.equals(itemId)) {
                    continue;
                }
                if (!isSafeCurrency(item, itemId)) {
                    continue;
                }
                
                int stackCount = item.getAmount();
                int canTakeValue = stackCount * unitValue;
                
                if (canTakeValue <= remaining) {
                    // Take entire stack
                    remaining -= canTakeValue;
                    player.getInventory().setItem(i, null);
                } else {
                    // Take partial stack
                    int itemsNeeded = remaining / unitValue;
                    if (itemsNeeded > 0) {
                        remaining -= itemsNeeded * unitValue;
                        item.setAmount(stackCount - itemsNeeded);
                    }
                    // If remainder < unitValue, need to break this coin into smaller ones
                    if (remaining > 0 && unitValue > remaining) {
                        // Remove one coin, give back change
                        remaining -= unitValue; // remaining goes negative = overpaid
                        item.setAmount(item.getAmount() - 1);
                        if (item.getAmount() <= 0) {
                            player.getInventory().setItem(i, null);
                        }
                    }
                }
            }
            
            // Check offhand too
            if (remaining > 0) {
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (offhand != null && offhand.hasItemMeta()) {
                    String offhandId = ItemModule.getInstance().getItemManager().getItemId(offhand);
                    if (id.equals(offhandId) && isSafeCurrency(offhand, offhandId)) {
                        int stackCount = offhand.getAmount();
                        int canTakeValue = stackCount * unitValue;
                        
                        if (canTakeValue <= remaining) {
                            remaining -= canTakeValue;
                            player.getInventory().setItemInOffHand(null);
                        } else {
                            int itemsNeeded = remaining / unitValue;
                            if (itemsNeeded > 0) {
                                remaining -= itemsNeeded * unitValue;
                                offhand.setAmount(stackCount - itemsNeeded);
                            }
                            if (remaining > 0 && unitValue > remaining) {
                                remaining -= unitValue;
                                offhand.setAmount(offhand.getAmount() - 1);
                                if (offhand.getAmount() <= 0) {
                                    player.getInventory().setItemInOffHand(null);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // If we overpaid (remaining is negative), give change
        if (remaining < 0) {
            giveCurrency(player, Math.abs(remaining));
        }
        
        return true;
    }

    private void giveItem(Player player, String id, int amount) {
        ItemStack template = currencyTemplates.get(id);
        if (template == null) {
             // Fallback or skip
             me.ray.midgard.modules.item.model.MidgardItem mItem = ItemModule.getInstance().getItemManager().getItem(id);
             if (mItem == null) {
                 MidgardLogger.warn("Currency item not found: " + id);
                 return;
             }
             template = mItem.build();
        }
        
        ItemStack item = template;
        int maxStack = item.getMaxStackSize();
        
        boolean dropped = false;
        
        while (amount > 0) {
            int stackAmount = Math.min(amount, maxStack);
            ItemStack clone = item.clone();
            clone.setAmount(stackAmount);
            
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(clone);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(l -> player.getWorld().dropItem(player.getLocation(), l));
                dropped = true;
            }
            
            amount -= stackAmount;
        }
        
        if (dropped) {
            sendFullInventoryMessage(player);
        }
    }
    
    // Helper to send message only once per transaction if needed
    public void sendFullInventoryMessage(Player player) {
         MessageUtils.send(player, module.getMessage("currency.inventory_full_drop"));
    }

}
