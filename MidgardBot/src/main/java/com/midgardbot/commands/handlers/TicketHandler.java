package com.midgardbot.commands.handlers;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.midgardbot.config.BotConfig;
import com.midgardbot.config.MessagesConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.DatabaseManager;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.utils.TranscriptUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.FileUpload;

/**
 * Handler para todas as interações relacionadas ao sistema de Tickets.
 * Inclui: criação, claim, painel staff, snippets, voz, prioridade,
 * renomear, mover, adicionar membro, fechar, avaliação e limpeza.
 */
public final class TicketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketHandler.class);

    // Mapa em memória para tracking imediato de collabs (channelId → set de userIds)
    private static final ConcurrentHashMap<String, Set<String>> COLLAB_CACHE = new ConcurrentHashMap<>();

    private TicketHandler() {}

    /** Verifica se o usuário foi adicionado como collab (cache em memória). */
    public static boolean isCachedCollab(String channelId, String userId) {
        Set<String> collabs = COLLAB_CACHE.get(channelId);
        return collabs != null && collabs.contains(userId);
    }

    /** Remove o cache de collabs de um canal (chamar ao fechar/deletar ticket). */
    public static void clearCollabCache(String channelId) {
        COLLAB_CACHE.remove(channelId);
    }

    public static void postControlPanel(TextChannel channel) {
        postControlPanel(channel, channel.getTopic());
    }

    public static void postControlPanel(TextChannel channel, String topic) {
        channel.sendMessage("**Painel do Ticket**\nUse os botoes abaixo para gerenciar este atendimento.")
            .setActionRow(buildTicketControlButtons(channel.getGuild(), topic))
            .queue(null, error -> LOGGER.warn("Erro ao publicar painel do ticket {}", channel.getId(), error));
    }

    // ========================
    //   BUTTON INTERACTIONS
    // ========================

    /**
     * Tenta tratar um ButtonInteractionEvent relacionado a tickets.
     * @return true se o evento foi tratado, false caso contrário
     */
    public static boolean handleButton(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("btn_claim_ticket")) { handleClaimTicket(event); return true; }
        if (id.equals("btn_staff_panel")) { handleStaffPanel(event); return true; }
        if (id.equals("btn_ticket_snippets")) { handleSnippets(event); return true; }
        if (id.equals("btn_ticket_voice")) { handleVoice(event); return true; }
        if (id.equals("btn_ticket_priority")) { handlePriority(event); return true; }
        if (id.equals("btn_ticket_rename")) { handleRename(event); return true; }
        if (id.equals("btn_ticket_move")) { handleMove(event); return true; }
        if (id.equals("btn_add_member")) { handleAddMember(event); return true; }
        if (id.equals("btn_close_ticket")) { handleCloseTicket(event); return true; }
        if (id.equals("btn_confirm_close")) { handleConfirmClose(event); return true; }
        if (id.equals("btn_cancel_close")) { handleCancelClose(event); return true; }
        if (id.equals("btn_cancel_clear_tickets")) { handleCancelClearTickets(event); return true; }
        if (id.startsWith("btn_confirm_clear_tickets")) { handleConfirmClearTickets(event); return true; }
        if (id.startsWith("review_")) { handleReview(event); return true; }

        return false;
    }

    // ========================
    //  SELECT MENU INTERACTIONS
    // ========================

    /**
     * Tenta tratar um StringSelectInteractionEvent relacionado a tickets.
     * @return true se o evento foi tratado, false caso contrário
     */
    public static boolean handleSelectMenu(StringSelectInteractionEvent event) {
        String compId = event.getComponentId();

        if (compId.equals("ticket_selection")) { handleTicketCreation(event); return true; }
        if (compId.equals("menu_ticket_move_category")) { handleMoveCategory(event); return true; }
        if (compId.equals("menu_ticket_snippets")) { handleSnippetSelection(event); return true; }

        return false;
    }

    /**
     * Tenta tratar um EntitySelectInteractionEvent relacionado a tickets.
     * @return true se o evento foi tratado, false caso contrário
     */
    public static boolean handleEntitySelect(EntitySelectInteractionEvent event) {
        if (event.getComponentId().equals("ticket_add_user")) {
            handleAddUserEntity(event);
            return true;
        }
        return false;
    }

    // ========================
    //    MODAL INTERACTIONS
    // ========================

    /**
     * Tenta tratar um ModalInteractionEvent relacionado a tickets.
     * @return true se o evento foi tratado, false caso contrário
     */
    public static boolean handleModal(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        if (modalId.equals("modal_ticket_report")) { handleReportModal(event); return true; }
        if (modalId.equals("modal_ticket_bug")) { handleBugModal(event); return true; }
        if (modalId.equals("modal_ticket_support")) { handleSupportModal(event); return true; }
        if (modalId.equals("modal_ticket_rename")) { handleRenameModal(event); return true; }

        return false;
    }

    // ========================
    //   TICKET CREATION
    // ========================

    public static void createTicketChannel(Guild guild, User user, String categoryName, String emoji, String categoryId, String roleId, String initialContent) {
        try {
            String sanitizedName = user.getName().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (sanitizedName.isEmpty()) sanitizedName = "user-" + user.getId();

            int ticketId = DataManager.createTicket(user.getId(), "pending", categoryName);
            String ticketIdStr = (ticketId > 0) ? String.format("%04d", ticketId) : "xxxx";
            String channelName = "ticket-" + sanitizedName + "-" + ticketIdStr;

            net.dv8tion.jda.api.requests.restaction.ChannelAction<TextChannel> action;

            Category category = null;
            if (categoryId != null && !categoryId.isEmpty()) {
                try {
                    category = guild.getCategoryById(categoryId);
                } catch (Exception e) {
                    LOGGER.warn("Categoria de ticket não encontrada: " + categoryId);
                }
            }

            if (category != null) {
                if (category.getChannels().size() >= 50) {
                    LOGGER.error("Categoria de ticket CHEIA: " + category.getName());
                    DataManager.deleteTicket(ticketId);
                    return;
                }
                action = category.createTextChannel(channelName)
                    .clearPermissionOverrides();
            } else {
                LOGGER.error("Tentativa de criar ticket sem categoria definida (ou inválida). ID: " + categoryId);
                DataManager.deleteTicket(ticketId);
                return;
            }

            // Canal privado — nega VIEW_CHANNEL e MESSAGE_SEND para @everyone
            // assim o Discord impede envio de mensagens por quem não tem override explícito
            net.dv8tion.jda.api.entities.Member ownerMember = guild.getMemberById(user.getId());
            if (ownerMember == null) {
                LOGGER.warn("Não foi possível resolver o membro {} para criar o ticket.", user.getId());
                DataManager.deleteTicket(ticketId);
                return;
            }

            action.addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                .addPermissionOverride(ownerMember, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), null);

            // Staffs - usar apenas o cargo específico da categoria
            Set<String> rolesToAdd = new HashSet<>();
            if (roleId != null && !roleId.isEmpty()) {
                for (String r : roleId.split(",")) rolesToAdd.add(r.trim());
            }

            for (String rId : rolesToAdd) {
                try {
                    Role role = guild.getRoleById(rId);
                    if (role != null) {
                        action.addPermissionOverride(role, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY), null);
                    }
                } catch (Exception e) { LOGGER.debug("Erro ao adicionar permissão de cargo no ticket", e); }
            }

            String initialTopic = "TicketID:" + ticketId + " | OwnerID:" + user.getId() + " | Category:" + categoryName + " | Status:Open";
            action.setTopic(initialTopic);

            final String finalTicketIdStr = ticketIdStr;
            action.queue(channel -> {
                try {
                    ensureStaffRolePermissions(channel);
                    DataManager.updateTicketChannel(ticketId, channel.getName());

                    WhitelistStatusInfo info = DataManager.getStatus(user.getId());
                    String nickname = (info != null && info.nickname != null) ? info.nickname : "Desconhecido";

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("emoji", emoji);
                    placeholders.put("category", categoryName);
                    placeholders.put("user", user.getAsMention());
                    placeholders.put("nickname", nickname);
                    placeholders.put("id", finalTicketIdStr);
                    placeholders.put("user_name", user.getName());
                    placeholders.put("user_avatar", user.getEffectiveAvatarUrl());

                    // Menção automática
                    String mention = "";
                    if (categoryName.equals("Suporte")) mention = MessagesConfig.get().ticket.support_mention;
                    else if (categoryName.equals("Denúncia")) mention = MessagesConfig.get().ticket.report_mention;
                    else if (categoryName.equals("Bug")) mention = MessagesConfig.get().ticket.bug_mention;
                    else if (categoryName.equals("Lore")) mention = MessagesConfig.get().ticket.lore_mention;

                    if (mention != null && !mention.isEmpty()) {
                        channel.sendMessage(mention).queue(msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS));
                    }

                    EmbedBuilder embed = MessagesConfig.buildEmbed(MessagesConfig.get().ticket.created, placeholders);
                    embed.setFooter("Ticket #" + finalTicketIdStr + " • " + guild.getJDA().getSelfUser().getName(), guild.getJDA().getSelfUser().getEffectiveAvatarUrl());

                    if (initialContent != null && !initialContent.isEmpty()) {
                        embed.addField("📝 Detalhes do Ticket", initialContent, false);
                    }

                    channel.sendMessageEmbeds(embed.build())
                        .addActionRow(
                            Button.success("btn_claim_ticket", "🙋 Assumir Ticket"),
                            Button.danger("btn_close_ticket", "🔒 Fechar Ticket"),
                            Button.secondary("btn_staff_panel", "🛠️ Painel Staff")
                        )
                        .queue();

                    // Aviso de horário de atendimento
                    checkBusinessHours(channel, user, guild);

                    // Notifica staffs
                    if (roleId != null && !roleId.isEmpty()) {
                        StringBuilder mentions = new StringBuilder();
                        for (String r : roleId.split(",")) mentions.append("<@&").append(r.trim()).append("> ");
                        channel.sendMessage("🔔 " + mentions.toString().trim() + " novo ticket aberto.").queue();
                    }

                    // Verificação de inatividade (10 min)
                    scheduleInactivityCheck(channel, guild, user, roleId);

                } catch (Exception e) {
                    LOGGER.error("Erro ao configurar canal de ticket", e);
                }
            }, error -> {
                LOGGER.error("Erro ao criar canal de ticket", error);
                DataManager.deleteTicket(ticketId);
            });
        } catch (Exception e) {
            LOGGER.error("Erro fatal ao iniciar criação de ticket", e);
        }
    }

    private static void addConfiguredRoleIds(Set<String> roleIds, String configuredRoleIds) {
        if (configuredRoleIds == null || configuredRoleIds.isBlank()) {
            return;
        }

        for (String roleId : configuredRoleIds.split(",")) {
            String trimmedRoleId = roleId.trim();
            if (!trimmedRoleId.isEmpty()) {
                roleIds.add(trimmedRoleId);
            }
        }
    }

    private static Set<String> resolveTicketStaffRoleIds(TextChannel channel) {
        Set<String> roleIds = new HashSet<>();
        String parentCategoryId = channel.getParentCategoryId();

        if (Objects.equals(parentCategoryId, BotConfig.getTicketCategorySupport())) {
            addConfiguredRoleIds(roleIds, BotConfig.getTicketRoleSupport());
        }
        if (Objects.equals(parentCategoryId, BotConfig.getTicketCategoryReport())) {
            addConfiguredRoleIds(roleIds, BotConfig.getTicketRoleReport());
        }
        if (Objects.equals(parentCategoryId, BotConfig.getTicketCategoryBug())) {
            addConfiguredRoleIds(roleIds, BotConfig.getTicketRoleBug());
        }
        if (Objects.equals(parentCategoryId, BotConfig.getTicketCategoryLore())) {
            addConfiguredRoleIds(roleIds, BotConfig.getTicketRoleLore());
        }

        if (!roleIds.isEmpty()) {
            return roleIds;
        }

        String categoryName = extractTopicField(channel.getTopic(), "Category:");
        if ("Suporte".equalsIgnoreCase(categoryName)) {
            addConfiguredRoleIds(roleIds, BotConfig.getTicketRoleSupport());
        } else if ("DenÃºncia".equalsIgnoreCase(categoryName)) {
            addConfiguredRoleIds(roleIds, BotConfig.getTicketRoleReport());
        } else if ("Bug".equalsIgnoreCase(categoryName)) {
            addConfiguredRoleIds(roleIds, BotConfig.getTicketRoleBug());
        } else if ("Lore".equalsIgnoreCase(categoryName)) {
            addConfiguredRoleIds(roleIds, BotConfig.getTicketRoleLore());
        }

        return roleIds;
    }

    private static boolean isConfiguredOpenTicketCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return false;
        }

        return Objects.equals(categoryId, BotConfig.getTicketCategorySupport())
            || Objects.equals(categoryId, BotConfig.getTicketCategoryReport())
            || Objects.equals(categoryId, BotConfig.getTicketCategoryBug())
            || Objects.equals(categoryId, BotConfig.getTicketCategoryLore());
    }

    private static boolean isManagedOpenTicketChannel(TextChannel channel) {
        if (channel == null || channel.getName().startsWith("closed-")) {
            return false;
        }

        String topic = channel.getTopic();
        return topic != null
            && topic.contains("TicketID:")
            && isConfiguredOpenTicketCategory(channel.getParentCategoryId());
    }

    private static void ensureStaffRolePermissions(TextChannel channel) {
        for (String roleId : resolveTicketStaffRoleIds(channel)) {
            Role role = channel.getGuild().getRoleById(roleId);
            if (role == null) {
                LOGGER.debug("Cargo de ticket {} nÃ£o encontrado ao sincronizar permissÃµes do canal {}", roleId, channel.getId());
                continue;
            }

            channel.upsertPermissionOverride(role)
                .clear(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
                .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
                .queue(null, error -> LOGGER.warn("Erro ao sincronizar permissÃµes do cargo {} no ticket {}", roleId, channel.getId(), error));
        }
    }

    public static void syncOpenTicketPermissions(Guild guild) {
        if (guild == null) {
            return;
        }

        for (TextChannel channel : guild.getTextChannels()) {
            if (isManagedOpenTicketChannel(channel)) {
                ensureStaffRolePermissions(channel);
            }
        }
    }

    private static void checkBusinessHours(TextChannel channel, User user, Guild guild) {
        if (!BotConfig.isTicketScheduleEnabled()) return;
        try {
            java.time.ZoneId tz = java.time.ZoneId.of(BotConfig.getTicketScheduleTimezone());
            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(tz);
            int hour = now.getHour();
            java.time.DayOfWeek day = now.getDayOfWeek();

            boolean isWeekend = (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY);
            int startHour = isWeekend ? BotConfig.getTicketWeekendStart() : BotConfig.getTicketWeekdayStart();
            int endHour = isWeekend ? BotConfig.getTicketWeekendEnd() : BotConfig.getTicketWeekdayEnd();

            if (hour < startHour || hour >= endHour) {
                MessagesConfig.TicketScheduleSection sched = MessagesConfig.get().ticket.schedule;
                String schedTitle = sched.title.replace("{user_name}", user.getName());
                String schedDesc = sched.description
                    .replace("{weekday_start}", String.valueOf(BotConfig.getTicketWeekdayStart()))
                    .replace("{weekday_end}", String.valueOf(BotConfig.getTicketWeekdayEnd()))
                    .replace("{weekend_start}", String.valueOf(BotConfig.getTicketWeekendStart()))
                    .replace("{weekend_end}", String.valueOf(BotConfig.getTicketWeekendEnd()));

                EmbedBuilder schedEmbed = new EmbedBuilder()
                    .setTitle(schedTitle)
                    .setDescription(schedDesc)
                    .setColor(Color.decode(sched.color != null ? sched.color : "#E74C3C"))
                    .setFooter(sched.footer, guild.getJDA().getSelfUser().getEffectiveAvatarUrl())
                    .setTimestamp(java.time.Instant.now());

                channel.sendMessageEmbeds(schedEmbed.build()).queue();
            }
        } catch (Exception schedEx) {
            LOGGER.warn("Erro ao verificar horário de atendimento", schedEx);
        }
    }

    private static void scheduleInactivityCheck(TextChannel channel, Guild guild, User user, String roleId) {
        if (roleId == null || roleId.isEmpty()) return;

        InteractionUtils.SCHEDULER.schedule(() -> {
            try {
                TextChannel currentChannel = guild.getJDA().getTextChannelById(channel.getId());
                if (currentChannel == null) return;
                if (currentChannel.getName().startsWith("closed-")) return;

                currentChannel.getHistory().retrievePast(100).queue(messages -> {
                    boolean staffResponded = false;
                    boolean userSentMessage = false;

                    Set<String> staffRoleIds = new HashSet<>();
                    if (roleId != null) {
                        for (String r : roleId.split(",")) staffRoleIds.add(r.trim());
                    }
                    // Adicionar todos os cargos de categoria para detectar resposta de staff
                    String[] allCatRoles = {
                        BotConfig.getTicketRoleSupport(), BotConfig.getTicketRoleReport(),
                        BotConfig.getTicketRoleBug(), BotConfig.getTicketRoleLore()
                    };
                    for (String cr : allCatRoles) {
                        if (cr != null && !cr.isEmpty()) {
                            for (String r : cr.split(",")) staffRoleIds.add(r.trim());
                        }
                    }

                    for (net.dv8tion.jda.api.entities.Message msg : messages) {
                        if (msg.getAuthor().isBot()) continue;
                        if (msg.getType().isSystem()) continue;

                        boolean isUser = msg.getAuthor().getId().equals(user.getId());
                        if (isUser) userSentMessage = true;

                        boolean isStaff = false;
                        if (msg.getMember() != null) {
                            for (Role r : msg.getMember().getRoles()) {
                                if (staffRoleIds.contains(r.getId())) { isStaff = true; break; }
                            }
                        }

                        if (isStaff || !isUser) { staffResponded = true; break; }
                    }

                    if (!staffResponded && userSentMessage) {
                        StringBuilder mentions = new StringBuilder();
                        for (String r : roleId.split(",")) mentions.append("<@&").append(r.trim()).append("> ");
                        channel.sendMessage("⏳ " + mentions.toString().trim() + ", este ticket aguarda resposta há 10 minutos.").queue();
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Erro ao verificar inatividade do ticket", e);
            }
        }, 10, TimeUnit.MINUTES);
    }

    // ========================
    //   BUTTON HANDLERS
    // ========================

    private static void handleClaimTicket(ButtonInteractionEvent event) {
        if (!InteractionUtils.isStaff(event.getMember())) {
            event.reply("⛔ Apenas membros da equipe podem assumir tickets.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = (TextChannel) event.getChannel();
        String topic = channel.getTopic();

        if (topic != null && topic.contains("MainStaff:")) {
            String existingStaffId = null;
            for (String part : topic.split("\\|")) {
                if (part.trim().startsWith("MainStaff:")) {
                    existingStaffId = part.trim().substring("MainStaff:".length());
                    break;
                }
            }
            if (existingStaffId != null) {
                event.reply("⚠️ Este ticket já foi assumido por <@" + existingStaffId + ">.").setEphemeral(true).queue();
                return;
            }
        }

        // deferEdit() reconhece o clique instantaneamente sem mostrar "pensando..."
        event.deferEdit().queue();

        String staffId = event.getUser().getId();

        String newTopic = (topic != null ? topic : "") + " | MainStaff:" + staffId;

        // Mantém o nome original do ticket e adiciona emoji de bolinha amarela
        String currentName = channel.getName();
        // Remove emoji de prioridade se existir
        String baseName = currentName.startsWith("🔴-") ? currentName.substring("🔴-".length()) : currentName;
        String claimedName = "🟡-" + baseName;
        Guild guild = event.getGuild();

        // Staff roles continuam com acesso - não revogar VIEW_CHANNEL
        // Acesso completo ao claimer (além do cargo)
        ensureStaffRolePermissions(channel);
        channel.upsertPermissionOverride(event.getMember())
            .clear(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
            .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
            .queue(success -> syncLinkedVoicePermissions(channel), error -> LOGGER.warn("Erro ao atualizar permissoes do claimer {}", channel.getId(), error));

        channel.getManager().setTopic(newTopic).setName(claimedName).queue();
        DataManager.incrementTicketStats(staffId, true);

        // Atualiza claimed_by no banco de dados
        if (topic != null) {
            for (String part : topic.split("\\|")) {
                String p = part.trim();
                if (p.startsWith("TicketID:")) {
                    try {
                        int ticketDbId = Integer.parseInt(p.substring("TicketID:".length()));
                        DatabaseManager.updateTicketClaimedBy(ticketDbId, staffId);
                    } catch (Exception ignored) {}
                    break;
                }
            }
        }

        // Atualiza os botões na mensagem original via hook (sem "pensando")
        event.getHook().editOriginalComponents(
            ActionRow.of(
                Button.success("btn_claim_ticket", "🙋 Assumido por " + event.getUser().getName()).asDisabled(),
                Button.danger("btn_close_ticket", "🔒 Fechar Ticket"),
                Button.secondary("btn_staff_panel", "🛠️ Painel Staff")
            )
        ).queue();

        // Envia embed de confirmação como mensagem normal no canal (instantâneo)
        EmbedBuilder claimEmbed = new EmbedBuilder()
            .setTitle("🙋 Ticket Assumido")
            .setDescription("Este ticket foi assumido por " + event.getUser().getAsMention() + ".\n\n" +
                "Todos os membros da equipe continuam com acesso ao ticket. O staff responsável é o indicado acima.")
            .setColor(Color.decode("#2ECC71"))
            .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(claimEmbed.build()).queue();
    }

    private static void handleStaffPanel(ButtonInteractionEvent event) {
        if (!InteractionUtils.isStaff(event.getMember())) {
            event.reply("⛔ Acesso restrito à equipe.").setEphemeral(true).queue();
            return;
        }

        TextChannel panelChannel = (TextChannel) event.getChannel();
        ensureStaffRolePermissions(panelChannel);
        String panelTopic = panelChannel.getTopic();
        if (panelTopic != null && panelTopic.contains("MainStaff:")) {
            String claimedBy = extractTopicField(panelTopic, "MainStaff:");
            boolean isClaimerOrCollab = event.getUser().getId().equals(claimedBy)
                || (panelTopic.contains("Collab:" + event.getUser().getId()))
                || event.getMember().hasPermission(Permission.ADMINISTRATOR);
            if (!isClaimerOrCollab) {
                event.reply("⛔ Este ticket foi assumido por <@" + claimedBy + ">. Apenas ele(a), colaboradores adicionados ou admins podem acessar o painel.").setEphemeral(true).queue();
                return;
            }
        }

        List<Button> row1 = new ArrayList<>();
        row1.add(Button.primary("btn_ticket_priority", "🔴 Prioridade"));
        row1.add(Button.secondary("btn_ticket_rename", "✏️ Renomear"));
        row1.add(Button.secondary("btn_ticket_move", "📂 Mover"));
        row1.add(Button.success("btn_ticket_voice", "🔊 Criar Voz"));

        event.reply("🛠️ **Painel Administrativo**\nSelecione uma ação:")
            .addActionRow(row1)
            .addActionRow(
                Button.secondary("btn_add_member", "➕ Add Membro"),
                Button.secondary("btn_ticket_snippets", "💬 Respostas Rápidas")
            )
            .setEphemeral(true)
            .queue();
    }

    private static void handleSnippets(ButtonInteractionEvent event) {
        try {
            StringSelectMenu menu = StringSelectMenu.create("menu_ticket_snippets")
                .setPlaceholder("Selecione uma resposta rápida...")
                .addOption("Aguarde um momento", "snippet_wait", "Peça para o usuário aguardar", Emoji.fromUnicode("⏳"))
                .addOption("Reinicie o Modem", "snippet_modem", "Sugestão de conexão", Emoji.fromUnicode("📶"))
                .addOption("IP do Servidor", "snippet_ip", "Envia o IP de conexão", Emoji.fromUnicode("🔗"))
                .addOption("Limpar Cache", "snippet_cache", "Instruções para limpar cache", Emoji.fromUnicode("🧹"))
                .addOption("Encaminhando p/ Admin", "snippet_admin", "Avisa que um superior foi chamado", Emoji.fromUnicode("👮"))
                .addOption("Sem Reembolso", "snippet_refund", "Política de não reembolso", Emoji.fromUnicode("💸"))
                .build();

            event.reply(MessagesConfig.get().ticket.snippets.menu_text)
                .addActionRow(menu)
                .setEphemeral(true)
                .queue();
        } catch (Exception e) {
            LOGGER.error("Erro ao abrir menu de snippets", e);
            event.reply(MessagesConfig.get().ticket.snippets.error_load).setEphemeral(true).queue();
        }
    }

    private static void handleVoice(ButtonInteractionEvent event) {
        try {
            TextChannel textChannel = (TextChannel) event.getChannel();
            String ticketId = extractTicketId(textChannel.getName());
            String voiceName = "voice-" + ticketId;

            for (net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel vc : event.getGuild().getVoiceChannels()) {
                if (vc.getName().equals(voiceName)) {
                    event.reply(MessagesConfig.get().ticket.voice.exists.replace("{channel}", vc.getAsMention())).setEphemeral(true).queue();
                    return;
                }
            }

            Category category = textChannel.getParentCategory();
            if (category == null) {
                event.reply(MessagesConfig.get().ticket.voice.no_category).setEphemeral(true).queue();
                return;
            }

            category.createVoiceChannel(voiceName)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT), null)
                .queue(vc -> {
                    String topic = textChannel.getTopic();
                    if (topic != null && topic.contains("OwnerID:")) {
                        String ownerId = extractTopicField(topic, "OwnerID:");
                        if (ownerId != null) {
                            event.getGuild().retrieveMemberById(ownerId).queue(member -> {
                                vc.upsertPermissionOverride(member).grant(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT).queue();
                            }, e -> LOGGER.warn("Nao foi possivel adicionar dono ao canal de voz (saiu do servidor?)"));
                        }
                    }

                    syncVoicePermissions(textChannel, vc);

                    event.reply(MessagesConfig.get().ticket.voice.created.replace("{channel}", vc.getAsMention())).setEphemeral(true).queue(hook -> {
                        hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
                    });
                    textChannel.sendMessage(MessagesConfig.get().ticket.voice.channel_msg.replace("{channel}", vc.getAsMention())).queue(msg -> {
                        msg.delete().queueAfter(30, TimeUnit.SECONDS);
                    });
                }, error -> {
                    LOGGER.error("Erro ao criar canal de voz", error);
                    event.reply(MessagesConfig.get().ticket.voice.error_perms).setEphemeral(true).queue();
                });
        } catch (Exception e) {
            LOGGER.error("Erro critico em btn_ticket_voice", e);
            event.reply(MessagesConfig.get().ticket.voice.error_internal).setEphemeral(true).queue();
        }
    }

    private static void handlePriority(ButtonInteractionEvent event) {
        TextChannel channel = (TextChannel) event.getChannel();
        String name = channel.getName();
        if (name.startsWith("🔴-")) {
            channel.getManager().setName(name.replace("🔴-", "")).queue();
            event.reply("✅ Prioridade removida.").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(3, TimeUnit.SECONDS));
        } else {
            channel.getManager().setName("🔴-" + name).queue();
            event.reply("✅ Prioridade definida como ALTA.").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(3, TimeUnit.SECONDS));
        }
    }

    private static void handleRename(ButtonInteractionEvent event) {
        TextInput input = TextInput.create("ticket_new_name", "Novo Nome", TextInputStyle.SHORT)
            .setPlaceholder(event.getChannel().getName())
            .setRequired(true)
            .setMinLength(3)
            .setMaxLength(100)
            .build();

        Modal modal = Modal.create("modal_ticket_rename", "Renomear Ticket")
            .addActionRow(input)
            .build();

        event.replyModal(modal).queue();
    }

    private static void handleMove(ButtonInteractionEvent event) {
        StringSelectMenu.Builder menu = StringSelectMenu.create("menu_ticket_move_category");
        addOptionIfValid(menu, "Suporte", BotConfig.getTicketCategorySupport(), "🛡️");
        addOptionIfValid(menu, "Denúncia", BotConfig.getTicketCategoryReport(), "🚨");
        addOptionIfValid(menu, "Bug", BotConfig.getTicketCategoryBug(), "🐛");
        addOptionIfValid(menu, "Lore", BotConfig.getTicketCategoryLore(), "📜");

        event.reply("📂 **Mover Ticket**\nSelecione a nova categoria para este ticket:")
            .addActionRow(menu.build())
            .setEphemeral(true)
            .queue();
    }

    private static void handleAddMember(ButtonInteractionEvent event) {
        if (!InteractionUtils.isStaff(event.getMember())) {
            event.reply("❌ Apenas membros da Staff podem adicionar participantes.").setEphemeral(true).queue();
            return;
        }

        TextChannel addChannel = (TextChannel) event.getChannel();
        String addTopic = addChannel.getTopic();
        if (addTopic != null && addTopic.contains("MainStaff:")) {
            String addClaimedBy = extractTopicField(addTopic, "MainStaff:");
            boolean canAdd = event.getUser().getId().equals(addClaimedBy)
                || (addTopic.contains("Collab:" + event.getUser().getId()))
                || event.getMember().hasPermission(Permission.ADMINISTRATOR);
            if (!canAdd) {
                event.reply("⛔ Apenas o staff que assumiu (<@" + addClaimedBy + ">), colaboradores ou admins podem adicionar membros.").setEphemeral(true).queue();
                return;
            }
        }

        EntitySelectMenu userSelect = EntitySelectMenu.create("ticket_add_user", EntitySelectMenu.SelectTarget.USER)
            .setPlaceholder("Selecione um usuário para adicionar")
            .setMinValues(1)
            .setMaxValues(1)
            .build();

        event.reply("👥 **Adicionar Participante**\nSelecione abaixo o usuário que você deseja adicionar a este ticket.")
            .addActionRow(userSelect)
            .setEphemeral(true)
            .queue();
    }

    private static void handleCloseTicket(ButtonInteractionEvent event) {
        TextChannel channel = (TextChannel) event.getChannel();
        String topic = channel.getTopic();
        String userId = event.getUser().getId();

        String ownerId = extractTopicField(topic, "OwnerID:");

        boolean canClose = false;
        if (ownerId != null && ownerId.equals(userId)) canClose = true;
        else if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) canClose = true;
        else if (topic != null && topic.contains("MainStaff:")) {
            String claimedStaffId = extractTopicField(topic, "MainStaff:");
            if (userId.equals(claimedStaffId) || (topic.contains("Collab:" + userId))) canClose = true;
        } else if (InteractionUtils.isStaff(event.getMember())) canClose = true;

        if (!canClose) {
            event.reply("⛔ Apenas membros da equipe (ou o autor do ticket) podem fechar tickets.").setEphemeral(true).queue();
            return;
        }

        event.reply("Tem certeza que deseja fechar este ticket?")
            .addActionRow(
                Button.danger("btn_confirm_close", "Sim, fechar"),
                Button.secondary("btn_cancel_close", "Cancelar")
            )
            .queue();
    }

    private static void handleConfirmClose(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        event.getMessage().delete().queue(null, e -> {});
        TextChannel channel = (TextChannel) event.getChannel();
        closeTicket(channel, event.getGuild(), event.getUser());
    }

    private static void handleCancelClose(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        event.getMessage().delete().queue(null, e -> {});
    }

    private static void handleCancelClearTickets(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        event.getHook().deleteOriginal().queue();
    }

    private static void handleConfirmClearTickets(ButtonInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ Sem permissão.").setEphemeral(true).queue();
            return;
        }

        String type = "all";
        String id = event.getComponentId();
        if (id.contains(":")) type = id.split(":")[1];

        event.deferReply().setEphemeral(true).queue();
        Guild guild = event.getGuild();

        List<String> openCategories = new ArrayList<>();
        if (BotConfig.getTicketCategorySupport() != null) openCategories.add(BotConfig.getTicketCategorySupport());
        if (BotConfig.getTicketCategoryReport() != null) openCategories.add(BotConfig.getTicketCategoryReport());
        if (BotConfig.getTicketCategoryBug() != null) openCategories.add(BotConfig.getTicketCategoryBug());
        if (BotConfig.getTicketCategoryLore() != null) openCategories.add(BotConfig.getTicketCategoryLore());

        String logCategory = BotConfig.getTicketCategoryLog();
        List<TextChannel> channelsToDelete = new ArrayList<>();

        for (TextChannel channel : guild.getTextChannels()) {
            String parentId = channel.getParentCategoryId();
            String name = channel.getName();

            boolean isOpenTicket = (parentId != null && openCategories.contains(parentId)) ||
                                   (name.startsWith("ticket-") || name.startsWith("🔴-ticket-"));
            boolean isClosedTicket = (parentId != null && parentId.equals(logCategory)) ||
                                     name.startsWith("closed-");

            boolean shouldDelete = false;
            if (type.equals("open") && isOpenTicket && !name.startsWith("closed-")) shouldDelete = true;
            else if (type.equals("closed") && isClosedTicket) shouldDelete = true;
            else if (type.equals("all") && (isOpenTicket || isClosedTicket)) shouldDelete = true;

            if (shouldDelete) channelsToDelete.add(channel);
        }

        int deletedCount = 0;
        for (int i = 0; i < channelsToDelete.size(); i++) {
            TextChannel channel = channelsToDelete.get(i);
            // Rate limit: delay each delete to avoid Discord 429 errors
            final int delay = i * 2; // 2 seconds between each delete
            channel.delete().queueAfter(delay, java.util.concurrent.TimeUnit.SECONDS, null, e -> LOGGER.error("Erro ao deletar canal: " + channel.getName()));
            deletedCount++;
        }

        event.getHook().sendMessage("✅ Operação iniciada. " + deletedCount + " canais (" + type + ") estão sendo deletados.").setEphemeral(true).queue();
    }

    private static void handleReview(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        String[] parts = id.split("_");
        String ratingStr = parts[1];
        int rating = Integer.parseInt(ratingStr);
        String stars = "";
        switch (ratingStr) {
            case "5": stars = "⭐⭐⭐⭐⭐"; break;
            case "4": stars = "⭐⭐⭐⭐"; break;
            case "3": stars = "⭐⭐⭐"; break;
            case "2": stars = "⭐⭐"; break;
            case "1": stars = "⭐"; break;
        }

        event.reply("Obrigado pela sua avaliação! " + stars).queue();
        event.getMessage().editMessageComponents().queue();

        try {
            if (parts.length > 2) {
                String staffId = parts[2];
                DataManager.addStaffFeedback(staffId, event.getUser().getId(), rating, null);
                com.midgardbot.features.StaffFeedbackEmbedUpdater.forceUpdate();
            } else {
                if (!event.getMessage().getEmbeds().isEmpty()) {
                    String desc = event.getMessage().getEmbeds().get(0).getDescription();
                    String staffSection = null;
                    if (desc != null) {
                        if (desc.contains("Equipe de Atendimento:")) staffSection = desc.split("Equipe de Atendimento:")[1];
                        else if (desc.contains("Quem participou do atendimento:")) staffSection = desc.split("Quem participou do atendimento:")[1];
                    }

                    if (staffSection != null) {
                        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("<@(\\d+)>").matcher(staffSection);
                        boolean foundAny = false;
                        while (matcher.find()) {
                            String staffId = matcher.group(1);
                            DataManager.addStaffFeedback(staffId, event.getUser().getId(), rating, null);
                            foundAny = true;
                        }
                        if (foundAny) com.midgardbot.features.StaffFeedbackEmbedUpdater.forceUpdate();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao processar avaliação de staff", e);
        }
    }

    // ========================
    //  SELECT MENU HANDLERS
    // ========================

    private static void handleTicketCreation(StringSelectInteractionEvent event) {
        if (DataManager.isBlacklisted(event.getUser().getId())) {
            event.reply("🚫 **Acesso Negado:** Você está na blacklist do servidor e não pode abrir tickets.")
                .setEphemeral(true).queue();
            event.editSelectMenu(event.getSelectMenu().createCopy().build()).queue();
            return;
        }

        String selected = event.getValues().get(0);
        String tempCategoryName = "";
        String tempEmoji = "";
        String tempCategoryId = null;
        String tempRoleId = null;

        switch (selected) {
            case "ticket_support":
                tempCategoryName = "Suporte"; tempEmoji = "🆘";
                tempCategoryId = BotConfig.getTicketCategorySupport(); tempRoleId = BotConfig.getTicketRoleSupport(); break;
            case "ticket_report":
                tempCategoryName = "Denúncia"; tempEmoji = "🚨";
                tempCategoryId = BotConfig.getTicketCategoryReport(); tempRoleId = BotConfig.getTicketRoleReport(); break;
            case "ticket_bug":
                tempCategoryName = "Bug"; tempEmoji = "🐛";
                tempCategoryId = BotConfig.getTicketCategoryBug(); tempRoleId = BotConfig.getTicketRoleBug(); break;
            case "ticket_lore":
                tempCategoryName = "Lore"; tempEmoji = "📜";
                tempCategoryId = BotConfig.getTicketCategoryLore(); tempRoleId = BotConfig.getTicketRoleLore(); break;
            default: return;
        }

        final String categoryName = tempCategoryName;
        final String emojiStr = tempEmoji;
        final String targetCategoryId = tempCategoryId;
        final String targetRoleId = tempRoleId;

        Guild guild = event.getGuild();
        if (guild == null) return;

        // Limite de tickets por categoria
        if (targetCategoryId != null) {
            for (TextChannel c : guild.getTextChannels()) {
                String topic = c.getTopic();
                if (topic != null && topic.contains("OwnerID:" + event.getUser().getId())) {
                    if (c.getParentCategoryId() != null && c.getParentCategoryId().equals(targetCategoryId)) {
                        event.editSelectMenu(event.getSelectMenu().createCopy().build()).queue();
                        event.reply("❌ **Erro:** Você já possui um ticket aberto nesta categoria: " + c.getAsMention())
                            .setEphemeral(true)
                            .queue();
                        return;
                    }
                }
            }
        }

        // Modals para Denúncia, Bug e Suporte
        if (selected.equals("ticket_report")) {
            TextInput user = TextInput.create("report_user", "Usuário Denunciado", TextInputStyle.SHORT).setPlaceholder("Nick ou ID do usuário").setRequired(true).build();
            TextInput reason = TextInput.create("report_reason", "Motivo da Denúncia", TextInputStyle.PARAGRAPH).setPlaceholder("Descreva o que aconteceu...").setRequired(true).build();
            TextInput proof = TextInput.create("report_proof", "Provas (Links)", TextInputStyle.PARAGRAPH).setPlaceholder("Links de prints ou vídeos (Obrigatório)").setRequired(true).build();
            Modal modal = Modal.create("modal_ticket_report", "Nova Denúncia").addActionRow(user).addActionRow(reason).addActionRow(proof).build();
            event.replyModal(modal).queue();
            event.getMessage().editMessageComponents(event.getMessage().getComponents()).queue();
            return;
        }

        if (selected.equals("ticket_bug")) {
            TextInput desc = TextInput.create("bug_desc", "Descrição do Bug", TextInputStyle.PARAGRAPH).setPlaceholder("O que aconteceu?").setRequired(true).build();
            TextInput steps = TextInput.create("bug_steps", "Passos para Reproduzir", TextInputStyle.PARAGRAPH).setPlaceholder("1. Faça isso\n2. Faça aquilo...").setRequired(true).build();
            Modal modal = Modal.create("modal_ticket_bug", "Reportar Bug").addActionRow(desc).addActionRow(steps).build();
            event.replyModal(modal).queue();
            event.getMessage().editMessageComponents(event.getMessage().getComponents()).queue();
            return;
        }

        if (selected.equals("ticket_support")) {
            TextInput desc = TextInput.create("support_desc", "O que você precisa?", TextInputStyle.PARAGRAPH).setPlaceholder("Olâ, descreva como podemos ajudar você...").setRequired(true).build();
            Modal modal = Modal.create("modal_ticket_support", "Solicitar Ajuda").addActionRow(desc).build();
            event.replyModal(modal).queue();
            event.getMessage().editMessageComponents(event.getMessage().getComponents()).queue();
            return;
        }

        // Criação direta para Lore
        event.editSelectMenu(event.getSelectMenu().createCopy().build()).queue();
        createTicketChannel(guild, event.getUser(), categoryName, emojiStr, targetCategoryId, targetRoleId, null);
    }

    private static void handleMoveCategory(StringSelectInteractionEvent event) {
        try {
            String newCategoryId = event.getValues().get(0);
            Category newCategory = event.getGuild().getCategoryById(newCategoryId);

            if (newCategory != null) {
                TextChannel channel = (TextChannel) event.getChannel();
                Guild moveGuild = event.getGuild();

                channel.getManager().setParent(newCategory).queue(
                    success -> {
                        Set<String> allowedRoles = new HashSet<>();
                        // Determinar cargo específico da nova categoria
                        String catSupport = BotConfig.getTicketCategorySupport();
                        String catReport = BotConfig.getTicketCategoryReport();
                        String catBug = BotConfig.getTicketCategoryBug();
                        String catLore = BotConfig.getTicketCategoryLore();
                        String newCatRole = null;
                        if (newCategoryId.equals(catSupport)) newCatRole = BotConfig.getTicketRoleSupport();
                        else if (newCategoryId.equals(catReport)) newCatRole = BotConfig.getTicketRoleReport();
                        else if (newCategoryId.equals(catBug)) newCatRole = BotConfig.getTicketRoleBug();
                        else if (newCategoryId.equals(catLore)) newCatRole = BotConfig.getTicketRoleLore();
                        if (newCatRole != null && !newCatRole.isEmpty()) {
                            for (String r : newCatRole.split(",")) allowedRoles.add(r.trim());
                        }
                        for (Role role : moveGuild.getRoles()) {
                            if (role.hasPermission(Permission.ADMINISTRATOR)) allowedRoles.add(role.getId());
                        }
                        for (net.dv8tion.jda.api.entities.PermissionOverride po : channel.getPermissionOverrides()) {
                            if (po.isRoleOverride() && po.getAllowed().contains(Permission.VIEW_CHANNEL)) allowedRoles.add(po.getId());
                        }
                        for (net.dv8tion.jda.api.entities.PermissionOverride catOverride : newCategory.getPermissionOverrides()) {
                            if (catOverride.isRoleOverride()) {
                                String rid = catOverride.getId();
                                if (!rid.equals(moveGuild.getPublicRole().getId()) && !allowedRoles.contains(rid)) {
                                    Role role = moveGuild.getRoleById(rid);
                                    if (role != null && !role.hasPermission(Permission.ADMINISTRATOR)) {
                                        channel.upsertPermissionOverride(role).deny(Permission.VIEW_CHANNEL).queue();
                                    }
                                }
                            }
                        }
                        String updatedTopic = upsertTopicField(channel.getTopic(), "Category:", newCategory.getName());
                        channel.getManager().setTopic(updatedTopic).queue(null, e -> LOGGER.warn("Erro ao atualizar tópico do ticket movido", e));

                        ensureStaffRolePermissions(channel);
                        int ticketId = extractTicketDbId(channel.getTopic());
                        if (ticketId > 0) {
                            DataManager.updateTicketCategory(ticketId, newCategory.getName());
                        }
                        syncLinkedVoicePermissions(channel);
                        event.reply("✅ Ticket movido para a categoria: **" + newCategory.getName() + "**").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(3, TimeUnit.SECONDS));
                    },
                    error -> event.reply("❌ Erro ao mover ticket: " + error.getMessage()).setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS))
                );
            } else {
                event.reply("❌ Erro: Categoria não encontrada.").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao mover ticket de categoria", e);
            event.reply("❌ Erro interno ao mover ticket.").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
        }
    }

    private static void handleSnippetSelection(StringSelectInteractionEvent event) {
        try {
            String selected = event.getValues().get(0);
            String message = "";

            switch (selected) {
                case "snippet_wait": message = MessagesConfig.get().ticket.snippets.wait; break;
                case "snippet_modem": message = MessagesConfig.get().ticket.snippets.modem; break;
                case "snippet_ip": message = MessagesConfig.get().ticket.snippets.ip; break;
                case "snippet_cache": message = MessagesConfig.get().ticket.snippets.cache; break;
                case "snippet_admin": message = MessagesConfig.get().ticket.snippets.admin; break;
                case "snippet_refund": message = MessagesConfig.get().ticket.snippets.refund; break;
            }

            if (!message.isEmpty()) {
                event.getChannel().sendMessage(message).queue();
                event.reply(MessagesConfig.get().ticket.snippets.success).setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(3, TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao enviar snippet", e);
            event.reply("❌ Erro ao enviar resposta rápida.").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
        }
    }

    private static void handleAddUserEntity(EntitySelectInteractionEvent event) {
        event.deferReply(true).queue();
        Guild guild = event.getGuild();
        if (guild == null) {
            event.getHook().editOriginal("âŒ Guild nÃ£o encontrada.").queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }

        List<User> selectedUsers = event.getMentions().getUsers();
        if (selectedUsers.isEmpty()) {
            event.getHook().editOriginal("❌ Usuário inválido ou não encontrado no servidor.").queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }

        Member targetMember = guild.getMemberById(selectedUsers.get(0).getId());
        if (targetMember == null) {
            guild.retrieveMemberById(selectedUsers.get(0).getId()).queue(
                member -> finishAddUserEntity(event, member),
                error -> event.getHook().editOriginal("âŒ UsuÃ¡rio invÃ¡lido ou nÃ£o encontrado no servidor.").queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS))
            );
            return;
        }

        finishAddUserEntity(event, targetMember);
    }

    private static void finishAddUserEntity(EntitySelectInteractionEvent event, Member targetMember) {
        TextChannel channel = (TextChannel) event.getChannel();
        String topic = channel.getTopic();

        if (targetMember.getId().equals(event.getUser().getId())) {
            event.getHook().editOriginal("❌ Você não pode adicionar a si mesmo.").queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }
        if (targetMember.hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().editOriginal("⚠️ Administradores já possuem acesso a todos os tickets automaticamente. Não é necessário adicioná-los.").queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }
        if (topic != null && topic.contains("OwnerID:" + targetMember.getId())) {
            event.getHook().editOriginal("❌ Este usuário já é o dono do ticket.").queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }
        if (topic != null && topic.contains("Collab:" + targetMember.getId())) {
            restoreExistingTicketAccess(event, channel, targetMember);
            return;
        }

        // Registra collab no cache em memória ANTES do REST call para evitar race condition

        channel.upsertPermissionOverride(targetMember)
            .clear(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
            .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
            .queue(success -> {
                COLLAB_CACHE.computeIfAbsent(channel.getId(), k -> ConcurrentHashMap.newKeySet()).add(targetMember.getId());
                syncLinkedVoicePermissions(channel);
                if (topic != null && !topic.contains("Collab:" + targetMember.getId())) {
                    String newTopic = topic + " | Collab:" + targetMember.getId();
                    if (newTopic.length() > 1000) {
                        event.getHook().editOriginal("⚠️ Usuário adicionado, mas o registro de participantes está cheio.").queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
                        channel.sendMessage("✅ **" + targetMember.getAsMention() + "** foi adicionado ao ticket.").queue();
                    } else {
                        channel.getManager().setTopic(newTopic).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_CHANNEL));
                        channel.sendMessage("✅ **" + targetMember.getAsMention() + "** foi adicionado ao ticket.").queue();
                        event.getHook().editOriginal("✅ Operação concluída.").queue(m -> m.delete().queueAfter(3, TimeUnit.SECONDS));
                    }
                } else {
                    channel.sendMessage("✅ **" + targetMember.getAsMention() + "** já estava no ticket.").queue();
                    event.getHook().editOriginal("✅ Usuário já estava registrado.").queue(m -> m.delete().queueAfter(3, TimeUnit.SECONDS));
                }
            }, error -> {
                LOGGER.error("Erro ao adicionar permissão no ticket", error);
                event.getHook().editOriginal("❌ Erro ao atualizar permissões: " + error.getMessage()).queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            });
    }

    // ========================
    //    MODAL HANDLERS
    // ========================

    private static void handleReportModal(ModalInteractionEvent event) {
        String user = event.getValue("report_user").getAsString();
        String reason = event.getValue("report_reason").getAsString();
        String proof = event.getValue("report_proof").getAsString();

        String content = ">>> **👤 Usuário Denunciado:** " + user + "\n\n" +
                         "**⚖️ Motivo:**\n" + reason + "\n\n" +
                         "**📸 Provas:**\n" + proof;

        event.reply("✅ Denúncia recebida! Criando seu ticket...").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
        createTicketChannel(event.getGuild(), event.getUser(), "Denúncia", "🚨", BotConfig.getTicketCategoryReport(), BotConfig.getTicketRoleReport(), content);
    }

    private static void handleBugModal(ModalInteractionEvent event) {
        String desc = event.getValue("bug_desc").getAsString();
        String steps = event.getValue("bug_steps").getAsString();

        String content = ">>> **📌 Descrição do Problema**\n" + desc + "\n\n" +
                         "**👣 Passos para Reproduzir**\n" + steps;

        event.reply("✅ Report recebido! Criando seu ticket...").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
        createTicketChannel(event.getGuild(), event.getUser(), "Bug", "🐛", BotConfig.getTicketCategoryBug(), BotConfig.getTicketRoleBug(), content);
    }

    private static void handleSupportModal(ModalInteractionEvent event) {
        String desc = event.getValue("support_desc").getAsString();
        String content = ">>> **❓ Dúvida/Problema**\n" + desc;

        event.reply("✅ Solicitação recebida! Criando seu ticket...").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
        createTicketChannel(event.getGuild(), event.getUser(), "Suporte", "🆘", BotConfig.getTicketCategorySupport(), BotConfig.getTicketRoleSupport(), content);
    }

    private static void handleRenameModal(ModalInteractionEvent event) {
        String newName = event.getValue("ticket_new_name").getAsString();
        TextChannel channel = (TextChannel) event.getChannel();
        channel.getManager().setName(newName).queue(
            success -> event.reply("✅ Ticket renomeado para: **" + newName + "**").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(3, TimeUnit.SECONDS)),
            error -> event.reply("❌ Erro ao renomear ticket (Limite do Discord: 2x a cada 10 min).").setEphemeral(true).queue(hook -> hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS))
        );
    }

    // ========================
    //   CLOSE TICKET (PUBLIC)
    // ========================

    public static void closeTicket(TextChannel channel, Guild guild, User closer) {
        clearCollabCache(channel.getId());

        if (closer != null && !closer.isBot()) {
            DataManager.incrementTicketStats(closer.getId(), false);
        }

        String topic = channel.getTopic();
        String ownerId = null;
        String mainStaffId = "N/A";
        List<String> collabIds = new ArrayList<>();

        if (topic != null) {
            String[] parts = topic.split("\\|");
            for (String part : parts) {
                String p = part.trim();
                if (p.startsWith("OwnerID:")) ownerId = p.substring("OwnerID:".length());
                else if (p.startsWith("MainStaff:")) mainStaffId = p.substring("MainStaff:".length());
                else if (p.startsWith("Collab:")) collabIds.add(p.substring("Collab:".length()));
            }
        }

        StringBuilder staffDisplay = new StringBuilder();
        if (!mainStaffId.equals("N/A")) {
            staffDisplay.append("👑 <@").append(mainStaffId).append("> (Responsável)\n");
        } else {
            String closerMention = (closer != null) ? closer.getAsMention() : "Sistema Automático";
            staffDisplay.append("🔒 ").append(closerMention).append(" (Fechou o ticket)\n");
        }
        for (String cid : collabIds) staffDisplay.append("🤝 <@").append(cid).append("> (Colaborador)\n");

        final String finalStaffDisplay = staffDisplay.toString();

        // DM + Feedback
        if (ownerId != null) {
            if (closer != null && closer.getId().equals(ownerId)) {
                LOGGER.info("Feedback ignorado para ticket {}: Fechado pelo próprio dono.", channel.getName());
            } else {
                List<String> rawStaffsToRate = new ArrayList<>();
                if (!mainStaffId.equals("N/A")) rawStaffsToRate.add(mainStaffId);
                rawStaffsToRate.addAll(collabIds);
                final List<String> staffsToRate = rawStaffsToRate.stream().distinct().collect(Collectors.toList());

                guild.getJDA().retrieveUserById(ownerId).queue(user -> {
                    user.openPrivateChannel().queue(dm -> {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("user", user.getAsMention());
                        placeholders.put("staff_list", finalStaffDisplay);

                        EmbedBuilder closedEmbed = MessagesConfig.buildEmbed(MessagesConfig.get().ticket.closed_dm, placeholders);
                        closedEmbed.setFooter("Midgard RPG • Sistema de Tickets", guild.getJDA().getSelfUser().getAvatarUrl());
                        closedEmbed.setTimestamp(java.time.Instant.now());
                        dm.sendMessageEmbeds(closedEmbed.build()).queue(
                            null, new net.dv8tion.jda.api.exceptions.ErrorHandler().ignore(net.dv8tion.jda.api.requests.ErrorResponse.CANNOT_SEND_TO_USER)
                        );

                        for (String staffId : staffsToRate) {
                            guild.getJDA().retrieveUserById(staffId).queue(staffUser -> {
                                EmbedBuilder rateEmbed = new EmbedBuilder()
                                    .setTitle("⭐ Avalie o Atendimento")
                                    .setDescription("Como foi o atendimento de **" + staffUser.getName() + "**?")
                                    .setThumbnail(staffUser.getEffectiveAvatarUrl())
                                    .setColor(Color.ORANGE);
                                dm.sendMessageEmbeds(rateEmbed.build())
                                    .addActionRow(
                                        Button.success("review_5_" + staffId, "⭐⭐⭐⭐⭐"),
                                        Button.primary("review_4_" + staffId, "⭐⭐⭐⭐"),
                                        Button.secondary("review_3_" + staffId, "⭐⭐⭐"),
                                        Button.secondary("review_2_" + staffId, "⭐⭐"),
                                        Button.danger("review_1_" + staffId, "⭐")
                                    ).queue(null, new net.dv8tion.jda.api.exceptions.ErrorHandler().ignore(net.dv8tion.jda.api.requests.ErrorResponse.CANNOT_SEND_TO_USER));
                            }, e -> {});
                        }
                    }, e -> LOGGER.warn("Não foi possível abrir DM com " + user.getName()));
                }, e -> LOGGER.warn("Usuário dono do ticket não encontrado: " + e.getMessage()));
            }
        }

        final String finalMainStaffId = mainStaffId;
        final List<String> finalCollabIds = collabIds;
        final String finalOwnerId = ownerId;

        // Transcrição HTML
        channel.getIterableHistory().takeAsync(1000).thenAccept(messages -> {
            try {
                java.io.InputStream transcriptStream = TranscriptUtils.generate(messages);
                java.io.InputStream transcriptStreamDM = TranscriptUtils.generate(messages);

                FileUpload transcriptFile = FileUpload.fromData(transcriptStream, "transcript-" + channel.getName() + ".html");
                FileUpload transcriptFileDM = FileUpload.fromData(transcriptStreamDM, "transcript-" + channel.getName() + ".html");

                EmbedBuilder logEmbed = new EmbedBuilder()
                    .setTitle("🔒 Ticket Fechado")
                    .setColor(Color.GRAY)
                    .setDescription("Este ticket foi finalizado.");

                String closerMention = (closer != null) ? closer.getAsMention() : "Sistema Automático";
                logEmbed.addField("👤 Fechado por", closerMention, true);
                if (!finalMainStaffId.equals("N/A")) logEmbed.addField("👑 Staff Responsável", "<@" + finalMainStaffId + ">", true);
                if (!finalCollabIds.isEmpty()) {
                    StringBuilder collabStr = new StringBuilder();
                    for (String cid : finalCollabIds) collabStr.append("<@").append(cid).append(">\n");
                    logEmbed.addField("🤝 Colaboradores", collabStr.toString(), false);
                }
                logEmbed.setFooter("ID do Canal: " + channel.getId());
                logEmbed.setTimestamp(java.time.Instant.now());

                String serverLogId = BotConfig.getLogChannelId();
                if (serverLogId != null) {
                    TextChannel serverLog = guild.getTextChannelById(serverLogId);
                    if (serverLog != null) {
                        serverLog.sendMessageEmbeds(logEmbed.setTitle("📑 Log de Ticket: " + channel.getName()).build())
                                 .addFiles(transcriptFile).queue();
                    }
                }

                if (finalOwnerId != null) {
                    guild.getJDA().retrieveUserById(finalOwnerId).queue(user -> {
                        user.openPrivateChannel().queue(dm -> {
                            dm.sendMessage("📄 **Cópia do seu atendimento:**")
                              .addFiles(transcriptFileDM)
                              .queue(null, new net.dv8tion.jda.api.exceptions.ErrorHandler().ignore(net.dv8tion.jda.api.requests.ErrorResponse.CANNOT_SEND_TO_USER));
                        }, e -> LOGGER.warn("Não foi possível abrir DM para enviar transcript ao usuário " + finalOwnerId));
                    }, e -> LOGGER.warn("Usuário dono do ticket não encontrado para transcript: " + finalOwnerId));
                }

                String logCategoryId = BotConfig.getTicketCategoryLog();
                boolean canArchive = false;
                if (logCategoryId != null && !logCategoryId.isEmpty()) {
                    Category logCategory = guild.getCategoryById(logCategoryId);
                    if (logCategory != null) canArchive = true;
                }

                com.midgardbot.features.tickets.TicketArchiver archiver = InteractionUtils.getTicketArchiver();
                if (archiver != null && canArchive) {
                    channel.sendMessage(MessagesConfig.get().ticket.close.archiving).queue();
                    archiver.archiveTicket(channel, closer);
                } else {
                    // Salvar transcrição no banco antes de deletar
                    try {
                        int ticketDbId = -1;
                        String claimedBy = null;
                        List<String> storedCollabIds = new ArrayList<>();
                        if (channel.getTopic() != null) {
                            for (String part : channel.getTopic().split("\\|")) {
                                String p = part.trim();
                                if (p.startsWith("TicketID:")) {
                                    try { ticketDbId = Integer.parseInt(p.substring("TicketID:".length())); } catch (Exception ignored) {}
                                } else if (p.startsWith("MainStaff:")) {
                                    claimedBy = p.substring("MainStaff:".length());
                                } else if (p.startsWith("Collab:")) {
                                    storedCollabIds.add(p.substring("Collab:".length()));
                                }
                            }
                        }
                        List<Map<String, String>> chatLog = new ArrayList<>();
                        List<net.dv8tion.jda.api.entities.Message> reversed = new ArrayList<>(messages);
                        Collections.reverse(reversed);
                        for (net.dv8tion.jda.api.entities.Message msg : reversed) {
                            Map<String, String> entry = new LinkedHashMap<>();
                            entry.put("author", msg.getAuthor().getName());
                            entry.put("authorId", msg.getAuthor().getId());
                            entry.put("content", msg.getContentRaw());
                            entry.put("timestamp", msg.getTimeCreated().toString());
                            chatLog.add(entry);
                        }
                        String jsonContent = new Gson().toJson(chatLog);
                        String priority = (channel.getTopic() != null && channel.getTopic().contains("[HIGH-PRIORITY]")) ? "HIGH" : "NORMAL";
                        if (ticketDbId > 0) {
                            String collaboratorIds = storedCollabIds.isEmpty() ? null : String.join(",", storedCollabIds);
                            DatabaseManager.updateTicket(ticketDbId, jsonContent, priority, claimedBy, collaboratorIds);
                        }
                    } catch (Exception dbErr) {
                        LOGGER.warn("Erro ao salvar transcrição do ticket sem archiver", dbErr);
                    }
                    channel.sendMessage(MessagesConfig.get().ticket.close.no_archive).queue();
                    channel.delete().queueAfter(10, TimeUnit.SECONDS);
                }

                // Deletar canal de voz associado
                try {
                    String ticketId = extractTicketId(channel.getName());
                    String voiceName = "voice-" + ticketId;
                    for (net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel vc : guild.getVoiceChannels()) {
                        if (vc.getName().equals(voiceName)) { vc.delete().queue(); break; }
                    }
                } catch (Exception e) {
                    LOGGER.error("Erro ao deletar voz do ticket", e);
                }

            } catch (Exception e) {
                LOGGER.error("Erro ao processar fechamento do ticket", e);
                channel.sendMessage(MessagesConfig.get().ticket.close.error).queue();
                channel.delete().queueAfter(10, TimeUnit.SECONDS);
            }
        }).exceptionally(e -> {
            LOGGER.error("Erro critico ao recuperar historico do ticket", e);
            channel.delete().queueAfter(10, TimeUnit.SECONDS);
            return null;
        });
    }

    // ========================
    //      UTILITIES
    // ========================

    /**
     * Extrai um campo do tópico do canal. Ex: extractTopicField(topic, "OwnerID:") → "123456789"
     */
    public static String extractTopicField(String topic, String fieldPrefix) {
        if (topic == null) return null;
        for (String part : topic.split("\\|")) {
            String p = part.trim();
            if (p.startsWith(fieldPrefix)) return p.substring(fieldPrefix.length());
        }
        return null;
    }

    private static int extractTicketDbId(String topic) {
        String ticketId = extractTopicField(topic, "TicketID:");
        if (ticketId == null || ticketId.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(ticketId);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String upsertTopicField(String topic, String fieldPrefix, String value) {
        List<String> parts = new ArrayList<>();
        boolean replaced = false;

        if (topic != null && !topic.isBlank()) {
            for (String rawPart : topic.split("\\|")) {
                String part = rawPart.trim();
                if (part.isEmpty()) continue;
                if (part.startsWith(fieldPrefix)) {
                    parts.add(fieldPrefix + value);
                    replaced = true;
                } else {
                    parts.add(part);
                }
            }
        }

        if (!replaced) {
            parts.add(fieldPrefix + value);
        }

        return String.join(" | ", parts);
    }

    private static String extractTicketId(String channelName) {
        String ticketId = "xxxx";
        // Remove prefixos de emoji (🟡-, 🔴-, etc.) para extrair o ID
        String cleanName = channelName.replaceAll("^[^a-zA-Z]*-", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("ticket-.*?(\\d{4})").matcher(cleanName);
        if (m.find()) {
            ticketId = m.group(1);
        } else {
            // Fallback: tenta extrair qualquer sequência de 4 dígitos
            m = java.util.regex.Pattern.compile("(\\d{4})").matcher(channelName);
            if (m.find()) ticketId = m.group(1);
        }
        return ticketId;
    }

    private static Button[] buildTicketControlButtons(Guild guild, String topic) {
        String claimedBy = extractTopicField(topic, "MainStaff:");
        Button claimButton;
        if (claimedBy != null && !claimedBy.isBlank()) {
            String label = buildClaimButtonLabel(guild, claimedBy);
            claimButton = Button.success("btn_claim_ticket", label).asDisabled();
        } else {
            claimButton = Button.success("btn_claim_ticket", "Assumir Ticket");
        }

        return new Button[] {
            claimButton,
            Button.danger("btn_close_ticket", "Fechar Ticket"),
            Button.secondary("btn_staff_panel", "Painel Staff")
        };
    }

    private static String buildClaimButtonLabel(Guild guild, String claimedBy) {
        if (guild == null || claimedBy == null || claimedBy.isBlank()) {
            return "Ticket Assumido";
        }

        Member member = guild.getMemberById(claimedBy);
        String name = member != null ? member.getUser().getName() : claimedBy;
        String label = "Assumido por " + name;
        return label.length() > 80 ? label.substring(0, 77) + "..." : label;
    }

    private static void syncLinkedVoicePermissions(TextChannel textChannel) {
        VoiceChannel voiceChannel = findLinkedVoiceChannel(textChannel);
        if (voiceChannel == null) {
            return;
        }

        syncVoicePermissions(textChannel, voiceChannel);
    }

    private static void syncVoicePermissions(TextChannel textChannel, VoiceChannel voiceChannel) {
        for (net.dv8tion.jda.api.entities.PermissionOverride override : textChannel.getPermissionOverrides()) {
            if (!override.getAllowed().contains(Permission.VIEW_CHANNEL)) {
                continue;
            }

            if (override.isMemberOverride()) {
                Member member = override.getMember();
                if (member == null) {
                    continue;
                }

                voiceChannel.upsertPermissionOverride(member)
                    .clear(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
                    .grant(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
                    .queue(null, error -> LOGGER.debug("Erro ao sincronizar permissao de voz do membro {}", member.getId(), error));
                continue;
            }

            if (!override.isRoleOverride()) {
                continue;
            }

            Role role = override.getRole();
            if (role == null || role.getId().equals(textChannel.getGuild().getPublicRole().getId())) {
                continue;
            }

            voiceChannel.upsertPermissionOverride(role)
                .clear(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
                .grant(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
                .queue(null, error -> LOGGER.debug("Erro ao sincronizar permissao de voz do cargo {}", role.getId(), error));
        }
    }

    private static VoiceChannel findLinkedVoiceChannel(TextChannel textChannel) {
        String voiceName = "voice-" + extractTicketId(textChannel.getName());
        for (VoiceChannel voiceChannel : textChannel.getGuild().getVoiceChannels()) {
            if (voiceChannel.getName().equals(voiceName)) {
                return voiceChannel;
            }
        }
        return null;
    }

    private static void restoreExistingTicketAccess(EntitySelectInteractionEvent event, TextChannel channel, Member targetMember) {
        channel.upsertPermissionOverride(targetMember)
            .clear(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
            .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
            .queue(success -> {
                COLLAB_CACHE.computeIfAbsent(channel.getId(), k -> ConcurrentHashMap.newKeySet()).add(targetMember.getId());
                syncLinkedVoicePermissions(channel);
                channel.sendMessage("**" + targetMember.getAsMention() + "** teve o acesso ao ticket restaurado.").queue();
                event.getHook().editOriginal("Permissoes do usuario restauradas.").queue(m -> m.delete().queueAfter(3, TimeUnit.SECONDS));
            }, error -> {
                LOGGER.error("Erro ao restaurar permissao do colaborador no ticket", error);
                event.getHook().editOriginal("Erro ao restaurar permissoes: " + error.getMessage()).queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            });
    }

    private static void addOptionIfValid(StringSelectMenu.Builder menu, String label, String value, String emoji) {
        if (value != null && !value.isEmpty() && !value.equals("000000000000000000")) {
            menu.addOption(label, value, emoji != null ? Emoji.fromUnicode(emoji) : null);
        }
    }
}
