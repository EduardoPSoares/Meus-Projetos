package me.ray.midgard.proxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import me.ray.midgard.proxy.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ReloadCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("midgard.admin")) {
             invocation.source().sendMessage(message("reload-no-permission", "<red>✖</red> <gray>Você não tem permissão para isso.</gray>"));
             return;
        }

        String[] args = invocation.arguments();
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            configManager.load();
            invocation.source().sendMessage(message("reload-success", "<green>Configuração do MidgardProxy recarregada!</green>"));
        } else {
            invocation.source().sendMessage(message("reload-usage", "<yellow>MidgardProxy v1.0.0. Use <white>/midgardproxy reload</white></yellow>"));
        }
    }

    private Component message(String key, String fallback) {
        return miniMessage.deserialize(configManager.getMessage(key, fallback));
    }
}
