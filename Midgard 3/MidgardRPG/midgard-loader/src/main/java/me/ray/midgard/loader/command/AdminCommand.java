package me.ray.midgard.loader.command;

import me.ray.midgard.core.command.AdminCommandRegistry;
import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.*;

/**
 * Comando central de administração do MidgardRPG.
 * Agrupa todos os subcomandos administrativos sob /rpg admin <subcomando>
 * 
 * Uso: /rpg admin <reload|reset|stats|scan|item|performance>
 */
public class AdminCommand extends MidgardCommand implements AdminCommandRegistry {

    private final Map<String, MidgardCommand> subcommands = new LinkedHashMap<>();
    private final JavaPlugin plugin;

    public AdminCommand(JavaPlugin plugin) {
        super("admin", "midgard.admin", false);
        this.plugin = plugin;
        
        // Register core subcommands
        registerSubcommand(new ReloadCommand(plugin));
        registerSubcommand(new ResetCommand());
        registerSubcommand(new ScanMessagesCommand(plugin));
        registerSubcommand(new InfoCommand());
    }
    
    /**
     * Returns all registered subcommands.
     */
    public Collection<MidgardCommand> getSubcommands() {
        // Return unique commands (no aliases)
        Map<String, MidgardCommand> unique = new LinkedHashMap<>();
        for (MidgardCommand cmd : subcommands.values()) {
            unique.putIfAbsent(cmd.getName().toLowerCase(), cmd);
        }
        return unique.values();
    }

    /**
     * Registra um subcomando de admin.
     * 
     * @param command O comando a ser registrado
     */
    @Override
    public void registerSubcommand(MidgardCommand command) {
        if (command == null) {
            return;
        }
        subcommands.put(command.getName().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            subcommands.put(alias.toLowerCase(), command);
        }
    }

    /**
     * Remove um subcomando de admin.
     * 
     * @param name Nome do subcomando
     */
    @Override
    public void unregisterSubcommand(String name) {
        if (name == null) {
            return;
        }
        subcommands.remove(name.toLowerCase());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender == null) {
            return;
        }
        
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendAdminHelp(sender);
            return;
        }

        String subcommandName = args[0].toLowerCase();
        MidgardCommand subcommand = subcommands.get(subcommandName);

        if (subcommand == null) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.admin.unknown_subcommand", "%sub%", subcommandName));
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.admin.help_prompt"));
            return;
        }

        // Verificar permissão específica do subcomando
        String subPerm = subcommand.getPermission();
        if (subPerm != null && !sender.hasPermission(subPerm)) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("loader.admin.no_permission"));
            return;
        }

        // Verificar flag playerOnly
        if (subcommand.isPlayerOnly() && !(sender instanceof org.bukkit.entity.Player)) {
            MessageUtils.send(sender, MidgardCore.getLanguageManager().getMessage("core.general.players_only"));
            return;
        }

        // Executar subcomando com argumentos restantes
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subcommand.execute(sender, subArgs);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender == null) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            // Sugerir subcomandos de admin
            List<String> completions = new ArrayList<>();
            for (Map.Entry<String, MidgardCommand> entry : subcommands.entrySet()) {
                MidgardCommand cmd = entry.getValue();
                // Só sugerir se o nome for igual à chave (evita duplicatas de aliases)
                if (entry.getKey().equals(cmd.getName().toLowerCase())) {
                    String perm = cmd.getPermission();
                    if (perm == null || sender.hasPermission(perm)) {
                        completions.add(cmd.getName());
                    }
                }
            }
            return StringUtil.copyPartialMatches(args[0], completions, new ArrayList<>());
        } else if (args.length > 1) {
            // Delegar tab completion ao subcomando
            String subcommandName = args[0].toLowerCase();
            MidgardCommand subcommand = subcommands.get(subcommandName);
            if (subcommand != null) {
                String perm = subcommand.getPermission();
                if (perm == null || sender.hasPermission(perm)) {
                    String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                    return subcommand.tabComplete(sender, subArgs);
                }
            }
        }
        return Collections.emptyList();
    }

    private void sendAdminHelp(CommandSender sender) {
        MidgardCore.getHelpSystem().sendAdminHelp(sender, getSubcommands());
    }

    @Override
    public List<String> getAliases() {
        return List.of("adm", "a");
    }
}
