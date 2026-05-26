package de.maxhenkel.voicechat.voice.server;

import de.maxhenkel.voicechat.BuildConstants;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.compatibility.PlayerHideEvent;
import de.maxhenkel.voicechat.compatibility.PlayerShowEvent;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.net.RequestSecretPacket;
import de.maxhenkel.voicechat.net.SecretPacket;
import de.maxhenkel.voicechat.plugins.PluginManager;
import de.maxhenkel.voicechat.voice.common.Secret;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerVoiceEvents implements Listener {

    private final Map<UUID, Integer> clientCompatibilities;
    private Server server;

    public ServerVoiceEvents() {
        clientCompatibilities = new ConcurrentHashMap<>();
    }

    public void init() {
        if (server != null) {
            return;
        }
        if (!Bukkit.getOnlineMode()) {
            Voicechat.LOGGER.warn("Running in offline mode - Voice chat encryption is not secure!");
        }

        Voicechat.compatibility.registerPlayerHideEvent(this::onPlayerHide);
        Voicechat.compatibility.registerPlayerShowEvent(this::onPlayerShow);

        server = new Server();
        server.start();

        PluginManager.instance().onServerStarted();
    }

    public void onRequestSecretPacket(Player player, RequestSecretPacket packet) {
        Voicechat.LOGGER.info("Received secret request of {} ({})", player.getName(), packet.getCompatibilityVersion());

        UUID playerUUID;

        try {
            playerUUID = player.getUniqueId();
        } catch (UnsupportedOperationException e) {
            player.kickPlayer(Voicechat.MESSAGES.text(
                    "voice.auth_still_connecting_kick",
                    "&cA autenticacao do voice chat foi tentada cedo demais durante a conexao."
            ));
            Voicechat.LOGGER.warn("{} tried to authenticate voice chat while still connecting", player.getName());
            return;
        }

        clientCompatibilities.put(playerUUID, packet.getCompatibilityVersion());
        if (packet.getCompatibilityVersion() != Voicechat.COMPATIBILITY_VERSION) {
            Voicechat.LOGGER.warn("Connected client {} has incompatible voice chat version (server={}, client={})", player.getName(), Voicechat.COMPATIBILITY_VERSION, packet.getCompatibilityVersion());
            sendIncompatibleMessage(player, packet.getCompatibilityVersion());
        } else {
            initializePlayerConnection(player);
        }
    }

    public boolean isCompatible(Player player) {
        return isCompatible(player.getUniqueId());
    }

    public boolean isCompatible(UUID playerUuid) {
        return clientCompatibilities.getOrDefault(playerUuid, -1) == Voicechat.COMPATIBILITY_VERSION;
    }

    public static void sendIncompatibleMessage(Player player, int clientCompatibilityVersion) {
        Voicechat.compatibility.sendIncompatibleMessage(player, BuildConstants.MOD_COMPATIBLE_VERSION, BuildConstants.PLUGIN_NAME);
    }

    public void initializePlayerConnection(Player player) {
        if (server == null) {
            return;
        }
        server.getPlayerStateManager().onPlayerCompatibilityCheckSucceeded(player);
        server.getCategoryManager().onPlayerCompatibilityCheckSucceeded(player);
        server.getGroupManager().onPlayerCompatibilityCheckSucceeded(player);

        Secret secret = server.generateNewSecret(player.getUniqueId());
        if (secret == null) {
            Voicechat.LOGGER.warn("Player already requested secret - ignoring");
            return;
        }
        NetManager.sendToClient(player, new SecretPacket(player, secret, server.getPort(), Voicechat.SERVER_CONFIG));
        Voicechat.LOGGER.info("Sent secret to {}", player.getName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        server.getPlayerStateManager().onPlayerJoin(event);

        if (!Voicechat.SERVER_CONFIG.forceVoiceChat.get()) {
            return;
        }
        Player player = event.getPlayer();

        Voicechat.compatibility.runTaskLater(() -> {
            if (!player.isOnline()) {
                return;
            }
            if (!isCompatible(player)) {
                player.kickPlayer(String.format(
                        Voicechat.TRANSLATIONS.forceVoicechatKickMessage.get(),
                        BuildConstants.PLUGIN_NAME,
                        Voicechat.INSTANCE.getDescription().getVersion()
                ));
            }
        }, Voicechat.SERVER_CONFIG.loginTimeout.get() / 50L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        server.getPlayerStateManager().onPlayerQuit(event);
        server.getGroupManager().onPlayerQuit(event);

        if (Voicechat.voiceRecordingManager != null) {
            Voicechat.voiceRecordingManager.stopRecording(event.getPlayer().getUniqueId());
        }

        clientCompatibilities.remove(event.getPlayer().getUniqueId());
        if (server == null) {
            return;
        }

        server.disconnectClient(event.getPlayer().getUniqueId());
        Voicechat.LOGGER.info("Disconnecting client {}", event.getPlayer().getName());
    }

    public void onPlayerHide(PlayerHideEvent event) {
        server.getPlayerStateManager().onPlayerHide(event);
    }

    public void onPlayerShow(PlayerShowEvent event) {
        server.getPlayerStateManager().onPlayerShow(event);
    }

    public void restart() {
        if (server != null) {
            server.close();
            server = null;
        }
        server = new Server();
        server.start();
        PluginManager.instance().onServerStarted();

        // Reconectar todos os jogadores com voice chat
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isCompatible(player)) {
                initializePlayerConnection(player);
            }
        }
    }

    public Server getServer() {
        return server;
    }
}
