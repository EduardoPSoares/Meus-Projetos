package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.commands.handlers.TicketHandler;
import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DatabaseManager;
import com.midgardbot.features.tickets.TicketArchiver;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class TicketCommand implements ISlashCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketCommand.class);

    private final TicketArchiver archiver;

    public TicketCommand(TicketArchiver archiver) {
        this.archiver = archiver;
    }

    @Override
    public String getName() {
        return "ticket";
    }

    @Override
    public String getDescription() {
        return "Gerencia arquivamento e tickets";
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of(
            new SubcommandData("priority", "Define a prioridade do ticket")
                .addOptions(new OptionData(OptionType.STRING, "level", "Nivel de prioridade", true)
                    .addChoice("Alta (HIGH)", "HIGH")
                    .addChoice("Normal", "NORMAL")),
            new SubcommandData("panel", "Recria o painel principal do ticket atual"),
            new SubcommandData("list", "Lista tickets arquivados"),
            new SubcommandData("view", "Visualiza o transcript de um ticket")
                .addOption(OptionType.INTEGER, "id", "ID do ticket", true),
            new SubcommandData("reopen", "Reabre um ticket arquivado (restaura o canal)")
                .addOption(OptionType.INTEGER, "id", "ID do ticket", true),
            new SubcommandData("clean", "Limpa tickets antigos")
                .addOptions(new OptionData(OptionType.INTEGER, "days", "Dias para manter", true)
                    .addChoice("1 Dia", 1)
                    .addChoice("7 Dias", 7)
                    .addChoice("14 Dias", 14)
                    .addChoice("30 Dias", 30)),
            new SubcommandData("delete", "Deleta um ticket arquivado")
                .addOption(OptionType.INTEGER, "id", "ID do ticket", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_TICKET";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (event.getMember() == null) {
            event.reply("Erro: este comando so pode ser usado dentro de um servidor.").setEphemeral(true).queue();
            return;
        }

        // Permission check handled by InteractionManager or fallback below.
        if (BotConfig.getAuthorizedRoles("PERM_CMD_TICKET").isEmpty()
            && !event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {
            event.replyEmbeds(EmbedUtils.createError(
                "Sem permissao",
                "Voce precisa de permissao para gerenciar canais ou cargo configurado.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            return;
        }

        switch (subcommand) {
            case "priority":
                if (!(event.getChannel() instanceof TextChannel channel)) {
                    event.reply("Erro: este subcomando so pode ser usado em canais de texto de ticket.").setEphemeral(true).queue();
                    return;
                }

                String priorityLevel = event.getOption("level").getAsString();
                String topic = channel.getTopic() == null ? "" : channel.getTopic();
                if (!topic.contains("TicketID:")) {
                    event.reply("Erro: este canal nao parece ser um ticket valido.").setEphemeral(true).queue();
                    return;
                }

                if ("HIGH".equals(priorityLevel)) {
                    if (!topic.contains("[HIGH-PRIORITY]")) {
                        channel.getManager().setTopic(topic + " [HIGH-PRIORITY]").queue();
                        event.reply("Prioridade definida como ALTA. Este ticket nao sera excluido automaticamente do banco.").setEphemeral(true).queue();
                    } else {
                        event.reply("Este ticket ja esta marcado como alta prioridade.").setEphemeral(true).queue();
                    }
                } else {
                    channel.getManager().setTopic(topic.replace("[HIGH-PRIORITY]", "").trim()).queue();
                    event.reply("Prioridade definida como NORMAL.").setEphemeral(true).queue();
                }
                break;

            case "panel":
                if (!(event.getChannel() instanceof TextChannel panelChannel)) {
                    event.reply("Erro: este subcomando so pode ser usado em canais de texto de ticket.").setEphemeral(true).queue();
                    return;
                }

                String panelTopic = panelChannel.getTopic() == null ? "" : panelChannel.getTopic();
                if (!panelTopic.contains("TicketID:")) {
                    event.reply("Erro: este canal nao parece ser um ticket valido.").setEphemeral(true).queue();
                    return;
                }

                event.deferReply(true).queue();
                TicketHandler.postControlPanel(panelChannel);
                event.getHook().sendMessage("Painel do ticket recriado em " + panelChannel.getAsMention()).queue();
                break;

            case "list":
                List<String> tickets = archiver.listTickets(null);
                if (tickets.isEmpty()) {
                    event.reply("Nenhum ticket arquivado encontrado.").setEphemeral(true).queue();
                } else {
                    StringBuilder sb = new StringBuilder("**Ultimos Tickets Arquivados:**\n");
                    for (String ticket : tickets) {
                        sb.append(ticket).append("\n");
                    }

                    if (sb.length() > 2000) {
                        event.reply(sb.substring(0, 1990) + "...").setEphemeral(true).queue();
                    } else {
                        event.reply(sb.toString()).setEphemeral(true).queue();
                    }
                }
                break;

            case "view":
                int id = event.getOption("id").getAsInt();
                event.deferReply(true).queue();

                File transcript = archiver.generateTicketTranscript(id);
                if (transcript != null) {
                    event.getHook()
                        .sendFiles(FileUpload.fromData(transcript, "ticket-" + id + ".txt"))
                        .queue(success -> transcript.delete(), error -> transcript.delete());
                } else {
                    event.getHook().sendMessage("Erro: ticket nao encontrado ou falha ao gerar arquivo.").queue();
                }
                break;

            case "reopen":
                int ticketId = event.getOption("id").getAsInt();
                event.deferReply(true).queue();

                TicketArchiver.TicketData data = archiver.getTicket(ticketId);
                if (data == null) {
                    event.getHook().sendMessage("Erro: ticket nao encontrado no banco de dados.").queue();
                    return;
                }

                Guild guild = event.getGuild();
                if (guild == null) {
                    event.getHook().sendMessage("Erro: guild nao encontrada para reabrir o ticket.").queue();
                    return;
                }

                String categoryName = resolveTicketCategoryName(data);
                String categoryId = resolveTicketCategoryId(categoryName);
                Category category = categoryId != null ? guild.getCategoryById(categoryId) : null;

                if (category == null) {
                    event.getHook().sendMessage("Erro: categoria de ticket nao encontrada. Nao e possivel reabrir.").queue();
                    return;
                }

                var action = category.createTextChannel(data.channelName)
                    .clearPermissionOverrides();

                // Base: @everyone sem acesso.
                action.addPermissionOverride(guild.getPublicRole(), null, java.util.EnumSet.of(Permission.VIEW_CHANNEL));

                if (data.userId != null) {
                    try {
                        var ownerMember = guild.getMemberById(data.userId);
                        if (ownerMember != null) {
                            action.addPermissionOverride(ownerMember, java.util.EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Erro ao adicionar permissao do dono ao reabrir ticket", e);
                    }
                }

                java.util.Set<String> rolesToAdd = new java.util.HashSet<>();
                String categoryRoleId = resolveTicketRoleId(categoryName);
                if (categoryRoleId != null && !categoryRoleId.isEmpty()) {
                    for (String roleId : categoryRoleId.split(",")) {
                        rolesToAdd.add(roleId.trim());
                    }
                }

                for (String roleId : rolesToAdd) {
                    try {
                        var role = guild.getRoleById(roleId);
                        if (role != null) {
                            action.addPermissionOverride(role, java.util.EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Erro ao adicionar permissao de cargo ao reabrir ticket", e);
                    }
                }

                action.queue(newChannel -> {
                    if (data.userId != null) {
                        guild.retrieveMemberById(data.userId).queue(member ->
                            newChannel.upsertPermissionOverride(member)
                                .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
                                .queue(), e -> {
                        });
                    }

                    if (data.claimedBy != null && !data.claimedBy.isBlank()) {
                        guild.retrieveMemberById(data.claimedBy).queue(member ->
                            newChannel.upsertPermissionOverride(member)
                                .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
                                .queue(),
                            e -> LOGGER.debug("Nao foi possivel restaurar claimer do ticket {}", data.id, e)
                        );
                    }

                    if (data.collaboratorIds != null) {
                        for (String collaboratorId : data.collaboratorIds) {
                            if (collaboratorId == null || collaboratorId.isBlank()) {
                                continue;
                            }

                            guild.retrieveMemberById(collaboratorId).queue(member ->
                                newChannel.upsertPermissionOverride(member)
                                    .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_HISTORY)
                                    .queue(),
                                e -> LOGGER.debug("Nao foi possivel restaurar colaborador {} do ticket {}", collaboratorId, data.id, e)
                            );
                        }
                    }

                    newChannel.sendMessage("**Ticket Reaberto**\nEste ticket foi restaurado do arquivo por " + event.getUser().getAsMention()).queue();

                    StringBuilder history = new StringBuilder("**Historico Restaurado:**\n\n");
                    if (data.messages != null) {
                        for (TicketArchiver.TicketMessage msg : data.messages) {
                            String line = "**" + msg.author + "**: " + msg.content + "\n";
                            if (history.length() + line.length() > 1900) {
                                newChannel.sendMessage(history.toString()).queue();
                                history = new StringBuilder();
                            }
                            history.append(line);
                        }
                    }
                    if (history.length() > 0) {
                        newChannel.sendMessage(history.toString()).queue();
                    }

                    StringBuilder reopenedTopicBuilder = new StringBuilder("TicketID:")
                        .append(data.id)
                        .append(" | OwnerID:")
                        .append(data.userId)
                        .append(" | Category:")
                        .append(categoryName)
                        .append(" | Status:Open");

                    if (data.claimedBy != null && !data.claimedBy.isBlank()) {
                        reopenedTopicBuilder.append(" | MainStaff:").append(data.claimedBy);
                    }
                    if (data.collaboratorIds != null) {
                        for (String collaboratorId : data.collaboratorIds) {
                            if (collaboratorId != null && !collaboratorId.isBlank()) {
                                reopenedTopicBuilder.append(" | Collab:").append(collaboratorId);
                            }
                        }
                    }

                    String reopenedTopic = reopenedTopicBuilder.toString();
                    newChannel.getManager().setTopic(reopenedTopic).queue();
                    DatabaseManager.updateTicketChannel(data.id, newChannel.getName());
                    DatabaseManager.updateTicketCategory(data.id, categoryName);
                    TicketHandler.postControlPanel(newChannel, reopenedTopic);

                    event.getHook().sendMessage("Ticket reaberto: " + newChannel.getAsMention()).queue();
                });
                break;

            case "clean":
                int days = event.getOption("days").getAsInt();
                event.deferReply(true).queue();
                int count = archiver.cleanTicketsManually(days);
                event.getHook().sendMessage("Limpeza concluida. " + count + " tickets removidos.").queue();
                break;

            case "delete":
                int deleteId = event.getOption("id").getAsInt();
                if (archiver.deleteTicket(deleteId)) {
                    event.reply("Ticket #" + deleteId + " deletado do banco de dados.").queue();
                } else {
                    event.reply("Erro: falha ao deletar ticket, nao encontrado ou com erro.").setEphemeral(true).queue();
                }
                break;

            default:
                event.reply("Subcomando de ticket nao suportado.").setEphemeral(true).queue();
                break;
        }
    }

    private String resolveTicketCategoryName(TicketArchiver.TicketData data) {
        if (data.categoryName != null && !data.categoryName.isBlank()) {
            return data.categoryName;
        }

        String source = normalizeCategory(data.channelName);
        if (source.contains("suporte")) {
            return "Suporte";
        }
        if (source.contains("denuncia") || source.contains("report")) {
            return "Denuncia";
        }
        if (source.contains("bug")) {
            return "Bug";
        }
        if (source.contains("lore")) {
            return "Lore";
        }
        return null;
    }

    private String resolveTicketCategoryId(String categoryName) {
        String normalized = normalizeCategory(categoryName);
        if (normalized.contains("suporte")) {
            return BotConfig.getTicketCategorySupport();
        }
        if (normalized.contains("denuncia") || normalized.contains("report")) {
            return BotConfig.getTicketCategoryReport();
        }
        if (normalized.contains("bug")) {
            return BotConfig.getTicketCategoryBug();
        }
        if (normalized.contains("lore")) {
            return BotConfig.getTicketCategoryLore();
        }
        return null;
    }

    private String resolveTicketRoleId(String categoryName) {
        String normalized = normalizeCategory(categoryName);
        if (normalized.contains("suporte")) {
            return BotConfig.getTicketRoleSupport();
        }
        if (normalized.contains("denuncia") || normalized.contains("report")) {
            return BotConfig.getTicketRoleReport();
        }
        if (normalized.contains("bug")) {
            return BotConfig.getTicketRoleBug();
        }
        if (normalized.contains("lore")) {
            return BotConfig.getTicketRoleLore();
        }
        return null;
    }

    private String normalizeCategory(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    }
}
