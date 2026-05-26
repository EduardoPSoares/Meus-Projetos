package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.integrations.GitHubIntegration;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "reportar";
    }

    @Override
    public String getDescription() {
        return "Reporta um bug ou envia uma sugestão diretamente para o GitHub.";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "tipo", "O tipo de report (Bug ou Sugestão)", true)
                .addChoice("Bug", "bug")
                .addChoice("Sugestão", "enhancement"));
        options.add(new OptionData(OptionType.STRING, "titulo", "Resumo do problema ou ideia", true));
        options.add(new OptionData(OptionType.STRING, "descricao", "Detalhes completos do report", true));
        return options;
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_REPORT";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String type = event.getOption("tipo").getAsString();
        String title = event.getOption("titulo").getAsString();
        String description = event.getOption("descricao").getAsString();
        String user = event.getUser().getName(); // Updated for new username system

        String body = description + "\n\n**Reportado por:** " + user + " via Discord.";
        List<String> labels = Collections.singletonList(type);

        String issueUrl = GitHubIntegration.createIssue(title, body, labels);

        if (issueUrl != null) {
            event.getHook().sendMessage("✅ **Report enviado com sucesso!**\nAcompanhe aqui: " + issueUrl).queue();
        } else {
            event.getHook().sendMessage("❌ **Erro ao enviar report.** Verifique se o token do GitHub está configurado.").queue();
        }
    }
}
