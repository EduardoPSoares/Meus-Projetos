package com.midgardbot.commands.impl;

import com.midgardbot.commands.CommandContext;
import com.midgardbot.commands.CommandManager;
import com.midgardbot.commands.ICommand;
import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.List;

/**
 * Comando de Ajuda (Texto).
 * Lista todos os comandos disponíveis e suas descrições.
 */
public class HelpCommand implements ICommand {

    private final CommandManager manager;

    public HelpCommand(CommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(CommandContext ctx) {
        List<String> args = ctx.getArgs();
        String prefix = BotConfig.getPrefix();

        if (args.isEmpty()) {
            EmbedBuilder builder = EmbedUtils.createInfo(
                "📂 Documentação do Sistema",
                "Utilize `" + prefix + "help <comando>` para inspecionar parâmetros específicos.",
                ctx.getJDA().getSelfUser()
            );

            StringBuilder sb = new StringBuilder();
            manager.getCommands().stream().map(ICommand::getName).forEach(
                    (it) -> sb.append("`").append(it).append("` ") // Removi o prefixo repetido para ficar mais limpo
            );

            builder.addField("🛠️ Índice de Comandos", sb.toString(), false);
            builder.addField("🔗 Recursos Externos", 
                "• [Base de Conhecimento (Wiki)](https://wiki.midgard.com)\n" +
                "• [Portal do Cliente (Store)](https://loja.midgard.com)", 
                false);
            
            ctx.getChannel().sendMessageEmbeds(builder.build()).queue();
            return;
        }

        String search = args.get(0);
        ICommand cmd = manager.getCommand(search);

        if (cmd == null) {
            ctx.getChannel().sendMessageEmbeds(EmbedUtils.createError(
                "❌ Comando Desconhecido",
                "O módulo `" + search + "` não foi localizado no registro.",
                ctx.getJDA().getSelfUser()
            ).build()).queue();
            return;
        }

        EmbedBuilder builder = EmbedUtils.createDefault(
            "📖 Módulo: " + cmd.getName().toUpperCase(),
            cmd.getHelp(),
            ctx.getJDA().getSelfUser()
        );
        
        builder.addField("⌨️ Sintaxe", "`" + prefix + cmd.getName() + "`", true);
        
        if (!cmd.getAliases().isEmpty()) {
            StringBuilder aliases = new StringBuilder();
            cmd.getAliases().forEach(alias -> aliases.append("`").append(alias).append("` "));
            builder.addField("🔀 Abreviações", aliases.toString(), true);
        }
        
        ctx.getChannel().sendMessageEmbeds(builder.build()).queue();
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getHelp() {
        return "Acessa a documentação interna dos comandos.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("ajuda", "comandos", "man");
    }
}