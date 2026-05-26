package com.midgardbot.features.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração de Sincronização de Cargos.
 * Carrega e salva o mapeamento entre IDs de cargos do Discord e nomes de grupos do LuckPerms.
 * Arquivo: data/role_sync_config.json
 */
public class RoleSyncConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleSyncConfig.class);
    private static final File CONFIG_FILE = new File("data/role_sync_config.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static ConfigData data;

    public static class ConfigData {
        public String adminRoleId = "000000000000000000"; // ID do cargo que pode acionar a sync
        public Map<String, String> roleMap = new HashMap<>(); // Discord Role ID -> LuckPerms Group Name
    }

    static {
        load();
    }

    public static void load() {
        if (!CONFIG_FILE.getParentFile().exists()) {
            CONFIG_FILE.getParentFile().mkdirs();
        }

        if (!CONFIG_FILE.exists()) {
            data = new ConfigData();
            // Exemplo
            data.roleMap.put("123456789012345678", "vip");
            save();
            return;
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            data = gson.fromJson(reader, ConfigData.class);
            if (data == null) data = new ConfigData();
        } catch (IOException e) {
            LOGGER.error("Erro ao carregar role_sync_config.json", e);
            data = new ConfigData();
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            LOGGER.error("Erro ao salvar role_sync_config.json", e);
        }
    }

    public static String getAdminRoleId() {
        return data.adminRoleId;
    }

    public static String getGroupForRole(String roleId) {
        return data.roleMap.get(roleId);
    }
    
    public static boolean isSyncRole(String roleId) {
        return data.roleMap.containsKey(roleId);
    }
}
