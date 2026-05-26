package me.ray.midgard.modules.combat;

import me.ray.midgard.modules.combat.level.LevelListener;
import me.ray.midgard.modules.combat.level.LevelManager;
import me.ray.midgard.modules.combat.listener.DummyListener;
import me.ray.midgard.core.ModulePriority;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.utils.ConsoleUtils;
import me.ray.midgard.modules.combat.command.CombatDummyCommand;

/**
 * Módulo de Combate do MidgardRPG.
 * <p>
 * Este módulo é responsável por gerenciar toda a lógica de combate do RPG, incluindo:
 * <ul>
 *     <li>Registro e gerenciamento de atributos de combate (Vida, Mana, Stamina, etc).</li>
 *     <li>Sistema de indicadores de dano (Hologramas).</li>
 *     <li>Gerenciamento de regeneração e stamina.</li>
 *     <li>Sistema de níveis e experiência.</li>
 * </ul>
 */
public class CombatModule extends RPGModule {

    private static volatile CombatModule instance;
    private LevelManager levelManager; // Stored field
    private me.ray.midgard.core.sync.DefinitionSyncManager syncManager;
    private CombatOverlay combatOverlay;
    private CombatManager combatManager;

    /**
     * Construtor do módulo de combate.
     * Inicializa o módulo com o nome "MidgardCombat".
     */
    public CombatModule() {
        super("MidgardCombat", ModulePriority.HIGH);
    }
    
    public static CombatModule getInstance() {
        return instance;
    }
    
    public LevelManager getLevelManager() {
        return levelManager;
    }

    /**
     * Chamado quando o módulo é habilitado.
     * Inicializa configurações, atributos, overlay, indicadores de dano, gerenciador de combate e sistema de níveis.
     */
    @Override
    public void onEnable() {
        instance = this;
        // loadMessages(); // Removido: Usando sistema centralizado via RPGModule
        // ConsoleUtils.success("Lógica de combate inicializada! Espadas estão afiadas.");
        
        CombatConfig config = new CombatConfig(plugin);
        CombatAttributes.register();
        CombatPlaceholders.register();
        this.combatOverlay = new CombatOverlay(plugin);
        combatOverlay.start();
        
        // Inicializa o Sistema de Indicadores de Dano
        DamageIndicatorManager indicatorManager = new DamageIndicatorManager(plugin, config);
        // plugin.getServer().getPluginManager().registerEvents(new DamageListener(indicatorManager, config), plugin);
        
        this.combatManager = new CombatManager(plugin, config, indicatorManager);
        combatManager.start();

        // Inicializa o Sistema de Níveis
        this.levelManager = new LevelManager(config);
        plugin.getServer().getPluginManager().registerEvents(new LevelListener(levelManager, config), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DummyListener(), plugin);

        // Registra dummy command apenas no AdminCommand para /rpg admin dummy
        if (MidgardCore.getAdminCommand() != null) {
            MidgardCore.getAdminCommand().registerSubcommand(new CombatDummyCommand());
            MidgardCore.getAdminCommand().registerSubcommand(new me.ray.midgard.modules.combat.command.XPCommand());
        }

        // Start config sync
        if (config.getRepository() != null) {
            me.ray.midgard.core.redis.RedisManager redisManager = me.ray.midgard.core.MidgardCore.getRedisManager();
            this.syncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                "combat_config", config.getRepository(), redisManager, 30,
                id -> me.ray.midgard.core.utils.Task.sync(() -> config.reload()),
                id -> {}, // No delete for config
                () -> me.ray.midgard.core.utils.Task.sync(() -> config.reload()),
                null
            );
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (CombatManager.getInstance() != null && CombatManager.getInstance().getConfig() != null) {
            CombatManager.getInstance().getConfig().reload();
            DamageIndicatorManager indicatorManager = CombatManager.getInstance().getIndicatorManager();
            if (indicatorManager != null) {
                indicatorManager.reload();
            }
        }
    }

    /**
     * Chamado quando o módulo é desabilitado.
     * Realiza a limpeza necessária.
     */
    @Override
    public void onDisable() {
        if (combatOverlay != null) {
            combatOverlay.shutdown();
            combatOverlay = null;
        }
        if (combatManager != null) {
            combatManager.shutdown();
            combatManager = null;
        }
        if (syncManager != null) {
            try { syncManager.shutdown(); } catch (Exception ignored) { /* Shutdown pode falhar se a conexão já estiver fechada */ }
        }
        instance = null;
        ConsoleUtils.info("Lógica de combate desabilitada.");
    }
}
