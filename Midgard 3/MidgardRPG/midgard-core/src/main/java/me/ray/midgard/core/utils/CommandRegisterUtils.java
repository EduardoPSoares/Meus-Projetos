package me.ray.midgard.core.utils;

import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;

public class CommandRegisterUtils {

    private static CommandMap commandMap;

    static {
        try {
            // Helper reflection to avoid direct dependency on SimplePluginManager
            Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
        } catch (Exception e) {
            MidgardLogger.error("Falha ao acessar CommandMap via reflexão", e);
        }
    }

    public static void register(Plugin plugin, Command command) {
        commandMap.register(plugin.getName(), command);
    }
    
    public static void unregister(String commandName) {
        // Unregistering is complex because you have to remove from the knownCommands map
        // Implement if needed for reloads
    }
}
