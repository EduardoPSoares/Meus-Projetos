package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlyCommand extends EssentialsBaseCommand {

    public FlyCommand(EssentialsManager manager) {
        super(manager, "fly", "midgard.essentials.fly", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("midgard.essentials.fly.others")) {
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.commands.fly.no_permission_others"));
                return;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.commands.common.player_not_found"));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.commands.common.console_must_specify_player"));
                return;
            }
            target = (Player) sender;
        }

        boolean flying = !target.getAllowFlight();
        target.setAllowFlight(flying);
        target.setFlying(flying);

        String status = flying ? "enabled" : "disabled";
        
        if (target.equals(sender)) {
            MessageUtils.send(sender, manager.getMessage("player.fly_" + status));
        } else {
            MessageUtils.send(sender, manager.getMessage("player.fly_" + status + "_other")
                    .replace("%player%", target.getName()));
            MessageUtils.send(target, manager.getMessage("player.fly_" + status));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("midgard.essentials.fly.others")) {
            return match(args[0], onlinePlayers());
        }
        return Collections.emptyList();
    }
}
