package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.features.streamer.Streamer;
import com.midgardbot.features.streamer.StreamerManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;

public class StreamerCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "streamer";
    }

    @Override
    public String getDescription() {
        return "Gerencia a lista de streamers autorizados.";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_STREAMER";
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        List<SubcommandData> subcommands = new ArrayList<>();
        
        subcommands.add(new SubcommandData("add", "Adiciona um streamer autorizado")
            .addOption(OptionType.STRING, "platform", "Plataforma (twitch, youtube, kick)", true)
            .addOption(OptionType.STRING, "channel", "Nome do canal ou ID", true)
            .addOption(OptionType.USER, "user", "Usuário do Discord dono do canal", true));

        subcommands.add(new SubcommandData("remove", "Remove um streamer autorizado")
            .addOption(OptionType.USER, "user", "Usuário do Discord para remover", true));

        subcommands.add(new SubcommandData("list", "Lista os streamers autorizados"));

        return subcommands;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        if (subcommand == null) return;

        switch (subcommand) {
            case "add":
                String platform = event.getOption("platform").getAsString().toLowerCase();
                String channel = event.getOption("channel").getAsString();
                String userId = event.getOption("user").getAsUser().getId();

                if (!platform.equals("twitch") && !platform.equals("youtube") && !platform.equals("kick")) {
                    event.reply("Plataforma inválida. Use: twitch, youtube, kick.").setEphemeral(true).queue();
                    return;
                }

                StreamerManager.addStreamer(userId, platform, channel);
                event.reply("Streamer adicionado com sucesso!").setEphemeral(true).queue();
                break;

            case "remove":
                String removeUserId = event.getOption("user").getAsUser().getId();
                StreamerManager.removeStreamer(removeUserId);
                event.reply("Streamer removido com sucesso!").setEphemeral(true).queue();
                break;

            case "list":
                List<Streamer> streamers = StreamerManager.getStreamers();
                if (streamers.isEmpty()) {
                    event.reply("Nenhum streamer cadastrado.").setEphemeral(true).queue();
                    return;
                }

                StringBuilder sb = new StringBuilder("**Streamers Autorizados:**\n");
                for (Streamer s : streamers) {
                    sb.append(String.format("- <@%s> (%s): %s\n", s.getUserId(), s.getPlatform(), s.getChannelName()));
                }
                event.reply(sb.toString()).setEphemeral(true).queue();
                break;
        }
    }
}
