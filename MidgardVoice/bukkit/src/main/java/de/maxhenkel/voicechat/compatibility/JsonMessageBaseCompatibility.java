package de.maxhenkel.voicechat.compatibility;

import de.maxhenkel.voicechat.util.ConfigurableMessageJson;
import org.bukkit.entity.Player;

public abstract class JsonMessageBaseCompatibility extends BaseCompatibility {

    @Override
    public void sendTranslationMessage(Player player, String key, String... args) {
        sendJsonMessage(player, ConfigurableMessageJson.translationMessage(key, args));
    }

    @Override
    public void sendStatusMessage(Player player, String key, String... args) {
        sendJsonStatusMessage(player, ConfigurableMessageJson.translationMessage(key, args));
    }

    public abstract void sendJsonMessage(Player player, String json);

    public abstract void sendJsonStatusMessage(Player player, String json);

    public abstract String createTranslationMessage(String key, String... args);

}
