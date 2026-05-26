package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Comando de Status.
 * Permite que o usuário verifique em que etapa está sua aplicação de whitelist (Pendente, Aprovado, Reprovado).
 */
public class StatusCommand implements ISlashCommand {
    
    @Override
    public String getName() {
        return "status";
    }

    @Override
    public String getDescription() {
        return "Consulta o andamento do processo de admissão (Whitelist).";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        WhitelistStatusInfo info = DataManager.getStatus(userId);

        if (info == null) {
            event.replyEmbeds(EmbedUtils.createInfo(
                "📂 Registro Não Localizado",
                "Não encontramos nenhum processo de admissão vinculado à sua conta.\n\n" +
                "💡 **Ação Recomendada:**\n" +
                "Dirija-se ao canal oficial de registros para iniciar seu formulário.",
                event.getJDA().getSelfUser()
            ).build()).setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed;
        switch (info.status) {
            case APPROVED:
                embed = EmbedUtils.createSuccess(
                    "✅ Processo Deferido (Aprovado)",
                    "Sua solicitação de cidadania foi aceita. Bem-vindo a Midgard!\n\n" +
                    "📅 **Data de Aprovação:** " + info.timestamp + "\n" +
                    "📡 **Conexão Java:** `jogar.midgard.com`\n" +
                    "📱 **Conexão Bedrock:** `bedrock.midgard.com`",
                    event.getJDA().getSelfUser()
                );
                break;
            case REJECTED:
                embed = EmbedUtils.createError(
                    "❌ Processo Indeferido (Reprovado)",
                    "Sua solicitação não atendeu aos critérios vigentes.\n\n" +
                    "📅 **Data da Análise:** " + info.timestamp + "\n" +
                    "📝 **Parecer Técnico:**\n" + 
                    "> " + (info.reason != null ? info.reason : "Inconsistência nas respostas apresentadas.") + "\n\n" +
                    "💡 Você pode submeter um novo formulário após o período de carência, seguindo as orientações acima.",
                    event.getJDA().getSelfUser()
                );
                break;
            case PENDING:
            default:
                embed = EmbedUtils.createWarning(
                    "🕒 Em Análise",
                    "Seu formulário encontra-se na fila de avaliação técnica.\n\n" +
                    "📅 **Submetido em:** " + info.timestamp + "\n\n" +
                    "ℹ️ Nossa equipe está revisando seus dados. Você receberá uma notificação automática assim que o veredito for lançado.",
                    event.getJDA().getSelfUser()
                );
                break;
        }

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}