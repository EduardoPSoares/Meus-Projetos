package com.midgard.core.command;

import com.midgard.core.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for dynamic command registration without plugin.yml entries.
 */
public class CommandRegistry {

    private final JavaPlugin plugin;
    private final Map<String, MidgardCommand> commands = new ConcurrentHashMap<>();
    private CommandMap commandMap;

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commandMap = resolveCommandMap();
        if (this.commandMap == null) {
            plugin.getLogger().severe("Failed to access CommandMap — commands will not work!");
        }
    }

    private CommandMap resolveCommandMap() {
        // Paper exposes getCommandMap() directly on Server
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            return (CommandMap) method.invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException ignored) {
            // Paper API not available — fall through to reflective field access
        }
        // Fallback: reflection on CraftServer field
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().severe("CommandMap reflection failed: " + e.getMessage());
            return null;
        }
    }

    public void registerCommand(MidgardCommand midgardCommand) {
        if (commandMap == null) {
            plugin.getLogger().severe("Cannot register command: CommandMap is unavailable.");
            return;
        }

        CommandInfo info = midgardCommand.getClass().getAnnotation(CommandInfo.class);
        if (info == null) {
            plugin.getLogger().warning("Command class " + midgardCommand.getClass().getSimpleName()
                    + " is missing @CommandInfo annotation!");
            return;
        }

        commands.put(info.name().toLowerCase(), midgardCommand);

        Command cmd = new Command(info.name()) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                if (info.playerOnly() && !(sender instanceof Player)) {
                    sender.sendMessage(MessageUtils.toComponent("&cThis command is player-only."));
                    return true;
                }
                if (!info.permission().isEmpty() && !sender.hasPermission(info.permission())) {
                    sender.sendMessage(MessageUtils.toComponent("&cYou don't have permission."));
                    return true;
                }
                midgardCommand.execute(sender, args);
                return true;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                return midgardCommand.tabComplete(sender, args);
            }
        };

        cmd.setDescription(info.description());
        cmd.setUsage(info.usage());
        cmd.setAliases(Arrays.asList(info.aliases()));
        if (!info.permission().isEmpty()) {
            cmd.setPermission(info.permission());
        }

        commandMap.register(plugin.getName().toLowerCase(), cmd);
    }

    public MidgardCommand getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public Map<String, MidgardCommand> getCommands() {
        return commands;
    }
}
