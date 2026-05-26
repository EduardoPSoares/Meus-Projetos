package com.midgardbot.commands;

import com.midgardbot.commands.handlers.*;
import com.midgardbot.config.BotConfig;
import com.midgardbot.config.Constants;
import com.midgardbot.data.DataManager;
import com.midgardbot.features.whitelist.WhitelistCache;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Dispatcher central de interações do Discord.
 * Delega para handlers especializados em cada domínio:
 * - TicketHandler: Tickets
 * - WhitelistWizardHandler: Formulário de whitelist (jogador)
 * - WhitelistReviewHandler: Revisão de whitelist (staff)
 * - MiscHandler: Vinculação de conta, status, guia, etc.
 */
public class InteractionManager extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractionManager.class);
    private final List<ISlashCommand> commands = new ArrayList<>();

    public void addCommand(ISlashCommand command) {
        commands.add(command);
    }

    {
        // Registro automático dos comandos internos
        commands.add(new com.midgardbot.commands.impl.StaffFeedbackCommand());
        commands.add(new com.midgardbot.commands.impl.AtualizarFeedbackStaffCommand());
        commands.add(new com.midgardbot.commands.impl.SyncDatabaseCommand());
        commands.add(new com.midgardbot.commands.impl.NoticiaCommand());
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // Separar comandos: todos vs apenas os permitidos no servidor de staffs
        List<CommandData> allCommands = new ArrayList<>();
        List<CommandData> staffGuildCommands = new ArrayList<>();

        for (ISlashCommand command : commands) {
            net.dv8tion.jda.api.interactions.commands.build.SlashCommandData data = Commands.slash(command.getName(), command.getDescription());
            if (command.getOptions() != null && !command.getOptions().isEmpty()) {
                data.addOptions(command.getOptions());
            }
            if (command.getSubcommands() != null && !command.getSubcommands().isEmpty()) {
                data.addSubcommands(command.getSubcommands());
            }
            allCommands.add(data);
            if (command.allowedInStaffGuild()) {
                staffGuildCommands.add(data);
            }
        }

        String staffGuildId = BotConfig.get("STAFF_GUILD_ID");

        for (Guild guild : event.getJDA().getGuilds()) {
            boolean isStaffGuild = staffGuildId != null && !staffGuildId.isEmpty()
                    && guild.getId().equals(staffGuildId);

            List<CommandData> cmds = isStaffGuild ? staffGuildCommands : allCommands;

            guild.updateCommands().addCommands(cmds).queue(
                success -> LOGGER.info("Comandos registrados no servidor: {} ({} comandos{})",
                        guild.getName(), cmds.size(), isStaffGuild ? " — modo staff" : ""),
                error -> LOGGER.error("Falha ao registrar comandos no servidor: {} - {} (Verifique se o bot tem o escopo 'applications.commands')", guild.getName(), error.getMessage())
            );
        }

        // Agendar limpeza de cache a cada 1 hora
        InteractionUtils.SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                WhitelistCache.cleanup();
                long now = System.currentTimeMillis();
                InteractionUtils.interactionDebounce.entrySet().removeIf(entry -> now - entry.getValue() > Constants.CACHE_CLEANUP_INTERVAL_MS);
                // Limpa caches de paginação para evitar memory leak
                InteractionUtils.staffViewPages.clear();
                InteractionUtils.staffStatsPages.clear();
                InteractionUtils.staffMessages.clear();
                InteractionUtils.logViewPages.clear();
                LOGGER.debug("Cache de paginação limpo com sucesso");
            } catch (Exception e) {
                LOGGER.error("Erro ao limpar cache", e);
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (isMaintenanceBlocked(event)) return;

        String commandName = event.getName();
        for (ISlashCommand command : commands) {
            if (command.getName().equals(commandName)) {
                // Bloquear comandos não permitidos no servidor de staffs
                String staffGuildId = BotConfig.get("STAFF_GUILD_ID");
                if (staffGuildId != null && !staffGuildId.isEmpty()
                        && event.getGuild() != null
                        && event.getGuild().getId().equals(staffGuildId)
                        && !command.allowedInStaffGuild()) {
                    event.reply("❌ Este comando não está disponível neste servidor.")
                            .setEphemeral(true).queue();
                    return;
                }

                // Rate limiting
                if (com.midgardbot.utils.PermissionUtils.isRateLimited(event.getUser().getId(), commandName)) {
                    event.reply("⏳ Aguarde alguns segundos antes de usar este comando novamente.").setEphemeral(true).queue();
                    return;
                }

                // Verificação de permissões centralizada
                if (!com.midgardbot.utils.PermissionUtils.hasPermission(event.getMember(), command.getPermissionKey(), commandName)) {
                    event.replyEmbeds(EmbedUtils.createError(
                        "Sem Permissão",
                        "Você não tem permissão para usar este comando.",
                        event.getJDA().getSelfUser()
                    ).build()).setEphemeral(true).queue();
                    return;
                }

                try {
                    command.handle(event);
                } catch (Exception e) {
                    LOGGER.error("Erro ao executar comando slash: " + commandName, e);
                    if (!event.isAcknowledged()) {
                        event.reply("Ocorreu um erro ao executar o comando.").setEphemeral(true).queue();
                    }
                }
                return;
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (isMaintenanceBlocked(event)) return;

        // Debounce Global (Anti-Double Click)
        String debounceKey = event.getUser().getId() + ":" + event.getComponentId();
        long now = System.currentTimeMillis();
        Long last = InteractionUtils.interactionDebounce.get(debounceKey);
        if (last != null && now - last < 2000) {
            event.deferEdit().queue();
            return;
        }
        InteractionUtils.interactionDebounce.put(debounceKey, now);

        try {
            if (PlayerControlHandler.handleButton(event)) return;
            if (PermaDeathHandler.handleButton(event)) return;
            if (IntimacaoHandler.handleButton(event)) return;
            if (NewsHandler.handleButton(event)) return;
            if (TicketHandler.handleButton(event)) return;
            if (WhitelistWizardHandler.handleButton(event)) return;
            if (WhitelistReviewHandler.handleButton(event)) return;
            if (MiscHandler.handleButton(event)) return;

            LOGGER.warn("Botão não tratado: {}", event.getComponentId());
        } catch (Exception e) {
            LOGGER.error("Erro ao processar botão: " + event.getComponentId(), e);
            if (!event.isAcknowledged()) {
                event.replyEmbeds(EmbedUtils.createError(
                    "Erro Interno",
                    "Ocorreu um erro ao processar sua solicitação.\nPor favor, tente novamente ou contate um administrador.",
                    event.getJDA().getSelfUser()
                ).build()).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (isMaintenanceBlocked(event)) return;

        try {
            if (PlayerControlHandler.handleSelectMenu(event)) return;
            if (PermaDeathHandler.handleSelectMenu(event)) return;
            if (NewsHandler.handleSelectMenu(event)) return;
            if (TicketHandler.handleSelectMenu(event)) return;
            if (MiscHandler.handleSelectMenu(event)) return;

            LOGGER.warn("Select menu não tratado: {}", event.getComponentId());
        } catch (Exception e) {
            LOGGER.error("Erro ao processar select menu: " + event.getComponentId(), e);
            if (!event.isAcknowledged()) {
                event.reply("❌ Ocorreu um erro ao processar sua solicitação.").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
        if (isMaintenanceBlocked(event)) return;

        try {
            if (TicketHandler.handleEntitySelect(event)) return;

            LOGGER.warn("Entity select não tratado: {}", event.getComponentId());
        } catch (Exception e) {
            LOGGER.error("Erro ao processar EntitySelectInteraction", e);
            if (!event.isAcknowledged()) {
                event.reply("❌ Ocorreu um erro ao processar sua solicitação.").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        try {
            if (PlayerControlHandler.handleModal(event)) return;
            if (PermaDeathHandler.handleModal(event)) return;
            if (NewsHandler.handleModal(event)) return;
            if (TicketHandler.handleModal(event)) return;
            if (WhitelistWizardHandler.handleModal(event)) return;
            if (WhitelistReviewHandler.handleModal(event)) return;
            if (MiscHandler.handleModal(event)) return;

            LOGGER.warn("Modal não tratado: {}", event.getModalId());
        } catch (Exception e) {
            LOGGER.error("Erro ao processar modal: " + event.getModalId(), e);
            if (!event.isAcknowledged()) {
                event.replyEmbeds(EmbedUtils.createError(
                    "Erro Interno",
                    "Ocorreu um erro ao processar seu formulário.\nPor favor, tente novamente.",
                    event.getJDA().getSelfUser()
                ).build()).setEphemeral(true).queue();
            }
        }
    }

    // ========================
    //    UTILITY
    // ========================

    private boolean isMaintenanceBlocked(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
        if (!DataManager.isMaintenanceMode()) return false;
        net.dv8tion.jda.api.entities.Member member = event.getMember();
        if (member == null || member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) return false;
        event.replyEmbeds(EmbedUtils.createError(
            "🚧 Manutenção",
            "O bot está em modo de manutenção.\nApenas administradores podem usar este recurso no momento.",
            event.getJDA().getSelfUser()
        ).build()).setEphemeral(true).queue();
        return true;
    }
}
