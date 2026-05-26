package me.ray.midgardProxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import me.ray.midgardProxy.config.ConfigManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ReloadCommand implements SimpleCommand {

    private final ConfigManager configManager;

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            configManager.load();
            invocation.source().sendMessage(MiniMessage.miniMessage().deserialize("<green>MidgardProxy configuration reloaded."));
        } else {
             invocation.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /midgardproxy reload"));
        }
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("midgardproxy.admin");
    }
}
