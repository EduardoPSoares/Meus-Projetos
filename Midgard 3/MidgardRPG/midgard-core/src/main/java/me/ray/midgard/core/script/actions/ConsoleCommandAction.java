package me.ray.midgard.core.script.actions;

import me.ray.midgard.core.script.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleCommandAction implements Action {

    private static final int MAX_COMMAND_LENGTH = 256;
    private final String command;

    public ConsoleCommandAction(String command) {
        this.command = command;
    }

    @Override
    public void execute(Player player) {
        if (command == null || command.isBlank()) {
            return;
        }
        String cmd = command.replace("%player%", player.getName()).trim();
        cmd = cmd.replace("\n", "").replace("\r", "");
        if (cmd.length() > MAX_COMMAND_LENGTH) {
            cmd = cmd.substring(0, MAX_COMMAND_LENGTH);
        }
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (!cmd.isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
}
