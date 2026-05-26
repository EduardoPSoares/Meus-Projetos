package com.midgard.core.command;

import com.midgard.core.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for commands with subcommand support.
 */
public abstract class MidgardCommand {

    private final Map<String, MidgardCommand> subcommands = new LinkedHashMap<>();

    public abstract void execute(CommandSender sender, String[] args);

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && !subcommands.isEmpty()) {
            List<String> completions = new ArrayList<>();
            String lower = args[0].toLowerCase();
            for (String sub : subcommands.keySet()) {
                if (sub.toLowerCase().startsWith(lower)) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length > 1) {
            MidgardCommand sub = subcommands.get(args[0].toLowerCase());
            if (sub != null) {
                return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return new ArrayList<>();
    }

    public void addSubCommand(String name, MidgardCommand command) {
        subcommands.put(name.toLowerCase(), command);
    }

    protected boolean executeSubCommand(CommandSender sender, String[] args) {
        if (args.length == 0) return false;

        MidgardCommand sub = subcommands.get(args[0].toLowerCase());
        if (sub != null) {
            CommandInfo info = sub.getClass().getAnnotation(CommandInfo.class);
            if (info != null) {
                if (info.playerOnly() && !(sender instanceof Player)) {
                    sender.sendMessage(MessageUtils.toComponent("&cThis command is player-only."));
                    return true;
                }
                if (!info.permission().isEmpty() && !sender.hasPermission(info.permission())) {
                    sender.sendMessage(MessageUtils.toComponent("&cYou don't have permission."));
                    return true;
                }
            }
            sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        return false;
    }

    public Map<String, MidgardCommand> getSubcommands() {
        return subcommands;
    }

    public CommandInfo getInfo() {
        return getClass().getAnnotation(CommandInfo.class);
    }
}
