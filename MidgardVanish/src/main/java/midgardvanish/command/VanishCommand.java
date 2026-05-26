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

public class VanishCommand implements CommandExecutor, TabCompleter {

    private final MidgardVanish plugin;
    private final VanishManager vanishManager;

    public VanishCommand(MidgardVanish plugin, VanishManager vanishManager, Object settingsGUI) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        plugin.getCommand("vanish").setTabCompleter(this);
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

        if (args.length == 0) {
            // Toggle vanish for self
            vanishManager.toggleVanish(player);

            if (vanishManager.isVanished(player)) {
                player.sendMessage("§aᴠᴏᴄê ᴀɢᴏʀᴀ ᴇsᴛá §eɪɴᴠɪsíᴠᴇʟ§a.");
            } else {
                player.sendMessage("§cᴠᴏᴄê ᴀɢᴏʀᴀ ᴇsᴛá §eᴠɪsíᴠᴇʟ§c.");
            }
            return true;
        }

        if (args.length == 1) {
            if (!player.hasPermission("midgardvanish.others")) {
                player.sendMessage("§cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɪssᴏ.");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.");
                return true;
            }

            vanishManager.toggleVanish(target);

            if (vanishManager.isVanished(target)) {
                player.sendMessage("§e" + target.getName() + " §aᴀɢᴏʀᴀ ᴇsᴛá ɪɴᴠɪsíᴠᴇʟ.");
                target.sendMessage("§aᴠᴏᴄê ꜰᴏɪ ᴄᴏʟᴏᴄᴀᴅᴏ ᴇᴍ ᴠᴀɴɪsʜ ᴘᴏʀ §e" + player.getName() + "§a.");
            } else {
                player.sendMessage("§e" + target.getName() + " §cᴀɢᴏʀᴀ ᴇsᴛá ᴠɪsíᴠᴇʟ.");
                target.sendMessage("§cᴠᴏᴄê ꜰᴏɪ ʀᴇᴛɪʀᴀᴅᴏ ᴅᴏ ᴠᴀɴɪsʜ ᴘᴏʀ §e" + player.getName() + "§c.");
            }
            return true;
        }

        player.sendMessage("§cᴜsᴏ: §e/vanish [jogador]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            if (sender.hasPermission("midgardvanish.others")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(online.getName());
                    }
                }
            }
        }
        return completions;
    }
}
