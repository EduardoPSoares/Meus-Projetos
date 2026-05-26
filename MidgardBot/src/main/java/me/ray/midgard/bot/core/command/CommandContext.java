package me.ray.midgard.bot.core.command;

import me.ray.midgard.bot.MidgardBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;

import java.util.Collection;

public class CommandContext {

    private final SlashCommandInteractionEvent event;
    private final MidgardBot bot;

    public CommandContext(SlashCommandInteractionEvent event, MidgardBot bot) {
        this.event = event;
        this.bot = bot;
    }

    // ==================== Accessors ====================

    public SlashCommandInteractionEvent getEvent() { return event; }
    public MidgardBot getBot() { return bot; }
    public JDA getJDA() { return event.getJDA(); }
    public User getUser() { return event.getUser(); }
    public Member getMember() { return event.getMember(); }
    public Guild getGuild() { return event.getGuild(); }
    public MessageChannel getChannel() { return event.getChannel(); }
    public String getCommandName() { return event.getName(); }
    public String getSubCommandName() { return event.getSubcommandName(); }
    public String getSubCommandGroup() { return event.getSubcommandGroup(); }

    // ==================== Options ====================

    public OptionMapping getOption(String name) {
        return event.getOption(name);
    }

    public String getString(String name) {
        return getStringOr(name, null);
    }

    public String getStringOr(String name, String defaultValue) {
        OptionMapping opt = event.getOption(name);
        return opt != null ? opt.getAsString() : defaultValue;
    }

    public long getLong(String name) {
        return getLongOr(name, 0);
    }

    public long getLongOr(String name, long defaultValue) {
        OptionMapping opt = event.getOption(name);
        return opt != null ? opt.getAsLong() : defaultValue;
    }

    public int getInt(String name) {
        return (int) getLong(name);
    }

    public int getIntOr(String name, int defaultValue) {
        return (int) getLongOr(name, defaultValue);
    }

    public double getDouble(String name) {
        return getDoubleOr(name, 0.0);
    }

    public double getDoubleOr(String name, double defaultValue) {
        OptionMapping opt = event.getOption(name);
        return opt != null ? opt.getAsDouble() : defaultValue;
    }

    public boolean getBoolean(String name) {
        return getBooleanOr(name, false);
    }

    public boolean getBooleanOr(String name, boolean defaultValue) {
        OptionMapping opt = event.getOption(name);
        return opt != null ? opt.getAsBoolean() : defaultValue;
    }

    public User getUser(String name) {
        OptionMapping opt = event.getOption(name);
        return opt != null ? opt.getAsUser() : null;
    }

    public Member getMember(String name) {
        OptionMapping opt = event.getOption(name);
        return opt != null ? opt.getAsMember() : null;
    }

    public MessageChannel getChannel(String name) {
        OptionMapping opt = event.getOption(name);
        return opt != null ? opt.getAsChannel().asGuildMessageChannel() : null;
    }

    public boolean hasOption(String name) {
        return event.getOption(name) != null;
    }

    // ==================== Replies ====================

    public void reply(String content) {
        event.reply(content).queue();
    }

    public void reply(MessageEmbed embed) {
        event.replyEmbeds(embed).queue();
    }

    public void reply(String content, boolean ephemeral) {
        event.reply(content).setEphemeral(ephemeral).queue();
    }

    public void reply(MessageEmbed embed, boolean ephemeral) {
        event.replyEmbeds(embed).setEphemeral(ephemeral).queue();
    }

    public void replyEphemeral(String content) {
        reply(content, true);
    }

    public void replyEphemeral(MessageEmbed embed) {
        reply(embed, true);
    }

    public void replyWithComponents(MessageEmbed embed, ActionRow... rows) {
        event.replyEmbeds(embed).addComponents(rows).queue();
    }

    public void replyWithComponents(String content, ActionRow... rows) {
        event.reply(content).addComponents(rows).queue();
    }

    public void deferReply() {
        event.deferReply().queue();
    }

    public void deferReply(boolean ephemeral) {
        event.deferReply(ephemeral).queue();
    }

    public void editReply(String content) {
        event.getHook().editOriginal(content).queue();
    }

    public void editReply(MessageEmbed embed) {
        event.getHook().editOriginalEmbeds(embed).queue();
    }

    public void replySuccess(String message) {
        replyEphemeral("✅ " + message);
    }

    public void replyError(String message) {
        replyEphemeral("❌ " + message);
    }

    public void replyWarning(String message) {
        replyEphemeral("⚠️ " + message);
    }

    // ==================== Checks ====================

    public boolean isOwner() {
        return event.getUser().getId().equals(bot.getConfig().getOwnerId());
    }

    public boolean isGuild() {
        return event.isFromGuild();
    }
}
