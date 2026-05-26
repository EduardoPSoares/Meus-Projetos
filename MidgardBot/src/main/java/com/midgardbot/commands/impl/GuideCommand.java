package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

/**
 * Comando de Guia.
 * Exibe informações úteis para novos jogadores, como links da loja e wiki.
 */
public class GuideCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "guia";
    }

    @Override
    public String getDescription() {
        return "Acessa o manual de integração e diretrizes do servidor";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = EmbedUtils.createRpg(
            "📘 Manual de Integração",
            "Bem-vindo ao ecossistema Midgard. Abaixo encontram-se os recursos essenciais para sua jornada.\n\n" +
            "🏷️ **Commerce:** [loja.midgard.com](https://loja.midgard.com/)\n" +
            "💡 **Dica:** Utilize o voucher `DISCORD` para obter **10% de abatimento** em sua primeira aquisição.",
            event.getJDA().getSelfUser()
        );
        
        embed.setImage("https://dummyimage.com/600x150/5865F2/ffffff&text=NAVEGACAO+DO+SISTEMA");

        event.replyEmbeds(embed.build())
            .addActionRow(
                Button.secondary("btn_java", "☕ Conexão Java"),
                Button.primary("btn_bedrock", "📱 Conexão Bedrock"),
                Button.success("btn_console", "🎮 Conexão Console")
            )
            .addActionRow(
                Button.link("https://wiki.midgard.com/rpg", "📄 Documentação RPG"),
                Button.link("https://wiki.midgard.com", "📚 Wiki Oficial")
            )
            .queue();
    }
}