package midgardvanish.command;

import midgardvanish.MidgardVanish;
import midgardvanish.manager.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class VanishTpCommand implements CommandExecutor, TabCompleter {

    private final MidgardVanish plugin;
    private final VanishManager vanishManager;

    public VanishTpCommand(MidgardVanish plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        plugin.getCommand("vanishtp").setTabCompleter(this);
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

        if (args.length != 1) {
            player.sendMessage("§cᴜsᴏ: §e/vanishtp <jogador>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.");
            return true;
        }

        // Enable vanish if not already vanished
        if (!vanishManager.isVanished(player)) {
            vanishManager.enableVanish(player);
            player.sendMessage("§aᴠᴀɴɪsʜ ᴀᴛɪᴠᴀᴅᴏ ᴀᴜᴛᴏᴍᴀᴛɪᴄᴀᴍᴇɴᴛᴇ.");
        }

        // Teleport silently
        player.teleport(target.getLocation());
        player.sendMessage("§aᴛᴇʟᴇᴘᴏʀᴛᴀᴅᴏ sɪʟᴇɴᴄɪᴏsᴀᴍᴇɴᴛᴇ ᴘᴀʀᴀ §e" + target.getName() + "§a.");
        plugin.getLogger().info("[VANISH] " + player.getName() + " teleportou silenciosamente para " + target.getName());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("midgardvanish.use")) {
            String prefix = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(online.getName());
                }
            }
        }
        return completions;
    }
}
