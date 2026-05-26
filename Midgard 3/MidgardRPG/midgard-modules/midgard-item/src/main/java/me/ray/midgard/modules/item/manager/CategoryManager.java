package me.ray.midgard.modules.item.manager;

import me.ray.midgard.core.database.DefinitionMigrationTool;
import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.ItemCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CategoryManager {

    private final ItemModule module;
    private final Map<String, ItemCategory> categories;
    private DefinitionRepository repository;

    public CategoryManager(ItemModule module) {
        this.module = module;
        this.categories = new HashMap<>();

        // Initialize DB repository + migrate if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new DefinitionRepository(dbManager, "midgard_item_categories");
            File typesFile = new File(module.getDataFolder(), "item-types.yml");
            new DefinitionMigrationTool(repository, "item_categories")
                .migrateSingleFile(typesFile, null, "category");
        }
    }

    public void loadCategories() {
        categories.clear();

        // Try DB first
        if (repository != null && repository.count() > 0) {
            Map<String, DefinitionRepository.DefinitionData> dbCategories = repository.loadAll();
            for (Map.Entry<String, DefinitionRepository.DefinitionData> entry : dbCategories.entrySet()) {
                try {
                    YamlConfiguration yaml = new YamlConfiguration();
                    yaml.loadFromString(entry.getValue().yamlData());
                    ItemCategory category = new ItemCategory(entry.getKey(), yaml);
                    categories.put(entry.getKey(), category);
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar categoria " + entry.getKey() + " do banco", e);
                }
            }
            module.getPlugin().getLogger().info("Carregadas " + categories.size() + " categorias do banco.");
            return;
        }

        // Fallback: load from YAML
        File file = new File(module.getDataFolder(), "item-types.yml");
        if (!file.exists()) {
            module.saveResource("modules/item/item-types.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    ItemCategory category = new ItemCategory(key, section);
                    categories.put(key, category);
                }
            } catch (Exception e) {
                module.getPlugin().getLogger().log(Level.WARNING, "Erro ao carregar categoria " + key, e);
            }
        }
        module.getPlugin().getLogger().info("Carregadas " + categories.size() + " categorias.");
    }

    public void reloadCategoryFromDb(String categoryId, DefinitionRepository.DefinitionData data) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(data.yamlData());
            ItemCategory category = new ItemCategory(categoryId, yaml);
            categories.put(categoryId, category);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao recarregar categoria " + categoryId + " do banco", e);
        }
    }

    public void unregisterCategory(String categoryId) {
        categories.remove(categoryId);
    }

    public DefinitionRepository getRepository() {
        return repository;
    }

    public ItemCategory getCategory(String id) {
        return categories.get(id);
    }

    public Collection<ItemCategory> getCategories() {
        return categories.values();
    }

    public void updateCategoryDisplayItem(String categoryId, String itemId) {
        ItemCategory old = categories.get(categoryId);
        if (old == null) { return; }

        // Create new instance
        ItemCategory newCat = new ItemCategory(categoryId, old.getName(), old.getIcon(), old.getModelData(), old.getSlot(), old.getPage(), itemId);
        categories.put(categoryId, newCat);

        // Save to DB if available
        if (repository != null) {
            repository.load(categoryId).thenAccept(existing -> {
                if (existing != null) {
                    try {
                        YamlConfiguration yaml = new YamlConfiguration();
                        yaml.loadFromString(existing.yamlData());
                        yaml.set("display-item", itemId);
                        repository.save(categoryId, existing.category(), yaml.saveToString(), "admin");
                    } catch (Exception e) {
                        MidgardLogger.error("Error saving category display item to DB", e);
                    }
                }
            });
        } else {
            // Fallback: save to file
            File file = new File(module.getDataFolder(), "item-types.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (config.contains(categoryId)) {
                config.set(categoryId + ".display-item", itemId);
                try {
                    config.save(file);
                } catch (Exception e) {
                    module.getPlugin().getLogger().log(Level.SEVERE, "Error saving category display item update", e);
                }
            }
        }
    }
}
