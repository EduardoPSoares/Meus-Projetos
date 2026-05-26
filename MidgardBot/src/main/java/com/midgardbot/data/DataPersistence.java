package com.midgardbot.data;

import com.google.gson.Gson;
import com.midgardbot.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Utilitários centrais de persistência JSON usados por todos os sub-managers.
 * Fornece executor de IO single-thread, scheduler, e métodos genéricos de save/load.
 */
final class DataPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPersistence.class);

    static final Gson GSON = new Gson();
    static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();
    static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    static final String D = Constants.DATA_DIR + "/";

    private DataPersistence() {}

    static void persistAsync(File file, Object data) {
        IO_EXECUTOR.submit(() -> saveSync(file, data));
    }

    static void saveSync(File file, Object data) {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            File tempFile = new File(file.getPath() + ".tmp");
            try (Writer writer = new FileWriter(tempFile)) {
                GSON.toJson(data, writer);
            }
            java.nio.file.Files.move(tempFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("Erro ao salvar " + file.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T loadFromFile(File file, Type type) {
        if (!file.exists()) return null;
        try (Reader reader = new FileReader(file)) {
            return (T) GSON.fromJson(reader, type);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Erro ao carregar " + file.getName(), e);
            return null;
        }
    }

    static void backupFile(File file) {
        if (!file.exists()) return;
        try {
            File backup = new File(file.getPath() + ".bak");
            java.nio.file.Files.copy(file.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Erro ao fazer backup de " + file.getName(), e);
        }
    }

    static void copyFile(File source, File dest) {
        if (!source.exists()) return;
        try {
            java.nio.file.Files.copy(source.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Erro ao copiar arquivo para backup: " + source.getName(), e);
        }
    }
}
