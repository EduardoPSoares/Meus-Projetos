package me.ray.midgard.loader.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.loader.gui.PlayerInfoGui;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InfoCommand extends MidgardCommand {

    public InfoCommand() {
        super("info", "midgard.admin.info", false);
    }

    @Override
    public String getDescription() {
        return "Exibe informações detalhadas de um jogador";
    }

    @Override
    public String getUsage() {
        return "/rpg admin info <jogador>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("loader.info.only_players"));
            return;
        }

        if (args.length < 1) {
            MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("loader.info.usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtils.send(player, me.ray.midgard.core.MidgardCore.getLanguageManager().getMessage("loader.info.player_not_found"));
            return;
        }

        new PlayerInfoGui(player, target).open();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
