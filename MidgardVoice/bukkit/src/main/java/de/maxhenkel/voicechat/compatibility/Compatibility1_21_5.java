package de.maxhenkel.voicechat.compatibility;

import de.maxhenkel.voicechat.BukkitVersion;
import de.maxhenkel.voicechat.util.ConfigurableMessageJson;
import org.bukkit.entity.Player;

public class Compatibility1_21_5 extends Compatibility1_20_3 {

    public static final BukkitVersion VERSION_1_21_5 = BukkitVersion.parseBukkitVersion("1.21.5-R0.1");
    public static final BukkitVersion VERSION_1_21_6 = BukkitVersion.parseBukkitVersion("1.21.6-R0.1");
    public static final BukkitVersion VERSION_1_21_7 = BukkitVersion.parseBukkitVersion("1.21.7-R0.1");
    public static final BukkitVersion VERSION_1_21_8 = BukkitVersion.parseBukkitVersion("1.21.8-R0.1");
    public static final BukkitVersion VERSION_1_21_9 = BukkitVersion.parseBukkitVersion("1.21.9-R0.1");
    public static final BukkitVersion VERSION_1_21_10 = BukkitVersion.parseBukkitVersion("1.21.10-R0.1");
    public static final BukkitVersion VERSION_1_21_11 = BukkitVersion.parseBukkitVersion("1.21.11-R0.1");

    public static final Compatibility1_21_5 INSTANCE = new Compatibility1_21_5();

    @Override
    public void sendInviteMessage(Player player, Player commandSender, String groupName, String joinCommand) {
        sendJsonMessage(player, constructInviteMessage(commandSender, groupName, joinCommand));
    }

    public static String constructInviteMessage(Player commandSender, String groupName, String joinCommand) {
        return ConfigurableMessageJson.modernInviteMessage(commandSender.getName(), groupName, joinCommand);
    }

}
