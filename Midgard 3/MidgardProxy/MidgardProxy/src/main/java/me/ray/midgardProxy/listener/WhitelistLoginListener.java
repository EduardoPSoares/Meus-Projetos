package me.ray.midgardProxy.listener;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import me.ray.midgardProxy.config.ConfigManager;
import me.ray.midgardProxy.whitelist.WhitelistChecker;
import me.ray.midgardProxy.whitelist.WhitelistChecker.WhitelistStatus;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

public class WhitelistLoginListener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final WhitelistChecker whitelistChecker;
    private final ConfigManager configManager;
    private final Logger logger;

    public WhitelistLoginListener(WhitelistChecker whitelistChecker, ConfigManager configManager, Logger logger) {
        this.whitelistChecker = whitelistChecker;
        this.configManager = configManager;
        this.logger = logger;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (!configManager.isWhitelistEnabled()) return;

        Player player = event.getPlayer();
        WhitelistStatus result = whitelistChecker.check(player.getUsername());

        if (result == null) {
            String msg = configManager.getWhitelistMessage("no_whitelist");
            event.setResult(ResultedEvent.ComponentResult.denied(MINI.deserialize(msg)));
            logger.info("Blocked login for {} - no whitelist", player.getUsername());
            return;
        }

        switch (result.status()) {
            case "PENDING":
                String pendingMsg = configManager.getWhitelistMessage("pending");
                event.setResult(ResultedEvent.ComponentResult.denied(MINI.deserialize(pendingMsg)));
                logger.info("Blocked login for {} - whitelist pending", player.getUsername());
                break;

            case "REJECTED":
                String rejectedMsg = configManager.getWhitelistMessage("rejected");
                event.setResult(ResultedEvent.ComponentResult.denied(MINI.deserialize(rejectedMsg)));
                logger.info("Blocked login for {} - whitelist rejected", player.getUsername());
                break;

            case "APPROVED":
                logger.info("Allowed login for {} - whitelist approved", player.getUsername());
                break;

            default:
                String defaultMsg = configManager.getWhitelistMessage("no_whitelist");
                event.setResult(ResultedEvent.ComponentResult.denied(MINI.deserialize(defaultMsg)));
                logger.warn("Blocked login for {} - unknown whitelist status: {}", player.getUsername(), result.status());
                break;
        }
    }
}
