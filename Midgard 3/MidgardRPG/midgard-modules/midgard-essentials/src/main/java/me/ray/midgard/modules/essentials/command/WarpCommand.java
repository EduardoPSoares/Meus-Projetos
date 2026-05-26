package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.TeleportUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class WarpCommand extends EssentialsBaseCommand {

    public WarpCommand(EssentialsManager manager) {
        super(manager, "warp", "midgard.essentials.warp", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            String warps = String.join(", ", manager.getWarpManager().getWarps());
            MessageUtils.send(player, manager.getMessage("warp.list_header"));
            MessageUtils.send(player, manager.getMessage("warp.list_line").replace("%warps%", warps));
            return;
        }

        String warpName = args[0];
        Location warp = manager.getWarpManager().getWarp(warpName);

        if (warp == null) {
            MessageUtils.send(player, manager.getMessage("warp.not_found"));
            return;
        }
        
        // Permission Check per Warp
        if (!player.hasPermission("midgard.essentials.warp." + warpName.toLowerCase()) && !player.hasPermission("midgard.essentials.warp.*")) {
             MessageUtils.send(player, manager.getMessage("warp.no_permission"));
             return;
        }
        
        // Safety Check
        if (!TeleportUtils.isSafeLocation(warp)) {
             MessageUtils.send(player, manager.getMessage("warp.unsafe_location"));
             return;
        }

        TeleportUtils.teleport(player, warp);
        MessageUtils.send(player, manager.getMessage("warp.teleport").replace("%warp%", warpName));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], manager.getWarpManager().getWarps());
        }
        return super.tabComplete(sender, args);
    }
}
