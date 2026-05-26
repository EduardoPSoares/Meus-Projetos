package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FeedCommand extends EssentialsBaseCommand {

    public FeedCommand(EssentialsManager manager) {
        super(manager, "feed", "midgard.essentials.feed", false);
    }

    @Override
    public List<String> getAliases() {
        return List.of("eat");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("midgard.essentials.feed.others")) {
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.commands.feed.no_permission_others"));
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

        target.setFoodLevel(20);
        target.setSaturation(20);

        if (target.equals(sender)) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.player.feed"));
        } else {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.player.feed_other", "%player%", target.getName()));
            MessageUtils.send(target, MidgardCore.getLanguageManager().getMessage("essentials.player.fed_by", "%player%", sender.getName()));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("midgard.essentials.feed.others")) {
            return match(args[0], onlinePlayers());
        }
        return Collections.emptyList();
    }
}
