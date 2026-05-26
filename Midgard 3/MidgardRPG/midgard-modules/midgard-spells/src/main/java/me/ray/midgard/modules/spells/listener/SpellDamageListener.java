package me.ray.midgard.modules.spells.listener;

import me.ray.midgard.modules.spells.SpellsModule;
import me.ray.midgard.modules.spells.data.SpellProfile;
import me.ray.midgard.modules.spells.obj.Spell;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellDamageListener implements Listener {

    private final SpellsModule module;

    // Track recent spell casts: Player UUID -> (Spell ID -> timestamp)
    private final Map<UUID, Map<String, Long>> recentCasts = new ConcurrentHashMap<>();
    private static final long CAST_TRACKING_WINDOW = 5000; // 5 seconds

    public SpellDamageListener(SpellsModule module) {
        this.module = module;
    }

    public void trackCast(Player player, String spellId) {
        recentCasts.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(spellId, System.currentTimeMillis());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) { return; }

        Map<String, Long> casts = recentCasts.get(killer.getUniqueId());
        if (casts == null || casts.isEmpty()) { return; }

        long now = System.currentTimeMillis();
        boolean isPvP = event.getEntity().getType() == EntityType.PLAYER;

        for (Map.Entry<String, Long> entry : casts.entrySet()) {
            if (now - entry.getValue() < CAST_TRACKING_WINDOW) {
                Spell spell = module.getSpellManager().getSpell(entry.getKey());
                if (spell != null) {
                    module.getSpellManager().getXPManager().grantKillBonusXP(killer, spell, isPvP);

                    SpellProfile profile = module.getSpellManager().getProfile(killer);
                    if (profile != null) {
                        profile.getSpellStats(entry.getKey()).incrementKills();
                    }
                }
            }
        }

        // Cleanup old entries
        casts.entrySet().removeIf(e -> now - e.getValue() >= CAST_TRACKING_WINDOW);
    }

    public void cleanup(UUID uuid) {
        recentCasts.remove(uuid);
    }
}
