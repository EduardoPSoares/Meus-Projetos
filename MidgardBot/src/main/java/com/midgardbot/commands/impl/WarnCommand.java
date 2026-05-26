package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.config.MessagesConfig;
import com.midgardbot.data.PunishmentManager;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.midgardbot.features.link.LinkManager;
import com.midgardbot.features.sync.ActionSyncManager;
import java.util.UUID;

public class WarnCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "warn";
    }

    @Override
    public String getDescription() {
        return "Adverte um usuário (Discord e Minecraft).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário a ser advertido", true),
            new OptionData(OptionType.STRING, "severidade", "Nível da advertência", true)
                .addChoice("Leve", "low")
                .addChoice("Média", "medium")
                .addChoice("Pesada", "high"),
            new OptionData(OptionType.STRING, "motivo", "O motivo da advertência", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_WARN";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean isConfigured = !BotConfig.getAuthorizedRoles("PERM_CMD_WARN").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.replyEmbeds(
                EmbedUtils.createError("Permissão Negada", "Você não tem permissão para advertir membros.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        User target = event.getOption("usuario").getAsUser();
        String severity = event.getOption("severidade").getAsString();
        String reason = event.getOption("motivo").getAsString();

        PunishmentManager.PunishmentType type;
        switch (severity) {
            case "low": type = PunishmentManager.PunishmentType.WARN_LOW; break;
            case "medium": type = PunishmentManager.PunishmentType.WARN_MEDIUM; break;
            case "high": type = PunishmentManager.PunishmentType.WARN_HIGH; break;
            default: type = PunishmentManager.PunishmentType.WARN; break;
        }

        PunishmentManager.Punishment p = PunishmentManager.createPunishment(target.getId(), target.getName(), target.getId(), type, reason, event.getUser().getId(), event.getUser().getName(), -1);

        if (p == null) {
            event.replyEmbeds(EmbedUtils.createError("Erro", "Falha ao criar punição no banco de dados.", event.getJDA().getSelfUser()).build()).setEphemeral(true).queue();
            return;
        }

        // Sync with Minecraft
        UUID uuid = LinkManager.getUUID(target.getId());
        if (uuid != null) {
            ActionSyncManager.queueAction(uuid.toString(), "WARN", "[" + severity.toUpperCase() + "] " + reason, event.getUser().getName());
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("user_mention", target.getAsMention());
        placeholders.put("user_id", target.getId());
        placeholders.put("reason", reason);
        placeholders.put("severity", severity.toUpperCase());
        placeholders.put("staff_mention", event.getUser().getAsMention());
        placeholders.put("warn_id", String.valueOf(p.id));
        placeholders.put("warn_count", String.valueOf(PunishmentManager.getWarns(target.getId()).size()));
        placeholders.put("guild_icon", event.getGuild().getIconUrl() != null ? event.getGuild().getIconUrl() : event.getJDA().getSelfUser().getAvatarUrl());
        placeholders.put("guild_name", event.getGuild().getName());

        event.replyEmbeds(
            MessagesConfig.buildEmbed(MessagesConfig.get().moderation.warn, placeholders).build()
        ).queue();

        // Try to DM the user
        target.openPrivateChannel().queue(pc -> {
            pc.sendMessageEmbeds(
                MessagesConfig.buildEmbed(MessagesConfig.get().moderation.dm_warn, placeholders).build()
            ).queue(null, error -> {});
        });

        logPunishment(event, "Warn (" + severity.toUpperCase() + ")", target, reason, String.valueOf(p.id));
    }

    private void logPunishment(SlashCommandInteractionEvent event, String type, User target, String reason, String warnId) {
        String channelId = BotConfig.getPunishmentChannelId();
        if (channelId != null) {
            TextChannel channel = event.getGuild().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(
                    EmbedUtils.createEmbed("🔨 Punição: " + type, "", EmbedUtils.COLOR_WARNING)
                        .addField("Usuário", target.getAsMention() + " (" + target.getId() + ")", false)
                        .addField("Moderador", event.getUser().getAsMention(), false)
                        .addField("ID do Warn", warnId, false)
                        .addField("Motivo", reason, false)
                        .addField("Total de Warns", String.valueOf(PunishmentManager.getWarns(target.getId()).size()), false)
                        .build()
                ).queue();
            }
        }
    }
}
