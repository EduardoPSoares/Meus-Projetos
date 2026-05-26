package com.midgard.core.module;

import com.midgard.core.MidgardCore;
import org.bukkit.event.Listener;

/**
 * Base class for all MidgardMechanics modules.
 * Each mechanic plugin should extend this to integrate with the core.
 */
public abstract class MidgardModule {

    private final String name;
    private final String version;
    private boolean enabled = false;

    protected MidgardModule(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public abstract void onEnable();

    public abstract void onDisable();

    public void enable() {
        if (!enabled) {
            onEnable();
            enabled = true;
            MidgardCore.getInstance().getLogger().info("Module " + name + " v" + version + " enabled.");
        }
    }

    public void disable() {
        if (enabled) {
            onDisable();
            enabled = false;
            MidgardCore.getInstance().getLogger().info("Module " + name + " v" + version + " disabled.");
        }
    }

    protected void registerListener(Listener listener) {
        MidgardCore core = MidgardCore.getInstance();
        core.getServer().getPluginManager().registerEvents(listener, core);
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
