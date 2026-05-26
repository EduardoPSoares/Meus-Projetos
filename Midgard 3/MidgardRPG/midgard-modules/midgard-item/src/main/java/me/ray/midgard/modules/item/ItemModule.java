package me.ray.midgard.modules.item;

import me.ray.midgard.core.ModulePriority;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.ConsoleUtils;
import me.ray.midgard.modules.item.command.ItemCommand;
import me.ray.midgard.modules.item.listener.ItemAbilityListener;
import me.ray.midgard.modules.item.manager.CategoryManager;
import me.ray.midgard.modules.item.manager.ItemManager;
import me.ray.midgard.modules.item.task.PassiveAbilityTask;

import java.io.File;

public class ItemModule extends RPGModule {

    private static volatile ItemModule instance;
    private me.ray.midgard.modules.item.manager.TierManager tierManager;
    private ItemManager itemManager;
    private CategoryManager categoryManager;
    private me.ray.midgard.modules.item.manager.RecipeManager recipeManager;
    private me.ray.midgard.modules.item.manager.UpgradeManager upgradeManager;
    private me.ray.midgard.modules.item.gui.ItemEditionMessagesLoader itemEditionLoader;
    private me.ray.midgard.modules.item.task.EquipmentUpdateTask equipmentUpdateTask;
    private org.bukkit.scheduler.BukkitTask equipmentUpdateBukkitTask;
    private ItemAbilityListener itemAbilityListener;
    private PassiveAbilityTask passiveAbilityTask;
    private me.ray.midgard.modules.item.repository.ItemRepository itemRepository;
    private me.ray.midgard.modules.item.repository.ItemSyncManager itemSyncManager;
    private me.ray.midgard.core.sync.DefinitionSyncManager tierSyncManager;
    private me.ray.midgard.core.sync.DefinitionSyncManager categorySyncManager;
    private me.ray.midgard.core.sync.DefinitionSyncManager upgradeSyncManager;

    public ItemModule() {
        super("MidgardItem", ModulePriority.NORMAL);
    }

    @Override
    public void onEnable() {
        instance = this;
        ConsoleUtils.info("Habilitando Midgard-Item...");

        // Ensure module folder exists
        // Super reloadConfig handles config.yml creation via ConfigWrapper

        // Save other default files
        saveExtraResources();

        // Inicializar gerenciadores
        this.tierManager = new me.ray.midgard.modules.item.manager.TierManager(this);
        this.tierManager.loadTiers();

        this.categoryManager = new CategoryManager(this);
        this.categoryManager.loadCategories();

        this.itemEditionLoader = new me.ray.midgard.modules.item.gui.ItemEditionMessagesLoader(this);
        this.itemEditionLoader.load();

        // Inicializar repositório no banco de dados
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.itemRepository = new me.ray.midgard.modules.item.repository.ItemRepository(dbManager);
            // Migrar YAML → DB se o banco estiver vazio e existirem YAMLs
            new me.ray.midgard.modules.item.repository.ItemMigrationTool(itemRepository, getDataFolder()).migrateIfNeeded();
        }

        this.itemManager = new ItemManager(this);
        this.itemManager.loadItems();

        this.upgradeManager = new me.ray.midgard.modules.item.manager.UpgradeManager(this);

        this.recipeManager = new me.ray.midgard.modules.item.manager.RecipeManager(this);
        this.recipeManager.registerRecipes();
        
        // Registrar comando item no AdminCommand
        if (me.ray.midgard.core.MidgardCore.getAdminCommand() != null) {
            me.ray.midgard.core.MidgardCore.getAdminCommand().registerSubcommand(new ItemCommand(this));
        }

        // Registrar listeners
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.item.listener.ChatInputListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.item.listener.ItemUpdateListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.item.listener.GemListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.item.listener.EquipListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.item.listener.ConsumableListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.item.listener.ItemRestrictionListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.item.listener.RngCraftingListener(this), plugin);
        
        // Register Item Ability Listener (handles spell bindings on items)
        this.itemAbilityListener = new ItemAbilityListener(this);
        plugin.getServer().getPluginManager().registerEvents(this.itemAbilityListener, plugin);

        // Iniciar tarefas
        this.equipmentUpdateTask = new me.ray.midgard.modules.item.task.EquipmentUpdateTask();
        this.equipmentUpdateBukkitTask = me.ray.midgard.core.utils.Task.syncTimer(this.equipmentUpdateTask, 20L, 10L); // Run every 10 ticks (0.5s)
        
        // Start Passive Ability Task (handles timer-based spell bindings)
        this.passiveAbilityTask = new PassiveAbilityTask(this);
        this.passiveAbilityTask.start();

        // Iniciar sincronização entre servidores (Redis ou polling)
        me.ray.midgard.core.redis.RedisManager redisManager = me.ray.midgard.core.MidgardCore.getRedisManager();
        if (itemRepository != null) {
            this.itemSyncManager = new me.ray.midgard.modules.item.repository.ItemSyncManager(this, itemRepository, redisManager);
            this.itemSyncManager.start();
        }

        // Start sync for tiers
        if (tierManager.getRepository() != null) {
            this.tierSyncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                "item_tiers", tierManager.getRepository(), redisManager, 30,
                (id) -> tierManager.getRepository().load(id).thenAccept(data -> {
                    if (data != null) {
                        me.ray.midgard.core.utils.Task.sync(() -> tierManager.reloadTierFromDb(id, data));
                    }
                }),
                (id) -> tierManager.unregisterTier(id),
                () -> tierManager.loadTiers(),
                (dbIds) -> {
                    java.util.Set<String> dbSet = new java.util.HashSet<>();
                    for (String dbId : dbIds) {
                        dbSet.add(dbId.toLowerCase());
                    }
                    for (String loadedId : new java.util.ArrayList<>(tierManager.getTiers().stream()
                            .map(me.ray.midgard.modules.item.manager.TierManager.Tier::getId).toList())) {
                        if (!dbSet.contains(loadedId.toLowerCase())) {
                            tierManager.unregisterTier(loadedId);
                        }
                    }
                }
            );
        }

        // Start sync for categories
        if (categoryManager.getRepository() != null) {
            this.categorySyncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                "item_categories", categoryManager.getRepository(), redisManager, 30,
                (id) -> categoryManager.getRepository().load(id).thenAccept(data -> {
                    if (data != null) {
                        me.ray.midgard.core.utils.Task.sync(() -> categoryManager.reloadCategoryFromDb(id, data));
                    }
                }),
                (id) -> categoryManager.unregisterCategory(id),
                () -> categoryManager.loadCategories(),
                (dbIds) -> {
                    java.util.Set<String> dbSet = new java.util.HashSet<>(dbIds);
                    for (String loadedId : new java.util.ArrayList<>(categoryManager.getCategories().stream()
                            .map(me.ray.midgard.modules.item.model.ItemCategory::getId).toList())) {
                        if (!dbSet.contains(loadedId)) {
                            categoryManager.unregisterCategory(loadedId);
                        }
                    }
                }
            );
        }

        // Start sync for upgrade config
        if (upgradeManager.getRepository() != null) {
            this.upgradeSyncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                "upgrade_config", upgradeManager.getRepository(), redisManager, 30,
                (id) -> upgradeManager.loadConfig(),
                (id) -> {},
                () -> upgradeManager.loadConfig(),
                (ids) -> {}
            );
        }

        // ConsoleUtils.success("Midgard-Item habilitado com sucesso!");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (tierManager != null) {
            tierManager.loadTiers();
        }
        if (itemEditionLoader != null) {
            itemEditionLoader.load();
        }
        if (upgradeManager != null) {
            upgradeManager.loadConfig();
        }
        if (categoryManager != null) {
            categoryManager.loadCategories();
        }
        if (itemManager != null) {
            itemManager.loadItems();
            if (recipeManager != null) {
                recipeManager.reload();
            }
        }
    }

    @Override
    public void onDisable() {
        ConsoleUtils.info("Desabilitando Midgard-Item...");

        // Stop item sync
        if (itemSyncManager != null) {
            try { itemSyncManager.shutdown(); } catch (Exception ignored) { /* Shutdown cleanup */ }
        }
        if (tierSyncManager != null) {
            try { tierSyncManager.shutdown(); } catch (Exception ignored) { /* Shutdown cleanup */ }
        }
        if (categorySyncManager != null) {
            try { categorySyncManager.shutdown(); } catch (Exception ignored) { /* Shutdown cleanup */ }
        }
        if (upgradeSyncManager != null) {
            try { upgradeSyncManager.shutdown(); } catch (Exception ignored) { /* Shutdown cleanup */ }
        }
        
        // Stop equipment update task
        if (equipmentUpdateBukkitTask != null) {
            try {
                equipmentUpdateBukkitTask.cancel();
            } catch (Exception ignored) { /* Shutdown cleanup */ }
        }
        
        // Stop passive ability task
        if (passiveAbilityTask != null) {
            try {
                passiveAbilityTask.stop();
            } catch (Exception ignored) { /* Shutdown cleanup */ }
        }
        
        instance = null;
    }

    public me.ray.midgard.modules.item.manager.RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public me.ray.midgard.modules.item.manager.UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public me.ray.midgard.modules.item.manager.TierManager getTierManager() {
        return tierManager;
    }

    public static ItemModule getInstance() {
        return instance;
    }

    public me.ray.midgard.modules.item.gui.ItemEditionMessagesLoader getItemEditionLoader() {
        return itemEditionLoader;
    }

    public File getDataFolder() {
        return new File(plugin.getDataFolder(), "modules/item");
    }

    public void saveExtraResources() {
        File folder = getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        
        String[] resources = {"item-tiers.yml", "item-sets.yml", "custom-stats.yml", "item-types.yml"};
        for (String res : resources) {
            File resFile = new File(folder, res);
            if (!resFile.exists()) {
                try {
                    plugin.saveResource("modules/item/" + res, false);
                } catch (IllegalArgumentException e) {
                    MidgardLogger.warn("Could not save resource " + res + ": " + e.getMessage());
                }
            }
        }

        // Save Item Edition GUI files
        String editionPath = "modules/item/lang/gui/item_edition/";
        String[] editionFiles = {
            "_main.yml",
            "editors.yml",
            "categories/attributes.yml",
            "categories/combat_defense.yml",
            "categories/combat_offense.yml",
            "categories/consumables.yml",
            "categories/display.yml",
            "categories/general.yml",
            "categories/misc.yml",
            "categories/restrictions.yml",
            "categories/tools.yml"
        };
        
        for (String file : editionFiles) {
             File f = new File(plugin.getDataFolder(), editionPath + file);
             if (!f.exists()) {
                 try {
                     plugin.saveResource(editionPath + file, false);
                 } catch (Exception e) {
                     MidgardLogger.warn("Could not save resource " + file + ": " + e.getMessage());
                 }
             }
        }

        // Save default item definition files from resources
        String[] defaultItemFiles = {
            "items/accessories/ornaments.yml",
            "items/accessories/rings.yml",
            "items/accessories/talismans.yml",
            "items/armor/heavy_armor.yml",
            "items/armor/light_armor.yml",
            "items/armor/magical_armor.yml",
            "items/armor/medium_armor.yml",
            "items/consumables/food.yml",
            "items/consumables/potions.yml",
            "items/gems/gem_stones.yml",
            "items/materials/essences.yml",
            "items/materials/gems_raw.yml",
            "items/materials/ingots.yml",
            "items/materials/rare_materials.yml",
            "items/weapons/axes.yml",
            "items/weapons/bows.yml",
            "items/weapons/daggers.yml",
            "items/weapons/hammers.yml",
            "items/weapons/other_weapons.yml",
            "items/weapons/spears.yml",
            "items/weapons/staffs.yml",
            "items/weapons/swords.yml",
            "items/weapons/unique_weapons.yml"
        };

        for (String itemFile : defaultItemFiles) {
            File f = new File(folder, itemFile);
            if (!f.exists()) {
                try {
                    plugin.saveResource("modules/item/" + itemFile, false);
                } catch (Exception e) {
                    // Resource may not exist in JAR, skip silently
                }
            }
        }
    }
    
    public void saveResource(String resourcePath, boolean replace) {
        plugin.saveResource(resourcePath, replace);
    }

    public ItemManager getItemManager() {
        return itemManager;
    }
    
    public CategoryManager getCategoryManager() {
        return categoryManager;
    }
    
    public ItemAbilityListener getItemAbilityListener() {
        return itemAbilityListener;
    }
    
    public PassiveAbilityTask getPassiveAbilityTask() {
        return passiveAbilityTask;
    }

    public me.ray.midgard.modules.item.repository.ItemRepository getItemRepository() {
        return itemRepository;
    }

    public me.ray.midgard.modules.item.repository.ItemSyncManager getItemSyncManager() {
        return itemSyncManager;
    }


}
