package com.midgardbot.features;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.PunishmentManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PunishmentMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentMonitor.class);
    private final JDA jda;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public PunishmentMonitor(JDA jda) {
        this.jda = jda;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkTempBans, 1, 1, TimeUnit.MINUTES);
        LOGGER.info("PunishmentMonitor iniciado.");
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void checkTempBans() {
        try {
            long now = System.currentTimeMillis();
            Map<String, PunishmentManager.TempBan> activeTempBans = PunishmentManager.getActiveTempBans();

            for (PunishmentManager.TempBan tempBan : activeTempBans.values()) {
                if (now >= tempBan.endTime) {
                    unbanUser(tempBan);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao verificar tempbans", e);
        }
    }

    private void unbanUser(PunishmentManager.TempBan tempBan) {
        // We need to find the guild where the user was banned. 
        // Since TempBan doesn't store guildId (my bad), we'll try to unban from all guilds the bot is in.
        // Ideally, we should store guildId in TempBan.
        // For now, I'll iterate over all guilds.
        
        for (Guild guild : jda.getGuilds()) {
            guild.retrieveBanList().queue(bans -> {
                try {
                    boolean isBanned = bans.stream().anyMatch(ban -> ban.getUser().getId().equals(tempBan.userId));
                    if (isBanned) {
                        guild.unban(User.fromId(tempBan.userId)).reason("Tempban expirado").queue(
                            success -> {
                                PunishmentManager.removeTempBan(tempBan.userId);
                                logUnban(guild, tempBan.userId);
                            },
                            error -> LOGGER.error("Falha ao desbanir usuário {} no servidor {}: {}", tempBan.userId, guild.getName(), error.getMessage())
                        );
                    }
                } catch (Exception e) {
                    LOGGER.error("Erro ao processar lista de bans no servidor " + guild.getName(), e);
                }
            }, error -> LOGGER.error("Erro ao recuperar lista de bans no servidor " + guild.getName(), error));
        }
        
    }

    private void logUnban(Guild guild, String userId) {
        try {
            String channelId = BotConfig.getPunishmentChannelId();
            if (channelId != null) {
                TextChannel channel = guild.getTextChannelById(channelId);
                if (channel != null) {
                    jda.retrieveUserById(userId).queue(user -> {
                        channel.sendMessageEmbeds(
                            EmbedUtils.createSuccess("Tempban Expirado", user.getAsMention() + " foi desbanido automaticamente.", jda.getSelfUser()).build()
                        ).queue();
                    }, error -> {
                         channel.sendMessageEmbeds(
                            EmbedUtils.createSuccess("Tempban Expirado", "Usuário ID " + userId + " foi desbanido automaticamente.", jda.getSelfUser()).build()
                        ).queue();
                    });
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao logar unban", e);
        }
    }
}
