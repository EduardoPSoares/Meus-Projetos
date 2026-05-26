package com.midgardbot.features;

import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.stream.Collectors;

public class ServerLogListener extends ListenerAdapter {

    private TextChannel getLogChannel(@NotNull net.dv8tion.jda.api.entities.Guild guild) {
        String logChannelId = BotConfig.get("LOG_CHANNEL_ID");
        if (logChannelId == null || logChannelId.isEmpty()) return null;
        try {
            return guild.getTextChannelById(logChannelId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void log(@NotNull net.dv8tion.jda.api.entities.Guild guild, String title, String description, java.awt.Color color) {
        TextChannel channel = getLogChannel(guild);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(color)
            .setTimestamp(Instant.now())
            .setFooter("Log do Servidor", guild.getSelfMember().getUser().getAvatarUrl());

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    // --- Channel Events ---

    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        log(event.getGuild(), "📁 Canal Criado",
            String.format("**Nome:** %s\n**Tipo:** %s\n**ID:** %s", 
                event.getChannel().getName(), 
                event.getChannelType().name(), 
                event.getChannel().getId()), 
            EmbedUtils.COLOR_SUCCESS);
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        log(event.getGuild(), "🗑️ Canal Deletado",
            String.format("**Nome:** %s\n**Tipo:** %s\n**ID:** %s", 
                event.getChannel().getName(), 
                event.getChannelType().name(), 
                event.getChannel().getId()), 
            EmbedUtils.COLOR_ERROR);
    }

    @Override
    public void onChannelUpdateName(@NotNull ChannelUpdateNameEvent event) {
        log(event.getGuild(), "📝 Canal Renomeado",
            String.format("**Anterior:** %s\n**Novo:** %s\n**Canal:** %s", 
                event.getOldValue(), 
                event.getNewValue(), 
                event.getChannel().getAsMention()), 
            EmbedUtils.COLOR_WARNING);
    }

    // --- Role Events ---

    @Override
    public void onRoleCreate(@NotNull RoleCreateEvent event) {
        log(event.getGuild(), "🛡️ Cargo Criado",
            String.format("**Nome:** %s\n**ID:** %s", 
                event.getRole().getName(), 
                event.getRole().getId()), 
            EmbedUtils.COLOR_SUCCESS);
    }

    @Override
    public void onRoleDelete(@NotNull RoleDeleteEvent event) {
        log(event.getGuild(), "🗑️ Cargo Deletado",
            String.format("**Nome:** %s\n**ID:** %s", 
                event.getRole().getName(), 
                event.getRole().getId()), 
            EmbedUtils.COLOR_ERROR);
    }

    @Override
    public void onRoleUpdateName(@NotNull RoleUpdateNameEvent event) {
        log(event.getGuild(), "📝 Cargo Renomeado",
            String.format("**Anterior:** %s\n**Novo:** %s\n**Cargo:** %s", 
                event.getOldValue(), 
                event.getNewValue(), 
                event.getRole().getAsMention()), 
            EmbedUtils.COLOR_WARNING);
    }

    // --- Member Role Events ---

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        String roles = event.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.joining(", "));
        
        log(event.getGuild(), "➕ Cargo(s) Adicionado(s)",
            String.format("**Usuário:** %s\n**Cargo(s):** %s", 
                event.getMember().getAsMention(), 
                roles), 
            EmbedUtils.COLOR_INFO);
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        String roles = event.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.joining(", "));
        
        log(event.getGuild(), "➖ Cargo(s) Removido(s)",
            String.format("**Usuário:** %s\n**Cargo(s):** %s", 
                event.getMember().getAsMention(), 
                roles), 
            EmbedUtils.COLOR_WARNING);
    }

    // --- Guild Events ---

    @Override
    public void onGuildUpdateName(@NotNull GuildUpdateNameEvent event) {
        log(event.getGuild(), "🏠 Nome do Servidor Alterado",
            String.format("**Anterior:** %s\n**Novo:** %s", 
                event.getOldValue(), 
                event.getNewValue()), 
            EmbedUtils.COLOR_PRIMARY);
    }

    @Override
    public void onGuildUpdateIcon(@NotNull GuildUpdateIconEvent event) {
        log(event.getGuild(), "🖼️ Ícone do Servidor Alterado",
            String.format("**Anterior:** [Link](%s)\n**Novo:** [Link](%s)", 
                event.getOldIconUrl() != null ? event.getOldIconUrl() : "Nenhum", 
                event.getNewIconUrl() != null ? event.getNewIconUrl() : "Nenhum"), 
            EmbedUtils.COLOR_PRIMARY);
    }
}
