package me.ray.midgard.core.i18n.validation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Relatório detalhado de chaves de mensagem faltantes, não utilizadas ou problemáticas.
 * <p>
 * Contém:
 * <ul>
 *     <li>Chaves faltantes (usadas no código mas não existem nos YAMLs)</li>
 *     <li>Chaves não utilizadas (existem nos YAMLs mas nunca são usadas)</li>
 *     <li>Chaves com placeholders faltantes</li>
 *     <li>Sugestões de correção</li>
 * </ul>
 *
 * @since 2.0.0
 */
public class MissingKeyReport {
    
    private final LocalDateTime generatedAt;
    private final String moduleName;
    
    private final List<MissingKeyEntry> missingKeys = new ArrayList<>();
    private final List<UnusedKeyEntry> unusedKeys = new ArrayList<>();
    private final List<PlaceholderIssue> placeholderIssues = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    
    public MissingKeyReport() {
        this(null);
    }
    
    public MissingKeyReport(String moduleName) {
        this.generatedAt = LocalDateTime.now();
        this.moduleName = moduleName;
    }
    
    // ============================================
    // ADD METHODS
    // ============================================
    
    /**
     * Adiciona uma chave faltante ao relatório.
     *
     * @param key A chave que está faltando
     * @param usedInClass Classe onde foi usada
     * @param usedAtLine Linha onde foi usada
     * @param expectedFile Arquivo YAML esperado
     */
    public void addMissingKey(String key, String usedInClass, int usedAtLine, String expectedFile) {
        missingKeys.add(new MissingKeyEntry(key, usedInClass, usedAtLine, expectedFile));
    }
    
    /**
     * Adiciona uma chave faltante simplificada.
     *
     * @param key A chave que está faltando
     * @param expectedFile Arquivo YAML esperado
     */
    public void addMissingKey(String key, String expectedFile) {
        missingKeys.add(new MissingKeyEntry(key, null, 0, expectedFile));
    }
    
    /**
     * Adiciona uma chave não utilizada ao relatório.
     *
     * @param key A chave não utilizada
     * @param definedInFile Arquivo onde está definida
     */
    public void addUnusedKey(String key, String definedInFile) {
        unusedKeys.add(new UnusedKeyEntry(key, definedInFile));
    }
    
    /**
     * Adiciona um problema de placeholder.
     *
     * @param key A chave com problema
     * @param expectedPlaceholders Placeholders esperados
     * @param foundPlaceholders Placeholders encontrados na mensagem
     */
    public void addPlaceholderIssue(String key, Set<String> expectedPlaceholders, Set<String> foundPlaceholders) {
        placeholderIssues.add(new PlaceholderIssue(key, expectedPlaceholders, foundPlaceholders));
    }
    
    /**
     * Adiciona um aviso geral.
     *
     * @param warning O aviso
     */
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    // ============================================
    // GETTERS
    // ============================================
    
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public String getModuleName() {
        return moduleName;
    }
    
    public List<MissingKeyEntry> getMissingKeys() {
        return Collections.unmodifiableList(missingKeys);
    }
    
    public List<UnusedKeyEntry> getUnusedKeys() {
        return Collections.unmodifiableList(unusedKeys);
    }
    
    public List<PlaceholderIssue> getPlaceholderIssues() {
        return Collections.unmodifiableList(placeholderIssues);
    }
    
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
    
    public boolean hasIssues() {
        return !missingKeys.isEmpty() || !unusedKeys.isEmpty() || 
               !placeholderIssues.isEmpty() || !warnings.isEmpty();
    }
    
    public int getTotalIssues() {
        return missingKeys.size() + unusedKeys.size() + 
               placeholderIssues.size() + warnings.size();
    }
    
    // ============================================
    // FORMATTING METHODS
    // ============================================
    
    /**
     * Gera uma representação formatada para console.
     *
     * @return String formatada para log
     */
    public String toConsoleFormat() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║         RELATÓRIO DE VALIDAÇÃO - MIDGARD                   ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        
        if (moduleName != null) {
            sb.append(String.format("║ Módulo: %-53s ║\n", moduleName));
        }
        sb.append(String.format("║ Gerado em: %-50s ║\n", 
                generatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
        sb.append(String.format("║ Total de Problemas: %-41d ║\n", getTotalIssues()));
        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        
        // Chaves faltantes
        if (!missingKeys.isEmpty()) {
            sb.append("\n🔴 CHAVES FALTANTES (").append(missingKeys.size()).append("):\n");
            sb.append("─────────────────────────────────────────────────────────────────\n");
            for (MissingKeyEntry entry : missingKeys) {
                sb.append(String.format("  [FALTANTE] %s\n", entry.key));
                if (entry.usedInClass != null) {
                    sb.append(String.format("             └─ Usada em: %s:%d\n", 
                            getSimpleClassName(entry.usedInClass), entry.usedAtLine));
                }
                sb.append(String.format("             └─ Arquivo esperado: %s\n", entry.expectedFile));
            }
        }
        
        // Chaves não utilizadas
        if (!unusedKeys.isEmpty()) {
            sb.append("\n🟡 CHAVES NÃO UTILIZADAS (").append(unusedKeys.size()).append("):\n");
            sb.append("─────────────────────────────────────────────────────────────────\n");
            for (UnusedKeyEntry entry : unusedKeys) {
                sb.append(String.format("  [NÃO USADA] %s\n", entry.key));
                sb.append(String.format("              └─ Definida em: %s\n", entry.definedInFile));
            }
        }
        
        // Problemas de placeholder
        if (!placeholderIssues.isEmpty()) {
            sb.append("\n🟠 PROBLEMAS DE PLACEHOLDER (").append(placeholderIssues.size()).append("):\n");
            sb.append("─────────────────────────────────────────────────────────────────\n");
            for (PlaceholderIssue issue : placeholderIssues) {
                sb.append(String.format("  [PLACEHOLDER] %s\n", issue.key));
                sb.append(String.format("                └─ Esperados: %s\n", issue.expectedPlaceholders));
                sb.append(String.format("                └─ Encontrados: %s\n", issue.foundPlaceholders));
                sb.append(String.format("                └─ Faltando: %s\n", issue.getMissingPlaceholders()));
            }
        }
        
        // Avisos
        if (!warnings.isEmpty()) {
            sb.append("\n⚠️ AVISOS (").append(warnings.size()).append("):\n");
            sb.append("─────────────────────────────────────────────────────────────────\n");
            for (String warning : warnings) {
                sb.append("  ").append(warning).append("\n");
            }
        }
        
        if (!hasIssues()) {
            sb.append("\n✅ Nenhum problema encontrado! Todas as chaves de mensagem estão configuradas corretamente.\n");
        }
        
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * Gera YAML para as chaves faltantes (para auto-geração).
     *
     * @return String YAML com placeholders
     */
    public String generateYamlStubs() {
        if (missingKeys.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("# ============================================\n");
        sb.append("# MENSAGENS GERADAS AUTOMATICAMENTE\n");
        sb.append("# Gerado em: ").append(generatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("\n");
        sb.append("# TODO: Substitua os valores padrão pelas mensagens reais\n");
        sb.append("# ============================================\n\n");
        
        // Agrupar por prefixo (primeiro segmento da chave)
        Map<String, List<MissingKeyEntry>> groupedKeys = new LinkedHashMap<>();
        for (MissingKeyEntry entry : missingKeys) {
            String prefix = entry.key.contains(".") 
                    ? entry.key.substring(0, entry.key.indexOf('.'))
                    : "general";
            groupedKeys.computeIfAbsent(prefix, k -> new ArrayList<>()).add(entry);
        }
        
        for (Map.Entry<String, List<MissingKeyEntry>> group : groupedKeys.entrySet()) {
            sb.append("# --- ").append(group.getKey().toUpperCase()).append(" ---\n");
            for (MissingKeyEntry entry : group.getValue()) {
                String yamlKey = entry.key.replace(".", ":\n  ");
                sb.append("# TODO: Traduza esta mensagem\n");
                sb.append(yamlKey).append(": \"<red>TODO: ").append(entry.key).append("</red>\"\n\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Gera relatório em formato JSON.
     *
     * @return String JSON
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"generatedAt\": \"").append(generatedAt).append("\",\n");
        sb.append("  \"module\": ").append(moduleName != null ? "\"" + moduleName + "\"" : "null").append(",\n");
        sb.append("  \"totalIssues\": ").append(getTotalIssues()).append(",\n");
        
        // Missing keys
        sb.append("  \"missingKeys\": [\n");
        for (int i = 0; i < missingKeys.size(); i++) {
            MissingKeyEntry e = missingKeys.get(i);
            sb.append("    {\"key\": \"").append(e.key).append("\", ");
            sb.append("\"file\": \"").append(e.expectedFile).append("\", ");
            sb.append("\"class\": ").append(e.usedInClass != null ? "\"" + e.usedInClass + "\"" : "null").append(", ");
            sb.append("\"line\": ").append(e.usedAtLine).append("}");
            if (i < missingKeys.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");
        
        // Unused keys
        sb.append("  \"unusedKeys\": [\n");
        for (int i = 0; i < unusedKeys.size(); i++) {
            UnusedKeyEntry e = unusedKeys.get(i);
            sb.append("    {\"key\": \"").append(e.key).append("\", ");
            sb.append("\"file\": \"").append(e.definedInFile).append("\"}");
            if (i < unusedKeys.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        
        sb.append("}");
        return sb.toString();
    }
    
    private String getSimpleClassName(String fullName) {
        if (fullName == null) {
            return "Unknown";
        }
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }
    
    // ============================================
    // INNER CLASSES
    // ============================================
    
    /**
     * Entrada de chave faltante.
     */
    public record MissingKeyEntry(
            String key,
            String usedInClass,
            int usedAtLine,
            String expectedFile
    ) {}
    
    /**
     * Entrada de chave não utilizada.
     */
    public record UnusedKeyEntry(
            String key,
            String definedInFile
    ) {}
    
    /**
     * Problema de placeholder.
     */
    public static class PlaceholderIssue {
        private final String key;
        private final Set<String> expectedPlaceholders;
        private final Set<String> foundPlaceholders;
        
        public PlaceholderIssue(String key, Set<String> expected, Set<String> found) {
            this.key = key;
            this.expectedPlaceholders = expected != null ? expected : Collections.emptySet();
            this.foundPlaceholders = found != null ? found : Collections.emptySet();
        }
        
        public String getKey() {
            return key;
        }
        
        public Set<String> getExpectedPlaceholders() {
            return expectedPlaceholders;
        }
        
        public Set<String> getFoundPlaceholders() {
            return foundPlaceholders;
        }
        
        /**
         * Retorna placeholders esperados mas não encontrados.
         */
        public Set<String> getMissingPlaceholders() {
            Set<String> missing = new HashSet<>(expectedPlaceholders);
            missing.removeAll(foundPlaceholders);
            return missing;
        }
        
        /**
         * Retorna placeholders encontrados mas não esperados.
         */
        public Set<String> getExtraPlaceholders() {
            Set<String> extra = new HashSet<>(foundPlaceholders);
            extra.removeAll(expectedPlaceholders);
            return extra;
        }
    }
}
