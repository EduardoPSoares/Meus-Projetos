package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

import com.midgardbot.data.PunishmentManager;
import com.midgardbot.features.link.LinkManager;
import com.midgardbot.features.sync.ActionSyncManager;
import java.util.UUID;

public class UnbanCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "unban";
    }

    @Override
    public String getDescription() {
        return "Desbane um usuário do servidor (Discord e Minecraft).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.STRING, "userid", "O ID do usuário a ser desbanido", true),
            new OptionData(OptionType.STRING, "motivo", "O motivo do desbanimento", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_UNBAN";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        boolean isConfigured = !BotConfig.getAuthorizedRoles("PERM_CMD_UNBAN").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.replyEmbeds(
                EmbedUtils.createError("Permissão Negada", "Você não tem permissão para desbanir membros.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        String userId = event.getOption("userid").getAsString();
        OptionMapping reasonOption = event.getOption("motivo");
        String reason = reasonOption != null ? reasonOption.getAsString() : "Sem motivo especificado";

        event.getGuild().retrieveBanList().queue(bans -> {
            boolean isBanned = bans.stream().anyMatch(ban -> ban.getUser().getId().equals(userId));
            
            if (!isBanned) {
                // Check if banned in Minecraft only (via PunishmentManager)
                if (PunishmentManager.getBan(userId) == null) {
                    event.replyEmbeds(
                        EmbedUtils.createError("Erro", "Este usuário não está banido.", event.getJDA().getSelfUser()).build()
                    ).setEphemeral(true).queue();
                    return;
                }
            }

            // Unban from Discord
            event.getGuild().unban(User.fromId(userId)).reason(reason).queue(
                success -> {
                    // Remove from PunishmentManager
                    PunishmentManager.removeBan(userId);

                    // Sync with Minecraft
                    UUID uuid = LinkManager.getUUID(userId);
                    if (uuid != null) {
                        ActionSyncManager.queueAction(uuid.toString(), "UNBAN", reason, event.getUser().getName());
                    }

                    event.getJDA().retrieveUserById(userId).queue(user -> {
                        event.replyEmbeds(
                            EmbedUtils.createSuccess("Usuário Desbanido", user.getAsMention() + " foi desbanido do Discord e Minecraft.\nMotivo: " + reason, event.getJDA().getSelfUser()).build()
                        ).queue();
                        logPunishment(event, "Unban", user, reason);
                    });
                },
                error -> {
                    // If failed to unban from Discord (maybe not banned there), try to unban from Minecraft anyway
                    PunishmentManager.removeBan(userId);
                    UUID uuid = LinkManager.getUUID(userId);
                    if (uuid != null) {
                        ActionSyncManager.queueAction(uuid.toString(), "UNBAN", reason, event.getUser().getName());
                    }
                    
                    event.replyEmbeds(
                        EmbedUtils.createSuccess("Usuário Desbanido (Minecraft)", "O usuário foi desbanido do sistema Minecraft (não estava banido no Discord ou erro ao desbanir).\nMotivo: " + reason, event.getJDA().getSelfUser()).build()
                    ).queue();
                }
            );
        });
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
                        .addField("Motivo", reason, false)
                        .build()
                ).queue();
            }
        }
    }
}
