package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class GamemodeCommand extends EssentialsBaseCommand {

    public GamemodeCommand(EssentialsManager manager) {
        super(manager, "gamemode", "midgard.essentials.gamemode", false);
    }

    @Override
    public List<String> getAliases() {
        return List.of("gm", "gmode");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.commands.gamemode.usage"));
            return;
        }

        GameMode mode = getGameMode(args[0]);
        if (mode == null) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.commands.gamemode.invalid_mode"));
            return;
        }

        Player target;
        if (args.length > 1) {
            if (!sender.hasPermission("midgard.essentials.gamemode.others")) {
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.commands.gamemode.no_permission_others"));
                return;
            }
            target = Bukkit.getPlayer(args[1]);
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

        target.setGameMode(mode);
        
        if (target.equals(sender)) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.player.gamemode_changed", "%gamemode%", mode.name().toLowerCase()));
        } else {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.player.gamemode_changed_other", "%player%", target.getName(), "%gamemode%", mode.name().toLowerCase()));
            MessageUtils.send(target, MidgardCore.getLanguageManager().getMessage("essentials.player.gamemode_changed", "%gamemode%", mode.name().toLowerCase()));
        }
    }

    private GameMode getGameMode(String arg) {
        switch (arg.toLowerCase()) {
            case "0":
            case "s":
            case "survival":
                return GameMode.SURVIVAL;
            case "1":
            case "c":
            case "creative":
                return GameMode.CREATIVE;
            case "2":
            case "a":
            case "adventure":
                return GameMode.ADVENTURE;
            case "3":
            case "sp":
            case "spectator":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], Arrays.asList("survival", "creative", "adventure", "spectator"));
        }
        if (args.length == 2 && sender.hasPermission("midgard.essentials.gamemode.others")) {
            return match(args[1], Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).collect(java.util.stream.Collectors.toList()));
        }
        return super.tabComplete(sender, args);
    }
}
