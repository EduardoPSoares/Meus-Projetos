package com.midgardbot.commands.impl;

import com.midgardbot.commands.CommandContext;
import com.midgardbot.commands.ICommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

import java.awt.Color;

/**
 * Comando de Ping (Texto).
 * Verifica a latência da conexão entre o Bot e a API do Discord.
 */
public class PingCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) {
        JDA jda = ctx.getJDA();

        jda.getRestPing().queue(
                (ping) -> {
                    EmbedBuilder embed = EmbedUtils.createDefault(
                        "📡 Telemetria de Rede",
                        "Diagnóstico de conectividade e latência da API.",
                        jda.getSelfUser()
                    );
                    
                    String status;
                    Color color;
                    if (ping < 100) {
                        status = "🟢 Estável";
                        color = EmbedUtils.COLOR_SUCCESS;
                    } else if (ping < 200) {
                        status = "🟡 Moderado";
                        color = EmbedUtils.COLOR_WARNING;
                    } else {
                        status = "🔴 Instável";
                        color = EmbedUtils.COLOR_ERROR;
                    }
                    
                    embed.setColor(color);
                    embed.addField("☁️ Latência API (REST)", "`" + ping + "ms`", true);
                    embed.addField("⚡ Gateway (WebSocket)", "`" + jda.getGatewayPing() + "ms`", true);
                    embed.addField("📊 Integridade", status, true);
                    
                    ctx.getChannel().sendMessageEmbeds(embed.build()).queue();
                }
        );
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getHelp() {
        return "Exibe estatísticas de latência e saúde da conexão.";
    }
}