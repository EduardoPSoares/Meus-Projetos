package me.ray.midgardDiscord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tarefa de Sincronização de Cargos e Ações.
 * Executa periodicamente para ler arquivos de fila gerados pelo Bot do Discord.
 * Aplica as alterações de grupos/permissões no LuckPerms e executa punições (Kick, Ban, Warn).
 */
public class RoleSyncTask implements Runnable {

    private final MidgardVelocity plugin;
    private final LuckPermsHandler luckPermsHandler;
    private final Logger logger;
    private final File queueFolder;
    private final Gson gson;

    public RoleSyncTask(MidgardVelocity plugin, LuckPermsHandler luckPermsHandler, Logger logger, File botDataFolder) {
        this.plugin = plugin;
        this.luckPermsHandler = luckPermsHandler;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        this.queueFolder = new File(botDataFolder, "sync_queue");
    }

    @Override
    public void run() {
        try {
            if (!queueFolder.exists() || !queueFolder.isDirectory()) return;

            // Otimização: Usar Files.list() (Stream) para evitar carregar todos os arquivos na memória
            // Isso protege contra ataques de "File Bomb" onde milhares de arquivos são criados
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(queueFolder.toPath())) {
                
                List<File> filesToProcess = stream
                    .filter(path -> path.toString().endsWith(".json"))
                    .limit(50) // Processa no máximo 50 por vez
                    .map(java.nio.file.Path::toFile)
                    .collect(java.util.stream.Collectors.toList());

                if (filesToProcess.isEmpty()) return;

                for (File file : filesToProcess) {
                    // Proteção contra Path Traversal e Symlinks
                    // Garante que o arquivo está realmente dentro da pasta de fila e não é um link para fora
                    if (!isFileSafe(file, queueFolder)) {
                        logger.warn("Arquivo suspeito ignorado (Symlink ou Path Traversal): " + file.getName());
                        // Não deleta para evitar deletar arquivos do sistema se for um link malicioso
                        continue;
                    }

                    // Proteção contra arquivos gigantes (DoS de memória)
                    if (file.length() > 50 * 1024) { // 50KB limit
                        logger.warn("Arquivo de sync ignorado por ser muito grande (>50KB): " + file.getName());
                        file.delete();
                        continue;
                    }
                    
                    try {
                        processFile(file);
                        // Sucesso: deleta o arquivo
                        if (!file.delete()) {
                            logger.warn("Não foi possível deletar o arquivo de sync processado: " + file.getName());
                        }
                    } catch (Exception e) {
                        logger.error("Erro ao processar arquivo de sync: " + file.getName(), e);
                        moveToFailed(file);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro inesperado na task RoleSyncTask: ", e);
        }
    }
    
    private void moveToFailed(File file) {
        try {
            File failedDir = new File(queueFolder, "failed");
            if (!failedDir.exists()) failedDir.mkdirs();
            
            File dest = new File(failedDir, file.getName() + ".err");
            if (file.renameTo(dest)) {
                logger.info("Arquivo movido para pasta de falhas: " + dest.getName());
            } else {
                logger.error("Falha ao mover arquivo corrompido: " + file.getName());
                // Tenta deletar em último caso para não travar a fila
                file.delete();
            }
        } catch (Exception e) {
            logger.error("Erro ao mover arquivo para falhas", e);
        }
    }

    private void processFile(File file) throws Exception {
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);
            
            if (data == null) throw new IOException("Arquivo JSON vazio ou inválido");
            
            if (data.containsKey("group")) {
                    // Role Sync
                    String uuidStr = (String) data.get("uuid");
                    String action = (String) data.get("action");
                    String group = (String) data.get("group");
                    
                    // Validation
                    if (uuidStr == null || !isValidUUID(uuidStr)) {
                        logger.warn("Sync ignorado: UUID inválido em " + file.getName());
                        return;
                    }
                    if (group == null || !group.matches("[a-zA-Z0-9_-]+")) {
                        logger.warn("Sync ignorado: Nome de grupo inválido em " + file.getName());
                        return;
                    }
                    
                    UUID uuid = UUID.fromString(uuidStr);
                    
                    if ("add".equalsIgnoreCase(action)) {
                        luckPermsHandler.addGroup(uuid, group);
                        if (plugin.getAuditLogger() != null) plugin.getAuditLogger().log("BOT", "GROUP_ADD", uuidStr, "Group: " + group);
                    } else if ("remove".equalsIgnoreCase(action)) {
                        luckPermsHandler.removeGroup(uuid, group);
                        if (plugin.getAuditLogger() != null) plugin.getAuditLogger().log("BOT", "GROUP_REMOVE", uuidStr, "Group: " + group);
                    }
                } else if (data.containsKey("type")) {
                    // Action Sync (Kick, Ban, Warn)
                    String uuidStr = (String) data.get("uuid");
                    String actionType = (String) data.get("type");
                    String rawReason = (String) data.get("reason");
                    
                    if (uuidStr == null || !isValidUUID(uuidStr)) {
                        logger.warn("Action ignorada: UUID inválido em " + file.getName());
                        return;
                    }
                    
                    // Sanitização do motivo
                    String reason = (rawReason != null && !rawReason.isEmpty()) ? rawReason : "Sem motivo";
                    if (reason.length() > 255) reason = reason.substring(0, 255) + "...";
                    
                    UUID uuid = UUID.fromString(uuidStr);
                    
                    final String finalReason = reason;
                    
                    if (plugin.getAuditLogger() != null) plugin.getAuditLogger().log("BOT", actionType, uuidStr, "Reason: " + finalReason);

                    // Captura o nome ANTES de desconectar para evitar race condition
                    String targetName = plugin.getServer().getPlayer(uuid).map(p -> p.getUsername()).orElse("Offline Player");

                    plugin.getServer().getPlayer(uuid).ifPresent(player -> {
                        try {
                            if ("KICK".equalsIgnoreCase(actionType)) {
                                player.disconnect(net.kyori.adventure.text.Component.text("Você foi expulso: " + finalReason, net.kyori.adventure.text.format.NamedTextColor.RED));
                            } else if ("BAN".equalsIgnoreCase(actionType)) {
                                player.disconnect(net.kyori.adventure.text.Component.text("Você foi banido: " + finalReason, net.kyori.adventure.text.format.NamedTextColor.RED));
                            } else if ("WARN".equalsIgnoreCase(actionType)) {
                                player.sendMessage(net.kyori.adventure.text.Component.text("⚠️ VOCÊ RECEBEU UM AVISO: " + finalReason, net.kyori.adventure.text.format.NamedTextColor.RED));
                            }
                        } catch (Exception e) {
                            logger.error("Erro ao aplicar punição ao jogador: " + player.getUsername(), e);
                        }
                    });
                    
                    if ("BAN".equalsIgnoreCase(actionType)) {
                        String discordId = plugin.getLinkManager().getDiscordId(uuid);
                        plugin.getPunishmentManager().createPunishment(uuidStr, targetName, discordId, me.ray.midgardDiscord.PunishmentManager.PunishmentType.BAN, finalReason, "DISCORD", "Discord Bot", -1);
                    } else if ("UNBAN".equalsIgnoreCase(actionType)) {
                        String discordId = plugin.getLinkManager().getDiscordId(uuid);
                        plugin.getPunishmentManager().revokePunishment(uuidStr, me.ray.midgardDiscord.PunishmentManager.PunishmentType.BAN, "Discord Bot", "Unban via Discord");
                    }
                }
                
                logger.info("Sync processado: " + file.getName());
            }
        }
    
    private boolean isValidUUID(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isFileSafe(File file, File parent) {
        try {
            String canonicalFile = file.getCanonicalPath();
            String canonicalParent = parent.getCanonicalPath();
            // Verifica se o arquivo está dentro do pai E se não é um link simbólico (comparando canonical com absolute)
            // Nota: getCanonicalPath resolve links. Se o link apontar para fora, o startWith falha.
            // Mas para detectar se É um link, comparamos canonical com absolute path.
            return canonicalFile.startsWith(canonicalParent) && 
                   file.toPath().toRealPath().equals(file.toPath().toAbsolutePath().normalize());
        } catch (Exception e) {
            return false;
        }
    }
}
