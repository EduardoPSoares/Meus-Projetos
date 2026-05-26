package com.midgardbot.features.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.midgardbot.data.PunishmentManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BotSyncTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotSyncTask.class);
    private static final File QUEUE_FOLDER = new File("data/bot_queue");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final JDA jda;

    public BotSyncTask(JDA jda) {
        this.jda = jda;
        if (!QUEUE_FOLDER.exists()) QUEUE_FOLDER.mkdirs();
    }

    @Override
    public void run() {
        if (!QUEUE_FOLDER.exists()) return;

        File[] files = QUEUE_FOLDER.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return;

        for (File file : files) {
            try {
                processFile(file);
            } catch (Exception e) {
                LOGGER.error("Erro ao processar arquivo de bot sync: " + file.getName(), e);
            }
        }
    }

    private void processFile(File file) {
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);
            
            String action = (String) data.get("action"); // BAN, KICK, WARN
            String discordId = (String) data.get("discordId");
            String reason = (String) data.get("reason");
            String moderator = (String) data.get("moderator");

            if (discordId != null) {
                try {
                    User user = jda.retrieveUserById(discordId).complete();
                    if (user != null) {
                        if (action.equalsIgnoreCase("BAN")) {
                            // Ban on Discord
                            jda.getGuilds().forEach(guild -> {
                                guild.retrieveMember(user).queue(member -> {
                                    try {
                                        if (member != null) {
                                            guild.ban(user, 0, TimeUnit.DAYS).reason(reason).queue();
                                        }
                                    } catch (Exception e) {
                                        LOGGER.error("Erro ao banir membro no servidor " + guild.getName(), e);
                                    }
                                }, error -> {});
                            });
                            PunishmentManager.addBan(discordId, "Minecraft:" + moderator, reason);
                        } else if (action.equalsIgnoreCase("KICK")) {
                            jda.getGuilds().forEach(guild -> {
                                guild.retrieveMember(user).queue(member -> {
                                    try {
                                        if (member != null) {
                                            guild.kick(member).reason(reason).queue();
                                        }
                                    } catch (Exception e) {
                                        LOGGER.error("Erro ao kickar membro no servidor " + guild.getName(), e);
                                    }
                                }, error -> {});
                            });
                        } else if (action.startsWith("WARN")) {
                            PunishmentManager.addWarn(discordId, "Minecraft:" + moderator, reason);
                            user.openPrivateChannel().queue(pc -> {
                                try {
                                    pc.sendMessageEmbeds(
                                        EmbedUtils.createWarning("Você recebeu uma advertência (In-Game)", "Motivo: " + reason, jda.getSelfUser()).build()
                                    ).queue();
                                } catch (Exception e) {
                                    LOGGER.error("Erro ao enviar DM de warn", e);
                                }
                            }, error -> LOGGER.warn("Não foi possível enviar DM para " + user.getName()));
                            
                            // Log no canal de punições
                            logPunishment(user, action, reason, moderator);
                        } else if (action.equalsIgnoreCase("UNBAN")) {
                            jda.getGuilds().forEach(guild -> {
                                guild.unban(user).queue(
                                    success -> LOGGER.info("Desbanido no Discord: " + user.getName()),
                                    error -> LOGGER.warn("Erro ao desbanir no Discord: " + error.getMessage())
                                );
                            });
                            PunishmentManager.removeBan(discordId);
                            logPunishment(user, "Unban", reason, moderator);
                        } else if (action.equalsIgnoreCase("BAN-IP")) {
                             PunishmentManager.createPunishment(discordId, user.getName(), discordId, PunishmentManager.PunishmentType.IP_BAN, reason, moderator, moderator, -1);
                             logPunishment(user, "IP-Ban", reason, moderator);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Usuario nao encontrado no Discord: " + discordId);
                }
            }
            
            LOGGER.info("Bot Sync processado: " + file.getName());
            // Only delete file after successful processing
            if (!file.delete()) {
                LOGGER.warn("Não foi possível deletar o arquivo de bot sync: " + file.getName());
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao ler arquivo: " + file.getName(), e);
            // Move failed file to error directory to prevent data loss
            File errorDir = new File(QUEUE_FOLDER, "errors");
            if (!errorDir.exists()) errorDir.mkdirs();
            File errorFile = new File(errorDir, file.getName());
            if (!file.renameTo(errorFile)) {
                LOGGER.error("Falha ao mover arquivo com erro para pasta de erros: " + file.getName());
            }
        }
    }

    private void logPunishment(User target, String type, String reason, String moderator) {
        try {
            String channelId = com.midgardbot.config.BotConfig.getPunishmentChannelId();
            if (channelId != null) {
                net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(channelId);
                if (channel != null) {
                    channel.sendMessageEmbeds(
                        EmbedUtils.createEmbed("🔨 Punição Sincronizada: " + type, "", EmbedUtils.COLOR_WARNING)
                            .addField("Usuário", target.getAsMention() + " (" + target.getId() + ")", false)
                            .addField("Moderador", moderator + " (In-Game)", false)
                            .addField("Motivo", reason, false)
                            .setFooter("Sincronizado do Minecraft")
                            .setTimestamp(java.time.Instant.now())
                            .build()
                    ).queue();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao logar punição de sync", e);
        }
    }
}
