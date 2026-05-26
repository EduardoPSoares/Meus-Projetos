package me.ray.midgard.modules.combat;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.Task;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Gerenciador de Overlay (Action Bar) de combate.
 * <p>
 * Exibe informações vitais (Vida, Mana, Stamina) na Action Bar do jogador em tempo real.
 */
public class CombatOverlay implements Runnable {

    private final JavaPlugin plugin;
    private org.bukkit.scheduler.BukkitTask task;

    /**
     * Construtor do CombatOverlay.
     *
     * @param plugin Instância do plugin principal.
     */
    public CombatOverlay(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicia a tarefa de atualização da Action Bar.
     * Executa a cada 10 ticks (0.5 segundos) com dispatch por entity scheduler para Folia.
     */
    public void start() {
        this.task = Task.syncTimer(this, 10L, 10L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * Executado periodicamente. Delega a renderização para o entity scheduler de cada jogador (Folia-safe).
     */
    @Override
    public void run() {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Task.sync(player, () -> renderOverlay(player));
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro no CombatOverlay", e);
        }
    }
    
    /**
     * Renderiza a action bar para um jogador específico.
     * Executado no entity scheduler do jogador (thread-safe em Folia).
     */
    private void renderOverlay(Player player) {
        if (!player.isOnline()) {
            return;
        }
        
        if (player.hasMetadata("midgard_combo_active") || player.hasMetadata("midgard_casting_mode") || player.hasMetadata("midgard_channeling")) {
            return;
        }
        
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }

        CombatData combatData = profile.getData(CombatData.class);
        CoreAttributeData attributeData = profile.getData(CoreAttributeData.class);
        
        if (combatData == null) {
            combatData = profile.getOrCreateData(CombatData.class);
        }
        
        if (attributeData == null) {
            attributeData = profile.getOrCreateData(CoreAttributeData.class);
        }

        double currentHealth = combatData.getCurrentHealth();
        AttributeInstance maxHealthAttr = attributeData.getInstance(CombatAttributes.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 100;
        
        double currentMana = combatData.getCurrentMana();
        AttributeInstance maxManaAttr = attributeData.getInstance(CombatAttributes.MAX_MANA);
        double maxMana = maxManaAttr != null ? maxManaAttr.getValue() : 100;

        double currentStamina = combatData.getCurrentStamina();
        AttributeInstance maxStaminaAttr = attributeData.getInstance(CombatAttributes.MAX_STAMINA);
        double maxStamina = maxStaminaAttr != null ? maxStaminaAttr.getValue() : 100;

        String bar = String.format(
                "<red>❤</red> <gray>%.0f/%.0f  <blue>💧</blue> <gray>%.0f/%.0f  <yellow>⚡</yellow> <gray>%.0f/%.0f</gray>",
                currentHealth, maxHealth, currentMana, maxMana, currentStamina, maxStamina
        );
        
        MessageUtils.sendActionBar(player, bar);
    }
}
