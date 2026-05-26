package me.ray.midgard.modules.essentials.config;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.config.ConfigWrapper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class EssentialsConfig {

    private final ConfigWrapper configWrapper;

    public EssentialsConfig(JavaPlugin plugin) {
        this.configWrapper = new ConfigWrapper(plugin, "modules/essentials/config.yml");
    }

    public FileConfiguration getConfig() {
        return configWrapper.getConfig();
    }

    public void save() {
        configWrapper.saveConfig();
    }

    public void reload() {
        configWrapper.reloadConfig();
    }
    
    public String getMessage(String path) {
        // Redirect to LanguageManager
        // Keys are expected to be in messages/essentials/messages.yml -> "essentials.key"
        return MidgardCore.getLanguageManager().getRawMessage("essentials." + path);
    }
    
    public String getRawMessage(String path) {
        return getMessage(path);
    }
}
