package me.ray.midgard.modules.professions.command;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.professions.ProfessionManager;
import me.ray.midgard.modules.professions.ProfessionType;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.gui.ProfessionProgressionGui;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Comando admin para gerenciar profissões.
 * Registrado como subcomando de /rpg admin: "/rpg admin profession ..."
 *
 * Subcomandos:
 * - set <player> <profissão> <nível>  — Define nível de profissão para um jogador
 * - addxp <player> <profissão> <xp>   — Adiciona XP a um jogador
 * - info <player> <profissão>         — Mostra info de profissão de um jogador
 * - menu <profissão>                  — Abre o menu de progressão (para o executor)
 * - openmenu <player> <profissão>     — Abre o menu de progressão para outro jogador
 */
public class ProfessionCommand extends MidgardCommand {

    private static final List<String> SUB_COMMANDS = List.of("set", "addxp", "info", "menu", "openmenu", "choose");

    private final ProfessionsModule module;

    public ProfessionCommand(ProfessionsModule module) {
        super("profession", "midgard.admin.profession", false);
        this.module = module;
    }

    private String msg(String key) {
        return module.getMessage("professions.command." + key);
    }

    @Override
    public List<String> getAliases() {
        return List.of("prof");
    }

    @Override
    public String getDescription() {
        return "Gerencia profissões dos jogadores";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args);
            case "addxp" -> handleAddXp(sender, args);
            case "info" -> handleInfo(sender, args);
            case "menu" -> handleMenu(sender, args);
            case "openmenu" -> handleOpenMenu(sender, args);
            case "choose" -> handleChoose(sender, args);
            default -> sendHelp(sender);
        }
    }

    // ---------- SET ----------

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, msg("usage_set"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, msg("player_not_found").replace("%name%", args[1]));
            return;
        }

        Optional<ProfessionType> typeOpt = ProfessionType.fromId(args[2]);
        if (typeOpt.isEmpty()) {
            MessageUtils.send(sender, msg("invalid_profession").replace("%name%", args[2]));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, msg("invalid_number").replace("%value%", args[3]));
            return;
        }

        ProfessionManager manager = module.getProfessionManager();
        if (manager == null) {
            MessageUtils.send(sender, msg("system_disabled"));
            return;
        }

        manager.setLevel(target, typeOpt.get(), level);
        MessageUtils.send(sender, msg("set_success")
                .replace("%profession%", typeOpt.get().getDisplayName())
                .replace("%player%", target.getName())
                .replace("%level%", String.valueOf(level)));
    }

    // ---------- ADDXP ----------

    private void handleAddXp(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtils.send(sender, msg("usage_addxp"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, msg("player_not_found").replace("%name%", args[1]));
            return;
        }

        Optional<ProfessionType> typeOpt = ProfessionType.fromId(args[2]);
        if (typeOpt.isEmpty()) {
            MessageUtils.send(sender, msg("invalid_profession").replace("%name%", args[2]));
            return;
        }

        double xp;
        try {
            xp = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            MessageUtils.send(sender, msg("invalid_number").replace("%value%", args[3]));
            return;
        }

        ProfessionManager manager = module.getProfessionManager();
        if (manager == null) {
            MessageUtils.send(sender, msg("system_disabled"));
            return;
        }

        int levelsGained = manager.addXp(target, typeOpt.get(), xp);
        String levelsStr = levelsGained > 0
                ? msg("addxp_levels").replace("%count%", String.valueOf(levelsGained))
                : "";
        MessageUtils.send(sender, msg("addxp_success")
                .replace("%xp%", String.format("%.0f", xp))
                .replace("%profession%", typeOpt.get().getDisplayName())
                .replace("%player%", target.getName())
                .replace("%levels%", levelsStr));
    }

    // ---------- INFO ----------

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, msg("usage_info"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, msg("player_not_found").replace("%name%", args[1]));
            return;
        }

        Optional<ProfessionType> typeOpt = ProfessionType.fromId(args[2]);
        if (typeOpt.isEmpty()) {
            MessageUtils.send(sender, msg("invalid_profession").replace("%name%", args[2]));
            return;
        }

        ProfessionManager manager = module.getProfessionManager();
        if (manager == null) {
            MessageUtils.send(sender, msg("system_disabled"));
            return;
        }

        ProfessionType type = typeOpt.get();
        var progress = manager.getProgress(target, type);
        int level = progress != null ? progress.getLevel() : 0;
        double xp = progress != null ? progress.getXp() : 0;
        double xpNext = progress != null ? progress.getXpToNextLevel() : 0;

        MessageUtils.send(sender, msg("info_header")
                .replace("%symbol%", type.getSymbol())
                .replace("%profession%", type.getDisplayName()));
        MessageUtils.send(sender, msg("info_player").replace("%player%", target.getName()));
        MessageUtils.send(sender, msg("info_level").replace("%level%", String.valueOf(level)));
        MessageUtils.send(sender, msg("info_xp")
                .replace("%current%", String.format("%.0f", xp))
                .replace("%max%", String.format("%.0f", xpNext)));
    }

    // ---------- MENU ----------

    private void handleMenu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, msg("players_only"));
            return;
        }

        if (args.length < 2) {
            MessageUtils.send(sender, msg("usage_menu"));
            return;
        }

        Optional<ProfessionType> typeOpt = ProfessionType.fromId(args[1]);
        if (typeOpt.isEmpty()) {
            MessageUtils.send(sender, msg("invalid_profession").replace("%name%", args[1]));
            return;
        }

        new ProfessionProgressionGui(player, typeOpt.get(), 0).open();
    }

    // ---------- OPENMENU ----------

    private void handleOpenMenu(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, msg("usage_openmenu"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, msg("player_not_found").replace("%name%", args[1]));
            return;
        }

        Optional<ProfessionType> typeOpt = ProfessionType.fromId(args[2]);
        if (typeOpt.isEmpty()) {
            MessageUtils.send(sender, msg("invalid_profession").replace("%name%", args[2]));
            return;
        }

        new ProfessionProgressionGui(target, typeOpt.get(), 0).open();
        MessageUtils.send(sender, msg("menu_opened")
                .replace("%profession%", typeOpt.get().getDisplayName())
                .replace("%player%", target.getName()));
    }

    // ---------- CHOOSE ----------

    private void handleChoose(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.send(sender, msg("usage_choose"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, msg("player_not_found").replace("%name%", args[1]));
            return;
        }

        Optional<ProfessionType> typeOpt = ProfessionType.fromId(args[2]);
        if (typeOpt.isEmpty()) {
            MessageUtils.send(sender, msg("invalid_profession").replace("%name%", args[2]));
            return;
        }

        ProfessionManager manager = module.getProfessionManager();
        if (manager == null) {
            MessageUtils.send(sender, msg("system_disabled"));
            return;
        }

        ProfessionType type = typeOpt.get();
        manager.chooseProfession(target, type);
        MessageUtils.send(sender, msg("choose_success")
                .replace("%player%", target.getName())
                .replace("%profession%", type.getDisplayName())
                .replace("%symbol%", type.getSymbol()));
    }

    // ---------- HELP ----------

    private void sendHelp(CommandSender sender) {
        MessageUtils.send(sender, msg("help_header"));
        MessageUtils.send(sender, msg("help_set"));
        MessageUtils.send(sender, msg("help_addxp"));
        MessageUtils.send(sender, msg("help_info"));
        MessageUtils.send(sender, msg("help_menu"));
        MessageUtils.send(sender, msg("help_openmenu"));
        MessageUtils.send(sender, msg("help_choose"));
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("help_professions")
                .replace("%list%", String.join(", ", professionIds())));
    }

    // ---------- TAB COMPLETE ----------

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return match(args[0], SUB_COMMANDS);
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set", "addxp", "info", "openmenu" -> {
                if (args.length == 2) {
                    return match(args[1], onlinePlayers());
                }
                if (args.length == 3) {
                    return match(args[2], professionIds());
                }
            }
            case "menu" -> {
                if (args.length == 2) {
                    return match(args[1], professionIds());
                }
            }
            case "choose" -> {
                if (args.length == 2) {
                    return match(args[1], onlinePlayers());
                }
                if (args.length == 3) {
                    return match(args[2], professionIds());
                }
            }
        }

        return Collections.emptyList();
    }

    private static List<String> professionIds() {
        List<String> ids = new ArrayList<>();
        for (ProfessionType type : ProfessionType.values()) {
            ids.add(type.getId());
        }
        return ids;
    }
}
