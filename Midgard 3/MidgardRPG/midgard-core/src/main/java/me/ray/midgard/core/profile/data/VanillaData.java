package me.ray.midgard.core.profile.data;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.ModuleData;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VanillaData implements ModuleData {

    private String inventoryBase64;
    private String enderChestBase64;
    private double health;
    private int foodLevel;
    private float saturation;
    private float exp;
    private int level;
    private String gameMode;
    private boolean isFlying;
    private List<Map<String, Object>> activeEffects;
    // private double [] location; // x, y, z, pitch, yaw (optional, if we want position sync)

    public VanillaData() {
        // Default constructor for Gson
    }

    public static VanillaData fromPlayer(Player player) {
        VanillaData data = new VanillaData();
        data.health = player.getHealth();
        data.foodLevel = player.getFoodLevel();
        data.saturation = player.getSaturation();
        data.exp = player.getExp();
        data.level = player.getLevel();
        data.gameMode = player.getGameMode().name();
        data.isFlying = player.isFlying();
        data.activeEffects = player.getActivePotionEffects().stream()
                .map(PotionEffect::serialize)
                .collect(Collectors.toList());
        
        data.inventoryBase64 = itemStackArrayToBase64(player.getInventory().getContents());
        data.enderChestBase64 = itemStackArrayToBase64(player.getEnderChest().getContents());
        
        return data;
    }

    public void applyTo(Player player) {
        @SuppressWarnings("deprecation")
        double maxHealth = player.getMaxHealth();
        player.setHealth(Math.max(0.0, Math.min(health, maxHealth)));
        player.setFoodLevel(Math.max(0, Math.min(foodLevel, 20)));
        player.setSaturation(Math.max(0f, Math.min(saturation, (float) foodLevel)));
        player.setExp(Math.max(0f, Math.min(exp, 1f)));
        player.setLevel(Math.max(0, level));
        try {
            player.setGameMode(GameMode.valueOf(gameMode));
        } catch (IllegalArgumentException e) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        
        // Ensure allowFlight is set before setFlying to avoid IllegalArgumentException
        if (isFlying || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            player.setAllowFlight(true);
        }
        player.setFlying(isFlying);

        // Effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        if (activeEffects != null) {
            for (Map<String, Object> serialized : activeEffects) {
                try {
                    // Fix: Convert double duration to int
                    if (serialized.containsKey("duration") && serialized.get("duration") instanceof Number) {
                        Number duration = (Number) serialized.get("duration");
                        serialized.put("duration", duration.intValue());
                    }
                    if (serialized.containsKey("amplifier") && serialized.get("amplifier") instanceof Number) {
                        Number amplifier = (Number) serialized.get("amplifier");
                        serialized.put("amplifier", amplifier.intValue());
                    }
                    player.addPotionEffect(new PotionEffect(serialized));
                } catch (Exception e) {
                    MidgardLogger.error("Erro ao restaurar efeito de poção", e);
                }
            }
        }

        // Inventory
        if (inventoryBase64 != null && !inventoryBase64.isEmpty()) {
            try {
                ItemStack[] items = itemStackArrayFromBase64(inventoryBase64);
                player.getInventory().setContents(items);
            } catch (Exception e) {
                MidgardLogger.error("Erro ao restaurar inventário do jogador", e);
            }
        }
        
        if (enderChestBase64 != null && !enderChestBase64.isEmpty()) {
            try {
                ItemStack[] items = itemStackArrayFromBase64(enderChestBase64);
                player.getEnderChest().setContents(items);
            } catch (Exception e) {
                MidgardLogger.error("Erro ao restaurar ender chest do jogador", e);
            }
        }
    }

    private static String itemStackArrayToBase64(ItemStack[] items) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("items", items);
            return config.saveToString();
        } catch (Exception e) {
            MidgardLogger.error("Erro ao serializar inventário", e);
            return "";
        }
    }

    private static ItemStack[] itemStackArrayFromBase64(String data) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(data);
            List<?> list = config.getList("items");
            if (list == null) {
                return new ItemStack[0];
            }
            
            // Convert list to array safely
            List<ItemStack> items = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof ItemStack) {
                    items.add((ItemStack) obj);
                } else {
                    items.add(null);
                }
            }
            return items.toArray(new ItemStack[0]);
        } catch (Exception e) {
            MidgardLogger.error("Erro ao desserializar inventário", e);
            return new ItemStack[0];
        }
    }
}
