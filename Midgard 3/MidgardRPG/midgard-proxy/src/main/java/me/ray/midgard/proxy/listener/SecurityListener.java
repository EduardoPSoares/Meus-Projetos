package me.ray.midgard.proxy.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.ray.midgard.proxy.manager.SessionManager;

public class SecurityListener {

    private final SessionManager sessionManager;

    public SecurityListener(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Subscribe
    public void onServerSwitch(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        
        // Ignore initial connection (no current server)
        if (!player.getCurrentServer().isPresent()) return;

        // Check where they are going
        RegisteredServer target = event.getResult().getServer().orElse(null);
        if (target == null) return;
        
        // If targeting the same server (reconnect?), usually allowed or handled by Velocity
        if (player.getCurrentServer().get().getServer().equals(target)) return;

        // Check for safe token
        if (sessionManager.isSafe(player.getUniqueId())) {
            sessionManager.setSafe(player.getUniqueId(), false); // Consume token
            return;
        }

        // Halt and Sync
        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        
        sessionManager.requestSave(player.getUniqueId()).thenAccept(success -> {
            if (!success) {
                // Save timed out — do NOT allow server switch to prevent data loss/duplication
                player.sendMessage(net.kyori.adventure.text.Component.text(
                    "Sincronização falhou. Tente novamente em alguns segundos.",
                    net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
            
            sessionManager.setSafe(player.getUniqueId(), true);
            
            player.createConnectionRequest(target).connect().thenAccept(result -> {
                if (!result.isSuccessful()) {
                   player.sendMessage(net.kyori.adventure.text.Component.text(
                       "Falha ao conectar ao servidor de destino.",
                       net.kyori.adventure.text.format.NamedTextColor.RED));
                }
            });
        });
    }
}
