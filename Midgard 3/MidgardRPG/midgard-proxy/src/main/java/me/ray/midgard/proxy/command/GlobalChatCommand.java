package me.ray.midgard.proxy.command;

import com.google.gson.JsonObject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import me.ray.midgard.proxy.config.ConfigManager;
import me.ray.midgard.proxy.redis.ProxyRedisManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class GlobalChatCommand implements SimpleCommand {

    private static final int MAX_MESSAGE_LENGTH = 256;
    private final ProxyRedisManager redisManager;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public GlobalChatCommand(ProxyRedisManager redisManager, ConfigManager configManager) {
        this.redisManager = redisManager;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(message("only-players", "<red>✖</red> <gray>Apenas jogadores podem usar este comando.</gray>"));
            return;
        }

        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            player.sendMessage(message("global-chat-usage", "<red>✖</red> <gray>Uso: <yellow>/g <mensagem></yellow></gray>"));
            return;
        }

        if (!redisManager.isEnabled()) {
            player.sendMessage(message("global-chat-unavailable", "<red>✖</red> <gray>Chat global indisponível no momento.</gray>"));
            return;
        }

        String message = normalizeMessage(String.join(" ", args));
        if (message.isBlank()) {
            player.sendMessage(message("global-chat-usage", "<red>✖</red> <gray>Uso: <yellow>/g <mensagem></yellow></gray>"));
            return;
        }
        
        // Publish to Redis
        JsonObject json = new JsonObject();
        json.addProperty("sender", player.getUsername());
        json.addProperty("msg", message);
        json.addProperty("server", player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("Unknown"));

        redisManager.publish("midgard:global_chat", json.toString());
    }

    private Component message(String key, String fallback) {
        return miniMessage.deserialize(configManager.getMessage(key, fallback));
    }

    private String normalizeMessage(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= 32 && c != 127) {
                sb.append(c);
            }
        }
        String normalized = sb.toString();
        return normalized.length() > MAX_MESSAGE_LENGTH ? normalized.substring(0, MAX_MESSAGE_LENGTH) : normalized;
    }
}
