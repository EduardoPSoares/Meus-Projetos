package com.midgardbot.commands.handlers;

import com.midgardbot.config.BotConfig;
import com.midgardbot.config.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Estado compartilhado e utilitários usados por todos os handlers de interação.
 */
public final class InteractionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractionUtils.class);

    // Scheduler compartilhado para tarefas agendadas (limpeza de cache, inatividade de tickets, etc.)
    public static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    // Armazena a página atual de cada mensagem da staff (MessageId -> PageIndex)
    public static final Map<String, Integer> staffViewPages = new java.util.concurrent.ConcurrentHashMap<>();

    // Armazena a página atual de cada mensagem de estatísticas da staff (MessageId -> PageIndex)
    public static final Map<String, Integer> staffStatsPages = new java.util.concurrent.ConcurrentHashMap<>();

    // Armazena o ID da mensagem da staff para cada usuário (UserId -> MessageId)
    public static final Map<String, String> staffMessages = new java.util.concurrent.ConcurrentHashMap<>();

    // Cache para paginação de logs (MessageId -> Lista de Embeds)
    public static final Map<String, List<MessageEmbed>> logCache = java.util.Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<MessageEmbed>> eldest) {
            return size() > Constants.LOG_CACHE_MAX_SIZE;
        }
    });

    // Armazena a página atual de cada log (MessageId -> PageIndex)
    public static final Map<String, Integer> logViewPages = new java.util.concurrent.ConcurrentHashMap<>();

    // Debounce (Anti-Double Click)
    public static final Map<String, Long> interactionDebounce = new java.util.concurrent.ConcurrentHashMap<>();

    // Palavras suspeitas para o detector de Powergaming/Metagaming
    public static final List<String> SUSPICIOUS_WORDS = List.of(
        "matar todos", "vencer", "ser o melhor", "vingança", "assassino", "god", "deus",
        "imortal", "invencivel", "invencível", "roubar tudo", "destruir tudo", "win", "ganhar"
    );

    // Referência ao TicketArchiver
    private static com.midgardbot.features.tickets.TicketArchiver ticketArchiver;

    public static void setTicketArchiver(com.midgardbot.features.tickets.TicketArchiver archiver) {
        ticketArchiver = archiver;
    }

    public static com.midgardbot.features.tickets.TicketArchiver getTicketArchiver() {
        return ticketArchiver;
    }

    public static void registerLogView(String messageId, List<MessageEmbed> pages) {
        logCache.put(messageId, pages);
        logViewPages.put(messageId, 0);
    }

    public static void registerStaffView(String messageId, int page) {
        staffViewPages.put(messageId, page);
    }

    /**
     * Verifica se o membro é staff (admin ou tem cargo de suporte de tickets).
     */
    public static boolean isStaff(net.dv8tion.jda.api.entities.Member member) {
        if (member == null) return false;
        if (member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) return true;

        String[] allCategoryRoles = {
            BotConfig.getTicketRoleSupport(), BotConfig.getTicketRoleReport(),
            BotConfig.getTicketRoleBug(), BotConfig.getTicketRoleLore()
        };
        for (String roleStr : allCategoryRoles) {
            if (roleStr != null && !roleStr.isEmpty()) {
                for (String roleId : roleStr.split(",")) {
                    if (member.getRoles().stream().anyMatch(r -> r.getId().equals(roleId.trim()))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Verifica se o membro pode revisar whitelists.
     */
    public static boolean canReviewWhitelist(net.dv8tion.jda.api.entities.Member member) {
        if (isStaff(member)) return true;

        List<String> reviewRoles = BotConfig.getAuthorizedRoles("PERM_CMD_REVIEW");
        List<String> infoRoles = BotConfig.getAuthorizedRoles("PERM_CMD_WHITELIST_INFO");

        for (net.dv8tion.jda.api.entities.Role role : member.getRoles()) {
            if (reviewRoles.contains(role.getId()) || infoRoles.contains(role.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Envia uma DM com um embed para o usuário.
     */
    public static void sendDM(User user, MessageEmbed embed) {
        user.openPrivateChannel().queue(channel ->
            channel.sendMessageEmbeds(embed).queue(null, new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER))
        );
    }

    public static void sendDM(User user, MessageEmbed embed, FileUpload... files) {
        user.openPrivateChannel().queue(channel ->
            channel.sendMessageEmbeds(embed).addFiles(files).queue(null, new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER))
        );
    }

    /**
     * Registra uma ação no canal de logs.
     */
    public static void logAction(net.dv8tion.jda.api.JDA jda, String title, String description, Color color, User user, User staff) {
        String logChannelId = BotConfig.getLogChannelId();
        if (logChannelId == null) return;

        TextChannel channel = jda.getTextChannelById(logChannelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(color)
            .setTimestamp(java.time.Instant.now());

        if (user != null) embed.addField("Usuário", user.getAsMention() + " (`" + user.getId() + "`)", true);
        if (staff != null) embed.addField("Staff", staff.getAsMention(), true);

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    private InteractionUtils() {}
}
