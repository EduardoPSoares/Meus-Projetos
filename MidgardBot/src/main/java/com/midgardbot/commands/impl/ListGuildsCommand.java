package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils; // Import necessário
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Comando de Listar Servidores.
 * Gera um arquivo de texto listando todos os servidores onde o bot está presente.
 * Útil para auditoria.
 */
public class ListGuildsCommand implements ISlashCommand {

    @Override
    public String getName() {
        return "listservers";
    }

    @Override
    public String getDescription() {
        return "Gera um relatório de distribuição de instâncias do bot (Admin)";
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_LISTGUILDS";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_LISTGUILDS").isEmpty() && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtils.createError("⛔ Acesso Negado", "Sem permissão.", event.getJDA().getSelfUser()).build())
                .setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        List<Guild> guilds = event.getJDA().getGuilds();
        StringBuilder sb = new StringBuilder();
        
        // Cabeçalho do arquivo de texto
        sb.append("=========================================\n");
        sb.append(" RELATÓRIO DE INSTÂNCIAS - MIDGARD BOT\n");
        sb.append(" Total de Servidores: ").append(guilds.size()).append("\n");
        sb.append("=========================================\n\n");

        for (Guild guild : guilds) {
            sb.append(String.format("[ID: %-20s] | Membros: %-6d | Nome: %s\n", 
                guild.getId(), 
                guild.getMemberCount(), 
                guild.getName()));
        }

        String content = sb.toString();

        if (content.length() > 1900) {
            event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(
                    "📄 Relatório Gerado", 
                    "A lista de servidores excede o limite de visualização.\nO relatório completo foi anexado.", 
                    event.getJDA().getSelfUser()).build())
                .addFiles(FileUpload.fromData(content.getBytes(StandardCharsets.UTF_8), "relatorio_instancias.txt"))
                .queue();
        } else {
            // Se couber, usa um Embed bonito em vez de bloco de código solto
            event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(
                "🌐 Topologia de Rede",
                "Lista de servidores conectados atualmente:\n```ini\n" + content + "\n```",
                event.getJDA().getSelfUser()
            ).build()).queue();
        }
    }
}