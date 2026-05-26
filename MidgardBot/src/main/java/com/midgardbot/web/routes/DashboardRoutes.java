package com.midgardbot.web.routes;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import net.dv8tion.jda.api.JDA;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;

/**
 * Rotas do Dashboard — informações gerais do bot e servidor.
 */
public class DashboardRoutes {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardRoutes.class);

    public static void register(Javalin app, JDA jda) {

        // GET /api/dashboard — dados gerais para o dashboard
        app.get("/api/dashboard", ctx -> {
            Runtime runtime = Runtime.getRuntime();
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

            // Estatísticas do bot
            int totalGuilds = jda.getGuilds().size();
            int totalUsers = jda.getGuilds().stream()
                    .mapToInt(g -> g.getMemberCount()).sum();
            int totalChannels = jda.getTextChannels().size() + jda.getVoiceChannels().size();

            // Estatísticas de whitelist
            var allStatus = DataManager.getAllStatus();
            var pendingStatuses = Set.of(
                    WhitelistStatus.PENDING, WhitelistStatus.REVIEWING,
                    WhitelistStatus.NEEDS_REVIEW, WhitelistStatus.FLAGGED,
                    WhitelistStatus.PRIORITY, WhitelistStatus.STANDBY
            );
            // Contar apenas pendentes reais (com respostas preenchidas no status)
            long pendingCount = allStatus.values().stream()
                    .filter(s -> pendingStatuses.contains(s.status))
                    .filter(s -> s.answers != null && !s.answers.isEmpty() && !"{}" .equals(s.answers))
                    .count();
            long approvedCount = allStatus.values().stream()
                    .filter(s -> s.status == WhitelistStatus.APPROVED || s.status == WhitelistStatus.EXCELLENT)
                    .count();
            long rejectedCount = allStatus.values().stream()
                    .filter(s -> s.status == WhitelistStatus.REJECTED)
                    .count();

            // Informações do servidor MC
            String serverIp = BotConfig.getServerIp();
            int lobbyPort = BotConfig.getLobbyPort();

            ctx.json(Map.of(
                    "bot", Map.of(
                            "name", jda.getSelfUser().getName(),
                            "avatar", jda.getSelfUser().getAvatarUrl() != null
                                    ? jda.getSelfUser().getAvatarUrl() : "",
                            "status", jda.getStatus().name(),
                            "ping", jda.getGatewayPing(),
                            "uptime", uptime,
                            "guilds", totalGuilds,
                            "users", totalUsers,
                            "channels", totalChannels
                    ),
                    "memory", Map.of(
                            "used", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
                            "total", runtime.totalMemory() / 1024 / 1024,
                            "max", runtime.maxMemory() / 1024 / 1024
                    ),
                    "whitelist", Map.of(
                            "pending", pendingCount,
                            "approved", approvedCount,
                            "rejected", rejectedCount
                    ),
                    "server", Map.of(
                            "ip", serverIp != null ? serverIp : "N/A",
                            "port", String.valueOf(lobbyPort)
                    )
            ));
        });

        // GET /api/dashboard/uptime — uptime formatado
        app.get("/api/dashboard/uptime", ctx -> {
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            long seconds = uptime / 1000;
            long days = seconds / 86400;
            long hours = (seconds % 86400) / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;

            ctx.json(Map.of(
                    "raw", uptime,
                    "formatted", String.format("%dd %dh %dm %ds", days, hours, minutes, secs)
            ));
        });
    }
}
