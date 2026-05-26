package me.ray.midgardProxy.command;

import com.google.gson.JsonObject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import me.ray.midgardProxy.redis.RedisManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class GlobalChatCommand implements SimpleCommand {

    private final RedisManager redisManager;

    public GlobalChatCommand(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /g <message>"));
            return;
        }

        String message = String.join(" ", args);
        
        JsonObject json = new JsonObject();
        json.addProperty("sender", player.getUsername());
        json.addProperty("msg", message);
        
        redisManager.publish("midgard:global_chat", json.toString());
    }
}
