package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.TeleportUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HomeCommand extends EssentialsBaseCommand {

    public HomeCommand(EssentialsManager manager) {
        super(manager, "home", "midgard.essentials.home", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            String homes = String.join(", ", manager.getHomeManager().getHomes(player));
            MessageUtils.send(player, manager.getMessage("home.list_header"));
            MessageUtils.send(player, manager.getMessage("home.list_line").replace("%homes%", homes));
            return;
        }

        String homeName = args[0];
        Location home = manager.getHomeManager().getHome(player, homeName);

        if (home == null) {
            MessageUtils.send(player, manager.getMessage("home.not_found"));
            return;
        }

        TeleportUtils.teleport(player, home);
        MessageUtils.send(player, manager.getMessage("home.teleport").replace("%home%", homeName));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender instanceof Player && args.length == 1) {
            return match(args[0], manager.getHomeManager().getHomes((Player) sender));
        }
        return super.tabComplete(sender, args);
    }
}
