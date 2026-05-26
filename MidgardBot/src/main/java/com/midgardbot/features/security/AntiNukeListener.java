package com.midgardbot.features.security;

import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class AntiNukeListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AntiNukeListener.class);

    // Limites: X ações em Y segundos
    private static final int LIMIT_CHANNEL_DELETE = 3;
    private static final int LIMIT_ROLE_DELETE = 3;
    private static final int LIMIT_BAN = 5;
    private static final int TIME_WINDOW = 10; // segundos

    private final Map<String, Queue<Long>> channelDeleteTracker = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Queue<Long>> roleDeleteTracker = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Queue<Long>> banTracker = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        checkAuditLog(event.getGuild(), ActionType.CHANNEL_DELETE, event.getChannel().getId(), channelDeleteTracker, LIMIT_CHANNEL_DELETE, "Deleção de Canais em Massa");
    }

    @Override
    public void onRoleDelete(@NotNull RoleDeleteEvent event) {
        checkAuditLog(event.getGuild(), ActionType.ROLE_DELETE, event.getRole().getId(), roleDeleteTracker, LIMIT_ROLE_DELETE, "Deleção de Cargos em Massa");
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        checkAuditLog(event.getGuild(), ActionType.BAN, event.getUser().getId(), banTracker, LIMIT_BAN, "Banimento em Massa");
    }

    private void checkAuditLog(Guild guild, ActionType type, String targetId, Map<String, Queue<Long>> tracker, int limit, String reason) {
        guild.retrieveAuditLogs().type(type).limit(10).queue(logs -> {
            AuditLogEntry entry = logs.stream()
                .filter(e -> e.getTargetId().equals(targetId))
                .findFirst()
                .orElse(null);

            if (entry == null) return;
            
            User user = entry.getUser();
            if (user == null || user.getId().equals(guild.getSelfMember().getId())) return; // Ignorar o próprio bot

            // Verificar Bypass Role
            String bypassRoleId = BotConfig.get("ANTINUKE_BYPASS_ROLE_ID");
            if (bypassRoleId != null && !bypassRoleId.isEmpty()) {
                Member member = guild.getMember(user);
                if (member != null && member.getRoles().stream().anyMatch(r -> r.getId().equals(bypassRoleId))) {
                    return; // Usuário tem cargo de bypass
                }
            }

            String userId = user.getId();
            long now = System.currentTimeMillis();

            synchronized (tracker) {
                tracker.putIfAbsent(userId, new LinkedList<>());
                Queue<Long> timestamps = tracker.get(userId);
                timestamps.add(now);

                // Limpar antigos
                while (!timestamps.isEmpty() && now - timestamps.peek() > TIME_WINDOW * 1000) {
                    timestamps.poll();
                }

                if (timestamps.size() >= limit) {
                    takeAction(guild, user, reason);
                    timestamps.clear(); // Resetar para evitar spam de punições
                }
            }
        }, error -> LOGGER.error("Falha ao recuperar audit logs", error));
    }

    private void takeAction(Guild guild, User user, String reason) {
        // Banir usuário
        guild.ban(user, 7, TimeUnit.DAYS).reason("ANTI-NUKE: " + reason).queue(
            success -> logAction(guild, user, reason, true),
            error -> logAction(guild, user, reason, false)
        );
    }

    private void logAction(Guild guild, User user, String reason, boolean success) {
        String channelId = BotConfig.get("SECURITY_CHANNEL_ID");
        if (channelId != null) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel != null) {
                String description = "**Usuário:** " + user.getAsMention() + " (" + user.getId() + ")\n" +
                                     "**Motivo:** " + reason + "\n" +
                                     "**Ação:** " + (success ? "✅ Banido com sucesso" : "❌ Falha ao banir (Verifique permissões)");
                
                channel.sendMessageEmbeds(
                    EmbedUtils.createError("☢️ ANTI-NUKE ATIVADO", description, guild.getJDA().getSelfUser()).build()
                ).queue();
            }
        }
    }
}
