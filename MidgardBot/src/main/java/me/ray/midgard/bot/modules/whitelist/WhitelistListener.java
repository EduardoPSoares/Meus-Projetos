package me.ray.midgard.bot.modules.whitelist;

import me.ray.midgard.bot.MidgardBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WhitelistListener {

    private static final Logger logger = LoggerFactory.getLogger(WhitelistListener.class);

    public static final String BUTTON_START = "whitelist:start";
    public static final String BUTTON_ACCEPT_TERMS = "whitelist:accept_terms";
    public static final String BUTTON_CONTINUE = "whitelist:continue:";
    public static final String MODAL_PREFIX = "whitelist:modal:";

    private final MidgardBot bot;
    private final WhitelistConfig config;
    private final WhitelistRepository repository;
    private WhitelistReviewListener reviewListener;
    private WhitelistRedisSync redisSync;
    private final Map<String, InteractionHook> lastEphemeralHooks = new ConcurrentHashMap<>();

    public WhitelistListener(MidgardBot bot, WhitelistConfig config, WhitelistRepository repository) {
        this.bot = bot;
        this.config = config;
        this.repository = repository;
    }

    public void setReviewListener(WhitelistReviewListener reviewListener) {
        this.reviewListener = reviewListener;
    }

    public void setRedisSync(WhitelistRedisSync redisSync) {
        this.redisSync = redisSync;
    }



    public void register() {
        var buttons = bot.getInteractionManager().getButtons();
        var modals = bot.getInteractionManager().getModals();

        // Register button handlers
        buttons.register(BUTTON_START, this::onStartButton);
        buttons.register(BUTTON_ACCEPT_TERMS, this::onAcceptTerms);
        buttons.registerPrefix(BUTTON_CONTINUE, this::onContinueButton);

        // Register modal handler
        modals.registerPrefix(MODAL_PREFIX, this::onModalSubmit);
    }

    // ==================== Button: Start Registration ====================

    private void onStartButton(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();

        // Check if user already has an application
        Optional<WhitelistApplication> existing = repository.findById(userId);
        if (existing.isPresent()) {
            WhitelistApplication app = existing.get();
            switch (app.getStatus()) {
                case PENDING:
                case APPROVED:
                case REJECTED:
                    event.replyEmbeds(WhitelistEmbeds.alreadyApplied(app.getStatus()))
                            .setEphemeral(true).queue();
                    return;
                case IN_PROGRESS:
                    // Allow to continue from where they left off
                    showProgress(event, app);
                    return;
            }
        }

        // Show terms
        event.replyEmbeds(WhitelistEmbeds.terms(config))
                .addComponents(ActionRow.of(
                        Button.success(BUTTON_ACCEPT_TERMS, config.getButtonAcceptText())
                ))
                .setEphemeral(true)
                .queue(hook -> storeEphemeralHook(userId, hook));
    }

    // ==================== Button: Accept Terms ====================

    private void onAcceptTerms(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();

        // Check again for existing application
        Optional<WhitelistApplication> existing = repository.findById(userId);
        if (existing.isPresent() && existing.get().getStatus() != WhitelistApplication.Status.IN_PROGRESS) {
            event.replyEmbeds(WhitelistEmbeds.alreadyApplied(existing.get().getStatus()))
                    .setEphemeral(true).queue();
            return;
        }

        // Create new application if none exists
        if (existing.isEmpty()) {
            WhitelistApplication app = new WhitelistApplication(userId);
            app.setUsername(event.getUser().getName());
            repository.save(app);
        }

        // Delete previous ephemeral message (terms)
        deleteEphemeralHook(userId);

        // Show first modal
        showModal(event, 0);
    }

    // ==================== Button: Continue to Next Part ====================

    private void onContinueButton(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();

        // Extract part number from button ID: whitelist:continue:N
        String componentId = event.getComponentId();
        int nextPart;
        try {
            nextPart = Integer.parseInt(componentId.substring(BUTTON_CONTINUE.length()));
        } catch (NumberFormatException e) {
            event.reply("❌ Erro interno.").setEphemeral(true).queue();
            return;
        }

        Optional<WhitelistApplication> existing = repository.findById(userId);
        if (existing.isEmpty()) {
            event.reply("❌ Nenhuma inscrição encontrada. Inicie novamente.").setEphemeral(true).queue();
            return;
        }

        WhitelistApplication app = existing.get();
        if (app.getStatus() != WhitelistApplication.Status.IN_PROGRESS) {
            event.replyEmbeds(WhitelistEmbeds.alreadyApplied(app.getStatus()))
                    .setEphemeral(true).queue();
            return;
        }

        // Delete previous ephemeral message (progress)
        deleteEphemeralHook(userId);

        showModal(event, nextPart);
    }

    // ==================== Modal Submit ====================

    private void onModalSubmit(ModalInteractionEvent event) {
        String userId = event.getUser().getId();

        // Extract part from modal ID: whitelist:modal:N
        String modalId = event.getModalId();
        int part;
        try {
            part = Integer.parseInt(modalId.substring(MODAL_PREFIX.length()));
        } catch (NumberFormatException e) {
            event.reply("❌ Erro interno.").setEphemeral(true).queue();
            return;
        }

        Optional<WhitelistApplication> existing = repository.findById(userId);
        if (existing.isEmpty()) {
            event.reply("❌ Nenhuma inscrição encontrada. Inicie novamente.").setEphemeral(true).queue();
            return;
        }

        WhitelistApplication app = existing.get();

        // Save answers from this part
        List<WhitelistConfig.QuestionData> questions = config.getQuestions(part);
        Map<String, String> newAnswers = new LinkedHashMap<>();
        for (WhitelistConfig.QuestionData q : questions) {
            ModalMapping mapping = event.getValue(q.getId());
            if (mapping != null) {
                newAnswers.put(q.getId(), mapping.getAsString());
            }
        }
        app.setAnswers(newAnswers);

        int totalParts = config.getPartCount();
        int nextPart = part + 1;

        if (nextPart >= totalParts) {
            // All parts completed
            app.setCurrentPart(totalParts);
            app.setStatus(WhitelistApplication.Status.PENDING);
            repository.save(app);

            event.replyEmbeds(WhitelistEmbeds.submitted())
                    .setEphemeral(true).queue();

            // Send log
            sendLogMessage(app, event.getUser());

            // Add pending role
            addRole(event.getUser(), event.getGuild(), config.getPendingRoleId());

            // Update review panel
            if (reviewListener != null) {
                reviewListener.updatePanel(event.getGuild());
            }

            // Sync to Redis cache
            if (redisSync != null) {
                redisSync.syncApplication(app);
            }
        } else {
            // More parts to go
            app.setCurrentPart(nextPart);
            repository.save(app);

            // Show progress with continue button
            event.replyEmbeds(WhitelistEmbeds.progress(config, nextPart, totalParts))
                    .addComponents(ActionRow.of(
                            Button.primary(BUTTON_CONTINUE + nextPart, config.getButtonContinueText())
                    ))
                    .setEphemeral(true)
                    .queue(hook -> storeEphemeralHook(userId, hook));
        }
    }

    // ==================== Helpers ====================

    private void showProgress(ButtonInteractionEvent event, WhitelistApplication app) {
        String userId = event.getUser().getId();
        int totalParts = config.getPartCount();
        int currentPart = app.getCurrentPart();

        if (currentPart >= totalParts) {
            event.replyEmbeds(WhitelistEmbeds.alreadyApplied(WhitelistApplication.Status.PENDING))
                    .setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(WhitelistEmbeds.progress(config, currentPart, totalParts))
                .addComponents(ActionRow.of(
                        Button.primary(BUTTON_CONTINUE + currentPart, config.getButtonContinueText())
                ))
                .setEphemeral(true)
                .queue(hook -> storeEphemeralHook(userId, hook));
    }

    private void showModal(ButtonInteractionEvent event, int partIndex) {
        List<WhitelistConfig.QuestionData> questions = config.getQuestions(partIndex);
        if (questions.isEmpty()) {
            event.reply("❌ Nenhuma pergunta configurada para esta parte.").setEphemeral(true).queue();
            return;
        }

        int totalParts = config.getPartCount();
        String title = "Whitelist - Parte " + (partIndex + 1) + "/" + totalParts;

        Modal.Builder modalBuilder = Modal.create(MODAL_PREFIX + partIndex, title);

        for (WhitelistConfig.QuestionData q : questions) {
            TextInput.Builder input = TextInput.create(q.getId(), q.getLabel(), q.getStyle())
                    .setRequired(q.isRequired());

            if (q.getPlaceholder() != null && !q.getPlaceholder().isEmpty()) {
                input.setPlaceholder(q.getPlaceholder());
            }

            if (q.getStyle() == net.dv8tion.jda.api.interactions.components.text.TextInputStyle.PARAGRAPH) {
                input.setMinLength(10);
                input.setMaxLength(1000);
            } else {
                input.setMaxLength(100);
            }

            modalBuilder.addActionRow(input.build());
        }

        event.replyModal(modalBuilder.build()).queue();
    }

    private void showModal(ModalInteractionEvent event, int partIndex) {
        // Modals can't reply with another modal, so this shouldn't happen
        // But kept for safety
        event.reply("Use o botão 'Continuar' para prosseguir.").setEphemeral(true).queue();
    }

    private void sendLogMessage(WhitelistApplication app, User user) {
        String logChannelId = config.getLogChannelId();
        if (logChannelId == null || logChannelId.isEmpty()) return;

        try {
            TextChannel logChannel = bot.getJda().getTextChannelById(logChannelId);
            if (logChannel != null) {
                logChannel.sendMessageEmbeds(WhitelistEmbeds.reviewLog(app, config, user))
                        .addComponents(ActionRow.of(
                                Button.success("whitelist:approve:" + app.getUserId(), "✅ Aprovar"),
                                Button.danger("whitelist:reject:" + app.getUserId(), "❌ Rejeitar")
                        ))
                        .queue();
            }
        } catch (Exception e) {
            logger.error("Failed to send whitelist log message", e);
        }
    }

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

    // ==================== Review Handlers ====================

    public void registerReviewButtons() {
        var buttons = bot.getInteractionManager().getButtons();
        buttons.registerPrefix("whitelist:approve:", this::onApprove);
        buttons.registerPrefix("whitelist:reject:", this::onReject);
    }

    private void onApprove(ButtonInteractionEvent event) {
        String userId = event.getComponentId().replace("whitelist:approve:", "");
        handleReview(event, userId, true);
    }

    private void onReject(ButtonInteractionEvent event) {
        String userId = event.getComponentId().replace("whitelist:reject:", "");
        handleReview(event, userId, false);
    }

    private void handleReview(ButtonInteractionEvent event, String userId, boolean approved) {
        Optional<WhitelistApplication> existing = repository.findById(userId);
        if (existing.isEmpty()) {
            event.reply("❌ Inscrição não encontrada.").setEphemeral(true).queue();
            return;
        }

        WhitelistApplication app = existing.get();
        if (app.getStatus() != WhitelistApplication.Status.PENDING) {
            event.reply("⚠️ Esta inscrição já foi revisada.").setEphemeral(true).queue();
            return;
        }

        String reviewerId = event.getUser().getId();

        if (approved) {
            app.approve(reviewerId, null);
            repository.save(app);

            // Sync to Redis cache
            if (redisSync != null) {
                redisSync.syncApplication(app);
            }

            // Add approved role, remove pending role
            Guild guild = event.getGuild();
            if (guild != null) {
                guild.retrieveMemberById(userId).queue(member -> {
                    addRole(member.getUser(), guild, config.getApprovedRoleId());
                    removeRole(member.getUser(), guild, config.getPendingRoleId());
                }, error -> logger.error("Failed to retrieve member for role update", error));
            }

            // Notify user
            notifyUser(userId, WhitelistEmbeds.approved(null));

            event.reply("✅ Inscrição de <@" + userId + "> **aprovada** por " + event.getUser().getAsMention())
                    .queue();

            // Disable buttons on the original message
            event.getMessage().editMessageComponents(ActionRow.of(
                    Button.success("whitelist:approved", "✅ Aprovado").asDisabled(),
                    Button.secondary("whitelist:reviewer", "Por: " + event.getUser().getName()).asDisabled()
            )).queue();
        } else {
            app.reject(reviewerId, null);
            repository.save(app);

            // Sync to Redis cache
            if (redisSync != null) {
                redisSync.syncApplication(app);
            }

            // Notify user
            notifyUser(userId, WhitelistEmbeds.rejected(null));

            event.reply("❌ Inscrição de <@" + userId + "> **rejeitada** por " + event.getUser().getAsMention())
                    .queue();

            // Disable buttons
            event.getMessage().editMessageComponents(ActionRow.of(
                    Button.danger("whitelist:rejected", "❌ Rejeitado").asDisabled(),
                    Button.secondary("whitelist:reviewer", "Por: " + event.getUser().getName()).asDisabled()
            )).queue();
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

    // ==================== Ephemeral Hook Management ====================

    private void storeEphemeralHook(String userId, InteractionHook hook) {
        InteractionHook old = lastEphemeralHooks.put(userId, hook);
        if (old != null) {
            old.deleteOriginal().queue(s -> {}, e -> {});
        }
    }

    private void deleteEphemeralHook(String userId) {
        InteractionHook hook = lastEphemeralHooks.remove(userId);
        if (hook != null) {
            hook.deleteOriginal().queue(s -> {}, e -> {});
        }
    }

    private void notifyUser(String userId, net.dv8tion.jda.api.entities.MessageEmbed embed) {
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
}
