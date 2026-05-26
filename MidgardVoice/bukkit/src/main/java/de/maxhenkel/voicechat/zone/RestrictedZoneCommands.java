package de.maxhenkel.voicechat.zone;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.zone.gui.ZoneListMenu;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class RestrictedZoneCommands implements CommandExecutor, TabCompleter {

    private final Map<UUID, Location> pos1Selections = new HashMap<>();
    private final Map<UUID, Location> pos2Selections = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Voicechat.MESSAGES.somente_jogadores);
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission(RestrictedZoneManager.ZONE_ADMIN_PERMISSION)) {
            player.sendMessage(Voicechat.MESSAGES.zona_sem_permissao_gerenciar);
            return true;
        }

        if (args.length < 1) {
            ZoneListMenu.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "menu":
                ZoneListMenu.open(player);
                return true;
            case "pos1":
                return pos1Command(player);
            case "pos2":
                return pos2Command(player);
            case "create":
                return createCommand(player, args);
            case "reload":
                return reloadCommand(player);
            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendHelp(Player player) {
        for (String line : Voicechat.MESSAGES.textList(
                "commands.zone.help_lines",
                Voicechat.MESSAGES.zona_titulo,
                Voicechat.MESSAGES.zona_menu + Voicechat.MESSAGES.zona_menu_desc,
                "&e/mvoice zone menu" + Voicechat.MESSAGES.zona_menu_desc,
                Voicechat.MESSAGES.zona_pos1 + Voicechat.MESSAGES.zona_pos1_desc,
                Voicechat.MESSAGES.zona_pos2 + Voicechat.MESSAGES.zona_pos2_desc,
                Voicechat.MESSAGES.zona_create + Voicechat.MESSAGES.zona_create_desc,
                Voicechat.MESSAGES.zona_reload + Voicechat.MESSAGES.zona_reload_desc,
                Voicechat.MESSAGES.zona_bypass_permissao)) {
            player.sendMessage(line);
        }
    }

    private boolean pos1Command(Player player) {
        Location loc = player.getLocation();
        pos1Selections.put(player.getUniqueId(), loc);
        player.sendMessage(String.format(Voicechat.MESSAGES.zona_pos1_definida, formatLocation(loc)));
        // Show particle preview if both positions are set
        Location pos2 = pos2Selections.get(player.getUniqueId());
        if (pos2 != null && Voicechat.zoneParticleVisualizer != null) {
            Voicechat.zoneParticleVisualizer.showSelection(player, loc, pos2);
        }
        return true;
    }

    private boolean pos2Command(Player player) {
        Location loc = player.getLocation();
        pos2Selections.put(player.getUniqueId(), loc);
        player.sendMessage(String.format(Voicechat.MESSAGES.zona_pos2_definida, formatLocation(loc)));
        // Show particle preview if both positions are set
        Location pos1 = pos1Selections.get(player.getUniqueId());
        if (pos1 != null && Voicechat.zoneParticleVisualizer != null) {
            Voicechat.zoneParticleVisualizer.showSelection(player, pos1, loc);
        }
        return true;
    }

    private boolean createCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Voicechat.MESSAGES.zona_uso_create);
            return true;
        }

        String name = args[1];
        if (!name.matches("^[a-zA-Z0-9_-]+$")) {
            player.sendMessage(Voicechat.MESSAGES.zona_nome_invalido);
            return true;
        }

        // Duracao opcional em minutos
        long expiresAt = -1;
        if (args.length >= 3) {
            try {
                int minutes = Integer.parseInt(args[2]);
                if (minutes > 0) {
                    expiresAt = System.currentTimeMillis() + (minutes * 60_000L);
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Voicechat.MESSAGES.text("commands.zone.invalid_duration", "&cDuracao invalida! Use um numero de minutos."));
                return true;
            }
        }

        Location p1 = pos1Selections.get(player.getUniqueId());
        Location p2 = pos2Selections.get(player.getUniqueId());

        if (p1 == null || p2 == null) {
            player.sendMessage(Voicechat.MESSAGES.zona_definir_posicoes);
            return true;
        }

        if (p1.getWorld() == null || p2.getWorld() == null || !p1.getWorld().equals(p2.getWorld())) {
            player.sendMessage(Voicechat.MESSAGES.zona_mesmo_mundo);
            return true;
        }

        RestrictedZone zone = new RestrictedZone(
                name,
                p1.getWorld().getName(),
                p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                p2.getBlockX(), p2.getBlockY(), p2.getBlockZ()
        );

        if (expiresAt > 0) {
            zone.setExpiresAt(expiresAt);
        }

        if (Voicechat.restrictedZoneManager.addZone(zone)) {
            player.sendMessage(String.format(Voicechat.MESSAGES.zona_criada, name));
            if (expiresAt > 0) {
                player.sendMessage(String.format(
                        Voicechat.MESSAGES.text("commands.zone.temporary_created", "&eZona temporaria: expira em %s minutos."),
                        args[2]
                ));
            }
            player.sendMessage(Voicechat.MESSAGES.zona_sem_permissao_bypass);
            Voicechat.activityLogger.logZoneCreated(player.getName(), name);
            pos1Selections.remove(player.getUniqueId());
            pos2Selections.remove(player.getUniqueId());
            // Stop particle selection preview
            if (Voicechat.zoneParticleVisualizer != null) {
                Voicechat.zoneParticleVisualizer.stopViewing(player);
            }
        } else {
            player.sendMessage(Voicechat.MESSAGES.zona_ja_existe);
        }
        return true;
    }

    private boolean reloadCommand(Player player) {
        Voicechat.restrictedZoneManager.load();
        if (Voicechat.globalZoneSettings != null) Voicechat.globalZoneSettings.load();
        player.sendMessage(Voicechat.MESSAGES.zona_recarregada);
        return true;
    }

    private String formatLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " (" + (loc.getWorld() != null ? loc.getWorld().getName() : "?") + ")";
    }

    @Nullable
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return tabComplete(args[0], Arrays.asList("menu", "pos1", "pos2", "create", "reload"));
        }
        return Collections.emptyList();
    }

    private List<String> tabComplete(String arg, List<String> options) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(arg.toLowerCase())).collect(Collectors.toList());
    }

}
