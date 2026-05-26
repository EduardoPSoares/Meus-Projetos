package me.ray.midgard.modules.races.trait;

import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;

import java.util.List;
import java.util.Map;

public class BiomeBuffTrait implements RaceTrait {

    @Override
    public String getId() {
        return "biome_buff";
    }

    @Override
    @SuppressWarnings("removal")
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (trigger == TraitTrigger.ON_REMOVE || trigger == TraitTrigger.ON_QUIT) {
            removeEffects(player, config);
            return;
        }

        if (trigger != TraitTrigger.PASSIVE_TICK) { return; }

        Object biomesObj = config.get("biomes");
        if (!(biomesObj instanceof List)) { return; }
        List<?> biomes = (List<?>) biomesObj;

        Biome currentBiome = player.getLocation().getBlock().getBiome();
        boolean match = false;
        
        for (Object b : biomes) {
            // Check for exact match or contains (e.g. "FOREST" matches "BIRCH_FOREST")
            String biomeName = b.toString().toUpperCase();
            String currentName = currentBiome.name();
            
            if (currentName.equals(biomeName) || currentName.contains(biomeName)) {
                match = true;
                break;
            }
        }

        if (match) {
            applyEffects(player, config);
        }
    }

    private void applyEffects(Player player, Map<String, Object> config) {
        Object effectsObj = config.get("effects");
        if (effectsObj instanceof Map) {
            Map<?, ?> effects = (Map<?, ?>) effectsObj;
            for (Map.Entry<?, ?> entry : effects.entrySet()) {
                PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(entry.getKey().toString().toLowerCase()));
                if (type != null) {
                    int amp = (entry.getValue() instanceof Number n) ? n.intValue() : 0;
                    player.addPotionEffect(new PotionEffect(type, 60, amp, false, false, true));
                }
            }
        }
        
        // Single effect fallback
        Object effectNameObj = config.get("effect");
        if (effectNameObj instanceof String effectName) {
            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(effectName.toLowerCase()));
            int amp = (config.get("amplifier") instanceof Number n) ? n.intValue() : 0;
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type, 60, amp, false, false, true));
            }
        }
    }

    private void removeEffects(Player player, Map<String, Object> config) {
         Object effectsObj = config.get("effects");
        if (effectsObj instanceof Map) {
            Map<?, ?> effects = (Map<?, ?>) effectsObj;
            for (Object key : effects.keySet()) {
                PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(key.toString().toLowerCase()));
                if (type != null) { player.removePotionEffect(type); }
            }
        }

        Object effectNameObj = config.get("effect");
        if (effectNameObj instanceof String effectName) {
            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(effectName.toLowerCase()));
            if (type != null) { player.removePotionEffect(type); }
        }
    }
}
