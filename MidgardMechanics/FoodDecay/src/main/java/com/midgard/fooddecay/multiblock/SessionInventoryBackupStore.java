package com.midgard.fooddecay.multiblock;

import com.midgard.core.MidgardCore;
import com.midgard.core.utils.MessageUtils;
import com.midgard.fooddecay.FoodDecayConfig;
import com.midgard.fooddecay.FoodDecayPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

import static com.midgard.core.utils.MessageUtils.sc;

final class SessionInventoryBackupStore {

    private final FoodDecayConfig config;
    private final File sessionDir;

    SessionInventoryBackupStore(FoodDecayPlugin plugin, FoodDecayConfig config) {
        this.config = config;
        this.sessionDir = new File(plugin.getDataFolder(), "sessions");
    }

    void save(UUID playerId, ItemStack[] inventory) {
        File file = backupFile(playerId);
        YamlConfiguration data = new YamlConfiguration();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null) {
                data.set("inv." + i, inventory[i]);
            }
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            data.save(file);
        } catch (IOException e) {
            MidgardCore.getInstance().getLogger().log(Level.WARNING,
                    "[FoodDecay] Failed to save inventory backup for " + playerId, e);
        }
    }

    void delete(UUID playerId) {
        File file = backupFile(playerId);
        if (file.exists()) {
            file.delete();
        }
    }

    boolean restore(Player player) {
        File file = backupFile(player.getUniqueId());
        if (!file.exists()) {
            return false;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        var section = data.getConfigurationSection("inv");
        if (section == null) {
            file.delete();
            return false;
        }

        ItemStack[] inventory = new ItemStack[player.getInventory().getContents().length];
        for (String key : section.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (slot >= 0 && slot < inventory.length) {
                inventory[slot] = data.getItemStack("inv." + key);
            }
        }

        player.getInventory().setContents(inventory);
        file.delete();
        MidgardCore.getInstance().getLogger().info(
                "[FoodDecay] Restored crash-saved inventory for " + player.getName());
        player.sendMessage(MessageUtils.toComponent(sc(config.msg("session-inventory-restored"))));
        return true;
    }

    private File backupFile(UUID playerId) {
        return new File(sessionDir, playerId + ".yml");
    }
}
