package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.TeleportUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TeleportHereCommand extends MidgardCommand {

    private final EssentialsManager manager;

    public TeleportHereCommand(EssentialsManager manager) {
        super("tphere", "midgard.essentials.tphere", true);
        this.manager = manager;
    }

    @Override
    public List<String> getAliases() {
        return List.of("tph");
    }

    @Override
    public String getDescription() {
        return manager.getMessage("command.desc.tphere");
    }

    @Override
    public String getUsage() {
        return manager.getMessage("command.usage.tphere");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            MessageUtils.send(sender, manager.getMessage("commands.tphere.usage"));
            return;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            String msg = manager.getMessage("errors.player_not_found")
                .replace("%player%", args[0]);
            MessageUtils.send(player, msg);
            return;
        }

        TeleportUtils.teleport(target, player.getLocation());
        
        String pullerMsg = manager.getMessage("commands.tphere.success")
            .replace("%player%", target.getName());
        MessageUtils.send(player, pullerMsg);
        
        String targetMsg = manager.getMessage("commands.tphere.target_notified")
            .replace("%player%", player.getName());
        MessageUtils.send(target, targetMsg);
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], onlinePlayers());
        }
        return Collections.emptyList();
    }
}
