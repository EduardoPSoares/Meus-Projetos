package midgardvanish.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishDataManager {

    private final File file;
    private FileConfiguration config;
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();

    public VanishDataManager(JavaPlugin plugin) {
        file = new File(plugin.getDataFolder(), "vanished.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        vanishedPlayers.clear();
        if (config.contains("vanished")) {
            for (String uuidStr : config.getStringList("vanished")) {
                try {
                    vanishedPlayers.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void save() {
        config.set("vanished", vanishedPlayers.stream().map(UUID::toString).toList());
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    public void setVanished(UUID uuid, boolean vanished) {
        if (vanished) {
            vanishedPlayers.add(uuid);
        } else {
            vanishedPlayers.remove(uuid);
        }
        save();
    }

    public Set<UUID> getVanishedPlayers() {
        return Set.copyOf(vanishedPlayers);
    }
}
