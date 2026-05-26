package me.ray.midgard.core.command;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.text.MessageUtils;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Sistema centralizado de ajuda para comandos do MidgardRPG.
 * Gerencia a exibição de ajuda organizada por categorias e módulos.
 */
public class HelpSystem {
    
    private final Map<String, CommandInfo> commands = new LinkedHashMap<>();
    
    /**
     * Registra informações de um comando para exibição de ajuda.
     */
    public void register(CommandInfo info) {
        commands.put(info.getName().toLowerCase(), info);
        for (String alias : info.getAliases()) {
            commands.put(alias.toLowerCase(), info);
        }
    }
    
    /**
     * Remove informações de um comando.
     */
    public void unregister(String name) {
        CommandInfo info = commands.remove(name.toLowerCase());
        if (info != null) {
            for (String alias : info.getAliases()) {
                commands.remove(alias.toLowerCase());
            }
        }
    }
    
    /**
     * Obtém informações de um comando.
     */
    public CommandInfo getInfo(String name) {
        return commands.get(name.toLowerCase());
    }
    
    /**
     * Envia a ajuda principal para o sender.
     */
    public void sendMainHelp(CommandSender sender) {
        MessageUtils.send(sender, msg("core.help.header"));
        MessageUtils.send(sender, msg("core.help.separator"));
        
        // Agrupar por categoria
        Map<CommandCategory, List<CommandInfo>> byCategory = getVisibleCommands(sender)
            .stream()
            .collect(Collectors.groupingBy(CommandInfo::getCategory, 
                LinkedHashMap::new, Collectors.toList()));
        
        // Player commands
        if (byCategory.containsKey(CommandCategory.PLAYER)) {
            MessageUtils.send(sender, msg("core.help.category_player"));
            for (CommandInfo info : getUniqueCommands(byCategory.get(CommandCategory.PLAYER))) {
                sendCommandLine(sender, info);
            }
        }
        
        // Moderator commands
        if (byCategory.containsKey(CommandCategory.MODERATOR)) {
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, msg("core.help.category_moderator"));
            for (CommandInfo info : getUniqueCommands(byCategory.get(CommandCategory.MODERATOR))) {
                sendCommandLine(sender, info);
            }
        }
        
        // Admin commands
        if (byCategory.containsKey(CommandCategory.ADMIN)) {
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, msg("core.help.category_admin"));
            MessageUtils.send(sender, msg("core.help.admin_entry"));
        }
        
        MessageUtils.send(sender, msg("core.help.separator"));
        MessageUtils.send(sender, msg("core.help.footer_hint"));
        MessageUtils.send(sender, msg("core.help.aliases_footer"));
    }
    
    /**
     * Envia ajuda para comandos administrativos.
     */
    public void sendAdminHelp(CommandSender sender, Collection<MidgardCommand> adminCommands) {
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, msg("core.help.admin_header"));
        MessageUtils.send(sender, msg("core.help.separator"));
        
        Set<String> shown = new HashSet<>();
        for (MidgardCommand cmd : adminCommands) {
            if (shown.contains(cmd.getName())) {
                continue;
            }
            shown.add(cmd.getName());
            
            String perm = cmd.getPermission();
            if (perm != null && !sender.hasPermission(perm)) {
                continue;
            }
            
            String desc = getAdminDescription(cmd.getName());
            MessageUtils.send(sender, "<gray>• <yellow>/rpg admin " + cmd.getName() + "</yellow> <dark_gray>-</dark_gray> <gray>" + desc + "</gray>");
        }
        
        MessageUtils.send(sender, "");
    }
    
    /**
     * Envia ajuda detalhada para um comando específico.
     */
    public void sendCommandHelp(CommandSender sender, String commandName) {
        CommandInfo info = commands.get(commandName.toLowerCase());
        
        if (info == null) {
            MessageUtils.send(sender, msg("core.help.command_not_found").replace("%command%", commandName));
            return;
        }
        
        // Check permission
        if (info.getPermission() != null && !sender.hasPermission(info.getPermission())) {
            MessageUtils.send(sender, msg("core.help.no_permission_view"));
            return;
        }
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, "<gradient:#a855f7:#ec4899><bold>\u2694 /" + info.getName() + "</bold></gradient>");
        MessageUtils.send(sender, msg("core.help.separator"));
        MessageUtils.send(sender, msg("core.help.detail_description").replace("%description%", info.getDescription()));
        MessageUtils.send(sender, msg("core.help.detail_usage").replace("%usage%", info.getUsage()));
        
        if (!info.getAliases().isEmpty()) {
            MessageUtils.send(sender, msg("core.help.detail_aliases").replace("%aliases%", String.join(", ", info.getAliases())));
        }
        
        if (info.getPermission() != null) {
            MessageUtils.send(sender, msg("core.help.detail_permission").replace("%permission%", info.getPermission()));
        }
        
        MessageUtils.send(sender, msg("core.help.detail_module").replace("%module%", info.getModule()));
        MessageUtils.send(sender, "");
    }
    
    private void sendCommandLine(CommandSender sender, CommandInfo info) {
        String aliasText = info.getAliases().isEmpty() ? "" : 
            " <dark_gray>[" + String.join(", ", info.getAliases()) + "]</dark_gray>";
        MessageUtils.send(sender, "<gray>• <yellow>" + info.getUsage() + "</yellow>" + aliasText + " <dark_gray>-</dark_gray> <gray>" + info.getDescription() + "</gray>");
    }
    
    private List<CommandInfo> getVisibleCommands(CommandSender sender) {
        return commands.values().stream()
            .filter(info -> {
                // Check permission
                if (info.getPermission() != null && !sender.hasPermission(info.getPermission())) {
                    return false;
                }
                // Check category permission
                switch (info.getCategory()) {
                    case ADMIN:
                        return sender.hasPermission("midgard.admin");
                    case MODERATOR:
                        return sender.hasPermission("midgard.moderator") || sender.hasPermission("midgard.admin");
                    default:
                        return true;
                }
            })
            .collect(Collectors.toList());
    }
    
    private List<CommandInfo> getUniqueCommands(List<CommandInfo> infos) {
        Map<String, CommandInfo> unique = new LinkedHashMap<>();
        for (CommandInfo info : infos) {
            unique.putIfAbsent(info.getName().toLowerCase(), info);
        }
        return new ArrayList<>(unique.values());
    }
    
    private String getAdminDescription(String commandName) {
        try {
            String key = "loader.admin.descriptions." + commandName.toLowerCase();
            String val = MidgardCore.getLanguageManager().getRawMessage(key);
            if (val != null && !val.equals(key)) {
                return val;
            }
        } catch (Exception e) {
            MidgardLogger.warn("Erro ao buscar descrição do comando admin '%s'", commandName);
        }
        
        return switch (commandName.toLowerCase()) {
            case "reload" -> "Recarrega configurações e módulos";
            case "reset" -> "Reseta dados de um jogador";
            case "info" -> "Informações detalhadas de um jogador";
            case "scan", "scanmessages" -> "Escaneia chaves de mensagens";
            case "item" -> "Gerencia itens do sistema";
            case "performance", "perf" -> "Métricas de performance";
            case "class" -> "Gerencia classes";
            case "import" -> "Importa pacotes externos (MMOCore)";
            case "dummy" -> "Cria/remove dummies de combate";
            case "xp" -> "Gerencia XP de jogadores";
            case "econ" -> "Gerencia economia (esmeraldas)";
            case "race" -> "Gerencia raças";
            case "spell" -> "Gerencia magias";
            case "gamemode", "gm", "gmode" -> "Altera o gamemode";
            case "fly" -> "Ativa/desativa voo";
            case "heal" -> "Cura um jogador";
            case "feed", "eat" -> "Alimenta um jogador";
            case "setspawn" -> "Define o spawn do servidor";
            case "setwarp" -> "Cria um warp";
            case "delwarp" -> "Remove um warp";
            case "vanish", "v", "invis" -> "Fica invisível";
            case "tp", "teleport", "tele" -> "Teleporta jogadores";
            case "tphere", "tph" -> "Teleporta um jogador até você";
            case "top" -> "Teleporta ao bloco mais alto";
            case "speed" -> "Altera a velocidade";
            case "invsee", "inv", "openinv" -> "Abre o inventário de outro jogador";
            case "commands" -> "Lista e valida comandos";
            case "forge", "forgeadmin" -> "Gerencia o sistema de forja";
            case "smeltery" -> "Gerencia o sistema de fundição";
            default -> "Comando administrativo";
        };
    }

    private String msg(String key) {
        LanguageManager lm = MidgardCore.getLanguageManager();
        if (lm != null) {
            String val = lm.getRawMessage(key);
            if (val != null && !val.equals(key)) {
                return val;
            }
        }
        return key;
    }
}
