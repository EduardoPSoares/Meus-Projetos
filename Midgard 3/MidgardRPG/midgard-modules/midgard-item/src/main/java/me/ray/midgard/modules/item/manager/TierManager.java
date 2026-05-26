package me.ray.midgard.modules.item.manager;

import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.database.DefinitionMigrationTool;
import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.integration.NexoUtils;
import me.ray.midgard.modules.item.ItemModule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TierManager {

    private final ItemModule module;
    private final Map<String, Tier> tiers = new HashMap<>();
    private ConfigWrapper config;
    private DefinitionRepository repository;

    public TierManager(ItemModule module) {
        this.module = module;

        // Initialize DB repository + migrate if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new DefinitionRepository(dbManager, "midgard_item_tiers");
            File tiersFile = new File(module.getDataFolder(), "item-tiers.yml");
            new DefinitionMigrationTool(repository, "item_tiers")
                .migrateSingleFile(tiersFile, null, "tier");
        }
    }

    public void loadTiers() {
        tiers.clear();

        // Try DB first
        if (repository != null && repository.count() > 0) {
            Map<String, DefinitionRepository.DefinitionData> dbTiers = repository.loadAll();
            for (Map.Entry<String, DefinitionRepository.DefinitionData> entry : dbTiers.entrySet()) {
                try {
                    YamlConfiguration yaml = new YamlConfiguration();
                    yaml.loadFromString(entry.getValue().yamlData());
                    String name = yaml.getString("name", entry.getKey());
                    String tag = yaml.getString("tag", "");
                    tiers.put(entry.getKey().toUpperCase(), new Tier(entry.getKey(), name, tag));
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar tier " + entry.getKey() + " do banco", e);
                }
            }
            return;
        }

        // Fallback: load from YAML
        config = new ConfigWrapper(module.getPlugin(), "modules/item/item-tiers.yml");
        
        ConfigurationSection section = config.getConfig();
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                ConfigurationSection tierSection = section.getConfigurationSection(key);
                String name = tierSection.getString("name", key);
                String tag = tierSection.getString("tag", ""); // Nexo tag or icon
                
                tiers.put(key.toUpperCase(), new Tier(key, name, tag));
            }
        }
    }

    public void reloadTierFromDb(String tierId, DefinitionRepository.DefinitionData data) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(data.yamlData());
            String name = yaml.getString("name", tierId);
            String tag = yaml.getString("tag", "");
            tiers.put(tierId.toUpperCase(), new Tier(tierId, name, tag));
        } catch (Exception e) {
            MidgardLogger.error("Erro ao recarregar tier " + tierId + " do banco", e);
        }
    }

    public void unregisterTier(String tierId) {
        tiers.remove(tierId.toUpperCase());
    }

    public DefinitionRepository getRepository() {
        return repository;
    }

    public Tier getTier(String id) {
        if (id == null) { return null; }
        return tiers.get(id.toUpperCase());
    }

    public Collection<Tier> getTiers() {
        return tiers.values();
    }

    public static class Tier {
        private final String id;
        private final String displayName;
        private final String tag;

        public Tier(String id, String displayName, String tag) {
            this.id = id;
            this.displayName = displayName;
            this.tag = tag;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTag() {
            return tag;
        }

        /**
         * Returns the tag if Nexo is enabled and the glyph is valid,
         * otherwise falls back to the display name.
         */
        public String resolveDisplay() {
            if (tag != null && !tag.isEmpty() && NexoUtils.isAvailable()) {
                // Extract glyph id from "<glyph:tier_common>" format
                String glyphId = tag.replace("<glyph:", "").replace(">", "").trim();
                if (NexoUtils.hasGlyph(glyphId)) {
                    return tag;
                }
            }
            return displayName;
        }
    }
}
