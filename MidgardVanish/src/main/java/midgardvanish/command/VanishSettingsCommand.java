package midgardvanish.command;

import midgardvanish.gui.VanishSettingsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishSettingsCommand implements CommandExecutor {

    private final VanishSettingsGUI settingsGUI;

    public VanishSettingsCommand(VanishSettingsGUI settingsGUI) {
        this.settingsGUI = settingsGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cᴀᴘᴇɴᴀs ᴊᴏɢᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ᴜsᴀʀ ᴇsᴛᴇ ᴄᴏᴍᴀɴᴅᴏ.");
            return true;
        }

        if (!player.hasPermission("midgardvanish.use")) {
            player.sendMessage("§cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɪssᴏ.");
            return true;
        }

        settingsGUI.open(player);
        return true;
    }
}
