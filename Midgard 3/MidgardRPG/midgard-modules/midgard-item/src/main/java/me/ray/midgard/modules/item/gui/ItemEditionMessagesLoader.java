package me.ray.midgard.modules.item.gui;

import me.ray.midgard.modules.item.ItemModule;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemEditionMessagesLoader {

    private final ItemModule module;
    private final Map<String, StatConfig> stats = new HashMap<>();
    private final Map<String, StatConfig> buttons = new HashMap<>();
    private final Map<String, String> messages = new HashMap<>();
    private String title = "Item Edition: %id%";

    private static final String BASE_PATH = "messages/gui/item_edition/";

    public ItemEditionMessagesLoader(ItemModule module) {
        this.module = module;
    }

    public void load() {
        stats.clear();
        buttons.clear();
        messages.clear();

        loadMainConfig();
        loadCategories();
        loadEditors();
        
        module.getPlugin().getLogger().info("ItemEdition: Loaded " + stats.size() + " stats configurations and " + messages.size() + " messages.");
    }

    private void loadMainConfig() {
        File mainFile = new File(module.getDataFolder(), BASE_PATH + "_main.yml");
        if (!mainFile.exists()) {
             module.getPlugin().getLogger().warning("ItemEdition: _main.yml not found at " + mainFile.getPath());
             return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(mainFile);
        title = yaml.getString("title", "Item Edition: %id%");
        
        ConfigurationSection msgs = yaml.getConfigurationSection("messages");
        if (msgs != null) {
            for (String key : msgs.getKeys(false)) {
                messages.put("messages." + key, msgs.getString(key));
            }
        }
        
        ConfigurationSection btns = yaml.getConfigurationSection("buttons");
        if (btns != null) {
            for (String key : btns.getKeys(false)) {
                ConfigurationSection btn = btns.getConfigurationSection(key);
                if (btn != null) {
                    buttons.put(key, new StatConfig(
                        btn.getString("name"),
                        btn.getStringList("lore")
                    ));
                }
            }
        }
    }

    private void loadCategories() {
        File categoriesDir = new File(module.getDataFolder(), BASE_PATH + "categories/");
        if (!categoriesDir.exists()) {
            return;
        }

        File[] categoryFiles = categoriesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (categoryFiles == null) {
            return;
        }

        for (File file : categoryFiles) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection ads = yaml.getConfigurationSection("stats");
            if (ads != null) {
                for (String key : ads.getKeys(false)) {
                    ConfigurationSection statSec = ads.getConfigurationSection(key);
                    if (statSec != null) {
                        stats.put(key, new StatConfig(
                                statSec.getString("name"),
                                statSec.getStringList("lore")
                        ));
                    }
                }
            }
        }
    }

    private void loadEditors() {
        File file = new File(module.getDataFolder(), BASE_PATH + "editors.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        // Load editor messages into main map prefixed
        // e.g. editors.boolean.reset
        flatten(yaml.getConfigurationSection("editors"), "editors");
    }

    private void flatten(ConfigurationSection section, String prefix) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                flatten(section.getConfigurationSection(key), prefix + "." + key);
            } else {
                messages.put(prefix + "." + key, section.getString(key));
            }
        }
    }

    public String getTitle() {
        return title;
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, key);
    }
    
    // Helper for GUI to find lore, etc.
    // However, the GUI currently asks LanguageManager for lists. 
    // We should probably just expose getStatConfig
    
    public StatConfig getStatConfig(String key) {
        return stats.get(key);
    }

    public StatConfig getButtonConfig(String key) {
        return buttons.get(key);
    }

    public static class StatConfig {
        private final String name;
        private final List<String> lore;

        public StatConfig(String name, List<String> lore) {
            this.name = name;
            this.lore = lore;
        }

        public String getName() {
            return name;
        }

        public List<String> getLore() {
            return lore;
        }
    }
}
