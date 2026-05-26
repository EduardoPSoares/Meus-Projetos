package me.ray.midgardDiscord;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listener de eventos do Velocity.
 * Gerencia conexões de jogadores, verificações de whitelist e ping do servidor.
 */
public class VelocityListener {

    private final MidgardVelocity plugin;
    private final ProxyServer server;
    private final org.slf4j.Logger logger;
    private final WhitelistManager whitelistManager;
    private final LinkManager linkManager;
    
    // Rate Limiting
    private final Map<String, Long> connectionCooldowns = new ConcurrentHashMap<>();
    private static final long CONNECTION_COOLDOWN_MS = 3000; // 3 segundos

    // Panic Mode (Anti-Bot Attack)
    private final AtomicInteger failedLoginsCount = new AtomicInteger(0);
    private volatile boolean panicMode = false;
    private static final int PANIC_THRESHOLD = 50; // 50 falhas por minuto
    private static final long PANIC_DURATION_MS = 5 * 60 * 1000; // 5 minutos

    public VelocityListener(MidgardVelocity plugin, ProxyServer server, org.slf4j.Logger logger, WhitelistManager whitelistManager, LinkManager linkManager) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.whitelistManager = whitelistManager;
        this.linkManager = linkManager;
        
        // Limpeza periódica do cache de rate limit e reset do contador de falhas
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            long now = System.currentTimeMillis();
            connectionCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > CONNECTION_COOLDOWN_MS * 2);
            
            // Reset contador de falhas a cada minuto
            if (failedLoginsCount.getAndSet(0) > 0 && !panicMode) {
                // Log discreto se houve falhas mas não ativou panic
                // plugin.getLogger().info("Falhas de login no último minuto: " + count);
            }
        }).repeat(1, TimeUnit.MINUTES).schedule();
    }

    private void incrementFailure() {
        if (panicMode) return;
        
        int count = failedLoginsCount.incrementAndGet();
        if (count >= PANIC_THRESHOLD) {
            activatePanicMode();
        }
    }

    private void activatePanicMode() {
        if (panicMode) return;
        panicMode = true;
        plugin.getLogger().warn("!!! PANIC MODE ATIVADO !!!");
        plugin.getLogger().warn("Detectado alto volume de falhas de login (" + PANIC_THRESHOLD + "/min).");
        plugin.getLogger().warn("Bloqueando conexões não-administrativas por 5 minutos.");
        
        if (plugin.getAuditLogger() != null) {
            plugin.getAuditLogger().log("SYSTEM", "PANIC_MODE_ON", "ALL", "High failure rate detected");
        }
        
        // Desativa após 5 minutos
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            panicMode = false;
            plugin.getLogger().info("Panic Mode desativado. Retornando à operação normal.");
            if (plugin.getAuditLogger() != null) {
                plugin.getAuditLogger().log("SYSTEM", "PANIC_MODE_OFF", "ALL", "Timer expired");
            }
        }).delay(PANIC_DURATION_MS, TimeUnit.MILLISECONDS).schedule();
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getIdentifier() instanceof MinecraftChannelIdentifier)) return;
        MinecraftChannelIdentifier id = (MinecraftChannelIdentifier) event.getIdentifier();
        
        if (id.getNamespace().equals("midgard") && id.getName().equals("death")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            try {
                String subChannel = in.readUTF();
                if (subChannel.equals("DEATH")) {
                    String player = in.readUTF();
                    String killer = in.readUTF();
                    String message = in.readUTF();
                    
                    // Forward to Bot via Socket
                    plugin.sendSocketMessage("DEATH:" + player + ":" + killer + ":" + message);
                } else if (subChannel.equals("KICK")) {
                    String playerName = in.readUTF();
                    String reason = in.readUTF();
                    server.getPlayer(playerName).ifPresent(p -> p.disconnect(Component.text(reason)));
                } else if (subChannel.equals("WHITELIST_REMOVE")) {
                    String playerName = in.readUTF();
                    server.getPlayer(playerName).ifPresent(p -> {
                        UUID uuid = p.getUniqueId();
                        String discordId = linkManager.getDiscordId(uuid);
                        if (discordId != null) {
                            // Envia comando para o Bot remover do cache/arquivo
                            plugin.sendSocketMessage("WHITELIST_REMOVE:" + discordId);
                            plugin.getLogger().info("Solicitacao de remocao de whitelist enviada para o Bot: " + playerName + " (" + discordId + ")");
                        }
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().error("Erro ao processar mensagem de morte: " + e.getMessage());
            }
        }
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        try {
            ServerPing ping = event.getPing();
            
            // Verifica se o RPG (ou qualquer servidor importante) está em manutenção
            // Se sim, altera o MOTD para indicar manutenção
            // Isso permite que o Bot do Discord detecte a manutenção via ping
            boolean rpgMaintenance = plugin.isMaintenance("rpg");
            boolean lobbyMaintenance = plugin.isMaintenance("lobby");
            
            if (rpgMaintenance || lobbyMaintenance) {
                ServerPing.Builder builder = ping.asBuilder();
                
                // Verifica se há um agendamento para voltar (OFF)
                Long endTimeRpg = plugin.getMaintenanceScheduler().getScheduledEndTime("rpg");
                Boolean typeRpg = plugin.getMaintenanceScheduler().getScheduledType("rpg");
                
                // Prioriza mostrar o tempo de volta do RPG se existir e for do tipo OFF (false)
                String timeRemaining = null;
                if (endTimeRpg != null && typeRpg != null && !typeRpg) {
                    long diff = endTimeRpg - System.currentTimeMillis();
                    if (diff > 0) {
                        timeRemaining = formatTime(diff / 1000);
                    }
                }

                Component motd;
                if (timeRemaining != null) {
                    motd = Component.text("        ")
                        .append(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.newline())
                        .append(Component.text("     sᴇʀᴠɪᴅᴏʀ ᴇᴍ ᴍᴀɴᴜᴛᴇɴçãᴏ. ʀᴇᴛᴏʀɴᴏ ᴇᴍ ", NamedTextColor.GRAY))
                        .append(Component.text(timeRemaining, NamedTextColor.GREEN, TextDecoration.BOLD));
                } else {
                    motd = Component.text("        ")
                        .append(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.newline())
                        .append(Component.text("    sᴇʀᴠɪᴅᴏʀ ᴇᴍ ᴍᴀɴᴜᴛᴇɴçãᴏ.", NamedTextColor.GRAY));
                }
                
                builder.description(motd);
                event.setPing(builder.build());
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao processar ping do servidor: ", e);
        }
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) {
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            return h + "h" + (m > 0 ? " " + m + "m" : "");
        } else if (seconds >= 60) {
            long m = seconds / 60;
            return m + "m";
        } else {
            return seconds + "s";
        }
    }

    @Subscribe
    public EventTask onLogin(LoginEvent event) {
        return EventTask.async(() -> {
            try {
                Player player = event.getPlayer();
                UUID uuid = player.getUniqueId();
                
                // Panic Mode Check
                if (panicMode) {
                    if (!player.hasPermission("midgard.admin") && !player.hasPermission("midgard.staff")) {
                        event.setResult(LoginEvent.ComponentResult.denied(
                            Component.text("sᴇʀᴠɪᴅᴏʀ ᴛᴇᴍᴘᴏʀᴀʀɪᴀᴍᴇɴᴛᴇ ɪɴᴅɪsᴘᴏɴíᴠᴇʟ. ᴛᴇɴᴛᴇ ɴᴏᴠᴀᴍᴇɴᴛᴇ ᴇᴍ ɪɴsᴛᴀɴᴛᴇs.", NamedTextColor.RED)
                        ));
                        return;
                    }
                }
                
                // Rate Limiting Check
                String ip = player.getRemoteAddress().getAddress().getHostAddress();
                long now = System.currentTimeMillis();
                Long lastConnect = connectionCooldowns.get(ip);
                if (lastConnect != null && now - lastConnect < CONNECTION_COOLDOWN_MS) {
                    event.setResult(LoginEvent.ComponentResult.denied(
                        Component.text("ᴀɢᴜᴀʀᴅᴇ ᴀɴᴛᴇs ᴅᴇ ᴄᴏɴᴇᴄᴛᴀʀ ɴᴏᴠᴀᴍᴇɴᴛᴇ.", NamedTextColor.RED)
                    ));
                    if (plugin.getAuditLogger() != null) {
                        plugin.getAuditLogger().log("SYSTEM", "LOGIN_DENIED_RATELIMIT", player.getUsername() + " (" + uuid + ")", "IP: " + ip);
                    }
                    incrementFailure();
                    return;
                }
                connectionCooldowns.put(ip, now);
                
                String discordId = linkManager.getDiscordId(uuid);

                // 0. Verifica Banimentos (Prioridade Absoluta)
                // Verifica UUID, IP e Discord ID
                PunishmentManager.Punishment activePunishment = plugin.getPunishmentManager().getActiveBan(uuid.toString(), ip, discordId);
                
                if (activePunishment != null) {
                    Component reasonComp = Component.text("ᴠᴏᴄê ᴇsᴛá ʙᴀɴɪᴅᴏ ᴅᴏ sᴇʀᴠɪᴅᴏʀ.", NamedTextColor.RED)
                        .append(Component.newline())
                        .append(Component.text("ᴍᴏᴛɪᴠᴏ: " + (activePunishment.reason != null ? activePunishment.reason : "Sem motivo"), NamedTextColor.YELLOW));
                    
                    if (activePunishment.endTime > 0) {
                         long diff = (activePunishment.endTime - System.currentTimeMillis()) / 1000;
                         String timeStr = formatTime(Math.max(0, diff));
                         reasonComp = reasonComp.append(Component.newline())
                             .append(Component.text("ᴇxᴘɪʀᴀ ᴇᴍ: " + timeStr, NamedTextColor.GRAY));
                    }
                    
                    event.setResult(LoginEvent.ComponentResult.denied(reasonComp));
                    
                    if (plugin.getAuditLogger() != null) {
                         plugin.getAuditLogger().log("SYSTEM", "LOGIN_DENIED_BANNED", player.getUsername(), "Locked by: " + activePunishment.type + " #" + activePunishment.id);
                    }
                    incrementFailure();
                    return;
                }

                // 1. Verifica se está na Whitelist (Prioridade Máxima)
                // Se o jogador estiver na whitelist ou tiver bypass, permite a entrada
                if (whitelistManager.isWhitelisted(discordId) || whitelistManager.isBypassed(uuid)) {
                    return;
                }
                
                // 1.1 Correção para Migração: Se o jogador já vinculou (discordId != null) mas não está na whitelist pelo ID,
                // verifica se ele está aprovado pelo Nickname (Legacy) e atualiza o registro.
                if (discordId != null && whitelistManager.isApprovedByNickname(player.getUsername())) {
                    logger.info("Jogador vinculado detectado com whitelist legada. Atualizando registro: " + player.getUsername() + " -> " + discordId);
                    if (whitelistManager.updateDiscordId(player.getUsername(), discordId)) {
                        // Sucesso na atualização, permite a entrada
                        return;
                    }
                }

                // 2. Motivo: Não Vinculado
                // Se não tiver conta do Discord vinculada, nega a entrada com instruções
                if (discordId == null) {
                    // Verifica se o jogador já foi aprovado na whitelist pelo Nickname
                    if (whitelistManager.isApprovedByNickname(player.getUsername())) {
                        String code = linkManager.generateCode(uuid);
                        
                        if (code == null) {
                            event.setResult(LoginEvent.ComponentResult.denied(
                                Component.text("Por favor, aguarde alguns segundos antes de tentar entrar novamente.", NamedTextColor.RED)
                            ));
                            return;
                        }
                        
                        Component kickMessage = Component.empty()
                                .append(Component.newline())
                                .append(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD))
                                .append(Component.newline())
                                .append(Component.newline())
                                .append(Component.text("ᴠɪɴᴄᴜʟᴀçãᴏ ɴᴇᴄᴇssáʀɪᴀ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                                .append(Component.newline())
                                .append(Component.newline())
                                .append(Component.text("sᴜᴀ ᴡʜɪᴛᴇʟɪsᴛ ꜰᴏɪ ᴀᴘʀᴏᴠᴀᴅᴀ.", NamedTextColor.GRAY))
                                .append(Component.text("ᴘᴀʀᴀ ᴀᴄᴇssᴀʀ, ᴠɪɴᴄᴜʟᴇ sᴜᴀ ᴄᴏɴᴛᴀ ᴀᴏ ᴅɪsᴄᴏʀᴅ.", NamedTextColor.GRAY))
                                .append(Component.newline())
                                .append(Component.newline())
                                .append(Component.text("ᴄóᴅɪɢᴏ ᴅᴇ ᴠɪɴᴄᴜʟᴀçãᴏ:", NamedTextColor.WHITE))
                                .append(Component.newline())
                                .append(Component.text(code, NamedTextColor.YELLOW, TextDecoration.BOLD))
                                .append(Component.newline())
                                .append(Component.newline())
                                .append(Component.text("discord.gg/midgardrpg", NamedTextColor.AQUA))
                                .append(Component.newline());

                        event.setResult(LoginEvent.ComponentResult.denied(kickMessage));
                    } else {
                        // Se não foi aprovado (ou nem aplicou), manda fazer a whitelist
                        Component kickMessage = Component.empty()
                                .append(Component.newline())
                                .append(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD))
                                .append(Component.newline())
                                .append(Component.newline())
                                .append(Component.text("ᴀᴄᴇssᴏ ʀᴇsᴛʀɪᴛᴏ", NamedTextColor.RED, TextDecoration.BOLD))
                                .append(Component.newline())
                                .append(Component.newline())
                                .append(Component.text("ᴇsᴛᴇ sᴇʀᴠɪᴅᴏʀ ᴜᴛɪʟɪᴢᴀ sɪsᴛᴇᴍᴀ ᴅᴇ ᴡʜɪᴛᴇʟɪsᴛ.", NamedTextColor.GRAY))
                                .append(Component.newline())
                                .append(Component.text("ᴘᴀʀᴀ sᴏʟɪᴄɪᴛᴀʀ ᴀᴄᴇssᴏ, ᴘᴀʀᴛɪᴄɪᴘᴇ ᴅᴏ ɴᴏsᴏ", NamedTextColor.GRAY))
                                .append(Component.newline())
                                .append(Component.text("ᴅɪsᴄᴏʀᴅ ᴇ sɪɢᴀ ᴀs ɪɴsᴛʀᴜçõᴇs.", NamedTextColor.GRAY))
                                .append(Component.newline())
                                .append(Component.newline())
                                .append(Component.text("discord.gg/midgardrpg", NamedTextColor.AQUA))
                                .append(Component.newline());
                        
                        event.setResult(LoginEvent.ComponentResult.denied(kickMessage));
                        incrementFailure();
                    }
                    return;
                }

                // 3. Motivo: Vinculado, mas Whitelist Pendente/Recusada/Inexistente
                String status = whitelistManager.getStatus(discordId);
                Component kickMessage;

                if ("PENDING".equalsIgnoreCase(status)) {
                    kickMessage = Component.empty()
                            .append(Component.newline())
                            .append(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("sᴏʟɪᴄɪᴛᴀçãᴏ ᴇᴍ ᴀɴáʟɪsᴇ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("sᴜᴀ sᴏʟɪᴄɪᴛᴀçãᴏ ᴇsᴛá sᴇɴᴅᴏ ᴀɴᴀʟɪsᴀᴅᴀ.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("ᴠᴏᴄê sᴇʀá ɴᴏᴛɪꜰɪᴄᴀᴅᴏ ɴᴏ ᴅɪsᴄᴏʀᴅ.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("discord.gg/midgardrpg", NamedTextColor.AQUA))
                            .append(Component.newline());
                } else if ("REJECTED".equalsIgnoreCase(status)) {
                    String reason = whitelistManager.getReason(discordId);
                    if (reason == null || reason.isEmpty()) reason = "Não especificado";
                    
                    kickMessage = Component.empty()
                            .append(Component.newline())
                            .append(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("sᴏʟɪᴄɪᴛᴀçãᴏ ʀᴇᴄᴜsᴀᴅᴀ", NamedTextColor.RED, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("sᴜᴀ sᴏʟɪᴄɪᴛᴀçãᴏ ᴅᴇ ᴡʜɪᴛᴇʟɪsᴛ ꜰᴏɪ ʀᴇᴄᴜsᴀᴅᴀ.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("ᴍᴏᴛɪᴠᴏ: ", NamedTextColor.GRAY))
                            .append(Component.text(reason, NamedTextColor.YELLOW))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("ᴠᴏᴄê ᴘᴏᴅᴇ ᴛᴇɴᴛᴀʀ ɴᴏᴠᴀᴍᴇɴᴛᴇ ᴇᴍ ʙʀᴇᴠᴇ.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("discord.gg/midgardrpg", NamedTextColor.AQUA))
                            .append(Component.newline());
                    
                    if (plugin.getAuditLogger() != null) {
                        plugin.getAuditLogger().log("SYSTEM", "LOGIN_DENIED_REJECTED", player.getUsername(), "Discord ID: " + discordId + ", Reason: " + reason);
                    }
                    incrementFailure();
                } else if ("STANDBY".equalsIgnoreCase(status)) {
                    kickMessage = Component.empty()
                            .append(Component.newline())
                            .append(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("ᴡʜɪᴛᴇʟɪsᴛ ᴇᴍ sᴛᴀɴᴅʙʏ", NamedTextColor.YELLOW, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("sᴜᴀ ᴡʜɪᴛᴇʟɪsᴛ ᴇsᴛá ᴇᴍ sᴛᴀɴᴅʙʏ ᴅᴇᴠɪᴅᴏ ᴀ", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("ᴜᴍᴀ ɪɴᴛɪᴍᴀçãᴏ ᴘᴇɴᴅᴇɴᴛᴇ.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("ᴄᴏɴꜰɪʀᴍᴇ ᴏ ʀᴇᴄᴇʙɪᴍᴇɴᴛᴏ ᴅᴀ ɪɴᴛɪᴍᴀçãᴏ", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("ɴᴏ ᴅɪsᴄᴏʀᴅ ᴘᴀʀᴀ ʀᴇᴄᴜᴘᴇʀᴀʀ ᴏ ᴀᴄᴇssᴏ.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("discord.gg/midgardrpg", NamedTextColor.AQUA))
                            .append(Component.newline());
                    
                    if (plugin.getAuditLogger() != null) {
                        plugin.getAuditLogger().log("SYSTEM", "LOGIN_DENIED_STANDBY", player.getUsername(), "Discord ID: " + discordId);
                    }
                    incrementFailure();
                } else {
                    kickMessage = Component.empty()
                            .append(Component.newline())
                            .append(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("ᴀᴄᴇssᴏ ʀᴇsᴛʀɪᴛᴏ", NamedTextColor.RED, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("ᴇsᴛᴇ sᴇʀᴠɪᴅᴏʀ ᴜᴛɪʟɪᴢᴀ sɪsᴛᴇᴍᴀ ᴅᴇ ᴡʜɪᴛᴇʟɪsᴛ.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("ᴘᴀʀᴀ sᴏʟɪᴄɪᴛᴀʀ ᴀᴄᴇssᴏ, ᴘᴀʀᴛɪᴄɪᴘᴇ ᴅᴏ ɴᴏsᴏ", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("ᴅɪsᴄᴏʀᴅ ᴇ sɪɢᴀ ᴀs ɪɴsᴛʀᴜçõᴇs.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("discord.gg/midgardrpg", NamedTextColor.AQUA))
                            .append(Component.newline());
                    incrementFailure();
                }
                
                event.setResult(LoginEvent.ComponentResult.denied(kickMessage));
            } catch (Exception e) {
                plugin.getLogger().error("Erro ao processar login do jogador: ", e);
                event.setResult(LoginEvent.ComponentResult.denied(Component.text("ᴇʀʀᴏ ᴀᴏ ᴠᴇʀɪꜰɪᴄᴀʀ sᴇᴜ ᴀᴄᴇssᴏ. ᴛᴇɴᴛᴇ ɴᴏᴠᴀᴍᴇɴᴛᴇ.", NamedTextColor.RED)));
            }
        });
    }

    @Subscribe
    public void onServerConnect(ServerPreConnectEvent event) {
        try {
            Player player = event.getPlayer();
            RegisteredServer target = event.getOriginalServer();
            String targetName = target.getServerInfo().getName();
            
            // Lógica de Manutenção
            if (plugin.isMaintenance(targetName)) {
                boolean isStaff = player.hasPermission("midgard.staff");
                boolean isAdmin = player.hasPermission("midgard.admin");
                boolean isOp = player.hasPermission("midgard.op");
                boolean isBypass = player.hasPermission("midgard.maintenance.bypass");
                boolean isConfigAdmin = plugin.isAdmin(player.getUsername());

                if (!isStaff && !isAdmin && !isOp && !isBypass && !isConfigAdmin) {
                    
                    // Check for scheduled end time
                    MaintenanceScheduler scheduler = plugin.getMaintenanceScheduler();
                    Long endTime = scheduler.getScheduledEndTime(targetName);
                    Boolean type = scheduler.getScheduledType(targetName);
                    String timeRemaining = null;
                    
                    if (endTime != null && type != null && !type) {
                        long remainingMillis = endTime - System.currentTimeMillis();
                        if (remainingMillis > 0) {
                            timeRemaining = formatTime(remainingMillis / 1000);
                        }
                    }

                    // Redireciona para o Lobby se tentar entrar em um servidor em manutenção
                    String finalTimeRemaining = timeRemaining;
                    java.util.Optional<RegisteredServer> lobbyOpt = server.getServer(plugin.getLobbyServerName());
                    
                    if (lobbyOpt.isEmpty()) {
                        // Lobby não encontrado: nega conexão ao servidor em manutenção
                        event.setResult(ServerPreConnectEvent.ServerResult.denied());
                        plugin.getLogger().warn("Lobby '{}' não encontrado! Negando conexão de {} ao servidor em manutenção '{}'.",
                            plugin.getLobbyServerName(), player.getUsername(), targetName);
                        return;
                    }
                    
                    RegisteredServer lobby = lobbyOpt.get();
                    if (!target.equals(lobby)) {
                        event.setResult(ServerPreConnectEvent.ServerResult.allowed(lobby));
                        Component msg = Component.empty()
                            .append(Component.newline())
                            .append(Component.text("sᴇʀᴠɪᴅᴏʀ ɪɴᴅɪsᴘᴏɴíᴠᴇʟ", NamedTextColor.RED, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("ᴏ sᴇʀᴠɪᴅᴏʀ ", NamedTextColor.GRAY))
                            .append(Component.text(targetName, NamedTextColor.WHITE))
                            .append(Component.text(" ᴇsᴛá ᴛᴇᴍᴘᴏʀᴀʀɪᴀᴍᴇɴᴛᴇ", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("ᴇᴍ ᴍᴀɴᴜᴛᴇɴçãᴏ. ᴠᴏᴄê ꜰᴏɪ ʀᴇᴅɪʀᴇᴄɪᴏɴᴀᴅᴏ", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("ᴘᴀʀᴀ ᴏ ʟᴏʙʙʏ.", NamedTextColor.GRAY));
                        
                        if (finalTimeRemaining != null) {
                            msg = msg.append(Component.newline())
                                     .append(Component.newline())
                                     .append(Component.text("ʀᴇᴛᴏʀɴᴏ ᴘʀᴇᴠɪsᴛᴏ ᴇᴍ ", NamedTextColor.GRAY))
                                     .append(Component.text(finalTimeRemaining, NamedTextColor.GREEN, TextDecoration.BOLD));
                        }
                        
                        msg = msg.append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("discord.gg/midgardrpg", NamedTextColor.AQUA))
                            .append(Component.newline());
                        player.sendMessage(msg);
                    } else {
                        // Se o lobby estiver em manutenção e o jogador tentar entrar nele (login), kicka
                        event.setResult(ServerPreConnectEvent.ServerResult.denied());
                        Component kickMessage = Component.empty()
                            .append(Component.newline())
                            .append(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("sᴇʀᴠɪᴅᴏʀ ᴇᴍ ᴍᴀɴᴜᴛᴇɴçãᴏ", NamedTextColor.RED, TextDecoration.BOLD))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("ᴏ sᴇʀᴠɪᴅᴏʀ ᴇɴᴄᴏɴᴛʀᴀ-sᴇ ᴛᴇᴍᴘᴏʀᴀʀɪᴀᴍᴇɴᴛᴇ", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("ɪɴᴅɪsᴘᴏɴíᴠᴇʟ.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.newline());
                            
                        if (finalTimeRemaining != null) {
                            kickMessage = kickMessage
                                    .append(Component.text("ʀᴇᴛᴏʀɴᴏ ᴘʀᴇᴠɪsᴛᴏ ᴇᴍ ", NamedTextColor.GRAY))
                                    .append(Component.text(finalTimeRemaining, NamedTextColor.GREEN, TextDecoration.BOLD));
                        } else {
                            kickMessage = kickMessage
                                    .append(Component.text("ʀᴇᴛᴏʀɴᴏ ᴇᴍ ʙʀᴇᴠᴇ.", NamedTextColor.GRAY));
                        }
                        
                        kickMessage = kickMessage
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("ᴀɢʀᴀᴅᴇᴄᴇᴍᴏs sᴜᴀ ᴄᴏᴍᴘʀᴇᴇɴsãᴏ.", NamedTextColor.DARK_GRAY))
                            .append(Component.newline())
                            .append(Component.newline())
                            .append(Component.text("discord.gg/midgardrpg", NamedTextColor.AQUA))
                            .append(Component.newline());
                        player.disconnect(kickMessage);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao processar conexão do servidor: ", e);
        }
    }
}