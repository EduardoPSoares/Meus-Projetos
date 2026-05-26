package com.midgardbot.features.whitelist;

import com.midgardbot.config.BotConfig;
import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import com.midgardbot.features.link.LinkManager;
import com.midgardbot.utils.RconClient;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WhitelistCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistCleaner.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static void start(JDA jda) {
        LOGGER.info("Iniciando WhitelistCleaner (Verificação de Pendentes: 1m | Integridade: 1h)...");
        
        // Tarefa 1: Limpeza de Whitelists Pendentes Inconsistentes (1 minuto)
        scheduler.scheduleAtFixedRate(WhitelistCleaner::cleanPending, 1, 1, TimeUnit.MINUTES);
        
        // Tarefa 2: Verificação de Integridade (Quem saiu do servidor) (1 hora)
        // Executa imediatamente no início e depois a cada 1 hora
        scheduler.scheduleAtFixedRate(() -> checkIntegrity(jda), 0, 1, TimeUnit.HOURS);
    }

    /**
     * Remove whitelists pendentes se o status já foi definido como Aprovado/Reprovado
     */
    private static void cleanPending() {
        try {
            Map<String, Map<String, String>> pending = DataManager.getAllPendingWhitelists();
            int removedCount = 0;
            
            for (String userId : pending.keySet()) {
                WhitelistStatusInfo info = DataManager.getStatus(userId);
                if (info == null) {
                    // Entrada órfã — formulário sem status associado (dados obsoletos)
                    LOGGER.debug("Removendo entrada orfã de pending (sem status): {}", userId);
                    DataManager.removePendingWhitelist(userId);
                    removedCount++;
                } else if (info.status == WhitelistStatus.APPROVED
                        || info.status == WhitelistStatus.REJECTED
                        || info.status == WhitelistStatus.EXCELLENT) {
                    DataManager.removePendingWhitelist(userId);
                    removedCount++;
                }
            }
            if (removedCount > 0) {
                LOGGER.info("Limpeza Pendentes: {} whitelists removidas.", removedCount);
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao limpar whitelists pendentes", e);
        }
    }

    /**
     * Verifica se usuários com whitelist/link ainda estão no servidor Discord.
     * Se não estiverem, remove os dados.
     */
    private static void checkIntegrity(JDA jda) {
        LOGGER.info("Iniciando Verificacao de Integridade (Usuarios fora do servidor)...");
        try {
            // Tenta obter a Guilda principal através de um canal conhecido
            String channelId = com.midgardbot.config.BotConfig.getStaffChannelId();
            TextChannel channel = null;
            if (channelId != null && !channelId.isEmpty()) {
                channel = jda.getTextChannelById(channelId);
            }
            
            // Se não achou pelo canal, tenta a primeira guilda disponível (fallback)
            Guild guild = null;
            if (channel != null) {
                guild = channel.getGuild();
            } else if (!jda.getGuilds().isEmpty()) {
                guild = jda.getGuilds().get(0);
            }
            
            if (guild == null) {
                LOGGER.warn("Integridade: Nenhuma guilda encontrada para verificacao. Abortando.");
                return;
            }
            
            LOGGER.info("Verificando integridade na guilda: {}", guild.getName());

            // 1. Coletar todos os IDs que precisamos verificar
            Set<String> idsToCheck = new HashSet<>();
            
            // Do Status
            idsToCheck.addAll(DataManager.getAllStatus().keySet());
            // Das Pendentes
            idsToCheck.addAll(DataManager.getAllPendingWhitelists().keySet());
            // Dos Links
            idsToCheck.addAll(LinkManager.getAllLinkedAccounts().values());
            
            // Remove IDs inválidos/legados
            idsToCheck.removeIf(id -> id == null || !id.matches("\\d+"));

            Set<String> usersToRemove = new HashSet<>();
            
            // Verificação em Lote usando retrieveMembersByIds (se possível) ou loop com retrieveMember
            // Como JDA não tem um "check bulk existence", faremos um loop com retrieveMemberById
            // Mas para não bloquear, precisamos fazer isso de forma inteligente ou aceitar a lentidão (é uma tarefa agendada a cada 1h)
            
            // Melhor abordagem para garantir integridade:
            // 1. Tenta pegar do cache (getMemberById)
            // 2. Se falhar, tenta retrieveMemberById (API)
            // 3. Se retrieve falhar com ErrorResponse.UNKNOWN_MEMBER, aí sim marca para remover.
            
            for (String userId : idsToCheck) {
                if (guild.getMemberById(userId) != null) {
                    continue; // Está no cache, seguro.
                }
                
                // Não está no cache, verificar na API
                try {
                    guild.retrieveMemberById(userId).complete(); // Síncrono pois estamos em uma thread dedicada do ScheduledExecutor
                } catch (net.dv8tion.jda.api.exceptions.ErrorResponseException e) {
                    if (e.getErrorResponse() == net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_MEMBER) {
                        usersToRemove.add(userId);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Erro ao verificar membro {}: {}", userId, e.getMessage());
                    // Em caso de erro de rede ou outro, NÃO removemos por segurança
                }
            }
            
            // Processar remoção
            if (!usersToRemove.isEmpty()) {
                LOGGER.warn("Encontrados {} usuarios confirmados fora do servidor. Removendo...", usersToRemove.size());
                for (String userId : usersToRemove) {
                    LOGGER.info("CleanUP: Removendo dados do usuario ID {} (Nao esta no servidor).", userId);
                    
                    // Tenta kickar do jogo antes de deletar dados
                    kickFromGame(userId);

                    DataManager.removeWhitelistStatus(userId);
                    DataManager.removePendingWhitelist(userId);
                    LinkManager.unlinkAccount(userId);
                    DataManager.removeCooldown(userId);
                }
                LOGGER.info("Verificacao de Integridade concluida. Limpeza finalizada.");
            } else {
                LOGGER.info("Verificacao de Integridade concluida. Nenhum usuario inconsistente encontrado.");
            }

        } catch (Exception e) {
            LOGGER.error("Erro na Verificacao de Integridade", e);
        }
    }
    
    // Tenta expulsar o jogador do servidor se tiver configuração RCON
    public static void kickFromGame(String discordId) {
        String host = BotConfig.getRconHost();
        String pass = BotConfig.getRconPassword();
        
        if (host == null || host.isEmpty() || pass == null || pass.isEmpty()) {
            return; // RCON não configurado
        }
        
        String nick = null;
        
        // Tenta achar nick pelo status
        WhitelistStatusInfo status = DataManager.getStatus(discordId);
        if (status != null) nick = status.nickname;
        
        // Se não achou, tenta pelo LinkManager (UUID -> Nickname)
        // LinkManager não guarda Nick, só UUID.
        // Tentar resolver UUID na Mojang demoraria e bloquearia thread se não for assíncrono.
        // Mas kick funciona por UUID em versões modernas? Geralmente sim, ou convertemos.
        // Assumiremos que o nick da Whitelist é o mais confiável para "quem acabou de ser aprovado".
        
        if (nick == null) {
            // Tenta obter UUID do Link
            UUID uuid = LinkManager.getUUID(discordId);
            if (uuid != null) {
               // Poderíamos tentar converter online, mas por hora vamos pular se não tiver nick conhecido
               // Se o servidor suportar kick UUID, poderíamos tentar "kick <uuid>"
            }
        }
        
        if (nick != null) {
            final String targetNick = nick;
            Executors.newSingleThreadExecutor().submit(() -> {
                try (RconClient rcon = new RconClient(host, BotConfig.getRconPort(), pass)) {
                    rcon.connect();
                    String response = rcon.sendCommand("kick " + targetNick + " Você saiu do Discord ou perdeu o vínculo!");
                    LOGGER.info("RCON Kick enviado para {}: {}", targetNick, response);
                } catch (Exception e) {
                    LOGGER.error("Falha ao enviar Kick RCON para " + targetNick, e);
                }
            });
        }
    }
    
    public static void stop() {
        scheduler.shutdown();
    }
}
