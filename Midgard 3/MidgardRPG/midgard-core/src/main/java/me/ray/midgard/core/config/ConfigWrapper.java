package me.ray.midgard.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Wrapper para facilitar o gerenciamento de arquivos de configuração YAML.
 */
public class ConfigWrapper {

    private final JavaPlugin plugin;
    private final String fileName;
    private File file;
    private FileConfiguration config;

    /**
     * Construtor do ConfigWrapper.
     *
     * @param plugin Instância do plugin.
     * @param fileName Nome do arquivo (ex: "config.yml").
     */
    public ConfigWrapper(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        createFile();
    }

    private void createFile() {
        if (plugin == null) {
            java.util.logging.Logger.getLogger("MidgardCore").severe("Plugin instance is null in ConfigWrapper for " + fileName);
            return;
        }
        try {
            file = new File(plugin.getDataFolder(), fileName);
            if (!file.exists()) {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                } else {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Não foi possível criar o arquivo de configuração " + fileName, e);
                    }
                }
            }
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro crítico ao inicializar configuração: " + fileName, e);
            config = new YamlConfiguration(); // Fallback empty config prevents NPE
        }
    }

    /**
     * Obtém a configuração carregada.
     *
     * @return FileConfiguration.
     */
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    /**
     * Salva a configuração no disco.
     */
    public void saveConfig() {
        if (config == null || file == null) {
            plugin.getLogger().warning("Tentativa de salvar configuração não inicializada ou inválida: " + fileName);
            return;
        }
        try {
            getConfig().save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Não foi possível salvar a configuração em " + file, e);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro inesperado ao salvar configuração " + fileName, e);
        }
    }

    /**
     * Recarrega a configuração do disco.
     */
    public void reloadConfig() {
        try {
            if (file == null) {
                file = new File(plugin.getDataFolder(), fileName);
            }
            config = YamlConfiguration.loadConfiguration(file);
            
            // Validate if load was successful (YamlConfiguration usually doesn't throw on load, but keeps errors internally or returns empty)
        } catch (Exception e) {
             plugin.getLogger().log(Level.SEVERE, "Erro ao recarregar configuração " + fileName, e);
             if (config == null) config = new YamlConfiguration();
        }
    }
}
