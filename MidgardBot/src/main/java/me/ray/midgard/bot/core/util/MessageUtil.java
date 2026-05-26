package me.ray.midgard.bot.core.util;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.concurrent.TimeUnit;

public final class MessageUtil {

    private MessageUtil() {}

    public static void replyEphemeral(IReplyCallback callback, String content) {
        callback.reply(content).setEphemeral(true).queue();
    }

    public static void replyEphemeral(IReplyCallback callback, MessageEmbed embed) {
        callback.replyEmbeds(embed).setEphemeral(true).queue();
    }

    public static void replySuccess(IReplyCallback callback, String content) {
        callback.reply("✅ " + content).setEphemeral(true).queue();
    }

    public static void replyError(IReplyCallback callback, String content) {
        callback.reply("❌ " + content).setEphemeral(true).queue();
    }

    public static void replyWarning(IReplyCallback callback, String content) {
        callback.reply("⚠️ " + content).setEphemeral(true).queue();
    }

    public static void sendTemporary(MessageChannel channel, String content, int seconds) {
        channel.sendMessage(content).queue(msg ->
                msg.delete().queueAfter(seconds, TimeUnit.SECONDS, null, t -> {})
        );
    }

    public static void sendTemporary(MessageChannel channel, MessageEmbed embed, int seconds) {
        channel.sendMessageEmbeds(embed).queue(msg ->
                msg.delete().queueAfter(seconds, TimeUnit.SECONDS, null, t -> {})
        );
    }

    public static void deleteAfter(Message message, int seconds) {
        message.delete().queueAfter(seconds, TimeUnit.SECONDS, null, t -> {});
    }
}
