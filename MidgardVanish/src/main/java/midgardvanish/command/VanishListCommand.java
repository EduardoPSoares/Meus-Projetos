package midgardvanish.command;

import midgardvanish.manager.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class VanishListCommand implements CommandExecutor {

    private final VanishManager vanishManager;

    public VanishListCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("midgardvanish.use")) {
            sender.sendMessage("§cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɪssᴏ.");
            return true;
        }

        var vanished = vanishManager.getVanishedPlayers();
        if (vanished.isEmpty()) {
            sender.sendMessage("§7ɴᴇɴʜᴜᴍ ᴊᴏɢᴀᴅᴏʀ ᴇsᴛá ᴇᴍ ᴠᴀɴɪsʜ.");
            return true;
        }

        int count = 0;
        StringBuilder list = new StringBuilder();

        for (UUID uuid : vanished) {
            Player player = Bukkit.getPlayer(uuid);
            count++;
            if (player != null) {
                list.append("  §7- §a").append(player.getName()).append(" §7(ᴏɴʟɪɴᴇ)\n");
            } else {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                list.append("  §7- §c").append(name != null ? name : uuid).append(" §7(ᴏꜰꜰʟɪɴᴇ)\n");
            }
        }

        sender.sendMessage("§eᴊᴏɢᴀᴅᴏʀᴇs ᴇᴍ ᴠᴀɴɪsʜ §7(" + count + ")§e:");
        sender.sendMessage(list.toString().stripTrailing());

        return true;
    }
}
