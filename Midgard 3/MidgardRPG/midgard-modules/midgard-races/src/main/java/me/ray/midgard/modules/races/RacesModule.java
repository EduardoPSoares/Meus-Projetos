package me.ray.midgard.modules.races;

import me.ray.midgard.core.ModulePriority;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.utils.ConsoleUtils;
import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.races.command.RaceCommand;
import me.ray.midgard.modules.races.manager.RaceLevelManager;
import me.ray.midgard.modules.races.manager.RaceManager;
import me.ray.midgard.modules.races.task.RaceTraitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RacesModule extends RPGModule {

    private static volatile RacesModule instance;
    private RaceManager raceManager;
    private RaceLevelManager levelManager;
    private me.ray.midgard.modules.races.listener.RaceAttributeListener attributeListener;
    private RaceTraitRunnable traitRunnable;
    private BukkitTask traitTask;
    private FileConfiguration messagesConfig;
    private FileConfiguration guiMessagesConfig;
    private me.ray.midgard.core.database.DefinitionRepository repository;
    private me.ray.midgard.core.sync.DefinitionSyncManager syncManager;

    public RacesModule() {
        super("MidgardRaces", ModulePriority.NORMAL);
    }

    @Override
    public void onEnable() {
        instance = this;
        ConsoleUtils.info("Habilitando Midgard-Races...");

        loadMessages();

        // Initialize managers
        this.raceManager = new RaceManager(this);
        this.levelManager = new RaceLevelManager(this);
        
        // Initialize DB repository + migrate YAML if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new me.ray.midgard.core.database.DefinitionRepository(dbManager, "midgard_races");
            File racesFile = new File(getDataFolder(), "races.yml");
            new me.ray.midgard.core.database.DefinitionMigrationTool(repository, "races")
                .migrateSingleFile(racesFile, "races", "race");
        }
        
        // Register Traits
        registerTraits();

        // Register Placeholders if PAPI is enabled
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new RacePlaceholders(this).register();
        }

        this.raceManager.loadRaces();

        // Register commands - unified under /rpg race
        RaceCommand raceCommand = new RaceCommand(this);
        me.ray.midgard.core.command.UnifiedCommandManager commandManager = me.ray.midgard.core.MidgardCore.getCommandManager();
        if (commandManager != null) {
            commandManager.registerPlayerCommand(raceCommand);
        }

        // Register listeners
        this.attributeListener = new me.ray.midgard.modules.races.listener.RaceAttributeListener(this);
        plugin.getServer().getPluginManager().registerEvents(attributeListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.races.listener.RaceTraitListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.races.listener.RacePermissionListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.races.listener.RaceXpListener(this), plugin);

        // Start tasks
        this.traitRunnable = new RaceTraitRunnable(this);
        this.traitTask = Task.syncTimer(traitRunnable, 20L, 20L);

        // Start sync
        if (repository != null) {
            me.ray.midgard.core.redis.RedisManager redisManager = me.ray.midgard.core.MidgardCore.getRedisManager();
            this.syncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                "races", repository, redisManager, 30,
                id -> {
                    repository.load(id).thenAccept(data -> {
                        if (data != null) { me.ray.midgard.core.utils.Task.sync(() -> raceManager.reloadRaceFromDb(id, data)); }
                    });
                },
                id -> me.ray.midgard.core.utils.Task.sync(() -> raceManager.unregisterRace(id)),
                () -> { raceManager.loadRaces(); },
                dbIds -> {
                    java.util.Set<String> dbSet = new java.util.HashSet<>(dbIds);
                    for (String loadedId : new java.util.ArrayList<>(raceManager.getRaces().stream()
                            .map(me.ray.midgard.modules.races.model.Race::getId).toList())) {
                        if (!dbSet.contains(loadedId)) {
                            raceManager.unregisterRace(loadedId);
                        }
                    }
                }
            );
        }

        ConsoleUtils.success("Midgard-Races habilitado com sucesso!");
    }

    private void registerTraits() {
        me.ray.midgard.modules.races.registry.TraitRegistry registry = me.ray.midgard.modules.races.registry.TraitRegistry.getInstance();
        registry.register(new me.ray.midgard.modules.races.trait.PotionEffectTrait());
        registry.register(new me.ray.midgard.modules.races.trait.DamageModifierTrait());
        registry.register(new me.ray.midgard.modules.races.trait.AttributeModifierTrait());
        registry.register(new me.ray.midgard.modules.races.trait.ExpBoostTrait());
        registry.register(new me.ray.midgard.modules.races.trait.CommandTrait());
        registry.register(new me.ray.midgard.modules.races.trait.ActiveAbilityTrait());
        registry.register(new me.ray.midgard.modules.races.trait.BiomeBuffTrait());
        registry.register(new me.ray.midgard.modules.races.trait.TimeBuffTrait());
        registry.register(new me.ray.midgard.modules.races.trait.SunBurnTrait());
        registry.register(new me.ray.midgard.modules.races.trait.LifeStealTrait());
        registry.register(new me.ray.midgard.modules.races.trait.DietRestrictionTrait());
        registry.register(new me.ray.midgard.modules.races.trait.RegenTrait());
        registry.register(new me.ray.midgard.modules.races.trait.WaterBreathingTrait());
        registry.register(new me.ray.midgard.modules.races.trait.FallResistanceTrait());
        registry.register(new me.ray.midgard.modules.races.trait.FireResistanceTrait());
        registry.register(new me.ray.midgard.modules.races.trait.NightVisionTrait());
        registry.register(new me.ray.midgard.modules.races.trait.TeleportTrait());
        registry.register(new me.ray.midgard.modules.races.trait.TransformTrait());
        registry.register(new me.ray.midgard.modules.races.trait.AuraTrait());
        registry.register(new me.ray.midgard.modules.races.trait.ParticleTrait());
        registry.register(new me.ray.midgard.modules.races.trait.MountTrait());
        registry.register(new me.ray.midgard.modules.races.trait.HarvestTrait());
    }

    @Override
    public void onDisable() {
        if (syncManager != null) {
            try { syncManager.shutdown(); } catch (Exception ignored) { /* Shutdown pode falhar se já encerrado */ }
        }
        if (traitTask != null) {
            traitTask.cancel();
            traitTask = null;
        }
        ConsoleUtils.info("Desabilitando Midgard-Races...");
        instance = null;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadMessages();
        if (raceManager != null) { raceManager.loadRaces(); }
        if (levelManager != null) { levelManager.loadConfig(); }
    }

    public void loadMessages() {
        File file = new File(getDataFolder(), "lang/messages.yml");
        if (!file.exists()) {
            saveResource("modules/races/lang/messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(file);
        
        // Load GUI messages
        File guiFile = new File(getDataFolder(), "lang/gui_messages.yml");
        if (!guiFile.exists()) {
            saveResource("modules/races/lang/gui_messages.yml", false);
        }
        guiMessagesConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    public String getMessage(String key) {
        if (messagesConfig == null) { return key; }
        return messagesConfig.getString(key, key);
    }

    public List<String> getMessageList(String... keys) {
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            if (messagesConfig != null) {
                if (messagesConfig.isList(key)) {
                    result.addAll(messagesConfig.getStringList(key));
                } else {
                    String msg = messagesConfig.getString(key);
                    if (msg != null) { result.add(msg); }
                }
            }
        }
        return result;
    }

    public String getGuiMessage(String key) {
        if (guiMessagesConfig == null) { return key; }
        return guiMessagesConfig.getString(key, key);
    }

    public List<String> getGuiMessageList(String key) {
        if (guiMessagesConfig == null) { return new ArrayList<>(); }
        List<String> list = guiMessagesConfig.getStringList(key);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    /**
     * Verifica se é dia no mundo baseado nos valores de night-start/night-end do config.
     */
    public static boolean isDayTime(long worldTime) {
        RacesModule mod = instance;
        long nightStart = (mod != null) ? mod.getConfig().getLong("settings.night-start", 12610) : 12610;
        long nightEnd = (mod != null) ? mod.getConfig().getLong("settings.night-end", 23041) : 23041;
        return worldTime < nightStart || worldTime > nightEnd;
    }

    public static RacesModule getInstance() {
        return instance;
    }

    public RaceManager getRaceManager() {
        return raceManager;
    }

    public RaceLevelManager getLevelManager() {
        return levelManager;
    }

    public me.ray.midgard.modules.races.listener.RaceAttributeListener getAttributeListener() {
        return attributeListener;
    }

    public RaceTraitRunnable getTraitRunnable() {
        return traitRunnable;
    }

    public me.ray.midgard.core.database.DefinitionRepository getRepository() {
        return repository;
    }

    public me.ray.midgard.core.sync.DefinitionSyncManager getSyncManager() {
        return syncManager;
    }

    public File getDataFolder() {
        return new File(plugin.getDataFolder(), "modules/races");
    }

    public void saveResource(String resourcePath, boolean replace) {
        plugin.saveResource(resourcePath, replace);
    }
    
    public String getAttributeName(String key) {
        String path = "attributes.names." + key;
        if (messagesConfig != null && messagesConfig.contains(path)) {
            return messagesConfig.getString(path);
        }
        return prettify(key);
    }

    public String getTraitName(String key) {
        String path = "attributes.traits." + key;
        if (messagesConfig != null && messagesConfig.contains(path)) {
            return messagesConfig.getString(path);
        }
        return prettify(key);
    }

    private String prettify(String text) {
        if (text == null) { return ""; }
        String[] words = text.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) { continue; }
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) { sb.append(word.substring(1).toLowerCase()); }
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}
