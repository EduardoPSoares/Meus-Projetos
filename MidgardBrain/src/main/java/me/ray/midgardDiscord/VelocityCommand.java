package me.ray.midgardDiscord;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;
import me.ray.midgardDiscord.PunishmentManager.PunishmentType;

/**
 * Comando administrativo do plugin (/midgard).
 * Permite gerenciar whitelist, manutenção e recarregar configurações via jogo ou console.
 */
public class VelocityCommand implements SimpleCommand {

    private final MidgardVelocity plugin;
    private final ProxyServer server;
    private final WhitelistManager whitelistManager;
    private final LinkManager linkManager;
    private final File botDataFolder;
    private final MaintenanceCommand maintenanceCommand;
    
    // Cooldown para comandos (UUID -> Timestamp)
    // ConcurrentHashMap para thread-safety
    private final Map<UUID, Long> commandCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COOLDOWN_TIME = 2000; // 2 segundos
    
    private final Component PREFIX = Component.empty();

    public VelocityCommand(MidgardVelocity plugin, ProxyServer server, WhitelistManager whitelistManager, LinkManager linkManager, File botDataFolder) {
        this.plugin = plugin;
        this.server = server;
        this.whitelistManager = whitelistManager;
        this.linkManager = linkManager;
        this.botDataFolder = botDataFolder;
        this.maintenanceCommand = new MaintenanceCommand(plugin, server);
        
        // Limpeza periódica do cache de cooldowns para evitar vazamento de memória
        server.getScheduler().buildTask(plugin, () -> {
            long now = System.currentTimeMillis();
            commandCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > COOLDOWN_TIME * 2);
        }).repeat(5, java.util.concurrent.TimeUnit.MINUTES).schedule();
    }

    @Override
    public void execute(Invocation invocation) {
        try {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();

            boolean isConsole = source instanceof com.velocitypowered.api.proxy.ConsoleCommandSource;
            boolean hasAdmin = source.hasPermission("midgard.admin");
            boolean hasOp = source.hasPermission("midgard.op"); // Fallback for "OP" permission
            boolean hasWildcard = source.hasPermission("*");

            if (!isConsole && !hasAdmin && !hasOp && !hasWildcard) {
                source.sendMessage(PREFIX.append(Component.text("ᴠᴏᴄê ɴãᴏ ᴛᴇᴍ ᴘᴇʀᴍɪssãᴏ ᴘᴀʀᴀ ɪssᴏ.", NamedTextColor.RED)));
                return;
            }
            
            if (source instanceof Player) {
                Player p = (Player) source;
                UUID uuid = p.getUniqueId();
                long now = System.currentTimeMillis();
                Long lastTime = commandCooldowns.get(uuid);
                if (lastTime != null && now - lastTime < COOLDOWN_TIME) {
                    source.sendMessage(PREFIX.append(Component.text("ᴀɢᴜᴀʀᴅᴇ ᴀɴᴛᴇs ᴅᴇ ᴜsᴀʀ ᴇsᴛᴇ ᴄᴏᴍᴀɴᴅᴏ ɴᴏᴠᴀᴍᴇɴᴛᴇ.", NamedTextColor.RED)));
                    return;
                }
                commandCooldowns.put(uuid, now);
            }

            if (args.length == 0) {
                sendHelp(source);
                return;
            }

            if (args[0].equalsIgnoreCase("security")) {
                if (!source.hasPermission("midgard.admin")) {
                    source.sendMessage(PREFIX.append(Component.text("ᴀᴘᴇɴᴀs ᴀᴅᴍɪɴɪsᴛʀᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ᴠᴇʀ ᴏ sᴛᴀᴛᴜs ᴅᴇ sᴇɢᴜʀᴀɴçᴀ.", NamedTextColor.RED)));
                    return;
                }
                
                source.sendMessage(Component.empty());
                source.sendMessage(Component.text("ʀᴇʟᴀᴛóʀɪᴏ ᴅᴇ sᴇɢᴜʀᴀɴçᴀ", NamedTextColor.GOLD, TextDecoration.BOLD));
                
                // Database
                boolean dbConnected = plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected();
                source.sendMessage(Component.text("Database: ", NamedTextColor.GRAY)
                    .append(Component.text(dbConnected ? "ᴄᴏɴᴇᴄᴛᴀᴅᴏ" : "ᴅᴇsᴄᴏɴᴇᴄᴛᴀᴅᴏ", dbConnected ? NamedTextColor.GREEN : NamedTextColor.RED)));
                
                if (dbConnected) {
                    String type = plugin.getDatabaseManager().getType();
                    source.sendMessage(Component.text("  Tipo: ", NamedTextColor.GRAY).append(Component.text(type, NamedTextColor.YELLOW)));
                }
                
                // Panic Mode
                // Precisamos expor o estado do panic mode no VelocityListener ou apenas checar logs
                // Como não expusemos publicamente, vamos pular ou adicionar um getter depois.
                // Por enquanto, vamos mostrar o Audit Logger
                
                boolean auditEnabled = plugin.getAuditLogger() != null;
                source.sendMessage(Component.text("Audit Logger: ", NamedTextColor.GRAY)
                    .append(Component.text(auditEnabled ? "ᴀᴛɪᴠᴏ" : "ɪɴᴀᴛɪᴠᴏ", auditEnabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
                
                return;
            }

            if (args[0].equalsIgnoreCase("ban")) {
                server.getScheduler().buildTask(plugin, () -> handleBan(source, args)).schedule();
                return;
            }
            if (args[0].equalsIgnoreCase("ban-ip")) {
                server.getScheduler().buildTask(plugin, () -> handleBanIp(source, args)).schedule();
                return;
            }
            if (args[0].equalsIgnoreCase("kick")) {
                server.getScheduler().buildTask(plugin, () -> handleKick(source, args)).schedule();
                return;
            }
            if (args[0].equalsIgnoreCase("warn")) {
                server.getScheduler().buildTask(plugin, () -> handleWarn(source, args)).schedule();
                return;
            }
            if (args[0].equalsIgnoreCase("unban")) {
                server.getScheduler().buildTask(plugin, () -> handleUnban(source, args)).schedule();
                return;
            }
            if (args[0].equalsIgnoreCase("unban-ip")) {
                server.getScheduler().buildTask(plugin, () -> handleUnbanIp(source, args)).schedule();
                return;
            }
            if (args[0].equalsIgnoreCase("unwarn")) {
                server.getScheduler().buildTask(plugin, () -> handleUnwarn(source, args)).schedule();
                return;
            }
            if (args[0].equalsIgnoreCase("debug")) {
                server.getScheduler().buildTask(plugin, () -> handleDebug(source, args)).schedule();
                return;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!source.hasPermission("midgard.admin") && !isConsole) {
                    source.sendMessage(PREFIX.append(Component.text("ᴀᴘᴇɴᴀs ᴀᴅᴍɪɴɪsᴛʀᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ʀᴇᴄᴀʀʀᴇɢᴀʀ ᴏ ᴘʟᴜɢɪɴ.", NamedTextColor.RED)));
                    return;
                }
                source.sendMessage(PREFIX.append(Component.text("ʀᴇᴄᴀʀʀᴇɢᴀɴᴅᴏ ᴄᴏɴꜰɪɢᴜʀᴀçõᴇs...", NamedTextColor.YELLOW)));
                server.getScheduler().buildTask(plugin, () -> {
                    try {
                        plugin.reload();
                        source.sendMessage(PREFIX.append(Component.text("ᴄᴏɴꜰɪɢᴜʀᴀçõᴇs ʀᴇᴄᴀʀʀᴇɢᴀᴅᴀs ᴄᴏᴍ sᴜᴄᴇssᴏ!", NamedTextColor.GREEN)));
                    } catch (Exception e) {
                        plugin.getLogger().error("Erro ao recarregar configurações: ", e);
                        source.sendMessage(PREFIX.append(Component.text("ᴇʀʀᴏ ᴀᴏ ʀᴇᴄᴀʀʀᴇɢᴀʀ. ᴠᴇʀɪꜰɪǫᴜᴇ ᴏ ᴄᴏɴsᴏʟᴇ.", NamedTextColor.RED)));
                    }
                }).schedule();
                return;
            }

            if (args[0].equalsIgnoreCase("dbreconnect")) {
                if (!source.hasPermission("midgard.admin")) {
                    source.sendMessage(PREFIX.append(Component.text("ᴀᴘᴇɴᴀs ᴀᴅᴍɪɴɪsᴛʀᴀᴅᴏʀᴇs ᴘᴏᴅᴇᴍ ʀᴇᴄᴏɴᴇᴄᴛᴀʀ ᴏ ʙᴀɴᴄᴏ.", NamedTextColor.RED)));
                    return;
                }
                source.sendMessage(PREFIX.append(Component.text("ʀᴇᴄᴏɴᴇᴄᴛᴀɴᴅᴏ ᴀᴏ ʙᴀɴᴄᴏ ᴅᴇ ᴅᴀᴅᴏs...", NamedTextColor.YELLOW)));
                server.getScheduler().buildTask(plugin, () -> {
                    try {
                        plugin.getDatabaseManager().reconnect();
                        boolean connected = plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected();
                        if (connected) {
                            source.sendMessage(PREFIX.append(Component.text("ʙᴀɴᴄᴏ ᴅᴇ ᴅᴀᴅᴏs ʀᴇᴄᴏɴᴇᴄᴛᴀᴅᴏ ᴄᴏᴍ sᴜᴄᴇssᴏ!", NamedTextColor.GREEN)));
                        } else {
                            source.sendMessage(PREFIX.append(Component.text("ꜰᴀʟʜᴀ ᴀᴏ ʀᴇᴄᴏɴᴇᴄᴛᴀʀ. ᴠᴇʀɪꜰɪǫᴜᴇ ᴏ ᴄᴏɴsᴏʟᴇ.", NamedTextColor.RED)));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().error("ᴇʀʀᴏ ᴀᴏ ʀᴇᴄᴏɴᴇᴄᴛᴀʀ ᴀᴏ ʙᴀɴᴄᴏ: ", e);
                        source.sendMessage(PREFIX.append(Component.text("ᴇʀʀᴏ ᴀᴏ ʀᴇᴄᴏɴᴇᴄᴛᴀʀ: " + e.getMessage(), NamedTextColor.RED)));
                    }
                }).schedule();
                return;
            }

            if (args[0].equalsIgnoreCase("maintenance")) {
                String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
                maintenanceCommand.execute(source, subArgs, isConsole);
                return;
            }

            if (args[0].equalsIgnoreCase("unlink")) {
                server.getScheduler().buildTask(plugin, () -> {
                    if (args.length < 2) {
                        source.sendMessage(PREFIX.append(Component.text("Sintaxe incorreta.", NamedTextColor.RED)));
                        source.sendMessage(Component.text("Uso: /midgard unlink <jogador>", NamedTextColor.GRAY));
                        return;
                    }

                    String playerName = args[1];
                    server.getPlayer(playerName).ifPresentOrElse(target -> {
                        try {
                            UUID targetUuid = target.getUniqueId();
                            if (!linkManager.isLinked(targetUuid)) {
                                source.sendMessage(PREFIX.append(Component.text("O jogador ", NamedTextColor.RED)
                                        .append(Component.text(playerName, NamedTextColor.YELLOW))
                                        .append(Component.text(" ɴãᴏ ᴘᴏsᴜɪ ᴠɪɴᴄᴜʟᴀçãᴏ.", NamedTextColor.RED))));
                            } else {
                                linkManager.unlinkAccount(targetUuid);
                                source.sendMessage(PREFIX.append(Component.text("ᴠɪɴᴄᴜʟᴀçãᴏ ᴅᴇ ", NamedTextColor.GREEN)
                                        .append(Component.text(playerName, NamedTextColor.YELLOW))
                                        .append(Component.text(" ʀᴇᴍᴏᴠɪᴅᴀ.", NamedTextColor.GREEN))));
                            }
                        } catch (Exception e) {
                            plugin.getLogger().error("Erro ao desvincular jogador: " + playerName, e);
                        }
                    }, () -> {
                        source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.", NamedTextColor.RED)));
                    });
                }).schedule();
                return;
            }

            if (args[0].equalsIgnoreCase("whitelist")) {
                server.getScheduler().buildTask(plugin, () -> {
                    if (args.length < 3) {
                        source.sendMessage(PREFIX.append(Component.text("Sintaxe incorreta.", NamedTextColor.RED)));
                        source.sendMessage(Component.text("Uso: /midgard whitelist <add/remove> <jogador>", NamedTextColor.GRAY));
                        return;
                    }

                    String action = args[1];
                    String playerName = args[2];
                    
                    String moderatorName = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
                    server.getPlayer(playerName).ifPresentOrElse(target -> {
                        try {
                            UUID targetUuid = target.getUniqueId();
                            
                            if (action.equalsIgnoreCase("add")) {
                                if (linkManager.isLinked(targetUuid)) {
                                    String discordId = linkManager.getDiscordId(targetUuid);
                                    whitelistManager.setWhitelisted(discordId, true);
                                    source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ", NamedTextColor.GREEN)
                                            .append(Component.text(playerName, NamedTextColor.YELLOW))
                                            .append(Component.text(" ᴀᴅɪᴄɪᴏɴᴀᴅᴏ à ᴡʜɪᴛᴇʟɪsᴛ.", NamedTextColor.GREEN))));
                                    source.sendMessage(Component.text("ᴠɪɴᴄᴜʟᴀçãᴏ: Discord ID ʀᴇɢɪsᴛʀᴀᴅᴏ", NamedTextColor.GRAY));
                                    if (plugin.getAuditLogger() != null) plugin.getAuditLogger().log(moderatorName, "WHITELIST_ADD", playerName, "Discord ID: " + discordId);
                                } else {
                                    whitelistManager.addBypass(targetUuid);
                                    source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ", NamedTextColor.GREEN)
                                            .append(Component.text(playerName, NamedTextColor.YELLOW))
                                            .append(Component.text(" ᴀᴅɪᴄɪᴏɴᴀᴅᴏ à ᴡʜɪᴛᴇʟɪsᴛ.", NamedTextColor.GREEN))));
                                    source.sendMessage(Component.text("ᴠɪɴᴄᴜʟᴀçãᴏ: Bypass ᴀᴛɪᴠᴀᴅᴏ", NamedTextColor.GRAY));
                                    if (plugin.getAuditLogger() != null) plugin.getAuditLogger().log(moderatorName, "WHITELIST_ADD_BYPASS", playerName, "UUID: " + targetUuid);
                                }
                            } else if (action.equalsIgnoreCase("remove")) {
                                boolean removed = false;
                                if (linkManager.isLinked(targetUuid)) {
                                    String discordId = linkManager.getDiscordId(targetUuid);
                                    whitelistManager.setWhitelisted(discordId, false);
                                    removed = true;
                                    sendSocketMessage("WHITELIST_REMOVE:" + discordId);
                                    if (plugin.getAuditLogger() != null) plugin.getAuditLogger().log(moderatorName, "WHITELIST_REMOVE", playerName, "Discord ID: " + discordId);
                                }
                                if (whitelistManager.isBypassed(targetUuid)) {
                                    whitelistManager.removeBypass(targetUuid);
                                    removed = true;
                                    if (plugin.getAuditLogger() != null) plugin.getAuditLogger().log(moderatorName, "WHITELIST_REMOVE_BYPASS", playerName, "UUID: " + targetUuid);
                                }
                                
                                if (removed) {
                                    source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ", NamedTextColor.GREEN)
                                            .append(Component.text(playerName, NamedTextColor.YELLOW))
                                            .append(Component.text(" ʀᴇᴍᴏᴠɪᴅᴏ ᴅᴀ ᴡʜɪᴛᴇʟɪsᴛ.", NamedTextColor.GREEN))));
                                } else {
                                    source.sendMessage(PREFIX.append(Component.text("O jogador ", NamedTextColor.YELLOW)
                                            .append(Component.text(playerName, NamedTextColor.YELLOW))
                                            .append(Component.text(" não estava na whitelist.", NamedTextColor.YELLOW))));
                                }
                            } else {
                                source.sendMessage(PREFIX.append(Component.text("Opção inválida. Use 'add' ou 'remove'.", NamedTextColor.RED)));
                            }
                        } catch (Exception e) {
                            plugin.getLogger().error("Erro ao gerenciar whitelist para jogador: " + playerName, e);
                        }
                    }, () -> {
                        source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.", NamedTextColor.RED)));
                    });
                }).schedule();
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao executar comando /midgard: ", e);
            invocation.source().sendMessage(Component.text("ᴇʀʀᴏ ɪɴᴛᴇʀɴᴏ ᴀᴏ ᴇxᴇᴄᴜᴛᴀʀ ᴏ ᴄᴏᴍᴀɴᴅᴏ.", NamedTextColor.RED));
        }
        
    }

    private static final List<String> COMMON_REASONS = List.of(
        "Hacks", "X-Ray", "KillAura", "Fly", 
        "Desrespeito", "Ofensa a Staff", "Toxicidade", 
        "Spam", "Flood", "Divulgacao", 
        "Bug Abuse", "Duping"
    );

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        boolean isConsole = source instanceof com.velocitypowered.api.proxy.ConsoleCommandSource;
        boolean hasAdmin = source.hasPermission("midgard.admin");
        boolean hasOp = source.hasPermission("midgard.op");
        boolean hasWildcard = source.hasPermission("*");

        if (!isConsole && !hasAdmin && !hasOp && !hasWildcard) {
            return List.of();
        }

        String[] args = invocation.arguments();
        // Se nenhum argumento foi digitado ainda ou está no primeiro argumento
        if (args.length <= 1) {
            String current = args.length == 0 ? "" : args[0];
            return filter(List.of("ban", "ban-ip", "kick", "warn", "unban", "unban-ip", "unwarn", 
            "maintenance", "whitelist", "unlink", "security", "debug", "dbreconnect", "reload"), current);
        }
        
        String subCommand = args[0].toLowerCase();
        
        // Sugerir jogadores no segundo argumento para comandos que visam jogadores
        if (args.length == 2) {
            if (subCommand.equals("debug")) {
                return filter(List.of("test-unban-msg"), args[1]);
            }
            if (List.of("ban", "ban-ip", "kick", "warn", "unlink").contains(subCommand)) {
                List<String> players = new ArrayList<>();
                server.getAllPlayers().forEach(p -> players.add(p.getUsername()));
                return filter(players, args[1]);
            }
            if (subCommand.equals("unban")) {
                // Suggest cached banned players
                return filter(new ArrayList<>(plugin.getPunishmentManager().getBannedPlayersCache()), args[1]);
            }
            if (subCommand.equals("unban-ip")) {
                // Suggest cached banned IPs
                return filter(new ArrayList<>(plugin.getPunishmentManager().getBannedIpsCache()), args[1]);
            }
            if (subCommand.equals("unwarn")) {
                List<String> players = new ArrayList<>();
                server.getAllPlayers().forEach(p -> players.add(p.getUsername()));
                return filter(players, args[1]);
            }
            if (subCommand.equals("whitelist")) {
                return filter(List.of("add", "remove"), args[1]);
            }
            if (subCommand.equals("maintenance")) {
                String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
                return maintenanceCommand.suggest(subArgs);
            }
        }
        
        // Terceiro argumento
        if (args.length == 3) {
            if (subCommand.equals("debug") && args[1].equalsIgnoreCase("test-unban-msg")) {
                 // Sugerir bans cacheados para facilitar teste
                 return filter(new ArrayList<>(plugin.getPunishmentManager().getBannedPlayersCache()), args[2]);
            }
            if (subCommand.equals("warn")) {
                return filter(List.of("low", "medium", "high"), args[2]);
            }
            if (List.of("ban", "ban-ip", "kick").contains(subCommand)) {
                // Suggest reasons
                return filter(COMMON_REASONS, args[2]);
            }
            if (subCommand.equals("whitelist")) {
                List<String> players = new ArrayList<>();
                server.getAllPlayers().forEach(p -> players.add(p.getUsername()));
                return filter(players, args[2]);
            }
            if (subCommand.equals("maintenance")) {
                String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
                return maintenanceCommand.suggest(subArgs);
            }
        }
        
        // Quarto argumento
        if (args.length == 4) {
            if (subCommand.equals("warn")) {
                // Suggest reasons for warn
                return filter(COMMON_REASONS, args[3]);
            }
            if (subCommand.equals("maintenance")) {
                String[] subArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
                return maintenanceCommand.suggest(subArgs);
            }
        }
        
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        if (prefix.isEmpty()) return list;
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }

    private void sendHelp(CommandSource source) {
        source.sendMessage(Component.empty());
        source.sendMessage(Component.text("ᴍɪᴅɢᴀʀᴅ ʀᴘɢ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(" — sɪsᴛᴇᴍᴀ ᴀᴅᴍɪɴɪsᴛʀᴀᴛɪᴠᴏ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)));
        source.sendMessage(Component.empty());
        source.sendMessage(Component.text("  /midgard whitelist <add/remove> <jogador>", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ɢᴇʀᴇɴᴄɪᴀʀ ᴀᴄᴇssᴏ ᴅᴇ ᴊᴏɢᴀᴅᴏʀᴇs", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard unlink <jogador>", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ᴅᴇsᴠɪɴᴄᴜʟᴀʀ ᴄᴏɴᴛᴀ ᴅᴏ Discord", NamedTextColor.GRAY)));
        maintenanceCommand.sendHelp(source);
        source.sendMessage(Component.text("  /midgard ban <jogador> <motivo>", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ʙᴀɴɪʀ ᴊᴏɢᴀᴅᴏʀ", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard kick <jogador> <motivo>", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ᴇxᴘᴜʟsᴀʀ ᴊᴏɢᴀᴅᴏʀ", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard warn <jogador> <severidade> <motivo>", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ᴀᴅᴠᴇʀᴛɪʀ ᴊᴏɢᴀᴅᴏʀ", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard security", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ʀᴇʟᴀᴛóʀɪᴏ ᴅᴇ sᴇɢᴜʀᴀɴçᴀ", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard dbreconnect", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ʀᴇᴄᴏɴᴇᴄᴛᴀ ᴀᴏ ʙᴀɴᴄᴏ ᴅᴇ ᴅᴀᴅᴏs", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /midgard reload", NamedTextColor.AQUA));
        source.sendMessage(Component.text("    ", NamedTextColor.DARK_GRAY)
                .append(Component.text("ʀᴇᴄᴀʀʀᴇɢᴀ ᴛᴏᴅᴀs ᴀs ᴄᴏɴꜰɪɢᴜʀᴀçõᴇs", NamedTextColor.GRAY)));
        source.sendMessage(Component.empty());
    }

    private void sendSocketMessage(String message) {
        plugin.sendSocketMessage(message);
    }

    private void handleBan(CommandSource source, String[] args) {
        try {
            if (args.length < 3) {
                source.sendMessage(PREFIX.append(Component.text("Uso: /midgard ban <jogador> <motivo>", NamedTextColor.RED)));
                return;
            }
            String playerName = args[1];
            String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            String moderator = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
            String moderatorId = (source instanceof Player) ? ((Player) source).getUniqueId().toString() : "CONSOLE";

            Player target = server.getPlayer(playerName).orElse(null);
            UUID uuid = null;
            
            if (target != null) {
                uuid = target.getUniqueId();
                target.disconnect(Component.text("Você foi banido: " + reason, NamedTextColor.RED));
            } else {
                // Offline support
                source.sendMessage(PREFIX.append(Component.text("Jogador offline. Buscando UUID...", NamedTextColor.YELLOW)));
                uuid = me.ray.midgardDiscord.utils.UUIDFetcher.getUUID(playerName);
                if (uuid == null) {
                    source.sendMessage(PREFIX.append(Component.text("Conta original não encontrada. Gerando UUID offline (Pirata)...", NamedTextColor.YELLOW)));
                    uuid = me.ray.midgardDiscord.utils.UUIDFetcher.getOfflineUUID(playerName);
                }
            }
            
            if (uuid != null) {
                String discordId = linkManager.getDiscordId(uuid);
                
                plugin.getPunishmentManager().createPunishment(uuid.toString(), playerName, discordId, PunishmentType.BAN, reason, moderatorId, moderator, -1);
                
                if (discordId != null) {
                    queueBotAction("BAN", discordId, reason, moderator);
                    source.sendMessage(PREFIX.append(Component.text("Jogador " + playerName + " banido no Minecraft e Discord.", NamedTextColor.GREEN)));
                } else {
                    source.sendMessage(PREFIX.append(Component.text("Jogador " + playerName + " banido localmente.", NamedTextColor.GREEN)));
                }
                
                if (plugin.getAuditLogger() != null) {
                    plugin.getAuditLogger().log(moderator, "BAN", playerName, "Reason: " + reason);
                }
            } else {
                source.sendMessage(PREFIX.append(Component.text("Erro fatal: Não foi possível gerar um UUID para o jogador.", NamedTextColor.RED)));
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao executar ban: ", e);
            source.sendMessage(PREFIX.append(Component.text("Erro interno ao banir jogador.", NamedTextColor.RED)));
        }
    }

    private void handleBanIp(CommandSource source, String[] args) {
        try {
            if (args.length < 3) {
                source.sendMessage(PREFIX.append(Component.text("Uso: /midgard ban-ip <jogador> <motivo>", NamedTextColor.RED)));
                return;
            }
            String playerName = args[1];
            String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            String moderator = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
            String moderatorId = (source instanceof Player) ? ((Player) source).getUniqueId().toString() : "CONSOLE";

            Player target = server.getPlayer(playerName).orElse(null);
            if (target != null) {
                String ip = target.getRemoteAddress().getAddress().getHostAddress();
                target.disconnect(Component.text("Você foi banido por IP: " + reason, NamedTextColor.RED));
                UUID uuid = target.getUniqueId();
                String discordId = linkManager.getDiscordId(uuid);
                
                plugin.getPunishmentManager().createPunishment(ip, playerName, discordId, PunishmentType.IP_BAN, reason, moderatorId, moderator, -1);
                
                if (discordId != null) {
                    queueBotAction("BAN-IP", discordId, reason, moderator);
                }
                
                source.sendMessage(PREFIX.append(Component.text("IP de " + playerName + " banido.", NamedTextColor.GREEN)));
                
                if (plugin.getAuditLogger() != null) {
                    plugin.getAuditLogger().log(moderator, "BAN-IP", playerName, "IP: " + ip);
                }
            } else {
                source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.", NamedTextColor.RED)));
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao executar ban-ip: ", e);
        }
    }

    private void handleKick(CommandSource source, String[] args) {
        try {
            if (args.length < 3) {
                source.sendMessage(PREFIX.append(Component.text("Uso: /midgard kick <jogador> <motivo>", NamedTextColor.RED)));
                return;
            }
            String playerName = args[1];
            String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            String moderator = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
            String moderatorId = (source instanceof Player) ? ((Player) source).getUniqueId().toString() : "CONSOLE";

            server.getPlayer(playerName).ifPresentOrElse(p -> {
                p.disconnect(Component.text("Você foi expulso: " + reason, NamedTextColor.RED));
                UUID uuid = p.getUniqueId();
                String discordId = linkManager.getDiscordId(uuid);
                
                plugin.getPunishmentManager().createPunishment(uuid.toString(), playerName, discordId, PunishmentType.KICK, reason, moderatorId, moderator, -1);
                
                if (discordId != null) {
                    queueBotAction("KICK", discordId, reason, moderator);
                    source.sendMessage(PREFIX.append(Component.text("Jogador expulso no Minecraft e Discord.", NamedTextColor.GREEN)));
                } else {
                    source.sendMessage(PREFIX.append(Component.text("Jogador expulso no Minecraft.", NamedTextColor.GREEN)));
                }
                
                if (plugin.getAuditLogger() != null) {
                    plugin.getAuditLogger().log(moderator, "KICK", playerName, "Reason: " + reason);
                }
            }, () -> source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.", NamedTextColor.RED))));
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao executar kick: ", e);
        }
    }

    private void handleWarn(CommandSource source, String[] args) {
        try {
            if (args.length < 4) {
                source.sendMessage(PREFIX.append(Component.text("Uso: /midgard warn <jogador> <severidade> <motivo>", NamedTextColor.RED)));
                source.sendMessage(Component.text("Severidades: low, medium, high", NamedTextColor.GRAY));
                return;
            }
            String playerName = args[1];
            String severityStr = args[2].toLowerCase();
            String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
            
            PunishmentType type;
            switch (severityStr) {
                case "low": type = PunishmentType.WARN_LOW; break;
                case "medium": type = PunishmentType.WARN_MEDIUM; break;
                case "high": type = PunishmentType.WARN_HIGH; break;
                default:
                    source.sendMessage(PREFIX.append(Component.text("Severidade inválida. Use low, medium ou high.", NamedTextColor.RED)));
                    return;
            }

            String moderator = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
            String moderatorId = (source instanceof Player) ? ((Player) source).getUniqueId().toString() : "CONSOLE";

            server.getPlayer(playerName).ifPresentOrElse(p -> {
                p.sendMessage(Component.text("⚠️ VOCÊ RECEBEU UM AVISO (" + severityStr.toUpperCase() + "): " + reason, NamedTextColor.RED));
                UUID uuid = p.getUniqueId();
                applyWarn(source, uuid, playerName, type, reason, moderatorId, moderator, severityStr);
            }, () -> {
                // Offline Warn
                source.sendMessage(PREFIX.append(Component.text("Jogador offline. Buscando UUID...", NamedTextColor.YELLOW)));
                UUID uuid = me.ray.midgardDiscord.utils.UUIDFetcher.getUUID(playerName);
                if (uuid == null) {
                    source.sendMessage(PREFIX.append(Component.text("Conta original não encontrada. Gerando UUID offline (Pirata)...", NamedTextColor.YELLOW)));
                    uuid = me.ray.midgardDiscord.utils.UUIDFetcher.getOfflineUUID(playerName);
                }
                
                if (uuid != null) {
                    applyWarn(source, uuid, playerName, type, reason, moderatorId, moderator, severityStr);
                } else {
                    source.sendMessage(PREFIX.append(Component.text("ᴊᴏɢᴀᴅᴏʀ ɴãᴏ ᴇɴᴄᴏɴᴛʀᴀᴅᴏ.", NamedTextColor.RED)));
                }
            });
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao executar warn: ", e);
            source.sendMessage(PREFIX.append(Component.text("Erro interno ao aplicar warn.", NamedTextColor.RED)));
        }
    }

    private void applyWarn(CommandSource source, UUID uuid, String playerName, PunishmentType type, String reason, String moderatorId, String moderator, String severityStr) {
        String discordId = linkManager.getDiscordId(uuid);
        plugin.getPunishmentManager().createPunishment(uuid.toString(), playerName, discordId, type, reason, moderatorId, moderator, -1);
        
        if (discordId != null) {
            queueBotAction("WARN-" + severityStr.toUpperCase(), discordId, reason, moderator);
            source.sendMessage(PREFIX.append(Component.text("Aviso enviado no Minecraft (DB) e Discord.", NamedTextColor.GREEN)));
        } else {
            source.sendMessage(PREFIX.append(Component.text("Aviso registrado no banco de dados.", NamedTextColor.GREEN)));
        }
    }

    private void handleUnwarn(CommandSource source, String[] args) {
         source.sendMessage(PREFIX.append(Component.text("Comando 'unwarn' registrado, mas avisos são apenas logs por enquanto.", NamedTextColor.YELLOW)));
    }

    private void handleUnban(CommandSource source, String[] args) {
        try {
            if (args.length < 2) {
                source.sendMessage(PREFIX.append(Component.text("Uso: /midgard unban <jogador>", NamedTextColor.RED)));
                return;
            }
            String targetName = args[1];
            String moderator = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
            
            // Resolve UUID
            UUID targetUuid = me.ray.midgardDiscord.utils.UUIDFetcher.getUUID(targetName);
            if (targetUuid == null) {
                targetUuid = me.ray.midgardDiscord.utils.UUIDFetcher.getOfflineUUID(targetName);
            }
            
            String targetIdentifier = (targetUuid != null) ? targetUuid.toString() : targetName;
            String discordId = (targetUuid != null) ? linkManager.getDiscordId(targetUuid) : null;

            plugin.getPunishmentManager().revokePunishment(targetIdentifier, PunishmentType.BAN, moderator, "Command Unban");
            
            // Remove do cache (Case Insensitive)
            plugin.getPunishmentManager().getBannedPlayersCache().removeIf(n -> n.equalsIgnoreCase(targetName));

            // Envia pro Discord
            // Se discordId for null, enviamos o nome para que o bot possa tentar logar ou pelo menos registrar o evento.
            // Mas o ideal é passar o ID se tivermos.
            queueBotAction("UNBAN", discordId != null ? discordId : targetName, "Unbanned " + targetName + " by " + moderator, moderator);
            
            source.sendMessage(PREFIX.append(Component.text("Solicitação de desbanimento processada para: " + targetName, NamedTextColor.GREEN)));
            
            if (plugin.getAuditLogger() != null) {
                plugin.getAuditLogger().log(moderator, "UNBAN", targetName, "Unbanned via command");
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao executar unban: ", e);
            source.sendMessage(PREFIX.append(Component.text("Erro interno ao desbanir jogador.", NamedTextColor.RED)));
        }
    }

    private void handleDebug(CommandSource source, String[] args) {
        if (!source.hasPermission("midgard.admin")) return;
        
        if (args.length < 2) {
            source.sendMessage(PREFIX.append(Component.text("Subcomandos: test-unban-msg", NamedTextColor.RED)));
            return;
        }
        
        if (args[1].equalsIgnoreCase("test-unban-msg")) {
            if (args.length < 3) {
                source.sendMessage(PREFIX.append(Component.text("Uso: /midgard debug test-unban-msg <jogador>", NamedTextColor.RED)));
                return;
            }
            String targetName = args[2];
            String moderator = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
            
            try {
                UUID targetUuid = me.ray.midgardDiscord.utils.UUIDFetcher.getUUID(targetName);
                if (targetUuid == null) targetUuid = me.ray.midgardDiscord.utils.UUIDFetcher.getOfflineUUID(targetName);
                
                String discordId = (targetUuid != null) ? linkManager.getDiscordId(targetUuid) : null;
                String finalId = discordId != null ? discordId : targetName;
                
                queueBotAction("UNBAN", finalId, "DEBUG: Unbanned " + targetName + " via Debug Command", moderator);
                
                source.sendMessage(PREFIX.append(Component.text("Mensagem de debug (UNBAN) enviada para a fila.", NamedTextColor.GREEN)));
                source.sendMessage(Component.text("Alvo: " + targetName, NamedTextColor.GRAY));
                source.sendMessage(Component.text("Discord ID resolvido: " + finalId, NamedTextColor.GRAY));
                
            } catch (Exception e) {
                source.sendMessage(PREFIX.append(Component.text("Erro ao executar debug: " + e.getMessage(), NamedTextColor.RED)));
                e.printStackTrace();
            }
        }
    }

    private void handleUnbanIp(CommandSource source, String[] args) {
        try {
            if (args.length < 2) {
                source.sendMessage(PREFIX.append(Component.text("Uso: /midgard unban-ip <ip>", NamedTextColor.RED)));
                return;
            }
            String targetIp = args[1];
            String moderator = (source instanceof Player) ? ((Player) source).getUsername() : "Console";
            
            plugin.getPunishmentManager().revokePunishment(targetIp, PunishmentType.IP_BAN, moderator, "Command Unban IP");
            
            queueBotAction("UNBAN", targetIp, "Unbanned IP by " + moderator, moderator);
            source.sendMessage(PREFIX.append(Component.text("Solicitação de desbanimento de IP processada para: " + targetIp, NamedTextColor.GREEN)));
            
            if (plugin.getAuditLogger() != null) {
                plugin.getAuditLogger().log(moderator, "UNBAN-IP", targetIp, "Unbanned IP via command");
            }
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao executar unban-ip: ", e);
        }
    }

    private void queueBotAction(String action, String discordId, String reason, String moderator) {
        try {
            File botQueue = new File(botDataFolder, "bot_queue");
            if (!botQueue.exists()) botQueue.mkdirs();
            
            // Proteção contra flood de arquivos (DoS de disco)
            // Se houver mais de 1000 arquivos na fila, descarta a ação ou limpa os antigos
            File[] pendingFiles = botQueue.listFiles();
            if (pendingFiles != null && pendingFiles.length >= 1000) {
                plugin.getLogger().warn("Fila do bot cheia (1000+ arquivos). Limpando arquivos antigos para evitar travamento.");
                
                // Remove os 100 mais antigos
                java.util.Arrays.stream(pendingFiles)
                    .sorted(java.util.Comparator.comparingLong(File::lastModified))
                    .limit(100)
                    .forEach(File::delete);
            }
            
            Map<String, String> data = new HashMap<>();
            data.put("action", action);
            data.put("discordId", discordId);
            data.put("reason", reason);
            data.put("moderator", moderator);
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // Nome do arquivo final
            String fileName = "bot_action_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".json";
            File finalFile = new File(botQueue, fileName);
            
            // Escrita atômica: Escreve em .tmp e move
            File tempFile = new File(botQueue, fileName + ".tmp");
            
            try (FileWriter writer = new FileWriter(tempFile)) {
                new Gson().toJson(data, writer);
            }
            
            java.nio.file.Files.move(tempFile.toPath(), finalFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            
        } catch (Exception e) {
            plugin.getLogger().error("Erro ao enfileirar ação do bot: ", e);
        }
    }
}