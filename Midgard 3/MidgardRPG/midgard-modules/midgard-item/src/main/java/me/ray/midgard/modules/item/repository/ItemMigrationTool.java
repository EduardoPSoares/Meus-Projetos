package me.ray.midgard.modules.item.repository;

import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Ferramenta de migração que transfere itens de armazenamento YAML local
 * para o banco de dados compartilhado. Executa automaticamente na primeira
 * inicialização quando o banco de dados está vazio e existem YAMLs locais.
 */
public class ItemMigrationTool {

    private final ItemRepository repository;
    private final File dataFolder;

    public ItemMigrationTool(ItemRepository repository, File dataFolder) {
        this.repository = repository;
        this.dataFolder = dataFolder;
    }

    /**
     * Migra itens YAML para o banco de dados se necessário.
     * 
     * @return número de itens migrados
     */
    public int migrateIfNeeded() {
        if (repository.count() > 0) {
            MidgardLogger.info("[Migration] Banco de dados já contém itens. Migração não necessária.");
            return 0;
        }

        if (!dataFolder.exists()) {
            return 0;
        }

        // Verifica se existem itens YAML para migrar
        boolean hasYamlItems = hasItemFiles(dataFolder);
        if (!hasYamlItems) {
            return 0;
        }

        MidgardLogger.info("[Migration] Banco de dados vazio. Iniciando migração de itens YAML...");
        int migrated = migrateFolder(dataFolder, "MISCELLANEOUS");

        if (migrated > 0) {
            // Move pastas de itens para backup
            File backup = new File(dataFolder.getParentFile(), "item_backup_yaml");
            if (!backup.exists()) {
                backup.mkdirs();
            }

            File[] folders = dataFolder.listFiles(File::isDirectory);
            if (folders != null) {
                for (File folder : folders) {
                    if (isSystemFolder(folder.getName())) {
                        continue;
                    }
                    File dest = new File(backup, folder.getName());
                    if (folder.renameTo(dest)) {
                        MidgardLogger.info("[Migration] Pasta movida para backup: " + folder.getName());
                    }
                }
            }

            // Move arquivos yml de itens da raiz
            File[] rootFiles = dataFolder.listFiles((dir, name) ->
                name.endsWith(".yml") && !isSystemFile(name));
            if (rootFiles != null) {
                for (File f : rootFiles) {
                    f.renameTo(new File(backup, f.getName()));
                }
            }

            MidgardLogger.info("[Migration] " + migrated + " itens migrados de YAML → banco de dados.");
            MidgardLogger.info("[Migration] Backup: " + backup.getAbsolutePath());
        }

        return migrated;
    }

    private boolean hasItemFiles(File folder) {
        File[] files = folder.listFiles();
        if (files == null) {
            return false;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (isSystemFolder(file.getName())) {
                    continue;
                }
                if (hasItemFiles(file)) {
                    return true;
                }
            } else if (file.getName().endsWith(".yml") && !isSystemFile(file.getName())) {
                // Verifica se o arquivo contém seções de itens
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String key : config.getKeys(false)) {
                    if (config.isConfigurationSection(key)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int migrateFolder(File folder, String defaultCategory) {
        int count = 0;
        File[] files = folder.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (isSystemFolder(file.getName())) {
                    continue;
                }
                // Subpasta diretamente sob dataFolder → nome vira categoria
                String cat = file.getParentFile().equals(dataFolder)
                    ? file.getName().toUpperCase()
                    : defaultCategory;
                count += migrateFolder(file, cat);
            } else if (file.getName().endsWith(".yml") && !isSystemFile(file.getName())) {
                count += migrateFile(file, defaultCategory);
            }
        }
        return count;
    }

    private int migrateFile(File file, String defaultCategory) {
        int count = 0;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            if (!config.isConfigurationSection(key)) {
                continue;
            }

            try {
                ConfigurationSection section = config.getConfigurationSection(key);
                String categoryId = resolveCategoryId(section, defaultCategory);
                String yamlData = serializeSection(section);

                // Salva sincronamente durante migração (join)
                repository.saveItem(key, categoryId, yamlData, "migration").join();
                count++;
                MidgardLogger.info("[Migration] Item migrado: " + key + " (categoria: " + categoryId + ")");
            } catch (Exception e) {
                MidgardLogger.error("Erro ao migrar item " + key + " de " + file.getName(), e);
            }
        }
        return count;
    }

    private String resolveCategoryId(ConfigurationSection section, String fallback) {
        String type = section.getString("type", null);
        if (type != null) {
            return type.toUpperCase();
        }

        if (section.isConfigurationSection("base")) {
            type = section.getConfigurationSection("base").getString("type", null);
            if (type != null) {
                return type.toUpperCase();
            }
        }

        return fallback;
    }

    /**
     * Serializa uma ConfigurationSection para YAML string.
     * Usada tanto na migração quanto no save().
     */
    static String serializeSection(ConfigurationSection section) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (String key : section.getKeys(true)) {
            if (!section.isConfigurationSection(key)) {
                yaml.set(key, section.get(key));
            }
        }
        return yaml.saveToString();
    }

    private boolean isSystemFolder(String name) {
        return name.equalsIgnoreCase("messages") ||
               name.equalsIgnoreCase("lang") ||
               name.equalsIgnoreCase("settings");
    }

    private boolean isSystemFile(String name) {
        return name.equals("config.yml") ||
               name.equals("item-types.yml") ||
               name.equals("item-tiers.yml") ||
               name.equals("item-sets.yml") ||
               name.equals("custom-stats.yml") ||
               name.equals("upgrade.yml");
    }
}
