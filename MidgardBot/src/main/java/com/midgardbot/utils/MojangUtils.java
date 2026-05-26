package com.midgardbot.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class MojangUtils {

    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Obtém o UUID de um jogador a partir do seu nickname usando a API da Mojang.
     * @param nickname O nickname do jogador.
     * @return O UUID do jogador ou null se não encontrado/erro.
     */
    public static String getUUID(String nickname) {
        if (nickname == null || !nickname.matches("[a-zA-Z0-9_]{1,16}")) return null;
        Request request = new Request.Builder()
                .url("https://api.mojang.com/users/profiles/minecraft/" + nickname)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                if (jsonObject.has("id")) {
                    String uuidStr = jsonObject.get("id").getAsString();
                    // Insere os hifens no UUID (formato padrão 8-4-4-4-12)
                    return uuidStr.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                    );
                }
            }
        } catch (IOException e) {
            // Ignora erros de conexão, apenas retorna null
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Obtém o nickname de um jogador a partir do UUID (sem hífens) usando a API da Mojang.
     * @param uuidNoHyphens UUID sem hífens.
     * @return O nickname atual ou null se não encontrado.
     */
    public static String getNickname(String uuidNoHyphens) {
        if (uuidNoHyphens == null || !uuidNoHyphens.matches("[0-9a-fA-F]{32}")) return null;
        Request request = new Request.Builder()
                .url("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidNoHyphens)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                if (jsonObject.has("name")) {
                    return jsonObject.get("name").getAsString();
                }
            }
        } catch (IOException e) {
            // Ignora erros de conexão
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gera um UUID offline (Cracked) baseado no nickname.
     * @param nickname O nickname do jogador.
     * @return O UUID gerado.
     */
    public static java.util.UUID getOfflineUUID(String nickname) {
        return java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
