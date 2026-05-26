package me.ray.midgard.core.command;

import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Gerenciador central de comandos do MidgardRPG.
 * Responsável por rotear subcomandos para seus respectivos executores.
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, MidgardCommand> commands = new HashMap<>();

    /**
     * Registra um subcomando.
     *
     * @param command O comando a ser registrado.
     */
    public void registerCommand(MidgardCommand command) {
        commands.put(command.getName().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias.toLowerCase(), command);
        }
    }

    /**
     * Remove o registro de um subcomando.
     *
     * @param name Nome do comando a ser removido.
     */
    public void unregisterCommand(String name) {
        commands.remove(name.toLowerCase());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        MidgardCommand subCommand = commands.get(subCommandName);

        if (subCommand == null) {
            MessageUtils.send(sender, "<red>Unknown command. Type /rpg help for help.");
            return true;
        }

        // Shift arguments
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        
        // Execute using the existing MidgardCommand logic with profiling
        return me.ray.midgard.core.debug.MidgardProfiler.monitor("command:" + subCommandName,
            () -> subCommand.onCommand(sender, command, label, newArgs)
        );
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (MidgardCommand cmd : commands.values()) {
                String perm = cmd.getPermission();
                if (perm == null || sender.hasPermission(perm)) {
                    completions.add(cmd.getName());
                }
            }
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        } else if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            MidgardCommand subCommand = commands.get(subCommandName);
            if (subCommand != null) {
                String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.onTabComplete(sender, command, label, newArgs);
            }
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender) {
        MessageUtils.send(sender, "<gradient:#5e4fa2:#f79459><bold>⚔ MidgardRPG Comandos</bold></gradient>");
        MessageUtils.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        java.util.Set<String> shown = new java.util.HashSet<>();
        for (java.util.Map.Entry<String, MidgardCommand> entry : commands.entrySet()) {
            MidgardCommand cmd = entry.getValue();
            if (shown.contains(cmd.getName())) {
                continue;
            }
            shown.add(cmd.getName());
            String perm = cmd.getPermission();
            if (perm == null || sender.hasPermission(perm)) {
                String usage = cmd.getUsage();
                String desc = cmd.getDescription();
                MessageUtils.send(sender, "<gray>• <yellow>" + usage + " <dark_gray>- <gray>" + desc);
            }
        }
        MessageUtils.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
