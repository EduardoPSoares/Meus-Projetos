package me.ray.midgard.modules.essentials.manager;

import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.database.DefinitionMigrationTool;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.essentials.config.EssentialsConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;

public class WarpManager {

    private final JavaPlugin plugin;
    private final EssentialsConfig config;
    private final Map<String, Location> warps = new java.util.concurrent.ConcurrentHashMap<>();
    private DefinitionRepository repository;
    private java.util.function.Consumer<String> onUpdateNotify;
    private java.util.function.Consumer<String> onDeleteNotify;

    public WarpManager(JavaPlugin plugin, EssentialsConfig config) {
        this.plugin = plugin;
        this.config = config;
        
        // Initialize DB repository + migrate warps if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new DefinitionRepository(dbManager, "midgard_warps");
            migrateWarpsFromConfig();
        }
        
        loadWarps();
    }

    private void migrateWarpsFromConfig() {
        if (repository == null || repository.count() > 0) {
            return;
        }
        ConfigurationSection section = config.getConfig().getConfigurationSection("warps");
        if (section == null) {
            return;
        }
        int count = 0;
        for (String key : section.getKeys(false)) {
            ConfigurationSection warpSection = section.getConfigurationSection(key);
            if (warpSection != null) {
                String yaml = DefinitionMigrationTool.serializeSection(warpSection);
                repository.save(key.toLowerCase(), "warp", yaml, "migration").join();
                count++;
            }
        }
        if (count > 0) {
            MidgardLogger.info("[Migration:warps] " + count + " warps migrados para o banco.");
        }
    }

    public void loadWarps() {
        warps.clear();
        
        // Try DB first
        if (repository != null && repository.count() > 0) {
            Map<String, DefinitionRepository.DefinitionData> dbWarps = repository.loadAll();
            for (Map.Entry<String, DefinitionRepository.DefinitionData> entry : dbWarps.entrySet()) {
                try {
                    YamlConfiguration yaml = new YamlConfiguration();
                    yaml.loadFromString(entry.getValue().yamlData());
                    World world = Bukkit.getWorld(yaml.getString("world"));
                    if (world != null) {
                        warps.put(entry.getKey(), new Location(world,
                            yaml.getDouble("x"), yaml.getDouble("y"), yaml.getDouble("z"),
                            (float) yaml.getDouble("yaw"), (float) yaml.getDouble("pitch")));
                    }
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar warp " + entry.getKey() + " do banco", e);
                }
            }
            return;
        }
        
        // Fallback: load from config
        ConfigurationSection section = config.getConfig().getConfigurationSection("warps");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection warpSection = section.getConfigurationSection(key);
            if (warpSection != null) {
                try {
                    World world = Bukkit.getWorld(warpSection.getString("world"));
                    double x = warpSection.getDouble("x");
                    double y = warpSection.getDouble("y");
                    double z = warpSection.getDouble("z");
                    float yaw = (float) warpSection.getDouble("yaw");
                    float pitch = (float) warpSection.getDouble("pitch");
                    
                    if (world != null) {
                        warps.put(key.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
                    }
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar warp: " + key, e);
                }
            }
        }
    }

    public void setWarp(String name, Location location) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Nome de warp inválido. Use apenas letras e números.");
        }
        if (location.getWorld() == null) {
            return;
        }
        
        String key = name.toLowerCase();
        warps.put(key, location);
        
        // Save to DB
        if (repository != null) {
            String yaml = serializeLocation(location);
            repository.save(key, "warp", yaml, "admin").thenRun(() -> {
                if (onUpdateNotify != null) {
                    onUpdateNotify.accept(key);
                }
            });
        } else {
            // Fallback: save to config
            String path = "warps." + key;
            synchronized (config) {
                config.getConfig().set(path + ".world", location.getWorld().getName());
                config.getConfig().set(path + ".x", location.getX());
                config.getConfig().set(path + ".y", location.getY());
                config.getConfig().set(path + ".z", location.getZ());
                config.getConfig().set(path + ".yaw", location.getYaw());
                config.getConfig().set(path + ".pitch", location.getPitch());
                Task.async(() -> {
                    synchronized (config) {
                        config.save();
                    }
                });
            }
        }
    }

    public void deleteWarp(String name) {
        String key = name.toLowerCase();
        warps.remove(key);
        
        if (repository != null) {
            repository.delete(key).thenRun(() -> {
                if (onDeleteNotify != null) {
                    onDeleteNotify.accept(key);
                }
            });
        } else {
            synchronized (config) {
                config.getConfig().set("warps." + key, null);
                Task.async(() -> {
                    synchronized (config) {
                        config.save();
                    }
                });
            }
        }
    }
    
    private String serializeLocation(Location loc) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("world", loc.getWorld().getName());
        yaml.set("x", loc.getX());
        yaml.set("y", loc.getY());
        yaml.set("z", loc.getZ());
        yaml.set("yaw", (double) loc.getYaw());
        yaml.set("pitch", (double) loc.getPitch());
        return yaml.saveToString();
    }

    public void reloadWarpFromDb(String warpId, DefinitionRepository.DefinitionData data) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(data.yamlData());
            World world = Bukkit.getWorld(yaml.getString("world"));
            if (world != null) {
                warps.put(warpId, new Location(world,
                    yaml.getDouble("x"), yaml.getDouble("y"), yaml.getDouble("z"),
                    (float) yaml.getDouble("yaw"), (float) yaml.getDouble("pitch")));
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao recarregar warp " + warpId + " do banco", e);
        }
    }

    public void unregisterWarp(String warpId) {
        warps.remove(warpId);
    }

    public DefinitionRepository getRepository() {
        return repository;
    }

    public void setSyncNotifiers(java.util.function.Consumer<String> onUpdate, java.util.function.Consumer<String> onDelete) {
        this.onUpdateNotify = onUpdate;
        this.onDeleteNotify = onDelete;
    }
    
    private boolean isValidName(String name) {
        return name.matches("[a-zA-Z0-9_]+");
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public Set<String> getWarps() {
        return warps.keySet();
    }
}
