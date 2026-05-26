package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SetHomeCommand extends EssentialsBaseCommand {

    public SetHomeCommand(EssentialsManager manager) {
        super(manager, "sethome", "midgard.essentials.sethome", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String homeName = args.length > 0 ? args[0] : "home";

        if (!homeName.matches("[a-zA-Z0-9_]+")) {
            MessageUtils.send(player, manager.getMessage("home.invalid_name"));
            return;
        }

        if (manager.getHomeManager().getHome(player, homeName) == null) {
            int limit = manager.getHomeManager().getHomeLimit(player);
            if (manager.getHomeManager().getHomeCount(player) >= limit) {
                MessageUtils.send(player, manager.getMessage("home.limit_reached").replace("%limit%", String.valueOf(limit)));
                return;
            }
        }

        manager.getHomeManager().setHome(player, homeName, player.getLocation());
        MessageUtils.send(player, manager.getMessage("home.set").replace("%home%", homeName));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            List<String> homes = new ArrayList<>(manager.getHomeManager().getHomes(player));
            return match(args[0], homes);
        }
        return Collections.emptyList();
    }
}
