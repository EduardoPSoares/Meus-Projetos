package me.ray.midgardDiscord;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Agendador de Manutenção.
 * Permite programar o início ou fim da manutenção de servidores com contagem regressiva.
 * Envia avisos (Title/Actionbar) para os jogadores antes da manutenção ocorrer.
 */
public class MaintenanceScheduler {

    private final MidgardVelocity plugin;
    private final ProxyServer server;
    // Mapas thread-safe para acesso concorrente
    private final Map<String, ScheduledTask> tasks = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Long> scheduledEndTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Boolean> scheduledTypes = new java.util.concurrent.ConcurrentHashMap<>(); // true = ON, false = OFF

    public MaintenanceScheduler(MidgardVelocity plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;
    }

    public void schedule(String serverName, long seconds, boolean targetState) {
        try {
            cancel(serverName); // Cancel existing if any

            long endTime = System.currentTimeMillis() + (seconds * 1000);
            scheduledEndTimes.put(serverName.toLowerCase(), endTime);
            scheduledTypes.put(serverName.toLowerCase(), targetState);

            ScheduledTask task = server.getScheduler()
                    .buildTask(plugin, new Runnable() {
                        long remaining = seconds;

                        @Override
                        public void run() {
                            try {
                                if (remaining <= 0) {
                                    if (targetState) {
                                        startMaintenance(serverName);
                                    } else {
                                        stopMaintenance(serverName);
                                    }
                                    cancel(serverName);
                                    return;
                                }

                                if (remaining == 5 && targetState) {
                                    sendPreMaintenanceSignal(serverName);
                                }

                                if (shouldWarn(remaining, seconds)) {
                                    broadcastWarning(serverName, remaining, targetState);
                                }

                                remaining--;
                            } catch (Exception e) {
                                plugin.getLogger().error("Erro na task de agendamento de manutenção: ", e);
                            }
                        }
                    })
                    .repeat(1, TimeUnit.SECONDS)
                    .schedule();

            tasks.put(serverName.toLowerCase(), task);
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao agendar manutenção: ", e);
        }
    }

    public void cancel(String serverName) {
        try {
            ScheduledTask task = tasks.remove(serverName.toLowerCase());
            scheduledEndTimes.remove(serverName.toLowerCase());
            scheduledTypes.remove(serverName.toLowerCase());
            
            if (task != null) {
                task.cancel();
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao cancelar manutenção: ", e);
        }
    }
    
    public Long getScheduledEndTime(String serverName) {
        return scheduledEndTimes.get(serverName.toLowerCase());
    }
    
    public Boolean getScheduledType(String serverName) {
        return scheduledTypes.get(serverName.toLowerCase());
    }

    private void sendPreMaintenanceSignal(String serverName) {
        try {
            server.getServer(serverName).ifPresent(registeredServer -> {
                try {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("PRE_MAINTENANCE");
                    registeredServer.sendPluginMessage(MinecraftChannelIdentifier.create("midgard", "maintenance"), out.toByteArray());
                } catch (Exception e) {
                    plugin.getLogger().error("Erro ao enviar sinal de pré-manutenção: ", e);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao processar sinal de pré-manutenção: ", e);
        }
    }

    private void startMaintenance(String serverName) {
        try {
            plugin.setMaintenance(serverName, true);
            plugin.sendSocketMessage("MAINTENANCE:" + serverName + ":true");
            
            server.getServer(serverName).ifPresent(registeredServer -> {
                 registeredServer.getPlayersConnected().forEach(player -> {
                     try {
                         boolean isStaff = player.hasPermission("midgard.staff");
                         boolean isAdmin = player.hasPermission("midgard.admin");
                         boolean isOp = player.hasPermission("midgard.op");
                         boolean isBypass = player.hasPermission("midgard.maintenance.bypass");
                         boolean isConfigAdmin = plugin.isAdmin(player.getUsername());

                         if (!isStaff && !isAdmin && !isOp && !isBypass && !isConfigAdmin) {
                             if (serverName.equalsIgnoreCase(plugin.getLobbyServerName())) {
                                 Component kickMessage = Component.empty()
                                         .append(Component.newline())
                                         .append(plugin.getMessagesManager().get("maintenance.kick-title"))
                                         .append(Component.newline())
                                         .append(Component.newline())
                                         .append(plugin.getMessagesManager().get("maintenance.kick-subtitle"))
                                         .append(Component.newline())
                                         .append(Component.newline())
                                         .append(plugin.getMessagesManager().get("maintenance.kick-message", "server", serverName));
                                 player.disconnect(kickMessage);
                             } else {
                                 java.util.Optional<RegisteredServer> lobbyOpt = server.getServer(plugin.getLobbyServerName());
                                 if (lobbyOpt.isPresent()) {
                                     RegisteredServer lobby = lobbyOpt.get();
                                     player.createConnectionRequest(lobby).connect();
                                     player.sendMessage(Component.empty());
                                     player.sendMessage(Component.text("                                                  ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
                                     player.sendMessage(Component.empty());
                                     player.sendMessage(plugin.getMessagesManager().get("maintenance.lobby-title"));
                                     player.sendMessage(Component.empty());
                                     player.sendMessage(plugin.getMessagesManager().get("maintenance.lobby-message", "server", serverName));
                                     player.sendMessage(Component.empty());
                                     player.sendMessage(Component.text("                                                  ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
                                     player.sendMessage(Component.empty());
                                 } else {
                                     // Lobby não encontrado: desconecta o jogador como fallback
                                     plugin.getLogger().warn("Lobby '{}' não encontrado! Desconectando {} durante início de manutenção.", plugin.getLobbyServerName(), player.getUsername());
                                     Component kickMessage = Component.empty()
                                             .append(Component.newline())
                                             .append(plugin.getMessagesManager().get("maintenance.kick-title"))
                                             .append(Component.newline())
                                             .append(Component.newline())
                                             .append(plugin.getMessagesManager().get("maintenance.kick-message", "server", serverName));
                                     player.disconnect(kickMessage);
                                 }
                             }
                         }
                     } catch (Exception e) {
                         plugin.getLogger().error("Erro ao processar jogador durante início de manutenção: " + player.getUsername(), e);
                     }
                 });
            });
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao iniciar manutenção: ", e);
        }
    }
    
    private void stopMaintenance(String serverName) {
        try {
            plugin.setMaintenance(serverName, false);
            plugin.sendSocketMessage("MAINTENANCE:" + serverName + ":false");
            
            server.getAllPlayers().forEach(player -> {
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
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao parar manutenção: ", e);
        }
    }

    private boolean shouldWarn(long remaining, long total) {
        if (remaining == total) return true; // Start
        
        // Warn every hour if > 1h
        if (remaining > 3600 && remaining % 3600 == 0) return true;
        
        // Warn at specific marks
        if (remaining == 1800) return true; // 30m
        if (remaining == 900) return true;  // 15m
        if (remaining == 600) return true;  // 10m
        if (remaining == 300) return true;  // 5m
        if (remaining == 60) return true;   // 1m
        if (remaining == 30) return true;   // 30s
        if (remaining == 10) return true;   // 10s
        if (remaining <= 5 && remaining > 0) return true; // 5, 4, 3, 2, 1

        return false;
    }

    private void broadcastWarning(String serverName, long remaining, boolean isMaintenanceStarting) {
        try {
            String timeString = formatTime(remaining);
            
            Component title;
            Component subtitle;
            
            if (isMaintenanceStarting) {
                title = plugin.getMessagesManager().get("maintenance.warning-start-title");
                subtitle = plugin.getMessagesManager().get("maintenance.warning-start-subtitle", "server", serverName, "time", timeString);
            } else {
                title = plugin.getMessagesManager().get("maintenance.warning-end-title");
                subtitle = plugin.getMessagesManager().get("maintenance.warning-end-subtitle", "server", serverName, "time", timeString);
            }
            
            Title t = Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofMillis(1000)));
            
            server.getAllPlayers().forEach(player -> {
                try {
                    player.showTitle(t);
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("                                                  ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
                    player.sendMessage(Component.empty());
                    
                    if (isMaintenanceStarting) {
                        player.sendMessage(plugin.getMessagesManager().get("maintenance.warning-start-chat", "server", serverName, "time", timeString));
                    } else {
                        player.sendMessage(plugin.getMessagesManager().get("maintenance.warning-end-chat", "server", serverName, "time", timeString));
                    }
                    
                    player.sendMessage(Component.empty());
                    player.sendMessage(Component.text("                                                  ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
                    player.sendMessage(Component.empty());
                } catch (Exception e) {
                    plugin.getLogger().error("Erro ao enviar aviso de manutenção para jogador: " + player.getUsername(), e);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao transmitir aviso de manutenção: ", e);
        }
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) {
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            return h + "h" + (m > 0 ? " " + m + "m" : "");
        } else if (seconds >= 60) {
            long m = seconds / 60;
            long s = seconds % 60;
            return m + "m" + (s > 0 ? " " + s + "s" : "");
        } else {
            return seconds + "s";
        }
    }
}