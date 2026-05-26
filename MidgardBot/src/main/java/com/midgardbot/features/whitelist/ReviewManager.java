package com.midgardbot.features.whitelist;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReviewManager {
    // CandidateID -> StaffID
    private static final Map<String, String> activeReviews = new ConcurrentHashMap<>();
    // CandidateID -> Timestamp (para expirar locks velhos se necessário)
    private static final Map<String, Long> reviewTimestamps = new ConcurrentHashMap<>();

    /**
     * Verifica se uma whitelist está sendo analisada por alguém.
     */
    public static boolean isUnderReview(String candidateId) {
        // Verifica timeout (ex: 15 minutos)
        if (reviewTimestamps.containsKey(candidateId)) {
            long start = reviewTimestamps.get(candidateId);
            if (System.currentTimeMillis() - start > 15 * 60 * 1000) {
                endReview(candidateId); // Expira o lock
                return false;
            }
        }
        return activeReviews.containsKey(candidateId);
    }

    /**
     * Limpa locks expirados (Garbage Collection).
     */
    public static void cleanExpiredLocks() {
        long now = System.currentTimeMillis();
        long timeout = 15 * 60 * 1000; // 15 minutos

        for (Map.Entry<String, Long> entry : reviewTimestamps.entrySet()) {
            if (now - entry.getValue() > timeout) {
                endReview(entry.getKey());
            }
        }
    }

    /**
     * Inicia a revisão (Lock).
     */
    public static void startReview(String candidateId, String staffId) {
        activeReviews.put(candidateId, staffId);
        reviewTimestamps.put(candidateId, System.currentTimeMillis());
    }

    /**
     * Finaliza a revisão (Unlock).
     */
    public static void endReview(String candidateId) {
        activeReviews.remove(candidateId);
        reviewTimestamps.remove(candidateId);
    }

    /**
     * Retorna quem está analisando (para debug ou info).
     */
    public static String getReviewer(String candidateId) {
        return activeReviews.get(candidateId);
    }

    /**
     * Retorna um mapa de todas as revisões ativas (Candidato -> Staff).
     */
    public static Map<String, String> getActiveReviews() {
        return new ConcurrentHashMap<>(activeReviews);
    }
}
