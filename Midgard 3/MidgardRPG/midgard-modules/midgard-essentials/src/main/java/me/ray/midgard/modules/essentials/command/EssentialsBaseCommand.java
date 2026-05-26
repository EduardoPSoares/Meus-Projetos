package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;

import java.util.Map;

public abstract class EssentialsBaseCommand extends MidgardCommand {

    protected final EssentialsManager manager;
    
    private static final Map<String, String> DESCRIPTION_KEYS = Map.ofEntries(
        Map.entry("gamemode", "command.desc.gamemode"),
        Map.entry("fly", "command.desc.fly"),
        Map.entry("heal", "command.desc.heal"),
        Map.entry("feed", "command.desc.feed"),
        Map.entry("spawn", "command.desc.spawn"),
        Map.entry("setspawn", "command.desc.setspawn"),
        Map.entry("warp", "command.desc.warp"),
        Map.entry("setwarp", "command.desc.setwarp"),
        Map.entry("delwarp", "command.desc.delwarp"),
        Map.entry("home", "command.desc.home"),
        Map.entry("sethome", "command.desc.sethome"),
        Map.entry("delhome", "command.desc.delhome"),
        Map.entry("tpa", "command.desc.tpa"),
        Map.entry("tpaccept", "command.desc.tpaccept"),
        Map.entry("tpdeny", "command.desc.tpdeny")
    );
    
    private static final Map<String, String> USAGE_KEYS = Map.ofEntries(
        Map.entry("gamemode", "command.usage.gamemode"),
        Map.entry("fly", "command.usage.fly"),
        Map.entry("heal", "command.usage.heal"),
        Map.entry("feed", "command.usage.feed"),
        Map.entry("spawn", "command.usage.spawn"),
        Map.entry("setspawn", "command.usage.setspawn"),
        Map.entry("warp", "command.usage.warp"),
        Map.entry("setwarp", "command.usage.setwarp"),
        Map.entry("delwarp", "command.usage.delwarp"),
        Map.entry("home", "command.usage.home"),
        Map.entry("sethome", "command.usage.sethome"),
        Map.entry("delhome", "command.usage.delhome"),
        Map.entry("tpa", "command.usage.tpa"),
        Map.entry("tpaccept", "command.usage.tpaccept"),
        Map.entry("tpdeny", "command.usage.tpdeny")
    );

    public EssentialsBaseCommand(EssentialsManager manager, String name, String permission, boolean playerOnly) {
        super(name, permission, playerOnly);
        this.manager = manager;
    }
    
    @Override
    public String getDescription() {
        String key = DESCRIPTION_KEYS.get(getName().toLowerCase());
        if (key != null) {
            return manager.getMessage(key);
        }
        return manager.getMessage("command.desc.default");
    }
    
    @Override
    public String getUsage() {
        String key = USAGE_KEYS.get(getName().toLowerCase());
        if (key != null) {
            return manager.getMessage(key);
        }
        return "/" + getName();
    }
}
