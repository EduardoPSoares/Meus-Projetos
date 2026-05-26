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

public class UnwarnCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "unwarn";
    }

    @Override
    public String getDescription() {
        return "Remove uma advertência de um usuário.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário", true),
            new OptionData(OptionType.STRING, "warnid", "O ID da advertência", true)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_UNWARN";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean isConfigured = !BotConfig.getAuthorizedRoles("PERM_CMD_UNWARN").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.replyEmbeds(
                EmbedUtils.createError("Permissão Negada", "Você não tem permissão para remover advertências.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        User target = event.getOption("usuario").getAsUser();
        String warnId = event.getOption("warnid").getAsString();

        boolean removed = PunishmentManager.removeWarn(target.getId(), warnId);

        if (removed) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("user_mention", target.getAsMention());
            placeholders.put("user_id", target.getId());
            placeholders.put("warn_id", warnId);
            placeholders.put("staff_mention", event.getUser().getAsMention());
            placeholders.put("guild_icon", event.getGuild().getIconUrl() != null ? event.getGuild().getIconUrl() : event.getJDA().getSelfUser().getAvatarUrl());

            event.replyEmbeds(
                MessagesConfig.buildEmbed(MessagesConfig.get().moderation.unwarn, placeholders).build()
            ).queue();
            logPunishment(event, "Unwarn", target, "Warn ID: " + warnId);
        } else {
            event.replyEmbeds(
                EmbedUtils.createError("Erro", "Advertência não encontrada.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
        }
    }

    private void logPunishment(SlashCommandInteractionEvent event, String type, User target, String reason) {
        String channelId = BotConfig.getPunishmentChannelId();
        if (channelId != null) {
            TextChannel channel = event.getGuild().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(
                    EmbedUtils.createEmbed("🔨 Punição: " + type, "", EmbedUtils.COLOR_SUCCESS)
                        .addField("Usuário", target.getAsMention() + " (" + target.getId() + ")", false)
                        .addField("Moderador", event.getUser().getAsMention(), false)
                        .addField("Detalhes", reason, false)
                        .build()
                ).queue();
            }
        }
    }
}
