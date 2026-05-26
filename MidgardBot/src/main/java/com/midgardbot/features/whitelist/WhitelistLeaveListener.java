package com.midgardbot.features.whitelist;

import com.midgardbot.data.DataManager;
import com.midgardbot.features.link.LinkManager;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhitelistLeaveListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistLeaveListener.class);

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        String userId = event.getUser().getId();
        
        // Verifica se o usuário tinha whitelist ou estava pendente
        boolean hadStatus = DataManager.getStatus(userId) != null;
        boolean wasPending = DataManager.getPendingWhitelist(userId) != null;
        boolean isLinked = LinkManager.isLinked(userId);

        if (hadStatus || wasPending || isLinked) {
            LOGGER.info("Usuário {} saiu do servidor. Removendo dados de whitelist e vinculo...", event.getUser().getName());
            
            // Tenta kickar do jogo (RCON)
            WhitelistCleaner.kickFromGame(userId);

            // Remove status (Aprovado/Reprovado)
            DataManager.removeWhitelistStatus(userId);
            
            // Remove da fila de pendentes se estiver lá
            DataManager.removePendingWhitelist(userId);
            
            // Remove vínculo
            LinkManager.unlinkAccount(userId);
            
            // Remove cooldowns também, para garantir limpeza total
            DataManager.removeCooldown(userId);
        }
    }
}
