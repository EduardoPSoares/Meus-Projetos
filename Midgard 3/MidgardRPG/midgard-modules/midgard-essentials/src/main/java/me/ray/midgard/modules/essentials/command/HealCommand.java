package me.ray.midgard.modules.essentials.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.essentials.manager.EssentialsManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class HealCommand extends EssentialsBaseCommand {

    public HealCommand(EssentialsManager manager) {
        super(manager, "heal", "midgard.essentials.heal", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("midgard.essentials.heal.others")) {
                MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.commands.heal.no_permission_others"));
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

        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH) != null
            ? target.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setSaturation(20);
        target.setFireTicks(0);
        
        // Remove negative effects
        for (org.bukkit.potion.PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
        }

        if (target.equals(sender)) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.player.heal"));
        } else {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("essentials.player.heal_other", "%player%", target.getName()));
            MessageUtils.send(target, MidgardCore.getLanguageManager().getMessage("essentials.player.healed_by", "%player%", sender.getName()));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("midgard.essentials.heal.others")) {
            return match(args[0], onlinePlayers());
        }
        return Collections.emptyList();
    }
}
