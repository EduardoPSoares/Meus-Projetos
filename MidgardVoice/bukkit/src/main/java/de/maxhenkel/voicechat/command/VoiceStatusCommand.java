package de.maxhenkel.voicechat.command;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.permission.PermissionManager;
import de.maxhenkel.voicechat.voice.common.Utils;
import de.maxhenkel.voicechat.zone.RestrictedZone;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VoiceStatusCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Voicechat.MESSAGES.somente_jogadores);
            return true;
        }

        Player player = (Player) sender;
        Player target;

        if (args.length >= 1) {
            if (!player.hasPermission(PermissionManager.ADMIN_PERMISSION)) {
                player.sendMessage(Voicechat.MESSAGES.sem_permissao);
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage(Voicechat.MESSAGES.jogador_nao_encontrado);
                return true;
            }
        } else {
            target = player;
        }

        showStatus(player, target);
        return true;
    }

    private void showStatus(Player player, Player target) {
        player.sendMessage(String.format(Voicechat.MESSAGES.status_titulo, target.getName()));

        // Range
        float defaultRange = Utils.getDefaultDistance();
        Float customRange = Voicechat.playerRangeManager != null
                ? Voicechat.playerRangeManager.getRange(target.getUniqueId())
                : null;

        if (customRange != null) {
            player.sendMessage(String.format(Voicechat.MESSAGES.status_range_custom, String.valueOf(customRange)));
        } else {
            player.sendMessage(String.format(Voicechat.MESSAGES.status_range, String.valueOf(defaultRange)));
        }

        // Global
        boolean isGlobal = Voicechat.playerRangeManager != null
                && Voicechat.playerRangeManager.isGlobalPlayer(target.getUniqueId());
        player.sendMessage(isGlobal ? Voicechat.MESSAGES.status_global_sim : Voicechat.MESSAGES.status_global_nao);

        // Global players count
        if (Voicechat.playerRangeManager != null) {
            int globalCount = Voicechat.playerRangeManager.getGlobalPlayers().size();
            int maxGlobal = Voicechat.playerRangeManager.getMaxGlobalPlayers();
            String limitStr = maxGlobal <= 0 ? Voicechat.MESSAGES.status_ilimitado : String.valueOf(maxGlobal);
            player.sendMessage(String.format(Voicechat.MESSAGES.status_global_total, globalCount, limitStr));
        }

        // Zone
        String zoneName = null;
        if (Voicechat.zoneNotificationListener != null) {
            zoneName = Voicechat.zoneNotificationListener.getCurrentZone(target.getUniqueId());
        }
        if (zoneName != null) {
            RestrictedZone zone = Voicechat.restrictedZoneManager.getZone(zoneName);
            String voiceStatus;
            if (zone != null && zone.isStageMode()) {
                voiceStatus = Voicechat.MESSAGES.text("status.zona_stage_mode", "&d* stage mode");
            } else if (zone != null && zone.isVoiceEnabled()) {
                voiceStatus = Voicechat.MESSAGES.status_zona_voz_ativada;
            } else {
                voiceStatus = Voicechat.MESSAGES.status_zona_voz_desativada;
            }
            player.sendMessage(String.format(Voicechat.MESSAGES.status_zona_em, zoneName, voiceStatus));
        } else {
            player.sendMessage(Voicechat.MESSAGES.status_zona_nenhuma);
        }

        // Volume
        if (Voicechat.playerRangeManager != null) {
            Float volume = Voicechat.playerRangeManager.getVolume(target.getUniqueId());
            if (volume != null) {
                player.sendMessage(String.format(
                        Voicechat.MESSAGES.text("status.volume", "&7Volume: &a%s"),
                        String.format("%.2fx", volume)
                ));
            }
        }

        // Priority
        if (Voicechat.playerRangeManager != null) {
            int priority = Voicechat.playerRangeManager.getPriority(target.getUniqueId());
            if (priority > 0) {
                player.sendMessage(String.format(
                        Voicechat.MESSAGES.text("status.prioridade", "&7Prioridade: &e%s"),
                        priority
                ));
            }
        }

        // Cooldown
        if (Voicechat.voiceCooldownManager != null && Voicechat.voiceCooldownManager.isEnabled()) {
            long remaining = Voicechat.voiceCooldownManager.getRemainingCooldown(target.getUniqueId());
            if (remaining > 0) {
                player.sendMessage(String.format(
                        Voicechat.MESSAGES.text("status.cooldown_restante", "&7Cooldown: &c%ss restantes"),
                        remaining
                ));
            } else {
                long talkRemain = Voicechat.voiceCooldownManager.getRemainingTalkTime(target.getUniqueId());
                player.sendMessage(String.format(
                        Voicechat.MESSAGES.text("status.tempo_fala_restante", "&7Tempo de fala: &a%ss restantes"),
                        talkRemain
                ));
            }
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(PermissionManager.ADMIN_PERMISSION)) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}
