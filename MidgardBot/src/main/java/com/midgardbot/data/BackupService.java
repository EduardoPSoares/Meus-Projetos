package com.midgardbot.data;

import com.midgardbot.config.BotConfig;
import com.midgardbot.config.Constants;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.midgardbot.data.DataPersistence.*;

/**
 * Gerencia backups automáticos rotativos com upload para o Discord.
 */
final class BackupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

    private static JDA jdaInstance;

    private BackupService() {}

    static void setJDA(JDA jda) {
        jdaInstance = jda;
    }

    static void startAutoBackup() {
        SCHEDULER.scheduleAtFixedRate(() -> {
            LOGGER.info("Iniciando backup automatico rotativo...");
            performRotatingBackup();
        }, Constants.BACKUP_INTERVAL_HOURS, Constants.BACKUP_INTERVAL_HOURS, TimeUnit.HOURS);
    }

    private static void performRotatingBackup() {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        File backupDir = new File(Constants.BACKUPS_DIR + "/" + timestamp);
        if (!backupDir.exists()) backupDir.mkdirs();

        // Copia todos os arquivos de dados dos sub-managers
        copyFile(ModerationDataManager.FLAGGED_FILE, new File(backupDir, ModerationDataManager.FLAGGED_FILE.getName()));
        copyFile(WhitelistDataManager.ATTEMPTS_FILE, new File(backupDir, WhitelistDataManager.ATTEMPTS_FILE.getName()));
        copyFile(WhitelistDataManager.STATUS_FILE, new File(backupDir, WhitelistDataManager.STATUS_FILE.getName()));
        copyFile(WhitelistDataManager.PENDING_FILE, new File(backupDir, WhitelistDataManager.PENDING_FILE.getName()));
        copyFile(StaffDataManager.STAFF_STATS_FILE, new File(backupDir, StaffDataManager.STAFF_STATS_FILE.getName()));
        copyFile(WhitelistDataManager.COOLDOWNS_FILE, new File(backupDir, WhitelistDataManager.COOLDOWNS_FILE.getName()));
        copyFile(ModerationDataManager.BLACKLIST_FILE, new File(backupDir, ModerationDataManager.BLACKLIST_FILE.getName()));
        copyFile(BotStateManager.CONFIG_FILE, new File(backupDir, BotStateManager.CONFIG_FILE.getName()));
        copyFile(BotStateManager.MAINTENANCE_FILE, new File(backupDir, BotStateManager.MAINTENANCE_FILE.getName()));

        // Zip e upload para o Discord
        if (jdaInstance != null) {
            String backupChannelId = BotConfig.getBackupChannelId();
            if (backupChannelId != null && !backupChannelId.isEmpty()) {
                try {
                    File zipFile = new File(Constants.BACKUPS_DIR + "/backup_" + timestamp + ".zip");
                    zipDirectory(backupDir, zipFile);
                    TextChannel channel = jdaInstance.getTextChannelById(backupChannelId);
                    if (channel != null) {
                        channel.sendMessage("📦 **Backup Automático** - " + timestamp)
                               .addFiles(FileUpload.fromData(zipFile))
                               .queue(s -> zipFile.delete(),
                                      e -> LOGGER.error("Falha ao enviar backup para o Discord", e));
                    }
                } catch (Exception e) {
                    LOGGER.error("Erro ao criar/enviar zip de backup", e);
                }
            }
        }

        cleanOldBackups();
    }

    private static void zipDirectory(File sourceFolder, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            File[] files = sourceFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try (FileInputStream fis = new FileInputStream(file)) {
                            ZipEntry zipEntry = new ZipEntry(file.getName());
                            zos.putNextEntry(zipEntry);
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = fis.read(buffer)) >= 0) {
                                zos.write(buffer, 0, length);
                            }
                            zos.closeEntry();
                        }
                    }
                }
            }
        }
    }

    private static void cleanOldBackups() {
        File backupsFolder = new File(Constants.BACKUPS_DIR);
        if (!backupsFolder.exists()) return;
        File[] folders = backupsFolder.listFiles(File::isDirectory);
        if (folders == null) return;
        long cutoff = System.currentTimeMillis() - ((long) Constants.BACKUP_RETENTION_DAYS * 24 * 60 * 60 * 1000);
        for (File folder : folders) {
            if (folder.lastModified() < cutoff) {
                deleteDirectory(folder);
            }
        }
    }

    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}
