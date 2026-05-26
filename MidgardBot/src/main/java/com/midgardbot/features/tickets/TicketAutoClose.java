package com.midgardbot.features.tickets;

import com.midgardbot.commands.handlers.TicketHandler;
import com.midgardbot.config.BotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TicketAutoClose {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketAutoClose.class);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final JDA jda;

    public TicketAutoClose(JDA jda) {
        this.jda = jda;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkTickets, 1, 15, TimeUnit.MINUTES);
        LOGGER.info("Sistema de fechamento automatico de tickets iniciado.");
    }

    private void checkTickets() {
        try {
            for (Guild guild : jda.getGuilds()) {
                List<String> ticketCategories = new ArrayList<>();
                addIfValid(ticketCategories, BotConfig.getTicketCategorySupport());
                addIfValid(ticketCategories, BotConfig.getTicketCategoryReport());
                addIfValid(ticketCategories, BotConfig.getTicketCategoryBug());
                addIfValid(ticketCategories, BotConfig.getTicketCategoryLore());

                for (String catId : ticketCategories) {
                    Category category = guild.getCategoryById(catId);
                    if (category == null) {
                        continue;
                    }

                    for (TextChannel channel : category.getTextChannels()) {
                        checkChannelActivity(channel);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao verificar tickets para fechamento automatico", e);
        }
    }

    private void addIfValid(List<String> list, String id) {
        if (id != null && !id.isEmpty() && !id.equals("000000000000000000")) {
            list.add(id);
        }
    }

    private void checkChannelActivity(TextChannel channel) {
        try {
            if (channel.getName().startsWith("closed-")) {
                return;
            }

            channel.getHistory().retrievePast(20).queue(messages -> {
                try {
                    if (messages.isEmpty()) {
                        return;
                    }

                    Message lastAnyMsg = messages.get(0);
                    Message lastUserMsg = null;

                    for (Message msg : messages) {
                        if (!msg.getAuthor().isBot()) {
                            lastUserMsg = msg;
                            break;
                        }
                    }

                    String topic = channel.getTopic();
                    boolean warned = topic != null && topic.contains("| Warned:True");
                    boolean warned2 = topic != null && topic.contains("| Warned2:True");
                    OffsetDateTime now = OffsetDateTime.now();

                    if (warned && userRespondedRecently(lastUserMsg, now)) {
                        String newTopic = topic.replace("| Warned:True", "").replace("| Warned2:True", "");
                        channel.getManager().setTopic(newTopic).queue();
                        return;
                    }

                    if (warned2) {
                        long minutesSinceLast = Duration.between(lastAnyMsg.getTimeCreated(), now).toMinutes();
                        if (minutesSinceLast >= 10) {
                            LOGGER.info("Fechando ticket por inatividade: {}", channel.getName());
                            channel.sendMessage("⏳ **Fechamento por Inatividade**\nEste ticket sera fechado automaticamente por falta de resposta.")
                                .queue(message -> TicketHandler.closeTicket(channel, channel.getGuild(), null));
                        }
                        return;
                    }

                    if (warned) {
                        long minutesSinceLast = Duration.between(lastAnyMsg.getTimeCreated(), now).toMinutes();
                        if (minutesSinceLast >= 10) {
                            LOGGER.info("Segundo aviso de inatividade: {}", channel.getName());
                            channel.sendMessage("⚠️ **Segundo Aviso de Inatividade**\nEste ticket continua sem resposta. Se nao houver retorno, ele sera fechado automaticamente.")
                                .queue();

                            String newTopic = (topic == null ? "" : topic) + " | Warned2:True";
                            channel.getManager().setTopic(newTopic).queue();
                        }
                        return;
                    }

                    long hoursCheck = (lastUserMsg != null)
                        ? Duration.between(lastUserMsg.getTimeCreated(), now).toHours()
                        : Duration.between(lastAnyMsg.getTimeCreated(), now).toHours();

                    if (hoursCheck >= 24) {
                        LOGGER.info("Avisando ticket por inatividade: {}", channel.getName());
                        channel.sendMessage("⚠️ **Aviso de Inatividade**\nEste ticket esta sem interacao ha 24 horas. Se continuar assim, ele sera fechado automaticamente.")
                            .queue();

                        String newTopic = (topic == null ? "" : topic) + " | Warned:True";
                        channel.getManager().setTopic(newTopic).queue();
                    }
                } catch (Exception e) {
                    LOGGER.error("Erro ao processar historico do canal {}", channel.getName(), e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Erro ao verificar canal {}", channel.getName(), e);
        }
    }

    private boolean userRespondedRecently(Message lastUserMsg, OffsetDateTime now) {
        if (lastUserMsg == null) {
            return false;
        }

        long hoursSinceUser = Duration.between(lastUserMsg.getTimeCreated(), now).toHours();
        return hoursSinceUser < 24;
    }
}
