package de.maxhenkel.voicechat.range;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.permission.PermissionManager;
import de.maxhenkel.voicechat.range.gui.RangeListMenu;
import de.maxhenkel.voicechat.range.gui.RangeGlobalMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class VoiceRangeCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Voicechat.MESSAGES.somente_jogadores);
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission(PermissionManager.RANGE_ADMIN_PERMISSION)) {
            player.sendMessage(Voicechat.MESSAGES.range_sem_permissao);
            return true;
        }

        if (args.length < 1) {
            RangeListMenu.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "menu":
                RangeListMenu.open(player);
                return true;
            case "set":
                return handleSet(player, args);
            case "remove":
                return handleRemove(player, args);
            case "info":
                return handleInfo(player, args);
            case "list":
                return handleList(player);
            case "reload":
                return handleReload(player);
            case "global":
                return handleGlobal(player, args);
            case "volume":
                return handleVolume(player, args);
            case "priority":
                return handlePriority(player, args);
            case "cooldown":
                return handleCooldown(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Voicechat.MESSAGES.range_titulo);
        player.sendMessage(Voicechat.MESSAGES.range_menu + Voicechat.MESSAGES.range_menu_desc);
        player.sendMessage(Voicechat.MESSAGES.range_set + Voicechat.MESSAGES.range_set_desc);
        player.sendMessage(Voicechat.MESSAGES.range_remove + Voicechat.MESSAGES.range_remove_desc);
        player.sendMessage(Voicechat.MESSAGES.range_info + Voicechat.MESSAGES.range_info_desc);
        player.sendMessage(Voicechat.MESSAGES.range_list + Voicechat.MESSAGES.range_list_desc);
        player.sendMessage(Voicechat.MESSAGES.range_global + Voicechat.MESSAGES.range_global_desc);
        player.sendMessage(Voicechat.MESSAGES.range_volume + Voicechat.MESSAGES.range_volume_desc);
        player.sendMessage(Voicechat.MESSAGES.range_priority + Voicechat.MESSAGES.range_priority_desc);
        player.sendMessage(Voicechat.MESSAGES.range_cooldown + Voicechat.MESSAGES.range_cooldown_desc);
        player.sendMessage(Voicechat.MESSAGES.range_reload + Voicechat.MESSAGES.range_reload_desc);
    }

    private boolean handleSet(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Voicechat.MESSAGES.range_uso_set);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
            return true;
        }

        float distance;
        try {
            distance = Float.parseFloat(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Voicechat.MESSAGES.range_valor_invalido);
            return true;
        }

        if (distance <= 0 || distance > 1000) {
            player.sendMessage(Voicechat.MESSAGES.range_valor_invalido);
            return true;
        }

        Voicechat.playerRangeManager.setRange(target.getUniqueId(), distance);
        player.sendMessage(String.format(Voicechat.MESSAGES.range_definido, target.getName(), String.valueOf(distance)));
        Voicechat.activityLogger.logRangeSet(player.getName(), target.getName(), distance);
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Voicechat.MESSAGES.range_uso_remove);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
            return true;
        }

        if (Voicechat.playerRangeManager.removeRange(target.getUniqueId())) {
            player.sendMessage(String.format(Voicechat.MESSAGES.range_removido, target.getName()));
            Voicechat.activityLogger.logRangeRemoved(player.getName(), target.getName());
        } else {
            player.sendMessage(String.format(Voicechat.MESSAGES.range_sem_custom, target.getName()));
        }
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
                return true;
            }
        } else {
            target = player;
        }

        Float customRange = Voicechat.playerRangeManager.getRange(target.getUniqueId());
        float defaultRange = de.maxhenkel.voicechat.voice.common.Utils.getDefaultDistance();

        if (customRange != null) {
            player.sendMessage(String.format(Voicechat.MESSAGES.range_info_jogador, target.getName(), String.valueOf(customRange)));
        } else {
            player.sendMessage(String.format(Voicechat.MESSAGES.range_info_padrao, target.getName(), String.valueOf(defaultRange)));
        }
        return true;
    }

    private boolean handleList(Player player) {
        Map<UUID, Float> allRanges = Voicechat.playerRangeManager.getAllRanges();
        if (allRanges.isEmpty()) {
            player.sendMessage(Voicechat.MESSAGES.range_lista_vazia);
            return true;
        }

        player.sendMessage(Voicechat.MESSAGES.range_lista_titulo);
        List<Map.Entry<UUID, Float>> entries = new ArrayList<>(allRanges.entrySet());
        entries.sort(Comparator.comparing(entry -> resolvePlayerName(entry.getKey()), String.CASE_INSENSITIVE_ORDER));
        for (Map.Entry<UUID, Float> entry : entries) {
            Player target = Bukkit.getPlayer(entry.getKey());
            String name = target != null ? target.getName() : entry.getKey().toString();
            player.sendMessage(String.format(Voicechat.MESSAGES.range_lista_item, name, String.valueOf(entry.getValue())));
        }
        return true;
    }

    private boolean handleReload(Player player) {
        Voicechat.playerRangeManager.load();
        player.sendMessage(Voicechat.MESSAGES.range_recarregado);
        return true;
    }

    private boolean handleGlobal(Player player, String[] args) {
        if (!player.hasPermission(PermissionManager.GLOBAL_ADMIN_PERMISSION)) {
            player.sendMessage(Voicechat.MESSAGES.global_sem_permissao);
            return true;
        }

        if (args.length < 2) {
            RangeGlobalMenu.open(player);
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "menu":
                RangeGlobalMenu.open(player);
                return true;
            case "add":
                if (args.length < 3) {
                    player.sendMessage(Voicechat.MESSAGES.range_global_uso_add);
                    return true;
                }
                Player addTarget = Bukkit.getPlayerExact(args[2]);
                if (addTarget == null) {
                    player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
                    return true;
                }
                if (Voicechat.playerRangeManager.isGlobalLimitReached()) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.global_limite_atingido, Voicechat.playerRangeManager.getMaxGlobalPlayers()));
                    return true;
                }
                if (Voicechat.playerRangeManager.addGlobalPlayer(addTarget.getUniqueId())) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.range_global_adicionado, addTarget.getName()));
                    Voicechat.activityLogger.logGlobalAdded(player.getName(), addTarget.getName());
                } else {
                    player.sendMessage(String.format(Voicechat.MESSAGES.range_global_ja_global, addTarget.getName()));
                }
                return true;
            case "remove":
                if (args.length < 3) {
                    player.sendMessage(Voicechat.MESSAGES.range_global_uso_remove);
                    return true;
                }
                Player removeTarget = Bukkit.getPlayerExact(args[2]);
                if (removeTarget == null) {
                    player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
                    return true;
                }
                if (Voicechat.playerRangeManager.removeGlobalPlayer(removeTarget.getUniqueId())) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.range_global_removido, removeTarget.getName()));
                    Voicechat.activityLogger.logGlobalRemoved(player.getName(), removeTarget.getName());
                } else {
                    player.sendMessage(String.format(Voicechat.MESSAGES.range_global_nao_global, removeTarget.getName()));
                }
                return true;
            case "list":
                Set<UUID> globals = Voicechat.playerRangeManager.getGlobalPlayers();
                if (globals.isEmpty()) {
                    player.sendMessage(Voicechat.MESSAGES.range_global_lista_vazia);
                    return true;
                }
                player.sendMessage(Voicechat.MESSAGES.range_global_lista_titulo);
                List<UUID> sortedGlobals = new ArrayList<>(globals);
                sortedGlobals.sort(Comparator.comparing(VoiceRangeCommands::resolvePlayerName, String.CASE_INSENSITIVE_ORDER));
                for (UUID uuid : sortedGlobals) {
                    Player p = Bukkit.getPlayer(uuid);
                    String name = p != null ? p.getName() : uuid.toString();
                    player.sendMessage(String.format(Voicechat.MESSAGES.range_global_lista_item, name));
                }
                return true;
            default:
                RangeGlobalMenu.open(player);
                return true;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return tabComplete(args[0], Arrays.asList("menu", "set", "remove", "info", "list", "global", "volume", "priority", "cooldown", "reload"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("remove") || sub.equals("info") || sub.equals("volume") || sub.equals("priority")) {
                return tabComplete(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            }
            if (sub.equals("global")) {
                return tabComplete(args[1], Arrays.asList("menu", "add", "remove", "list"));
            }
            if (sub.equals("cooldown")) {
                return tabComplete(args[1], Arrays.asList("set", "off", "info"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                return tabComplete(args[2], Arrays.asList("48", "64", "96", "128", "200", "300", "500"));
            }
            if (args[0].equalsIgnoreCase("volume")) {
                return tabComplete(args[2], Arrays.asList("0.25", "0.5", "0.75", "1.0", "1.5", "2.0", "3.0", "reset"));
            }
            if (args[0].equalsIgnoreCase("priority")) {
                return tabComplete(args[2], Arrays.asList("0", "1", "2", "3", "5", "10"));
            }
            if (args[0].equalsIgnoreCase("global") && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
                return tabComplete(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            }
            if (args[0].equalsIgnoreCase("cooldown") && args[1].equalsIgnoreCase("set")) {
                return tabComplete(args[2], Arrays.asList("30", "60", "120", "300"));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("cooldown") && args[1].equalsIgnoreCase("set")) {
                return tabComplete(args[3], Arrays.asList("5", "10", "15", "30"));
            }
        }
        return Collections.emptyList();
    }

    private List<String> tabComplete(String arg, List<String> options) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(arg.toLowerCase())).collect(Collectors.toList());
    }

    private static String resolvePlayerName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
        return offlineName != null ? offlineName : uuid.toString();
    }

    // === Volume Command ===

    private boolean handleVolume(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Voicechat.MESSAGES.volume_uso);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
            return true;
        }

        if (args[2].equalsIgnoreCase("reset")) {
            if (Voicechat.playerRangeManager.removeVolume(target.getUniqueId())) {
                player.sendMessage(String.format(Voicechat.MESSAGES.volume_removido, target.getName()));
                Voicechat.activityLogger.log(player.getName() + " removeu volume customizado de " + target.getName());
            } else {
                player.sendMessage(String.format(Voicechat.MESSAGES.volume_sem_custom, target.getName()));
            }
            return true;
        }

        float volume;
        try {
            volume = Float.parseFloat(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Voicechat.MESSAGES.volume_valor_invalido);
            return true;
        }

        if (volume < 0.1f || volume > 5.0f) {
            player.sendMessage(Voicechat.MESSAGES.volume_valor_invalido);
            return true;
        }

        Voicechat.playerRangeManager.setVolume(target.getUniqueId(), volume);
        player.sendMessage(String.format(Voicechat.MESSAGES.volume_definido, target.getName(), String.format("%.2f", volume)));
        Voicechat.activityLogger.log(player.getName() + " definiu volume de " + target.getName() + " para " + String.format("%.2f", volume));
        return true;
    }

    // === Priority Command ===

    private boolean handlePriority(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Voicechat.MESSAGES.priority_uso);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
            return true;
        }

        int priority;
        try {
            priority = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(Voicechat.MESSAGES.priority_valor_invalido);
            return true;
        }

        if (priority < 0 || priority > 100) {
            player.sendMessage(Voicechat.MESSAGES.priority_valor_invalido);
            return true;
        }

        Voicechat.playerRangeManager.setPriority(target.getUniqueId(), priority);
        player.sendMessage(String.format(Voicechat.MESSAGES.priority_definido, target.getName(), priority));
        Voicechat.activityLogger.log(player.getName() + " definiu prioridade de " + target.getName() + " para " + priority);
        return true;
    }

    // === Cooldown Command ===

    private boolean handleCooldown(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Voicechat.MESSAGES.cooldown_uso);
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "set":
                if (args.length < 4) {
                    player.sendMessage(Voicechat.MESSAGES.cooldown_uso_set);
                    return true;
                }
                try {
                    long maxTalk = Long.parseLong(args[2]);
                    long cooldown = Long.parseLong(args[3]);
                    if (maxTalk <= 0 || cooldown <= 0) {
                        player.sendMessage(Voicechat.MESSAGES.cooldown_valor_invalido);
                        return true;
                    }
                    Voicechat.voiceCooldownManager.setMaxTalkTime(maxTalk);
                    Voicechat.voiceCooldownManager.setCooldown(cooldown);
                    Voicechat.persistGlobalCooldownSettings();
                    player.sendMessage(String.format(Voicechat.MESSAGES.cooldown_definido, maxTalk, cooldown));
                    Voicechat.activityLogger.log(player.getName() + " definiu cooldown: fala=" + maxTalk + "s, espera=" + cooldown + "s");
                } catch (NumberFormatException e) {
                    player.sendMessage(Voicechat.MESSAGES.cooldown_valor_invalido);
                }
                return true;
            case "off":
                Voicechat.voiceCooldownManager.setMaxTalkTime(0);
                Voicechat.voiceCooldownManager.setCooldown(0);
                Voicechat.persistGlobalCooldownSettings();
                player.sendMessage(Voicechat.MESSAGES.cooldown_desativado);
                Voicechat.activityLogger.log(player.getName() + " desativou o cooldown de voz");
                return true;
            case "info":
                if (Voicechat.voiceCooldownManager.isEnabled()) {
                    player.sendMessage(String.format(Voicechat.MESSAGES.cooldown_info,
                            Voicechat.voiceCooldownManager.getMaxTalkTimeMs() / 1000,
                            Voicechat.voiceCooldownManager.getCooldownMs() / 1000));
                } else {
                    player.sendMessage(Voicechat.MESSAGES.cooldown_desativado_info);
                }
                return true;
            default:
                player.sendMessage(Voicechat.MESSAGES.cooldown_uso);
                return true;
        }
    }

}
