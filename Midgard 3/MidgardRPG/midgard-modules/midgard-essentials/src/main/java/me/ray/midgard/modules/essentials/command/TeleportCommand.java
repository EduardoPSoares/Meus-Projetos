package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.TeleportUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TeleportCommand extends MidgardCommand {

    private final EssentialsManager manager;

    public TeleportCommand(EssentialsManager manager) {
        super("tp", "midgard.essentials.tp", false);
        this.manager = manager;
    }

    @Override
    public List<String> getAliases() {
        return List.of("teleport", "tele");
    }

    @Override
    public String getDescription() {
        return manager.getMessage("command.desc.tp");
    }

    @Override
    public String getUsage() {
        return manager.getMessage("command.usage.tp");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtils.send(sender, manager.getMessage("commands.usage_tp"));
            return;
        }

        // /tp <player> (Só player pode usar)
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                MessageUtils.send(sender, manager.getMessage("commands.player_only"));
                return;
            }
            Player player = (Player) sender;
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                MessageUtils.send(player, manager.getMessage("errors.player_not_found")
                        .replace("%player%", args[0]));
                return;
            }

            TeleportUtils.teleport(player, target.getLocation());
            MessageUtils.send(player, manager.getMessage("teleport.success")
                    .replace("%target%", target.getName()));
            return;
        }

        // /tp <player> <target>
        if (args.length == 2) {
            Player playerToTeleport = Bukkit.getPlayer(args[0]);
            Player target = Bukkit.getPlayer(args[1]);

            if (playerToTeleport == null) {
                MessageUtils.send(sender, manager.getMessage("errors.player_not_found")
                        .replace("%player%", args[0]));
                return;
            }
            
            if (target == null) {
                MessageUtils.send(sender, manager.getMessage("errors.player_not_found")
                        .replace("%player%", args[1]));
                return;
            }

            TeleportUtils.teleport(playerToTeleport, target.getLocation());
            MessageUtils.send(sender, manager.getMessage("teleport.success")
                    .replace("%target%", target.getName()));
            MessageUtils.send(playerToTeleport, manager.getMessage("teleport.success")
                    .replace("%target%", target.getName()));
            return;
        }
        
        // /tp <x> <y> <z> (Só player)
        if (args.length == 3) {
             if (!(sender instanceof Player)) {
                MessageUtils.send(sender, manager.getMessage("commands.player_only"));
                return;
            }
            Player player = (Player) sender;
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                    MessageUtils.send(player, manager.getMessage("errors.invalid_number"));
                    return;
                }
                if (Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || y < -2048 || y > 2048) {
                    MessageUtils.send(player, manager.getMessage("errors.invalid_number"));
                    return;
                }
                Location loc = new Location(player.getWorld(), x, y, z);
                TeleportUtils.teleport(player, loc);
                MessageUtils.send(player, manager.getMessage("teleport.success_coords")
                        .replace("%x%", String.valueOf((int) x))
                        .replace("%y%", String.valueOf((int) y))
                        .replace("%z%", String.valueOf((int) z)));
            } catch (NumberFormatException e) {
                MessageUtils.send(player, manager.getMessage("errors.invalid_number"));
            }
            return;
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 || args.length == 2) {
            return match(args[args.length - 1], onlinePlayers());
        }
        return Collections.emptyList();
    }
}
