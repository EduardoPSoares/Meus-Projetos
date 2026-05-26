package me.ray.midgardDiscord.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class UUIDFetcher {

    /**
     * Busca o UUID de um jogador na API da Mojang.
     * Operação bloqueante, deve ser executada assincronamente.
     */
    public static UUID getUUID(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() != 200) {
                return null;
            }
            
            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                if (json == null || !json.has("id")) return null;
                
                String id = json.get("id").getAsString();
                
                // Format UUID with dashes
                return UUID.fromString(id.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gera um UUID offline (v3) baseado no nome do jogador.
     * Usado para jogadores piratas/offline mode.
     */
    public static UUID getOfflineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
