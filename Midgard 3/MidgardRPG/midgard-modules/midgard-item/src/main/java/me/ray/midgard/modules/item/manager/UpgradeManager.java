package me.ray.midgard.modules.item.manager;

import me.ray.midgard.core.database.DefinitionMigrationTool;
import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.ItemStat;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.utils.ItemPDC;
import me.ray.midgard.modules.item.utils.LoreFormatter;
import me.ray.midgard.modules.item.utils.StatRange;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class UpgradeManager {

    private final ItemModule module;
    private final Map<Integer, UpgradeLevel> levels = new HashMap<>();
    private int maxLevel = 10;
    private DefinitionRepository repository;

    public UpgradeManager(ItemModule module) {
        this.module = module;

        // Initialize DB repository + migrate if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new DefinitionRepository(dbManager, "midgard_upgrade_config");
            File upgradeFile = new File(module.getDataFolder(), "upgrade.yml");
            new DefinitionMigrationTool(repository, "upgrade_config")
                .migrateWholeConfig(upgradeFile, "upgrade_config");
        }

        loadConfig();
    }

    public void loadConfig() {
        levels.clear();

        // Try DB first (synchronous loadAll - safe for startup and sync callbacks)
        if (repository != null && repository.count() > 0) {
            DefinitionRepository.DefinitionData data = repository.loadAll().get("upgrade_config");
            if (data != null) {
                try {
                    FileConfiguration config = DefinitionMigrationTool.deserializeToConfig(data.yamlData());
                    loadFromConfig(config);
                    return;
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar upgrade config do banco", e);
                }
            }
        }

        // Fallback: load from YAML
        File file = new File(module.getDataFolder(), "upgrade.yml");
        if (!file.exists()) {
            module.saveResource("modules/item/upgrade.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadFromConfig(config);
    }

    private void loadFromConfig(FileConfiguration config) {
        maxLevel = config.getInt("upgrade.max-level", 10);

        ConfigurationSection section = config.getConfigurationSection("upgrade.levels");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    double chance = section.getDouble(key + ".chance", 100.0);
                    double breakChance = section.getDouble(key + ".break-chance", 0.0);
                    double downgradeChance = section.getDouble(key + ".downgrade-chance", 0.0);
                    String material = section.getString(key + ".material", "IRON_INGOT");
                    int amount = section.getInt(key + ".amount", 1);
                    double statMultiplier = section.getDouble(key + ".stat-multiplier", 1.05);

                    // Validate multiplier to prevent NaN/Infinity corruption
                    if (!Double.isFinite(statMultiplier) || statMultiplier == 0) {
                        MidgardLogger.warn("[Upgrade] Invalid stat-multiplier " + statMultiplier + " for level " + level + ". Using 1.0.");
                        statMultiplier = 1.0;
                    }

                    levels.put(level, new UpgradeLevel(chance, breakChance, downgradeChance, material, amount, statMultiplier));
                } catch (NumberFormatException ignored) { /* Invalid level key */ }
            }
        }
    }

    public DefinitionRepository getRepository() {
        return repository;
    }

    public UpgradeResult upgradeItem(Player player, ItemStack item, ItemStack materialItem) {
        String itemId = module.getItemManager().getItemId(item);
        if (itemId == null) { return UpgradeResult.INVALID_ITEM; }

        MidgardItem midgardItem = module.getItemManager().getMidgardItem(itemId);
        if (midgardItem == null) { return UpgradeResult.INVALID_ITEM; }

        // Check if item is upgradeable (can limit by category if needed)
        // For now allow all Midgard Items

        int currentLevel = ItemPDC.getInt(item.getItemMeta(), "midgard_upgrade_level");
        int nextLevel = currentLevel + 1;

        if (nextLevel > maxLevel) { return UpgradeResult.MAX_LEVEL; }

        UpgradeLevel levelConfig = levels.get(nextLevel);
        if (levelConfig == null) { return UpgradeResult.CONFIG_ERROR; }

        // Check material
        // Supports vanilla Material or Midgard Item ID
        boolean validMaterial = false;
        
        // Check if material is a Midgard Item
        String matId = module.getItemManager().getItemId(materialItem);
        if (matId != null && matId.equalsIgnoreCase(levelConfig.material)) {
            validMaterial = true;
        } else if (materialItem.getType().name().equalsIgnoreCase(levelConfig.material)) {
            validMaterial = true;
        }

        if (!validMaterial) { return UpgradeResult.INVALID_MATERIAL; }

        if (materialItem.getAmount() < levelConfig.amount) { return UpgradeResult.INSUFFICIENT_MATERIAL; }

        // Consume material
        materialItem.setAmount(materialItem.getAmount() - levelConfig.amount);

        // Roll RNG
        double roll = ThreadLocalRandom.current().nextDouble() * 100;

        if (roll <= levelConfig.chance) {
            // Success
            applyUpgrade(item, midgardItem, nextLevel, levelConfig.statMultiplier);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
            return UpgradeResult.SUCCESS;
        } else {
            // Failure
            // Check break
            double breakRoll = ThreadLocalRandom.current().nextDouble() * 100;
            if (breakRoll <= levelConfig.breakChance) {
                item.setAmount(0); // Break item
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
                return UpgradeResult.BREAK;
            }

            // Check downgrade
            double downgradeRoll = ThreadLocalRandom.current().nextDouble() * 100;
            if (downgradeRoll <= levelConfig.downgradeChance && currentLevel > 0) {
                // Revert to previous level stats
                int prevLevel = currentLevel - 1;
                // We need the multiplier for the PREVIOUS level
                // Actually, simplest way is to recalculate stats from base for the new level
                double prevMultiplier = (prevLevel == 0) ? 1.0 : (levels.containsKey(prevLevel) ? levels.get(prevLevel).statMultiplier : 1.0);
                
                applyUpgrade(item, midgardItem, prevLevel, prevMultiplier);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
                return UpgradeResult.DOWNGRADE;
            }

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 0.5f);
            return UpgradeResult.FAIL;
        }
    }

    private void applyUpgrade(ItemStack item, MidgardItem midgardItem, int level, double multiplier) {
        ItemMeta meta = item.getItemMeta();
        
        // Read old level BEFORE writing new level to correctly compute base stats
        int oldLevel = ItemPDC.getInt(meta, "midgard_upgrade_level");
        double oldMultiplier = 1.0;
        if (oldLevel > 0 && levels.containsKey(oldLevel)) {
            oldMultiplier = levels.get(oldLevel).statMultiplier;
        }
        
        // Set new level
        ItemPDC.setInt(meta, "midgard_upgrade_level", level);
        // Mark as Rng Rolled to preserve these stats on update
        ItemPDC.setString(meta, "midgard_rng_rolled", "true");
        
        // Update Stats: recalculate from base using old multiplier
        for (Map.Entry<ItemStat, StatRange> entry : midgardItem.getStats().entrySet()) {
            double currentVal = ItemPDC.getStat(meta, entry.getKey());
            if (currentVal == 0) {
                continue; // Stat not present on this instance
            }
            
            // Guard against division by zero or invalid multipliers
            if (oldMultiplier == 0 || !Double.isFinite(oldMultiplier)) {
                oldMultiplier = 1.0;
            }
            
            // Calculate Base (approximate) using old multiplier read before update
            double baseVal = currentVal / oldMultiplier;
            
            // Guard against invalid multiplier producing NaN/Infinity
            if (multiplier == 0 || !Double.isFinite(multiplier)) {
                multiplier = 1.0;
            }
            
            // Calculate New
            double newVal = baseVal * multiplier;
            
            // Final sanity check: never save NaN or Infinity to PDC
            if (!Double.isFinite(newVal)) {
                newVal = currentVal; // Preserve original value on computation error
            }
            
            ItemPDC.setStat(meta, entry.getKey(), newVal);
        }

        // Update Name
        String displayName = midgardItem.getDisplayName(); // Reset to base name
        if (level > 0) {
            displayName = displayName + " +" + level;
        }
        meta.displayName(MessageUtils.parse(displayName));
        
        item.setItemMeta(meta);
        
        // Update Lore
        List<Component> lore = LoreFormatter.formatLore(item);
        item.lore(lore);
    }

    public UpgradeLevel getLevelConfig(int level) {
        return levels.get(level);
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }

    public static class UpgradeLevel {
        public double chance;
        public double breakChance;
        public double downgradeChance;
        public String material;
        public int amount;
        public double statMultiplier;

        public UpgradeLevel(double chance, double breakChance, double downgradeChance, String material, int amount, double statMultiplier) {
            this.chance = chance;
            this.breakChance = breakChance;
            this.downgradeChance = downgradeChance;
            this.material = material;
            this.amount = amount;
            this.statMultiplier = statMultiplier;
        }
    }

    public enum UpgradeResult {
        SUCCESS, FAIL, BREAK, DOWNGRADE, INVALID_ITEM, INVALID_MATERIAL, INSUFFICIENT_MATERIAL, MAX_LEVEL, CONFIG_ERROR
    }
}
