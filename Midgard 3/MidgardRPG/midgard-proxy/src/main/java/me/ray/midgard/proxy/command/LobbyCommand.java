package me.ray.midgard.proxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.ray.midgard.proxy.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class LobbyCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public LobbyCommand(ProxyServer server, ConfigManager configManager) {
        this.server = server;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(message("only-players", "<red>✖</red> <gray>Apenas jogadores podem usar este comando.</gray>"));
            return;
        }

        Player player = (Player) invocation.source();
        List<String> lobbyServers = configManager.getLobbyServers();

        if (lobbyServers.isEmpty()) {
            player.sendMessage(message("lobby-no-servers", "<red>✖</red> <gray>Nenhum servidor de lobby configurado.</gray>"));
            return;
        }

        // Try to find a lobby server
        // Simple random load balancing
        String targetName = lobbyServers.get(ThreadLocalRandom.current().nextInt(lobbyServers.size()));
        Optional<RegisteredServer> target = server.getServer(targetName);
        
        // If the preferred one is not present, iterate to find any valid one
        if (target.isEmpty()) {
             for (String name : lobbyServers) {
                 Optional<RegisteredServer> s = server.getServer(name);
                 if (s.isPresent()) {
                     target = s;
                     break;
                 }
             }
        }

        if (target.isPresent()) {
            RegisteredServer lobby = target.get();
            if (player.getCurrentServer().map(s -> s.getServer().equals(lobby)).orElse(false)) {
                player.sendMessage(message("lobby-already", "<yellow>⚠</yellow> <gray>Você já está no lobby.</gray>"));
                return;
            }
            
            player.sendMessage(message("lobby-connecting", "<gray>Conectando ao lobby...</gray>"));
            player.createConnectionRequest(lobby).connect().thenAccept(result -> {
                if (!result.isSuccessful()) {
                   String reason = result.getReasonComponent()
                           .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                           .orElse("Motivo desconhecido");
                   player.sendMessage(message("lobby-connect-failed", "<red>✖</red> <gray>Falha ao conectar: <white>%reason%</white></gray>")
                           .replaceText(builder -> builder.match("%reason%").replacement(reason)));
                }
            });
        } else {
             player.sendMessage(message("lobby-not-found", "<red>✖</red> <gray>Servidor de lobby não encontrado na rede.</gray>"));
        }
    }

    private Component message(String key, String fallback) {
        return miniMessage.deserialize(configManager.getMessage(key, fallback));
    }
}
