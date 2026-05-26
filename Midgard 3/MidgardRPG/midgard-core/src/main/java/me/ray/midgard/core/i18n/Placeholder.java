package me.ray.midgard.core.i18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representa um placeholder tipado para substituição em mensagens.
 * <p>
 * Suporta múltiplos tipos de valores e formatação automática.
 *
 * @since 2.0.0
 */
public final class Placeholder {
    
    private final String key;
    private final String value;
    
    private Placeholder(String key, String value) {
        this.key = Objects.requireNonNull(key, "Placeholder key cannot be null");
        this.value = value != null ? value : "";
    }
    
    // ============================================
    // FACTORY METHODS
    // ============================================
    
    /**
     * Cria um placeholder com chave e valor string.
     *
     * @param key A chave do placeholder (sem %)
     * @param value O valor a substituir
     * @return Novo Placeholder
     */
    public static Placeholder of(String key, String value) {
        return new Placeholder(key, value);
    }
    
    /**
     * Cria um placeholder com valor numérico.
     *
     * @param key A chave do placeholder
     * @param value O valor numérico
     * @return Novo Placeholder
     */
    public static Placeholder of(String key, Number value) {
        return new Placeholder(key, value != null ? value.toString() : "0");
    }
    
    /**
     * Cria um placeholder com valor numérico formatado.
     *
     * @param key A chave do placeholder
     * @param value O valor numérico
     * @param decimalPlaces Casas decimais
     * @return Novo Placeholder
     */
    public static Placeholder of(String key, double value, int decimalPlaces) {
        String format = "%." + decimalPlaces + "f";
        return new Placeholder(key, String.format(format, value));
    }
    
    /**
     * Cria um placeholder com valor booleano (Sim/Não).
     *
     * @param key A chave do placeholder
     * @param value O valor booleano
     * @return Novo Placeholder
     */
    public static Placeholder of(String key, boolean value) {
        return new Placeholder(key, value ? "<green>Sim</green>" : "<red>Não</red>");
    }
    
    /**
     * Cria um placeholder com valor booleano customizado.
     *
     * @param key A chave do placeholder
     * @param value O valor booleano
     * @param trueValue Texto para true
     * @param falseValue Texto para false
     * @return Novo Placeholder
     */
    public static Placeholder of(String key, boolean value, String trueValue, String falseValue) {
        return new Placeholder(key, value ? trueValue : falseValue);
    }
    
    /**
     * Cria um placeholder para tempo formatado (segundos).
     *
     * @param key A chave do placeholder
     * @param seconds Tempo em segundos
     * @return Novo Placeholder com formato mm:ss
     */
    public static Placeholder time(String key, long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return new Placeholder(key, String.format("%02d:%02d", minutes, secs));
    }
    
    /**
     * Cria um placeholder para tempo formatado detalhado.
     *
     * @param key A chave do placeholder
     * @param seconds Tempo em segundos
     * @return Novo Placeholder com formato "Xh Xm Xs"
     */
    public static Placeholder timeDetailed(String key, long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.isEmpty()) sb.append(secs).append("s");
        
        return new Placeholder(key, sb.toString().trim());
    }
    
    /**
     * Cria um placeholder para porcentagem.
     *
     * @param key A chave do placeholder
     * @param value Valor entre 0 e 1 (ou 0 e 100)
     * @return Novo Placeholder formatado como porcentagem
     */
    public static Placeholder percent(String key, double value) {
        // Se valor <= 1, assume que é decimal (0.75 = 75%)
        double percentage = value <= 1 ? value * 100 : value;
        return new Placeholder(key, String.format("%.1f%%", percentage));
    }
    
    /**
     * Cria um placeholder colorido baseado em valor (vermelho → amarelo → verde).
     *
     * @param key A chave do placeholder
     * @param value Valor atual
     * @param max Valor máximo
     * @return Novo Placeholder com cor baseada na proporção
     */
    public static Placeholder colored(String key, double value, double max) {
        double ratio = max > 0 ? value / max : 0;
        String color;
        
        if (ratio >= 0.7) {
            color = "<green>";
        } else if (ratio >= 0.4) {
            color = "<yellow>";
        } else if (ratio >= 0.2) {
            color = "<gold>";
        } else {
            color = "<red>";
        }
        
        return new Placeholder(key, color + String.format("%.0f", value) + "</green></yellow></gold></red>");
    }
    
    /**
     * Cria um placeholder para barra de progresso.
     *
     * @param key A chave do placeholder
     * @param current Valor atual
     * @param max Valor máximo
     * @param length Tamanho da barra em caracteres
     * @return Novo Placeholder com barra visual
     */
    public static Placeholder progressBar(String key, double current, double max, int length) {
        double ratio = max > 0 ? Math.min(current / max, 1.0) : 0;
        int filled = (int) (ratio * length);
        int empty = length - filled;
        
        StringBuilder bar = new StringBuilder("<green>");
        bar.append("█".repeat(Math.max(0, filled)));
        bar.append("</green><gray>");
        bar.append("░".repeat(Math.max(0, empty)));
        bar.append("</gray>");
        
        return new Placeholder(key, bar.toString());
    }
    
    // ============================================
    // GETTERS
    // ============================================
    
    public String getKey() {
        return key;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Retorna a chave formatada com %.
     *
     * @return Chave no formato %key%
     */
    public String getFormattedKey() {
        return "%" + key + "%";
    }
    
    /**
     * Aplica este placeholder a uma string.
     *
     * @param message A mensagem original
     * @return Mensagem com placeholder substituído
     */
    public String apply(String message) {
        if (message == null) {
            return null;
        }
        return message.replace(getFormattedKey(), value);
    }
    
    // ============================================
    // UTILITY METHODS
    // ============================================
    
    /**
     * Cria um mapa de placeholders a partir de um array.
     *
     * @param placeholders Array de placeholders
     * @return Mapa chave → valor
     */
    public static Map<String, String> toMap(Placeholder... placeholders) {
        Map<String, String> map = new HashMap<>();
        if (placeholders != null) {
            for (Placeholder p : placeholders) {
                if (p != null) {
                    map.put(p.getKey(), p.getValue());
                }
            }
        }
        return map;
    }
    
    /**
     * Aplica múltiplos placeholders a uma mensagem.
     *
     * @param message A mensagem original
     * @param placeholders Placeholders a aplicar
     * @return Mensagem com todos os placeholders substituídos
     */
    public static String applyAll(String message, Placeholder... placeholders) {
        if (message == null) {
            return null;
        }
        if (placeholders == null) {
            return message;
        }
        
        String result = message;
        for (Placeholder p : placeholders) {
            if (p != null) {
                result = p.apply(result);
            }
        }
        return result;
    }
    
    // ============================================
    // EQUALS, HASHCODE, TOSTRING
    // ============================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Placeholder that = (Placeholder) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
    
    @Override
    public String toString() {
        return "Placeholder{" + key + "=" + value + "}";
    }
}
