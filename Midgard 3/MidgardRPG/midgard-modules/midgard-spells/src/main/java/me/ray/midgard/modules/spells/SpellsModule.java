package me.ray.midgard.modules.spells;

import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.modules.spells.command.SpellCommand;
import me.ray.midgard.modules.spells.listener.ScrollListener;
import me.ray.midgard.modules.spells.listener.SpellDamageListener;
import me.ray.midgard.modules.spells.listener.SpellsListener;
import me.ray.midgard.modules.spells.api.ResourceProvider;
import me.ray.midgard.modules.spells.integration.CombatModuleBridge;
import me.ray.midgard.modules.spells.integration.DummyResourceProvider;
import me.ray.midgard.modules.spells.manager.ScrollManager;
import me.ray.midgard.modules.spells.manager.SpellManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

import me.ray.midgard.modules.spells.task.SkillBarTask;

public class SpellsModule extends RPGModule {

    private SpellManager spellManager;
    private SpellsListener spellsListener;
    private ResourceProvider resourceProvider;
    private YamlConfiguration messagesConfig;
    private ScrollManager scrollManager;
    private SpellDamageListener damageListener;
    private ScrollListener scrollListener;
    private SkillBarTask skillBarTask;
    private me.ray.midgard.core.database.DefinitionRepository repository;
    private me.ray.midgard.core.sync.DefinitionSyncManager syncManager;

    public SpellsModule() {
        super("Spells");
    }

    @Override
    public void onEnable() {
        loadMessages();
        
        setupResourceProvider();
        
        // Initialize DB repository + migrate YAML if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new me.ray.midgard.core.database.DefinitionRepository(dbManager, "midgard_spells");
            java.io.File spellsFolder = new java.io.File(getPlugin().getDataFolder(), "modules/spells/spells");
            new me.ray.midgard.core.database.DefinitionMigrationTool(repository, "spells")
                .migrateFolderWholeFiles(spellsFolder, "spell");
        }
        
        this.spellManager = new SpellManager(this);
        this.spellManager.loadSpells();

        // Register Skill Provider
        me.ray.midgard.core.MidgardCore.setSkillProvider(new SpellsSkillProvider(this));

        this.spellsListener = new SpellsListener(this);
        Bukkit.getPluginManager().registerEvents(this.spellsListener, getPlugin());

        // Register SpellDamageListener
        this.damageListener = new SpellDamageListener(this);
        Bukkit.getPluginManager().registerEvents(this.damageListener, getPlugin());

        // Register ScrollManager and ScrollListener
        this.scrollManager = new ScrollManager(this);
        this.scrollListener = new ScrollListener(this);
        Bukkit.getPluginManager().registerEvents(this.scrollListener, getPlugin());
        
        // Register MythicMobs Integration
        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            me.ray.midgard.core.debug.MidgardLogger.debug("SpellsModule: MythicMobs integration enabled.");
        }
        
        // Register /midspell (renamed to /spell) in UnifiedCommandManager
        SpellCommand spellCmd = new SpellCommand(this);
        
        me.ray.midgard.core.MidgardCore.getCommandManager().registerPlayerCommand(spellCmd);

        // Comandos midspell e midskills agora são registrados no plugin.yml
        // Não precisam estar no /midgard - são comandos de jogador
        
        // Start SkillBar Task
        this.skillBarTask = new SkillBarTask(this);
        this.skillBarTask.start();
        
        // Start sync
        if (repository != null) {
            me.ray.midgard.core.redis.RedisManager redisManager = me.ray.midgard.core.MidgardCore.getRedisManager();
            this.syncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                "spells", repository, redisManager, 30,
                id -> {
                    repository.load(id).thenAccept(data -> {
                        if (data != null) { me.ray.midgard.core.utils.Task.sync(() -> spellManager.reloadSpellFromDb(id, data)); }
                    });
                },
                id -> me.ray.midgard.core.utils.Task.sync(() -> spellManager.unregisterSpell(id)),
                () -> { spellManager.loadSpells(); },
                dbIds -> {
                    java.util.Set<String> dbSet = new java.util.HashSet<>(dbIds);
                    for (String loadedId : new java.util.ArrayList<>(spellManager.getLoadedSpellIds())) {
                        if (!dbSet.contains(loadedId)) { spellManager.unregisterSpell(loadedId); }
                    }
                }
            );
        }
        
        // ConsoleUtils.success("SpellsModule enabled!");
    }

    @Override
    public void onDisable() {
        // Stop sync manager
        if (syncManager != null) {
            try { syncManager.shutdown(); } catch (Exception ignored) { /* Shutdown pode falhar se já encerrado */ }
        }
        // Stop SkillBar task
        if (skillBarTask != null) {
            skillBarTask.stop();
            skillBarTask = null;
        }
        // Unregister listeners before nullifying references
        if (spellsListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(spellsListener);
        }
        if (damageListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(damageListener);
        }
        if (scrollListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(scrollListener);
        }
        // Shutdown spell manager (cancel channelings, clear state)
        if (spellManager != null) {
            spellManager.shutdown();
            spellManager = null;
        }
        scrollManager = null;
        damageListener = null;
        spellsListener = null;
        scrollListener = null;
        resourceProvider = null;
        messagesConfig = null;
    }

    private void setupResourceProvider() {
        boolean combatLoaded = false;
        try {
            combatLoaded = me.ray.midgard.core.MidgardCore.getModuleManager().getModule("MidgardCombat") != null;
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.debug("SpellsModule: Fallback check for combat module - " + e.getMessage());
        }

        if (combatLoaded) {
            // Better check: Check if CombatData class is reachable
            try {
                Class.forName("me.ray.midgard.modules.combat.CombatData");
                this.resourceProvider = new CombatModuleBridge();
                me.ray.midgard.core.debug.MidgardLogger.debug("SpellsModule: Hooked into Midgard-Combat for resources.");
            } catch (ClassNotFoundException e) {
                this.resourceProvider = new DummyResourceProvider();
                me.ray.midgard.core.debug.MidgardLogger.warn("SpellsModule: Combat module loaded but CombatData class not found. Using dummy resources.");
            }
        } else {
            this.resourceProvider = new DummyResourceProvider();
        }
    }

    public ResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public SpellManager getSpellManager() {
        return spellManager;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadMessages();
        if (spellManager != null) {
            spellManager.loadSpells();
            spellManager.reloadCombos();
            spellManager.getXPManager().loadConfig();
        }
    }
    
    private void loadMessages() {
        File file = new File(getPlugin().getDataFolder(), "modules/spells/lang/messages.yml");
        boolean created = false;
        if (!file.exists()) {
            try {
                getPlugin().saveResource("modules/spells/lang/messages.yml", false);
                created = true;
            } catch (Exception e) {
                me.ray.midgard.core.debug.MidgardLogger.warn("Spells: " + e.getMessage());
            }
        }
        messagesConfig = YamlConfiguration.loadConfiguration(file);

        try {
            var languageManager = MidgardCore.getLanguageManager();
            if (languageManager != null) {
                boolean missingRequirementKeys = !languageManager.hasKey("spells.requirements.level_needed");
                if (created || missingRequirementKeys) {
                    languageManager.load(null);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public String getMessage(String path) {
        if (messagesConfig == null) { return path; }
        String msg = messagesConfig.getString(path);
        return msg != null ? msg : path;
    }
    
    public java.util.List<String> getMessageList(String path) {
        if (messagesConfig == null) { return java.util.Collections.emptyList(); }
        java.util.List<String> list = messagesConfig.getStringList(path);
        return list != null ? list : java.util.Collections.emptyList();
    }

    public ScrollManager getScrollManager() {
        return scrollManager;
    }

    public SpellDamageListener getDamageListener() {
        return damageListener;
    }

    public me.ray.midgard.core.database.DefinitionRepository getRepository() {
        return repository;
    }

    public me.ray.midgard.core.sync.DefinitionSyncManager getSyncManager() {
        return syncManager;
    }

}
