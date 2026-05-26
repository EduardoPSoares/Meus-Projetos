package me.ray.midgard.modules.economy.manager;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.SerializationUtils;
import me.ray.midgard.modules.economy.EconomyModule;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.utils.ItemPDC;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PouchManager {

    private static final String POUCH_CONTENT_KEY = "pouch_content";
    private final Map<String, PouchSettings> pouchSettings = new HashMap<>();
    private final EconomyModule module;
    private final Set<String> openSessions = new HashSet<>();

    public PouchManager(EconomyModule module) {
        this.module = module;
        loadSettings();
    }
    
    public void reload() {
        loadSettings();
    }
    
    public void openSession(String sessionId) {
        openSessions.add(sessionId);
    }

    public void closeSession(String sessionId) {
        openSessions.remove(sessionId);
    }

    public boolean isSessionOpen(String sessionId) {
        return sessionId != null && openSessions.contains(sessionId);
    }
    
    public void loadSettings() {
        pouchSettings.clear();
        org.bukkit.configuration.ConfigurationSection section = module.getConfig().getConfigurationSection("pouches");
        if (section == null) {
            return;
        }
        
        for (String key : section.getKeys(false)) {
            int capacity = section.getInt(key + ".capacity", 4096);
            int rows = section.getInt(key + ".rows", 1);
            boolean autoCompact = section.getBoolean(key + ".auto-compact", false);
            String permission = section.getString(key + ".permission", null);
            
            pouchSettings.put(key, new PouchSettings(capacity, rows, autoCompact, permission));
        }
    }
    
    private record PouchSettings(int capacity, int rows, boolean autoCompact, String permission) {}

    public boolean isPouch(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String id = ItemModule.getInstance().getItemManager().getItemId(item);
        return id != null && pouchSettings.containsKey(id);
    }

    public String assignSessionId(ItemStack pouch) {
        if (!isPouch(pouch)) {
            return null;
        }
        String uuid = java.util.UUID.randomUUID().toString();
        ItemMeta meta = pouch.getItemMeta();
        ItemPDC.setString(meta, "pouch_session", uuid);
        pouch.setItemMeta(meta);
        return uuid;
    }
    
    public String getSessionId(ItemStack pouch) {
        if (!isPouch(pouch)) {
            return null;
        }
        return ItemPDC.getString(pouch.getItemMeta(), "pouch_session");
    }

    private PouchSettings getSettings(ItemStack pouch) {
        if (!isPouch(pouch)) {
            return new PouchSettings(0, 1, false, null);
        }
        String id = ItemModule.getInstance().getItemManager().getItemId(pouch);
        return pouchSettings.getOrDefault(id, new PouchSettings(0, 1, false, null));
    }
    
    public String getPermission(ItemStack pouch) {
        return getSettings(pouch).permission();
    }

    public int getInventorySize(ItemStack pouch) {
        int rows = getSettings(pouch).rows();
        return Math.min(6, Math.max(1, rows)) * 9;
    }
    
    public int getCapacity(ItemStack pouch) {
        return getSettings(pouch).capacity();
    }
    
    public boolean isAutoCompact(ItemStack pouch) {
        return getSettings(pouch).autoCompact();
    }

    public ItemStack[] getContents(ItemStack pouch) {
        if (!isPouch(pouch)) {
            return new ItemStack[0];
        }
        String base64 = ItemPDC.getString(pouch.getItemMeta(), POUCH_CONTENT_KEY);
        if (base64 == null) {
            return new ItemStack[0];
        }
        try {
            return (ItemStack[]) SerializationUtils.fromBase64(base64);
        } catch (Exception e) {
            MidgardLogger.error("Failed to deserialize pouch contents", e);
            return new ItemStack[0];
        }
    }

    public void setContents(ItemStack pouch, ItemStack[] contents) {
        if (!isPouch(pouch)) {
            return;
        }
        ItemMeta meta = pouch.getItemMeta();
        
        try {
            if (isAutoCompact(pouch)) {
                contents = compactContents(contents, getInventorySize(pouch));
            }

            String base64 = SerializationUtils.toBase64(contents);
            
            // Safe Update: Only update if serialization succeeded
            ItemPDC.setString(meta, POUCH_CONTENT_KEY, base64);
            
            // Cache balance for quick access
            long balance = 0;
            for (ItemStack item : contents) {
                balance += getItemValue(item);
            }
            if (balance > Integer.MAX_VALUE) {
                balance = Integer.MAX_VALUE;
            }
            ItemPDC.setInt(meta, "pouch_balance", (int) balance);
            
            pouch.setItemMeta(meta);
            updateLore(pouch);
        } catch (Exception e) {
            MidgardLogger.error("Failed to serialize pouch contents", e);
            // Optional: Message player that save failed?
        }
    }

    private ItemStack[] compactContents(ItemStack[] contents, int size) {
        long totalValueLong = 0;
        for (ItemStack item : contents) {
            totalValueLong += getItemValue(item);
        }
        int totalValue = (int) Math.min(totalValueLong, Integer.MAX_VALUE);
        return convertBalanceToItems(totalValue, size);
    }

    private ItemStack[] convertBalanceToItems(int amount, int size) {
        CurrencyManager cm = module.getCurrencyManager();
        if (cm == null) {
            return new ItemStack[size];
        }
        List<ItemStack> list = cm.getCurrencyStacks(amount);
        
        ItemStack[] array = new ItemStack[size];
        for (int i = 0; i < Math.min(list.size(), size); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
    
    public int getItemValue(ItemStack item) {
        CurrencyManager cm = module.getCurrencyManager();
        if (cm == null) {
            return 0;
        }
        return cm.getValue(item);
    }

    public int getBalance(ItemStack pouch) {
        if (!isPouch(pouch)) {
            return 0;
        }
        
        // Try cached balance first
        if (ItemPDC.has(pouch.getItemMeta(), "pouch_balance")) {
            return ItemPDC.getInt(pouch.getItemMeta(), "pouch_balance");
        }
        
        ItemStack[] contents = getContents(pouch);
        long total = 0;
        
        for (ItemStack item : contents) {
            total += getItemValue(item);
        }
        
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }
    
    public void setBalance(ItemStack pouch, int amount) {
        ItemStack[] items = convertBalanceToItems(amount, getInventorySize(pouch));
        setContents(pouch, items);
    }

    public ItemStack addItem(ItemStack pouch, ItemStack itemToAdd) {
        if (itemToAdd == null || itemToAdd.getAmount() == 0) {
            return null;
        }
        
        int currentBalance = getBalance(pouch);
        int capacity = getCapacity(pouch);
        int itemValueUnit = getItemValue(itemToAdd) / itemToAdd.getAmount();
        
        if (itemValueUnit == 0) {
            return itemToAdd;
        }
        
        int remainingValueSpace = capacity - currentBalance;
        if (remainingValueSpace <= 0) {
            return itemToAdd; // Optimization: Don't deserialize if full
        }
        
        int maxCanFit = remainingValueSpace / itemValueUnit;
        if (maxCanFit == 0) {
            return itemToAdd;
        }
        
        int toAddAmount = Math.min(itemToAdd.getAmount(), maxCanFit);
        int leftoverAmount = itemToAdd.getAmount() - toAddAmount;
        
        ItemStack toAdd = itemToAdd.clone();
        toAdd.setAmount(toAddAmount);
        
        // NOW we deserialize because we know we can fit at least something
        ItemStack[] contents = getContents(pouch);
        int remaining = toAdd.getAmount();
        
        for (ItemStack content : contents) {
            if (content == null || content.getType() == Material.AIR) {
                continue;
            }
            if (content.isSimilar(toAdd)) {
                int space = content.getMaxStackSize() - content.getAmount();
                if (space > 0) {
                    int add = Math.min(remaining, space);
                    content.setAmount(content.getAmount() + add);
                    remaining -= add;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
        }
        
        if (remaining > 0) {
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] == null || contents[i].getType() == Material.AIR) {
                    ItemStack newItem = toAdd.clone();
                    newItem.setAmount(remaining);
                    contents[i] = newItem;
                    remaining = 0;
                    break;
                }
            }
        }
        
        leftoverAmount += remaining;
        
        if (toAddAmount - remaining > 0) {
            setContents(pouch, contents);
        }
        
        if (leftoverAmount > 0) {
            ItemStack leftover = itemToAdd.clone();
            leftover.setAmount(leftoverAmount);
            return leftover;
        }
        return null;
    }

    public int addCurrency(ItemStack pouch, int amount) {
        ItemStack[] items = convertBalanceToItems(amount, 100);
        int added = 0;
        
        for (ItemStack item : items) {
            int toAdd = item.getAmount();
            ItemStack leftover = addItem(pouch, item);
            int didAdd = toAdd - (leftover == null ? 0 : leftover.getAmount());
            
            // getValue(item) returns unitValue * stackAmount (total stack value).
            // We need the unit value, then multiply by items actually added.
            int unitValue = (toAdd > 0) ? (module.getCurrencyManager().getValue(item) / toAdd) : 0;
             
             added += didAdd * unitValue;
        }
        
        return amount - added;
    }

    public int removeCurrency(ItemStack pouch, int amount) {
        if (amount <= 0) {
            return 0;
        }
        
        int currentBalance = getBalance(pouch);
        if (currentBalance <= 0) {
            return 0;
        }
        
        int toRemove = Math.min(amount, currentBalance);
        int newBalance = currentBalance - toRemove;
        
        // Rebuild contents with the new balance
        setBalance(pouch, newBalance);
        
        return toRemove;
    }

    private void updateLore(ItemStack pouch) {
        if (!isPouch(pouch)) {
            return;
        }
        ItemMeta meta = pouch.getItemMeta();
        List<Component> lore = meta.lore();
        
        if (lore != null) {
            int current = getBalance(pouch);
            int capacity = getCapacity(pouch);
            
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(java.util.Locale.GERMANY);
            String capDisplay = nf.format(capacity);
            String curDisplay = nf.format(current);
            
            List<Component> newLore = new ArrayList<>();
            for (Component line : lore) {
                String text = me.ray.midgard.core.text.MessageUtils.serialize(line);
                
                if (text.contains(module.getMessage("pouch.lore.marker_capacity"))) {
                    newLore.add(MessageUtils.parse(module.getMessage("pouch.lore.capacity")
                            .replace("%capacity%", capDisplay)));
                    continue;
                }
                
                newLore.add(line);
            }
            
            boolean hasSaldo = newLore.stream().anyMatch(c -> MessageUtils.serialize(c).contains(module.getMessage("pouch.lore.marker_contains")));
            if (!hasSaldo) {
                 int index = -1;
                 for(int i=0; i<newLore.size(); i++) {
                     if (MessageUtils.serialize(newLore.get(i)).contains(module.getMessage("pouch.lore.marker_capacity"))) {
                         index = i;
                         break;
                     }
                 }
                 
                 if (index != -1 && index + 1 < newLore.size()) {
                     newLore.add(index + 1, MessageUtils.parse(module.getMessage("pouch.lore.contains")
                             .replace("%amount%", curDisplay)));
                 }
            } else {
                for(int i=0; i<newLore.size(); i++) {
                     if (MessageUtils.serialize(newLore.get(i)).contains(module.getMessage("pouch.lore.marker_contains"))) {
                         newLore.set(i, MessageUtils.parse(module.getMessage("pouch.lore.contains")
                                 .replace("%amount%", curDisplay)));
                     }
                 }
            }
            
            meta.lore(newLore);
        }
        
        pouch.setItemMeta(meta);
    }
}
