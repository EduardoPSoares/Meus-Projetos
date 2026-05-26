package com.midgardbot.features.whitelist;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuração de Perguntas da Whitelist.
 * Gerencia as perguntas que serão feitas aos usuários durante o processo de aplicação.
 * Permite personalização via arquivo JSON (data/whitelist_questions.json).
 */
public class WhitelistConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistConfig.class);
    private static final File QUESTIONS_FILE = new File("data/whitelist_questions.json");
    private static final Gson GSON = new Gson();
    
    private static Map<String, Map<String, String>> questionsCache = new LinkedHashMap<>();

    static {
        loadQuestions();
    }

    public static void loadQuestions() {
        if (!QUESTIONS_FILE.exists()) {
            LOGGER.warn("Arquivo de perguntas nao encontrado. Usando padrao.");
            loadDefaults();
            return;
        }
        try (Reader reader = new FileReader(QUESTIONS_FILE)) {
            Type type = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
            Map<String, Map<String, String>> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                questionsCache = loaded;
                LOGGER.info("Perguntas da whitelist recarregadas com sucesso!");
            } else {
                loadDefaults();
            }
        } catch (IOException e) {
            LOGGER.error("Erro ao carregar perguntas da whitelist", e);
            loadDefaults();
        }
    }

    private static void loadDefaults() {
        Map<String, String> p1 = new LinkedHashMap<>();
        p1.put("q1_nick", "Qual seu nick no Minecraft?");
        p1.put("q2_age", "Qual a sua idade?");
        p1.put("q3_xp", "Já participou de servidores de RP? Quais?");
        p1.put("q4_punish", "Já foi punido/expulso? Explique.");
        p1.put("q5_secondary", "Aceita interpretar chars secundários?");
        questionsCache.put("part1", p1);

        Map<String, String> p2 = new LinkedHashMap<>();
        p2.put("q6_frustration", "Como lida com frustrações/derrotas?");
        p2.put("q7_protagonism", "Preparado para perder protagonismo?");
        p2.put("q8_good_rp", "O que considera um bom roleplayer?");
        p2.put("q9_bad_rp", "O que é comportamento prejudicial ao RP?");
        p2.put("q10_concepts", "Diferencie Metagaming e Powergaming.");
        questionsCache.put("part2", p2);

        Map<String, String> p3 = new LinkedHashMap<>();
        p3.put("q11_break", "Reação a quebra de clima/incoerência?");
        p3.put("q12_ic_ooc", "Como separa Jogador de Personagem?");
        p3.put("q13_flaws", "Já interpretou chars com defeitos reais?");
        p3.put("q14_lore", "Lore: O Despertar (Quem é você?)");
        questionsCache.put("part3", p3);
    }
    
    public static Map<String, String> getPart1Questions() {
        return questionsCache.getOrDefault("part1", new LinkedHashMap<>());
    }

    public static Map<String, String> getPart2Questions() {
        return questionsCache.getOrDefault("part2", new LinkedHashMap<>());
    }

    public static Map<String, String> getPart3Questions() {
        return questionsCache.getOrDefault("part3", new LinkedHashMap<>());
    }
    
    // Helper para pegar o texto da pergunta pelo ID (para o Embed final)
    public static String getQuestionTitle(String key) {
        Map<String, String> all = new LinkedHashMap<>();
        all.putAll(getPart1Questions());
        all.putAll(getPart2Questions());
        all.putAll(getPart3Questions());
        return all.getOrDefault(key, key);
    }

    public static Map<String, String> getQuestionsByPage(int page) {
        switch (page) {
            case 0: return getPart1Questions();
            case 1: return getPart2Questions();
            case 2: return getPart3Questions();
            default: return new LinkedHashMap<>();
        }
    }

    public static String getPageTitle(int page) {
        switch (page) {
            case 0: return "👤 Identificação & Experiência";
            case 1: return "🧠 Comportamento & Conceitos";
            case 2: return "📖 Lore & Personagem";
            default: return "Desconhecido";
        }
    }
}