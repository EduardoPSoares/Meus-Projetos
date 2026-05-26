package com.midgardbot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import net.dv8tion.jda.api.entities.SelfUser;

import java.util.List;

/**
 * Contexto de Comando.
 * Encapsula as informações necessárias para a execução de um comando de texto.
 * Facilita o acesso a dados como autor, canal, argumentos e guilda.
 */
public class CommandContext {
    private final MessageReceivedEvent event;
    private final List<String> args;

    public CommandContext(MessageReceivedEvent event, List<String> args) {
        this.event = event;
        this.args = args;
    }

    public Guild getGuild() {
        return this.event.getGuild();
    }

    public MessageReceivedEvent getEvent() {
        return this.event;
    }

    public List<String> getArgs() {
        return this.args;
    }

    public MessageChannel getChannel() {
        return this.event.getChannel();
    }

    public Message getMessage() {
        return this.event.getMessage();
    }

    public User getAuthor() {
        return this.event.getAuthor();
    }

    public Member getMember() {
        return this.event.getMember();
    }

    public JDA getJDA() {
        return this.event.getJDA();
    }

    public SelfUser getSelfUser() {
        return this.event.getJDA().getSelfUser();
    }

    public Member getSelfMember() {
        return this.event.getGuild().getSelfMember();
    }
}