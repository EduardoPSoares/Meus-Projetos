package me.ray.midgard.modules.classes;

import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.database.DefinitionMigrationTool;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import me.ray.midgard.core.debug.MidgardLogger;

/**
 * Gerencia o carregamento e acesso às classes RPG.
 */
public class ClassManager {

    private final JavaPlugin plugin;
    private volatile Map<String, RPGClass> classes = new java.util.concurrent.ConcurrentHashMap<>();
    private final File classesFolder;

    private static final String[][] DEFAULT_CLASSES = {
        {"warriors/guerreiro", "guerreiro", "WARRIORS"},
        {"warriors/guardiao", "guardiao", "WARRIORS"},
        {"mages/mago", "mago", "MAGES"},
        {"mages/necromante", "necromante", "MAGES"},
        {"rogues/arqueiro", "arqueiro", "ROGUES"},
        {"rogues/ladino", "ladino", "ROGUES"},
        {"support/clerigo", "clerigo", "SUPPORT"},
        {"support/monge", "monge", "SUPPORT"}
    };

    /**
     * Construtor do ClassManager.
     *
     * @param plugin Instância do plugin.
     */
    public ClassManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.classesFolder = new File(plugin.getDataFolder(), "modules/classes/classes");
        loadClasses();
    }

    /**
     * Recarrega as classes do disco.
     */
    public void reload() {
        loadClasses();
    }

    private void loadClasses() {
        Map<String, RPGClass> newClasses = new java.util.concurrent.ConcurrentHashMap<>();

        // Try loading from database first
        ClassesModule module = ClassesModule.getInstance();
        DefinitionRepository repo = module != null ? module.getRepository() : null;
        if (repo != null) {
            Map<String, DefinitionRepository.DefinitionData> dbClasses = repo.loadAll();
            for (Map.Entry<String, DefinitionRepository.DefinitionData> entry : dbClasses.entrySet()) {
                try {
                    YamlConfiguration config = (YamlConfiguration) DefinitionMigrationTool.deserializeToConfig(entry.getValue().yamlData());
                    if (config != null) {
                        RPGClass rpgClass = loadClassFromConfig(entry.getKey(), config);
                        if (rpgClass != null) {
                            newClasses.put(entry.getKey(), rpgClass);
                        }
                    }
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar classe " + entry.getKey() + " do banco", e);
                }
            }
            // Seed any missing default classes from JAR resources
            int seeded = seedMissingDefaults(repo, newClasses);
            if (seeded > 0) {
                MidgardLogger.info("[Classes] " + seeded + " classes padrão inseridas no banco.");
            }
            MidgardLogger.info("Carregadas " + newClasses.size() + " classes do banco de dados.");
            this.classes = newClasses;
            return;
        }

        // Fallback: load from YAML files
        try {
            if (!classesFolder.exists()) {
                classesFolder.mkdirs();
                saveDefaultClass("warriors/guerreiro");
                saveDefaultClass("warriors/guardiao");
                saveDefaultClass("mages/mago");
                saveDefaultClass("mages/necromante");
                saveDefaultClass("rogues/arqueiro");
                saveDefaultClass("rogues/ladino");
                saveDefaultClass("support/clerigo");
                saveDefaultClass("support/monge");
            }
    
            List<File> files = collectYmlFilesRecursively(classesFolder);
            if (files.isEmpty()) {
                this.classes = newClasses;
                return;
            }
    
            for (File file : files) {
                try {
                    String key = file.getName().replace(".yml", "");
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    RPGClass rpgClass = loadClassFromConfig(key, config);
                    if (rpgClass != null) {
                        newClasses.put(key, rpgClass);
                    }
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar classe do arquivo: " + file.getName(), e);
                }
            }
            MidgardLogger.info("Foram carregadas " + newClasses.size() + " classes com sucesso.");
            this.classes = newClasses;
        } catch (Exception e) {
            MidgardLogger.error("Erro crítico ao carregar diretório de classes", e);
        }
    }

    /**
     * Carrega uma classe RPG a partir de um ConfigurationSection.
     */
    private RPGClass loadClassFromConfig(String key, ConfigurationSection config) {
        String displayName = config.getString("display-name", key);
        
        org.bukkit.inventory.ItemStack icon = null;
        if (config.contains("nexo") && me.ray.midgard.core.MidgardCore.getInstance().getServer().getPluginManager().isPluginEnabled("Nexo")) {
            try {
                icon = me.ray.midgard.core.integration.NexoUtils.getCustomItem(config.getString("nexo"));
            } catch (Throwable e) {
                // Plugin externo Nexo pode falhar - fallback para ícone padrão
                MidgardLogger.debug("Falha ao carregar item Nexo para classe " + key + ": " + e.getMessage());
            }
        }
        if (icon == null) {
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(config.getString("icon", "BARRIER"));
            if (mat == null) {
                mat = org.bukkit.Material.BARRIER;
            }
            icon = new org.bukkit.inventory.ItemStack(mat);
        }
        if (config.contains("model-data")) {
            me.ray.midgard.core.utils.ItemBuilder ib = new me.ray.midgard.core.utils.ItemBuilder(icon);
            ib.customModelData(config.getInt("model-data"));
            icon = ib.build();
        }

        List<String> lore = config.getStringList("lore");

        Map<String, Double> baseAttributes = new HashMap<>();
        Map<String, Double> perLevelAttributes = new HashMap<>();

        if (config.isConfigurationSection("attributes.base")) {
            ConfigurationSection baseAttrSection = config.getConfigurationSection("attributes.base");
            for (String attr : baseAttrSection.getKeys(false)) {
                baseAttributes.put(attr, baseAttrSection.getDouble(attr));
            }
        } else if (config.isConfigurationSection("attributes")) {
            ConfigurationSection attrSection = config.getConfigurationSection("attributes");
            for (String attr : attrSection.getKeys(false)) {
                if (!attr.equals("base") && !attr.equals("per-level")) {
                    baseAttributes.put(attr, attrSection.getDouble(attr));
                }
            }
        }

        if (config.isConfigurationSection("attributes.per-level")) {
            ConfigurationSection levelAttrSection = config.getConfigurationSection("attributes.per-level");
            for (String attr : levelAttrSection.getKeys(false)) {
                perLevelAttributes.put(attr, levelAttrSection.getDouble(attr));
            }
        } else if (config.isConfigurationSection("per-level.attributes")) {
            ConfigurationSection levelAttrSection = config.getConfigurationSection("per-level.attributes");
            for (String attr : levelAttrSection.getKeys(false)) {
                perLevelAttributes.put(attr, levelAttrSection.getDouble(attr));
            }
        }

        double baseHealth = config.getDouble("health.base", 20);
        double healthPerLevel = config.getDouble("health.per-level", 0);
        if (config.contains("per-level.health")) {
            healthPerLevel = config.getDouble("per-level.health");
        }

        double baseMana = config.getDouble("mana.base", 20);
        double manaPerLevel = config.getDouble("mana.per-level", 0);
        if (config.contains("per-level.mana")) {
            manaPerLevel = config.getDouble("per-level.mana");
        }
        
        List<ClassSkillLink> skills = new ArrayList<>();
        if (config.isConfigurationSection("skills")) {
            ConfigurationSection skillsSection = config.getConfigurationSection("skills");
            for (String skillId : skillsSection.getKeys(false)) {
                int level = 1;
                if (skillsSection.isInt(skillId)) {
                    level = skillsSection.getInt(skillId);
                } else if (skillsSection.isConfigurationSection(skillId)) {
                    level = skillsSection.getInt(skillId + ".level", 1);
                }
                skills.add(new ClassSkillLink(skillId, level));
            }
        }

        return new RPGClass(key, displayName, icon, lore, baseAttributes, perLevelAttributes, baseHealth, healthPerLevel, baseMana, manaPerLevel, skills);
    }

    /**
     * Recarrega uma classe específica a partir de dados do banco.
     */
    public void reloadClassFromDb(String classId, DefinitionRepository.DefinitionData data) {
        try {
            YamlConfiguration config = (YamlConfiguration) DefinitionMigrationTool.deserializeToConfig(data.yamlData());
            if (config != null) {
                RPGClass rpgClass = loadClassFromConfig(classId, config);
                if (rpgClass != null) {
                    classes.put(classId, rpgClass);
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao recarregar classe " + classId + " do banco", e);
        }
    }

    /**
     * Remove uma classe do registro em memória.
     */
    public void unregisterClass(String classId) {
        classes.remove(classId);
    }

    /**
     * Seeds any missing default classes from JAR resources into the database.
     */
    private int seedMissingDefaults(DefinitionRepository repo, Map<String, RPGClass> loadedClasses) {
        int count = 0;
        for (String[] defaultClass : DEFAULT_CLASSES) {
            String path = defaultClass[0];
            String id = defaultClass[1];
            String category = defaultClass[2];

            if (loadedClasses.containsKey(id)) {
                continue;
            }

            String resourcePath = "modules/classes/classes/" + path + ".yml";
            try (InputStream is = plugin.getResource(resourcePath)) {
                if (is == null) {
                    MidgardLogger.warn("[Classes] Recurso não encontrado no JAR: " + resourcePath);
                    continue;
                }
                YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
                String yamlData = config.saveToString();
                repo.save(id, category, yamlData, "default-seed").join();
                RPGClass rpgClass = loadClassFromConfig(id, config);
                if (rpgClass != null) {
                    loadedClasses.put(id, rpgClass);
                    count++;
                }
            } catch (Exception e) {
                MidgardLogger.error("Erro ao inserir classe padrão: " + id, e);
            }
        }
        return count;
    }

    private void saveDefaultClass(String name) {
        String resourcePath = "modules/classes/classes/" + name + ".yml";
        try {
            if (plugin.getResource(resourcePath) != null) {
                File dest = new File(plugin.getDataFolder(), resourcePath);
                if (!dest.exists()) {
                    if (dest.getParentFile() != null) {
                        dest.getParentFile().mkdirs();
                    }
                    plugin.saveResource(resourcePath, false);
                }
            }
        } catch (Exception e) {
            MidgardLogger.error("Não foi possível salvar a classe padrão: " + name, e);
        }
    }

    /**
     * Obtém uma classe pelo ID.
     *
     * @param id ID da classe.
     * @return A classe RPG ou null se não encontrada.
     */
    public RPGClass getClass(String id) {
        return classes.get(id);
    }

    /**
     * Obtém todas as classes carregadas.
     *
     * @return Mapa de classes.
     */
    public Map<String, RPGClass> getClasses() {
        return Collections.unmodifiableMap(classes);
    }

    /**
     * Coleta todos os arquivos .yml recursivamente de uma pasta e suas subpastas.
     *
     * @param folder Pasta raiz para iniciar a busca.
     * @return Lista de arquivos .yml encontrados.
     */
    private List<File> collectYmlFilesRecursively(File folder) {
        List<File> result = new ArrayList<>();
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return result;
        }
        
        File[] contents = folder.listFiles();
        if (contents == null) {
            return result;
        }
        
        for (File file : contents) {
            if (file.isDirectory()) {
                // Recursively scan subdirectories
                result.addAll(collectYmlFilesRecursively(file));
            } else if (file.getName().endsWith(".yml")) {
                result.add(file);
            }
        }
        
        return result;
    }
}
