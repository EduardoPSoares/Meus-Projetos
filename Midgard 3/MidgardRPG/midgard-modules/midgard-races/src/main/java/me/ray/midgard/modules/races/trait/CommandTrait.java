package me.ray.midgard.modules.races.trait;

import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.races.api.RaceTrait;
import me.ray.midgard.modules.races.api.TraitTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class CommandTrait implements RaceTrait {

    @Override
    public String getId() {
        return "command";
    }

    @Override
    public void execute(Player player, TraitTrigger trigger, Map<String, Object> context, Map<String, Object> config) {
        if (!config.containsKey("commands")) { return; }

        Object commandsObj = config.get("commands");
        if (!(commandsObj instanceof List)) { return; }
        
        List<?> commands = (List<?>) commandsObj;
        
        Object asConsoleObj = config.get("as_console");
        boolean asConsole = true;
        if (asConsoleObj instanceof Boolean) {
            asConsole = (Boolean) asConsoleObj;
        }

        for (Object cmdObj : commands) {
            String cmd = cmdObj.toString();
            String finalCmd = cmd.replace("%player%", player.getName());
            
            if (asConsole) {
                Task.sync(() -> {
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                    } catch (Exception e) {
                        me.ray.midgard.core.debug.MidgardLogger.error("Erro ao executar comando trait: %s", finalCmd, e);
                    }
                });
            } else {
                Task.sync(player, () -> {
                    try {
                        if (!player.isOnline()) { return; }
                        player.performCommand(finalCmd);
                    } catch (Exception e) {
                        me.ray.midgard.core.debug.MidgardLogger.error("Erro ao executar comando para %s: %s", player.getName(), finalCmd, e);
                    }
                });
            }
        }
    }
}
