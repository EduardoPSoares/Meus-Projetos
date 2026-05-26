package me.ray.midgard.modules.item.utils;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.manager.TierManager;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.socket.SocketData;
import me.ray.midgard.modules.item.socket.SocketEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LoreFormatter {

    private static final List<ItemStat> ELEMENTAL_STATS = java.util.Arrays.asList(
            ItemStat.FIRE_DAMAGE,
            ItemStat.ICE_DAMAGE,
            ItemStat.LIGHT_DAMAGE,
            ItemStat.DARKNESS_DAMAGE,
            ItemStat.DIVINE_DAMAGE
    );

    public static List<Component> formatLore(MidgardItem item) {
        return formatLore(item, null, null);
    }

    public static List<Component> formatLore(MidgardItem item, SocketData socketData) {
        return formatLore(item, socketData, null);
    }

    public static List<Component> formatLore(MidgardItem item, SocketData socketData, Map<ItemStat, Double> statsOverride) {
        List<Component> lore = new ArrayList<>();
        FileConfiguration config = ItemModule.getInstance().getConfig();
        List<String> layout = config.getStringList("settings.lore-format.layout");

        if (layout.isEmpty()) {
            // Fallback layout
            layout.add("{displayed-type}");
            layout.add("{tier}");
            layout.add("{stats}");
            layout.add("{sockets}");
            layout.add("{abilities}");
            layout.add("{elements}");
            layout.add("{lore}");
        }

        boolean hasElementsTag = layout.contains("{elements}");

        for (String section : layout) {
            List<Component> sectionLore = new ArrayList<>();

            if (section.equals("{stats}")) {
                sectionLore.addAll(formatStats(item, config, hasElementsTag, statsOverride));
            } else if (section.equals("{elements}")) {
                sectionLore.addAll(formatElements(item, config, statsOverride));
            } else if (section.equals("{sockets}")) {
                sectionLore.addAll(formatSockets(item, config, socketData));
            } else if (section.equals("{abilities}")) {
                sectionLore.addAll(formatAbilities(item, config));
            } else if (section.equals("{lore}")) {
                if (item.getLore() != null) {
                    for (String line : item.getLore()) {
                        sectionLore.add(MessageUtils.parse("<gray>" + line));
                    }
                }
            } else {
                // Handle generic string with placeholders (e.g. "{tier}", "{displayed-type}", or "Rank: {tier}")
                String processed = section;

                if (processed.contains("{tier}")) {
                    processed = processed.replace("{tier}", getTierString(item));
                }

                if (processed.contains("{displayed-type}")) {
                    String typeStr = item.getDisplayedType() != null ? item.getDisplayedType() : "";
                    processed = processed.replace("{displayed-type}", typeStr);
                }

                if (!processed.isEmpty() && !processed.trim().isEmpty()) {
                    sectionLore.add(MessageUtils.parse(processed));
                }
            }

            if (!sectionLore.isEmpty()) {
                if (!lore.isEmpty()) {
                    lore.add(Component.empty());
                }
                lore.addAll(sectionLore);
            }
        }

        return lore;
    }

    public static List<Component> formatLore(ItemStack itemStack) {
        String id = ItemModule.getInstance().getItemManager().getItemId(itemStack);
        if (id == null) { return new ArrayList<>(); }
        MidgardItem item = ItemModule.getInstance().getItemManager().getMidgardItem(id);
        if (item == null) { return new ArrayList<>(); }
        
        SocketData socketData = SocketData.fromItem(itemStack);

        // Read stats from PDC
        Map<ItemStat, Double> statsOverride = new java.util.HashMap<>();
        if (itemStack.hasItemMeta()) {
             for (ItemStat stat : ItemStat.values()) {
                 if (ItemPDC.hasStat(itemStack.getItemMeta(), stat)) {
                     double val = ItemPDC.getStat(itemStack.getItemMeta(), stat);
                     statsOverride.put(stat, val);
                 }
             }
        }

        return formatLore(item, socketData, statsOverride);
    }


    private static String getTierString(MidgardItem item) {
        String tierId = item.getTier();
        if (tierId == null || tierId.isEmpty()) { return ""; }

        TierManager tierManager = ItemModule.getInstance().getTierManager();
        if (tierManager == null) { return ""; }

        TierManager.Tier tier = tierManager.getTier(tierId);
        if (tier == null) { return ""; }

        return tier.resolveDisplay();
    }

    private static List<Component> formatStats(MidgardItem item, FileConfiguration config, boolean excludeElements, Map<ItemStat, Double> statsOverride) {
        List<Component> statsLore = new ArrayList<>();
        Map<ItemStat, StatRange> stats = item.getStats();

        if (stats.isEmpty()) { return statsLore; }

        for (Map.Entry<ItemStat, StatRange> entry : stats.entrySet()) {
            if (entry.getValue().getMax() == 0 && entry.getValue().getMin() == 0) { continue; }
            if (excludeElements && ELEMENTAL_STATS.contains(entry.getKey())) { continue; }

            boolean hasOverride = statsOverride != null && statsOverride.containsKey(entry.getKey());
            double val = hasOverride ? statsOverride.get(entry.getKey()).doubleValue() : 0;

            String statKey = entry.getKey().name();
            String format = config.getString("settings.lore-format.stats." + statKey);
            
            if (format == null) {
                format = config.getString("settings.lore-format.stats.default", "<gray>■ {name}: <white>{value}");
            }

            String valueStr = hasOverride ? formatValue(val) : formatValue(entry.getValue());

            String line = format
                    .replace("{name}", entry.getKey().getDisplayName())
                    .replace("{value}", valueStr);

            statsLore.add(MessageUtils.parse(line));
        }
        
        return statsLore;
    }

    private static List<Component> formatElements(MidgardItem item, FileConfiguration config, Map<ItemStat, Double> statsOverride) {
        List<Component> elementsLore = new ArrayList<>();
        Map<ItemStat, StatRange> stats = item.getStats();

        if (stats.isEmpty()) { return elementsLore; }

        for (Map.Entry<ItemStat, StatRange> entry : stats.entrySet()) {
            if (entry.getValue().getMax() == 0 && entry.getValue().getMin() == 0) { continue; }
            if (!ELEMENTAL_STATS.contains(entry.getKey())) { continue; }

            boolean hasOverride = statsOverride != null && statsOverride.containsKey(entry.getKey());
            double val = hasOverride ? statsOverride.get(entry.getKey()).doubleValue() : 0;

            String statKey = entry.getKey().name();
            String format = config.getString("settings.lore-format.stats." + statKey);
            
            if (format == null) {
                format = config.getString("settings.lore-format.stats.default", "<gray>■ {name}: <white>{value}");
            }

            String valueStr = hasOverride ? formatValue(val) : formatValue(entry.getValue());

            String line = format
                    .replace("{name}", entry.getKey().getDisplayName())
                    .replace("{value}", valueStr);

            elementsLore.add(MessageUtils.parse(line));
        }
        
        return elementsLore;
    }

    private static String formatValue(StatRange range) {
        if (range.getMin() == range.getMax()) {
            return formatValue(range.getMin());
        } else {
            return formatValue(range.getMin()) + "-" + formatValue(range.getMax());
        }
    }

    private static String formatValue(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        } else {
            return String.format("%.2f", value);
        }
    }

    private static List<Component> formatSockets(MidgardItem item, FileConfiguration config, SocketData socketData) {
        List<Component> socketsLore = new ArrayList<>();
        List<SocketEntry> sockets;
        
        if (socketData != null) {
            sockets = socketData.getSockets();
        } else {
            // Fallback to item definition (all empty)
            List<String> types = item.getGemSockets();
            if (types == null || types.isEmpty()) { return socketsLore; }
            sockets = new ArrayList<>();
            for (String type : types) {
                sockets.add(new SocketEntry(type, null));
            }
        }
        
        if (sockets.isEmpty()) { return socketsLore; }

        String separator = config.getString("settings.lore-format.sockets.separator", "");
        if (!separator.isEmpty()) {
            socketsLore.add(MessageUtils.parse(separator));
        }

        String emptyFormat = config.getString("settings.lore-format.sockets.empty", "<green>◆ Engaste de {type} Vazio");
        String filledFormat = config.getString("settings.lore-format.sockets.filled", "<green>◆ {gem}");

        for (SocketEntry entry : sockets) {
            if (entry.isEmpty()) {
                String typeName = entry.getType();
                
                // Format type name: "redWEAPON" -> "Red Weapon"
                // Check if it matches known patterns or just capitalize
                if (typeName.contains(":") || typeName.length() > 3) {
                     // Try to parse color prefix if present (e.g. "red:WEAPON" or "redWEAPON")
                     // The user image shows "redWEAPON".
                     // Let's try to split by camelCase or common colors
                     for (String color : new String[]{"red", "blue", "green", "yellow", "gold", "purple", "aqua", "gray", "white"}) {
                         if (typeName.toLowerCase().startsWith(color)) {
                             String suffix = typeName.substring(color.length());
                             // Check if suffix is uppercase or valid word
                             if (!suffix.isEmpty()) {
                                 typeName = "<" + color + ">" + translateSocketType(suffix);
                                 break;
                             }
                         }
                     }
                }
                // Fallback translation
                if (!typeName.contains("<")) {
                    typeName = translateSocketType(typeName);
                }

                String line = emptyFormat.replace("{type}", typeName);
                socketsLore.add(MessageUtils.parse(line));
            } else {
                String gemName = entry.getGemId(); 
                MidgardItem gemItem = ItemModule.getInstance().getItemManager().getMidgardItem(entry.getGemId());
                if (gemItem != null) {
                    gemName = gemItem.getDisplayName();
                }
                
                String line = filledFormat.replace("{gem}", gemName).replace("{type}", entry.getType());
                socketsLore.add(MessageUtils.parse(line));
            }
        }

        return socketsLore;
    }

    private static String translateSocketType(String type) {
        String msg = MidgardCore.getLanguageManager().getRawMessage("item.stat.socket_type." + type.toLowerCase());
        if (msg != null) { return msg; }
        switch (type.toUpperCase()) {
            case "WEAPON": return "Arma";
            case "ARMOR": return "Armadura";
            case "ACCESSORY": return "Acessório";
            case "ANY": return "Qualquer";
            default: return capitalize(type);
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) { return str; }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private static List<Component> formatAbilities(MidgardItem item, FileConfiguration config) {
        List<Component> abilitiesLore = new ArrayList<>();
        List<String> abilities = item.getItemAbilities();
        
        if (abilities == null || abilities.isEmpty()) { return abilitiesLore; }

        String separator = config.getString("settings.lore-format.abilities.separator", "");
        if (!separator.isEmpty()) {
            abilitiesLore.add(MessageUtils.parse(separator));
        }

        for (String ability : abilities) {
             // Placeholder for ability formatting
             abilitiesLore.add(MessageUtils.parse("<red>" + ability));
        }

        return abilitiesLore;
    }
}
