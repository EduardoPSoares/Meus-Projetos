package me.ray.midgard.modules.economy;

import me.ray.midgard.core.ModulePriority;
import me.ray.midgard.core.RPGModule;
import me.ray.midgard.core.utils.ConsoleUtils;
import me.ray.midgard.modules.economy.command.EconomyAdminCommand;
import me.ray.midgard.modules.economy.manager.CurrencyManager;
import me.ray.midgard.modules.economy.manager.PouchManager;

import java.io.File;

public class EconomyModule extends RPGModule {

    private static volatile EconomyModule instance;
    private CurrencyManager currencyManager;
    private PouchManager pouchManager;
    private me.ray.midgard.core.database.DefinitionRepository repository;
    private me.ray.midgard.core.sync.DefinitionSyncManager syncManager;

    public EconomyModule() {
        super("MidgardEconomy", ModulePriority.NORMAL);
    }

    public File getDataFolder() {
        return new File(plugin.getDataFolder(), "modules/economy");
    }

    @Override
    public void onEnable() {
        instance = this;
        // ConsoleUtils.info("Habilitando Midgard-Economy...");

        // Initialize DB repository + migrate config if needed
        me.ray.midgard.core.database.DatabaseManager dbManager = me.ray.midgard.core.MidgardCore.getDatabaseManager();
        if (dbManager != null) {
            this.repository = new me.ray.midgard.core.database.DefinitionRepository(dbManager, "midgard_economy_config");
            File configFile = new File(plugin.getDataFolder(), "modules/economy/config.yml");
            new me.ray.midgard.core.database.DefinitionMigrationTool(repository, "economy_config")
                .migrateWholeConfig(configFile, "economy_config");
        }

        // Carregar configuração principal
        reloadConfig();

        // Instalar recursos (itens) se necessário
        boolean newResources = installResources();

        // Se novos recursos foram instalados, recarregar ItemManager para reconhecê-los
        if (newResources && me.ray.midgard.modules.item.ItemModule.getInstance() != null) {
            me.ray.midgard.modules.item.ItemModule.getInstance().getItemManager().loadItems();
            me.ray.midgard.core.debug.MidgardLogger.debug("Recursos de economia instalados. ItemManager recarregado.");
        } else {
            // Force reload anyway to ensure sync if resources existed but weren't loaded correctly due to priority
            if (me.ray.midgard.modules.item.ItemModule.getInstance() != null) {
                me.ray.midgard.modules.item.ItemModule.getInstance().getItemManager().loadItems();
                me.ray.midgard.core.debug.MidgardLogger.debug("EconomyModule forcing ItemManager reload to ensure currency sync.");
            }
        }

        // Managers
        if (getConfig().getBoolean("features.currency-system", true)) {
            this.currencyManager = new CurrencyManager(this);
            me.ray.midgard.core.debug.MidgardLogger.debug("Sistema de Moedas habilitado.");
            
            // Registrar provedor de economia física no Core
            me.ray.midgard.core.MidgardCore.setEconomyProvider(new PhysicalEconomyProvider());
            me.ray.midgard.core.debug.MidgardLogger.debug("Provedor de Economia Física registrado no Core.");
        }

        if (getConfig().getBoolean("features.pouch-system", true)) {
            this.pouchManager = new PouchManager(this);
            plugin.getServer().getPluginManager().registerEvents(new me.ray.midgard.modules.economy.listener.PouchListener(this), plugin);
            me.ray.midgard.core.debug.MidgardLogger.debug("Sistema de Bolsas habilitado.");
        }
        
        // Commands
        EconomyAdminCommand cmd = new EconomyAdminCommand(this);
        me.ray.midgard.core.MidgardCore.getAdminCommand().registerSubcommand(cmd);
        
        // Start config sync
        if (repository != null) {
            me.ray.midgard.core.redis.RedisManager redisManager = me.ray.midgard.core.MidgardCore.getRedisManager();
            this.syncManager = new me.ray.midgard.core.sync.DefinitionSyncManager(
                "economy_config", repository, redisManager, 30,
                id -> me.ray.midgard.core.utils.Task.sync(() -> reloadConfig()),
                id -> {},
                () -> me.ray.midgard.core.utils.Task.sync(() -> reloadConfig()),
                null
            );
        }
        
        // Player Command REMOVED as per user request (mideco only for admin via /rpg admin econ)
        // me.ray.midgard.modules.economy.command.EconomyCommand playerCmd = new me.ray.midgard.modules.economy.command.EconomyCommand(this);
        // me.ray.midgard.core.MidgardCore.getCommandManager().registerPlayerCommand(playerCmd);
    }
    
    public class PhysicalEconomyProvider implements me.ray.midgard.core.economy.EconomyProvider {

        @Override
        public double getBalance(java.util.UUID player, String currency) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(player);
            if (p == null) {
                return 0;
            }
            return currencyManager.getPhysicalBalance(p);
        }

        @Override
        public void setBalance(java.util.UUID player, String currency, double amount) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(player);
            if (p == null) {
                return;
            }
            
            // Set balance is tricky for physical. We try to clear and give.
            currencyManager.removeCurrencyItems(p);
            currencyManager.giveCurrency(p, (int) amount);
        }

        @Override
        public void deposit(java.util.UUID player, String currency, double amount) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(player);
            if (p == null) {
                return;
            }
            currencyManager.giveCurrency(p, (int) amount);
        }

        @Override
        public void withdraw(java.util.UUID player, String currency, double amount) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(player);
            if (p == null) {
                return;
            }
            currencyManager.takeCurrency(p, (int) amount);
        }

        @Override
        public boolean has(java.util.UUID player, String currency, double amount) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(player);
            if (p == null) {
                return false;
            }
            return currencyManager.getPhysicalBalance(p) >= amount;
        }

        @Override
        public String format(String currency, double amount) {
            return getMessage("currency.format").replace("%amount%", String.valueOf((int) amount));
        }
    }

    @Override
    public void onDisable() {
        if (syncManager != null) {
            try {
                syncManager.shutdown();
            } catch (Exception e) {
                me.ray.midgard.core.debug.MidgardLogger.warn("Error shutting down economy sync manager: " + e.getMessage());
            }
        }
        instance = null;
    }
    
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        
        // Override with DB data if available (after migration, the YAML file may be empty)
        if (repository != null && repository.count() > 0) {
            me.ray.midgard.core.database.DefinitionRepository.DefinitionData data = 
                repository.loadAll().get("economy_config");
            if (data != null) {
                org.bukkit.configuration.file.FileConfiguration dbConfig = 
                    me.ray.midgard.core.database.DefinitionMigrationTool.deserializeToConfig(data.yamlData());
                for (String key : dbConfig.getKeys(true)) {
                    if (!dbConfig.isConfigurationSection(key)) {
                        getConfig().set(key, dbConfig.get(key));
                    }
                }
            }
        }
        
        if (currencyManager != null) {
            currencyManager.reload();
        }
        if (pouchManager != null) {
            pouchManager.reload();
        }
    }

    public static EconomyModule getInstance() {
        return instance;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public PouchManager getPouchManager() {
        return pouchManager;
    }

    private boolean installResources() {
        boolean created = false;
        
        me.ray.midgard.modules.item.ItemModule itemModule = me.ray.midgard.modules.item.ItemModule.getInstance();
        me.ray.midgard.modules.item.repository.ItemRepository itemRepo = itemModule != null ? itemModule.getItemRepository() : null;

        // Instalar Currencies
        if (getConfig().getBoolean("features.currency-system", true)) {
            for (String file : getConfig().getStringList("resources.currencies")) {
                if (seedItemFromJar("modules/economy/items/currency/" + file, itemRepo)) {
                    created = true;
                    ConsoleUtils.info("EconomyModule: Seeded currency item from " + file);
                }
            }
        }

        // Instalar Pouches
        if (getConfig().getBoolean("features.pouch-system", true)) {
            for (String file : getConfig().getStringList("resources.pouches")) {
                if (seedItemFromJar("modules/economy/items/pouch/" + file, itemRepo)) {
                    created = true;
                    ConsoleUtils.info("EconomyModule: Seeded pouch item from " + file);
                }
            }
        }
        
        if (created) {
             ConsoleUtils.info("EconomyModule: New resources created. Requesting ItemManager reload.");
        }
        
        return created;
    }

    /**
     * Seeds item definitions from a JAR resource YAML into the item database.
     * Each top-level key in the YAML is treated as an item ID.
     * Returns true if any items were seeded.
     */
    private boolean seedItemFromJar(String resourcePath, me.ray.midgard.modules.item.repository.ItemRepository itemRepo) {
        if (itemRepo == null) {
            return false;
        }

        try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return false;
            }

            org.bukkit.configuration.file.YamlConfiguration config =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));

            boolean seeded = false;
            for (String key : config.getKeys(false)) {
                if (!config.isConfigurationSection(key)) {
                    continue;
                }

                // Check if already in DB
                me.ray.midgard.modules.item.ItemModule itemModule = me.ray.midgard.modules.item.ItemModule.getInstance();
                if (itemModule != null && itemModule.getItemManager().getItem(key) != null) {
                    continue;
                }

                org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(key);
                String categoryId = "MISCELLANEOUS";
                if (section.isConfigurationSection("base")) {
                    String type = section.getConfigurationSection("base").getString("type", null);
                    if (type != null) {
                        categoryId = type.toUpperCase();
                    }
                }
                String yamlData = serializeSection(section);
                itemRepo.saveItem(key, categoryId, yamlData, "economy-seed").join();
                seeded = true;
            }
            return seeded;
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.warn("EconomyModule: Failed to seed item from " + resourcePath + ": " + e.getMessage());
            return false;
        }
    }

    private static String serializeSection(org.bukkit.configuration.ConfigurationSection section) {
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        for (String key : section.getKeys(true)) {
            if (!section.isConfigurationSection(key)) {
                yaml.set(key, section.get(key));
            }
        }
        return yaml.saveToString();
    }
}
