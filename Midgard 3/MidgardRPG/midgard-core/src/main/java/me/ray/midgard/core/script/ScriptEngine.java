package me.ray.midgard.core.script;

import me.ray.midgard.core.script.actions.ConsoleCommandAction;
import me.ray.midgard.core.script.actions.MessageAction;
import me.ray.midgard.core.script.actions.SoundAction;
import me.ray.midgard.core.script.conditions.ChanceCondition;
import me.ray.midgard.core.script.conditions.PermissionCondition;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptEngine {

    private static final Pattern ACTION_PATTERN = Pattern.compile("\\[(.*?)\\] (.*)");

    public static List<Action> parseActions(List<String> lines) {
        List<Action> actions = new ArrayList<>();
        if (lines == null) {
            return actions;
        }
        
        for (String line : lines) {
            try {
                Matcher matcher = ACTION_PATTERN.matcher(line);
                if (matcher.find()) {
                    String type = matcher.group(1).toLowerCase();
                    String value = matcher.group(2);

                    switch (type) {
                        case "message":
                        case "msg":
                            actions.add(new MessageAction(value));
                            break;
                        case "console":
                        case "cmd":
                            actions.add(new ConsoleCommandAction(value));
                            break;
                        case "sound":
                            actions.add(new SoundAction(value));
                            break;
                        default:
                             // Unknown action, ignore or log
                             break;
                    }
                }
            } catch (Exception e) {
                me.ray.midgard.core.debug.MidgardLogger.warn("Erro ao analisar linha de ação de script: '" + line + "' - " + e.getMessage());
            }
        }
        return actions;
    }

    public static void executeActions(Player player, List<String> lines) {
        if (player == null || lines == null) {
            return;
        }
        List<Action> actions = parseActions(lines);
        for (Action action : actions) {
            try {
                action.execute(player);
            } catch (Exception e) {
                me.ray.midgard.core.debug.MidgardLogger.error("Erro na execução de script ação para " + player.getName(), e);
            }
        }
    }

    public static List<Condition> parseConditions(List<String> lines) {
        List<Condition> conditions = new ArrayList<>();
        if (lines == null) {
            return conditions;
        }
        
        for (String line : lines) {
            try {
                String[] parts = line.split(": ", 2);
                if (parts.length < 2) {
                    continue;
                }

                String type = parts[0].toLowerCase();
                String value = parts[1];

                switch (type) {
                    case "permission":
                    case "perm":
                        conditions.add(new PermissionCondition(value));
                        break;
                    case "chance":
                        try {
                            conditions.add(new ChanceCondition(Double.parseDouble(value)));
                        } catch (NumberFormatException e) {
                             me.ray.midgard.core.debug.MidgardLogger.warn("Valor inválido para condição de chance: " + value);
                        }
                        break;
                }
            } catch (Exception e) {
                me.ray.midgard.core.debug.MidgardLogger.warn("Erro ao analisar linha de condição: '" + line + "'");
            }
        }
        return conditions;
    }

    public static boolean check(Player player, List<Condition> conditions) {
        if (player == null || conditions == null) {
            return false;
        }
        try {
            for (Condition condition : conditions) {
                if (!condition.check(player)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error("Erro ao verificar condições para " + player.getName(), e);
            return false;
        }
    }

    public static void execute(Player player, List<Action> actions) {
        if (player == null || actions == null) {
            return;
        }
        for (Action action : actions) {
            try {
                action.execute(player);
            } catch (Exception e) {
                me.ray.midgard.core.debug.MidgardLogger.error("Erro na execução de ação isolada para " + player.getName(), e);
            }
        }
    }
}
