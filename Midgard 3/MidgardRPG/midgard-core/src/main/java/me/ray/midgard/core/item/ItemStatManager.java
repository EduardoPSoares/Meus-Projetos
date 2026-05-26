package me.ray.midgard.core.item;

import me.ray.midgard.core.attribute.AttributeModifier;
import me.ray.midgard.core.attribute.AttributeOperation;
import me.ray.midgard.core.utils.PDCUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemStatManager {

    private final NamespacedKey STATS_KEY;

    public ItemStatManager(JavaPlugin plugin) {
        this.STATS_KEY = new NamespacedKey(plugin, "item_stats");
    }

    public void setStat(ItemStack item, String attributeId, double value) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // We store stats as "attributeId:value;attributeId2:value2"
        Map<String, Double> stats = getStatsMap(item);
        stats.put(attributeId, value);
        
        saveStats(pdc, stats);
        item.setItemMeta(meta);
    }
    
    public double getStat(ItemStack item, String attributeId) {
        return getStatsMap(item).getOrDefault(attributeId, 0.0);
    }

    public Map<String, Double> getStatsMap(ItemStack item) {
        Map<String, Double> stats = new HashMap<>();
        if (item == null || !item.hasItemMeta()) {
            return stats;
        }
        
        String data = PDCUtils.get(item, STATS_KEY, PersistentDataType.STRING);
        if (data == null || data.isEmpty()) {
            return stats;
        }
        
        String[] pairs = data.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                try {
                    stats.put(kv[0], Double.parseDouble(kv[1]));
                } catch (NumberFormatException e) {
                    me.ray.midgard.core.debug.MidgardLogger.warn("Erro ao analisar estatística '%s' com valor '%s': %s", kv[0], kv[1], e.getMessage());
                }
            }
        }
        return stats;
    }
    
    private void saveStats(PersistentDataContainer pdc, Map<String, Double> stats) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        pdc.set(STATS_KEY, PersistentDataType.STRING, sb.toString());
    }
    
    public List<AttributeModifier> getModifiers(ItemStack item, String slotName) {
        List<AttributeModifier> modifiers = new ArrayList<>();
        Map<String, Double> stats = getStatsMap(item);
        
        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            // For now, assume all item stats are ADD_NUMBER
            modifiers.add(new AttributeModifier(
                "Item Stat: " + entry.getKey(),
                entry.getValue(),
                AttributeOperation.ADD_NUMBER
            ));
        }
        return modifiers;
    }
}
