package de.maxhenkel.voicechat.compatibility;

import com.mojang.brigadier.arguments.ArgumentType;
import de.maxhenkel.voicechat.Voicechat;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

import static de.maxhenkel.voicechat.compatibility.ReflectionUtils.doesMethodExist;

public class SpigotCompatibility extends FallbackCompatibility {

    public static final SpigotCompatibility INSTANCE = new SpigotCompatibility();

    public static boolean isSpigotCompatible() {
        return doesMethodExist(Bukkit.class, "spigot");
    }

    @Override
    public void sendStatusMessage(Player player, String key, String... args) {
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(Voicechat.MESSAGES.translation(key, (Object[]) args))
        );
    }

    @Override
    @Nullable
    public ArgumentType<?> playerArgument() {
        return null;
    }

    @Override
    @Nullable
    public ArgumentType<?> uuidArgument() {
        return null;
    }
}
