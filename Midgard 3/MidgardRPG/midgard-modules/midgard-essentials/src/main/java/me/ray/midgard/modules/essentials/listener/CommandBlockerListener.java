package me.ray.midgard.modules.essentials.listener;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandBlockerListener implements Listener {

    private final EssentialsManager manager;

    public CommandBlockerListener(EssentialsManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission("midgard.essentials.bypass.blockedcmds")) {
            return;
        }

        List<String> blockedCommands = manager.getConfig().getConfig().getStringList("blocked-commands");
        String message = manager.getMessage("commands.command_blocked");

        // Normalize command handling
        // Split by whitespace to handle "/ command"
        String[] parts = event.getMessage().split("\\s+");
        if (parts.length == 0) {
            return;
        }
        
        String command = parts[0].toLowerCase();
        String normalizedCommand = normalize(command);
        String commandSuffix = getSuffixCommand(normalizedCommand);

        for (String blocked : blockedCommands) {
            String blockedCmd = normalize(blocked.toLowerCase());
            String blockedSuffix = getSuffixCommand(blockedCmd);

            // Block exact matches, suffix matches, and cross-matches
            // e.g. if "me" is blocked, block "minecraft:me" and "essentials:me"
            // e.g. if "minecraft:me" is blocked, block "me" (optional, usually stricter is better)
            
            if (normalizedCommand.equals(blockedCmd) || 
                commandSuffix.equals(blockedCmd) || 
                normalizedCommand.equals(blockedSuffix) ||
                commandSuffix.equals(blockedSuffix)) {
                
                event.setCancelled(true);
                MessageUtils.send(event.getPlayer(), message);
                return;
            }
        }
    }

    private String normalize(String command) {
        if (command == null || command.isEmpty()) {
            return "";
        }
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        return normalized.trim();
    }

    private String getSuffixCommand(String command) {
        if (command == null) {
            return "";
        }
        int idx = command.indexOf(':');
        return idx >= 0 ? command.substring(idx + 1) : command;
    }
}
