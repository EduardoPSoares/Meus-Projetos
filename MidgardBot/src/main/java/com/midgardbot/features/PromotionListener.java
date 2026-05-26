package com.midgardbot.features;

import com.midgardbot.config.BotConfig;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener de Divulgação.
 * Adiciona automaticamente o cargo de Streamer/Criador de Conteúdo
 * quando um usuário envia uma mensagem nos canais de divulgação.
 */
public class PromotionListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PromotionListener.class);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Ignora bots e webhooks
        if (event.getAuthor().isBot() || event.isWebhookMessage()) return;
        
        // Verifica se é uma mensagem em servidor
        if (!event.isFromGuild()) return;

        try {
            String streamAnnounceChannelId = BotConfig.getStreamAnnounceChannelId();
            String channelId = event.getChannel().getId();

            // Verifica se o canal atual é o canal de divulgação (Stream Announce)
            if (streamAnnounceChannelId != null && streamAnnounceChannelId.equals(channelId)) {
                String content = event.getMessage().getContentRaw().toLowerCase();
                String streamerRoleId = BotConfig.getStreamerRoleId();
                String creatorRoleId = BotConfig.getCreatorRoleId();
                
                // Lógica de detecção de plataforma
                boolean isStreamer = content.contains("twitch.tv") || content.contains("kick.com");
                boolean isCreator = content.contains("youtube.com") || content.contains("youtu.be") || content.contains("tiktok.com") || content.contains("instagram.com");

                // Se não detectou nada específico, mas postou no canal, assume Streamer como padrão (ou não faz nada?)
                // Vamos assumir que se postou link, é um dos dois. Se não tiver link conhecido, ignora.
                
                if (isStreamer && streamerRoleId != null && !streamerRoleId.isEmpty()) {
                    assignRole(event, streamerRoleId, "Streamer");
                }
                
                if (isCreator && creatorRoleId != null && !creatorRoleId.isEmpty()) {
                    assignRole(event, creatorRoleId, "Criador de Conteúdo");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erro no PromotionListener", e);
        }
    }

    private void assignRole(MessageReceivedEvent event, String roleId, String roleName) {
        try {
            Role role = event.getGuild().getRoleById(roleId);
            if (role == null) {
                LOGGER.warn("Cargo de " + roleName + " não encontrado: " + roleId);
                return;
            }

            if (event.getMember() != null && !event.getMember().getRoles().contains(role)) {
                event.getGuild().addRoleToMember(event.getMember(), role).queue(
                    success -> LOGGER.info("Cargo de " + roleName + " adicionado para: " + event.getAuthor().getName()),
                    error -> LOGGER.error("Erro ao adicionar cargo de " + roleName + " para: " + event.getAuthor().getName(), error)
                );
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao processar atribuição de cargo " + roleName, e);
        }
    }
}
