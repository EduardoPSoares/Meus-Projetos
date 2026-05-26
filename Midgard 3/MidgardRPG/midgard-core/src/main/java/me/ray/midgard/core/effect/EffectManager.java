package me.ray.midgard.core.effect;

import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.profile.ProfileManager;
import me.ray.midgard.core.utils.Task;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class EffectManager {

    private final ProfileManager profileManager;
    private org.bukkit.scheduler.BukkitTask tickTask;

    public EffectManager(JavaPlugin plugin, ProfileManager profileManager) {
        this.profileManager = profileManager;
        startTask();
    }

    private void startTask() {
        // Tick effects every 2 ticks and dispatch per-player to entity scheduler (Folia-safe)
        tickTask = Task.syncTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Task.sync(player, () -> processPlayerEffects(player));
            }
        }, 2L, 2L);
    }
    
    /**
     * Cancels the repeating tick task. Call on module disable to prevent
     * duplicate tasks after reload.
     */
    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }
    
    private void processPlayerEffects(Player player) {
        if (!player.isOnline()) {
            return;
        }
        
        MidgardProfile profile = profileManager.getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }
        
        EffectData data = profile.getData(EffectData.class);
        if (data == null || data.getActiveEffects().isEmpty()) {
            return;
        }
        
        // Use snapshot to avoid ConcurrentModificationException during iteration
        for (ActiveEffect effect : new java.util.ArrayList<>(data.getActiveEffects())) {
            try {
                boolean expired = effect.tick(profile);
                if (expired) {
                    effect.end(profile);
                    data.removeEffect(effect);
                }
            } catch (Exception e) {
                // Log but continue processing remaining effects
                me.ray.midgard.core.debug.MidgardLogger.error("Erro ao processar efeito " + effect.getEffect().getId() + " para " + player.getName(), e);
                data.removeEffect(effect);
            }
        }
    }
    
    public void applyEffect(Player player, StatusEffect effect, long duration) {
        // Dispatch to entity scheduler for Folia safety
        Task.sync(player, () -> {
            MidgardProfile profile = profileManager.getProfile(player.getUniqueId());
            if (profile == null) {
                return;
            }
            
            EffectData data = profile.getOrCreateData(EffectData.class);
            ActiveEffect active = new ActiveEffect(effect, duration, player.getUniqueId());
            data.addEffect(active);
            active.start(profile);
        });
    }
}
