package me.ray.midgard.bot.modules.whitelist;

import me.ray.midgard.bot.MidgardBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WhitelistReviewListener {

    private static final Logger logger = LoggerFactory.getLogger(WhitelistReviewListener.class);

    // Button IDs
    public static final String BUTTON_START_REVIEW = "wl:review:start";
    public static final String BUTTON_APPROVE = "wl:review:approve";
    public static final String BUTTON_REJECT = "wl:review:reject";
    public static final String BUTTON_SKIP = "wl:review:skip";
    public static final String BUTTON_EXIT = "wl:review:exit";
    public static final String BUTTON_NEXT_PAGE = "wl:review:page:";
    public static final String MODAL_REJECT_REASON = "wl:review:reject_modal";

    private static final int MAX_PAGE_CONTENT = 3900;

    private final MidgardBot bot;
    private final WhitelistConfig config;
    private final WhitelistRepository repository;
    private final WhitelistReviewManager reviewManager;
    private WhitelistRedisSync redisSync;

    public WhitelistReviewListener(MidgardBot bot, WhitelistConfig config,
                                    WhitelistRepository repository, WhitelistReviewManager reviewManager) {
        this.bot = bot;
        this.config = config;
        this.repository = repository;
        this.reviewManager = reviewManager;
    }

    public void setRedisSync(WhitelistRedisSync redisSync) {
        this.redisSync = redisSync;
    }


    public void register() {
        var buttons = bot.getInteractionManager().getButtons();
        var modals = bot.getInteractionManager().getModals();

        buttons.register(BUTTON_START_REVIEW, this::onStartReview);
        buttons.register(BUTTON_APPROVE, this::onApprove);
        buttons.register(BUTTON_REJECT, this::onRejectButton);
        buttons.register(BUTTON_SKIP, this::onSkip);
        buttons.register(BUTTON_EXIT, this::onExit);
        buttons.registerPrefix(BUTTON_NEXT_PAGE, this::onNextPage);
        modals.register(MODAL_REJECT_REASON, this::onRejectModal);
    }

    // ==================== Start Review ====================

    private void onStartReview(ButtonInteractionEvent event) {
        String staffId = event.getUser().getId();

        WhitelistApplication app = reviewManager.claimNext(staffId);
        if (app == null) {
            event.reply("📭 Não há whitelists pendentes para analisar no momento.")
                    .setEphemeral(true).queue();
            updatePanel(event.getGuild());
            return;
        }

        // Show the application to the staff (ephemeral, page 0)
        List<String> pages = buildReviewPages(app);
        event.replyEmbeds(buildReviewEmbed(app, event.getUser(), 0, pages))
                .addComponents(buildReviewButtons(0, pages.size()))
                .setEphemeral(true).queue();

        updatePanel(event.getGuild());
    }

    // ==================== Page Navigation ====================

    private void onNextPage(ButtonInteractionEvent event) {
        String staffId = event.getUser().getId();
        String appUserId = reviewManager.getActiveReview(staffId);

        if (appUserId == null) {
            event.reply("❌ Você não está analisando nenhuma whitelist.").setEphemeral(true).queue();
            return;
        }

        var appOpt = repository.findById(appUserId);
        if (appOpt.isEmpty()) {
            reviewManager.release(staffId);
            event.reply("❌ Inscrição não encontrada.").setEphemeral(true).queue();
            return;
        }

        int page;
        try {
            page = Integer.parseInt(event.getComponentId().substring(BUTTON_NEXT_PAGE.length()));
        } catch (NumberFormatException e) {
            event.reply("❌ Erro interno.").setEphemeral(true).queue();
            return;
        }

        WhitelistApplication app = appOpt.get();
        List<String> pages = buildReviewPages(app);
        int safePage = Math.min(page, pages.size() - 1);
        event.editMessageEmbeds(buildReviewEmbed(app, event.getUser(), safePage, pages))
                .setComponents(buildReviewButtons(safePage, pages.size()))
                .queue();
    }

    // ==================== Approve ====================

    private void onApprove(ButtonInteractionEvent event) {
        String staffId = event.getUser().getId();
        String appUserId = reviewManager.getActiveReview(staffId);

        if (appUserId == null) {
            event.reply("❌ Você não está analisando nenhuma whitelist.").setEphemeral(true).queue();
            return;
        }

        var appOpt = repository.findById(appUserId);
        if (appOpt.isEmpty()) {
            reviewManager.release(staffId);
            event.reply("❌ Inscrição não encontrada.").setEphemeral(true).queue();
            return;
        }

        WhitelistApplication app = appOpt.get();

        if (app.getStatus() != WhitelistApplication.Status.PENDING) {
            reviewManager.release(staffId);
            event.editMessageEmbeds(buildResultEmbed("⚠️ Já Revisada",
                    "Esta inscrição já foi revisada por outro staff.", new Color(0xFEE75C)))
                    .setComponents().queue();
            return;
        }

        app.approve(staffId, null);
        repository.save(app);
        reviewManager.complete(staffId);

        // Sync to Redis cache
        if (redisSync != null) {
            redisSync.syncApplication(app);
        }

        // Role management
        Guild guild = event.getGuild();
        if (guild != null) {
            guild.retrieveMemberById(appUserId).queue(member -> {
                addRole(member.getUser(), guild, config.getApprovedRoleId());
                removeRole(member.getUser(), guild, config.getPendingRoleId());
            }, error -> logger.error("Failed to retrieve member for role update", error));
        }

        // DM user
        notifyUser(appUserId, WhitelistEmbeds.approved(null));

        // Update the review message
        event.editMessageEmbeds(buildResultEmbed("✅ Whitelist Aprovada",
                "A whitelist de **" + app.getUsername() + "** (<@" + appUserId + ">) foi **aprovada**!\n\n" +
                "👤 Revisado por: " + event.getUser().getAsMention(),
                new Color(0x57F287)))
                .setComponents().queue();

        updatePanel(event.getGuild());
    }

    // ==================== Reject ====================

    private void onRejectButton(ButtonInteractionEvent event) {
        String staffId = event.getUser().getId();

        if (!reviewManager.isReviewing(staffId)) {
            event.reply("❌ Você não está analisando nenhuma whitelist.").setEphemeral(true).queue();
            return;
        }

        // Open modal for rejection reason
        Modal modal = Modal.create(MODAL_REJECT_REASON, "Motivo da Rejeição")
                .addActionRow(
                        TextInput.create("reason", "Motivo", TextInputStyle.PARAGRAPH)
                                .setPlaceholder("Explique o motivo da rejeição (opcional)")
                                .setRequired(false)
                                .setMaxLength(500)
                                .build()
                ).build();

        event.replyModal(modal).queue();
    }

    private void onRejectModal(ModalInteractionEvent event) {
        String staffId = event.getUser().getId();
        String appUserId = reviewManager.getActiveReview(staffId);

        if (appUserId == null) {
            event.reply("❌ Você não está analisando nenhuma whitelist.").setEphemeral(true).queue();
            return;
        }

        var appOpt = repository.findById(appUserId);
        if (appOpt.isEmpty()) {
            reviewManager.release(staffId);
            event.reply("❌ Inscrição não encontrada.").setEphemeral(true).queue();
            return;
        }

        WhitelistApplication app = appOpt.get();

        if (app.getStatus() != WhitelistApplication.Status.PENDING) {
            reviewManager.release(staffId);
            event.reply("⚠️ Esta inscrição já foi revisada.").setEphemeral(true).queue();
            return;
        }

        ModalMapping reasonMapping = event.getValue("reason");
        String reason = (reasonMapping != null && !reasonMapping.getAsString().isBlank())
                ? reasonMapping.getAsString() : null;

        app.reject(staffId, reason);
        repository.save(app);
        reviewManager.complete(staffId);

        // Sync to Redis cache
        if (redisSync != null) {
            redisSync.syncApplication(app);
        }

        // DM user
        notifyUser(appUserId, WhitelistEmbeds.rejected(reason));

        String desc = "A whitelist de **" + app.getUsername() + "** (<@" + appUserId + ">) foi **rejeitada**.\n\n" +
                "👤 Revisado por: " + event.getUser().getAsMention();
        if (reason != null) {
            desc += "\n📝 Motivo: " + reason;
        }

        event.replyEmbeds(buildResultEmbed("❌ Whitelist Rejeitada", desc, new Color(0xED4245)))
                .setEphemeral(true).queue();

        updatePanel(event.getGuild());
    }

    // ==================== Skip ====================

    private void onSkip(ButtonInteractionEvent event) {
        String staffId = event.getUser().getId();

        if (!reviewManager.isReviewing(staffId)) {
            event.reply("❌ Você não está analisando nenhuma whitelist.").setEphemeral(true).queue();
            return;
        }

        // Release current and claim next
        reviewManager.release(staffId);
        WhitelistApplication next = reviewManager.claimNext(staffId);

        if (next == null) {
            event.editMessageEmbeds(buildResultEmbed("📭 Fim da Fila",
                    "Não há mais whitelists pendentes para analisar.", new Color(0x5865F2)))
                    .setComponents().queue();
            updatePanel(event.getGuild());
            return;
        }

        List<String> nextPages = buildReviewPages(next);
        event.editMessageEmbeds(buildReviewEmbed(next, event.getUser(), 0, nextPages))
                .setComponents(buildReviewButtons(0, nextPages.size()))
                .queue();
    }

    // ==================== Exit ====================

    private void onExit(ButtonInteractionEvent event) {
        String staffId = event.getUser().getId();
        reviewManager.release(staffId);

        event.editMessageEmbeds(buildResultEmbed("👋 Modo de Análise Encerrado",
                "Você saiu do modo de análise.\nAs whitelists não revisadas voltaram para a fila.",
                new Color(0x5865F2)))
                .setComponents().queue();

        updatePanel(event.getGuild());
    }

    // ==================== Panel ====================

    public MessageEmbed buildPanelEmbed() {
        long pendingCount = repository.countPending();
        Set<String> activeStaff = reviewManager.getActiveStaff();

        StringBuilder staffList = new StringBuilder();
        if (activeStaff.isEmpty()) {
            staffList.append("• Ninguém");
        } else {
            for (String staffId : activeStaff) {
                staffList.append("• <@").append(staffId).append(">\n");
            }
        }

        return new EmbedBuilder()
                .setTitle("📋 ✦ CENTRAL DE ANÁLISE ✦")
                .setDescription(
                        "👋 **Bem-vindo ao painel de gerenciamento de Whitelists.**\n" +
                        "📋 Utilize este painel para revisar as aplicações pendentes.\n\n" +
                        "❓ **» COMO FUNCIONA**\n" +
                        "**1️⃣** Clique em **Iniciar Análise** para puxar uma aplicação.\n" +
                        "**2️⃣** O sistema entrará em **Modo Foco**.\n" +
                        "**3️⃣** Revise as respostas e decida o veredito.\n" +
                        "**4️⃣** Continue para a próxima aplicação.\n\n" +
                        "📊 **» STATUS ATUAL** ┃ 👥 **» EM ANÁLISE**\n" +
                        "• Pendentes: **" + pendingCount + "** ┃ " + staffList
                )
                .setColor(new Color(0x2B2D31))
                .setFooter("MidgardBot • Sistema de Whitelist • Hoje às " +
                        java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                .setTimestamp(Instant.now())
                .build();
    }

    public ActionRow buildPanelButton() {
        long pendingCount = repository.countPending();
        String label = "🔍 Iniciar Análise" + (pendingCount > 0 ? " (" + pendingCount + ")" : "");

        return ActionRow.of(
                Button.success(BUTTON_START_REVIEW, label).withDisabled(pendingCount == 0)
        );
    }

    /**
     * Updates the panel embed message with current stats.
     */
    public void updatePanel(Guild guild) {
        if (!reviewManager.hasPanelMessage() || guild == null) return;

        try {
            TextChannel channel = guild.getTextChannelById(reviewManager.getPanelChannelId());
            if (channel == null) return;

            channel.editMessageEmbedsById(reviewManager.getPanelMessageId(), buildPanelEmbed())
                    .setComponents(buildPanelButton())
                    .queue(
                            success -> {},
                            error -> logger.debug("Could not update review panel: {}", error.getMessage())
                    );
        } catch (Exception e) {
            logger.debug("Failed to update review panel", e);
        }
    }

    // ==================== Embed Builders ====================

    // ==================== Dynamic Page Builder ====================

    private List<String> buildReviewPages(WhitelistApplication app) {
        List<String> pages = new ArrayList<>();

        String overviewHeader = "👤 **Usuário:** <@" + app.getUserId() + ">\n" +
                "📅 **Enviada:** <t:" + app.getCreatedAt().getEpochSecond() + ":R>\n" +
                "📊 **Status:** Pendente\n\n" +
                "Use os botões abaixo para navegar pelas respostas e dar o veredito.\n\n";

        StringBuilder current = new StringBuilder(overviewHeader);

        for (int i = 0; i < config.getPartCount(); i++) {
            String partTitle = config.getPartTitle(i);
            List<WhitelistConfig.QuestionData> questions = config.getQuestions(i);

            for (int j = 0; j < questions.size(); j++) {
                WhitelistConfig.QuestionData q = questions.get(j);
                String answer = app.getAnswer(q.getId());
                String answerText = answer != null ? answer : "*sem resposta*";

                // Build the block: part header (only for first question in part) + question + answer
                StringBuilder blockBuilder = new StringBuilder();
                if (j == 0) {
                    blockBuilder.append("📝 ").append(partTitle).append("\n");
                }
                blockBuilder.append("**").append(q.getLabel()).append("**\n");
                blockBuilder.append(answerText).append("\n\n");
                String blockText = blockBuilder.toString();

                // If a single block exceeds page limit, split the answer across pages
                if (blockText.length() > MAX_PAGE_CONTENT) {
                    // Flush current page first
                    if (current.length() > 0) {
                        pages.add(current.toString());
                        current = new StringBuilder();
                    }

                    String firstPrefix = (j == 0 ? "📝 " + partTitle + "\n" : "") +
                            "**" + q.getLabel() + "**\n";
                    String contPrefix = "**" + q.getLabel() + " (cont.)**\n";
                    String remaining = answerText;
                    boolean first = true;

                    while (!remaining.isEmpty()) {
                        String prefix = first ? firstPrefix : contPrefix;
                        int availableSpace = MAX_PAGE_CONTENT - prefix.length() - 2;
                        int cutAt = Math.min(remaining.length(), availableSpace);

                        // Try to cut at a word or line boundary
                        if (cutAt < remaining.length()) {
                            int lastNewline = remaining.lastIndexOf('\n', cutAt);
                            int lastSpace = remaining.lastIndexOf(' ', cutAt);
                            int cutPoint = Math.max(lastNewline, lastSpace);
                            if (cutPoint > cutAt * 0.5) {
                                cutAt = cutPoint + 1;
                            }
                        }

                        String chunk = remaining.substring(0, cutAt);
                        remaining = remaining.substring(cutAt);

                        pages.add(prefix + chunk + "\n\n");
                        first = false;
                    }
                    continue;
                }

                // Normal case: check if it fits on current page
                if (current.length() + blockText.length() > MAX_PAGE_CONTENT) {
                    // Save current page and start a new one
                    pages.add(current.toString());
                    current = new StringBuilder();

                    // Add part context header on the new page
                    current.append("📝 ").append(partTitle);
                    if (j > 0) current.append(" (cont.)");
                    current.append("\n");
                    current.append("**").append(q.getLabel()).append("**\n");
                    current.append(answerText).append("\n\n");
                } else {
                    current.append(blockText);
                }
            }
        }

        if (current.length() > 0) {
            pages.add(current.toString());
        }

        if (pages.isEmpty()) {
            pages.add(overviewHeader + "*Nenhuma resposta encontrada.*");
        }

        return pages;
    }

    private MessageEmbed buildReviewEmbed(WhitelistApplication app, User reviewer, int page, List<String> contentPages) {
        int totalPages = contentPages.size();
        String displayName = app.getUsername() != null ? app.getUsername() : app.getUserId();

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(new Color(0xFEE75C))
                .setTitle("📋 Análise de Whitelist — " + displayName)
                .setTimestamp(Instant.now())
                .setFooter("Análise por " + reviewer.getName() + " • Página " + (page + 1) + "/" + totalPages);

        if (page >= 0 && page < totalPages) {
            builder.setDescription(contentPages.get(page));
        }

        return builder.build();
    }

    private List<ActionRow> buildReviewButtons(int currentPage, int totalPages) {
        // Navigation buttons
        ActionRow navRow = ActionRow.of(
                currentPage > 0
                        ? Button.secondary(BUTTON_NEXT_PAGE + (currentPage - 1), "◀ Anterior")
                        : Button.secondary("wl:review:disabled:prev", "◀ Anterior").asDisabled(),
                currentPage < totalPages - 1
                        ? Button.secondary(BUTTON_NEXT_PAGE + (currentPage + 1), "Próximo ▶")
                        : Button.secondary("wl:review:disabled:next", "Próximo ▶").asDisabled()
        );

        // Action buttons
        ActionRow actionRow = ActionRow.of(
                Button.success(BUTTON_APPROVE, "✅ Aprovar"),
                Button.danger(BUTTON_REJECT, "❌ Rejeitar"),
                Button.primary(BUTTON_SKIP, "⏭ Pular"),
                Button.secondary(BUTTON_EXIT, "🚪 Sair")
        );

        return List.of(navRow, actionRow);
    }

    private MessageEmbed buildResultEmbed(String title, String description, Color color) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now())
                .setFooter("MidgardBot • Sistema de Whitelist")
                .build();
    }

    // ==================== Role Helpers ====================

    private void addRole(User user, Guild guild, String roleId) {
        if (roleId == null || roleId.isEmpty() || guild == null) return;
        try {
            Role role = guild.getRoleById(roleId);
            if (role != null) {
                guild.addRoleToMember(user, role).queue(
                        success -> logger.debug("Added role {} to {}", role.getName(), user.getName()),
                        error -> logger.error("Failed to add role to {}", user.getName(), error)
                );
            }
        } catch (Exception e) {
            logger.error("Failed to add role", e);
        }
    }

    private void removeRole(User user, Guild guild, String roleId) {
        if (roleId == null || roleId.isEmpty() || guild == null) return;
        try {
            Role role = guild.getRoleById(roleId);
            if (role != null) {
                guild.removeRoleFromMember(user, role).queue(
                        success -> logger.debug("Removed role {} from {}", role.getName(), user.getName()),
                        error -> logger.error("Failed to remove role from {}", user.getName(), error)
                );
            }
        } catch (Exception e) {
            logger.error("Failed to remove role", e);
        }
    }

    private void notifyUser(String userId, MessageEmbed embed) {
        try {
            bot.getJda().retrieveUserById(userId).queue(user -> {
                user.openPrivateChannel().queue(channel -> {
                    channel.sendMessageEmbeds(embed).queue(
                            success -> {},
                            error -> logger.debug("Could not DM user {}", userId)
                    );
                }, error -> logger.debug("Could not open DM with {}", userId));
            }, error -> logger.debug("Could not find user {}", userId));
        } catch (Exception e) {
            logger.debug("Failed to notify user {}", userId);
        }
    }

    // ==================== Accessors ====================

    public WhitelistReviewManager getReviewManager() { return reviewManager; }
}
