package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DelHomeCommand extends EssentialsBaseCommand {

    public DelHomeCommand(EssentialsManager manager) {
        super(manager, "delhome", "midgard.essentials.delhome", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String homeName = args.length > 0 ? args[0] : "home";

        if (manager.getHomeManager().getHome(player, homeName) == null) {
            MessageUtils.send(player, manager.getMessage("home.not_found"));
            return;
        }

        manager.getHomeManager().deleteHome(player, homeName);
        MessageUtils.send(player, manager.getMessage("home.deleted").replace("%home%", homeName));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender instanceof Player && args.length == 1) {
            return match(args[0], manager.getHomeManager().getHomes((Player) sender));
        }
        return super.tabComplete(sender, args);
    }
}
