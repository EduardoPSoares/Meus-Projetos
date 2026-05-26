package me.ray.midgard.modules.economy.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.economy.EconomyModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Subcomando administrativo para economia.
 * Substitui o comando /mideco antigo.
 * Uso: /rpg admin econ <subcomando>
 */
public class EconomyAdminCommand extends MidgardCommand {

    private final EconomyModule module;

    public EconomyAdminCommand(EconomyModule module) {
        super("econ", "midgard.admin.economy", false);
        this.module = module;
    }

    @Override
    public List<String> getAliases() {
        return List.of("economy", "money", "eco");
    }

    @Override
    public String getDescription() {
        return module.getMessage("command.economy_admin_description");
    }

    @Override
    public String getUsage() {
        return module.getMessage("command.economy_admin_usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "give":
                if (!sender.hasPermission("midgard.admin.economy.give")) {
                    MessageUtils.send(sender, module.getMessage("error.no_permission"));
                    return;
                }
                handleGive(sender, args);
                break;
            case "take":
                if (!sender.hasPermission("midgard.admin.economy.take")) {
                    MessageUtils.send(sender, module.getMessage("error.no_permission"));
                    return;
                }
                handleTake(sender, args);
                break;
            case "set":
                if (!sender.hasPermission("midgard.admin.economy.set")) {
                    MessageUtils.send(sender, module.getMessage("error.no_permission"));
                    return;
                }
                handleSet(sender, args);
                break;
            case "balance":
            case "bal":
                if (!sender.hasPermission("midgard.admin.economy.balance")) {
                    MessageUtils.send(sender, module.getMessage("error.no_permission"));
                    return;
                }
                handleBalance(sender, args);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }
    }

    private int parseAmount(String input) throws NumberFormatException {
        if (input == null || input.isEmpty()) {
            throw new NumberFormatException("Empty input");
        }
        
        String lower = input.toLowerCase();
        long multiplier = 1;
        
        if (lower.endsWith("k")) {
            multiplier = 1000;
            lower = lower.substring(0, lower.length() - 1);
        } else if (lower.endsWith("m")) {
            multiplier = 1000000;
            lower = lower.substring(0, lower.length() - 1);
        } else if (lower.endsWith("b")) {
             multiplier = 1000000000;
             lower = lower.substring(0, lower.length() - 1);
        }
        
        double val = Double.parseDouble(lower);
        if (!Double.isFinite(val)) {
            throw new NumberFormatException("NaN or Infinity is not a valid amount");
        }
        long result = (long) (val * multiplier);
        if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            throw new NumberFormatException("Amount exceeds integer range: " + result);
        }
        return (int) result;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, module.getMessage("admin.give.usage"));
            return;
        }

        Player target = sender.getServer().getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, module.getMessage("error.player_not_found"));
            return;
        }

        try {
            int amount = parseAmount(args[2]);
            if (amount <= 0) {
                MessageUtils.send(sender, module.getMessage("error.positive_amount"));
                return;
            }

            module.getCurrencyManager().givePhysicalCurrency(target, amount);
            MessageUtils.send(sender, module.getMessage("admin.give.success_sender")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%target%", target.getName()));
            MessageUtils.send(target, module.getMessage("admin.give.success_target")
                    .replace("%amount%", String.valueOf(amount)));
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, module.getMessage("error.invalid_number"));
        }
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, module.getMessage("admin.take.usage"));
            return;
        }

        Player target = sender.getServer().getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, module.getMessage("error.player_not_found"));
            return;
        }

        try {
            int amount = parseAmount(args[2]);
            if (amount <= 0) {
                MessageUtils.send(sender, module.getMessage("error.positive_amount"));
                return;
            }

            boolean success = module.getCurrencyManager().takeCurrency(target, amount);
            if (success) {
                MessageUtils.send(sender, module.getMessage("admin.take.success_sender")
                        .replace("%amount%", String.valueOf(amount))
                        .replace("%target%", target.getName()));
                MessageUtils.send(target, module.getMessage("admin.take.success_target")
                        .replace("%amount%", String.valueOf(amount)));
            } else {
                MessageUtils.send(sender, module.getMessage("admin.take.insufficient"));
            }
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, module.getMessage("error.invalid_number"));
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, module.getMessage("admin.set.usage"));
            return;
        }

        Player target = sender.getServer().getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, module.getMessage("error.player_not_found"));
            return;
        }

        try {
            int amount = parseAmount(args[2]);
            if (amount < 0) {
                MessageUtils.send(sender, module.getMessage("admin.set.negative"));
                return;
            }

            // Remove all then give
            module.getCurrencyManager().removeCurrencyItems(target);
            if (amount > 0) {
                module.getCurrencyManager().givePhysicalCurrency(target, amount);
            }
            
            MessageUtils.send(sender, module.getMessage("admin.set.success_sender")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%target%", target.getName()));
            MessageUtils.send(target, module.getMessage("admin.set.success_target")
                    .replace("%amount%", String.valueOf(amount)));
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, module.getMessage("error.invalid_number"));
        }
    }

    private void handleBalance(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1) {
            target = sender.getServer().getPlayer(args[1]);
            if (target == null) {
                MessageUtils.send(sender, module.getMessage("error.player_not_found"));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            MessageUtils.send(sender, module.getMessage("error.must_specify_player"));
            return;
        }

        int physical = module.getCurrencyManager().getPhysicalBalance(target);
        MessageUtils.send(sender, module.getMessage("balance.other").replace("%player%", target.getName()));
        MessageUtils.send(sender, module.getMessage("balance.physical").replace("%amount%", String.valueOf(physical)));
    }

    private void sendHelp(CommandSender sender) {
        MessageUtils.send(sender, module.getMessage("admin.help.header"));
        MessageUtils.send(sender, module.getMessage("admin.help.give"));
        MessageUtils.send(sender, module.getMessage("admin.help.take"));
        MessageUtils.send(sender, module.getMessage("admin.help.set"));
        MessageUtils.send(sender, module.getMessage("admin.help.balance"));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("give", "take", "set", "balance", "help"));
            return StringUtil.copyPartialMatches(args[0], suggestions, new ArrayList<>());
        } else if (args.length == 2 && ("give".equals(args[0]) || "take".equals(args[0]) || "set".equals(args[0]) || "balance".equals(args[0]))) {
            // Sugerir jogadores online para o comando give, take, set, balance
            List<String> playerNames = new ArrayList<>();
            for (Player player : sender.getServer().getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        } else if (args.length == 3 && ("give".equals(args[0]) || "take".equals(args[0]) || "set".equals(args[0]))) {
            // Sugestões de quantidades comuns
            List<String> amounts = Arrays.asList("64", "100", "1k", "10k", "1m");
            return StringUtil.copyPartialMatches(args[2], amounts, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
