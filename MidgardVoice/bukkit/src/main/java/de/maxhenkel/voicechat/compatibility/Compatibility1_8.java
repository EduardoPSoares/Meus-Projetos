package de.maxhenkel.voicechat.compatibility;

import com.mojang.brigadier.arguments.ArgumentType;
import de.maxhenkel.voicechat.BukkitVersion;
import de.maxhenkel.voicechat.util.ConfigurableMessageJson;
import de.maxhenkel.voicechat.util.Key;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import static de.maxhenkel.voicechat.compatibility.ReflectionUtils.*;

public class Compatibility1_8 extends JsonMessageBaseCompatibility {

    public static final BukkitVersion VERSION_1_8_8 = BukkitVersion.parseBukkitVersion("1.8.8-R0.1");

    public static final Compatibility1_8 INSTANCE = new Compatibility1_8();

    @Override
    public Key createNamespacedKey(String key) {
        return Key.of(Compatibility1_12.CHANNEL, key);
    }

    @Override
    public void sendJsonMessage(Player player, String json) {
        send(player, json, (byte) 0);
    }

    @Override
    public void sendJsonStatusMessage(Player player, String json) {
        send(player, json, (byte) 2);
    }

    @Override
    public String createTranslationMessage(String key, String... args) {
        return constructTranslationMessage(key, args);
    }

    public static String constructTranslationMessage(String key, String... args) {
        JSONObject msg = new JSONObject();
        msg.put("translate", key);
        msg.put("with", args);
        return msg.toString();
    }

    @Override
    public void sendInviteMessage(Player player, Player commandSender, String groupName, String joinCommand) {
        sendJsonMessage(player, constructInviteMessage(commandSender, groupName, joinCommand));
    }

    @Override
    public void sendIncompatibleMessage(Player player, String pluginVersion, String pluginName) {
        sendJsonMessage(player, constructIncompatibleMessage(pluginVersion, pluginName));
    }

    public static String constructInviteMessage(Player commandSender, String groupName, String joinCommand) {
        return ConfigurableMessageJson.inviteMessage(commandSender.getName(), groupName, joinCommand);
    }

    public static String constructIncompatibleMessage(String pluginVersion, String pluginName) {
        return ConfigurableMessageJson.incompatibleMessage(pluginVersion, pluginName);
    }

    @Override
    public ArgumentType<?> playerArgument() {
        return null;
    }

    @Override
    public ArgumentType<?> uuidArgument() {
        return null;
    }

    private void send(Player player, String json, byte chatMessageType) {
        Object entityPlayer = callMethod(player, "getHandle");
        Object playerConnection = getField(entityPlayer, "playerConnection");
        Class<?> packet = getServerClass("Packet");
        Class<?> chatSerializer = getServerClass("IChatBaseComponent$ChatSerializer");

        Class<?> iChatBaseComponentClass = getServerClass("IChatBaseComponent");
        Object iChatBaseComponent = callMethod(chatSerializer, "a", new Class[]{String.class}, json);

        Class<?> packetPlayOutChatClass = getServerClass("PacketPlayOutChat");

        Object clientboundSystemChatPacket = callConstructor(packetPlayOutChatClass, new Class[]{iChatBaseComponentClass, byte.class}, iChatBaseComponent, chatMessageType);

        callMethod(playerConnection, "sendPacket", new Class[]{packet}, clientboundSystemChatPacket);
    }

}
