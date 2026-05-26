package com.midgardbot.features.whitelist;

import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache de Sessões de Whitelist.
 * Armazena temporariamente as respostas dos usuários enquanto eles preenchem o formulário de whitelist.
 * Possui limpeza automática de sessões expiradas.
 */
public class WhitelistCache {
    // Armazena as respostas temporárias: UserID -> (Pergunta -> Resposta)
    // Usando ConcurrentHashMap para thread-safety
    private static final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastUpdate = new ConcurrentHashMap<>();
    private static final long EXPIRATION_TIME = 3600000; // 1 hour

    public static void addAnswer(String userId, String question, String answer) {
        cache.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(question, answer);
        lastUpdate.put(userId, System.currentTimeMillis());
    }

    public static Map<String, String> getAnswers(String userId) {
        return cache.getOrDefault(userId, new ConcurrentHashMap<>());
    }

    public static void clear(String userId) {
        cache.remove(userId);
        lastUpdate.remove(userId);
    }
    
    public static void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = lastUpdate.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > EXPIRATION_TIME) {
                cache.remove(entry.getKey());
                // Em ConcurrentHashMap, o iterador reflete o estado no momento da criação ou atualizações.
                // remove() via iterador é seguro.
                it.remove();
            }
        }
    }
}