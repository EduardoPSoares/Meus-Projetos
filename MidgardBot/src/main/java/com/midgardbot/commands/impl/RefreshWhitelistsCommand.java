package com.midgardbot.commands.impl;

import com.midgardbot.commands.ISlashCommand;
import com.midgardbot.config.BotConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;
import java.util.List;

public class RefreshWhitelistsCommand implements ISlashCommand {

    // private static final Logger LOGGER = LoggerFactory.getLogger(RefreshWhitelistsCommand.class);

    @Override
    public String getName() {
        return "refreshwhitelists";
    }

    @Override
    public String getDescription() {
        return "Limpa as mensagens de whitelist antigas do chat (Migração para o novo sistema)";
    }

    @Override
    public List<OptionData> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public String getPermissionKey() {
        return "PERM_CMD_WHITELIST_REFRESH";
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        // Permissão gerenciada pelo InteractionManager via getPermissionKey()
        // Fallback para ADMINISTRATOR se não configurado no env
        if (com.midgardbot.config.BotConfig.getAuthorizedRoles("PERM_CMD_WHITELIST_REFRESH").isEmpty() && 
            !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ Sem permissão. Apenas Administradores ou cargos autorizados.").setEphemeral(true).queue();
            return;
        }

        String staffChannelId = BotConfig.getStaffChannelId();
        TextChannel staffChannel = event.getJDA().getTextChannelById(staffChannelId);

        if (staffChannel == null) {
            event.reply("❌ Canal da staff não encontrado.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        java.util.concurrent.atomic.AtomicInteger deletedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        staffChannel.getIterableHistory().forEachAsync(msg -> {
            // Verifica se a mensagem é do bot
            if (!msg.getAuthor().equals(event.getJDA().getSelfUser())) return true;
            
            // Verifica se tem embeds (característica das whitelists)
            if (msg.getEmbeds().isEmpty()) return true;

            MessageEmbed embed = msg.getEmbeds().get(0);
            String description = embed.getDescription();

            // Verifica se é uma mensagem de whitelist pelo padrão do ID na descrição
            if (description != null && description.contains("**ID:**")) {
                // Apaga a mensagem antiga para limpar o chat
                msg.delete().queue(null, e -> {}); // Callback de erro vazio para ignorar falhas
                deletedCount.incrementAndGet();
            }
            
            return true;
        }).thenRun(() -> {
            event.getHook().editOriginal("✅ **Limpeza Concluída!**\n\n" + 
                "🗑️ Foram removidas **" + deletedCount.get() + "** mensagens de whitelist antigas do chat.\n" +
                "✨ O chat agora está limpo. Utilize o **Painel** ou `/analisar` para ver as pendências.").queue();
        });
    }
}
