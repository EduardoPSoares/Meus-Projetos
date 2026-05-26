package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.config.MessagesConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.midgardbot.data.PunishmentManager;
import com.midgardbot.features.link.LinkManager;
import com.midgardbot.features.sync.ActionSyncManager;
import java.util.UUID;

public class BanCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public String getDescription() {
        return "Bane um usuário do servidor (Discord e Minecraft).";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
            new OptionData(OptionType.USER, "usuario", "O usuário a ser banido", true),
            new OptionData(OptionType.STRING, "motivo", "O motivo do banimento", false)
        );
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_BAN";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Se a permissão não estiver configurada no .env, usa a permissão nativa do Discord como fallback
        boolean isConfigured = !BotConfig.getAuthorizedRoles("PERM_CMD_BAN").isEmpty();
        if (!isConfigured && !event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.replyEmbeds(
                EmbedUtils.createError("Permissão Negada", "Você não tem permissão para banir membros.", event.getJDA().getSelfUser()).build()
            ).setEphemeral(true).queue();
            return;
        }

        Member target = event.getOption("usuario").getAsMember();
        User targetUser = event.getOption("usuario").getAsUser();

        if (target != null) {
            if (!event.getMember().canInteract(target) || !event.getGuild().getSelfMember().canInteract(target)) {
                event.replyEmbeds(
                    EmbedUtils.createError("Erro", "Não posso banir este usuário (cargo superior ou igual).", event.getJDA().getSelfUser()).build()
                ).setEphemeral(true).queue();
                return;
            }
        }

        OptionMapping reasonOption = event.getOption("motivo");
        String reason = reasonOption != null ? reasonOption.getAsString() : "Sem motivo especificado";

        event.getGuild().ban(targetUser, 0, TimeUnit.DAYS).reason(reason).queue(
            success -> {
                // Add to PunishmentManager (JSON)
                PunishmentManager.addBan(targetUser.getId(), event.getUser().getId(), reason);

                // Sync with Minecraft
                UUID uuid = LinkManager.getUUID(targetUser.getId());
                if (uuid != null) {
                    ActionSyncManager.queueAction(uuid.toString(), "BAN", reason, event.getUser().getName());
                }

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("user_mention", targetUser.getAsMention());
                placeholders.put("user_id", targetUser.getId());
                placeholders.put("reason", reason);
                placeholders.put("staff_mention", event.getUser().getAsMention());
                placeholders.put("guild_icon", event.getGuild().getIconUrl() != null ? event.getGuild().getIconUrl() : event.getJDA().getSelfUser().getAvatarUrl());

                event.replyEmbeds(
                    MessagesConfig.buildEmbed(MessagesConfig.get().moderation.ban, placeholders).build()
                ).queue();

                logPunishment(event, "Ban", targetUser, reason);
            },
            error -> {
                event.replyEmbeds(
                    EmbedUtils.createError("Erro", "Falha ao banir usuário: " + error.getMessage(), event.getJDA().getSelfUser()).build()
                ).setEphemeral(true).queue();
            }
        );
    }

    private void logPunishment(SlashCommandInteractionEvent event, String type, User target, String reason) {
        String channelId = BotConfig.getPunishmentChannelId();
        if (channelId != null) {
            TextChannel channel = event.getGuild().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(
                    EmbedUtils.createEmbed("🔨 Punição: " + type, "", EmbedUtils.COLOR_ERROR)
                        .addField("Usuário", target.getAsMention() + " (" + target.getId() + ")", false)
                        .addField("Moderador", event.getUser().getAsMention(), false)
                        .addField("Motivo", reason, false)
                        .build()
                ).queue();
            }
        }
    }
}
