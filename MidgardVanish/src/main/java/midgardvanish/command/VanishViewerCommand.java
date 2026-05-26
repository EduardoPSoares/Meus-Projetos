package midgardvanish.command;

import midgardvanish.gui.ViewerMenuGUI;
import midgardvanish.manager.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VanishViewerCommand implements CommandExecutor, TabCompleter {

    private final ViewerMenuGUI viewerMenuGUI;
    private final VanishManager vanishManager;

    public VanishViewerCommand(ViewerMenuGUI viewerMenuGUI, VanishManager vanishManager) {
        this.viewerMenuGUI = viewerMenuGUI;
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cᴀᴘᴇɴᴀs ᴊᴏɢᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ᴜsᴀʀ ᴇsᴛᴇ ᴄᴏᴍᴀɴᴅᴏ.");
            return true;
        }

        if (!player.hasPermission("midgardvanish.viewer")) {
            player.sendMessage("§cᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɪssᴏ.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cᴜsᴏ: §e/vanishviewer <jogador>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.");
            return true;
        }

        if (!vanishManager.isVanished(target)) {
            player.sendMessage("§cᴇssᴇ ᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇsᴛá ᴇᴍ ᴠᴀɴɪsʜ.");
            return true;
        }

        viewerMenuGUI.openMainMenu(player, target.getUniqueId(), 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("midgardvanish.viewer")) {
            String prefix = args[0].toLowerCase();
            for (UUID uuid : vanishManager.getVanishedPlayers()) {
                Player vanished = Bukkit.getPlayer(uuid);
                if (vanished != null && vanished.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(vanished.getName());
                }
            }
        }
        return completions;
    }
}
