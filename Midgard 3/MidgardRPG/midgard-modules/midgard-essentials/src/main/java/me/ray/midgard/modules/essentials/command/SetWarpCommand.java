package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetWarpCommand extends EssentialsBaseCommand {

    public SetWarpCommand(EssentialsManager manager) {
        super(manager, "setwarp", "midgard.essentials.setwarp", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            MessageUtils.send(player, MidgardCore.getLanguageManager().getMessage("essentials.commands.warp.set.usage"));
            return;
        }

        String warpName = args[0];
        manager.getWarpManager().setWarp(warpName, player.getLocation());
        MessageUtils.send(player, manager.getMessage("warp.set").replace("%warp%", warpName));
    }

    @Override
    public java.util.List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], manager.getWarpManager().getWarps());
        }
        return super.tabComplete(sender, args);
    }
}
