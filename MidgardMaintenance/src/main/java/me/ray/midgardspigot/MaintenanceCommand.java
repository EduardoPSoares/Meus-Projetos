package me.ray.midgardspigot;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MaintenanceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("midgard.admin")) {
            sender.sendMessage("§cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɪssᴏ.");
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("ungod") || args[0].equalsIgnoreCase("removerimortalidade"))) {
            if (args.length < 2) {
                sender.sendMessage("§cᴜsᴏ: §e/mmaintenance ungod <player|all>");
                return true;
            }

            String targetName = args[1];
            if (targetName.equalsIgnoreCase("all")) {
                int count = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getGameMode() != org.bukkit.GameMode.CREATIVE && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                        // Ignora ghosts
                        if (MidgardSpigot.getInstance().isMidgardGhost(p)) continue;

                        if (p.isInvulnerable()) {
                            p.setInvulnerable(false);
                            count++;
                        }
                    }
                }
                sender.sendMessage("§aɪᴍᴏʀᴛᴀʟɪᴅᴀᴅᴇ ʀᴇᴍᴏᴠɪᴅᴀ ᴅᴇ §e" + count + " §aᴊᴏɢᴀᴅᴏʀᴇs.");
            } else {
                Player p = Bukkit.getPlayer(targetName);
                if (p == null) {
                    sender.sendMessage("§cᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.");
                    return true;
                }
                p.setInvulnerable(false);
                sender.sendMessage("§aɪᴍᴏʀᴛᴀʟɪᴅᴀᴅᴇ ʀᴇᴍᴏᴠɪᴅᴀ ᴅᴇ §e" + p.getName() + "§a.");
            }
            return true;
        }

        sender.sendMessage("§7ᴄᴏᴍᴀɴᴅᴏs ᴅɪsᴘᴏɴíᴠᴇɪs:");
        sender.sendMessage("§e/mmaintenance ungod <player|all> §7— ʀᴇᴍᴏᴠᴇ ɪᴍᴏʀᴛᴀʟɪᴅᴀᴅᴇ");
        return true;
    }
}
