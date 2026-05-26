package me.ray.midgard.modules.classes.skilltree;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillTreeManager {

    private final JavaPlugin plugin;
    private final Map<String, SkillTree> skillTrees = new HashMap<>();
    private final File folder;

    public SkillTreeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "modules/classes/skill-trees");
        loadSkillTrees();
    }

    public void reload() {
        loadSkillTrees();
    }

    private void loadSkillTrees() {
        skillTrees.clear();
        if (!folder.exists()) {
            folder.mkdirs();
            // Save resource if available
            saveDefaultTree("combat.yml");
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String id = config.getString("id", file.getName().replace(".yml", ""));
                String name = config.getString("name", id);
                List<String> lore = config.getStringList("lore");
                int maxPoints = config.getInt("max-point-spent", 20);

                org.bukkit.inventory.ItemStack icon = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_AXE);
                if (config.contains("item")) {
                    try {
                        icon = new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(config.getString("item")));
                    } catch (Exception e) {
                        MidgardLogger.warn("Material inválido em skill tree " + id + ": " + config.getString("item"));
                    }
                }

                SkillTree tree = new SkillTree(id, name, lore, icon, maxPoints);

                ConfigurationSection nodesSection = config.getConfigurationSection("nodes");
                if (nodesSection != null) {
                    for (String nodeId : nodesSection.getKeys(false)) {
                        ConfigurationSection node = nodesSection.getConfigurationSection(nodeId);
                        if (node == null) {
                            continue;
                        }
                        String nodeName = node.getString("name", nodeId);
                        
                        // Coordinates
                        int x = 0;
                        int y = 0;
                        if (node.contains("coordinates")) {
                             if (node.isConfigurationSection("coordinates")) {
                                 x = node.getInt("coordinates.x");
                                 y = node.getInt("coordinates.y");
                             } else {
                                 String[] coords = node.getString("coordinates").split(",");
                                 if (coords.length >= 2) {
                                     try {
                                         x = Integer.parseInt(coords[0].trim());
                                         y = Integer.parseInt(coords[1].trim());
                                     } catch (NumberFormatException e) {
                                         MidgardLogger.debug("Coordenadas inválidas no nó " + nodeId + " da skill tree");
                                     }
                                 }
                             }
                        }

                        boolean root = node.getBoolean("root", false);

                        // Parents
                        Map<String, Integer> parents = new HashMap<>();
                        if (node.isConfigurationSection("parents.soft")) {
                            for (String parentId : node.getConfigurationSection("parents.soft").getKeys(false)) {
                                parents.put(parentId, node.getInt("parents.soft." + parentId));
                            }
                        }
                        // TODO: Implement strong parents logic if different

                        // Lore per level
                        Map<Integer, List<String>> lorePerLevel = new HashMap<>();
                        if (node.isConfigurationSection("lores")) {
                            for (String key : node.getConfigurationSection("lores").getKeys(false)) {
                                try {
                                    int lvl = Integer.parseInt(key);
                                    lorePerLevel.put(lvl, node.getStringList("lores." + key));
                                } catch (NumberFormatException e) {
                                    MidgardLogger.debug("Chave de lore inválida '" + key + "' no nó " + nodeId);
                                }
                            }
                        }
                        
                        // Calculate max level based on lores or triggers
                        int maxLevel = lorePerLevel.keySet().stream().mapToInt(v -> v).max().orElse(1);

                        SkillTreeNode treeNode = new SkillTreeNode(nodeId, nodeName, lorePerLevel, maxLevel, x, y, root, parents);
                        tree.addNode(treeNode);
                    }
                }

                skillTrees.put(id, tree);
                MidgardLogger.info("Loaded Skill Tree: " + id);

            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar skill tree do arquivo: " + file.getName(), e);
            }
        }
    }

    public SkillTree getSkillTree(String id) {
        return skillTrees.get(id);
    }
    
    public Map<String, SkillTree> getSkillTrees() {
        return skillTrees;
    }
    
    private void saveDefaultTree(String filename) {
        if (plugin.getResource("modules/classes/skill-trees/" + filename) != null) {
            String targetPath = "modules/classes/skill-trees/" + filename;
            File targetFile = new File(plugin.getDataFolder(), targetPath);
            if (!targetFile.exists()) {
                plugin.saveResource(targetPath, false);
            }
        }
    }
}
