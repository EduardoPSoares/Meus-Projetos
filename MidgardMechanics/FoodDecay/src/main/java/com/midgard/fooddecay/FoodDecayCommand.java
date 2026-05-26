package com.midgard.fooddecay;

import com.midgard.core.command.CommandInfo;
import com.midgard.core.command.MidgardCommand;
import com.midgard.core.utils.MessageUtils;
import static com.midgard.core.utils.MessageUtils.sc;
import com.midgard.fooddecay.gui.CookbookGui;
import com.midgard.fooddecay.gui.FoodInspectionGui;
import com.midgard.fooddecay.gui.AdminMainGui;
import com.midgard.fooddecay.gui.NutritionGui;
import com.midgard.fooddecay.gui.RecipeManagerGui;
import com.midgard.fooddecay.multiblock.MultiblockManager;
import com.midgard.fooddecay.multiblock.MultiblockType;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command for the TFC-style FoodDecay module.
 * /fooddecay check      - open food inspection GUI
 * /fooddecay blueprint  - show multiblock blueprint
 * /fooddecay nutrition   - open nutrition GUI
 * /fooddecay cookbook    - open recipe book GUI
 * /fooddecay give       - give pre-processed food with traits
 * /fooddecay reload     - reload the configuration
 */
@CommandInfo(
        name = "fooddecay",
        aliases = {"fd", "decay"},
        permission = "midgard.fooddecay",
        description = "Gerenciar o sistema de validade de comidas",
        usage = "/fooddecay <check|blueprint|nutrition|cookbook|reload>"
)
public class FoodDecayCommand extends MidgardCommand {

    private final FoodDecayModule module;

    public FoodDecayCommand(FoodDecayModule module) {
        this.module = module;
        addSubCommand("check", new CheckSubCommand());
        addSubCommand("blueprint", new BlueprintSubCommand());
        addSubCommand("nutrition", new NutritionSubCommand());
        addSubCommand("cookbook", new CookbookSubCommand());
        addSubCommand("recipe", new RecipeSubCommand());
        addSubCommand("give", new GiveSubCommand());
        addSubCommand("ferment-barrel", new FermentBarrelSubCommand());
        addSubCommand("reload", new ReloadSubCommand());
        addSubCommand("admin", new AdminSubCommand());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!executeSubCommand(sender, args)) {
            FoodDecayConfig config = module.getDecayConfig();
            sender.sendMessage(MessageUtils.toComponent(config.getMsgCommandHeader()));
            sender.sendMessage(MessageUtils.toComponent(sc(config.msg("command-help-line-check"))));
            sender.sendMessage(MessageUtils.toComponent(sc(config.msg("command-help-line-blueprint"))));
            if (config.isNutritionEnabled()) {
                sender.sendMessage(MessageUtils.toComponent(sc(config.msg("command-help-line-nutrition"))));
            }
            sender.sendMessage(MessageUtils.toComponent(sc(config.msg("command-help-line-cookbook"))));
            sender.sendMessage(MessageUtils.toComponent(sc(config.msg("command-help-line-recipe"))));
            sender.sendMessage(MessageUtils.toComponent(sc(config.msg("command-help-line-give"))));
            sender.sendMessage(MessageUtils.toComponent(sc(config.msg("command-help-line-ferment-barrel"))));
            sender.sendMessage(MessageUtils.toComponent(sc(config.msg("command-help-line-reload"))));
            sender.sendMessage(MessageUtils.toComponent(sc(config.msg("command-help-line-admin"))));
        }
    }

    // =====================================================
    // Check subcommand — opens the Food Inspection GUI
    // =====================================================

    @CommandInfo(
            name = "check",
            permission = "midgard.fooddecay.check",
            description = "Inspecionar alimento na mao via GUI",
            playerOnly = true
    )
    private class CheckSubCommand extends MidgardCommand {
        @Override
        public void execute(CommandSender sender, String[] args) {
            Player player = (Player) sender;
            if (!module.getDecayConfig().isGuiEnabled()) {
                player.sendMessage(MessageUtils.toComponent(sc(module.getDecayConfig().msg("gui-disabled"))));
                return;
            }
            new FoodInspectionGui(module).open(player);
        }
    }

    // =====================================================
    // Blueprint subcommand — shows multiblock blueprint
    // =====================================================

    @CommandInfo(
            name = "blueprint",
            aliases = {"bp", "planta"},
            permission = "midgard.fooddecay.admin",
            description = "Obter blueprint ou cancelar construcao",
            playerOnly = true
    )
    private class BlueprintSubCommand extends MidgardCommand {
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                String lower = args[0].toLowerCase();
                for (MultiblockType type : MultiblockType.values()) {
                    if (module.getDecayConfig().isMultiblockTypeEnabled(type)
                            && type.getConfigKey().toLowerCase().startsWith(lower)) {
                        completions.add(type.getConfigKey());
                    }
                }
                if ("cancel".startsWith(lower)) {
                    completions.add("cancel");
                }
                return completions;
            }
            if (args.length == 2) {
                MultiblockType type = MultiblockType.fromKey(args[0]);
                if (type != null) {
                    List<String> completions = new ArrayList<>();
                    String lower = args[1].toLowerCase();
                    for (int t = 1; t <= type.getMaxTier(); t++) {
                        String s = String.valueOf(t);
                        if (s.startsWith(lower)) completions.add(s);
                    }
                    return completions;
                }
            }
            return new ArrayList<>();
        }
        @Override
        public void execute(CommandSender sender, String[] args) {
            Player player = (Player) sender;
            FoodDecayConfig cfg = module.getDecayConfig();
            MultiblockManager mbManager = module.getMultiblockManager();

            if (!cfg.isMultiblockEnabled()) {
                player.sendMessage(MessageUtils.toComponent(sc(cfg.msg("multiblock-disabled"))));
                return;
            }

            // /fd blueprint cancel
            if (args.length >= 1 && args[0].equalsIgnoreCase("cancel")) {
                mbManager.cancelSession(player);
                return;
            }

            // /fd blueprint (no args) — list available types
            if (args.length < 1) {
                player.sendMessage(MessageUtils.toComponent(sc(cfg.msg("blueprint-structures-header"))));
                for (MultiblockType type : MultiblockType.values()) {
                    if (cfg.isMultiblockTypeEnabled(type)) {
                        for (MultiblockType.TierDef td : type.getTiers()) {
                            player.sendMessage(MessageUtils.toComponent(
                                    sc(cfg.msg("blueprint-structure-line")
                                            .replace("{key}", type.getConfigKey() + " " + td.tier())
                                            .replace("{name}", td.displayName())
                                            .replace("{blocks}", String.valueOf(type.getTotalBlocks(td.tier()))))));
                        }
                    }
                }
                player.sendMessage(MessageUtils.toComponent(sc(cfg.msg("blueprint-usage"))));
                player.sendMessage(MessageUtils.toComponent(sc(cfg.msg("blueprint-cancel-usage"))));
                return;
            }

            // /fd blueprint <type> [tier] — give blueprint item
            MultiblockType type = MultiblockType.fromKey(args[0]);
            if (type == null) {
                StringJoiner available = new StringJoiner(", ");
                for (MultiblockType t : MultiblockType.values()) {
                    available.add(t.getConfigKey());
                }
                player.sendMessage(MessageUtils.toComponent(
                        sc(cfg.msg("blueprint-unknown")
                                .replace("{available}", available.toString()))));
                return;
            }

            if (!cfg.isMultiblockTypeEnabled(type)) {
                player.sendMessage(MessageUtils.toComponent(
                        sc(cfg.msg("blueprint-type-disabled"))));
                return;
            }

            int tier = 1;
            if (args.length >= 2) {
                try {
                    tier = Integer.parseInt(args[1]);
                    if (tier < 1 || tier > type.getMaxTier()) {
                        player.sendMessage(MessageUtils.toComponent(
                                sc("&cTier invalido. Use 1-" + type.getMaxTier() + ".")));
                        return;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(MessageUtils.toComponent(
                            sc("&cTier invalido. Use 1-" + type.getMaxTier() + ".")));
                    return;
                }
            }

            ItemStack blueprint = mbManager.createBlueprintItem(type, tier);
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(blueprint);
            if (!overflow.isEmpty()) {
                overflow.values().forEach(item ->
                        player.getWorld().dropItemNaturally(player.getLocation(), item));
            }

            player.sendMessage(MessageUtils.toComponent(
                    sc(cfg.msg("blueprint-received")
                            .replace("{name}", type.getDisplayName(tier)))));
        }
    }

    // =====================================================
    // Nutrition subcommand — opens Nutrition GUI
    // =====================================================

    @CommandInfo(
            name = "nutrition",
            aliases = {"nutri"},
            permission = "midgard.fooddecay.check",
            description = "Ver status nutricional via GUI",
            playerOnly = true
    )
    private class NutritionSubCommand extends MidgardCommand {
        @Override
        public void execute(CommandSender sender, String[] args) {
            Player player = (Player) sender;
            FoodDecayConfig cfg = module.getDecayConfig();
            NutritionManager nutritionManager = module.getNutritionManager();

            if (!cfg.isNutritionEnabled() || nutritionManager == null) {
                player.sendMessage(MessageUtils.toComponent(sc(cfg.msg("nutrition-disabled"))));
                return;
            }
            if (!cfg.isGuiEnabled()) {
                player.sendMessage(MessageUtils.toComponent(sc(cfg.msg("gui-disabled"))));
                return;
            }
            new NutritionGui(nutritionManager, cfg).open(player);
        }
    }

    // =====================================================
    // Cookbook subcommand — opens the recipe book GUI
    // =====================================================

    @CommandInfo(
            name = "cookbook",
            aliases = {"livro", "recipes"},
            permission = "midgard.fooddecay.check",
            description = "Abrir livro de receitas",
            playerOnly = true
    )
    private class CookbookSubCommand extends MidgardCommand {
        @Override
        public void execute(CommandSender sender, String[] args) {
            Player player = (Player) sender;
            if (!module.getDecayConfig().isGuiEnabled()) {
                player.sendMessage(MessageUtils.toComponent(sc(module.getDecayConfig().msg("gui-disabled"))));
                return;
            }
            new CookbookGui(module).open(player);
        }
    }

    // =====================================================
    // Give subcommand — give pre-processed food with traits
    // /fd give <material> [trait1,trait2,...] [amount] [player]
    // =====================================================

    @CommandInfo(
            name = "give",
            aliases = {"dar"},
            permission = "midgard.fooddecay.admin",
            description = "Dar comida com traços aplicados",
            playerOnly = false
    )
    private class GiveSubCommand extends MidgardCommand {
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                String lower = args[0].toLowerCase();
                return Arrays.stream(Material.values())
                        .filter(Material::isEdible)
                        .map(m -> m.name().toLowerCase())
                        .filter(n -> n.startsWith(lower))
                        .limit(30)
                        .collect(Collectors.toList());
            }
            if (args.length == 2) {
                // Suggest trait combos (comma-separated)
                String lower = args[1].toLowerCase();
                String[] parts = lower.split(",", -1);
                String prefix = parts.length > 1
                        ? lower.substring(0, lower.lastIndexOf(',') + 1) : "";
                String partial = parts[parts.length - 1];
                return Arrays.stream(FoodTrait.values())
                        .map(t -> t.name().toLowerCase())
                        .filter(n -> n.startsWith(partial))
                        .map(n -> prefix + n)
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                return List.of("1", "16", "32", "64");
            }
            if (args.length == 4) {
                String lower = args[3].toLowerCase();
                return org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(lower))
                        .collect(Collectors.toList());
            }
            return List.of();
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            FoodDecayConfig cfg = module.getDecayConfig();
            FoodDecayManager mgr = module.getManager();

            if (args.length < 1) {
                sender.sendMessage(MessageUtils.toComponent(
                        sc(cfg.msg("give-usage"))));
                return;
            }

            // Parse material
            Material material;
            try {
                material = Material.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(MessageUtils.toComponent(
                        sc(cfg.msg("give-invalid-material")
                                .replace("{input}", args[0]))));
                return;
            }

            // Parse traits
            Set<FoodTrait> traits = EnumSet.noneOf(FoodTrait.class);
            if (args.length >= 2 && !args[1].equalsIgnoreCase("none")) {
                for (String part : args[1].split(",")) {
                    FoodTrait trait = FoodTrait.fromString(part.trim());
                    if (trait == null) {
                        sender.sendMessage(MessageUtils.toComponent(
                                sc(cfg.msg("give-invalid-trait")
                                        .replace("{input}", part.trim())
                                        .replace("{available}", Arrays.stream(FoodTrait.values())
                                                .map(t -> t.name().toLowerCase())
                                                .collect(Collectors.joining(", "))))));
                        return;
                    }
                    traits.add(trait);
                }
            }

            // Parse amount
            int amount = 1;
            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                    if (amount < 1 || amount > 64) {
                        sender.sendMessage(MessageUtils.toComponent(
                                sc(cfg.msg("give-invalid-amount"))));
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.toComponent(
                            sc(cfg.msg("give-invalid-amount"))));
                    return;
                }
            }

            // Parse target player
            Player target;
            if (args.length >= 4) {
                target = org.bukkit.Bukkit.getPlayerExact(args[3]);
                if (target == null) {
                    sender.sendMessage(MessageUtils.toComponent(
                            sc(cfg.msg("give-player-not-found")
                                    .replace("{name}", args[3]))));
                    return;
                }
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(MessageUtils.toComponent(
                        sc(cfg.msg("give-specify-player"))));
                return;
            }

            // Create item
            ItemStack item = new ItemStack(material, amount);
            mgr.stampItem(item);
            for (FoodTrait trait : traits) {
                mgr.addTrait(item, trait);
            }

            Map<Integer, ItemStack> overflow = target.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                overflow.values().forEach(i ->
                        target.getWorld().dropItemNaturally(target.getLocation(), i));
            }

            String traitNames = traits.isEmpty()
                    ? cfg.msg("give-no-traits")
                    : traits.stream()
                        .map(t -> cfg.getTraitDisplayName(t))
                        .collect(Collectors.joining("&7, "));

            target.sendMessage(MessageUtils.toComponent(
                    sc(cfg.msg("give-received")
                            .replace("{amount}", String.valueOf(amount))
                            .replace("{item}", material.name().toLowerCase().replace('_', ' '))
                            .replace("{traits}", traitNames))));

            if (sender != target) {
                sender.sendMessage(MessageUtils.toComponent(
                        sc(cfg.msg("give-sent")
                                .replace("{amount}", String.valueOf(amount))
                                .replace("{item}", material.name().toLowerCase().replace('_', ' '))
                                .replace("{traits}", traitNames)
                                .replace("{player}", target.getName()))));
            }
        }
    }

    // =====================================================
    // Recipe subcommand — opens admin recipe editor GUI
    // =====================================================

    @CommandInfo(
            name = "recipe",
            aliases = {"receita", "editor"},
            permission = "midgard.fooddecay.admin",
            description = "Abrir editor de receitas (admin)",
            playerOnly = true
    )
    private class RecipeSubCommand extends MidgardCommand {
        @Override
        public void execute(CommandSender sender, String[] args) {
            Player player = (Player) sender;
            if (!module.getDecayConfig().isGuiEnabled()) {
                player.sendMessage(MessageUtils.toComponent(sc(module.getDecayConfig().msg("gui-disabled"))));
                return;
            }
            new RecipeManagerGui(module).open(player);
        }
    }

    // =====================================================
    // Reload subcommand
    // =====================================================

    @CommandInfo(
            name = "reload",
            permission = "midgard.fooddecay.reload",
            description = "Recarregar configuracao do FoodDecay"
    )
    private class ReloadSubCommand extends MidgardCommand {
        @Override
        public void execute(CommandSender sender, String[] args) {
            module.getDecayConfig().reload();
            module.getManager().stopDecayTask();
            module.getManager().startDecayTask();
            NutritionManager nm = module.getNutritionManager();
            if (nm != null) {
                nm.stopDecayTask();
                nm.reloadFoodGroups();
                nm.startDecayTask();
            }
            sender.sendMessage(MessageUtils.toComponent(module.getDecayConfig().getMsgReloadSuccess()));
        }
    }

    // =====================================================    // Ferment Barrel subcommand — give fermentation barrel item
    // =====================================================

    @CommandInfo(
            name = "ferment-barrel",
            permission = "midgard.fooddecay.fermentbarrel",
            description = "Dar barril de fermentação",
            playerOnly = false
    )
    private class FermentBarrelSubCommand extends MidgardCommand {
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                String lower = args[0].toLowerCase();
                return org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(lower))
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            FoodDecayConfig cfg = module.getDecayConfig();
            FermentationManager fermentManager = module.getFermentationManager();
            if (fermentManager == null) {
                sender.sendMessage(MessageUtils.toComponent(
                        sc(cfg.msg("fermentation-disabled"))));
                return;
            }

            Player target;
            if (args.length >= 1) {
                target = org.bukkit.Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage(MessageUtils.toComponent(
                            sc(cfg.msg("ferment-barrel-player-not-found").replace("{player}", args[0]))));
                    return;
                }
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(MessageUtils.toComponent("§cUso: /fd ferment-barrel [jogador]"));
                return;
            }

            ItemStack barrel = fermentManager.createFermentBarrelItem();
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(barrel);
            if (!overflow.isEmpty()) {
                overflow.values().forEach(item ->
                        target.getWorld().dropItemNaturally(target.getLocation(), item));
            }
            sender.sendMessage(MessageUtils.toComponent(
                    sc(cfg.msg("ferment-barrel-given").replace("{player}", target.getName()))));
        }
    }

    // =====================================================    // Admin subcommand — opens the admin settings GUI
    // =====================================================

    @CommandInfo(
            name = "admin",
            permission = "midgard.fooddecay.admin",
            description = "Abrir menu de administracao",
            playerOnly = true
    )
    private class AdminSubCommand extends MidgardCommand {
        @Override
        public void execute(CommandSender sender, String[] args) {
            new AdminMainGui(module).open((Player) sender);
        }
    }
}
