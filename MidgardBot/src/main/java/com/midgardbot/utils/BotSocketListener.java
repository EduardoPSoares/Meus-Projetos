package com.midgardbot.utils;

import com.midgardbot.features.ServerStatusMonitor;
import com.midgardbot.config.BotConfig;
import com.midgardbot.config.Constants;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listener de Socket para comunicação interna.
 * Abre uma porta TCP para receber mensagens do plugin Velocity (ex: atualizações de manutenção).
 * Permite comunicação bidirecional simples entre o Bot e o Servidor Minecraft.
 */
public class BotSocketListener extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotSocketListener.class);
    private final int port;
    private final JDA jda;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final ExecutorService connectionPool = Executors.newFixedThreadPool(4);

    public BotSocketListener(JDA jda) {
        this.jda = jda;
        this.port = com.midgardbot.config.BotConfig.getSocketPort();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            LOGGER.info("BotSocketListener ouvindo na porta " + port);

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    // Processa cada conexão em uma thread do pool para não bloquear o listener
                    connectionPool.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    if (running) {
                        LOGGER.error("Erro ao aceitar conexão socket", e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro fatal ao iniciar BotSocketListener na porta " + port, e);
        }
    }

    private void handleConnection(Socket socket) {
        try (socket) {
            socket.setSoTimeout(Constants.SOCKET_TIMEOUT_MS); // Timeout para leitura
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // Protocolos suportados:
            // 1) AUTH:<secret>\n<message>
            // 2) <secret>:<message>  (legado)
            // 3) Se secret estiver vazio, aceita apenas conexões locais

            String line1 = reader.readLine();
            if (line1 == null || line1.isEmpty()) {
                return;
            }

            String configuredSecret = com.midgardbot.config.BotConfig.getSocketSecret();
            String authSecret = null;
            String message;

            if (line1.startsWith("AUTH:")) {
                authSecret = line1.substring("AUTH:".length());
                message = reader.readLine();
                if (message == null) {
                    return;
                }
            } else {
                message = line1;
            }

            boolean isLocal = socket.getInetAddress().isLoopbackAddress() || socket.getInetAddress().isAnyLocalAddress();
            boolean secretConfigured = configuredSecret != null && !configuredSecret.isEmpty();

            if (!secretConfigured) {
                // Se não há secret configurada, pelo menos restringe ao localhost.
                if (!isLocal) {
                    LOGGER.warn("Tentativa de conexão não autorizada no Socket (não-local): " + socket.getInetAddress());
                    return;
                }
            } else {
                // Valida contra AUTH ou prefixo legado (comparação em tempo constante para evitar timing attacks)
                boolean authOk = authSecret != null && MessageDigest.isEqual(
                        authSecret.getBytes(StandardCharsets.UTF_8),
                        configuredSecret.getBytes(StandardCharsets.UTF_8)
                );
                boolean legacyOk = false;
                int colonIndex = message.indexOf(':');
                if (colonIndex > 0) {
                    String extractedSecret = message.substring(0, colonIndex);
                    legacyOk = MessageDigest.isEqual(
                            extractedSecret.getBytes(StandardCharsets.UTF_8),
                            configuredSecret.getBytes(StandardCharsets.UTF_8)
                    );
                }

                if (!authOk && !legacyOk) {
                    LOGGER.warn("⛔ Tentativa de conexão não autorizada no Socket: " + socket.getInetAddress());
                    LOGGER.warn("   Chave esperada: " + configuredSecret.substring(0, Math.min(3, configuredSecret.length())) + "***");
                    LOGGER.warn("   Recebido (Auth): " + (authSecret != null ? "Presente" : "Ausente"));
                    LOGGER.warn("   Recebido (Legacy): " + (message.contains(":") ? "Formato Correto" : "Formato Inválido"));
                    return;
                }

                if (legacyOk && colonIndex > 0) {
                    message = message.substring(colonIndex + 1);
                }
            }

            // Sanitização básica
            message = message.replace("\r", "").replace("\n", "");
            LOGGER.info("Mensagem recebida via Socket: " + message);

            processMessage(message);

        } catch (Exception e) {
            LOGGER.error("Erro ao processar conexão socket", e);
        }
    }

    private void processMessage(String message) {
        try {
            // Protocolo: MAINTENANCE:<server>:<true/false>
            if (message.startsWith("MAINTENANCE:")) {
                String[] parts = message.split(":");
                if (parts.length == 3) {
                    String serverName = parts[1];
                    boolean state = Boolean.parseBoolean(parts[2]);
                    LOGGER.info("Processando manutenção para: " + serverName + " Estado: " + state);
                    ServerStatusMonitor.setMaintenance(serverName, state);
                }
            } else if (message.startsWith("DEATH:")) {
                // DEATH:<player>:<killer>:<message>
                String[] parts = message.split(":", 4);
                if (parts.length == 4) {
                    String player = parts[1];
                    String killer = parts[2];
                    String deathMsg = parts[3];
                    
                    String channelId = BotConfig.getDeathChannelId();
                    if (channelId != null) {
                        TextChannel channel = jda.getTextChannelById(channelId);
                        if (channel != null) {
                            channel.sendMessageEmbeds(
                                EmbedUtils.createEmbed("☠ Morte Registrada", deathMsg, java.awt.Color.RED)
                                    .addField("Vítima", player, true)
                                    .addField("Assassino", killer, true)
                                    .setThumbnail("https://minotar.net/avatar/" + player + "/100.png")
                                    .build()
                            ).queue();
                        }
                    }
                }
            } else if (message.startsWith("WHITELIST_REMOVE:")) {
                String[] parts = message.split(":");
                if (parts.length == 2) {
                    String discordId = parts[1];
                    com.midgardbot.data.DataManager.removeWhitelistStatus(discordId);
                    LOGGER.info("Whitelist removida via Socket para ID: " + discordId);
                }
            } else {
                // Fallback para sinal simples (apenas update)
                ServerStatusMonitor.forceUpdate();
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao processar mensagem do socket: " + message, e);
        }
    }

    public void shutdown() {
        running = false;
        connectionPool.shutdownNow();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignora
        }
    }
}
