package me.ray.midgard.modules.combat.task;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatData;
import me.ray.midgard.modules.combat.CombatManager;
import me.ray.midgard.modules.combat.CombatModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tarefa responsável pela regeneração periódica de atributos.
 * <p>
 * Executa a cada segundo (20 ticks) e regenera:
 * <ul>
 *     <li>Vida (apenas fora de combate).</li>
 *     <li>Mana (sempre).</li>
 *     <li>Stamina (sempre, mas reduzida se estiver correndo).</li>
 * </ul>
 */
public class RegenerationTask implements Runnable {

    private final CombatManager combatManager;
    private final Map<UUID, Long> combatTag;
    private final Set<UUID> wasInCombat = ConcurrentHashMap.newKeySet();

    /**
     * Construtor da RegenerationTask.
     *
     * @param combatManager Gerenciador de combate.
     * @param combatTag Mapa de jogadores em combate.
     */
    public RegenerationTask(CombatManager combatManager, Map<UUID, Long> combatTag) {
        this.combatManager = combatManager;
        this.combatTag = combatTag;
    }

    /**
     * Limpa dados de um jogador ao sair do servidor.
     *
     * @param uuid UUID do jogador.
     */
    public void cleanupPlayer(UUID uuid) {
        wasInCombat.remove(uuid);
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        
        // Cleanup expired combat tags to prevent memory leak
        combatTag.entrySet().removeIf(entry -> entry.getValue() <= now);

        for (Player player : Bukkit.getOnlinePlayers()) {
            Task.sync(player, () -> processPlayer(player, now));
        }
    }

    private void processPlayer(Player player, long now) {
        if (!player.isOnline()) {
            return;
        }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }

        CombatData combatData = profile.getOrCreateData(CombatData.class);
        CoreAttributeData attributeData = profile.getOrCreateData(CoreAttributeData.class);

        boolean inCombat;
        Long expiry = combatTag.get(player.getUniqueId());
        inCombat = expiry != null && expiry > now;
        UUID playerId = player.getUniqueId();
        
        // Check if player just left combat
        if (!inCombat && wasInCombat.contains(playerId)) {
            wasInCombat.remove(playerId);
            if (CombatModule.getInstance() != null) {
                String exitMsg = CombatModule.getInstance().getMessage("combat_mode.disabled");
                MessageUtils.send(player, exitMsg);
            }
        } else if (inCombat) {
            wasInCombat.add(playerId);
        }

        // Absorption Decay (sempre, independente de combate)
        me.ray.midgard.modules.combat.CombatConfig combatConfig = combatManager.getConfig();
        if (combatConfig != null && combatConfig.absorptionDecayPerSecond > 0 && combatData.getCurrentAbsorption() > 0) {
            double newAbsorption = Math.max(0, combatData.getCurrentAbsorption() - combatConfig.absorptionDecayPerSecond);
            combatData.setCurrentAbsorption(newAbsorption);
        }

        // Regeneração de Vida (Apenas se NÃO estiver em combate)
        if (!inCombat) {
            AttributeInstance maxHealthAttr = attributeData.getInstance(CombatAttributes.MAX_HEALTH);
            AttributeInstance healthRegenAttr = attributeData.getInstance(CombatAttributes.HEALTH_REGEN);
            AttributeInstance healthRegenAmpAttr = attributeData.getInstance(CombatAttributes.HEALTH_REGEN_AMP);
            AttributeInstance maxHealthRegenAttr = attributeData.getInstance(CombatAttributes.MAX_HEALTH_REGEN);

            double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 100;
            double healthRegen = healthRegenAttr != null ? healthRegenAttr.getValue() : 1;
            double healthRegenAmp = healthRegenAmpAttr != null ? healthRegenAmpAttr.getValue() : 0;
            double maxHealthRegen = maxHealthRegenAttr != null ? maxHealthRegenAttr.getValue() : 0;

            double finalRegen = healthRegen * (1 + (healthRegenAmp / 100.0));
            if (maxHealthRegen > 0) {
                finalRegen = Math.min(finalRegen, maxHealthRegen);
            }

            double currentHealth = combatData.getCurrentHealth();
            if (currentHealth < maxHealth && !player.isDead()) {
                double newHealth = Math.min(maxHealth, currentHealth + finalRegen);
                combatData.setCurrentHealth(newHealth);
                combatManager.syncHealth(player, newHealth, maxHealth);
                
                // Notify when health is fully regenerated
                if (currentHealth < maxHealth * 0.99 && newHealth >= maxHealth && CombatModule.getInstance() != null) {
                    String regenMsg = CombatModule.getInstance().getMessage("status.health_regenerated")
                        .replace("%amount%", String.valueOf((int) (newHealth - currentHealth)));
                    MessageUtils.send(player, regenMsg);
                }
            } else if (currentHealth > maxHealth) {
                // CLAMP OVERFLOW: Fix health if it exceeds max (e.g. unequipped item)
                combatData.setCurrentHealth(maxHealth);
                combatManager.syncHealth(player, maxHealth, maxHealth);
            }
        }

        // Regeneração de Mana (Apenas se NÃO estiver em combate)
        if (!inCombat) {
            AttributeInstance maxManaAttr = attributeData.getInstance(CombatAttributes.MAX_MANA);
            AttributeInstance regenAttr = attributeData.getInstance(CombatAttributes.MANA_REGEN);
            AttributeInstance regenAmpAttr = attributeData.getInstance(CombatAttributes.MANA_REGEN_AMP);
            AttributeInstance maxManaRegenAttr = attributeData.getInstance(CombatAttributes.MAX_MANA_REGEN);

            double maxMana = maxManaAttr != null ? maxManaAttr.getValue() : 100;
            double regen = regenAttr != null ? regenAttr.getValue() : 5;
            double regenAmp = regenAmpAttr != null ? regenAmpAttr.getValue() : 0;
            double maxManaRegen = maxManaRegenAttr != null ? maxManaRegenAttr.getValue() : 0;

            double finalRegen = regen * (1 + (regenAmp / 100.0));
            if (maxManaRegen > 0) {
                finalRegen = Math.min(finalRegen, maxManaRegen);
            }

            double current = combatData.getCurrentMana();
            if (current < maxMana) {
                double newMana = Math.min(maxMana, current + finalRegen);
                combatData.setCurrentMana(newMana);
                
                // Notify when mana is fully regenerated
                if (current < maxMana * 0.99 && newMana >= maxMana && CombatModule.getInstance() != null) {
                    String manaMsg = CombatModule.getInstance().getMessage("status.mana_regenerated")
                            .replace("%amount%", String.valueOf((int) (newMana - current)));
                    MessageUtils.send(player, manaMsg);
                }
            } else if (current > maxMana) {
                // CLAMP OVERFLOW: Fix mana if it exceeds max
                combatData.setCurrentMana(maxMana);
            }
        }

        // Regeneração de Stamina (Apenas se NÃO estiver correndo)
        if (!player.isSprinting()) {
            AttributeInstance maxStaminaAttr = attributeData.getInstance(CombatAttributes.MAX_STAMINA);
            AttributeInstance staminaRegenAttr = attributeData.getInstance(CombatAttributes.STAMINA_REGEN);
            AttributeInstance staminaRegenAmpAttr = attributeData.getInstance(CombatAttributes.STAMINA_REGEN_AMP);
            AttributeInstance maxStaminaRegenAttr = attributeData.getInstance(CombatAttributes.MAX_STAMINA_REGEN);

            double maxStamina = maxStaminaAttr != null ? maxStaminaAttr.getValue() : 100;
            double staminaRegen = staminaRegenAttr != null ? staminaRegenAttr.getValue() : 10;
            double staminaRegenAmp = staminaRegenAmpAttr != null ? staminaRegenAmpAttr.getValue() : 0;
            double maxStaminaRegen = maxStaminaRegenAttr != null ? maxStaminaRegenAttr.getValue() : 0;

            double finalRegen = staminaRegen * (1 + (staminaRegenAmp / 100.0));
            if (maxStaminaRegen > 0) {
                finalRegen = Math.min(finalRegen, maxStaminaRegen);
            }

            double currentStamina = combatData.getCurrentStamina();
            if (currentStamina < maxStamina) {
                combatData.setCurrentStamina(Math.min(maxStamina, currentStamina + finalRegen));
            } else if (currentStamina > maxStamina) {
                // CLAMP OVERFLOW: Fix stamina if it exceeds max
                combatData.setCurrentStamina(maxStamina);
            }
        }
    }
}
