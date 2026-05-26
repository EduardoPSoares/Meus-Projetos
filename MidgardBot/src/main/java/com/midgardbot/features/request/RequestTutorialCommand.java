package com.midgardbot.features.request;

import com.midgardbot.commands.ISlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.Color;

public class RequestTutorialCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "tutorial-requisicao";
    }

    @Override
    public String getDescription() {
        return "Explica como funciona o sistema de requisições";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📚 Tutorial: Sistema de Requisições (Interno)");
        embed.setColor(Color.CYAN);
        embed.setDescription("Bem-vindo ao sistema de requisições do Midgard! Esta ferramenta é exclusiva para a equipe.");

        embed.addField("1️⃣ Criar uma Requisição", 
            "Use o comando `/requisicao` para registrar uma tarefa, bug ou melhoria.\n" +
            "Você precisará fornecer:\n" +
            "• **Conteúdo**: Detalhes completos da sua solicitação.\n" +
            "• **Prioridade**: Baixa, Média, Alta ou Urgente.\n" +
            "• **Tipo**: Bug, Feature, Configuração ou Outro.\n" +
            "• **Imagem**: (Opcional) Print ou anexo para auxiliar.", false);

        embed.addField("2️⃣ Acompanhamento e Discussão",
            "Após enviar, os Desenvolvedores analisarão sua requisição.\n" +
            "Qualquer dúvida ou atualização será comunicada diretamente a você.", false);

        embed.addField("🔄 Fluxo de Status",
            "• **Pendente**: Aguardando análise.\n" +
            "• **Aprovado**: Os Desenvolvedores aceitaram e colocaram na fila.\n" +
            "• **Em Andamento**: Um desenvolvedor está trabalhando nisso.\n" +
            "• **Concluído**: A requisição foi finalizada e entregue!\n" +
            "• **Negado**: A requisição não pode ser atendida no momento.\n\n" +
            "🔔 **Notificações**: Você receberá atualizações sobre o status da sua requisição diretamente no seu privado (DM).", false);

        embed.addField("❓ Dúvidas?",
            "Se tiver dúvidas sobre uma requisição específica, entre em contato com os Desenvolvedores.", false);

        embed.setFooter("Midgard Bot - Sistema de Requisições", event.getJDA().getSelfUser().getAvatarUrl());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
