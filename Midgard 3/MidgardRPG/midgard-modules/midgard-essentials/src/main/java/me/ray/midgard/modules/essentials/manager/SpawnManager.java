package me.ray.midgard.modules.essentials.manager;

import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.essentials.config.EssentialsConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.plugin.java.JavaPlugin;

public class SpawnManager {

    private final JavaPlugin plugin;
    private final EssentialsConfig config;
    private Location spawnLocation;
    private DefinitionRepository repository;
    private Runnable onUpdateNotify;

    public SpawnManager(JavaPlugin plugin, EssentialsConfig config) {
        this.plugin = plugin;
        this.config = config;
        
        // Initialize DB repository + migrate spawn if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new DefinitionRepository(dbManager, "midgard_spawn");
            migrateSpawnFromConfig();
        }
        
        loadSpawn();
    }

    private void migrateSpawnFromConfig() {
        if (repository == null || repository.count() > 0) {
            return;
        }
        ConfigurationSection section = config.getConfig().getConfigurationSection("spawn.location");
        if (section == null) {
            return;
        }
        String yaml = me.ray.midgard.core.database.DefinitionMigrationTool.serializeSection(section);
        repository.save("spawn", "spawn", yaml, "migration").join();
        MidgardLogger.info("[Migration:spawn] Spawn migrado para o banco.");
    }

    public void loadSpawn() {
        // Try DB first
        if (repository != null && repository.count() > 0) {
            DefinitionRepository.DefinitionData data = repository.loadAll().get("spawn");
            if (data != null) {
                try {
                    YamlConfiguration yaml = new YamlConfiguration();
                    yaml.loadFromString(data.yamlData());
                    World world = Bukkit.getWorld(yaml.getString("world"));
                    if (world != null) {
                        spawnLocation = new Location(world,
                            yaml.getDouble("x"), yaml.getDouble("y"), yaml.getDouble("z"),
                            (float) yaml.getDouble("yaw"), (float) yaml.getDouble("pitch"));
                    }
                    return;
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar spawn do banco", e);
                }
            }
        }
        
        // Fallback: load from config
        ConfigurationSection section = config.getConfig().getConfigurationSection("spawn.location");
        if (section != null) {
            try {
                World world = Bukkit.getWorld(section.getString("world"));
                double x = section.getDouble("x");
                double y = section.getDouble("y");
                double z = section.getDouble("z");
                float yaw = (float) section.getDouble("yaw");
                float pitch = (float) section.getDouble("pitch");

                if (world != null) {
                    spawnLocation = new Location(world, x, y, z, yaw, pitch);
                }
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar spawn", e);
            }
        }
    }

    public void setSpawn(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        this.spawnLocation = location;
        
        if (repository != null) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("world", location.getWorld().getName());
            yaml.set("x", location.getX());
            yaml.set("y", location.getY());
            yaml.set("z", location.getZ());
            yaml.set("yaw", (double) location.getYaw());
            yaml.set("pitch", (double) location.getPitch());
            repository.save("spawn", "spawn", yaml.saveToString(), "admin").thenRun(() -> {
                if (onUpdateNotify != null) {
                    onUpdateNotify.run();
                }
            });
        } else {
            String path = "spawn.location";
            config.getConfig().set(path + ".world", location.getWorld().getName());
            config.getConfig().set(path + ".x", location.getX());
            config.getConfig().set(path + ".y", location.getY());
            config.getConfig().set(path + ".z", location.getZ());
            config.getConfig().set(path + ".yaw", location.getYaw());
            config.getConfig().set(path + ".pitch", location.getPitch());
            Task.async(config::save);
        }
    }

    public void reloadSpawnFromDb(DefinitionRepository.DefinitionData data) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(data.yamlData());
            World world = Bukkit.getWorld(yaml.getString("world"));
            if (world != null) {
                spawnLocation = new Location(world,
                    yaml.getDouble("x"), yaml.getDouble("y"), yaml.getDouble("z"),
                    (float) yaml.getDouble("yaw"), (float) yaml.getDouble("pitch"));
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao recarregar spawn do banco", e);
        }
    }

    public DefinitionRepository getRepository() {
        return repository;
    }

    public void setSyncNotifier(Runnable onUpdate) {
        this.onUpdateNotify = onUpdate;
    }

    public Location getSpawn() {
        return spawnLocation;
    }
}
