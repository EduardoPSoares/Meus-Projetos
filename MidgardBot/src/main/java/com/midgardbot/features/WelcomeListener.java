package com.midgardbot.features;

import com.midgardbot.config.BotConfig;
import com.midgardbot.config.MessagesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener de Boas-vindas.
 * Envia uma mensagem embed personalizada quando um novo membro entra no servidor do Discord.
 */
public class WelcomeListener extends ListenerAdapter {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(WelcomeListener.class);

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        try {
            String welcomeChannelId = BotConfig.getWelcomeChannelId();
            if (welcomeChannelId == null || welcomeChannelId.isEmpty()) return;

            TextChannel channel = event.getGuild().getTextChannelById(welcomeChannelId);
            if (channel != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("user", event.getUser().getAsMention());
                placeholders.put("user_name", event.getUser().getName());
                placeholders.put("user_avatar", event.getUser().getEffectiveAvatarUrl());
                placeholders.put("guild_name", event.getGuild().getName());
                placeholders.put("member_count", String.valueOf(event.getGuild().getMemberCount()));

                EmbedBuilder embed = MessagesConfig.buildEmbed(MessagesConfig.get().welcome.join, placeholders);
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(WelcomeListener.class).error("Erro ao enviar mensagem de boas-vindas", e);
        }
    }
}
