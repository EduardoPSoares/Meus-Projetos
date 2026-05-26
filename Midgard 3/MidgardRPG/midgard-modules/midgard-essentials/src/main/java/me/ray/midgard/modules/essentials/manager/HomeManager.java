package me.ray.midgard.modules.essentials.manager;

import me.ray.midgard.core.database.DefinitionRepository;
import me.ray.midgard.core.config.ConfigWrapper;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.essentials.config.EssentialsConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HomeManager {

    private final JavaPlugin plugin;
    private final ConfigWrapper homesConfig;
    private final EssentialsConfig config;
    // Cache: UUID -> (HomeName -> Location)
    private final Map<UUID, Map<String, Location>> homesCache = new java.util.concurrent.ConcurrentHashMap<>();
    private DefinitionRepository repository;
    private java.util.function.Consumer<String> onUpdateNotify;
    private java.util.function.Consumer<String> onDeleteNotify;

    public HomeManager(JavaPlugin plugin, EssentialsConfig config) {
        this.plugin = plugin;
        this.homesConfig = new ConfigWrapper(plugin, "data/homes.yml");
        this.config = config;
        
        // Initialize DB repository + migrate homes if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new DefinitionRepository(dbManager, "midgard_homes");
            migrateHomesFromConfig();
        }
        
        loadHomes();
    }

    private void migrateHomesFromConfig() {
        if (repository == null || repository.count() > 0) {
            return;
        }
        ConfigurationSection section = homesConfig.getConfig().getConfigurationSection("homes");
        if (section == null) {
            return;
        }
        int count = 0;
        for (String uuidStr : section.getKeys(false)) {
            ConfigurationSection playerSection = section.getConfigurationSection(uuidStr);
            if (playerSection == null) {
                continue;
            }
            for (String homeName : playerSection.getKeys(false)) {
                ConfigurationSection homeLoc = playerSection.getConfigurationSection(homeName);
                if (homeLoc != null) {
                    String id = uuidStr + ":" + homeName.toLowerCase();
                    String yaml = me.ray.midgard.core.database.DefinitionMigrationTool.serializeSection(homeLoc);
                    repository.save(id, "home", yaml, "migration").join();
                    count++;
                }
            }
        }
        if (count > 0) {
            MidgardLogger.info("[Migration:homes] " + count + " homes migrados para o banco.");
        }
    }

    public void loadHomes() {
        homesCache.clear();
        
        // Try DB first
        if (repository != null && repository.count() > 0) {
            Map<String, DefinitionRepository.DefinitionData> dbHomes = repository.loadAll();
            for (Map.Entry<String, DefinitionRepository.DefinitionData> entry : dbHomes.entrySet()) {
                try {
                    String[] parts = entry.getKey().split(":", 2);
                    if (parts.length != 2) {
                        continue;
                    }
                    UUID uuid = UUID.fromString(parts[0]);
                    String homeName = parts[1];
                    
                    YamlConfiguration yaml = new YamlConfiguration();
                    yaml.loadFromString(entry.getValue().yamlData());
                    World world = Bukkit.getWorld(yaml.getString("world"));
                    if (world != null) {
                        homesCache.computeIfAbsent(uuid, k -> new java.util.concurrent.ConcurrentHashMap<>())
                            .put(homeName, new Location(world,
                                yaml.getDouble("x"), yaml.getDouble("y"), yaml.getDouble("z"),
                                (float) yaml.getDouble("yaw"), (float) yaml.getDouble("pitch")));
                    }
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao carregar home " + entry.getKey() + " do banco", e);
                }
            }
            return;
        }
        
        // Fallback: load from config
        ConfigurationSection section = homesConfig.getConfig().getConfigurationSection("homes");
        if (section == null) {
            return;
        }

        for (String uuidStr : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = section.getConfigurationSection(uuidStr);
                if (playerSection == null) {
                    continue;
                }

                Map<String, Location> playerHomes = new java.util.concurrent.ConcurrentHashMap<>();
                for (String homeName : playerSection.getKeys(false)) {
                    ConfigurationSection homeLoc = playerSection.getConfigurationSection(homeName);
                    if (homeLoc != null) {
                        World world = Bukkit.getWorld(homeLoc.getString("world"));
                        double x = homeLoc.getDouble("x");
                        double y = homeLoc.getDouble("y");
                        double z = homeLoc.getDouble("z");
                        float yaw = (float) homeLoc.getDouble("yaw");
                        float pitch = (float) homeLoc.getDouble("pitch");

                        if (world != null) {
                            playerHomes.put(homeName.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
                        }
                    }
                }
                homesCache.put(uuid, playerHomes);
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar homes do jogador: " + uuidStr, e);
            }
        }
    }

    public void setHome(Player player, String homeName, Location location) {
        if (location.getWorld() == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        homesCache.computeIfAbsent(uuid, k -> new java.util.concurrent.ConcurrentHashMap<>()).put(homeName.toLowerCase(), location);

        if (repository != null) {
            String id = uuid.toString() + ":" + homeName.toLowerCase();
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("world", location.getWorld().getName());
            yaml.set("x", location.getX());
            yaml.set("y", location.getY());
            yaml.set("z", location.getZ());
            yaml.set("yaw", (double) location.getYaw());
            yaml.set("pitch", (double) location.getPitch());
            repository.save(id, "home", yaml.saveToString(), player.getName()).thenRun(() -> {
                if (onUpdateNotify != null) {
                    onUpdateNotify.accept(id);
                }
            });
        } else {
            String path = "homes." + uuid.toString() + "." + homeName.toLowerCase();
            synchronized (homesConfig) {
                homesConfig.getConfig().set(path + ".world", location.getWorld().getName());
                homesConfig.getConfig().set(path + ".x", location.getX());
                homesConfig.getConfig().set(path + ".y", location.getY());
                homesConfig.getConfig().set(path + ".z", location.getZ());
                homesConfig.getConfig().set(path + ".yaw", location.getYaw());
                homesConfig.getConfig().set(path + ".pitch", location.getPitch());
                Task.async(() -> {
                    synchronized (homesConfig) {
                        homesConfig.saveConfig();
                    }
                });
            }
        }
    }

    public void deleteHome(Player player, String homeName) {
        UUID uuid = player.getUniqueId();
        if (homesCache.containsKey(uuid)) {
            homesCache.get(uuid).remove(homeName.toLowerCase());
        }
        
        if (repository != null) {
            String id = uuid.toString() + ":" + homeName.toLowerCase();
            repository.delete(id).thenRun(() -> {
                if (onDeleteNotify != null) {
                    onDeleteNotify.accept(id);
                }
            });
        } else {
            synchronized (homesConfig) {
                homesConfig.getConfig().set("homes." + uuid.toString() + "." + homeName.toLowerCase(), null);
                Task.async(() -> {
                    synchronized (homesConfig) {
                        homesConfig.saveConfig();
                    }
                });
            }
        }
    }

    public DefinitionRepository getRepository() {
        return repository;
    }

    public void setSyncNotifiers(java.util.function.Consumer<String> onUpdate, java.util.function.Consumer<String> onDelete) {
        this.onUpdateNotify = onUpdate;
        this.onDeleteNotify = onDelete;
    }

    public Location getHome(Player player, String homeName) {
        Map<String, Location> playerHomes = homesCache.get(player.getUniqueId());
        if (playerHomes != null) {
            return playerHomes.get(homeName.toLowerCase());
        }
        return null;
    }

    public Set<String> getHomes(Player player) {
        Map<String, Location> playerHomes = homesCache.get(player.getUniqueId());
        if (playerHomes != null) {
            return playerHomes.keySet();
        }
        return Set.of();
    }
    
    public int getHomeLimit(Player player) {
        return config.getConfig().getInt("homes.limit", 3);
    }
    
    public int getHomeCount(Player player) {
        Map<String, Location> playerHomes = homesCache.get(player.getUniqueId());
        return playerHomes == null ? 0 : playerHomes.size();
    }
}
