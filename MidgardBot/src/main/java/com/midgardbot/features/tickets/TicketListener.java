package com.midgardbot.features.tickets;

import com.midgardbot.commands.handlers.InteractionUtils;
import com.midgardbot.commands.handlers.TicketHandler;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class TicketListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) {
            return;
        }

        if (!(event.getChannel() instanceof TextChannel channel)) {
            return;
        }

        String topic = channel.getTopic();
        if (topic == null || !topic.contains("TicketID:")) {
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            return;
        }

        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        String userId = member.getId();
        String ownerId = TicketHandler.extractTopicField(topic, "OwnerID:");
        String mainStaffId = TicketHandler.extractTopicField(topic, "MainStaff:");
        boolean isOwner = ownerId != null && ownerId.equals(userId);
        boolean isMainStaff = mainStaffId != null && mainStaffId.equals(userId);
        boolean isCollab = topic.contains("Collab:" + userId)
            || TicketHandler.isCachedCollab(channel.getId(), userId);

        // Evita falso negativo de permissao apagar mensagens do dono, staff responsavel,
        // colaboradores ou outros staffs logo apos assumir o ticket.
        if (isOwner || isMainStaff || isCollab || InteractionUtils.isStaff(member)) {
            return;
        }

        if (member.hasPermission(channel, Permission.MESSAGE_SEND)) {
            return;
        }

        event.getMessage().delete().queue(
            success -> {},
            error -> {}
        );
    }
}
