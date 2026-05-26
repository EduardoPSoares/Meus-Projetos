package com.midgardbot.features.tickets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DatabaseManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TicketArchiver {

    private static final Logger logger = LoggerFactory.getLogger(TicketArchiver.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private JDA jda;

    public TicketArchiver(JDA jda) {
        this.jda = jda;
    }
    
    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    public void start() {
        if (jda == null) {
            logger.warn("TicketArchiver.start() chamado sem JDA configurado.");
            return;
        }

        // Verifica a cada 10 minutos se há tickets para arquivar (regra de 1 hora)
        scheduler.scheduleAtFixedRate(this::checkAndArchiveTickets, 1, 10, TimeUnit.MINUTES);
        
        // Verifica diariamente a limpeza do banco de dados (regra de 1 mês)
        scheduler.scheduleAtFixedRate(this::cleanupDatabase, 1, 24, TimeUnit.HOURS);
        
        logger.info("Sistema de Arquivamento de Tickets iniciado.");
    }

    private void checkAndArchiveTickets() {
        if (jda == null) return;

        String logCategoryId = BotConfig.getTicketCategoryLog();
        if (logCategoryId == null) return;

        net.dv8tion.jda.api.entities.channel.concrete.Category category = jda.getCategoryById(logCategoryId);
        if (category == null) return;

        for (GuildChannel channel : category.getChannels()) {
            if (channel instanceof TextChannel) {
                TextChannel textChannel = (TextChannel) channel;
                
                // Verifica se o canal foi criado ou movido há mais de 1 hora
                // Como JDA não tem "movedAt", usamos a última mensagem ou criação
                // Se a última mensagem for > 1 hora, arquiva.
                
                textChannel.getHistory().retrievePast(1).queue(messages -> {
                    if (messages.isEmpty()) {
                        // Canal vazio? Deleta direto ou verifica criação
                        if (textChannel.getTimeCreated().isBefore(OffsetDateTime.now().minusHours(1))) {
                            archiveAndDelete(textChannel);
                        }
                    } else {
                        Message lastMsg = messages.get(0);
                        if (lastMsg.getTimeCreated().isBefore(OffsetDateTime.now().minusHours(1))) {
                            archiveAndDelete(textChannel);
                        }
                    }
                });
            }
        }
    }

    public void archiveTicket(TextChannel channel, net.dv8tion.jda.api.entities.User closer) {
        logger.info("Arquivando ticket via comando: " + channel.getName());
        
        // Coleta mensagens
        channel.getIterableHistory().takeAsync(1000).thenAccept(messages -> {
            List<TicketMessage> chatLog = new ArrayList<>();
            List<net.dv8tion.jda.api.entities.Message> reversed = new ArrayList<>(messages);
            Collections.reverse(reversed);

            for (net.dv8tion.jda.api.entities.Message msg : reversed) {
                chatLog.add(new TicketMessage(
                    msg.getAuthor().getName(),
                    msg.getAuthor().getId(),
                    msg.getContentRaw(),
                    msg.getTimeCreated().toString()
                ));
            }

            String jsonContent = gson.toJson(chatLog);
            String priority = "NORMAL";
            String categoryName = resolveCategoryName(channel);
            
            if (channel.getTopic() != null && channel.getTopic().contains("[HIGH-PRIORITY]")) {
                priority = "HIGH";
            }

            // Tenta extrair user_id, ticket_id e staff do tópico
            String userId = null;
            int ticketId = -1;
            String claimedBy = null;
            String collaboratorIds = null;

            if (channel.getTopic() != null) {
                String[] parts = channel.getTopic().split("\\|");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.startsWith("OwnerID:")) {
                        userId = p.substring("OwnerID:".length());
                    } else if (p.startsWith("TicketID:")) {
                        try {
                            ticketId = Integer.parseInt(p.substring("TicketID:".length()));
                        } catch (NumberFormatException ignored) {}
                    } else if (p.startsWith("MainStaff:")) {
                        claimedBy = p.substring("MainStaff:".length());
                    }
                }
                collaboratorIds = extractCollaboratorIds(channel.getTopic());
            }

            if (ticketId > 0) {
                DatabaseManager.updateTicketCategory(ticketId, categoryName);
                DatabaseManager.updateTicket(ticketId, jsonContent, priority, claimedBy, collaboratorIds);
            } else {
                saveToDatabase(channel.getName(), userId, categoryName, priority, jsonContent, claimedBy, collaboratorIds);
            }
            
            // Deleta o canal após 5 segundos para garantir que tudo foi processado
            channel.delete().queueAfter(5, TimeUnit.SECONDS);
        });
    }

    private void archiveAndDelete(TextChannel channel) {
        logger.info("Arquivando ticket: " + channel.getName());
        
        // Coleta mensagens
        channel.getIterableHistory().takeAsync(1000).thenAccept(messages -> {
            List<TicketMessage> chatLog = new ArrayList<>();
            List<Message> reversed = new ArrayList<>(messages);
            Collections.reverse(reversed);

            for (Message msg : reversed) {
                chatLog.add(new TicketMessage(
                    msg.getAuthor().getName(),
                    msg.getAuthor().getId(),
                    msg.getContentRaw(),
                    msg.getTimeCreated().toString()
                ));
            }

            String jsonContent = gson.toJson(chatLog);
            String priority = "NORMAL";
            String categoryName = resolveCategoryName(channel);
            
            // Verifica prioridade pelo tópico do canal
            if (channel.getTopic() != null && channel.getTopic().contains("[HIGH-PRIORITY]")) {
                priority = "HIGH";
            }

            // Tenta extrair user_id, ticket_id e staff do tópico
            String userId = null;
            int ticketId = -1;
            String claimedBy = null;
            String collaboratorIds = null;

            if (channel.getTopic() != null) {
                String[] parts = channel.getTopic().split("\\|");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.startsWith("OwnerID:")) {
                        userId = p.substring("OwnerID:".length());
                    } else if (p.startsWith("TicketID:")) {
                        try {
                            ticketId = Integer.parseInt(p.substring("TicketID:".length()));
                        } catch (NumberFormatException ignored) {}
                    } else if (p.startsWith("MainStaff:")) {
                        claimedBy = p.substring("MainStaff:".length());
                    }
                }
                collaboratorIds = extractCollaboratorIds(channel.getTopic());
            }

            if (ticketId > 0) {
                DatabaseManager.updateTicketCategory(ticketId, categoryName);
                DatabaseManager.updateTicket(ticketId, jsonContent, priority, claimedBy, collaboratorIds);
            } else {
                saveToDatabase(channel.getName(), userId, categoryName, priority, jsonContent, claimedBy, collaboratorIds);
            }
            
            // Deleta o canal
            channel.delete().queue();
        });
    }

    private void saveToDatabase(String channelName, String userId, String categoryName, String priority, String content, String claimedBy, String collaboratorIds) {
        if (!DatabaseManager.isConnected()) return;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO midgard_tickets (channel_name, user_id, category_name, priority, content, claimed_by, collaborator_ids, closed_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)")) {
            
            ps.setString(1, channelName);
            ps.setString(2, userId);
            ps.setString(3, categoryName);
            ps.setString(4, priority);
            ps.setString(5, content);
            ps.setString(6, claimedBy);
            ps.setString(7, collaboratorIds);
            ps.executeUpdate();
            
        } catch (Exception e) {
            logger.error("Erro ao salvar ticket no banco", e);
        }
    }

    private void cleanupDatabase() {
        if (!DatabaseManager.isConnected()) return;
        logger.info("Executando limpeza automática de tickets antigos...");

        int deleted = cleanupTicketsOlderThan(30);
        if (deleted > 0) {
            logger.info("Limpeza concluída: {} tickets expirados removidos.", deleted);
        }
    }

    // --- Métodos Públicos para Comandos ---

    public List<String> getTicketsByUser(String userId) {
        List<String> results = new ArrayList<>();
        if (!DatabaseManager.isConnected() || userId == null || userId.isBlank()) return results;

        String sql = "SELECT id, channel_name, category_name, priority, closed_at FROM midgard_tickets " +
                "WHERE user_id = ? AND closed_at IS NOT NULL ORDER BY id DESC LIMIT 20";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String categoryName = rs.getString("category_name");
                results.add(String.format("#%d - %s%s [%s] (%s)",
                        rs.getInt("id"),
                        rs.getString("channel_name"),
                        categoryName != null && !categoryName.isBlank() ? " {" + categoryName + "}" : "",
                        rs.getString("priority"),
                        rs.getString("closed_at")
                ));
            }
        } catch (Exception e) {
            logger.error("Erro ao listar tickets do usuário {}", userId, e);
        }
        return results;
    }
    
    public File generateTicketTranscript(int id) {
        if (!DatabaseManager.isConnected()) return null;
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM midgard_tickets WHERE id = ?")) {
            
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String channelName = rs.getString("channel_name");
                String categoryName = rs.getString("category_name");
                String contentJson = rs.getString("content");
                String priority = rs.getString("priority");
                String date = rs.getString("closed_at");
                
                TicketMessage[] messages = gson.fromJson(contentJson, TicketMessage[].class);
                
                File tempFile = File.createTempFile("ticket-" + id + "-", ".txt");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write("Ticket ID: " + id + "\n");
                    writer.write("Canal: " + channelName + "\n");
                    if (categoryName != null && !categoryName.isBlank()) {
                        writer.write("Categoria: " + categoryName + "\n");
                    }
                    writer.write("Prioridade: " + priority + "\n");
                    writer.write("Fechado em: " + date + "\n");
                    writer.write("--------------------------------------------------\n\n");
                    
                    for (TicketMessage msg : messages) {
                        writer.write("[" + msg.timestamp + "] " + msg.author + ": " + msg.content + "\n");
                    }
                }
                return tempFile;
            }
        } catch (Exception e) {
            logger.error("Erro ao gerar transcript", e);
        }
        return null;
    }
    
    public int cleanTicketsManually(int days) {
        if (!DatabaseManager.isConnected()) return 0;
        return cleanupTicketsOlderThan(days);
    }
    
    public boolean deleteHighPriorityTicket(int id) {
        if (!DatabaseManager.isConnected()) return false;
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM midgard_tickets WHERE id = ? AND priority = 'HIGH'")) {
            
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Erro ao deletar ticket high priority", e);
            return false;
        }
    }
    
    public List<String> listTickets(String userId) {
        List<String> results = new ArrayList<>();
        if (!DatabaseManager.isConnected()) return results;
        
        // Se userId for null, lista todos (limitado)
        String sql = "SELECT id, channel_name, category_name, priority, closed_at FROM midgard_tickets " +
                (userId != null && !userId.isBlank()
                        ? "WHERE user_id = ? AND closed_at IS NOT NULL ORDER BY id DESC LIMIT 20"
                        : "WHERE closed_at IS NOT NULL ORDER BY id DESC LIMIT 20");
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (userId != null && !userId.isBlank()) {
                ps.setString(1, userId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String categoryName = rs.getString("category_name");
                results.add(String.format("#%d - %s%s [%s] (%s)",
                    rs.getInt("id"), 
                    rs.getString("channel_name"),
                    categoryName != null && !categoryName.isBlank() ? " {" + categoryName + "}" : "",
                    rs.getString("priority"),
                    rs.getString("closed_at")
                ));
            }
        } catch (Exception e) {
            logger.error("Erro ao listar tickets", e);
        }
        return results;
    }

    public boolean deleteTicket(int id) {
        if (!DatabaseManager.isConnected()) return false;
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM midgard_tickets WHERE id = ?")) {
            
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Erro ao deletar ticket", e);
            return false;
        }
    }

    public TicketData getTicket(int id) {
        if (!DatabaseManager.isConnected()) return null;
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM midgard_tickets WHERE id = ? AND closed_at IS NOT NULL")) {
            
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                TicketData data = new TicketData();
                data.id = rs.getInt("id");
                data.channelName = rs.getString("channel_name");
                data.userId = rs.getString("user_id");
                data.categoryName = rs.getString("category_name");
                data.claimedBy = rs.getString("claimed_by");
                data.collaboratorIds = parseCollaboratorIds(rs.getString("collaborator_ids"));
                data.priority = rs.getString("priority");
                data.closedAt = rs.getString("closed_at");
                data.messages = gson.fromJson(rs.getString("content"), TicketMessage[].class);
                return data;
            }
        } catch (Exception e) {
            logger.error("Erro ao recuperar ticket", e);
        }
        return null;
    }

    private String resolveCategoryName(TextChannel channel) {
        String categoryFromTopic = extractTopicField(channel.getTopic(), "Category:");
        if (categoryFromTopic != null && !categoryFromTopic.isBlank()) {
            return categoryFromTopic;
        }
        return channel.getParentCategory() != null ? channel.getParentCategory().getName() : null;
    }

    private String extractTopicField(String topic, String fieldPrefix) {
        if (topic == null || topic.isBlank()) return null;
        for (String rawPart : topic.split("\\|")) {
            String part = rawPart.trim();
            if (part.startsWith(fieldPrefix)) {
                return part.substring(fieldPrefix.length());
            }
        }
        return null;
    }

    private String extractCollaboratorIds(String topic) {
        if (topic == null || topic.isBlank()) return null;

        List<String> collaboratorIds = new ArrayList<>();
        for (String rawPart : topic.split("\\|")) {
            String part = rawPart.trim();
            if (part.startsWith("Collab:")) {
                collaboratorIds.add(part.substring("Collab:".length()));
            }
        }

        return collaboratorIds.isEmpty() ? null : String.join(",", collaboratorIds);
    }

    private String[] parseCollaboratorIds(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return new String[0];
        }

        return java.util.Arrays.stream(rawValue.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toArray(String[]::new);
    }

    private int cleanupTicketsOlderThan(int days) {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) return 0;

            boolean isSQLite = conn.getMetaData().getDriverName().toLowerCase().contains("sqlite");
            String selectSql;

            if (isSQLite) {
                selectSql = "SELECT id FROM midgard_tickets WHERE priority = 'NORMAL' AND closed_at < datetime('now', '-" + days + " days')";
            } else {
                selectSql = "SELECT id FROM midgard_tickets WHERE priority = 'NORMAL' AND closed_at < NOW() - INTERVAL " + days + " DAY";
            }

            Set<Integer> liveOpenTicketIds = collectLiveOpenTicketIds();
            List<Integer> idsToDelete = new ArrayList<>();

            try (PreparedStatement ps = conn.prepareStatement(selectSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int ticketId = rs.getInt("id");
                    if (!liveOpenTicketIds.contains(ticketId)) {
                        idsToDelete.add(ticketId);
                    }
                }
            }

            if (idsToDelete.isEmpty()) {
                return 0;
            }

            String placeholders = String.join(",", java.util.Collections.nCopies(idsToDelete.size(), "?"));
            String deleteSql = "DELETE FROM midgard_tickets WHERE id IN (" + placeholders + ")";
            try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                for (int i = 0; i < idsToDelete.size(); i++) {
                    deletePs.setInt(i + 1, idsToDelete.get(i));
                }
                return deletePs.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Erro na limpeza de tickets antigos", e);
            return 0;
        }
    }

    private Set<Integer> collectLiveOpenTicketIds() {
        Set<Integer> ids = new java.util.HashSet<>();
        if (jda == null) return ids;

        List<String> openCategoryIds = new ArrayList<>();
        addCategoryIfValid(openCategoryIds, BotConfig.getTicketCategorySupport());
        addCategoryIfValid(openCategoryIds, BotConfig.getTicketCategoryReport());
        addCategoryIfValid(openCategoryIds, BotConfig.getTicketCategoryBug());
        addCategoryIfValid(openCategoryIds, BotConfig.getTicketCategoryLore());

        for (var guild : jda.getGuilds()) {
            for (TextChannel channel : guild.getTextChannels()) {
                String parentId = channel.getParentCategoryId();
                if (parentId == null || !openCategoryIds.contains(parentId)) continue;

                String ticketId = extractTopicField(channel.getTopic(), "TicketID:");
                if (ticketId == null) continue;
                try {
                    ids.add(Integer.parseInt(ticketId));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return ids;
    }

    private void addCategoryIfValid(List<String> ids, String categoryId) {
        if (categoryId != null && !categoryId.isBlank() && !"000000000000000000".equals(categoryId)) {
            ids.add(categoryId);
        }
    }

    public static class TicketData {
        public int id;
        public String channelName;
        public String userId;
        public String categoryName;
        public String claimedBy;
        public String[] collaboratorIds;
        public String priority;
        public String closedAt;
        public TicketMessage[] messages;
    }

    public static class TicketMessage {
        public String author;
        public String authorId;
        public String content;
        public String timestamp;

        public TicketMessage(String author, String authorId, String content, String timestamp) {
            this.author = author;
            this.authorId = authorId;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}
