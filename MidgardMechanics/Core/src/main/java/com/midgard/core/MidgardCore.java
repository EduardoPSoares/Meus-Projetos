package com.midgard.core;

import com.midgard.core.command.CommandRegistry;
import com.midgard.core.config.ConfigManager;
import com.midgard.core.gui.GuiListener;
import com.midgard.core.module.ModuleManager;
import com.midgard.core.task.TaskManager;
import com.midgard.core.utils.MessageUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class MidgardCore extends JavaPlugin {

    private static MidgardCore instance;

    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private CommandRegistry commandRegistry;
    private TaskManager taskManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.taskManager = new TaskManager(this);
        this.commandRegistry = new CommandRegistry(this);
        this.moduleManager = new ModuleManager(this);

        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        MessageUtils.init(getConfig().getString("prefix", "&8[&6Midgard&8] "));

        getLogger().info("MidgardCore enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (taskManager != null) {
            taskManager.cancelAll();
        }
        instance = null;
        getLogger().info("MidgardCore disabled.");
    }

    public static MidgardCore getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }
}
