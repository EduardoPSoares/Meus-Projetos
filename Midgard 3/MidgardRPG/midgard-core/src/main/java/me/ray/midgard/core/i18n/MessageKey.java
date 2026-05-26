package me.ray.midgard.core.i18n;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Representa uma chave de mensagem tipada com metadados.
 * <p>
 * Permite rastrear:
 * <ul>
 *     <li>A chave da mensagem no YAML</li>
 *     <li>Os placeholders esperados</li>
 *     <li>O módulo de origem</li>
 *     <li>Localização no código para debug</li>
 * </ul>
 *
 * @since 2.0.0
 */
public final class MessageKey {
    
    private final String key;
    private final String module;
    private final Set<String> expectedPlaceholders;
    private final String fallbackKey;
    private final String defaultValue;
    
    // Debug info
    private String sourceClass;
    private int sourceLine;
    
    private MessageKey(Builder builder) {
        this.key = builder.key;
        this.module = builder.module;
        this.expectedPlaceholders = Collections.unmodifiableSet(new HashSet<>(builder.expectedPlaceholders));
        this.fallbackKey = builder.fallbackKey;
        this.defaultValue = builder.defaultValue;
    }
    
    /**
     * Cria uma MessageKey simples a partir de uma string.
     *
     * @param key A chave da mensagem (ex: "combat.mode.enabled")
     * @return Nova instância de MessageKey
     */
    public static MessageKey of(String key) {
        return builder(key).build();
    }
    
    /**
     * Cria uma MessageKey com módulo especificado.
     *
     * @param module O módulo de origem (ex: "combat")
     * @param key A chave relativa (ex: "mode.enabled")
     * @return Nova instância de MessageKey
     */
    public static MessageKey of(String module, String key) {
        return builder(module + "." + key)
                .module(module)
                .build();
    }
    
    /**
     * Inicia um builder para construção fluente.
     *
     * @param key A chave da mensagem
     * @return Builder configurável
     */
    public static Builder builder(String key) {
        return new Builder(key);
    }
    
    /**
     * Cria uma cópia desta MessageKey com placeholders adicionais.
     *
     * @param placeholders Os placeholders esperados
     * @return Nova MessageKey com placeholders
     */
    public MessageKey withPlaceholders(String... placeholders) {
        return builder(this.key)
                .module(this.module)
                .placeholders(placeholders)
                .fallback(this.fallbackKey)
                .defaultValue(this.defaultValue)
                .build();
    }
    
    /**
     * Cria uma cópia com uma chave de fallback.
     *
     * @param fallbackKey Chave alternativa se esta não existir
     * @return Nova MessageKey com fallback
     */
    public MessageKey withFallback(String fallbackKey) {
        return builder(this.key)
                .module(this.module)
                .placeholders(this.expectedPlaceholders.toArray(new String[0]))
                .fallback(fallbackKey)
                .defaultValue(this.defaultValue)
                .build();
    }
    
    /**
     * Define informações de debug (classe e linha de origem).
     * Usado internamente pelo sistema de validação.
     *
     * @param className Nome da classe
     * @param line Número da linha
     * @return Esta instância (modificada)
     */
    public MessageKey withSourceInfo(String className, int line) {
        this.sourceClass = className;
        this.sourceLine = line;
        return this;
    }
    
    // ============================================
    // GETTERS
    // ============================================
    
    public String getKey() {
        return key;
    }
    
    public String getModule() {
        return module;
    }
    
    public Set<String> getExpectedPlaceholders() {
        return expectedPlaceholders;
    }
    
    public String getFallbackKey() {
        return fallbackKey;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public boolean hasFallback() {
        return fallbackKey != null && !fallbackKey.isEmpty();
    }
    
    public boolean hasDefaultValue() {
        return defaultValue != null && !defaultValue.isEmpty();
    }
    
    public String getSourceClass() {
        return sourceClass;
    }
    
    public int getSourceLine() {
        return sourceLine;
    }
    
    public boolean hasSourceInfo() {
        return sourceClass != null;
    }
    
    /**
     * Extrai o módulo da chave automaticamente.
     * Ex: "combat.mode.enabled" → "combat"
     *
     * @return O módulo inferido ou null
     */
    public String inferModule() {
        if (module != null) {
            return module;
        }
        if (key == null || !key.contains(".")) {
            return null;
        }
        return key.substring(0, key.indexOf('.'));
    }
    
    /**
     * Retorna o caminho esperado do arquivo YAML.
     * Ex: "combat.mode.enabled" -> "modules/combat/lang/messages.yml"
     *
     * @return Caminho relativo do arquivo
     */
    public String getExpectedFilePath() {
        String mod = inferModule();
        if (mod == null) {
            return "lang/messages.yml";
        }
        return "modules/" + mod + "/lang/messages.yml";
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
        MessageKey that = (MessageKey) o;
        return Objects.equals(key, that.key);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MessageKey{");
        sb.append("key='").append(key).append('\'');
        if (module != null) {
            sb.append(", module='").append(module).append('\'');
        }
        if (!expectedPlaceholders.isEmpty()) {
            sb.append(", placeholders=").append(expectedPlaceholders);
        }
        if (hasSourceInfo()) {
            sb.append(", source=").append(sourceClass).append(":").append(sourceLine);
        }
        sb.append('}');
        return sb.toString();
    }
    
    // ============================================
    // BUILDER
    // ============================================
    
    public static class Builder {
        private final String key;
        private String module;
        private final Set<String> expectedPlaceholders = new HashSet<>();
        private String fallbackKey;
        private String defaultValue;
        
        private Builder(String key) {
            if (key == null || key.trim().isEmpty()) {
                throw new IllegalArgumentException("MessageKey cannot be null or empty");
            }
            this.key = key.trim();
        }
        
        /**
         * Define o módulo de origem.
         *
         * @param module Nome do módulo
         * @return Este builder
         */
        public Builder module(String module) {
            this.module = module;
            return this;
        }
        
        /**
         * Define os placeholders esperados nesta mensagem.
         *
         * @param placeholders Nomes dos placeholders (sem %)
         * @return Este builder
         */
        public Builder placeholders(String... placeholders) {
            if (placeholders != null) {
                this.expectedPlaceholders.addAll(Arrays.asList(placeholders));
            }
            return this;
        }
        
        /**
         * Define uma chave de fallback caso esta não exista.
         *
         * @param fallbackKey Chave alternativa
         * @return Este builder
         */
        public Builder fallback(String fallbackKey) {
            this.fallbackKey = fallbackKey;
            return this;
        }
        
        /**
         * Define um valor padrão caso nem a chave nem o fallback existam.
         *
         * @param defaultValue Valor padrão em MiniMessage format
         * @return Este builder
         */
        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }
        
        /**
         * Constrói a MessageKey.
         *
         * @return Nova instância de MessageKey
         */
        public MessageKey build() {
            return new MessageKey(this);
        }
    }
}
