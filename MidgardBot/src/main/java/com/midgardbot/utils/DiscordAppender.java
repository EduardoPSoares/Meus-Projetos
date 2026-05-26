package com.midgardbot.utils;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.midgardbot.config.BotConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordAppender extends AppenderBase<ILoggingEvent> {
    private static JDA jda;
    private static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private static final int MAX_QUEUE_SIZE = 10_000;
    private static volatile boolean isProcessing = false;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DiscordAppender-Worker");
        t.setDaemon(true);
        return t;
    });

    public static void setJda(JDA jdaInstance) {
        jda = jdaInstance;
        processQueue();
    }

    @Override
    protected void append(ILoggingEvent event) {
        String msg = formatMessage(event);
        if (queue.size() < MAX_QUEUE_SIZE) {
            queue.add(msg);
        }
        processQueue();
    }

    private String formatMessage(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder();
        // Format: [HH:mm:ss] [Level] Logger - Message
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = java.time.LocalDateTime.now().format(dtf);
        
        String color;
        String levelName;
        switch (event.getLevel().toString()) {
            case "ERROR": 
                color = "\u001B[2;31m"; // Red
                levelName = "ERRO";
                break;
            case "WARN":  
                color = "\u001B[2;33m"; // Yellow
                levelName = "AVISO";
                break;
            case "INFO":  
                color = "\u001B[2;36m"; // Cyan
                levelName = "INFO";
                break;
            case "DEBUG": 
                color = "\u001B[2;37m"; // Gray
                levelName = "DEBUG";
                break;
            default:      
                color = "\u001B[0m"; 
                levelName = event.getLevel().toString();
                break;
        }
        
        // [Time] [Level] Logger - Message
        // Time in Blue, Level and Message in specific color, Logger in Gray
        sb.append("\u001B[2;34m[").append(time).append("]\u001B[0m "); 
        sb.append(color).append("[").append(levelName).append("]\u001B[0m ");
        sb.append("\u001B[2;30m").append(event.getLoggerName()).append("\u001B[0m - ");
        sb.append(color).append(event.getFormattedMessage()).append("\u001B[0m");
        
        return sb.toString();
    }

    private static void processQueue() {
        if (jda == null || isProcessing) return;
        
        String logChannelId = BotConfig.getLogChannelId();
        if (logChannelId == null || logChannelId.isEmpty()) return;

        EXECUTOR.submit(() -> {
            synchronized (DiscordAppender.class) {
                if (isProcessing) return;
                isProcessing = true;
            }

            try {
                TextChannel channel = jda.getTextChannelById(logChannelId);
                if (channel == null) return;

                StringBuilder batch = new StringBuilder();
                while (!queue.isEmpty()) {
                    String msg = queue.poll();
                    if (msg == null) break;

                    // Discord limit is 2000 chars. We use 1900 to be safe with code blocks.
                    if (batch.length() + msg.length() + 10 > 1900) {
                        channel.sendMessage("```ansi\n" + batch.toString() + "```").queue();
                        batch.setLength(0);
                        try { Thread.sleep(1000); } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    batch.append(msg).append("\n");
                }
                if (batch.length() > 0) {
                    channel.sendMessage("```ansi\n" + batch.toString() + "```").queue();
                }
            } catch (java.util.concurrent.RejectedExecutionException e) {
                // JDA is shutting down, ignore
            } catch (Exception e) {
                // Do not log here to avoid infinite recursion
                e.printStackTrace();
            } finally {
                synchronized (DiscordAppender.class) {
                    isProcessing = false;
                }
            }
        });
    }
}
