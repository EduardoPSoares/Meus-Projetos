package de.maxhenkel.voicechat.command;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.gui.AdminHubMenu;
import de.maxhenkel.voicechat.permission.PermissionManager;
import de.maxhenkel.voicechat.range.VoiceRangeCommands;
import de.maxhenkel.voicechat.recording.RecordingCommands;
import de.maxhenkel.voicechat.recording.VoiceRecording;
import de.maxhenkel.voicechat.zone.RestrictedZoneCommands;
import de.maxhenkel.voicechat.zone.RestrictedZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class MvoiceCommand implements CommandExecutor, TabCompleter {

    private final VoiceChatCommands voiceChatCommands;
    private final RestrictedZoneCommands zoneCommands;
    private final VoiceRangeCommands rangeCommands;
    private final VoiceStatusCommand statusCommand;
    private final RecordingCommands recordingCommands;

    public MvoiceCommand() {
        this.voiceChatCommands = new VoiceChatCommands();
        this.zoneCommands = new RestrictedZoneCommands();
        this.rangeCommands = new VoiceRangeCommands();
        this.statusCommand = new VoiceStatusCommand();
        this.recordingCommands = new RecordingCommands();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasRootAccess(sender)) {
            sender.sendMessage(Voicechat.MESSAGES.sem_permissao);
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);

        switch (sub) {
            // Original voicechat subcommands - pass args directly (args[0] is the sub)
            case "help":
            case "test":
            case "invite":
            case "join":
            case "leave":
            case "reload":
                return voiceChatCommands.onCommand(sender, command, label, args);

            // Zone commands - strip "zone" prefix
            case "zone":
                return zoneCommands.onCommand(sender, command, label, remaining);

            // Zone shortcuts - pos1, pos2, create work directly without "zone" prefix
            case "pos1":
            case "pos2":
                return zoneCommands.onCommand(sender, command, label, new String[]{sub});
            case "create":
                return zoneCommands.onCommand(sender, command, label, args);

            // Range commands - strip "range" prefix
            case "range":
                return rangeCommands.onCommand(sender, command, label, remaining);

            // Global - delegate to range with "global" as first arg
            case "global":
                return rangeCommands.onCommand(sender, command, label, prependArg("global", remaining));

            // Volume - delegate to range with "volume" as first arg
            case "volume":
                return rangeCommands.onCommand(sender, command, label, prependArg("volume", remaining));

            // Priority - delegate to range with "priority" as first arg
            case "priority":
                return rangeCommands.onCommand(sender, command, label, prependArg("priority", remaining));

            // Cooldown - delegate to range with "cooldown" as first arg
            case "cooldown":
                return rangeCommands.onCommand(sender, command, label, prependArg("cooldown", remaining));

            // Status - strip "status" prefix
            case "status":
                return statusCommand.onCommand(sender, command, label, remaining);

            // Recording - strip "record" prefix
            case "record":
                return recordingCommands.onCommand(sender, command, label, remaining);

            // Admin hub
            case "admin":
                return handleAdmin(sender);

            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleAdmin(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Voicechat.MESSAGES.somente_jogadores);
            return true;
        }
        Player p = (Player) sender;
        if (!p.hasPermission("voicechat.admin.hub")) {
            p.sendMessage(Voicechat.MESSAGES.sem_permissao);
            return true;
        }
        AdminHubMenu.open(p);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        for (String line : Voicechat.MESSAGES.textList(
                "commands.mvoice.help_lines",
                "&6&l=== MidgardVoice ===",
                "&e/mvoice help &7- Ajuda do voicechat",
                "&e/mvoice test <jogador> &7- Testar conexao",
                "&e/mvoice invite <jogador> &7- Convidar para grupo",
                "&e/mvoice join <grupo> [senha] &7- Entrar em grupo",
                "&e/mvoice leave &7- Sair do grupo",
                "&e/mvoice status [jogador] &7- Ver status de voz",
                "&e/mvoice pos1 &7- Definir posicao 1 da zona",
                "&e/mvoice pos2 &7- Definir posicao 2 da zona",
                "&e/mvoice create <nome> [min] &7- Criar zona",
                "&e/mvoice zone <sub> &7- Gerenciar zonas",
                "&e/mvoice range <sub> &7- Gerenciar range",
                "&e/mvoice global <sub> &7- Gerenciar voz global",
                "&e/mvoice volume <jogador> <valor> &7- Ajustar volume",
                "&e/mvoice priority <jogador> <valor> &7- Ajustar prioridade",
                "&e/mvoice cooldown <sub> &7- Gerenciar cooldown",
                "&e/mvoice record <sub> &7- Gravacao de voz",
                "&e/mvoice admin &7- Painel de administracao",
                "&e/mvoice reload &7- Recarregar configuracoes")) {
            sender.sendMessage(line);
        }
    }

    // ============ TAB COMPLETION ============

    @Nullable
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!hasRootAccess(sender)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return tabCompleteRoot(sender, args[0]);
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "test":
            case "invite":
                return tabCompletePlayerArg(args, 2);

            case "join":
                return Collections.emptyList();

            case "status":
                if (args.length == 2 && sender.hasPermission(PermissionManager.ADMIN_PERMISSION)) {
                    return filterStartsWith(args[1], getOnlinePlayerNames());
                }
                return Collections.emptyList();

            case "zone":
                return tabCompleteZone(args);

            case "create":
                if (args.length == 2) {
                    return filterStartsWith(args[1], Collections.singletonList("<nome>"));
                }
                if (args.length == 3) {
                    return filterStartsWith(args[2], Arrays.asList("5", "10", "30", "60"));
                }
                return Collections.emptyList();

            case "range":
                return tabCompleteRange(args);

            case "global":
                return tabCompleteGlobal(args);

            case "volume":
                return tabCompleteVolume(args);

            case "priority":
                return tabCompletePriority(args);

            case "cooldown":
                return tabCompleteCooldown(args);

            case "record":
                return tabCompleteRecord(sender, args);
        }

        return Collections.emptyList();
    }

    // --- Root level ---

    private List<String> tabCompleteRoot(CommandSender sender, String arg) {
        List<String> subs = new ArrayList<>(Arrays.asList("help", "invite", "join", "leave", "status"));

        if (sender.hasPermission(PermissionManager.ADMIN_PERMISSION)) {
            subs.addAll(Arrays.asList("test", "reload", "admin"));
        }
        if (sender.hasPermission(RestrictedZoneManager.ZONE_ADMIN_PERMISSION)) {
            subs.addAll(Arrays.asList("zone", "pos1", "pos2", "create"));
        }
        if (sender.hasPermission(PermissionManager.RANGE_ADMIN_PERMISSION)) {
            subs.addAll(Arrays.asList("range", "volume", "priority", "cooldown"));
        }
        if (sender.hasPermission(PermissionManager.GLOBAL_ADMIN_PERMISSION)) {
            subs.add("global");
        }
        if (sender.hasPermission(RecordingCommands.RECORDING_PERMISSION)) {
            subs.add("record");
        }

        return filterStartsWith(arg, subs);
    }

    // --- test / invite ---

    private List<String> tabCompletePlayerArg(String[] args, int playerArgIndex) {
        if (args.length == playerArgIndex) {
            return filterStartsWith(args[playerArgIndex - 1], getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }

    // --- zone ---
    // /mvoice zone [menu|pos1|pos2|create|reload]
    // /mvoice zone create <nome> [minutos]

    private List<String> tabCompleteZone(String[] args) {
        if (args.length == 2) {
            return filterStartsWith(args[1], Arrays.asList("menu", "pos1", "pos2", "create", "reload"));
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("create")) {
            return filterStartsWith(args[2], Collections.singletonList("<nome>"));
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("create")) {
            return filterStartsWith(args[3], Arrays.asList("5", "10", "30", "60"));
        }
        return Collections.emptyList();
    }

    // --- range ---
    // /mvoice range [menu|set|remove|info|list|reload]
    // /mvoice range set <player> <distance>
    // /mvoice range remove <player>
    // /mvoice range info [player]

    private List<String> tabCompleteRange(String[] args) {
        if (args.length == 2) {
            return filterStartsWith(args[1], Arrays.asList("menu", "set", "remove", "info", "list", "reload"));
        }
        if (args.length == 3) {
            String rangeSub = args[1].toLowerCase();
            if (rangeSub.equals("set") || rangeSub.equals("remove") || rangeSub.equals("info")) {
                return filterStartsWith(args[2], getOnlinePlayerNames());
            }
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
            return filterStartsWith(args[3], Arrays.asList("48", "64", "96", "128", "200", "300", "500"));
        }
        return Collections.emptyList();
    }

    // --- global ---
    // /mvoice global [menu|add|remove|list]
    // /mvoice global add <player>
    // /mvoice global remove <player>

    private List<String> tabCompleteGlobal(String[] args) {
        if (args.length == 2) {
            return filterStartsWith(args[1], Arrays.asList("menu", "add", "remove", "list"));
        }
        if (args.length == 3) {
            String globalSub = args[1].toLowerCase();
            if (globalSub.equals("add") || globalSub.equals("remove")) {
                return filterStartsWith(args[2], getOnlinePlayerNames());
            }
        }
        return Collections.emptyList();
    }

    // --- volume ---
    // /mvoice volume <player> <value|reset>

    private List<String> tabCompleteVolume(String[] args) {
        if (args.length == 2) {
            return filterStartsWith(args[1], getOnlinePlayerNames());
        }
        if (args.length == 3) {
            return filterStartsWith(args[2], Arrays.asList("0.25", "0.5", "0.75", "1.0", "1.5", "2.0", "3.0", "reset"));
        }
        return Collections.emptyList();
    }

    // --- priority ---
    // /mvoice priority <player> <value>

    private List<String> tabCompletePriority(String[] args) {
        if (args.length == 2) {
            return filterStartsWith(args[1], getOnlinePlayerNames());
        }
        if (args.length == 3) {
            return filterStartsWith(args[2], Arrays.asList("0", "1", "2", "3", "5", "10"));
        }
        return Collections.emptyList();
    }

    // --- cooldown ---
    // /mvoice cooldown [set|off|info]
    // /mvoice cooldown set <maxTalkSeconds> <cooldownSeconds>

    private List<String> tabCompleteCooldown(String[] args) {
        if (args.length == 2) {
            return filterStartsWith(args[1], Arrays.asList("set", "off", "info"));
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("set")) {
            return filterStartsWith(args[2], Arrays.asList("30", "60", "120", "300"));
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
            return filterStartsWith(args[3], Arrays.asList("5", "10", "15", "30"));
        }
        return Collections.emptyList();
    }

    // --- record ---
    // /mvoice record [start|stop|active|list|info|delete]
    // /mvoice record start <player>
    // /mvoice record stop <player>
    // /mvoice record info <id>
    // /mvoice record delete <id>
    // /mvoice record list [page]

    private List<String> tabCompleteRecord(CommandSender sender, String[] args) {
        if (!sender.hasPermission(RecordingCommands.RECORDING_PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return filterStartsWith(args[1], Arrays.asList("start", "stop", "active", "list", "info", "delete"));
        }
        if (args.length == 3) {
            String recSub = args[1].toLowerCase();
            switch (recSub) {
                case "start":
                    return filterStartsWith(args[2], getOnlinePlayerNames());
                case "stop":
                    if (Voicechat.voiceRecordingManager != null) {
                        return filterStartsWith(args[2],
                                Voicechat.voiceRecordingManager.getActiveRecordings().values().stream()
                                        .map(VoiceRecording::getTargetName)
                                        .collect(Collectors.toList()));
                    }
                    return Collections.emptyList();
                case "info":
                case "delete":
                    if (Voicechat.voiceRecordingManager != null) {
                        return filterStartsWith(args[2], Voicechat.voiceRecordingManager.getSavedRecordings());
                    }
                    return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    // ============ UTILITIES ============

    private String[] prependArg(String prefix, String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = prefix;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    private boolean hasRootAccess(CommandSender sender) {
        return sender.hasPermission(PermissionManager.ADMIN_PERMISSION);
    }

    private List<String> filterStartsWith(String arg, List<String> options) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}
