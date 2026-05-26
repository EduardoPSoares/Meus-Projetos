package de.maxhenkel.voicechat.util;

import de.maxhenkel.voicechat.Voicechat;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Iterator;

public final class ConfigurableMessageJson {

    private static final String INVITE_MARKER = "__MIDGARDVOICE_ACCEPT__";

    private ConfigurableMessageJson() {
    }

    public static String translationMessage(String key, String... args) {
        return legacyTextMessage(Voicechat.MESSAGES.translation(key, (Object[]) args));
    }

    public static String incompatibleMessage(String pluginVersion, String pluginName) {
        return legacyTextMessage(Voicechat.MESSAGES.translation(
                "message.voicechat.incompatible_version",
                pluginVersion,
                pluginName
        ));
    }

    public static String inviteMessage(String commandSenderName, String groupName, String joinCommand) {
        return legacyInviteMessage(commandSenderName, groupName, joinCommand, false);
    }

    public static String modernInviteMessage(String commandSenderName, String groupName, String joinCommand) {
        return legacyInviteMessage(commandSenderName, groupName, joinCommand, true);
    }

    public static String legacyTextMessage(String message) {
        return ComponentSerializer.toString(legacyRoot(message));
    }

    private static String legacyInviteMessage(String commandSenderName, String groupName, String joinCommand, boolean modernizeEvents) {
        String inviteText = Voicechat.MESSAGES.translation(
                "message.voicechat.invite",
                commandSenderName,
                groupName,
                INVITE_MARKER
        );
        if (!inviteText.contains(INVITE_MARKER)) {
            inviteText = inviteText + " " + INVITE_MARKER;
        }

        int markerIndex = inviteText.indexOf(INVITE_MARKER);
        String before = inviteText.substring(0, markerIndex);
        String after = inviteText.substring(markerIndex + INVITE_MARKER.length());

        TextComponent root = legacyRoot(before);
        HoverEvent hoverEvent = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(Voicechat.MESSAGES.translation("message.voicechat.accept_invite.hover"))
        );
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, joinCommand);

        for (BaseComponent component : TextComponent.fromLegacyText(Voicechat.MESSAGES.translation("message.voicechat.accept_invite"))) {
            component.setClickEvent(clickEvent);
            component.setHoverEvent(hoverEvent);
            root.addExtra(component);
        }

        appendLegacy(root, after);
        String json = ComponentSerializer.toString(root);
        return modernizeEvents ? modernizeEventKeys(json) : json;
    }

    private static TextComponent legacyRoot(String message) {
        TextComponent root = new TextComponent();
        appendLegacy(root, message);
        return root;
    }

    private static void appendLegacy(TextComponent root, String message) {
        for (BaseComponent component : TextComponent.fromLegacyText(message)) {
            root.addExtra(component);
        }
    }

    private static String modernizeEventKeys(String json) {
        Object parsed = new JSONTokener(json).nextValue();
        return modernizeNode(parsed, false).toString();
    }

    private static Object modernizeNode(Object node, boolean clickEventNode) {
        if (node instanceof JSONArray) {
            JSONArray array = (JSONArray) node;
            JSONArray converted = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                converted.put(modernizeNode(array.get(i), false));
            }
            return converted;
        }
        if (!(node instanceof JSONObject)) {
            return node;
        }
        JSONObject object = (JSONObject) node;

        JSONObject converted = new JSONObject();
        for (Iterator<String> it = object.keys(); it.hasNext(); ) {
            String key = it.next();
            String convertedKey = key;
            if ("clickEvent".equals(key)) {
                convertedKey = "click_event";
            } else if ("hoverEvent".equals(key)) {
                convertedKey = "hover_event";
            } else if ("value".equals(key) && clickEventNode) {
                convertedKey = "command";
            }
            boolean childIsClickEvent = "clickEvent".equals(key) || "click_event".equals(convertedKey);
            converted.put(convertedKey, modernizeNode(object.get(key), childIsClickEvent));
        }
        return converted;
    }
}
