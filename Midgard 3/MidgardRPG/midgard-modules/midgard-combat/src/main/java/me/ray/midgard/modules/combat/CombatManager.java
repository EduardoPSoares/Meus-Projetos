package me.ray.midgard.modules.combat;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.Task;

import me.ray.midgard.modules.combat.listener.StatScalingListener;
import me.ray.midgard.modules.combat.task.RegenerationTask;
import me.ray.midgard.modules.combat.task.StaminaTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.ray.midgard.core.debug.MidgardLogger;

/**
 * Gerenciador principal de combate.
 * <p>
 * Esta classe é responsável por orquestrar as mecânicas de combate em tempo real, incluindo:
 * <ul>
 *     <li>Regeneração de Vida, Mana e Stamina.</li>
 *     <li>Consumo de Stamina ao correr.</li>
 *     <li>Gerenciamento de "Combat Tag" (estado de combate).</li>
 *     <li>Processamento de eventos de dano e aplicação de fórmulas de combate.</li>
 * </ul>
 */
public class CombatManager {
    
    private static volatile CombatManager instance;
    private final JavaPlugin plugin;
    private final CombatConfig config;
    @SuppressWarnings("unused")
    private final DamageIndicatorManager indicatorManager;
    private final DamageHandler damageHandler;
    private final Map<UUID, Long> combatTag = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> debugPlayers = ConcurrentHashMap.newKeySet();
    private final me.ray.midgard.modules.combat.debug.CombatDebugScoreboard debugScoreboard;
    
    // Tasks
    private final RegenerationTask regenerationTask;
    private final StaminaTask staminaTask;
    private org.bukkit.scheduler.BukkitTask regenBukkitTask;
    private org.bukkit.scheduler.BukkitTask staminaBukkitTask;

    /**
     * Construtor do CombatManager.
     *
     * @param plugin Instância do plugin principal.
     * @param config Configuração do módulo de combate.
     * @param indicatorManager Gerenciador de indicadores de dano para exibição visual.
     */
    public CombatManager(JavaPlugin plugin, CombatConfig config, DamageIndicatorManager indicatorManager) {
        this.plugin = plugin;
        this.config = config;
        this.indicatorManager = indicatorManager;
        this.debugScoreboard = new me.ray.midgard.modules.combat.debug.CombatDebugScoreboard();
        this.damageHandler = new DamageHandler(this, config, indicatorManager);
        
        // Initialize Tasks
        this.regenerationTask = new RegenerationTask(this, combatTag);
        this.staminaTask = new StaminaTask(plugin, config);
        
        // Register Listeners
        plugin.getServer().getPluginManager().registerEvents(new CombatListener(damageHandler), plugin);
        plugin.getServer().getPluginManager().registerEvents(new StatScalingListener(), plugin);

        // Publish instance AFTER full construction to prevent other threads
        // from seeing a partially-initialized object via getInstance()
        instance = this;
    }

    public static CombatManager getInstance() {
        return instance;
    }

    public DamageIndicatorManager getIndicatorManager() {
        return indicatorManager;
    }

    public DamageHandler getDamageHandler() {
        return damageHandler;
    }

    public CombatConfig getConfig() {
        return config;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
    
    public void updateCombatTag(UUID uuid) {
        combatTag.put(uuid, System.currentTimeMillis() + config.combatTagDuration);
    }
    
    public Map<UUID, Long> getCombatTag() {
        return combatTag;
    }

    /**
     * Inicia as tarefas agendadas do gerenciador.
     * Inicia o loop de regeneração e a verificação de stamina.
     */
    public void start() {
        // Run Regeneration every second (20 ticks) - Task global
        this.regenBukkitTask = Task.syncTimer(() -> {
            try {
                regenerationTask.run();
            } catch (Exception e) {
                MidgardLogger.error("Erro na tarefa de combate (Regen)", e);
            }
        }, 20L, 20L);
        
        // Run Stamina Task - Task global
        this.staminaBukkitTask = Task.syncTimer(staminaTask, config.staminaCheckInterval, config.staminaCheckInterval);
    }

    public void shutdown() {
        if (regenBukkitTask != null) {
            regenBukkitTask.cancel();
            regenBukkitTask = null;
        }
        if (staminaBukkitTask != null) {
            staminaBukkitTask.cancel();
            staminaBukkitTask = null;
        }
        instance = null;
    }

    /**
     * Sincroniza a vida visual do jogador com a vida do RPG.
     *
     * @param player Jogador.
     * @param currentHealth Vida atual do RPG.
     * @param maxHealth Vida máxima do RPG.
     */
    public void syncHealth(Player player, double currentHealth, double maxHealth) {
        // Escala a vida vanilla (0-20) para representar a porcentagem de vida do RPG
        double percent = currentHealth / maxHealth;
        double vanillaHealth = percent * 20.0;
        
        // Limitar (Clamp) — minimum 1 HP visual para não causar morte aqui;
        // a morte é tratada explicitamente pelo caller via player.setHealth(0)
        vanillaHealth = Math.max(1, Math.min(20, vanillaHealth));
        
        // Garante que a vida máxima visual seja 20
        org.bukkit.attribute.AttributeInstance maxHealthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }
        // Remove modifiers vanilla de MAX_HEALTH que itens antigos possam ter aplicado.
        // Vida máxima vanilla deve ser sempre 20 — o RPG gerencia HP via CombatAttributes.
        if (maxHealthAttr.getValue() != 20.0) {
            for (org.bukkit.attribute.AttributeModifier mod : maxHealthAttr.getModifiers()) {
                maxHealthAttr.removeModifier(mod);
            }
            maxHealthAttr.setBaseValue(20.0);
        }
        
        player.setHealth(vanillaHealth);
    }

    /**
     * Calcula o tempo de recarga ajustado após aplicar a Redução de Cooldown (CDR).
     *
     * @param player O jogador para verificar os atributos.
     * @param baseCooldownMillis O tempo de recarga base em milissegundos.
     * @return O tempo de recarga ajustado em milissegundos.
     */
    public static long getAdjustedCooldown(Player player, long baseCooldownMillis) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return baseCooldownMillis;
        }
        
        CoreAttributeData attributeData = profile.getOrCreateData(CoreAttributeData.class);
        AttributeInstance cdrAttr = attributeData.getInstance(CombatAttributes.COOLDOWN_REDUCTION);
        
        if (cdrAttr != null && cdrAttr.getValue() > 0) {
            double cdr = Math.min(80.0, cdrAttr.getValue()); // Limite de 80% geralmente
            return (long) (baseCooldownMillis * (1.0 - (cdr / 100.0)));
        }
        
        return baseCooldownMillis;
    }

    public boolean isDebugging(UUID uuid) {
        return debugPlayers.contains(uuid);
    }

    public void toggleDebug(UUID uuid) {
        if (debugPlayers.contains(uuid)) {
            debugPlayers.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                debugScoreboard.disable(p);
            }
        } else {
            debugPlayers.add(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                debugScoreboard.enable(p);
            }
        }
    }
    
    public me.ray.midgard.modules.combat.debug.CombatDebugScoreboard getDebugScoreboard() {
        return debugScoreboard;
    }

    /**
     * Preenche vida, mana, stamina e stellium ao máximo dos atributos atuais.
     * Deve ser chamado após aplicar atributos de classe (set de classe, level up)
     * para evitar o efeito visual de "dano" por discrepância entre current e novo max.
     *
     * @param player Jogador a ser curado.
     */
    public void fillResources(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }

        CoreAttributeData attributeData = profile.getData(CoreAttributeData.class);
        if (attributeData == null) {
            return;
        }

        CombatData combatData = profile.getData(CombatData.class);
        if (combatData == null) {
            return;
        }

        AttributeInstance maxHealthAttr = attributeData.getInstance(CombatAttributes.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 100;
        combatData.setCurrentHealth(maxHealth);
        syncHealth(player, maxHealth, maxHealth);

        AttributeInstance maxManaAttr = attributeData.getInstance(CombatAttributes.MAX_MANA);
        double maxMana = maxManaAttr != null ? maxManaAttr.getValue() : 100;
        combatData.setCurrentMana(maxMana);

        AttributeInstance maxStaminaAttr = attributeData.getInstance(CombatAttributes.MAX_STAMINA);
        double maxStamina = maxStaminaAttr != null ? maxStaminaAttr.getValue() : 100;
        combatData.setCurrentStamina(maxStamina);
    }

    /**
     * Ajusta vida, mana e stamina quando atributos máximos mudam (equipamento).
     * <p>
     * Apenas limita os recursos ao novo máximo se estiverem acima.
     * Não aumenta a vida ao equipar itens — isso deve vir da regeneração natural.
     *
     * @param player Jogador cujos recursos devem ser ajustados.
     */
    public void clampResources(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
        if (profile == null) {
            return;
        }

        CoreAttributeData attributeData = profile.getData(CoreAttributeData.class);
        if (attributeData == null) {
            return;
        }

        CombatData combatData = profile.getData(CombatData.class);
        if (combatData == null) {
            return;
        }

        // Health — apenas clamp, nunca aumenta (previne exploit de equip/desequip para curar)
        AttributeInstance maxHealthAttr = attributeData.getInstance(CombatAttributes.MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 100;
        double currentHealth = combatData.getCurrentHealth();

        if (currentHealth > maxHealth) {
            combatData.setCurrentHealth(maxHealth);
            currentHealth = maxHealth;
        }
        syncHealth(player, currentHealth, maxHealth);

        // Mana
        AttributeInstance maxManaAttr = attributeData.getInstance(CombatAttributes.MAX_MANA);
        double maxMana = maxManaAttr != null ? maxManaAttr.getValue() : 100;
        if (combatData.getCurrentMana() > maxMana) {
            combatData.setCurrentMana(maxMana);
        }

        // Stamina
        AttributeInstance maxStaminaAttr = attributeData.getInstance(CombatAttributes.MAX_STAMINA);
        double maxStamina = maxStaminaAttr != null ? maxStaminaAttr.getValue() : 100;
        if (combatData.getCurrentStamina() > maxStamina) {
            combatData.setCurrentStamina(maxStamina);
        }

        // Absorption — cap based on max_absorption attribute or % of max health
        AttributeInstance maxAbsorptionAttr = attributeData.getInstance(CombatAttributes.MAX_ABSORPTION);
        double maxAbsorption = maxAbsorptionAttr != null ? maxAbsorptionAttr.getValue() : 0;
        if (config.absorptionEnabled && config.absorptionMaxPercent > 0) {
            double percentCap = maxHealth * (config.absorptionMaxPercent / 100.0);
            maxAbsorption = Math.max(maxAbsorption, percentCap);
        }
        if (combatData.getCurrentAbsorption() > maxAbsorption) {
            combatData.setCurrentAbsorption(maxAbsorption);
        }
    }

    /**
     * Limpa todos os dados de um jogador ao sair do servidor.
     * Previne vazamento de memória removendo todas as referências por UUID.
     *
     * @param uuid UUID do jogador que saiu.
     */
    public void cleanupPlayer(UUID uuid) {
        combatTag.remove(uuid);
        debugPlayers.remove(uuid);
        debugScoreboard.cleanup(uuid);
        regenerationTask.cleanupPlayer(uuid);
    }
}
