package me.ray.midgard.core.command;

import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.utils.ConsoleUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class CommandRegistrar {

    private static CommandMap commandMap;

    static {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            commandMap = (CommandMap) f.get(Bukkit.getServer());
        } catch (Exception e) {
            MidgardLogger.error("Falha ao obter CommandMap via reflexão", e);
        }
    }

    public static void register(JavaPlugin plugin, MidgardCommand command) {
        if (commandMap == null) {
            ConsoleUtils.error("CommandMap is not available. Cannot register command: " + command.getName());
            return;
        }

        // Create a Bukkit Command wrapper
        Command bukkitCommand = new Command(command.getName()) {
            @Override
            public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                return command.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                return command.onTabComplete(sender, this, alias, args);
            }
        };

        // Configurar aliases e permissão
        if (command.getAliases() != null && !command.getAliases().isEmpty()) {
            bukkitCommand.setAliases(command.getAliases());
        }
        if (command.getPermission() != null) {
            bukkitCommand.setPermission(command.getPermission());
        }

        // Register with the plugin's name as fallback prefix
        commandMap.register(plugin.getName(), bukkitCommand);
        ConsoleUtils.info("Registered dynamic command: /" + command.getName());
    }
}
