package me.ray.midgard.modules.professions.blacksmith.forge.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.*;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.admin.SmelteryCreationManager;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.ghost.SmelteryBlueprintItem;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.ghost.SmelteryGhostBlockManager;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.schematic.SmelteryTemplate;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Comandos administrativos do sistema de Fundição (Smeltery).
 * /rpg admin smeltery [subcommand]
 */
public class SmelteryAdminCommand extends MidgardCommand {

    private final SmelteryManager smelteryManager;

    private SmelteryCreationManager creationManager;
    private SmelteryGhostBlockManager ghostBlockManager;

    public SmelteryAdminCommand(SmelteryManager smelteryManager) {
        super("smeltery", "midgard.admin.smeltery", true);
        this.smelteryManager = smelteryManager;
    }

    public void setCreationManager(SmelteryCreationManager creationManager) {
        this.creationManager = creationManager;
    }

    public void setGhostBlockManager(SmelteryGhostBlockManager ghostBlockManager) {
        this.ghostBlockManager = ghostBlockManager;
    }

    @Override
    public List<String> getAliases() {
        return List.of("sm", "tinkers");
    }

    @Override
    public String getDescription() {
        return msg("command.smeltery_description");
    }

    @Override
    public String getUsage() {
        return "/rpg admin smeltery [list|info|remove|detect|giveingot|stats|help]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> handleList(player);
            case "info" -> handleInfo(player, args);
            case "remove" -> handleRemove(player, args);
            case "detect" -> handleDetect(player);
            case "giveingot" -> handleGiveIngot(player, args);
            case "stats" -> handleStats(player);
            case "addfuel", "fuel" -> handleAddFuel(player, args);
            case "addmetal", "metal" -> handleAddMetal(player, args);
            case "templates" -> handleTemplates(player);
            case "create" -> handleCreate(player);
            case "cancelcreate" -> handleCancelCreate(player);
            case "giveblueprint" -> handleGiveBlueprint(player, args);
            case "rotate" -> handleRotate(player);
            case "cancelbuild" -> handleCancelBuild(player);
            case "help" -> sendHelp(player);
            default -> sendHelp(player);
        }
    }

    private void handleList(Player player) {
        Collection<SmelteryStructure> smelteries = smelteryManager.getRegistry().getAll();
        MessageUtils.send(player, msg("smeltery.list.header").replace("%count%", String.valueOf(smelteries.size())));

        if (smelteries.isEmpty()) {
            MessageUtils.send(player, msg("smeltery.list.none"));
            return;
        }

        for (SmelteryStructure smeltery : smelteries) {
            String ownerName = Bukkit.getOfflinePlayer(smeltery.getOwnerUuid()).getName();
            SmelteryTank tank = smeltery.getTank();
            MessageUtils.send(player, msg("smeltery.list.entry")
                    .replace("%id%", smeltery.getSmelteryId().toString().substring(0, 8))
                    .replace("%tier%", smeltery.getTier().getName())
                    .replace("%world%", smeltery.getWorldName())
                    .replace("%x%", String.valueOf(smeltery.getX()))
                    .replace("%y%", String.valueOf(smeltery.getY()))
                    .replace("%z%", String.valueOf(smeltery.getZ()))
                    .replace("%volume%", String.valueOf(tank.getTotalVolume()))
                    .replace("%capacity%", String.valueOf(tank.getCapacity()))
                    .replace("%owner%", ownerName != null ? ownerName : "?"));
        }
    }

    private void handleInfo(Player player, String[] args) {
        SmelteryStructure smeltery = findSmelteryByArg(player, args);
        if (smeltery == null) { return; }

        SmelteryTank tank = smeltery.getTank();
        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("smeltery.info.header"));
        MessageUtils.send(player, msg("smeltery.info.id").replace("%id%", smeltery.getSmelteryId().toString()));
        MessageUtils.send(player, msg("smeltery.info.tier").replace("%tier%", smeltery.getTier().getName()));
        MessageUtils.send(player, msg("smeltery.info.position")
                .replace("%world%", smeltery.getWorldName())
                .replace("%x%", String.valueOf(smeltery.getX()))
                .replace("%y%", String.valueOf(smeltery.getY()))
                .replace("%z%", String.valueOf(smeltery.getZ())));
        MessageUtils.send(player, smeltery.isActive() ? msg("smeltery.info.active_yes") : msg("smeltery.info.active_no"));
        MessageUtils.send(player, smeltery.isHeated() ? msg("smeltery.info.heated_yes") : msg("smeltery.info.heated_no"));
        MessageUtils.send(player, msg("smeltery.info.temperature").replace("%temp%", String.valueOf(tank.getTemperature())));
        MessageUtils.send(player, msg("smeltery.info.fuel").replace("%fuel%", String.valueOf(smeltery.getFuelRemaining())));
        MessageUtils.send(player, msg("smeltery.info.tank")
                .replace("%volume%", String.valueOf(tank.getTotalVolume()))
                .replace("%capacity%", String.valueOf(tank.getCapacity())));

        if (!tank.isEmpty()) {
            MessageUtils.send(player, msg("smeltery.info.metals_header"));
            for (var entry : tank.getSortedContents()) {
                MessageUtils.send(player, msg("smeltery.info.metal_entry")
                        .replace("%metal%", entry.getKey().getFormattedName())
                        .replace("%amount%", String.valueOf(entry.getValue())));
            }
        }

        if (!smeltery.getSmeltingQueue().isEmpty()) {
            MessageUtils.send(player, msg("smeltery.info.queue_header")
                    .replace("%count%", String.valueOf(smeltery.getSmeltingQueue().size())));
            for (SmelteryStructure.SmeltingEntry entry : smeltery.getSmeltingQueue()) {
                MessageUtils.send(player, msg("smeltery.info.queue_entry")
                        .replace("%metal%", entry.getOutputMetal().getFormattedName())
                        .replace("%remaining%", String.valueOf(entry.getRemainingItems()))
                        .replace("%progress%", String.format("%.0f%%", entry.getProgressPercent() * 100)));
            }
        }

        MessageUtils.send(player, msg("smeltery.info.items_smelted").replace("%count%", String.valueOf(smeltery.getTotalItemsSmelted())));
        MessageUtils.send(player, "");
    }

    private void handleRemove(Player player, String[] args) {
        SmelteryStructure smeltery = findSmelteryByArg(player, args);
        if (smeltery == null) { return; }

        smelteryManager.getRegistry().unregister(smeltery.getSmelteryId());
        MessageUtils.send(player, msg("smeltery.remove.success").replace("%id%", smeltery.getSmelteryId().toString()));
    }

    private void handleDetect(Player player) {
        SmelteryStructure detected = smelteryManager.detectAndRegister(
                player, player.getTargetBlockExact(5) != null
                        ? player.getTargetBlockExact(5).getLocation()
                        : player.getLocation());

        if (detected != null) {
            MessageUtils.send(player, msg("smeltery.detect.success"));
            MessageUtils.send(player, msg("smeltery.detect.tier").replace("%tier%", detected.getTier().getName()));
            MessageUtils.send(player, msg("smeltery.detect.id").replace("%id%", detected.getSmelteryId().toString()));
        } else {
            MessageUtils.send(player, msg("smeltery.detect.not_found"));
        }
    }

    private void handleGiveIngot(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(player, msg("smeltery.giveingot.usage"));
            MessageUtils.send(player, msg("smeltery.giveingot.metals_header"));
            for (MoltenMetal metal : MoltenMetal.values()) {
                MessageUtils.send(player, msg("smeltery.giveingot.metal_entry")
                        .replace("%name%", metal.name())
                        .replace("%formatted%", metal.getFormattedName())
                        .replace("%alloy%", metal.isAlloy() ? msg("smeltery.giveingot.alloy_tag") : ""));
            }
            return;
        }

        Player target;
        String metalName;
        int amount = 1;

        if (args.length >= 3 && Bukkit.getPlayer(args[1]) != null) {
            target = Bukkit.getPlayer(args[1]);
            metalName = args[2].toUpperCase();
            if (args.length >= 4) {
                try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) { /* uses default */ }
            }
        } else {
            target = player;
            metalName = args[1].toUpperCase();
            if (args.length >= 3) {
                try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) { /* uses default */ }
            }
        }

        MoltenMetal metal;
        try {
            metal = MoltenMetal.valueOf(metalName);
        } catch (IllegalArgumentException e) {
            MessageUtils.send(player, msg("smeltery.giveingot.invalid_metal").replace("%name%", metalName));
            return;
        }

        amount = Math.max(1, Math.min(amount, 64));
        ItemStack ingot = SmelteryOutputItem.createIngot(metal);
        ingot.setAmount(amount);
        target.getInventory().addItem(ingot);
        MessageUtils.send(player, msg("smeltery.giveingot.success")
                .replace("%amount%", String.valueOf(amount))
                .replace("%metal%", metal.getFormattedName())
                .replace("%player%", target.getName()));
    }

    private void handleStats(Player player) {
        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("smeltery.stats.header"));
        MessageUtils.send(player, msg("smeltery.stats.registered").replace("%count%", String.valueOf(smelteryManager.getRegistry().size())));

        int totalMetal = 0;
        int totalFuel = 0;
        int activeCount = 0;
        int heatedCount = 0;
        for (SmelteryStructure s : smelteryManager.getRegistry().getAll()) {
            totalMetal += s.getTank().getTotalVolume();
            totalFuel += s.getFuelRemaining();
            if (s.isActive()) { activeCount++; }
            if (s.isHeated()) { heatedCount++; }
        }

        MessageUtils.send(player, msg("smeltery.stats.active").replace("%count%", String.valueOf(activeCount)));
        MessageUtils.send(player, msg("smeltery.stats.heated").replace("%count%", String.valueOf(heatedCount)));
        MessageUtils.send(player, msg("smeltery.stats.total_metal").replace("%amount%", String.valueOf(totalMetal)));
        MessageUtils.send(player, msg("smeltery.stats.total_fuel").replace("%amount%", String.valueOf(totalFuel)));
        MessageUtils.send(player, msg("smeltery.stats.metals_available").replace("%count%", String.valueOf(MoltenMetal.values().length)));
        MessageUtils.send(player, msg("smeltery.stats.alloy_recipes").replace("%count%", String.valueOf(smelteryManager.getAlloyRecipeManager().getAllRecipes().size())));
        MessageUtils.send(player, "");
    }

    private void handleAddFuel(Player player, String[] args) {
        SmelteryStructure smeltery = findNearestSmeltery(player);
        if (smeltery == null) {
            MessageUtils.send(player, msg("smeltery.no_smeltery_nearby"));
            return;
        }

        int ticks = 20000;
        if (args.length >= 2) {
            try { ticks = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) { /* uses default */ }
        }

        smeltery.addFuel(ticks);
        MessageUtils.send(player, msg("smeltery.addfuel.success")
                .replace("%ticks%", String.valueOf(ticks))
                .replace("%seconds%", String.valueOf(ticks / 20)));
    }

    private void handleAddMetal(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.send(player, msg("smeltery.addmetal.usage"));
            return;
        }

        SmelteryStructure smeltery = findNearestSmeltery(player);
        if (smeltery == null) {
            MessageUtils.send(player, msg("smeltery.no_smeltery_nearby"));
            return;
        }

        MoltenMetal metal;
        try {
            metal = MoltenMetal.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtils.send(player, msg("smeltery.addmetal.invalid_metal").replace("%name%", args[1]));
            return;
        }

        int amount = 1296;
        if (args.length >= 3) {
            try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) { /* uses default */ }
        }

        int added = smeltery.getTank().addMetal(metal, amount);
        MessageUtils.send(player, msg("smeltery.addmetal.success")
                .replace("%amount%", String.valueOf(added))
                .replace("%metal%", metal.getFormattedName()));
    }

    // ── Subcomandos de Templates/Construção ──

    private void handleTemplates(Player player) {
        if (creationManager == null) {
            MessageUtils.send(player, msg("smeltery.templates.unavailable"));
            return;
        }
        creationManager.openAdminPanel(player);
    }

    private void handleCreate(Player player) {
        if (creationManager == null) {
            MessageUtils.send(player, msg("smeltery.templates.unavailable"));
            return;
        }
        creationManager.startSession(player);
    }

    private void handleCancelCreate(Player player) {
        if (creationManager == null) {
            MessageUtils.send(player, msg("smeltery.templates.unavailable"));
            return;
        }
        creationManager.cancelSession(player);
    }

    private void handleGiveBlueprint(Player player, String[] args) {
        if (creationManager == null) {
            MessageUtils.send(player, msg("smeltery.templates.unavailable"));
            return;
        }

        List<SmelteryTemplate> templates = creationManager.getTemplates();
        if (templates.isEmpty()) {
            if (args.length < 2) {
                MessageUtils.send(player, msg("smeltery.giveblueprint.usage_tier"));
                MessageUtils.send(player, msg("smeltery.giveblueprint.tiers")
                        .replace("%tiers%", String.join(", ",
                                Arrays.stream(SmelteryTier.values()).map(Enum::name).toArray(String[]::new))));
                return;
            }

            Player target;
            String tierName;
            if (args.length >= 3) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    MessageUtils.send(player, msg("smeltery.player_not_found").replace("%name%", args[1]));
                    return;
                }
                tierName = args[2].toUpperCase();
            } else {
                target = player;
                tierName = args[1].toUpperCase();
            }

            SmelteryTier tier;
            try {
                tier = SmelteryTier.valueOf(tierName);
            } catch (IllegalArgumentException e) {
                MessageUtils.send(player, msg("smeltery.giveblueprint.invalid_tier").replace("%name%", tierName));
                return;
            }

            ItemStack blueprint = SmelteryBlueprintItem.create(tier);
            target.getInventory().addItem(blueprint);
            MessageUtils.send(player, msg("smeltery.giveblueprint.success_tier")
                    .replace("%tier%", tier.getFormattedName())
                    .replace("%player%", target.getName()));
            return;
        }

        if (args.length < 2) {
            MessageUtils.send(player, msg("smeltery.giveblueprint.usage_template"));
            MessageUtils.send(player, msg("smeltery.giveblueprint.templates_header"));
            for (int i = 0; i < templates.size(); i++) {
                SmelteryTemplate t = templates.get(i);
                MessageUtils.send(player, msg("smeltery.giveblueprint.template_entry")
                        .replace("%index%", String.valueOf(i + 1))
                        .replace("%name%", t.getName())
                        .replace("%tier%", t.getTier().getFormattedName()));
            }
            return;
        }

        Player target;
        String templateRef;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                MessageUtils.send(player, msg("smeltery.player_not_found").replace("%name%", args[1]));
                return;
            }
            templateRef = args[2];
        } else {
            target = player;
            templateRef = args[1];
        }

        SmelteryTemplate template = findTemplate(templateRef, templates);
        if (template == null) {
            try {
                SmelteryTier tier = SmelteryTier.valueOf(templateRef.toUpperCase());
                ItemStack blueprint = SmelteryBlueprintItem.create(tier);
                target.getInventory().addItem(blueprint);
                MessageUtils.send(player, msg("smeltery.giveblueprint.success_tier")
                        .replace("%tier%", tier.getFormattedName())
                        .replace("%player%", target.getName()));
            } catch (IllegalArgumentException e) {
                MessageUtils.send(player, msg("smeltery.giveblueprint.not_found").replace("%name%", templateRef));
            }
            return;
        }

        ItemStack blueprint = SmelteryBlueprintItem.create(template);
        target.getInventory().addItem(blueprint);
        MessageUtils.send(player, msg("smeltery.giveblueprint.success_template")
                .replace("%name%", template.getName())
                .replace("%player%", target.getName()));
    }

    private void handleRotate(Player player) {
        if (ghostBlockManager == null) {
            MessageUtils.send(player, msg("smeltery.system_unavailable"));
            return;
        }
        if (!ghostBlockManager.hasSession(player.getUniqueId())) {
            MessageUtils.send(player, msg("smeltery.no_build_session"));
            return;
        }
        ghostBlockManager.rotateSession(player);
    }

    private void handleCancelBuild(Player player) {
        if (ghostBlockManager == null) {
            MessageUtils.send(player, msg("smeltery.system_unavailable"));
            return;
        }
        if (!ghostBlockManager.hasSession(player.getUniqueId())) {
            MessageUtils.send(player, msg("smeltery.no_build_session"));
            return;
        }
        ghostBlockManager.cancelSession(player.getUniqueId());
        MessageUtils.send(player, msg("smeltery.build_cancelled"));
    }

    private SmelteryTemplate findTemplate(String ref, List<SmelteryTemplate> templates) {
        // Tentar por índice
        try {
            int idx = Integer.parseInt(ref) - 1;
            if (idx >= 0 && idx < templates.size()) { return templates.get(idx); }
        } catch (NumberFormatException ignored) { /* fallback to name/ID search */ }

        // Tentar por nome (parcial)
        String lower = ref.toLowerCase();
        for (SmelteryTemplate t : templates) {
            if (t.getName().toLowerCase().contains(lower)) { return t; }
        }

        // Tentar por ID parcial
        for (SmelteryTemplate t : templates) {
            if (t.getTemplateId().toString().toLowerCase().startsWith(lower)) { return t; }
        }

        return null;
    }

    private void sendHelp(Player player) {
        MessageUtils.send(player, "");
        MessageUtils.send(player, msg("smeltery.help.header"));
        MessageUtils.send(player, msg("smeltery.help.list"));
        MessageUtils.send(player, msg("smeltery.help.info"));
        MessageUtils.send(player, msg("smeltery.help.remove"));
        MessageUtils.send(player, msg("smeltery.help.detect"));
        MessageUtils.send(player, msg("smeltery.help.giveingot"));
        MessageUtils.send(player, msg("smeltery.help.stats"));
        MessageUtils.send(player, msg("smeltery.help.addfuel"));
        MessageUtils.send(player, msg("smeltery.help.addmetal"));
        MessageUtils.send(player, msg("smeltery.help.templates"));
        MessageUtils.send(player, msg("smeltery.help.create"));
        MessageUtils.send(player, msg("smeltery.help.cancelcreate"));
        MessageUtils.send(player, msg("smeltery.help.giveblueprint"));
        MessageUtils.send(player, msg("smeltery.help.rotate"));
        MessageUtils.send(player, msg("smeltery.help.cancelbuild"));
        MessageUtils.send(player, "");
    }

    // ── Utilitários ──

    private SmelteryStructure findSmelteryByArg(Player player, String[] args) {
        if (args.length < 2) {
            SmelteryStructure nearest = findNearestSmeltery(player);
            if (nearest == null) {
                MessageUtils.send(player, msg("smeltery.specify_id_or_nearby"));
            }
            return nearest;
        }

        String partialId = args[1].toLowerCase();
        for (SmelteryStructure s : smelteryManager.getRegistry().getAll()) {
            if (s.getSmelteryId().toString().toLowerCase().startsWith(partialId)) {
                return s;
            }
        }

        MessageUtils.send(player, msg("smeltery.not_found").replace("%id%", partialId));
        return null;
    }

    private SmelteryStructure findNearestSmeltery(Player player) {
        SmelteryStructure nearest = null;
        double minDist = Double.MAX_VALUE;
        for (SmelteryStructure s : smelteryManager.getRegistry().getAll()) {
            var anchor = s.getAnchorLocation();
            if (anchor == null || anchor.getWorld() == null || !anchor.getWorld().equals(player.getWorld())) { continue; }
            double dist = anchor.distance(player.getLocation());
            if (dist < minDist && dist < 30) {
                minDist = dist;
                nearest = s;
            }
        }
        return nearest;
    }

    private String msg(String key) {
        return ProfessionsModule.getInstance().getMessage(key);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], "list", "info", "remove", "detect", "giveingot",
                    "stats", "addfuel", "addmetal",
                    "templates", "create", "cancelcreate", "giveblueprint",
                    "rotate", "cancelbuild", "help");
        }

        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            if (sub.equals("giveingot")) {
                List<String> suggestions = new ArrayList<>(onlinePlayers());
                for (MoltenMetal m : MoltenMetal.values()) { suggestions.add(m.name()); }
                return match(args[1], suggestions);
            }
            if (sub.equals("addmetal") || sub.equals("metal")) {
                List<String> metals = new ArrayList<>();
                for (MoltenMetal m : MoltenMetal.values()) { metals.add(m.name()); }
                return match(args[1], metals);
            }
            if (sub.equals("info") || sub.equals("remove")) {
                List<String> ids = new ArrayList<>();
                for (SmelteryStructure s : smelteryManager.getRegistry().getAll()) {
                    ids.add(s.getSmelteryId().toString().substring(0, 8));
                }
                return match(args[1], ids);
            }
            if (sub.equals("giveblueprint")) {
                List<String> suggestions = new ArrayList<>(onlinePlayers());
                for (SmelteryTier t : SmelteryTier.values()) { suggestions.add(t.name()); }
                if (creationManager != null) {
                    for (SmelteryTemplate t : creationManager.getTemplates()) {
                        suggestions.add(t.getName());
                    }
                }
                return match(args[1], suggestions);
            }
        }

        if (args.length == 3) {
            if (sub.equals("giveingot")) {
                boolean isPlayerName = Bukkit.getPlayer(args[1]) != null;
                if (isPlayerName) {
                    List<String> metals = new ArrayList<>();
                    for (MoltenMetal m : MoltenMetal.values()) { metals.add(m.name()); }
                    return match(args[2], metals);
                }
            }
        }

        return Collections.emptyList();
    }
}
