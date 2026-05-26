package me.ray.midgardDiscord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sistema de Log de Auditoria.
 * Registra ações sensíveis (bans, kicks, alterações de whitelist, manutenção)
 * em um arquivo separado para fins de segurança e rastreabilidade.
 */
public class AuditLogger {

    private final File auditFile;
    private final ExecutorService executor;
    private final DateTimeFormatter dateFormatter;
    
    private static final long MAX_LOG_SIZE = 5 * 1024 * 1024; // 5MB

    public AuditLogger(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.auditFile = new File(dataFolder, "audit.log");
        
        // Tenta restringir permissões do arquivo de log
        secureFile(this.auditFile);
        
        this.executor = Executors.newSingleThreadExecutor();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Limpeza de logs antigos na inicialização
        cleanupOldLogs();
    }
    
    private void cleanupOldLogs() {
        executor.submit(() -> {
            try {
                File folder = auditFile.getParentFile();
                if (folder == null || !folder.exists()) return;
                
                File[] logs = folder.listFiles((dir, name) -> name.startsWith("audit_") && name.endsWith(".log"));
                if (logs == null) return;
                
                long now = System.currentTimeMillis();
                long maxAge = 30L * 24 * 60 * 60 * 1000; // 30 dias
                
                for (File log : logs) {
                    if (now - log.lastModified() > maxAge) {
                        if (log.delete()) {
                            // Loga no console, já que o audit logger pode não estar pronto ou seria recursivo
                            System.out.println("[MidgardDiscord] Log de auditoria antigo removido: " + log.getName());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private void secureFile(File file) {
        try {
            if (!file.exists()) file.createNewFile();
            
            if (java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = java.util.EnumSet.of(
                    java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                    java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
                );
                java.nio.file.Files.setPosixFilePermissions(file.toPath(), perms);
            } else {
                // Fallback para Windows/Outros: Tenta tornar legível apenas pelo dono (best effort)
                file.setReadable(true, true); // readable=true, ownerOnly=true
                file.setWritable(true, true); // writable=true, ownerOnly=true
            }
        } catch (Exception ignored) {}
    }

    public void log(String actor, String action, String target, String details) {
        // Sanitização para prevenir Log Injection (forjar logs com quebra de linha)
        final String safeActor = sanitize(actor);
        final String safeAction = sanitize(action);
        final String safeTarget = sanitize(target);
        final String safeDetails = sanitize(details);

        executor.submit(() -> {
            try {
                checkRotate();
                
                try (FileWriter fw = new FileWriter(auditFile, true);
                     BufferedWriter bw = new BufferedWriter(fw);
                     PrintWriter out = new PrintWriter(bw)) {
                    
                    String timestamp = LocalDateTime.now().format(dateFormatter);
                    String logEntry = String.format("[%s] ACTOR: %s | ACTION: %s | TARGET: %s | DETAILS: %s",
                            timestamp,
                            safeActor,
                            safeAction,
                            safeTarget,
                            safeDetails);
                    
                    out.println(logEntry);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    private String sanitize(String input) {
        if (input == null) return "N/A";
        // Remove quebras de linha e caracteres de controle
        return input.replaceAll("[\\r\\n]", " ").trim();
    }
    
    private void checkRotate() {
        if (auditFile.exists() && auditFile.length() > MAX_LOG_SIZE) {
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                File rotated = new File(auditFile.getParent(), "audit_" + timestamp + ".log");
                
                // Tenta renomear. Se falhar (ex: arquivo preso no Windows), apenas loga o erro e continua no mesmo arquivo
                if (auditFile.renameTo(rotated)) {
                    // Cria novo arquivo vazio
                    new FileOutputStream(auditFile).close();
                }
            } catch (Exception e) {
                // Falha silenciosa na rotação para não perder o log atual
                System.err.println("Falha ao rotacionar log de auditoria: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
