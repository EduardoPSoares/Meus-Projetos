package com.midgardbot.features;

import com.midgardbot.config.BotConfig;
import com.midgardbot.config.Constants;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.MinecraftPing;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitor de Status do Servidor.
 * Verifica periodicamente se os servidores (Lobby/RPG) estão online via Ping.
 * Atualiza uma mensagem fixa no Discord com o status em tempo real.
 */
public class ServerStatusMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStatusMonitor.class);
    private final JDA jda;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private static ServerStatusMonitor instance;
    private static final java.util.Map<String, Boolean> maintenanceStates = new java.util.concurrent.ConcurrentHashMap<>();
    
    private String statusMessageId = null;
    
    // Cores da Marca (Visual Profissional)
    private static final Color COLOR_ONLINE = Color.decode("#43b581"); // Discord Green
    private static final Color COLOR_OFFLINE = Color.decode("#f04747"); // Discord Red
    private static final Color COLOR_MAINTENANCE = Color.decode("#faa61a"); // Discord Yellow
    private static final Color COLOR_DEFAULT = Color.decode("#2f3136"); // Dark Gray Clean

    private enum ServerState { ONLINE, OFFLINE, MAINTENANCE }
    private ServerState lastLobbyState = null;
    private ServerState lastRpgState = null;

    public ServerStatusMonitor(JDA jda) {
        this.jda = jda;
        instance = this;
    }

    public static void setMaintenance(String serverName, boolean state) {
        LOGGER.info("Definindo manutenção para " + serverName + ": " + state);
        maintenanceStates.put(serverName.toLowerCase(), state);
        forceUpdate();
    }

    public static void forceUpdate() {
        if (instance != null) {
            instance.updateStatusMessage();
        }
    }

    public void start() {
        LOGGER.info("Monitoramento de Status (Embed) iniciado.");
        scheduler.scheduleAtFixedRate(this::updateStatusMessage, 5, Constants.STATUS_CHECK_INTERVAL_S, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        
        if (statusMessageId != null) {
             String channelId = BotConfig.getStatusChannelId();
             if (channelId != null) {
                 TextChannel channel = jda.getTextChannelById(channelId);
                 if (channel != null) {
                     EmbedBuilder embed = new EmbedBuilder();
                     embed.setTitle("💤 Sistema em Hibernação");
                     embed.setDescription("O sistema de monitoramento automático foi **pausado** para manutenção interna do bot.\n\n> *O status dos servidores não será atualizado até o retorno do sistema.*");
                     embed.setColor(Color.BLACK);
                     embed.setThumbnail(jda.getSelfUser().getAvatarUrl());
                     embed.setTimestamp(Instant.now());
                     embed.setFooter("Monitoramento Suspenso", null);
                     
                     try {
                        channel.editMessageEmbedsById(statusMessageId, embed.build()).setComponents().complete();
                     } catch (Exception e) {
                        // Ignora erros de interrupção durante o shutdown
                        if (e.getMessage().contains("InterruptedIOException") || e.getCause() instanceof java.io.InterruptedIOException) {
                            LOGGER.info("Mensagem de offline não enviada (Processo interrompido).");
                        } else {
                            LOGGER.error("Erro ao definir mensagem de offline: " + e.getMessage());
                        }
                     }
                 }
             }
        }

        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    private void updateStatusMessage() {
        try {
            String channelId = BotConfig.getStatusChannelId();
            if (channelId == null || channelId.isEmpty()) return;

            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                LOGGER.warn("⚠️ Canal de status não encontrado: " + channelId);
                return;
            }

            String serverIp = BotConfig.getServerIp();
            MinecraftPing.ServerInfo lobbyInfo;
            MinecraftPing.ServerInfo rpgInfo;
            
            try {
                lobbyInfo = MinecraftPing.getPing(serverIp, BotConfig.getLobbyPort());
            } catch (Exception e) {
                LOGGER.error("Erro ao pingar Lobby", e);
                lobbyInfo = new MinecraftPing.ServerInfo("Offline", 0, 0, false); // Offline fallback
            }
            
            try {
                rpgInfo = MinecraftPing.getPing(serverIp, BotConfig.getRpgPort());
            } catch (Exception e) {
                LOGGER.error("Erro ao pingar RPG", e);
                rpgInfo = new MinecraftPing.ServerInfo("Offline", 0, 0, false); // Offline fallback
            }

            boolean lobbyMaintenance = DataManager.isMaintenanceMode() || maintenanceStates.getOrDefault("lobby", false);
            boolean rpgMaintenance = DataManager.isMaintenanceMode() || maintenanceStates.getOrDefault("rpg", false) || maintenanceStates.getOrDefault("survival", false);
            
            // LOGGER.info("Atualizando Status -> Lobby: " + lobbyMaintenance + " | RPG: " + rpgMaintenance + " (Map: " + maintenanceStates + ")");

            ServerState currentLobbyState = determineState(lobbyInfo.isOnline, lobbyMaintenance);
            ServerState currentRpgState = determineState(rpgInfo.isOnline, rpgMaintenance);

            checkAndNotify(channel, currentLobbyState, currentRpgState);

            lastLobbyState = currentLobbyState;
            lastRpgState = currentRpgState;

            // --- CONSTRUÇÃO DO EMBED PRINCIPAL ---
            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor("Midgard Network • Monitoramento", "https://midgard.com", jda.getSelfUser().getAvatarUrl());
            embed.setTitle("📡 Status dos Reinos");
            
            // Descrição limpa com IP
            embed.setDescription("Acompanhe a disponibilidade e estabilidade dos nossos servidores em tempo real.\n\n" +
                "**Endereço de Conexão:**\n" +
                "`" + BotConfig.getServerIp() + "`");
            
            embed.setColor(COLOR_DEFAULT);
            embed.setThumbnail(jda.getSelfUser().getAvatarUrl());
            embed.setTimestamp(Instant.now());
            embed.setFooter("Última sincronização", null);

            // Formatação visual dos campos
            String lobbyIcon = getStatusEmoji(lobbyInfo.isOnline, lobbyMaintenance);
            String rpgIcon = getStatusEmoji(rpgInfo.isOnline, rpgMaintenance);

            // Campo Lobby
            embed.addField(
                lobbyIcon + " **Lobby Principal**",
                formatServerDescRich(lobbyInfo, lobbyMaintenance),
                true
            );

            // Campo RPG
            embed.addField(
                rpgIcon + " **Reino RPG**",
                formatServerDescRich(rpgInfo, rpgMaintenance),
                true
            );

            // BOTÕES
            Button btnNotify = Button.success("status_notify", "🔔 Receber Alertas");

            if (statusMessageId != null) {
                channel.retrieveMessageById(statusMessageId).queue(
                    msg -> msg.editMessageEmbeds(embed.build()).setComponents(ActionRow.of(btnNotify)).queue(),
                    err -> {
                        statusMessageId = null;
                        findOrCreateMessage(channel, embed, btnNotify);
                    }
                );
            } else {
                findOrCreateMessage(channel, embed, btnNotify);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar status do servidor", e);
        }
    }

    private void findOrCreateMessage(TextChannel channel, EmbedBuilder embed, Button... buttons) {
        channel.getHistory().retrievePast(10).queue(messages -> {
            try {
                for (Message msg : messages) {
                    if (msg.getAuthor().equals(jda.getSelfUser())) {
                        if (!msg.getEmbeds().isEmpty() && msg.getEmbeds().get(0).getTitle() != null && msg.getEmbeds().get(0).getTitle().contains("Status dos Reinos")) {
                            statusMessageId = msg.getId();
                            msg.editMessageEmbeds(embed.build()).setComponents(ActionRow.of(buttons)).queue();
                            return;
                        }
                    }
                }
                channel.sendMessageEmbeds(embed.build())
                       .setComponents(ActionRow.of(buttons))
                       .queue(msg -> statusMessageId = msg.getId());
            } catch (Exception e) {
                LOGGER.error("Erro ao processar histórico de mensagens", e);
            }
        }, error -> LOGGER.error("Erro ao recuperar histórico de mensagens", error));
    }

    private ServerState determineState(boolean isOnline, boolean isMaintenance) {
        if (isMaintenance) return ServerState.MAINTENANCE;
        if (isOnline) return ServerState.ONLINE;
        return ServerState.OFFLINE;
    }

    private void checkAndNotify(TextChannel channel, ServerState currentLobby, ServerState currentRpg) {
        if (lastLobbyState == null || lastRpgState == null) return;

        boolean lobbyChanged = currentLobby != lastLobbyState;
        boolean rpgChanged = currentRpg != lastRpgState;

        if (!lobbyChanged && !rpgChanged) return;

        String roleId = BotConfig.getNotificationRoleId();
        if (roleId == null || roleId.isEmpty()) return;

        // Lógica de Cor mais inteligente
        Color embedColor = COLOR_DEFAULT;
        boolean anyOffline = (currentLobby == ServerState.OFFLINE || currentRpg == ServerState.OFFLINE);
        boolean anyMaintenance = (currentLobby == ServerState.MAINTENANCE || currentRpg == ServerState.MAINTENANCE);
        boolean allOnline = (currentLobby == ServerState.ONLINE && currentRpg == ServerState.ONLINE);

        if (anyOffline) embedColor = COLOR_OFFLINE;
        else if (anyMaintenance) embedColor = COLOR_MAINTENANCE;
        else if (allOnline) embedColor = COLOR_ONLINE;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📢 Atualização de Rede");
        embed.setDescription("O sistema detectou uma alteração na conectividade dos servidores.");
        embed.setColor(embedColor);
        embed.setTimestamp(Instant.now());
        
        if (lobbyChanged) {
            embed.addField("🏰 Lobby Principal", formatStateChangeArrow(lastLobbyState, currentLobby), false);
        }
        if (rpgChanged) {
            embed.addField("⚔️ Reino RPG", formatStateChangeArrow(lastRpgState, currentRpg), false);
        }

        channel.getHistory().retrievePast(20).queue(messages -> {
            try {
                for (Message msg : messages) {
                    if (msg.getAuthor().equals(jda.getSelfUser()) && !msg.getEmbeds().isEmpty()) {
                        String title = msg.getEmbeds().get(0).getTitle();
                        if (title != null && title.contains("Atualização de Rede")) {
                            msg.delete().queue(null, e -> {});
                        }
                    }
                }

                channel.sendMessage("<@&" + roleId + ">")
                       .setEmbeds(embed.build())
                       .queue(msg -> {
                           scheduler.schedule(() -> {
                               channel.deleteMessageById(msg.getId()).queue(null, e -> {});
                           }, 1, TimeUnit.MINUTES);
                       });
            } catch (Exception e) {
                LOGGER.error("Erro ao notificar atualização de status", e);
            }
        }, error -> LOGGER.error("Erro ao recuperar histórico para notificação", error));
    }

    private String formatStateChangeArrow(ServerState oldState, ServerState newState) {
        return String.format("%s **%s** ➟ %s **%s**", 
            getStatusEmoji(oldState), getStateName(oldState),
            getStatusEmoji(newState), getStateName(newState));
    }

    private String getStatusEmoji(ServerState state) {
        switch (state) {
            case ONLINE: return "🟢"; 
            case OFFLINE: return "🔴"; 
            case MAINTENANCE: return "🟠"; 
            default: return "❓";
        }
    }

    private String getStateName(ServerState state) {
        switch (state) {
            case ONLINE: return "Online"; 
            case OFFLINE: return "Offline"; 
            case MAINTENANCE: return "Manutenção"; 
            default: return "Desconhecido";
        }
    }
    
    private String formatServerDescRich(MinecraftPing.ServerInfo info, boolean isMaintenance) {
        if (isMaintenance) {
            return ">>> 🚧 **Manutenção Programada**\nO acesso está restrito à equipe.";
        }
        if (!info.isOnline) {
            return ">>> 🛑 **Inacessível**\nTentando restabelecer conexão...";
        }
        
        int percentage = 0;
        if (info.maxPlayers > 0) percentage = (info.onlinePlayers * 100) / info.maxPlayers;
        String statusText = (percentage > 90) ? "Lotado" : "Estável";
        
        return String.format(
            "> Status: `🟢 %s`\n> Jogadores: **%d** / %d", 
            statusText, info.onlinePlayers, info.maxPlayers
        );
    }

    private String getStatusEmoji(boolean isOnline, boolean isMaintenance) {
        if (isMaintenance) return "🟠"; 
        if (isOnline) return "🟢";
        return "🔴";
    }
}