package de.maxhenkel.voicechat.recording;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RecordingCommands implements CommandExecutor, TabCompleter {

    public static final String RECORDING_PERMISSION = "voicechat.recording.admin";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Voicechat.MESSAGES.somente_jogadores);
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission(RECORDING_PERMISSION)) {
            player.sendMessage(Voicechat.MESSAGES.sem_permissao);
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start":
                handleStart(player, args);
                break;
            case "stop":
                handleStop(player, args);
                break;
            case "list":
                handleList(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "delete":
                handleDelete(player, args);
                break;
            case "active":
                handleActive(player);
                break;
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Voicechat.MESSAGES.rec_titulo);
        player.sendMessage(Voicechat.MESSAGES.rec_start + Voicechat.MESSAGES.rec_start_desc);
        player.sendMessage(Voicechat.MESSAGES.rec_stop + Voicechat.MESSAGES.rec_stop_desc);
        player.sendMessage(Voicechat.MESSAGES.rec_active + Voicechat.MESSAGES.rec_active_desc);
        player.sendMessage(Voicechat.MESSAGES.rec_list + Voicechat.MESSAGES.rec_list_desc);
        player.sendMessage(Voicechat.MESSAGES.rec_info + Voicechat.MESSAGES.rec_info_desc);
        player.sendMessage(Voicechat.MESSAGES.rec_delete + Voicechat.MESSAGES.rec_delete_desc);
    }

    private void handleStart(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Voicechat.MESSAGES.rec_uso_start);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
            return;
        }

        if (Voicechat.voiceRecordingManager.isRecording(target.getUniqueId())) {
            player.sendMessage(Voicechat.MESSAGES.prefix + String.format(Voicechat.MESSAGES.rec_ja_gravando, target.getName()));
            return;
        }

        VoiceRecording recording = Voicechat.voiceRecordingManager.startRecording(
                target.getUniqueId(), target.getName(),
                player.getUniqueId(), player.getName()
        );

        player.sendMessage(Voicechat.MESSAGES.prefix + String.format(Voicechat.MESSAGES.rec_iniciada, target.getName()));
        player.sendMessage(Voicechat.MESSAGES.prefix + String.format(Voicechat.MESSAGES.rec_id, recording.getId()));
    }

    private void handleStop(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Voicechat.MESSAGES.rec_uso_stop);
            return;
        }

        UUID targetUuid = resolveRecordingTarget(args[1]);
        if (targetUuid == null) {
            player.sendMessage(Voicechat.MESSAGES.prefix + String.format(Voicechat.MESSAGES.rec_nao_gravando, args[1]));
            return;
        }

        VoiceRecording activeRecording = Voicechat.voiceRecordingManager.getActiveRecording(targetUuid);
        if (activeRecording == null) {
            player.sendMessage(Voicechat.MESSAGES.prefix + String.format(Voicechat.MESSAGES.rec_nao_gravando, args[1]));
            return;
        }

        VoiceRecording recording = Voicechat.voiceRecordingManager.stopRecording(targetUuid);
        if (recording != null) {
            player.sendMessage(Voicechat.MESSAGES.prefix + String.format(Voicechat.MESSAGES.rec_parada, recording.getTargetName()));
            player.sendMessage(Voicechat.MESSAGES.prefix + String.format(Voicechat.MESSAGES.rec_salva, 
                    recording.getId(), recording.getFormattedDuration(), recording.getFrameCount()));
        }
    }

    private UUID resolveRecordingTarget(String query) {
        Player onlinePlayer = Bukkit.getPlayer(query);
        if (onlinePlayer != null && Voicechat.voiceRecordingManager.isRecording(onlinePlayer.getUniqueId())) {
            return onlinePlayer.getUniqueId();
        }

        try {
            UUID uuid = UUID.fromString(query);
            if (Voicechat.voiceRecordingManager.isRecording(uuid)) {
                return uuid;
            }
        } catch (IllegalArgumentException ignored) {
        }

        return Voicechat.voiceRecordingManager.getActiveRecordings().values().stream()
                .sorted(Comparator.comparing(VoiceRecording::getTargetName, String.CASE_INSENSITIVE_ORDER))
                .filter(recording -> recording.getTargetName().equalsIgnoreCase(query))
                .map(VoiceRecording::getTargetPlayer)
                .findFirst()
                .orElse(null);
    }

    private void handleActive(Player player) {
        Map<UUID, VoiceRecording> active = Voicechat.voiceRecordingManager.getActiveRecordings();
        if (active.isEmpty()) {
            player.sendMessage(Voicechat.MESSAGES.prefix + Voicechat.MESSAGES.rec_nenhuma_ativa);
            return;
        }

        player.sendMessage(Voicechat.MESSAGES.rec_ativas_titulo);
        List<VoiceRecording> recordings = new ArrayList<>(active.values());
        recordings.sort(Comparator.comparing(VoiceRecording::getTargetName, String.CASE_INSENSITIVE_ORDER));
        for (VoiceRecording rec : recordings) {
            player.sendMessage(String.format(Voicechat.MESSAGES.rec_ativa_item,
                    rec.getTargetName(), rec.getFormattedDuration(), rec.getFrameCount()));
        }
    }

    private void handleList(Player player, String[] args) {
        List<String> saved = Voicechat.voiceRecordingManager.getSavedRecordings();
        if (saved.isEmpty()) {
            player.sendMessage(Voicechat.MESSAGES.prefix + Voicechat.MESSAGES.rec_nenhuma_salva);
            return;
        }

        int page = 0;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException ignored) {
            }
        }

        int perPage = 8;
        int totalPages = (saved.size() + perPage - 1) / perPage;
        page = Math.max(0, Math.min(page, totalPages - 1));

        player.sendMessage(String.format(Voicechat.MESSAGES.rec_lista_titulo, page + 1, totalPages));
        int start = page * perPage;
        int end = Math.min(start + perPage, saved.size());
        for (int i = start; i < end; i++) {
            player.sendMessage(String.format(Voicechat.MESSAGES.rec_lista_item, saved.get(i)));
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Voicechat.MESSAGES.rec_uso_info);
            return;
        }

        String id = args[1];
        String info = Voicechat.voiceRecordingManager.getRecordingInfo(id);
        if (info == null) {
            player.sendMessage(Voicechat.MESSAGES.prefix + Voicechat.MESSAGES.rec_nao_encontrada);
            return;
        }

        player.sendMessage(Voicechat.MESSAGES.rec_info_titulo);
        for (String line : info.split("\n")) {
            if (!line.trim().isEmpty()) {
                player.sendMessage(Voicechat.MESSAGES.format("commands.recording.info_line", "&7%s", line));
            }
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Voicechat.MESSAGES.rec_uso_delete);
            return;
        }

        String id = args[1];
        if (Voicechat.voiceRecordingManager.deleteSavedRecording(id)) {
            player.sendMessage(Voicechat.MESSAGES.prefix + String.format(Voicechat.MESSAGES.rec_deletada, id));
        } else {
            player.sendMessage(Voicechat.MESSAGES.prefix + Voicechat.MESSAGES.rec_nao_encontrada);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(RECORDING_PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = Arrays.asList("start", "stop", "active", "list", "info", "delete");
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "start":
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "stop":
                    return Voicechat.voiceRecordingManager.getActiveRecordings().values().stream()
                            .sorted(Comparator.comparing(VoiceRecording::getTargetName, String.CASE_INSENSITIVE_ORDER))
                            .map(VoiceRecording::getTargetName)
                            .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "info":
                case "delete":
                    return Voicechat.voiceRecordingManager.getSavedRecordings().stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
