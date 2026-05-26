package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

/**
 * Comando de Ajuda (Slash).
 * Exibe um menu interativo para navegar pela documentação do bot.
 */
public class HelpSlashCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Abre o portal de assistência interativo";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = EmbedUtils.createDefault(
            "🗃️ Portal de Assistência",
            "Selecione o módulo desejado no menu abaixo para visualizar a documentação técnica.",
            event.getJDA().getSelfUser()
        );
        
        embed.addField("🔗 Navegação Rápida", 
            "📘 [Wiki Oficial](https://wiki.midgard.com)\n" +
            "💳 [Loja Virtual](https://loja.midgard.com)\n" +
            "📢 [Suporte Discord](https://discord.gg/midgard)", 
            false);

        StringSelectMenu menu = StringSelectMenu.create("help_menu")
            .setPlaceholder("Selecione um módulo...")
            .addOption("Geral & Utilitários", "cat_general", "Comandos de uso público", net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("📝"))
            .addOption("Whitelist & Acesso", "cat_whitelist", "Gestão de entrada no servidor", net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("🛡️"))
            .addOption("Administração", "cat_admin", "Ferramentas de moderação", net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("👮"))
            .build();

        event.replyEmbeds(embed.build())
            .addActionRow(menu)
            .setEphemeral(true)
            .queue();
    }
}