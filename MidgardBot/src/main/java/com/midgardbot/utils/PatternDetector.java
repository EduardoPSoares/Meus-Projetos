package com.midgardbot.utils;

import com.midgardbot.data.DataManager;
import com.midgardbot.data.WhitelistHistoryEntry;
import com.midgardbot.data.WhitelistStatus;
import com.midgardbot.data.WhitelistStatusInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Detector de Padrões Suspeitos em Whitelists.
 * Analisa automaticamente as respostas e gera alertas inteligentes.
 */
public class PatternDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatternDetector.class);
    
    // Padrões regex para detecção
    private static final Pattern LINK_PATTERN = Pattern.compile("https?://[^\\s]+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    
    /**
     * Analisa uma whitelist e gera alertas baseados em padrões suspeitos
     */
    public static void analyzeWhitelist(String userId, Map<String, String> answers) {
        try {
            // Limpa alertas antigos desta whitelist
            DataManager.clearAlerts(userId);
            
            String nick = answers.getOrDefault("q1_nick", "");
            String age = answers.getOrDefault("q2_age", "");
            String lore = answers.getOrDefault("q14_lore", "");
            
            // 1. Verificar nick suspeito ou muito curto
            checkNickname(userId, nick);
            
            // 2. Verificar idade suspeita
            checkAge(userId, age);
            
            // 3. Verificar lore muito curta
            checkLoreLength(userId, lore);
            
            // 4. Verificar links ou emails
            checkForbiddenContent(userId, answers);
            
            // 5. Verificar respostas genéricas demais
            checkGenericAnswers(userId, answers);
            
            // 6. Verificar se já existe nick similar reprovado
            checkSimilarRejected(userId, nick);
            
            // 7. Verificar múltiplas tentativas recentes
            checkMultipleAttempts(userId);
            
            LOGGER.info("Análise de padrões concluída para usuário " + userId + " (" + DataManager.getAlerts(userId).size() + " alertas gerados)");
            
        } catch (Exception e) {
            LOGGER.error("Erro ao analisar whitelist " + userId, e);
        }
    }
    
    private static void checkNickname(String userId, String nick) {
        if (nick == null || nick.trim().isEmpty()) {
            DataManager.addAlert(userId, "INVALID_NICK", "CRITICAL", 
                "❌ Nick vazio ou inválido", null);
            return;
        }
        
        if (nick.length() < 3) {
            DataManager.addAlert(userId, "SHORT_NICK", "HIGH", 
                "⚠️ Nick muito curto (menos de 3 caracteres)", null);
        }
        
        if (nick.length() > 16) {
            DataManager.addAlert(userId, "LONG_NICK", "MEDIUM", 
                "⚠️ Nick muito longo (mais de 16 caracteres)", null);
        }
        
        // Verificar números excessivos
        long digitCount = nick.chars().filter(Character::isDigit).count();
        if (digitCount > nick.length() / 2) {
            DataManager.addAlert(userId, "NUMERIC_NICK", "MEDIUM", 
                "⚠️ Nick com muitos números", null);
        }
        
        // Verificar caracteres especiais
        if (nick.matches(".*[^a-zA-Z0-9_].*")) {
            DataManager.addAlert(userId, "SPECIAL_CHARS_NICK", "LOW", 
                "ℹ️ Nick contém caracteres especiais", null);
        }
    }
    
    private static void checkAge(String userId, String age) {
        if (age == null || age.trim().isEmpty()) {
            DataManager.addAlert(userId, "INVALID_AGE", "HIGH", 
                "⚠️ Idade não informada", null);
            return;
        }
        
        try {
            int ageInt = Integer.parseInt(age);
            
            if (ageInt < 0 || ageInt > 120) {
                DataManager.addAlert(userId, "INVALID_AGE_VALUE", "CRITICAL", 
                    "❌ Idade inválida (" + ageInt + " anos)", null);
            } else if (ageInt < 14) {
                DataManager.addAlert(userId, "UNDERAGE", "HIGH", 
                    "🧒 Menor de 14 anos (" + ageInt + " anos) - Verificar servidor", null);
            } else if (ageInt < 16) {
                DataManager.addAlert(userId, "YOUNG_AGE", "MEDIUM", 
                    "👶 Idade jovem (" + ageInt + " anos) - Atenção redobrada", null);
            } else if (ageInt > 50) {
                DataManager.addAlert(userId, "UNUSUAL_AGE", "LOW", 
                    "ℹ️ Idade incomum (" + ageInt + " anos)", null);
            }
        } catch (NumberFormatException e) {
            DataManager.addAlert(userId, "INVALID_AGE_FORMAT", "HIGH", 
                "⚠️ Formato de idade inválido: " + age, null);
        }
    }
    
    private static void checkLoreLength(String userId, String lore) {
        if (lore == null || lore.trim().isEmpty()) {
            DataManager.addAlert(userId, "EMPTY_LORE", "CRITICAL", 
                "❌ Lore vazia", null);
            return;
        }
        
        int length = lore.trim().length();
        int words = lore.trim().split("\\s+").length;
        
        if (length < 100) {
            DataManager.addAlert(userId, "SHORT_LORE", "CRITICAL", 
                "❌ Lore muito curta (" + length + " caracteres, " + words + " palavras)", null);
        } else if (length < 300) {
            DataManager.addAlert(userId, "BRIEF_LORE", "HIGH", 
                "⚠️ Lore curta (" + length + " caracteres, " + words + " palavras)", null);
        } else if (length > 3000) {
            DataManager.addAlert(userId, "VERY_LONG_LORE", "LOW", 
                "ℹ️ Lore muito extensa (" + length + " caracteres, " + words + " palavras)", null);
        }
        
        // Verificar repetição excessiva
        if (detectRepetition(lore)) {
            DataManager.addAlert(userId, "REPETITIVE_LORE", "HIGH", 
                "⚠️ Lore com muito texto repetido", null);
        }
    }
    
    private static void checkForbiddenContent(String userId, Map<String, String> answers) {
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String value = entry.getValue();
            if (value == null) continue;
            
            // Verificar links
            if (LINK_PATTERN.matcher(value).find()) {
                DataManager.addAlert(userId, "CONTAINS_LINK", "MEDIUM", 
                    "⚠️ Campo '" + entry.getKey() + "' contém link/URL", null);
            }
            
            // Verificar emails
            if (EMAIL_PATTERN.matcher(value).find()) {
                DataManager.addAlert(userId, "CONTAINS_EMAIL", "MEDIUM", 
                    "⚠️ Campo '" + entry.getKey() + "' contém email", null);
            }
        }
    }
    
    private static void checkGenericAnswers(String userId, Map<String, String> answers) {
        String[] genericPhrases = {
            "não sei", "nao sei", "n sei", "talvez", "acho que", 
            "sei la", "sei lá", "tanto faz", "qualquer", "qqr"
        };
        
        int genericCount = 0;
        
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            if (entry.getKey().startsWith("_") || entry.getKey().equals("timestamp")) continue;
            
            String value = entry.getValue().toLowerCase();
            for (String phrase : genericPhrases) {
                if (value.contains(phrase)) {
                    genericCount++;
                    break;
                }
            }
        }
        
        if (genericCount > 3) {
            DataManager.addAlert(userId, "GENERIC_ANSWERS", "HIGH", 
                "⚠️ Muitas respostas genéricas ou evasivas (" + genericCount + " campos)", null);
        } else if (genericCount > 1) {
            DataManager.addAlert(userId, "SOME_GENERIC", "MEDIUM", 
                "⚠️ Algumas respostas genéricas (" + genericCount + " campos)", null);
        }
    }
    
    private static void checkSimilarRejected(String userId, String nick) {
        // Verificar whitelists reprovadas com nick similar
        Map<String, WhitelistStatusInfo> allStatus = DataManager.getAllStatus();
        
        for (Map.Entry<String, WhitelistStatusInfo> entry : allStatus.entrySet()) {
            if (entry.getKey().equals(userId)) continue;
            if (entry.getValue().status != WhitelistStatus.REJECTED) continue;
            
            String rejectedNick = entry.getValue().nickname;
            if (rejectedNick == null) continue;
            
            if (isSimilar(nick.toLowerCase(), rejectedNick.toLowerCase())) {
                DataManager.addAlert(userId, "SIMILAR_REJECTED_NICK", "HIGH", 
                    "⚠️ Nick similar a reprovação anterior: '" + rejectedNick + "' (ID: " + entry.getKey() + ")", 
                    entry.getKey());
            }
        }
    }
    
    private static void checkMultipleAttempts(String userId) {
        // Verificar se há múltiplas tentativas recentes do mesmo usuário
        long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        
        Map<String, java.util.List<WhitelistHistoryEntry>> allHistory = DataManager.getAllHistory();
        
        int recentAttempts = 0;
        java.util.List<WhitelistHistoryEntry> userHistory = allHistory.get(userId);
        if (userHistory != null) {
            for (WhitelistHistoryEntry entry : userHistory) {
                if (entry.action.equals("SUBMITTED") && entry.timestamp > oneDayAgo) {
                    recentAttempts++;
                }
            }
        }
        
        if (recentAttempts > 3) {
            DataManager.addAlert(userId, "MULTIPLE_ATTEMPTS", "HIGH", 
                "⚠️ Usuário com " + recentAttempts + " tentativas nas últimas 24h", null);
        }
    }
    
    // Métodos auxiliares
    
    private static boolean detectRepetition(String text) {
        // Algoritmo simples para detectar padrões repetidos
        int length = text.length();
        if (length < 50) return false;
        
        // Verificar se há blocos de 20+ caracteres repetidos
        for (int i = 0; i < length - 40; i++) {
            String substring = text.substring(i, i + 20);
            String remaining = text.substring(i + 20);
            if (remaining.contains(substring)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean isSimilar(String s1, String s2) {
        // Similaridade básica: Levenshtein distance
        if (s1.equals(s2)) return true;
        if (Math.abs(s1.length() - s2.length()) > 3) return false;
        
        // Se um contém o outro
        if (s1.contains(s2) || s2.contains(s1)) return true;
        
        // Levenshtein distance simplificado
        int distance = levenshteinDistance(s1, s2);
        return distance <= 2;
    }
    
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}
