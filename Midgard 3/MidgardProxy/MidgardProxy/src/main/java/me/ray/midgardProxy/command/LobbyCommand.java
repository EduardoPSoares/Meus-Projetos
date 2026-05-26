package me.ray.midgardProxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.ray.midgardProxy.config.ConfigManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class LobbyCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;

    public LobbyCommand(ProxyServer server, ConfigManager configManager) {
        this.server = server;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command."));
            return;
        }

        List<String> lobbies = configManager.getLobbyServers();
        if (lobbies.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(configManager.getMessage("no_lobby_available")));
            return;
        }

        // Simple random load balancing
        String targetName = lobbies.get(ThreadLocalRandom.current().nextInt(lobbies.size()));
        Optional<RegisteredServer> target = server.getServer(targetName);

        if (target.isPresent()) {
            if (player.getCurrentServer().map(s -> s.getServer().equals(target.get())).orElse(false)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already connected to a lobby!"));
                return;
            }

            player.sendMessage(MiniMessage.miniMessage().deserialize(configManager.getMessage("connecting_lobby")));
            player.createConnectionRequest(target.get()).fireAndForget();
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize(configManager.getMessage("no_lobby_available")));
        }
    }
}
