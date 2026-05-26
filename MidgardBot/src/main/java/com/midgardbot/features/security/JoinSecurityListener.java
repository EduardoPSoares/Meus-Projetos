package com.midgardbot.features.security;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.Queue;

public class JoinSecurityListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JoinSecurityListener.class);
    
    // Anti-Fake Config
    private static final int MIN_ACCOUNT_AGE_DAYS = 30;
    
    // Anti-Raid Config
    private static final int RAID_TRIGGER_COUNT = 10; // 10 joins
    private static final int RAID_TRIGGER_WINDOW = 60; // in 60 seconds
    
    private final Queue<Long> joinTimestamps = new LinkedList<>();
    private boolean raidModeEnabled = false;

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Member member = event.getMember();
        
        // 1. Anti-Fake (Account Age)
        long daysCreated = ChronoUnit.DAYS.between(member.getUser().getTimeCreated(), OffsetDateTime.now());
        if (daysCreated < MIN_ACCOUNT_AGE_DAYS && !DataManager.isAntiFakeBypass(member.getId())) {
            // Kick user
            try {
                member.kick().reason("Anti-Fake: Conta muito nova (" + daysCreated + " dias). Mínimo: " + MIN_ACCOUNT_AGE_DAYS + " dias.").queue();
                logSecurityAction(event, "🛡️ Anti-Fake", "Usuário " + member.getAsMention() + " expulso.\nConta criada há " + daysCreated + " dias.");
            } catch (Exception e) {
                LOGGER.error("Falha ao expulsar usuário fake", e);
            }
            return; // Don't process raid check if kicked
        }

        // 2. Anti-Raid (Join Rate)
        checkRaid(event);
    }

    private void checkRaid(GuildMemberJoinEvent event) {
        long now = System.currentTimeMillis();
        
        synchronized (joinTimestamps) {
            joinTimestamps.add(now);
            
            // Remove timestamps older than window
            while (!joinTimestamps.isEmpty() && now - joinTimestamps.peek() > RAID_TRIGGER_WINDOW * 1000) {
                joinTimestamps.poll();
            }
            
            if (joinTimestamps.size() >= RAID_TRIGGER_COUNT && !raidModeEnabled) {
                raidModeEnabled = true;
                // Trigger Lockdown or Alert
                logSecurityAction(event, "🚨 ANTI-RAID DETECTADO", "Mais de " + RAID_TRIGGER_COUNT + " usuários entraram em " + RAID_TRIGGER_WINDOW + " segundos.\n**Verifique o servidor imediatamente!**");
                // Optional: Auto-Lockdown logic here or call LockdownCommand
            } else if (joinTimestamps.size() < RAID_TRIGGER_COUNT / 2 && raidModeEnabled) {
                raidModeEnabled = false; // Reset flag when calm
            }
        }
    }

    private void logSecurityAction(GuildMemberJoinEvent event, String title, String description) {
        String channelId = BotConfig.get("SECURITY_CHANNEL_ID");
        if (channelId != null && !channelId.isEmpty()) {
            TextChannel channel = event.getGuild().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(
                    EmbedUtils.createError(title, description, event.getJDA().getSelfUser()).build()
                ).queue();
            }
        }
    }
}
