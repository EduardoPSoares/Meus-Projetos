package me.ray.midgard.bot.core.module;

import me.ray.midgard.bot.MidgardBot;
import me.ray.midgard.bot.core.command.BaseCommand;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BotModule {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected MidgardBot bot;

    private boolean enabled = false;
    private ModuleInfo info;
    private final List<BaseCommand> registeredCommands = new ArrayList<>();
    private final List<ListenerAdapter> registeredListeners = new ArrayList<>();

    public final void initialize(MidgardBot bot) {
        this.bot = bot;
        this.info = getClass().getAnnotation(ModuleInfo.class);
        if (this.info == null) {
            throw new IllegalStateException("Module " + getClass().getSimpleName() + " is missing @ModuleInfo annotation");
        }
    }

    public abstract void onEnable();

    public abstract void onDisable();

    // ==================== Command Registration ====================

    protected void registerCommand(BaseCommand command) {
        bot.getCommandManager().registerCommand(command);
        registeredCommands.add(command);
    }

    protected void registerCommands(BaseCommand... commands) {
        for (BaseCommand cmd : commands) {
            registerCommand(cmd);
        }
    }

    // ==================== Listener Registration ====================

    protected void registerListener(ListenerAdapter listener) {
        bot.getJda().addEventListener(listener);
        registeredListeners.add(listener);
    }

    // ==================== Lifecycle ====================

    public final void enable() {
        if (enabled) return;
        logger.info("Enabling module: {} v{}", getName(), getVersion());
        onEnable();
        enabled = true;
    }

    public final void disable() {
        if (!enabled) return;
        logger.info("Disabling module: {}", getName());
        onDisable();

        // Unregister listeners
        for (ListenerAdapter listener : registeredListeners) {
            bot.getJda().removeEventListener(listener);
        }
        registeredListeners.clear();

        enabled = false;
    }

    // ==================== Accessors ====================

    public String getName() { return info.name(); }
    public String getDescription() { return info.description(); }
    public String getVersion() { return info.version(); }
    public String[] getDependencies() { return info.dependencies(); }
    public boolean isEnabled() { return enabled; }
    public ModuleInfo getInfo() { return info; }
    public List<BaseCommand> getRegisteredCommands() { return Collections.unmodifiableList(registeredCommands); }
    public List<ListenerAdapter> getRegisteredListeners() { return Collections.unmodifiableList(registeredListeners); }
}
