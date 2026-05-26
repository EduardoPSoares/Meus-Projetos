package me.ray.midgard.modules.classes;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.ModulePriority;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.attribute.Attribute;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.AttributeRegistry;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.event.PlayerLevelUpEvent;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.ConsoleUtils;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.classes.gui.ClassSelectionGui;
import me.ray.midgard.modules.combat.CombatAttributes;
import me.ray.midgard.modules.combat.CombatModule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Módulo de Classes do MidgardRPG.
 * Gerencia classes, atributos base e seleção de classe.
 */
public class ClassesModule extends RPGModule implements Listener {

    private static volatile ClassesModule instance;
    private ClassManager classManager;
    private me.ray.midgard.modules.classes.skilltree.SkillTreeManager skillTreeManager;
    private FileConfiguration messagesConfig;
    private me.ray.midgard.core.database.DefinitionRepository repository;
    private me.ray.midgard.core.sync.DefinitionSyncManager syncManager;

    /**
     * Construtor do módulo de classes.
     */
    public ClassesModule() {
        super("Classes", ModulePriority.NORMAL);
    }

    @Override
    public void onEnable() {
        try {
            instance = this;
            loadMessages();
            
            // Initialize DB repository + migrate YAML if needed
            me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
            if (dbManager != null) {
                this.repository = new me.ray.midgard.core.database.DefinitionRepository(dbManager, "midgard_classes");
                File classesFolder = new File(plugin.getDataFolder(), "modules/classes/classes");
                new me.ray.midgard.core.database.DefinitionMigrationTool(repository, "classes")
                    .migrateFolderWholeFiles(classesFolder, "class");
            }
            
            // this.plugin is already set by super
            this.classManager = new ClassManager(plugin);
            this.skillTreeManager = new me.ray.midgard.modules.classes.skilltree.SkillTreeManager(plugin);
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            
            try {
                // Registra class command no AdminCommand
                if (MidgardCore.getAdminCommand() != null) {
                    MidgardCore.getAdminCommand().registerSubcommand(new ClassCommand(this));
                    MidgardCore.getAdminCommand().registerSubcommand(new me.ray.midgard.modules.classes.importer.ImportCommand(this));
                    MidgardLogger.debug("[ClassesModule] Comandos class e import registrados no AdminCommand");
                } else {
                    MidgardLogger.warn("[ClassesModule] AdminCommand é null - comandos não foram registrados!");
                }
                
                // Attributes command has been moved to CharacterModule

            } catch (Exception e) {
                MidgardLogger.error("Falha ao registrar comandos do módulo de Classes", e);
            }
            
            // Register default attributes if they don't exist
            registerDefaultAttributes();
            
            // Start sync
            if (repository != null) {
                me.ray.midgard.core.redis.RedisManager redisManager = me.ray.midgard.core.MidgardCore.getRedisManager();
                this.syncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                    "classes", repository, redisManager, 30,
                    id -> {
                        repository.load(id).thenAccept(data -> {
                            if (data != null) {
                            me.ray.midgard.core.utils.Task.sync(() -> classManager.reloadClassFromDb(id, data));
                        }
                        });
                    },
                    id -> me.ray.midgard.core.utils.Task.sync(() -> classManager.unregisterClass(id)),
                    () -> { classManager.reload(); },
                    dbIds -> {
                        java.util.Set<String> dbSet = new java.util.HashSet<>(dbIds);
                        for (String loadedId : new java.util.ArrayList<>(classManager.getClasses().keySet())) {
                            if (!dbSet.contains(loadedId)) {
                                classManager.unregisterClass(loadedId);
                            }
                        }
                    }
                );
            }
            
            MidgardLogger.debug("Foram carregadas " + (classManager != null ? classManager.getClasses().size() : 0) + " classes.");
        } catch (Exception e) {
            MidgardLogger.error("Erro fatal ao habilitar módulo de Classes", e);
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (classManager != null) {
            try {
                classManager.reload();
            } catch (Exception e) {
                MidgardLogger.error("Erro ao recarregar configurações de Classes", e);
            }
        }
        loadMessages();
    }

    @Override
    public void onDisable() {
        if (syncManager != null) {
            try {
                syncManager.shutdown();
            } catch (Exception e) {
                MidgardLogger.warn("Erro ao encerrar syncManager de Classes: " + e.getMessage());
            }
        }
        instance = null;
    }

    public static ClassesModule getInstance() {
        return instance;
    }

    public ClassManager getClassManager() {
        return classManager;
    }
    
    public me.ray.midgard.modules.classes.skilltree.SkillTreeManager getSkillTreeManager() {
        return skillTreeManager;
    }

    public me.ray.midgard.core.database.DefinitionRepository getRepository() {
        return repository;
    }

    public me.ray.midgard.core.sync.DefinitionSyncManager getSyncManager() {
        return syncManager;
    }

    private void registerDefaultAttributes() {
        try {
            // We should probably load these from a config too, but for now let's ensure the ones used in classes.yml exist
            String[] defaults = {"strength", "defense", "vitality", "intelligence", "dexterity"};
            for (String id : defaults) {
                if (AttributeRegistry.getInstance().getAttribute(id).isEmpty()) {
                    // Create a default attribute
                    // We need to know how to create attributes. Attribute constructor?
                    // Attribute(id, name, base, min, max)
                    Attribute attr = new Attribute(id, capitalize(id), 0, 0, 1000);
                    AttributeRegistry.getInstance().register(id, attr);
                }
            }
        } catch (Exception e) {
             ConsoleUtils.warn("Erro ao registrar atributos padrão: " + e.getMessage());
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @EventHandler
    public void onLevelUp(PlayerLevelUpEvent event) {
        try {
            Player player = event.getPlayer();
            if (player == null) {
                return;
            }
            
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
            if (profile == null) {
                return;
            }
            
            ClassData data = profile.getData(ClassData.class);
            if (data != null && data.hasClass()) {
                // Sync level
                data.setLevel(event.getNewLevel());
                
                int pointsPerLevel = plugin.getConfig().getInt("modules.classes.points-per-level", 2);
                data.addAttributePoints(pointsPerLevel);
                
                // Send progression messages
                String className = data.getClassName();
                if (classManager != null) {
                    RPGClass c = classManager.getClass(className);
                    if (c != null) {
                        className = c.getDisplayName();
                    }
                }
                
                String levelUpMsg = getMessage("progression.level_up")
                    .replace("%level%", String.valueOf(event.getNewLevel()))
                    .replace("%class%", className)
                    .replace("%class_name%", className);
                MessageUtils.send(player, levelUpMsg);
                
                String pointsMsg = getMessage("attributes.points_received")
                    .replace("%points%", String.valueOf(pointsPerLevel))
                    .replace("%total%", String.valueOf(data.getAttributePoints()));
                MessageUtils.send(player, pointsMsg);

                if (classManager != null) {
                    RPGClass rpgClass = classManager.getClass(data.getClassName());
                    if (rpgClass != null) {
                        java.util.List<ClassSkillLink> skills = rpgClass.getSkills();
                        if (skills != null) {
                            for (ClassSkillLink link : skills) {
                                link.tryUnlock(profile, event.getNewLevel());
                            }
                        }
                    }
                }
                
                // Recalculate attributes
                if (classManager != null) {
                    RPGClass rpgClass = classManager.getClass(data.getClassName());
                    if (rpgClass != null) {
                        applyClassAttributes(profile, rpgClass, data.getLevel());
                        
                        // Fill resources to full on level up (avoids false "damage" visual)
                        me.ray.midgard.modules.combat.CombatManager cm = me.ray.midgard.modules.combat.CombatManager.getInstance();
                        if (cm != null) {
                            cm.fillResources(player);
                        }
                    }
                }
                
                // Persistir imediatamente
                MidgardCore.getProfileManager().saveProfile(profile);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar LevelUp para o jogador", e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            Player player = event.getPlayer();
            if (player == null) {
                return;
            }
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
            
            if (profile == null) {
                // Not necessarily severe, but worth noting if profile system failed
                MidgardLogger.warn("Perfil não encontrado para o jogador " + player.getName());
                return;
            }

            ClassData data = profile.getOrCreateData(ClassData.class);
            
            if (data.hasClass()) {
                if (classManager != null) {
                    RPGClass rpgClass = classManager.getClass(data.getClassName());
                    if (rpgClass != null) {
                        applyClassAttributes(profile, rpgClass, data.getLevel());
                        
                        // Sync skills on join - unlock any skills the player should have based on their level
                        java.util.List<ClassSkillLink> skills = rpgClass.getSkills();
                        if (skills != null) {
                            for (ClassSkillLink link : skills) {
                                link.tryUnlock(profile, data.getLevel());
                            }
                        }
                    } else {
                         MidgardLogger.warn("Classe '" + data.getClassName() + "' não encontrada para o jogador " + player.getName());
                    }
                }
            } else {
                // Open selection GUI if no class
                // Delay slightly to ensure client is ready
                Task.syncLater(player, () -> {
                    if (player.isOnline()) {
                        String welcomeMsg = getMessage("gui.opening_selection");
                        MessageUtils.send(player, welcomeMsg);
                        new ClassSelectionGui(plugin, player, this).open();
                    }
                }, 20L);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao processar onJoin no ClassesModule", e);
        }
    }

    /**
     * Aplica os atributos da classe ao perfil do jogador.
     *
     * @param profile Perfil do jogador.
     * @param rpgClass Classe RPG.
     * @param level Nível da classe.
     */
    public void applyClassAttributes(MidgardProfile profile, RPGClass rpgClass, int level) {
        if (profile == null || rpgClass == null) {
            return;
        }
        try {
            CoreAttributeData attrData = profile.getOrCreateData(CoreAttributeData.class);
            ClassData classData = profile.getData(ClassData.class);
            
            // Collect all relevant attributes (from class definition, spent points, and cross-allocation targets)
            java.util.Set<String> attributesToUpdate = new java.util.HashSet<>();
            if (rpgClass.getBaseAttributes() != null) {
                attributesToUpdate.addAll(rpgClass.getBaseAttributes().keySet());
            }
            if (classData != null && classData.getSpentPoints() != null) {
                attributesToUpdate.addAll(classData.getSpentPoints().keySet());

                // Also include cross-allocation targets (e.g. spending DEF gives VIT)
                me.ray.midgard.modules.combat.CombatManager cm = me.ray.midgard.modules.combat.CombatManager.getInstance();
                if (cm != null && cm.getConfig() != null) {
                    Map<String, Map<String, Double>> pa = cm.getConfig().pointAllocation;
                    for (String spentAttr : classData.getSpentPoints().keySet()) {
                        Map<String, Double> targets = pa.get(spentAttr.toLowerCase());
                        if (targets != null) {
                            attributesToUpdate.addAll(targets.keySet());
                        }
                    }
                }
            }

            // Calculate stats
            for (String attrId : attributesToUpdate) {
                double base = rpgClass.getBaseAttributes() != null ? rpgClass.getBaseAttributes().getOrDefault(attrId, 0.0) : 0.0;
                double perLevel = rpgClass.getAttributesPerLevel() != null ? rpgClass.getAttributesPerLevel().getOrDefault(attrId, 0.0) : 0.0;
                
                double total = base + (perLevel * (level - 1));
                
                // Add spent points (using point-allocation config from CombatConfig)
                // Each spent attribute can contribute to multiple primary attributes
                if (classData != null && classData.getSpentPoints() != null) {
                    me.ray.midgard.modules.combat.CombatManager combatManager = me.ray.midgard.modules.combat.CombatManager.getInstance();
                    Map<String, Map<String, Double>> pointAlloc = (combatManager != null && combatManager.getConfig() != null)
                            ? combatManager.getConfig().pointAllocation : null;

                    for (Map.Entry<String, Integer> entry : classData.getSpentPoints().entrySet()) {
                        String spentAttr = entry.getKey();
                        int points = entry.getValue();
                        if (points <= 0) {
                            continue;
                        }

                        if (pointAlloc != null && pointAlloc.containsKey(spentAttr.toLowerCase())) {
                            // Config-driven: check if this spent attribute gives bonus to attrId
                            Double valuePerPoint = pointAlloc.get(spentAttr.toLowerCase()).get(attrId.toLowerCase());
                            if (valuePerPoint != null) {
                                total += points * valuePerPoint;
                            }
                        } else if (spentAttr.equalsIgnoreCase(attrId)) {
                            // Fallback: 1 point = 1 value (backwards compatible)
                            total += points;
                        }
                    }
                }
                
                AttributeInstance instance = attrData.getInstance(attrId);
                if (instance == null) {
                    // Try to register if missing? Or just skip?
                    // Safe to skip if attribute doesn't exist in registry, avoiding crash
                    // But ideally we should ensure it exists.
                    continue; 
                }
                
                instance.setBaseValue(total);
            }
            
            // Apply Health and Mana using dedicated RPGClass getters
            // (health/mana are stored in separate YAML sections, NOT in baseAttributes map)
            if (CombatModule.getInstance() != null) {
                // Base Health + (HealthPerLevel * Level)
                double baseHealth = rpgClass.getBaseHealth();
                double healthPerLevel = rpgClass.getHealthPerLevel();
                double totalHealth = baseHealth + (healthPerLevel * (level - 1));
                
                // Add Vitality contribution if applicable (e.g. 1 Vitality = 5 HP)
                // This is optional and depends on design. Assuming simple base stats for now.
                
                AttributeInstance healthAttr = attrData.getInstance(CombatAttributes.MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.setBaseValue(totalHealth);
                }
                
                // Base Mana
                double baseMana = rpgClass.getBaseMana();
                double manaPerLevel = rpgClass.getManaPerLevel();
                double totalMana = baseMana + (manaPerLevel * (level - 1));
                
                AttributeInstance manaAttr = attrData.getInstance(CombatAttributes.MAX_MANA);
                if (manaAttr != null) {
                    manaAttr.setBaseValue(totalMana);
                }
            }
            
        } catch (Exception e) {
            MidgardLogger.error("Erro ao aplicar atributos da classe", e);
        }
    }
    
    private void loadMessages() {
        File file = new File(plugin.getDataFolder(), "modules/classes/lang/messages.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("modules/classes/lang/messages.yml", false);
            } catch (Exception e) {
                ConsoleUtils.warn("Classes: " + e.getMessage());
            }
        }
        messagesConfig = YamlConfiguration.loadConfiguration(file);
    }

    public String getMessage(String path) {
        if (messagesConfig == null) {
            return path;
        }
        
        String msg = messagesConfig.getString(path);
        if (msg != null) {
            return msg.replace("&", "§");
        }
        
        return path;
    }
    
    public List<String> getMessageList(String path) {
        if (messagesConfig == null) {
            return Collections.emptyList();
        }
        List<String> list = messagesConfig.getStringList(path);
        if (list == null) {
            return Collections.emptyList();
        }
        
        List<String> colored = new ArrayList<>();
        for (String line : list) {
            colored.add(line.replace("&", "§"));
        }
        return colored;
    }
    
}
