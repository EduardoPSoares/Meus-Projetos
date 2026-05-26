package com.midgardbot.utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilitário para geração de transcrições de tickets.
 * Converte o histórico de mensagens de um canal em um arquivo HTML formatado.
 */
public class TranscriptUtils {

    private static final String HTML_TEMPLATE_START = 
        "<!DOCTYPE html>\n" +
        "<html lang=\"pt-BR\">\n" +
        "<head>\n" +
        "    <meta charset=\"UTF-8\">\n" +
        "    <title>Transcrição do Ticket</title>\n" +
        "    <style>\n" +
        "        body { background-color: #36393f; color: #dcddde; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; }\n" +
        "        .message-group { margin-bottom: 20px; border-bottom: 1px solid #4f545c; padding-bottom: 10px; }\n" +
        "        .header { display: flex; align-items: center; margin-bottom: 5px; }\n" +
        "        .avatar { width: 40px; height: 40px; border-radius: 50%; margin-right: 15px; }\n" +
        "        .username { font-weight: bold; color: #fff; margin-right: 10px; }\n" +
        "        .timestamp { color: #72767d; font-size: 0.75rem; }\n" +
        "        .content { margin-left: 55px; white-space: pre-wrap; }\n" +
        "        .embed { background-color: #2f3136; border-left: 4px solid #202225; padding: 10px; margin-top: 10px; border-radius: 4px; max-width: 500px; }\n" +
        "        .embed-title { font-weight: bold; color: #fff; margin-bottom: 5px; }\n" +
        "        .embed-desc { font-size: 0.9rem; }\n" +
        "        .attachment { margin-top: 10px; }\n" +
        "        .attachment img { max-width: 400px; border-radius: 5px; }\n" +
        "        .bot-tag { background-color: #5865f2; color: white; font-size: 0.6rem; padding: 2px 5px; border-radius: 3px; vertical-align: middle; margin-left: 5px; }\n" +
        "    </style>\n" +
        "</head>\n" +
        "<body>\n" +
        "    <h1>📝 Transcrição do Ticket</h1>\n" +
        "    <p>Gerado automaticamente pelo MidgardBOT</p>\n" +
        "    <hr>\n";

    private static final String HTML_TEMPLATE_END = 
        "</body>\n" +
        "</html>";

    public static InputStream generate(List<Message> messages) {
        StringBuilder html = new StringBuilder(HTML_TEMPLATE_START);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // As mensagens vêm do mais recente para o mais antigo, precisamos inverter
        List<Message> sortedMessages = messages.stream()
            .sorted((m1, m2) -> m1.getTimeCreated().compareTo(m2.getTimeCreated()))
            .collect(Collectors.toList());

        for (Message msg : sortedMessages) {
            User author = msg.getAuthor();
            String avatarUrl = author.getEffectiveAvatarUrl();
            String username = escapeHtml(author.getName());
            String time = msg.getTimeCreated().format(formatter);
            String content = msg.getContentDisplay(); // Usa Display para resolver menções
            
            // Sanitização de HTML
            content = escapeHtml(content);

            html.append("<div class=\"message-group\">\n");
            html.append("    <div class=\"header\">\n");
            html.append("        <img src=\"").append(avatarUrl).append("\" class=\"avatar\">\n");
            html.append("        <span class=\"username\">").append(username).append("</span>\n");
            if (author.isBot()) {
                html.append("        <span class=\"bot-tag\">BOT</span>\n");
            }
            html.append("        <span class=\"timestamp\">").append(time).append("</span>\n");
            html.append("    </div>\n");
            
            if (!content.isEmpty()) {
                html.append("    <div class=\"content\">").append(content).append("</div>\n");
            }

            // Embeds
            for (MessageEmbed embed : msg.getEmbeds()) {
                html.append("    <div class=\"content\">\n");
                html.append("        <div class=\"embed\" style=\"border-left-color: #").append(embed.getColor() != null ? Integer.toHexString(embed.getColor().getRGB()).substring(2) : "202225").append(";\">\n");
                if (embed.getTitle() != null) {
                    html.append("            <div class=\"embed-title\">").append(escapeHtml(embed.getTitle())).append("</div>\n");
                }
                if (embed.getDescription() != null) {
                    html.append("            <div class=\"embed-desc\">").append(escapeHtml(embed.getDescription()).replace("\n", "<br>")).append("</div>\n");
                }
                html.append("        </div>\n");
                html.append("    </div>\n");
            }

            // Attachments (Imagens)
            for (Message.Attachment attachment : msg.getAttachments()) {
                String safeUrl = escapeHtml(attachment.getUrl());
                if (attachment.isImage()) {
                    html.append("    <div class=\"content attachment\">\n");
                    html.append("        <a href=\"").append(safeUrl).append("\" target=\"_blank\"><img src=\"").append(safeUrl).append("\"></a>\n");
                    html.append("    </div>\n");
                } else {
                    html.append("    <div class=\"content attachment\">\n");
                    html.append("        <a href=\"").append(safeUrl).append("\" target=\"_blank\">📎 ").append(escapeHtml(attachment.getFileName())).append("</a>\n");
                    html.append("    </div>\n");
                }
            }

            html.append("</div>\n");
        }

        html.append(HTML_TEMPLATE_END);
        return new ByteArrayInputStream(html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
