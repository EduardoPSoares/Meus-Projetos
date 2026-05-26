package me.ray.midgardDiscord;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handler dedicado para subcomandos de manutenção (/midgard maintenance).
 * Gerencia: on/off de servidores, agendamento, staff bypass e status.
 */
public class MaintenanceCommand {

    private final MidgardVelocity plugin;
    private final ProxyServer server;
    private final Component PREFIX = Component.empty();

    public MaintenanceCommand(MidgardVelocity plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
    }

    /**
     * Executa o subcomando de manutenção.
     * @param source quem executou o comando
     * @param args argumentos APÓS "maintenance" (ex: args[0] = servidor ou subcomando)
     * @param isConsole se a fonte é o console
     */
    public void execute(CommandSource source, String[] args, boolean isConsole) {
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "addstaff":
                handleAddStaff(source, args, isConsole);
                return;
            case "removestaff":
                handleRemoveStaff(source, args, isConsole);
                return;
            case "liststaff":
                handleListStaff(source);
                return;
            case "status":
                handleStatus(source);
                return;
            default:
                // Trata como nome de servidor: /midgard maintenance <servidor> <on/off> [tempo]
                handleToggle(source, args);
        }
    }

    /**
     * Retorna sugestões de tab-complete para os argumentos de maintenance.
     * @param args argumentos APÓS "maintenance"
     */
    public List<String> suggest(String[] args) {
        if (args.length <= 1) {
            String current = args.length == 0 ? "" : args[0];
            List<String> options = new ArrayList<>();
            options.add("addstaff");
            options.add("removestaff");
            options.add("liststaff");
            options.add("status");
            server.getAllServers().forEach(s -> options.add(s.getServerInfo().getName()));
            return filter(options, current);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("addstaff")) {
                List<String> players = new ArrayList<>();
                server.getAllPlayers().forEach(p -> players.add(p.getUsername()));
                return filter(players, args[1]);
            }
            if (sub.equals("removestaff")) {
                return filter(new ArrayList<>(plugin.getAdminUsers()), args[1]);
            }
            // Assume nome de servidor — sugere on/off
            return filter(List.of("on", "off"), args[1]);
        }

        if (args.length == 3) {
            if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off")) {
                return filter(List.of("1h", "30m", "15m", "5m", "1m", "30s", "10s"), args[2]);
            }
        }

        return List.of();
    }

    /**
     * Retorna as linhas de ajuda do maintenance para o help geral.
     */
    public void sendHelp(CommandSource source) {
        source.sendMessage(Component.text("  /midgard maintenance <servidor> <on/off> [tempo]", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ɢᴇʀᴇɴᴄɪᴀʀ ᴍᴏᴅᴏ ᴅᴇ ᴍᴀɴᴜᴛᴇɴçãᴏ", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard maintenance status", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ᴇxɪʙᴇ sᴇʀᴠɪᴅᴏʀᴇs ᴇᴍ ᴍᴀɴᴜᴛᴇɴçãᴏ ᴇ ᴀɢᴇɴᴅᴀᴍᴇɴᴛᴏs", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard maintenance addstaff <jogador>", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ᴀᴅɪᴄɪᴏɴᴀ sᴛᴀꜰꜰ ᴀᴏ ʙʏᴘᴀss ᴅᴇ ᴍᴀɴᴜᴛᴇɴçãᴏ", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard maintenance removestaff <jogador>", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ʀᴇᴍᴏᴠᴇ sᴛᴀꜰꜰ ᴅᴏ ʙʏᴘᴀss ᴅᴇ ᴍᴀɴᴜᴛᴇɴçãᴏ", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard maintenance liststaff", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ʟɪsᴛᴀ sᴛᴀꜰꜰ ᴄᴏᴍ ʙʏᴘᴀss ᴅᴇ ᴍᴀɴᴜᴛᴇɴçãᴏ", NamedTextColor.GRAY)));
    }

    // ─── Subcomandos ───────────────────────────────────────────────

    private void handleAddStaff(CommandSource source, String[] args, boolean isConsole) {
        if (!source.hasPermission("midgard.admin") && !isConsole) {
            source.sendMessage(PREFIX.append(Component.text("ᴀᴘᴇɴᴀs ᴀᴅᴍɪɴɪsᴛʀᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ɢᴇʀᴇɴᴄɪᴀʀ sᴛᴀꜰꜰ.", NamedTextColor.RED)));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(PREFIX.append(Component.text("Uso: /midgard maintenance addstaff <jogador>", NamedTextColor.RED)));
            return;
        }
        String staffName = args[1];
        if (plugin.isAdmin(staffName)) {
            source.sendMessage(PREFIX.append(Component.text("O jogador ", NamedTextColor.YELLOW)
                    .append(Component.text(staffName, NamedTextColor.YELLOW))
                    .append(Component.text(" já está na lista de staff.", NamedTextColor.YELLOW))));
            return;
        }
        plugin.addAdmin(staffName);
        source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ", NamedTextColor.GREEN)
                .append(Component.text(staffName, NamedTextColor.YELLOW))
                .append(Component.text(" ᴀᴅɪᴄɪᴏɴᴀᴅᴏ à ʟɪsᴛᴀ ᴅᴇ sᴛᴀꜰꜰ ᴅᴀ ᴍᴀɴᴜᴛᴇɴçãᴏ.", NamedTextColor.GREEN))));
        if (plugin.getAuditLogger() != null) {
            String moderator = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
            plugin.getAuditLogger().log(moderator, "MAINTENANCE_ADDSTAFF", staffName, "Added to maintenance bypass list");
        }
    }

    private void handleRemoveStaff(CommandSource source, String[] args, boolean isConsole) {
        if (!source.hasPermission("midgard.admin") && !isConsole) {
            source.sendMessage(PREFIX.append(Component.text("ᴀᴘᴇɴᴀs ᴀᴅᴍɪɴɪsᴛʀᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ɢᴇʀᴇɴᴄɪᴀʀ sᴛᴀꜰꜰ.", NamedTextColor.RED)));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(PREFIX.append(Component.text("Uso: /midgard maintenance removestaff <jogador>", NamedTextColor.RED)));
            return;
        }
        String staffName = args[1];
        if (!plugin.isAdmin(staffName)) {
            source.sendMessage(PREFIX.append(Component.text("O jogador ", NamedTextColor.YELLOW)
                    .append(Component.text(staffName, NamedTextColor.YELLOW))
                    .append(Component.text(" não está na lista de staff.", NamedTextColor.YELLOW))));
            return;
        }
        plugin.removeAdmin(staffName);
        source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ", NamedTextColor.GREEN)
                .append(Component.text(staffName, NamedTextColor.YELLOW))
                .append(Component.text(" ʀᴇᴍᴏᴠɪᴅᴏ ᴅᴀ ʟɪsᴛᴀ ᴅᴇ sᴛᴀꜰꜰ ᴅᴀ ᴍᴀɴᴜᴛᴇɴçãᴏ.", NamedTextColor.GREEN))));
        if (plugin.getAuditLogger() != null) {
            String moderator = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
            plugin.getAuditLogger().log(moderator, "MAINTENANCE_REMOVESTAFF", staffName, "Removed from maintenance bypass list");
        }
    }

    private void handleListStaff(CommandSource source) {
        Set<String> admins = plugin.getAdminUsers();
        if (admins.isEmpty()) {
            source.sendMessage(PREFIX.append(Component.text("ɴᴇɴʜᴜᴍ sᴛᴀꜰꜰ ɴᴀ ʟɪsᴛᴀ ᴅᴇ ᴍᴀɴᴜᴛᴇɴçãᴏ.", NamedTextColor.YELLOW)));
        } else {
            source.sendMessage(PREFIX.append(Component.text("sᴛᴀꜰꜰ ᴅᴀ ᴍᴀɴᴜᴛᴇɴçãᴏ (" + admins.size() + "):", NamedTextColor.GOLD)));
            for (String admin : admins) {
                source.sendMessage(Component.text("  - ", NamedTextColor.GRAY)
                        .append(Component.text(admin, NamedTextColor.YELLOW)));
            }
        }
    }

    private void handleStatus(CommandSource source) {
        source.sendMessage(Component.empty());
        source.sendMessage(Component.text("sᴛᴀᴛᴜs ᴅᴇ ᴍᴀɴᴜᴛᴇɴçãᴏ", NamedTextColor.GOLD, TextDecoration.BOLD));
        source.sendMessage(Component.empty());

        boolean anyMaintenance = false;
        MaintenanceScheduler scheduler = plugin.getMaintenanceScheduler();

        for (var registered : server.getAllServers()) {
            String name = registered.getServerInfo().getName();
            boolean inMaintenance = plugin.isMaintenance(name);
            Long scheduledEnd = scheduler.getScheduledEndTime(name);
            Boolean scheduledType = scheduler.getScheduledType(name);

            if (!inMaintenance && scheduledEnd == null) continue;

            anyMaintenance = true;

            Component status;
            if (inMaintenance) {
                status = Component.text("  ● ", NamedTextColor.RED)
                        .append(Component.text(name, NamedTextColor.YELLOW))
                        .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("ᴇᴍ ᴍᴀɴᴜᴛᴇɴçãᴏ", NamedTextColor.RED));
            } else {
                status = Component.text("  ● ", NamedTextColor.GREEN)
                        .append(Component.text(name, NamedTextColor.YELLOW))
                        .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("ᴏɴʟɪɴᴇ", NamedTextColor.GREEN));
            }
            source.sendMessage(status);

            // Mostra agendamento pendente
            if (scheduledEnd != null) {
                long remaining = (scheduledEnd - System.currentTimeMillis()) / 1000;
                if (remaining > 0) {
                    String action = (scheduledType != null && scheduledType) ? "ɪɴíᴄɪᴏ" : "ᴛéʀᴍɪɴᴏ";
                    source.sendMessage(Component.text("    ⏱ ", NamedTextColor.GRAY)
                            .append(Component.text(action + " ᴀɢᴇɴᴅᴀᴅᴏ ᴇᴍ ", NamedTextColor.GRAY))
                            .append(Component.text(formatTime(remaining), NamedTextColor.AQUA)));
                }
            }
        }

        if (!anyMaintenance) {
            source.sendMessage(Component.text("  ɴᴇɴʜᴜᴍ sᴇʀᴠɪᴅᴏʀ ᴇᴍ ᴍᴀɴᴜᴛᴇɴçãᴏ.", NamedTextColor.GREEN));
        }

        // Staff count
        Set<String> staff = plugin.getAdminUsers();
        source.sendMessage(Component.empty());
        source.sendMessage(Component.text("  sᴛᴀꜰꜰ ʙʏᴘᴀss: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(staff.size()), NamedTextColor.YELLOW))
                .append(Component.text(" ᴊᴏɢᴀᴅᴏʀ(ᴇs)", NamedTextColor.GRAY)));
        source.sendMessage(Component.empty());
    }

    private void handleToggle(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(PREFIX.append(Component.text("Sintaxe incorreta.", NamedTextColor.RED)));
            source.sendMessage(Component.text("Uso: /midgard maintenance <servidor> <on/off> [tempo]", NamedTextColor.GRAY));
            return;
        }

        String serverName = args[0];
        if (server.getServer(serverName).isEmpty()) {
            source.sendMessage(PREFIX.append(Component.text("sᴇʀᴠɪᴅᴏʀ '", NamedTextColor.RED)
                    .append(Component.text(serverName, NamedTextColor.YELLOW))
                    .append(Component.text("' ɴãᴏ ꜰᴏɪ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.", NamedTextColor.RED))));
            return;
        }

        String action = args[1];
        if (action.equalsIgnoreCase("on")) {
            handleOn(source, serverName, args);
        } else if (action.equalsIgnoreCase("off")) {
            handleOff(source, serverName, args);
        } else {
            source.sendMessage(PREFIX.append(Component.text("Opção inválida. Use 'on' ou 'off'.", NamedTextColor.RED)));
        }
    }

    private void handleOn(CommandSource source, String serverName, String[] args) {
        if (args.length == 3) {
            // Manutenção Agendada
            String timeStr = args[2];
            long seconds = parseTime(timeStr);
            if (seconds <= 0) {
                source.sendMessage(PREFIX.append(Component.text("Formato de tempo inválido.", NamedTextColor.RED)));
                source.sendMessage(Component.text("Formatos aceitos: 1h, 30m, 10s", NamedTextColor.GRAY));
                return;
            }

            plugin.getMaintenanceScheduler().schedule(serverName, seconds, true);
            source.sendMessage(PREFIX.append(Component.text("ᴍᴀɴᴜᴛᴇɴçãᴏ ᴘʀᴏɢʀᴀᴍᴀᴅᴀ ᴘᴀʀᴀ ᴏ sᴇʀᴠɪᴅᴏʀ ", NamedTextColor.GREEN)
                    .append(Component.text(serverName, NamedTextColor.YELLOW))
                    .append(Component.text(" ᴇᴍ ", NamedTextColor.GREEN))
                    .append(Component.text(timeStr, NamedTextColor.YELLOW))));
            return;
        }

        if (plugin.isMaintenance(serverName)) {
            source.sendMessage(PREFIX.append(Component.text("O servidor ", NamedTextColor.YELLOW)
                    .append(Component.text(serverName, NamedTextColor.YELLOW))
                    .append(Component.text(" já está em manutenção.", NamedTextColor.YELLOW))));
            return;
        }

        // Manutenção "instantânea" tem 5s de delay para aplicar proteções
        plugin.getMaintenanceScheduler().schedule(serverName, 5, true);
        source.sendMessage(PREFIX.append(Component.text("Iniciando protocolos de segurança...", NamedTextColor.YELLOW)));
        source.sendMessage(PREFIX.append(Component.text("Manutenção iniciará em 5 segundos para proteção dos dados.", NamedTextColor.GREEN)));
    }

    private void handleOff(CommandSource source, String serverName, String[] args) {
        if (args.length == 3) {
            // Agendamento para DESLIGAR manutenção
            String timeStr = args[2];
            long seconds = parseTime(timeStr);
            if (seconds <= 0) {
                source.sendMessage(PREFIX.append(Component.text("Formato de tempo inválido.", NamedTextColor.RED)));
                source.sendMessage(Component.text("Formatos aceitos: 1h, 30m, 10s", NamedTextColor.GRAY));
                return;
            }

            if (!plugin.isMaintenance(serverName)) {
                source.sendMessage(PREFIX.append(Component.text("O servidor ", NamedTextColor.RED)
                        .append(Component.text(serverName, NamedTextColor.YELLOW))
                        .append(Component.text(" não está em manutenção.", NamedTextColor.RED))));
                source.sendMessage(Component.text("Não é possível agendar o término.", NamedTextColor.GRAY));
                return;
            }

            plugin.getMaintenanceScheduler().schedule(serverName, seconds, false);
            source.sendMessage(PREFIX.append(Component.text("Retorno programado para o servidor ", NamedTextColor.GREEN)
                    .append(Component.text(serverName, NamedTextColor.YELLOW))
                    .append(Component.text(" em ", NamedTextColor.GREEN))
                    .append(Component.text(timeStr, NamedTextColor.YELLOW))));
            return;
        }

        // Cancela agendamento se houver
        plugin.getMaintenanceScheduler().cancel(serverName);

        if (!plugin.isMaintenance(serverName)) {
            source.sendMessage(PREFIX.append(Component.text("O servidor ", NamedTextColor.YELLOW)
                    .append(Component.text(serverName, NamedTextColor.YELLOW))
                    .append(Component.text(" não está em manutenção.", NamedTextColor.YELLOW))));
            return;
        }
        plugin.setMaintenance(serverName, false);

        // Notificar Bot Discord
        plugin.sendSocketMessage("MAINTENANCE:" + serverName + ":false");

        // Notificar TODOS os jogadores que a manutenção acabou
        plugin.getServer().getAllPlayers().forEach(player -> {
            try {
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("                                                  ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
                player.sendMessage(Component.empty());
                player.sendMessage(plugin.getMessagesManager().get("maintenance.stop-title"));
                player.sendMessage(Component.empty());
                player.sendMessage(plugin.getMessagesManager().get("maintenance.stop-message", "server", serverName));
                player.sendMessage(Component.empty());
                player.sendMessage(Component.text("                                                  ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
                player.sendMessage(Component.empty());
            } catch (Exception e) {
                plugin.getLogger().error("Erro ao notificar jogador sobre fim de manutenção: " + player.getUsername(), e);
            }
        });
    }

    // ─── Utilitários ───────────────────────────────────────────────

    private void sendUsage(CommandSource source) {
        source.sendMessage(PREFIX.append(Component.text("Sintaxe incorreta.", NamedTextColor.RED)));
        source.sendMessage(Component.text("Uso: /midgard maintenance <servidor> <on/off> [tempo]", NamedTextColor.GRAY));
        source.sendMessage(Component.text("Uso: /midgard maintenance addstaff <jogador>", NamedTextColor.GRAY));
        source.sendMessage(Component.text("Uso: /midgard maintenance removestaff <jogador>", NamedTextColor.GRAY));
        source.sendMessage(Component.text("Uso: /midgard maintenance liststaff", NamedTextColor.GRAY));
        source.sendMessage(Component.text("Uso: /midgard maintenance status", NamedTextColor.GRAY));
    }

    private long parseTime(String timeStr) {
        try {
            String number = timeStr.replaceAll("[^0-9]", "");
            String unit = timeStr.replaceAll("[0-9]", "").toLowerCase();

            if (number.isEmpty()) return -1;
            long value = Long.parseLong(number);

            switch (unit) {
                case "d": return value * 86400;
                case "h": return value * 3600;
                case "m": return value * 60;
                case "s": return value;
                default: return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) {
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            return h + "h" + (m > 0 ? m + "m" : "");
        } else if (seconds >= 60) {
            long m = seconds / 60;
            long s = seconds % 60;
            return m + "m" + (s > 0 ? s + "s" : "");
        }
        return seconds + "s";
    }

    private List<String> filter(List<String> list, String prefix) {
        if (prefix.isEmpty()) return list;
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}
