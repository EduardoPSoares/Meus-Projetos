package com.midgardbot.features.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de Sincronização de Cargos.
 * Detecta mudanças de cargos no Discord e cria arquivos de fila para o plugin Minecraft processar.
 * Garante que VIPs e Staffs tenham suas permissões atualizadas no jogo.
 */
public class RoleSyncManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleSyncManager.class);
    private static final File DATA_FOLDER = new File("data");
    private static final File PENDING_FILE = new File(DATA_FOLDER, "pending_roles.json");
    private static final File QUEUE_FOLDER = new File(DATA_FOLDER, "sync_queue");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // DiscordID -> List of Pending Actions
    private static Map<String, List<PendingAction>> pendingRoles = new ConcurrentHashMap<>();

    public static class PendingAction {
        public String action; // "add" or "remove"
        public String group;
        public long timestamp;

        public PendingAction(String action, String group) {
            this.action = action;
            this.group = group;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class SyncQueueItem {
        public String uuid;
        public String action;
        public String group;
        public long timestamp;

        public SyncQueueItem(String uuid, String action, String group) {
            this.uuid = uuid;
            this.action = action;
            this.group = group;
            this.timestamp = System.currentTimeMillis();
        }
    }

    static {
        try {
            if (!DATA_FOLDER.exists()) DATA_FOLDER.mkdirs();
            if (!QUEUE_FOLDER.exists()) QUEUE_FOLDER.mkdirs();
            loadPending();
            LOGGER.info("RoleSyncManager inicializado com sucesso.");
        } catch (Throwable t) {
            LOGGER.error("Falha crítica ao inicializar RoleSyncManager", t);
        }
    }

    // Adiciona a lista de pendentes (usuario nao vinculado)
    public static void addPending(String discordId, String action, String group) {
        loadPending();
        pendingRoles.computeIfAbsent(discordId, k -> new ArrayList<>()).add(new PendingAction(action, group));
        savePending();
        LOGGER.info("Acao pendente armazenada para " + discordId + ": " + action + " " + group);
    }

    // Adiciona a fila de sincronizacao (usuario vinculado)
    public static void queueSync(String uuid, String action, String group) {
        SyncQueueItem item = new SyncQueueItem(uuid, action, group);
        String fileName = System.currentTimeMillis() + "_" + uuid + ".json";
        File file = new File(QUEUE_FOLDER, fileName);
        
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(item, writer);
            LOGGER.info("Sincronizacao enfileirada (Arquivo): " + fileName + " -> " + action + " " + group);
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar arquivo de sync: " + fileName, e);
        }
    }

    // Processa pendencias quando o usuario vincula a conta
    public static void processPending(String discordId, String uuid) {
        loadPending();
        if (pendingRoles.containsKey(discordId)) {
            List<PendingAction> actions = pendingRoles.get(discordId);
            for (PendingAction pa : actions) {
                queueSync(uuid, pa.action, pa.group);
            }
            pendingRoles.remove(discordId);
            savePending();
            LOGGER.info("Pendencias processadas para " + discordId + " -> " + uuid);
        }
    }

    private static void loadPending() {
        if (!PENDING_FILE.exists()) return;
        try (Reader reader = new FileReader(PENDING_FILE)) {
            Type type = new TypeToken<Map<String, List<PendingAction>>>(){}.getType();
            Map<String, List<PendingAction>> data = gson.fromJson(reader, type);
            if (data != null) {
                pendingRoles = new ConcurrentHashMap<>(data);
            }
        } catch (IOException e) {
            LOGGER.error("Erro ao carregar pending_roles.json", e);
        }
    }

    private static void savePending() {
        try (Writer writer = new FileWriter(PENDING_FILE)) {
            gson.toJson(pendingRoles, writer);
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar pending_roles.json", e);
        }
    }
}
