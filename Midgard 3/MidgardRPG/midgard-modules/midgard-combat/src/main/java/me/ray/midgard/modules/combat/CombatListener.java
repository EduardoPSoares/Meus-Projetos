package me.ray.midgard.modules.combat;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class CombatListener implements Listener {

    private final DamageHandler damageHandler;

    public CombatListener(DamageHandler damageHandler) {
        this.damageHandler = damageHandler;
    }

    /**
     * Evento chamado quando um jogador entra no servidor.
     * Configura a escala de vida visual para 20 corações (padrão do Minecraft),
     * independentemente da vida real do RPG.
     *
     * @param event O evento de entrada do jogador.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        org.bukkit.attribute.AttributeInstance vanillaMaxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (vanillaMaxHealth != null) {
            vanillaMaxHealth.setBaseValue(20.0);
        }
        player.setHealthScaled(true);
        player.setHealthScale(20.0);

        // Sincroniza a vida do RPG com a vida visual ao entrar
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile != null) {
            CombatData combatData = profile.getOrCreateData(CombatData.class);
            CoreAttributeData attributeData = profile.getOrCreateData(CoreAttributeData.class);
            
            AttributeInstance maxHealthAttr = attributeData.getInstance(CombatAttributes.MAX_HEALTH);
            double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 100;
            
            // Se for a primeira vez (vida padrão 100), garante que esteja cheio se maxHealth mudou
            if (combatData.getCurrentHealth() > maxHealth) {
                combatData.setCurrentHealth(maxHealth);
            }

            // Inicializa mana/stamina no primeiro login (padrão do CombatData é 0)
            AttributeInstance maxManaAttr = attributeData.getInstance(CombatAttributes.MAX_MANA);
            double maxMana = maxManaAttr != null ? maxManaAttr.getValue() : 100;
            if (combatData.getCurrentMana() <= 0) {
                combatData.setCurrentMana(maxMana);
            }

            AttributeInstance maxStaminaAttr = attributeData.getInstance(CombatAttributes.MAX_STAMINA);
            double maxStamina = maxStaminaAttr != null ? maxStaminaAttr.getValue() : 100;
            if (combatData.getCurrentStamina() <= 0) {
                combatData.setCurrentStamina(maxStamina);
            }
            
            me.ray.midgard.modules.combat.CombatManager manager = me.ray.midgard.modules.combat.CombatManager.getInstance();
            if (manager != null) {
                manager.syncHealth(player, combatData.getCurrentHealth(), maxHealth);
            }
            
            MidgardLogger.debug(DebugCategory.COMBAT, "Sincronizando vida de login para %s: Atual=%.1f/%.1f", 
                player.getName(), combatData.getCurrentHealth(), maxHealth);
        }
    }

    /**
     * Evento chamado quando um jogador sai do servidor.
     * Limpa todos os dados de combate associados para prevenir vazamento de memória.
     *
     * @param event O evento de saída do jogador.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        CombatManager manager = CombatManager.getInstance();
        if (manager != null) {
            manager.cleanupPlayer(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Evento principal de cálculo de dano.
     * <p>
     * Este método intercepta todo dano causado a JOGADORES e delega a lógica para o DamageHandler.
     * Mobs e outras entidades são ignorados para deixar o MythicMobs/Vanilla gerenciar.
     *
     * @param event O evento de dano.
     */
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        // Se a vítima não for uma entidade viva, ignoramos.
        if (!(event.getEntity() instanceof org.bukkit.entity.LivingEntity)) {
            return;
        }
        // Se o evento já foi cancelado por outro plugin, respeitamos (opcional, mas seguro)
        if (event.isCancelled()) {
            return;
        }
        
        // Log básico se necessário, mas o detalhamento fica no DamageHandler
        MidgardLogger.debug(DebugCategory.COMBAT, "Detectado evento de dano na entidade %s. Tipo: %s. Dano Original: %.2f", 
            event.getEntity().getName(), event.getCause(), event.getDamage());
            
        damageHandler.handleDamage(event);
    }

    /**
     * Evento de regeneração de vida do Minecraft.
     * <p>
     * Intercepta a regeneração natural (por saciedade) e a regeneração padrão (poção de regeneração vanilla),
     * convertendo-as para o sistema de vida do RPG.
     *
     * @param event O evento de regeneração.
     */
    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();

        // Se for regeneração natural ou mágica, aplicamos ao sistema de RPG
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED ||
                event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN ||
                event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC ||
                event.getRegainReason() == EntityRegainHealthEvent.RegainReason.MAGIC_REGEN) {
            
            event.setCancelled(true); // Cancela a cura vanilla para não duplicar ou desincronizar

            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null) {
                CombatData combatData = profile.getOrCreateData(CombatData.class);
                CoreAttributeData attributeData = profile.getOrCreateData(CoreAttributeData.class);
                
                AttributeInstance maxHealthAttr = attributeData.getInstance(CombatAttributes.MAX_HEALTH);
                double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 100;
                
                // Calcula quanto curar baseado na quantidade do evento vanilla
                // O evento vanilla geralmente cura 1.0 (meio coração) por tick de regen
                // Precisamos escalar isso para a vida do RPG.
                // Ex: Se vida vanilla é 20 e RPG é 100, a escala é 5x.
                // 1.0 vanilla = 5.0 RPG.
                
                double vanillaAmount = event.getAmount();
                double scale = maxHealth / 20.0;
                double rpgAmount = vanillaAmount * scale;
                
                double currentHealth = combatData.getCurrentHealth();
                if (currentHealth < maxHealth) {
                    double newHealth = Math.min(maxHealth, currentHealth + rpgAmount);
                    combatData.setCurrentHealth(newHealth);
                    CombatManager mgr = CombatManager.getInstance();
                    if (mgr != null) {
                        mgr.syncHealth(player, newHealth, maxHealth);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Ao renascer, restaura a vida do RPG para o máximo
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile != null) {
            CombatData combatData = profile.getOrCreateData(CombatData.class);
            CoreAttributeData attributeData = profile.getOrCreateData(CoreAttributeData.class);
            
            AttributeInstance maxHealthAttr = attributeData.getInstance(CombatAttributes.MAX_HEALTH);
            double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 100;
            
            combatData.setCurrentHealth(maxHealth);
            
            AttributeInstance maxManaAttr = attributeData.getInstance(CombatAttributes.MAX_MANA);
            if (maxManaAttr != null) {
                combatData.setCurrentMana(maxManaAttr.getValue());
            }
            
            AttributeInstance maxStaminaAttr = attributeData.getInstance(CombatAttributes.MAX_STAMINA);
            if (maxStaminaAttr != null) {
                combatData.setCurrentStamina(maxStaminaAttr.getValue());
            }
            
            // A sincronização visual acontecerá automaticamente ou podemos forçar um delay
            me.ray.midgard.core.utils.Task.syncLater(player, () -> {
                me.ray.midgard.modules.combat.CombatManager mgr = me.ray.midgard.modules.combat.CombatManager.getInstance();
                if (mgr != null) {
                    mgr.syncHealth(player, maxHealth, maxHealth);
                }
            }, 5L);
        }
    }

    /**
     * Evento de morte de entidade.
     * <p>
     * Aplica a mecânica de Sorte (Luck):
     * <ul>
     *     <li>Aumenta a experiência dropada (XP Boost).</li>
     *     <li>Chance de duplicar os itens dropados (Drop Boost).</li>
     * </ul>
     *
     * @param event O evento de morte.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        Player killer = event.getEntity().getKiller();

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(killer.getUniqueId());
        if (profile == null) {
            return;
        }

        CoreAttributeData attributeData = profile.getOrCreateData(CoreAttributeData.class);
        AttributeInstance luckAttr = attributeData.getInstance(CombatAttributes.LUCK);

        if (luckAttr != null && luckAttr.getValue() > 0) {
            double luck = luckAttr.getValue();

            // Boost de XP: Luck boost is now handled by LevelListener on RPG XP.
            // Vanilla XP is zeroed by LevelListener, so no vanilla XP boost here.

            // Boost de Drop: Chance de duplicar drops
            // Chance = Sorte / 10 (ex: 100 Sorte = 10% chance)
            double doubleDropChance = luck / 10.0;
            if (ThreadLocalRandom.current().nextDouble() * 100 < doubleDropChance) {
                for (ItemStack drop : event.getDrops()) {
                    if (drop != null && drop.getType() != org.bukkit.Material.AIR) {
                        org.bukkit.Location loc = event.getEntity().getLocation();
                        me.ray.midgard.core.utils.Task.sync(loc, () -> 
                            loc.getWorld().dropItemNaturally(loc, drop.clone())
                        );
                    }
                }
            }
        }
    }
}
