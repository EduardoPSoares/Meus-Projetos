package me.ray.midgard.core.command;

import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.MidgardCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.List;

public abstract class MidgardCommand implements CommandExecutor, TabCompleter {

    private final String name;
    private final String permission;
    private final boolean playerOnly;

    public MidgardCommand(String name, String permission, boolean playerOnly) {
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
    }

    public boolean isPlayerOnly() {
        return playerOnly;
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }
    
    public String getName() {
        return name;
    }
    
    public String getPermission() {
        return permission;
    }

    public String getUsage() {
        return "/rpg " + name;
    }

    public String getDescription() {
        return name;
    }

    /**
     * Obtém a permissão necessária para um subcomando específico.
     * Por padrão, retorna a permissão principal do comando.
     * Pode ser sobrescrito por comandos que tenham permissões específicas por subcomando.
     * 
     * @param subCommand Nome do subcomando
     * @return Permissão necessária ou null se não houver restrição específica
     */
    public String getSubCommandPermission(String subCommand) {
        return getPermission();
    }

    public abstract void execute(CommandSender sender, String[] args);

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    protected List<String> match(String token, Collection<String> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, result);
        return result;
    }

    protected List<String> match(String token, String... options) {
        if (options == null || options.length == 0) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        Collections.addAll(list, options);
        return match(token, list);
    }

    protected List<String> onlinePlayers() {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
        }
        return names;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (playerOnly && !(sender instanceof Player)) {
            String msg = MidgardCore.getLanguageManager() != null
                ? MidgardCore.getLanguageManager().getRawMessage("core.general.players_only")
                : "This command can only be used by players.";
            MessageUtils.send(sender, msg);
            return true;
        }

        if (permission != null && !sender.hasPermission(permission)) {
            String msg = MidgardCore.getLanguageManager() != null
                ? MidgardCore.getLanguageManager().getRawMessage("core.general.no-permission")
                : "You don't have permission to use this command.";
            MessageUtils.send(sender, msg);
            return true;
        }

        try {
            execute(sender, args);
        } catch (Exception e) {
            String msg = MidgardCore.getLanguageManager() != null
                ? MidgardCore.getLanguageManager().getRawMessage("error.generic")
                : "An error occurred while executing this command.";
            MessageUtils.send(sender, msg);
            if (MidgardCore.getInstance() != null) {
                MidgardCore.getInstance().getLogger().log(java.util.logging.Level.SEVERE, "Erro ao executar comando '" + name + "': " + e.getMessage(), e);
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            return Collections.emptyList();
        }
        try {
            return tabComplete(sender, args);
        } catch (Exception e) {
            // Silently fail tab complete to avoid console spam during typing
            return Collections.emptyList();
        }
    }
}
