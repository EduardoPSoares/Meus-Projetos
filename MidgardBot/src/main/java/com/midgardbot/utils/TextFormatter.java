package com.midgardbot.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formatador de texto para relatórios da staff.
 * Corrige abreviações informais, capitalização, espaçamento e pontuação
 * para manter os relatórios com aparência profissional.
 * Baseado em regras — sem IA.
 */
public final class TextFormatter {

    private TextFormatter() {}

    // ── Mapa de abreviações informais → forma completa ──
    // Usa LinkedHashMap para manter ordem (palavras maiores primeiro evitam conflitos)
    private static final Map<Pattern, String> ABBREVIATIONS = new LinkedHashMap<>();

    static {
        // Ordem importa: padrões mais longos/específicos primeiro
        addAbbrev("(?i)\\btbm\\b", "também");
        addAbbrev("(?i)\\btmb\\b", "também");
        addAbbrev("(?i)\\btmj\\b", "até mais");
        addAbbrev("(?i)\\bblz\\b", "beleza");
        addAbbrev("(?i)\\bflw\\b", "falou");
        addAbbrev("(?i)\\bmsm\\b", "mesmo");
        addAbbrev("(?i)\\bngm\\b", "ninguém");
        addAbbrev("(?i)\\bvlw\\b", "valeu");
        addAbbrev("(?i)\\bpfv\\b", "por favor");
        addAbbrev("(?i)\\bpfvr\\b", "por favor");
        addAbbrev("(?i)\\bqnd\\b", "quando");
        addAbbrev("(?i)\\bqndo\\b", "quando");
        addAbbrev("(?i)\\bctz\\b", "certeza");
        addAbbrev("(?i)\\bdps\\b", "depois");
        addAbbrev("(?i)\\bdpois\\b", "depois");
        addAbbrev("(?i)\\bhrs\\b", "horas");
        addAbbrev("(?i)\\bmds\\b", "meu Deus");
        addAbbrev("(?i)\\bslk\\b", "sério");
        addAbbrev("(?i)\\bfds\\b", "fim de semana");
        addAbbrev("(?i)\\bmto\\b", "muito");
        addAbbrev("(?i)\\bmta\\b", "muita");
        addAbbrev("(?i)\\bmsg\\b", "mensagem");
        addAbbrev("(?i)\\bmsgs\\b", "mensagens");
        addAbbrev("(?i)\\bqlqr\\b", "qualquer");
        addAbbrev("(?i)\\bqlq\\b", "qualquer");
        addAbbrev("(?i)\\btd\\b", "tudo");
        addAbbrev("(?i)\\btds\\b", "todos");
        addAbbrev("(?i)\\bobs\\b", "observação");
        addAbbrev("(?i)\\binfo\\b", "informação");
        addAbbrev("(?i)\\binfos\\b", "informações");
        addAbbrev("(?i)\\bcmg\\b", "comigo");
        addAbbrev("(?i)\\bctg\\b", "contigo");
        addAbbrev("(?i)\\bndp\\b", "não deu para");
        addAbbrev("(?i)\\bnpc\\b", "NPC"); // manter sigla de jogo
        addAbbrev("(?i)\\bpvp\\b", "PvP");
        addAbbrev("(?i)\\bpve\\b", "PvE");
        addAbbrev("(?i)\\brp\\b", "RP");
        addAbbrev("(?i)\\bwl\\b", "whitelist");
        addAbbrev("(?i)\\bwls\\b", "whitelists");

        // Abreviações comuns (2 letras — mais cuidado com contexto)
        addAbbrev("(?i)\\bvc\\b", "você");
        addAbbrev("(?i)\\bvcs\\b", "vocês");
        addAbbrev("(?i)\\bpq\\b", "porque");
        addAbbrev("(?i)\\btb\\b", "também");
        addAbbrev("(?i)\\bnd\\b", "nada");
        addAbbrev("(?i)\\bqm\\b", "quem");
        addAbbrev("(?i)\\bqq\\b", "qualquer");
        addAbbrev("(?i)\\bhj\\b", "hoje");
        addAbbrev("(?i)\\bmt\\b", "muito");
        addAbbrev("(?i)\\bdpz\\b", "depois");
        addAbbrev("(?i)\\bagt\\b", "agente"); // "a gente"
        addAbbrev("(?i)\\beh\\b", "é");
        addAbbrev("(?i)\\bpra\\b", "para");
        addAbbrev("(?i)\\bpro\\b", "para o");
        addAbbrev("(?i)\\bcmg\\b", "comigo");
        addAbbrev("(?i)\\bqtd\\b", "quantidade");
        addAbbrev("(?i)\\bqts\\b", "quantos");
        addAbbrev("(?i)\\bqtas\\b", "quantas");
        addAbbrev("(?i)\\bhrj\\b", "horário");
        addAbbrev("(?i)\\bsmp\\b", "sempre");
        addAbbrev("(?i)\\btmj\\b", "estamos juntos");
        addAbbrev("(?i)\\bvdd\\b", "verdade");
    }

    private static void addAbbrev(String regex, String replacement) {
        ABBREVIATIONS.put(Pattern.compile(regex), replacement);
    }

    // ── Palavras com acentuação comumente esquecida ──
    private static final Map<Pattern, String> ACCENT_FIXES = new LinkedHashMap<>();

    static {
        addAccent("(?i)\\bvoce\\b", "você");
        addAccent("(?i)\\bvoces\\b", "vocês");
        addAccent("(?i)\\btambem\\b", "também");
        addAccent("(?i)\\bnao\\b", "não");
        addAccent("(?i)\\bentao\\b", "então");
        addAccent("(?i)\\bsituacao\\b", "situação");
        addAccent("(?i)\\bsituacoes\\b", "situações");
        addAccent("(?i)\\bpunicao\\b", "punição");
        addAccent("(?i)\\bpunicoes\\b", "punições");
        addAccent("(?i)\\baprovacao\\b", "aprovação");
        addAccent("(?i)\\baprovacoes\\b", "aprovações");
        addAccent("(?i)\\brejeicao\\b", "rejeição");
        addAccent("(?i)\\brejeicoes\\b", "rejeições");
        addAccent("(?i)\\bmoderador\\b", "moderador");
        addAccent("(?i)\\binfracao\\b", "infração");
        addAccent("(?i)\\binfracoes\\b", "infrações");
        addAccent("(?i)\\bobservacao\\b", "observação");
        addAccent("(?i)\\bobservacoes\\b", "observações");
        addAccent("(?i)\\bverificacao\\b", "verificação");
        addAccent("(?i)\\bverificacoes\\b", "verificações");
        addAccent("(?i)\\banalise\\b", "análise");
        addAccent("(?i)\\banalises\\b", "análises");
        addAccent("(?i)\\bsessao\\b", "sessão");
        addAccent("(?i)\\bsessoes\\b", "sessões");
        addAccent("(?i)\\baudiencia\\b", "audiência");
        addAccent("(?i)\\baudiencias\\b", "audiências");
        addAccent("(?i)\\bdenuncia\\b", "denúncia");
        addAccent("(?i)\\bdenuncias\\b", "denúncias");
        addAccent("(?i)\\brevisao\\b", "revisão");
        addAccent("(?i)\\brevisoes\\b", "revisões");
        addAccent("(?i)\\batualizacao\\b", "atualização");
        addAccent("(?i)\\batualizacoes\\b", "atualizações");
        addAccent("(?i)\\bconfiguracao\\b", "configuração");
        addAccent("(?i)\\bconfiguracoes\\b", "configurações");
        addAccent("(?i)\\bmanutencao\\b", "manutenção");
        addAccent("(?i)\\bmanutencoes\\b", "manutenções");
        addAccent("(?i)\\bsolucao\\b", "solução");
        addAccent("(?i)\\bsolucoes\\b", "soluções");
        addAccent("(?i)\\bpossivel\\b", "possível");
        addAccent("(?i)\\bnecessario\\b", "necessário");
        addAccent("(?i)\\bnecessaria\\b", "necessária");
        addAccent("(?i)\\bdificil\\b", "difícil");
        addAccent("(?i)\\bhorario\\b", "horário");
        addAccent("(?i)\\bhorarios\\b", "horários");
        addAccent("(?i)\\bfacil\\b", "fácil");
        addAccent("(?i)\\buteis\\b", "úteis");
        addAccent("(?i)\\butil\\b", "útil");
        addAccent("(?i)\\bperiodo\\b", "período");
        addAccent("(?i)\\bperiodos\\b", "períodos");
        addAccent("(?i)\\brelatorio\\b", "relatório");
        addAccent("(?i)\\brelatorios\\b", "relatórios");
        addAccent("(?i)\\busuario\\b", "usuário");
        addAccent("(?i)\\busuarios\\b", "usuários");
        addAccent("(?i)\\bjogador(?!es)\\b", "jogador"); // sem acento, mas manter
        addAccent("(?i)\\bconteudo\\b", "conteúdo");
        addAccent("(?i)\\bconteudos\\b", "conteúdos");
        addAccent("(?i)\\bproximo\\b", "próximo");
        addAccent("(?i)\\bproxima\\b", "próxima");
        addAccent("(?i)\\bultimo\\b", "último");
        addAccent("(?i)\\bultima\\b", "última");
        addAccent("(?i)\\binicio\\b", "início");
        addAccent("(?i)\\btermino\\b", "término");
        addAccent("(?i)\\bexito\\b", "êxito");
    }

    private static void addAccent(String regex, String replacement) {
        ACCENT_FIXES.put(Pattern.compile(regex), replacement);
    }

    // ─── Padrões de formatação ───
    private static final Pattern MULTIPLE_SPACES = Pattern.compile(" {2,}");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");
    private static final Pattern SPACE_BEFORE_PUNCT = Pattern.compile(" +([.,;:!?])");
    private static final Pattern NO_SPACE_AFTER_PUNCT = Pattern.compile("([.,;:!?])([A-Za-zÀ-ÿ])");
    private static final Pattern SENTENCE_START = Pattern.compile("(^|[.!?]\\s+)(\\p{Ll})", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern MULTIPLE_PUNCT = Pattern.compile("([.!?]){2,}");

    /**
     * Aplica todas as correções ao texto.
     */
    public static String format(String text) {
        if (text == null || text.isBlank()) return text;

        String result = text.strip();

        // 1. Substituir abreviações informais
        result = replaceAbbreviations(result);

        // 2. Corrigir acentuação
        result = fixAccents(result);

        // 3. Limpar espaçamento
        result = MULTIPLE_SPACES.matcher(result).replaceAll(" ");
        result = MULTIPLE_NEWLINES.matcher(result).replaceAll("\n\n");

        // 4. Corrigir pontuação
        result = SPACE_BEFORE_PUNCT.matcher(result).replaceAll("$1");
        result = NO_SPACE_AFTER_PUNCT.matcher(result).replaceAll("$1 $2");
        result = MULTIPLE_PUNCT.matcher(result).replaceAll("$1");

        // 5. Capitalizar início de frases
        result = capitalizeSentences(result);

        // 6. Garantir que a primeira letra é maiúscula
        if (!result.isEmpty() && Character.isLowerCase(result.charAt(0))) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }

        return result;
    }

    /**
     * Versão leve para títulos — aplica tudo + garante que não termina com ponto.
     */
    public static String formatTitle(String title) {
        if (title == null || title.isBlank()) return title;
        String result = format(title);
        // Títulos geralmente não terminam com ponto final
        if (result.endsWith(".") && !result.endsWith("...")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String replaceAbbreviations(String text) {
        String result = text;
        for (var entry : ABBREVIATIONS.entrySet()) {
            Matcher m = entry.getKey().matcher(result);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String original = m.group();
                String replacement = entry.getValue();
                // Preservar capitalização do original
                if (Character.isUpperCase(original.charAt(0))) {
                    replacement = Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    private static String fixAccents(String text) {
        String result = text;
        for (var entry : ACCENT_FIXES.entrySet()) {
            Matcher m = entry.getKey().matcher(result);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String original = m.group();
                String replacement = entry.getValue();
                // Preservar capitalização
                if (Character.isUpperCase(original.charAt(0))) {
                    replacement = Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
                }
                // Se já tem acento correto, não mexer
                if (!original.equals(replacement) && original.equalsIgnoreCase(replacement.replaceAll("[áéíóúâêîôûãõàèìòùäëïöüç]", "?"))) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                } else {
                    m.appendReplacement(sb, Matcher.quoteReplacement(original));
                }
            }
            m.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    private static String capitalizeSentences(String text) {
        // Capitalizar após . ! ? seguidos de espaço
        Matcher m = SENTENCE_START.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + m.group(2).toUpperCase()));
        }
        m.appendTail(sb);

        // Capitalizar após quebra de linha
        String[] lines = sb.toString().split("\n");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isEmpty() && Character.isLowerCase(line.charAt(0))) {
                line = Character.toUpperCase(line.charAt(0)) + line.substring(1);
            }
            if (i > 0) result.append("\n");
            result.append(line);
        }
        return result.toString();
    }
}
