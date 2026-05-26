package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DelWarpCommand extends EssentialsBaseCommand {

    public DelWarpCommand(EssentialsManager manager) {
        super(manager, "delwarp", "midgard.essentials.delwarp", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("essentials.commands.warp.delete.usage"));
            return;
        }

        String warpName = args[0];
        if (manager.getWarpManager().getWarp(warpName) == null) {
            MessageUtils.send(player, manager.getMessage("warp.not_found"));
            return;
        }

        manager.getWarpManager().deleteWarp(warpName);
        MessageUtils.send(player, manager.getMessage("warp.deleted").replace("%warp%", warpName));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], manager.getWarpManager().getWarps());
        }
        return super.tabComplete(sender, args);
    }
}
