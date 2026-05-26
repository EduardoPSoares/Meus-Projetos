package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

import me.ray.midgard.modules.essentials.gui.InvseeGui;

public class InvseeCommand extends MidgardCommand {

    private final EssentialsManager manager;

    public InvseeCommand(EssentialsManager manager) {
        super("invsee", "midgard.essentials.invsee", true);
        this.manager = manager;
    }

    @Override
    public List<String> getAliases() {
        return List.of("inv", "openinv");
    }

    @Override
    public String getDescription() {
        return manager.getMessage("command.desc.invsee");
    }

    @Override
    public String getUsage() {
        return manager.getMessage("command.usage.invsee");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length != 1) {
            MessageUtils.send(player, manager.getMessage("invsee.usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtils.send(player, manager.getMessage("errors.player_not_found")
                    .replace("%player%", args[0]));
            return;
        }
        
        // Check Exempt permission
        if (target.hasPermission("midgard.essentials.invsee.exempt") && !player.hasPermission("midgard.essentials.invsee.bypass")) {
             MessageUtils.send(player, manager.getMessage("invsee.cannot_view_target"));
             return;
        }

        new InvseeGui(player, target, manager).open();
        MessageUtils.send(player, manager.getMessage("invsee.opening")
                .replace("%player%", target.getName()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], onlinePlayers());
        }
        return Collections.emptyList();
    }
}
