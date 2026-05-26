package midgardvanish.data;

import midgardvanish.MidgardVanish;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VanishSettingsManager {

    public enum VanishSetting {
        NO_DAMAGE("no-damage", "ᴅᴀɴᴏ", "§7Impede que você receba e cause dano",
                org.bukkit.Material.DIAMOND_SWORD, org.bukkit.Material.WOODEN_SWORD),
        NO_BLOCK_PLACE("no-block-place", "ᴄᴏʟᴏᴄᴀʀ ʙʟᴏᴄᴏs", "§7Impede que você coloque blocos",
                org.bukkit.Material.GRASS_BLOCK, org.bukkit.Material.DIRT),
        NO_BLOCK_BREAK("no-block-break", "ǫᴜᴇʙʀᴀʀ ʙʟᴏᴄᴏs", "§7Impede que você quebre blocos",
                org.bukkit.Material.DIAMOND_PICKAXE, org.bukkit.Material.WOODEN_PICKAXE),
        NO_ITEM_PICKUP("no-item-pickup", "ᴄᴏʟᴇᴛᴀʀ ɪᴛᴇɴs", "§7Impede que você colete itens",
                org.bukkit.Material.HOPPER, org.bukkit.Material.CAULDRON),
        NO_ITEM_DROP("no-item-drop", "ᴅʀᴏᴘᴀʀ ɪᴛᴇɴs", "§7Impede que você drope itens",
                org.bukkit.Material.DROPPER, org.bukkit.Material.DISPENSER),
        NO_HUNGER("no-hunger", "ꜰᴏᴍᴇ", "§7Impede que sua fome diminua",
                org.bukkit.Material.GOLDEN_APPLE, org.bukkit.Material.ROTTEN_FLESH),
        NO_MOB_TARGET("no-mob-target", "ᴍᴏʙs", "§7Impede que mobs te ataquem",
                org.bukkit.Material.TOTEM_OF_UNDYING, org.bukkit.Material.ZOMBIE_HEAD),
        SILENT_CHEST("silent-chest", "ᴄᴏɴᴛᴀɪɴᴇʀs sɪʟᴇɴᴄɪᴏsᴏs", "§7Abre containers sem som/animação",
                org.bukkit.Material.CHEST, org.bukkit.Material.TRAPPED_CHEST),
        SILENT_DOOR("silent-door", "ᴘᴏʀᴛᴀs sɪʟᴇɴᴄɪᴏsᴀs", "§7Abre portas/alavancas silenciosamente",
                org.bukkit.Material.OAK_DOOR, org.bukkit.Material.IRON_DOOR);

        private final String configKey;
        private final String displayName;
        private final String description;
        private final org.bukkit.Material iconEnabled;
        private final org.bukkit.Material iconDisabled;

        VanishSetting(String configKey, String displayName, String description, org.bukkit.Material iconEnabled, org.bukkit.Material iconDisabled) {
            this.configKey = configKey;
            this.displayName = displayName;
            this.description = description;
            this.iconEnabled = iconEnabled;
            this.iconDisabled = iconDisabled;
        }

        public String getConfigKey() { return configKey; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public org.bukkit.Material getIconEnabled() { return iconEnabled; }
        public org.bukkit.Material getIconDisabled() { return iconDisabled; }
    }

    private final File file;
    private YamlConfiguration config;
    private final Map<UUID, Set<VanishSetting>> playerSettings = new HashMap<>();

    public VanishSettingsManager(MidgardVanish plugin) {
        this.file = new File(plugin.getDataFolder(), "vanish_settings.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            config = new YamlConfiguration();
        } else {
            config = YamlConfiguration.loadConfiguration(file);
        }
        playerSettings.clear();

        if (config.contains("settings")) {
            for (String uuidStr : config.getConfigurationSection("settings").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Set<VanishSetting> settings = EnumSet.noneOf(VanishSetting.class);
                for (VanishSetting setting : VanishSetting.values()) {
                    if (config.getBoolean("settings." + uuidStr + "." + setting.getConfigKey(), true)) {
                        settings.add(setting);
                    }
                }
                playerSettings.put(uuid, settings);
            }
        }
    }

    public void save() {
        for (Map.Entry<UUID, Set<VanishSetting>> entry : playerSettings.entrySet()) {
            String path = "settings." + entry.getKey().toString();
            for (VanishSetting setting : VanishSetting.values()) {
                config.set(path + "." + setting.getConfigKey(), entry.getValue().contains(setting));
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isEnabled(UUID player, VanishSetting setting) {
        Set<VanishSetting> settings = playerSettings.get(player);
        if (settings == null) return true; // All enabled by default
        return settings.contains(setting);
    }

    public void toggle(UUID player, VanishSetting setting) {
        Set<VanishSetting> settings = playerSettings.computeIfAbsent(player, k -> {
            Set<VanishSetting> defaults = EnumSet.allOf(VanishSetting.class);
            return defaults;
        });
        if (settings.contains(setting)) {
            settings.remove(setting);
        } else {
            settings.add(setting);
        }
    }

    public Set<VanishSetting> getSettings(UUID player) {
        return playerSettings.computeIfAbsent(player, k -> EnumSet.allOf(VanishSetting.class));
    }
}
