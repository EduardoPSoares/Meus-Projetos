package me.ray.midgard.modules.combat.level;

import me.ray.midgard.core.integration.MythicMobsIntegration;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.combat.CombatConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener para eventos relacionados ao sistema de níveis.
 * <p>
 * Monitora eventos como morte de entidades para conceder experiência aos jogadores.
 */
public class LevelListener implements Listener {

    private final LevelManager levelManager;
    private final CombatConfig config;

    /**
     * Construtor do LevelListener.
     *
     * @param levelManager Gerenciador de níveis.
     * @param config Configuração do módulo de combate.
     */
    public LevelListener(LevelManager levelManager, CombatConfig config) {
        this.levelManager = levelManager;
        this.config = config;
    }

    /**
     * Evento chamado quando uma entidade morre.
     * Verifica se foi morta por um jogador e concede experiência.
     *
     * @param event O evento de morte da entidade.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.LOW)
    public void onEntityDeath(EntityDeathEvent event) {
        // Remove Vanilla XP Drops (LOW priority so CombatListener's Luck boost at NORMAL runs after)
        event.setDroppedExp(0);

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        
        // 1. Get Player Data
        me.ray.midgard.core.profile.MidgardProfile profile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(killer.getUniqueId());
        if (profile == null) {
            return;
        }
        
        me.ray.midgard.modules.combat.CombatData data = profile.getOrCreateData(me.ray.midgard.modules.combat.CombatData.class);
        int playerLevel = data.getLevel();
        
        // 2. Get Mob Data
        org.bukkit.entity.LivingEntity mob = event.getEntity();
        int mobLevel = getMobLevel(mob);
        double baseMobXp = config.getMobExperience(mob.getType()); // Busca XP específico do mob ou fallback
        
        // Integration with MythicMobs for custom XP
        if (MythicMobsIntegration.isMythicMob(mob)) {
            // Get MythicMob instance
            // We can assume MythicMobs stores experience in its config, 
            // but getting it directly via API is cleaner if available.
            // MythicMobs API: mob.getDrops() returns drops including XP if configured as drop?
            // Or usually people configure "Drops: - exp 50"
            // For now, let's try to get it via variable or just use our formula.
            // Better: Read a variable or tag if MM doesn't expose base XP easily in API v5.
            // Fallback: Use config value if set for specific mob type?
            
            // Let's rely on the formula for now, but allow a multiplier or override via tags
            // Tag: "midgard.xp=100"
            for (String tag : mob.getScoreboardTags()) {
                if (tag.startsWith("midgard.xp=")) {
                    try {
                        baseMobXp = Double.parseDouble(tag.substring("midgard.xp=".length()));
                    } catch (NumberFormatException ignored) { /* Tag com formato inválido, usar fallback */ }
                }
            }
        }
        double xp = levelManager.calculateKillXp(playerLevel, mobLevel, baseMobXp);
        
        if (xp <= 0) {
            return;
        }

        // Apply Luck XP boost (since vanilla XP is zeroed, Luck must boost RPG XP here)
        me.ray.midgard.core.attribute.CoreAttributeData killerAttributes = profile.getOrCreateData(me.ray.midgard.core.attribute.CoreAttributeData.class);
        me.ray.midgard.core.attribute.AttributeInstance luckAttr = killerAttributes.getInstance(me.ray.midgard.modules.combat.CombatAttributes.LUCK);
        if (luckAttr != null && luckAttr.getValue() > 0) {
            xp *= (1.0 + (luckAttr.getValue() / 100.0));
        }

        // Apply XP_BONUS attribute multiplier
        me.ray.midgard.core.attribute.AttributeInstance xpBonusAttr = killerAttributes.getInstance(me.ray.midgard.modules.combat.CombatAttributes.XP_BONUS);
        if (xpBonusAttr != null && xpBonusAttr.getValue() > 0) {
            xp *= (1.0 + (xpBonusAttr.getValue() / 100.0));
        }
        
        // 4. Send XP gain message
        if (me.ray.midgard.modules.combat.CombatModule.getInstance() != null) {
            // Using MiniMessage format if available or legacy
            String xpMsg = me.ray.midgard.modules.combat.CombatModule.getInstance()
                .getMessage("progression.xp_gained");
                
            if (xpMsg != null) {
                xpMsg = xpMsg.replace("%xp%", String.format("%.1f", xp))
                             .replace("%mob%", mob.getName())
                             .replace("%level%", String.valueOf(mobLevel)); // Extra placeholder
                me.ray.midgard.core.text.MessageUtils.send(killer, xpMsg);
            }
        }

        // Spawn XP Indicator
        me.ray.midgard.modules.combat.CombatManager combatManager = me.ray.midgard.modules.combat.CombatManager.getInstance();
        if (combatManager != null && combatManager.getIndicatorManager() != null) {
            // Using %.0f for cleaner look if it's integer-like, or %.1f if not
            String valStr = (xp % 1 == 0) ? String.valueOf((int)xp) : String.format("%.1f", xp);
            
            // Get format from config/messages
            String xpText = me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("progression.xp_indicator").replace("%xp%", valStr);
            
            // Debug
            // me.ray.midgard.core.debug.MidgardLogger.debug("Spawning XP Indicator: " + xpText + " at " + mob.getLocation());
            
            combatManager.getIndicatorManager().spawnCustomIndicator(mob, xpText, "<yellow>");
        }
        
        double finalXp = xp;
        Task.sync(killer, () -> levelManager.addExperience(killer, finalXp));
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        me.ray.midgard.core.profile.MidgardProfile profile = me.ray.midgard.core.MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        
        if (profile != null) {
            me.ray.midgard.modules.combat.CombatData data = profile.getOrCreateData(me.ray.midgard.modules.combat.CombatData.class);
            levelManager.updateVanillaExperience(player, data.getLevel(), data.getExperience());
        }
    }
    
    /**
     * Tenta extrair o nível do mob de várias fontes.
     */
    private int getMobLevel(org.bukkit.entity.LivingEntity mob) {
        // 1. Try Metadata (set by Midgard or other plugins)
        if (mob.hasMetadata("midgard_level") && !mob.getMetadata("midgard_level").isEmpty()) {
            return mob.getMetadata("midgard_level").get(0).asInt();
        }
        
        // 2. Try Name Pattern "[Lv. 10] Zombie" or "Zombie [10]"
        String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(mob.customName() != null ? mob.customName() : mob.name());
        if (name != null) {
            // Regex simples para capturar números perto de "Lv" ou "Lvl" ou entre colchetes
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[(?:Lv\\.?|Lvl\\.?)?\\s*(\\d+)\\]");
            java.util.regex.Matcher m = p.matcher(name);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) { /* Padrão de nível inválido, usar default */ }
            }
        }
        
        // 3. Default: level 1
        return 1;
    }
}
