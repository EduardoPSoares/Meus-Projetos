package me.ray.midgard.core.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BukkitCommandWrapper extends Command {

    private final MidgardCommand executor;

    public BukkitCommandWrapper(String name, MidgardCommand executor) {
        super(name);
        this.executor = executor;
        this.setAliases(executor.getAliases());
        // We could copy description/usage if MidgardCommand had them exposed
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        List<String> completions = executor.onTabComplete(sender, this, alias, args);
        return completions != null ? completions : super.tabComplete(sender, alias, args);
    }
}
