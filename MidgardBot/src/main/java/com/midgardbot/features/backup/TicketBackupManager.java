package com.midgardbot.features.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.midgardbot.config.BotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TicketBackupManager {

    private static final Logger logger = LoggerFactory.getLogger(TicketBackupManager.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String BACKUP_DIR = "backups/tickets";
    private static final DateTimeFormatter DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private JDA jda;

    public TicketBackupManager(JDA jda) {
        this.jda = jda;
    }
    
    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    public void startScheduler() {
        // Agenda para rodar a cada 1 hora
        scheduler.scheduleAtFixedRate(this::backupAllTickets, 1, 1, TimeUnit.HOURS);
        logger.info("Agendador de Backup de Tickets iniciado (1 hora).");
    }

    public String backupAllTickets() {
        if (jda == null) {
            logger.warn("Backup de tickets ignorado: JDA ainda não está disponível.");
            return "jda_unavailable";
        }

        String timestamp = LocalDateTime.now().format(DIR_FORMATTER);
        File backupFolder = new File(BACKUP_DIR + "/" + timestamp);
        
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        List<String> categories = new ArrayList<>();
        if (BotConfig.getTicketCategorySupport() != null) categories.add(BotConfig.getTicketCategorySupport());
        if (BotConfig.getTicketCategoryReport() != null) categories.add(BotConfig.getTicketCategoryReport());
        if (BotConfig.getTicketCategoryBug() != null) categories.add(BotConfig.getTicketCategoryBug());
        if (BotConfig.getTicketCategoryLore() != null) categories.add(BotConfig.getTicketCategoryLore());
        if (BotConfig.getTicketCategoryLog() != null) categories.add(BotConfig.getTicketCategoryLog());

        int count = 0;

        for (String catId : categories) {
            net.dv8tion.jda.api.entities.channel.concrete.Category category = jda.getCategoryById(catId);
            if (category == null) continue;

            for (GuildChannel channel : category.getChannels()) {
                if (channel instanceof TextChannel) {
                    TextChannel textChannel = (TextChannel) channel;
                    try {
                        backupChannel(textChannel, backupFolder);
                        count++;
                    } catch (Exception e) {
                        logger.error("Erro ao fazer backup do canal " + textChannel.getName(), e);
                    }
                }
            }
        }
        
        logger.info("Backup de tickets concluído. " + count + " canais salvos em " + backupFolder.getPath());
        return timestamp;
    }

    private void backupChannel(TextChannel channel, File folder) throws IOException {
        List<Message> messages = channel.getIterableHistory().takeAsync(1000).join(); // Limite de 1000 msgs por segurança
        List<BackupMessage> backupMessages = new ArrayList<>();

        // Inverte para ficar na ordem cronológica (mais antigo primeiro)
        List<Message> reversed = new ArrayList<>(messages);
        Collections.reverse(reversed);

        for (Message msg : reversed) {
            backupMessages.add(new BackupMessage(
                msg.getAuthor().getName(),
                msg.getAuthor().getId(),
                msg.getContentRaw(),
                msg.getTimeCreated().toString(),
                msg.getEmbeds().isEmpty() ? null : msg.getEmbeds().get(0).toData().toString() // Simplificação para 1 embed
            ));
        }

        File file = new File(folder, channel.getId() + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(backupMessages, writer);
        }
    }

    public void restoreTicket(String backupId, String channelId, TextChannel targetChannel) {
        File file = new File(BACKUP_DIR + "/" + backupId + "/" + channelId + ".json");
        if (!file.exists()) {
            targetChannel.sendMessage("❌ Backup não encontrado para este canal nesta data.").queue();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<BackupMessage>>(){}.getType();
            List<BackupMessage> messages = gson.fromJson(reader, listType);

            targetChannel.sendMessage("🔄 **Iniciando Rollback/Restauração...**").queue();

            for (BackupMessage msg : messages) {
                String content = "**" + msg.authorName + "**: " + msg.content;
                if (content.length() > 2000) content = content.substring(0, 2000);

                if (msg.embedJson != null) {
                    // Reconstrução básica de embed não é trivial via JSON cru, 
                    // então vamos apenas avisar que tinha um embed.
                    content += "\n*[Embed anexado]*";
                }

                targetChannel.sendMessage(content).queue();
                // Pequeno delay para evitar rate limit
                try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

            targetChannel.sendMessage("✅ **Restauração concluída.**").queue();

        } catch (IOException e) {
            targetChannel.sendMessage("❌ Erro ao ler arquivo de backup: " + e.getMessage()).queue();
            logger.error("Erro ao restaurar ticket", e);
        }
    }

    // Classe interna para estrutura do JSON
    private static class BackupMessage {
        String authorName;
        // String authorId;
        String content;
        // String timestamp;
        String embedJson;

        public BackupMessage(String authorName, String authorId, String content, String timestamp, String embedJson) {
            this.authorName = authorName;
            // this.authorId = authorId;
            this.content = content;
            // this.timestamp = timestamp;
            this.embedJson = embedJson;
        }
    }
}
