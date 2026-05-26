package me.ray.midgard.modules.combat.task;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.CombatConfig;
import me.ray.midgard.modules.combat.CombatData;
import me.ray.midgard.core.utils.Task;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import me.ray.midgard.core.debug.MidgardLogger;

/**
 * Tarefa responsável pelo consumo de Stamina ao correr.
 * <p>
 * Verifica periodicamente se os jogadores estão correndo e drena Stamina.
 * Se a Stamina acabar, força o jogador a parar de correr.
 */
public class StaminaTask implements Runnable {

    private final JavaPlugin plugin;
    private final CombatConfig config;

    /**
     * Construtor da StaminaTask.
     *
     * @param plugin Instância do plugin.
     * @param config Configuração de combate.
     */
    public StaminaTask(JavaPlugin plugin, CombatConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Task.sync(player, () -> processPlayer(player));
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro na tarefa de Stamina", e);
        }
    }

    private void processPlayer(Player player) {
        if (!player.isOnline()) {
            return;
        }
        if (!player.isSprinting()) {
            return;
        }

        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }

        CombatData combatData = profile.getOrCreateData(CombatData.class);
        double currentStamina = combatData.getCurrentStamina();
        
        // Get max stamina from attributes
        me.ray.midgard.core.attribute.CoreAttributeData attributeData = profile.getOrCreateData(me.ray.midgard.core.attribute.CoreAttributeData.class);
        me.ray.midgard.core.attribute.AttributeInstance maxStaminaAttr = attributeData.getInstance(me.ray.midgard.modules.combat.CombatAttributes.MAX_STAMINA);
        double maxStamina = maxStaminaAttr != null ? maxStaminaAttr.getValue() : 100;

        double drain = config.staminaSprintDrain;

        if (currentStamina >= drain) {
            double newStamina = currentStamina - drain;
            combatData.setCurrentStamina(newStamina);
            
            // Warn when stamina is critically low (15%)
            if (newStamina < maxStamina * 0.15 && newStamina >= drain && me.ray.midgard.modules.combat.CombatModule.getInstance() != null) {
                String lowMsg = me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("status.stamina_low");
                me.ray.midgard.core.text.MessageUtils.sendActionBar(player, lowMsg);
            }
        } else {
            combatData.setCurrentStamina(0);
            player.setSprinting(false);
            
            // Notify when forced to stop sprinting due to no stamina
            if (me.ray.midgard.modules.combat.CombatModule.getInstance() != null) {
                String exhaustedMsg = me.ray.midgard.modules.combat.CombatModule.getInstance().getMessage("status.stamina_exhausted");
                me.ray.midgard.core.text.MessageUtils.sendActionBar(player, exhaustedMsg);
            }
        }
    }
}
