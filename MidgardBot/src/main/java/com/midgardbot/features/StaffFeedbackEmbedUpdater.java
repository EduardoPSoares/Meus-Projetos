package com.midgardbot.features;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.StaffFeedback;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StaffFeedbackEmbedUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaffFeedbackEmbedUpdater.class);
    private final JDA jda;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private String lastMessageId = null;
    
    private static StaffFeedbackEmbedUpdater instance;

    public StaffFeedbackEmbedUpdater(JDA jda) {
        this.jda = jda;
        instance = this;
    }
    
    public static void forceUpdate() {
        if (instance != null) {
            instance.updateEmbed();
        }
    }

    public void start() {
        LOGGER.info("Monitoramento de Feedback (Embed) iniciado.");
        // Atualiza a cada 5 minutos para garantir consistência
        scheduler.scheduleAtFixedRate(this::updateEmbed, 10, 300, TimeUnit.SECONDS);
    }
    
    public void stop() {
        scheduler.shutdown();
    }

    private void updateEmbed() {
        try {
            String channelId = BotConfig.getStaffFeedbackChannelId();
            if (channelId == null || channelId.isEmpty()) return;
            
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                LOGGER.warn("Canal de feedback staff não encontrado: " + channelId);
                return;
            }

            Map<String, List<StaffFeedback>> allFeedbacks = DataManager.getAllStaffFeedbacks();
            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("📊 Avaliações dos Staffs");
            embed.setDescription("Ranking baseado nas avaliações dos jogadores.");
            embed.setColor(Color.ORANGE);
            embed.setThumbnail(jda.getSelfUser().getAvatarUrl());
            embed.setTimestamp(Instant.now());
            embed.setFooter("Atualizado automaticamente", null);

            if (allFeedbacks.isEmpty()) {
                embed.setDescription("Ainda não há avaliações registradas.");
            } else {
                // Top 5 staff mais avaliados
                List<Map.Entry<String, List<StaffFeedback>>> topStaffs = allFeedbacks.entrySet().stream()
                        .sorted(Comparator.comparingInt(e -> -e.getValue().size()))
                        .limit(10)
                        .collect(Collectors.toList());

                for (Map.Entry<String, List<StaffFeedback>> entry : topStaffs) {
                    String staffId = entry.getKey();
                    List<StaffFeedback> feedbacks = entry.getValue();
                    double avg = feedbacks.stream().mapToInt(f -> f.rating).average().orElse(0.0);
                    
                    // Formatação de estrelas
                    StringBuilder stars = new StringBuilder();
                    int fullStars = (int) Math.round(avg);
                    for (int i = 0; i < 5; i++) {
                        if (i < fullStars) stars.append("⭐");
                        else stars.append("⚫");
                    }
                    
                    embed.addField(
                        "",
                        "<@" + staffId + ">\n" + stars.toString() + " **" + String.format("%.1f", avg) + "** (" + feedbacks.size() + " avaliações)",
                        false
                    );
                }
            }
            
            // Envia ou edita a mensagem
            if (lastMessageId != null) {
                channel.retrieveMessageById(lastMessageId).queue(
                    msg -> msg.editMessageEmbeds(embed.build()).queue(),
                    err -> {
                        lastMessageId = null;
                        findOrCreateMessage(channel, embed);
                    }
                );
            } else {
                findOrCreateMessage(channel, embed);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar embed de feedback", e);
        }
    }

    private void findOrCreateMessage(TextChannel channel, EmbedBuilder embed) {
        channel.getHistory().retrievePast(10).queue(messages -> {
            try {
                for (Message msg : messages) {
                    if (msg.getAuthor().equals(jda.getSelfUser())) {
                        if (!msg.getEmbeds().isEmpty() && msg.getEmbeds().get(0).getTitle() != null && msg.getEmbeds().get(0).getTitle().contains("Avaliações dos Staffs")) {
                            lastMessageId = msg.getId();
                            msg.editMessageEmbeds(embed.build()).queue();
                            return;
                        }
                    }
                }
                // Se não achou, cria nova
                channel.sendMessageEmbeds(embed.build()).queue(msg -> lastMessageId = msg.getId());
            } catch (Exception e) {
                LOGGER.error("Erro ao processar histórico de mensagens de feedback", e);
            }
        }, error -> LOGGER.error("Erro ao recuperar histórico de mensagens de feedback", error));
    }
}
