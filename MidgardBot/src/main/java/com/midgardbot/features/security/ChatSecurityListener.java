package com.midgardbot.features.security;

import com.midgardbot.config.BotConfig;
import com.midgardbot.utils.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ChatSecurityListener extends ListenerAdapter {

    private static final Pattern INVITE_PATTERN = Pattern.compile("(?i)(discord\\.gg|discord\\.com/invite|discordapp\\.com/invite)/[a-zA-Z0-9]+");
    private static final Pattern SCAM_PATTERN = Pattern.compile("(?i)(steam-nitro|free-nitro|gift-discord|steam-gift|nitro-steam)\\.[a-z]+");
    
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;
        
        Member member = event.getMember();
        if (member == null) return;
        
        // Ignorar administradores e moderadores
        if (member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER)) {
            return;
        }

        // Verificar Bypass Roles
        String bypassRolesStr = BotConfig.get("CHAT_BYPASS_ROLE_IDS");
        if (bypassRolesStr != null && !bypassRolesStr.isEmpty()) {
            List<String> bypassRoleIds = Arrays.asList(bypassRolesStr.split(","));
            for (Role role : member.getRoles()) {
                if (bypassRoleIds.contains(role.getId())) {
                    return; // Usuário tem cargo de bypass
                }
            }
        }

        Message message = event.getMessage();
        String content = message.getContentRaw();

        // 1. Anti-Invite & Scam
        if (INVITE_PATTERN.matcher(content).find() || SCAM_PATTERN.matcher(content).find()) {
            message.delete().queue(
                success -> sendWarning(event.getChannel().asTextChannel(), member, "Divulgação de links proibidos ou suspeitos."),
                error -> {} // Mensagem já pode ter sido deletada
            );
            return;
        }

        // 2. Anti-Mass Mention
        long mentionCount = message.getMentions().getUsers().size() + message.getMentions().getRoles().size();
        if (mentionCount > 5) {
            message.delete().queue(
                success -> {
                    sendWarning(event.getChannel().asTextChannel(), member, "Menção em massa não permitida.");
                    // Timeout de 5 minutos
                    try {
                        member.timeoutFor(5, TimeUnit.MINUTES).reason("Anti-Mass Mention").queue();
                    } catch (Exception e) {
                        // Bot pode não ter permissão para dar timeout
                    }
                },
                error -> {}
            );
        }
    }

    private void sendWarning(TextChannel channel, Member member, String reason) {
        channel.sendMessageEmbeds(
            EmbedUtils.createError("🛡️ Segurança", member.getAsMention() + ", sua mensagem foi removida.\n**Motivo:** " + reason, channel.getJDA().getSelfUser()).build()
        ).queue(msg -> msg.delete().queueAfter(10, TimeUnit.SECONDS));
    }
}
