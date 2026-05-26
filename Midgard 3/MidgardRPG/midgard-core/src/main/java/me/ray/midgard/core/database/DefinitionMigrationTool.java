package me.ray.midgard.core.database;

import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Ferramenta genérica de migração YAML → banco de dados.
 * <p>
 * Suporta dois padrões:
 * - "Uma entidade por seção" (raças, categorias) — arquivo único com seções
 * - "Uma entidade por arquivo" (classes, spells) — pasta com um .yml por entidade
 */
public class DefinitionMigrationTool {

    private final DefinitionRepository repository;
    private final String moduleName;

    public DefinitionMigrationTool(DefinitionRepository repository, String moduleName) {
        this.repository = repository;
        this.moduleName = moduleName;
    }

    /**
     * Migra raças/tiers/etc de um arquivo onde cada top-level key é uma definição.
     * Ex: races.yml com { humano: {...}, elfo: {...} }
     *
     * @param file         Arquivo YAML
     * @param rootSection  Se != null, lê seção filha (ex: "races"). Se null, lê da raiz.
     * @param category     Categoria padrão para todas as definições
     * @return número de entidades migradas
     */
    public int migrateSingleFile(File file, String rootSection, String category) {
        if (!file.exists()) {
            return 0;
        }
        if (repository.count() > 0) {
            MidgardLogger.info("[Migration:" + moduleName + "] Banco já contém dados. Pulando migração.");
            return 0;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = rootSection != null
            ? config.getConfigurationSection(rootSection) : config;

        if (section == null) {
            return 0;
        }

        int count = 0;
        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) {
                continue;
            }
            try {
                String yamlData = serializeSection(section.getConfigurationSection(key));
                repository.save(key, category, yamlData, "migration").join();
                count++;
            } catch (Exception e) {
                MidgardLogger.error("[Migration:" + moduleName + "] Erro ao migrar " + key, e);
            }
        }

        if (count > 0) {
            backupFile(file);
            MidgardLogger.info("[Migration:" + moduleName + "] " + count + " entidades migradas.");
        }
        return count;
    }

    /**
     * Migra entidades de uma pasta onde cada arquivo pode conter múltiplas entidades
     * como seções de top-level (ex: items/swords.yml com { iron_sword: {...}, diamond_sword: {...} }).
     *
     * @param folder   Pasta raiz
     * @param category Categoria padrão
     * @return número de entidades migradas
     */
    public int migrateFolder(File folder, String category) {
        if (!folder.exists() || !folder.isDirectory()) {
            return 0;
        }
        if (repository.count() > 0) {
            MidgardLogger.info("[Migration:" + moduleName + "] Banco já contém dados. Pulando migração.");
            return 0;
        }

        int count = migrateFolderRecursive(folder, category);

        if (count > 0) {
            backupFolder(folder);
            MidgardLogger.info("[Migration:" + moduleName + "] " + count + " entidades migradas.");
        }
        return count;
    }

    /**
     * Migra entidades de uma pasta onde cada .yml É uma entidade inteira.
     * O nome do arquivo (sem extensão) vira o ID e todo o conteúdo é a definição.
     * Ideal para classes e spells que têm dados no nível raiz do YAML.
     *
     * @param folder   Pasta raiz (ex: modules/classes/classes/)
     * @param category Categoria padrão
     * @return número de entidades migradas
     */
    public int migrateFolderWholeFiles(File folder, String category) {
        if (!folder.exists() || !folder.isDirectory()) {
            return 0;
        }
        if (repository.count() > 0) {
            MidgardLogger.info("[Migration:" + moduleName + "] Banco já contém dados. Pulando migração.");
            return 0;
        }

        int count = migrateFolderWholeFilesRecursive(folder, category, folder);

        if (count > 0) {
            backupFolder(folder);
            MidgardLogger.info("[Migration:" + moduleName + "] " + count + " entidades migradas (whole-file mode).");
        }
        return count;
    }

    private void backupFolder(File folder) {
        File backup = new File(folder.getParentFile(), folder.getName() + "_backup_yaml");
        try {
            java.nio.file.Files.move(folder.toPath(), backup.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            MidgardLogger.info("[Migration:" + moduleName + "] Pasta movida para: " + backup.getName());
        } catch (java.io.IOException e) {
            MidgardLogger.error("[Migration:" + moduleName + "] Failed to move folder: " + folder.getName(), e);
        }
    }

    private int migrateFolderWholeFilesRecursive(File folder, String defaultCategory, File rootFolder) {
        int count = 0;
        File[] files = folder.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String subCategory = file.getParentFile().equals(rootFolder)
                    ? file.getName().toUpperCase() : defaultCategory;
                count += migrateFolderWholeFilesRecursive(file, subCategory, rootFolder);
            } else if (file.getName().endsWith(".yml")) {
                String id = file.getName().replace(".yml", "").toLowerCase();
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String cat = config.getString("type",
                        config.getString("category", defaultCategory));
                    String yamlData = config.saveToString();
                    repository.save(id, cat, yamlData, "migration").join();
                    count++;
                } catch (Exception e) {
                    MidgardLogger.error("[Migration:" + moduleName + "] Erro ao migrar " + id + " de " + file.getName(), e);
                }
            }
        }
        return count;
    }

    private int migrateFolderRecursive(File folder, String defaultCategory) {
        int count = 0;
        File[] files = folder.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String subCategory = file.getParentFile().equals(folder)
                    ? file.getName().toUpperCase() : defaultCategory;
                count += migrateFolderRecursive(file, subCategory);
            } else if (file.getName().endsWith(".yml")) {
                count += migrateFileEntries(file, defaultCategory);
            }
        }
        return count;
    }

    private int migrateFileEntries(File file, String defaultCategory) {
        int count = 0;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            if (!config.isConfigurationSection(key)) {
                continue;
            }
            try {
                ConfigurationSection section = config.getConfigurationSection(key);
                String cat = section.getString("type",
                    section.getString("category", defaultCategory));
                String yamlData = serializeSection(section);
                repository.save(key, cat, yamlData, "migration").join();
                count++;
            } catch (Exception e) {
                MidgardLogger.error("[Migration:" + moduleName + "] Erro ao migrar " + key + " de " + file.getName(), e);
            }
        }
        return count;
    }

    /**
     * Migra um config.yml inteiro como uma única entrada no banco.
     *
     * @param file     Arquivo config.yml
     * @param configId ID para a entrada (ex: "config", "combat_config")
     * @return 1 se migrado, 0 se não
     */
    public int migrateWholeConfig(File file, String configId) {
        if (!file.exists()) {
            return 0;
        }
        if (repository.count() > 0) {
            MidgardLogger.info("[Migration:" + moduleName + "] Banco já contém dados. Pulando migração.");
            return 0;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String yamlData = config.saveToString();

        try {
            repository.save(configId, "config", yamlData, "migration").join();
            backupFile(file);
            MidgardLogger.info("[Migration:" + moduleName + "] Config migrado para o banco.");
            return 1;
        } catch (Exception e) {
            MidgardLogger.error("[Migration:" + moduleName + "] Erro ao migrar config", e);
            return 0;
        }
    }

    private void backupFile(File file) {
        File backup = new File(file.getParentFile(),
            file.getName().replace(".yml", "_backup.yml"));
        if (!backup.exists()) {
            try {
                java.nio.file.Files.move(file.toPath(), backup.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.io.IOException e) {
                MidgardLogger.error("[Migration:" + moduleName + "] Failed to backup file: " + file.getName(), e);
            }
        }
    }

    /**
     * Serializa uma ConfigurationSection para YAML string.
     */
    public static String serializeSection(ConfigurationSection section) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (String key : section.getKeys(true)) {
            if (!section.isConfigurationSection(key)) {
                yaml.set(key, section.get(key));
            }
        }
        return yaml.saveToString();
    }

    /**
     * Desserializa uma YAML string para ConfigurationSection embutida em um root.
     */
    public static ConfigurationSection deserializeToSection(String id, String yamlData) {
        try {
            YamlConfiguration root = new YamlConfiguration();
            YamlConfiguration temp = new YamlConfiguration();
            temp.loadFromString(yamlData);
            ConfigurationSection section = root.createSection(id);
            for (String key : temp.getKeys(true)) {
                if (!temp.isConfigurationSection(key)) {
                    section.set(key, temp.get(key));
                }
            }
            return section;
        } catch (Exception e) {
            MidgardLogger.error("Erro ao desserializar YAML para " + id, e);
            return null;
        }
    }

    /**
     * Desserializa YAML string para FileConfiguration standalone.
     */
    public static FileConfiguration deserializeToConfig(String yamlData) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yamlData);
            return config;
        } catch (Exception e) {
            MidgardLogger.error("Erro ao desserializar config YAML", e);
            return new YamlConfiguration();
        }
    }
}
