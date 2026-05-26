package com.midgardbot.features.whitelist;

import com.google.gson.Gson;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ReviewPanelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewPanelManager.class);
    private static final File PANEL_FILE = new File("data/review_panel.json");
    private static final Gson GSON = new Gson();
    
    // Debounce Scheduler
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> pendingUpdate;

    private static PanelInfo panelInfo;

    public static class PanelInfo {
        public String channelId;
        public String messageId;

        public PanelInfo(String channelId, String messageId) {
            this.channelId = channelId;
            this.messageId = messageId;
        }
    }

    static {
        load();
    }

    public static void setPanel(String channelId, String messageId) {
        panelInfo = new PanelInfo(channelId, messageId);
        save();
    }

    public static synchronized void updatePanel(JDA jda) {
        if (panelInfo == null) {
            load();
            if (panelInfo == null) {
                LOGGER.warn("Nao foi possivel atualizar o painel de review: Configuracao nao encontrada (review_panel.json).");
                return;
            }
        }

        // Cancela atualização anterior se ainda estiver pendente (Debounce)
        if (pendingUpdate != null && !pendingUpdate.isDone()) {
            pendingUpdate.cancel(false);
        }

        // Agenda nova atualização para daqui a 2 segundos
        pendingUpdate = scheduler.schedule(() -> {
            performUpdate(jda);
        }, 2, TimeUnit.SECONDS);
    }

    private static void performUpdate(JDA jda) {
        if (panelInfo == null) return;

        TextChannel channel = jda.getTextChannelById(panelInfo.channelId);
        if (channel == null) return;

        channel.retrieveMessageById(panelInfo.messageId).queue(msg -> {
            var allStatus = DataManager.getAllStatus();
            var pendingStatuses = Set.of(
                    WhitelistStatus.PENDING, WhitelistStatus.REVIEWING,
                    WhitelistStatus.NEEDS_REVIEW, WhitelistStatus.FLAGGED,
                    WhitelistStatus.PRIORITY, WhitelistStatus.STANDBY
            );
            // Contar apenas pendentes reais (com respostas preenchidas no status)
            int pendingCount = (int) allStatus.values().stream()
                    .filter(s -> pendingStatuses.contains(s.status))
                    .filter(s -> s.answers != null && !s.answers.isEmpty() && !"{}".equals(s.answers))
                    .count();
            Map<String, String> activeReviews = ReviewManager.getActiveReviews();
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🛡️ ✦ CENTRAL DE ANÁLISE ✦")
                .setDescription("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n" +
                        "👋 **Bem-vindo ao painel de gerenciamento de Whitelists.**\n" +
                        "📝 Utilize este painel para revisar as aplicações pendentes.\n\n" +
                        "❓ **» COMO FUNCIONA**\n" +
                        "1️⃣ Clique em **Iniciar Análise** para puxar uma aplicação.\n" +
                        "2️⃣ O sistema entrará em **Modo Foco**.\n" +
                        "3️⃣ Revise as respostas e decida o veredito.\n" +
                        "4️⃣ Continue para a próxima aplicação.\n" +
                        "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
                .setColor(java.awt.Color.decode("#FFAA00"))
                .addField("📊 » STATUS ATUAL", 
                    (pendingCount > 0 ? "• Pendentes: **" + pendingCount + "**" : "• Pendentes: **0** (Tudo limpo!)"), 
                    true);

            String reviewers = "Ninguém";
            if (!activeReviews.isEmpty()) {
                reviewers = activeReviews.values().stream()
                    .distinct()
                    .map(id -> "<@" + id + ">")
                    .collect(java.util.stream.Collectors.joining(", "));
            }
            embed.addField("👀 » EM ANÁLISE", "• Staffs: " + reviewers, true);
            
            embed.setFooter("MidgardBot • Sistema de Whitelist", null);
            embed.setTimestamp(java.time.Instant.now());

            msg.editMessageEmbeds(embed.build())
               .setActionRow(Button.primary("btn_start_review", "🧐 Iniciar Análise (" + pendingCount + ")")
                   .withDisabled(pendingCount == 0))
               .queue(null, e -> LOGGER.error("Erro ao atualizar painel de review", e));

        }, e -> {
            LOGGER.warn("Mensagem do painel de review não encontrada (foi deletada?)");
        });
    }

    private static void load() {
        if (!PANEL_FILE.exists()) return;
        try (Reader reader = new FileReader(PANEL_FILE)) {
            panelInfo = GSON.fromJson(reader, PanelInfo.class);
        } catch (IOException e) {
            LOGGER.error("Erro ao carregar config do painel", e);
        }
    }

    private static void save() {
        try {
            if (!PANEL_FILE.getParentFile().exists()) {
                PANEL_FILE.getParentFile().mkdirs();
            }
            try (Writer writer = new FileWriter(PANEL_FILE)) {
                GSON.toJson(panelInfo, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar config do painel", e);
        }
    }
}
