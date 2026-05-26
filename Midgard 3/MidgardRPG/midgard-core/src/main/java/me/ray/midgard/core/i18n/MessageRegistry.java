package me.ray.midgard.core.i18n;

import me.ray.midgard.core.debug.MidgardLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry central que rastreia todas as MessageKeys usadas no sistema.
 * <p>
 * Funcionalidades:
 * <ul>
 *     <li>Registro automático de chaves usadas</li>
 *     <li>Rastreamento de onde cada chave é utilizada</li>
 *     <li>Detecção de chaves duplicadas ou conflitantes</li>
 *     <li>Estatísticas de uso</li>
 * </ul>
 *
 * @since 2.0.0
 */
public class MessageRegistry {
    
    private static final MessageRegistry INSTANCE = new MessageRegistry();
    
    // Chaves registradas por módulo
    private final Map<String, Set<MessageKey>> keysByModule = new ConcurrentHashMap<>();
    
    // Todas as chaves registradas
    private final Map<String, MessageKey> allKeys = new ConcurrentHashMap<>();
    
    // Chaves usadas em runtime (para detectar chaves não declaradas)
    private final Map<String, KeyUsage> runtimeUsage = new ConcurrentHashMap<>();
    
    // Modo debug
    private boolean debugMode = false;
    
    private MessageRegistry() {}
    
    public static MessageRegistry getInstance() {
        return INSTANCE;
    }
    
    // ============================================
    // REGISTRATION METHODS
    // ============================================
    
    /**
     * Registra uma MessageKey no registry.
     *
     * @param key A chave a registrar
     * @return A mesma chave (para encadeamento)
     */
    public MessageKey register(MessageKey key) {
        if (key == null || key.getKey() == null) {
            MidgardLogger.warn("Tentativa de registrar MessageKey nula");
            return key;
        }
        
        String keyStr = key.getKey();
        
        // Verificar duplicatas
        if (allKeys.containsKey(keyStr)) {
            MessageKey existing = allKeys.get(keyStr);
            if (!existing.equals(key) && debugMode) {
                MidgardLogger.warn("[MessageRegistry] Chave duplicada: %s", keyStr);
            }
        }
        
        allKeys.put(keyStr, key);
        
        // Agrupar por módulo
        String module = key.inferModule();
        if (module != null) {
            keysByModule.computeIfAbsent(module, k -> ConcurrentHashMap.newKeySet()).add(key);
        }
        
        if (debugMode) {
            MidgardLogger.debug("[MessageRegistry] Registrada: %s", keyStr);
        }
        
        return key;
    }
    
    /**
     * Registra múltiplas chaves de uma vez.
     *
     * @param keys As chaves a registrar
     */
    public void registerAll(MessageKey... keys) {
        if (keys != null) {
            for (MessageKey key : keys) {
                register(key);
            }
        }
    }
    
    /**
     * Registra uso em runtime de uma chave string.
     * Usado para rastrear chaves usadas dinamicamente.
     *
     * @param key A chave string usada
     * @param sourceClass Classe de origem
     * @param sourceLine Linha de origem
     */
    public void trackUsage(String key, String sourceClass, int sourceLine) {
        if (key == null) {
            return;
        }
        
        runtimeUsage.computeIfAbsent(key, k -> new KeyUsage(k))
                .addUsage(sourceClass, sourceLine);
        
        if (debugMode) {
            MidgardLogger.debug("[MessageRegistry] Uso: %s em %s:%d", key, sourceClass, sourceLine);
        }
    }
    
    /**
     * Registra uso sem informação de origem.
     *
     * @param key A chave usada
     */
    public void trackUsage(String key) {
        if (key == null) {
            return;
        }
        
        // Tentar capturar stack trace
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String sourceClass = "Unknown";
        int sourceLine = 0;
        
        // Pular frames internos
        for (int i = 2; i < stack.length; i++) {
            String className = stack[i].getClassName();
            if (!className.startsWith("me.ray.midgard.core.i18n.")) {
                sourceClass = className;
                sourceLine = stack[i].getLineNumber();
                break;
            }
        }
        
        trackUsage(key, sourceClass, sourceLine);
    }
    
    // ============================================
    // QUERY METHODS
    // ============================================
    
    /**
     * Verifica se uma chave está registrada.
     *
     * @param key A chave a verificar
     * @return true se registrada
     */
    public boolean isRegistered(String key) {
        return allKeys.containsKey(key);
    }
    
    /**
     * Obtém uma MessageKey registrada.
     *
     * @param key A chave string
     * @return A MessageKey ou null
     */
    public MessageKey get(String key) {
        return allKeys.get(key);
    }
    
    /**
     * Obtém todas as chaves registradas.
     *
     * @return Coleção imutável de chaves
     */
    public Collection<MessageKey> getAllKeys() {
        return Collections.unmodifiableCollection(allKeys.values());
    }
    
    /**
     * Obtém chaves por módulo.
     *
     * @param module Nome do módulo
     * @return Set de chaves do módulo
     */
    public Set<MessageKey> getKeysByModule(String module) {
        return keysByModule.getOrDefault(module, Collections.emptySet());
    }
    
    /**
     * Obtém todos os módulos registrados.
     *
     * @return Set de nomes de módulos
     */
    public Set<String> getModules() {
        return Collections.unmodifiableSet(keysByModule.keySet());
    }
    
    /**
     * Obtém todas as chaves usadas em runtime.
     *
     * @return Map de chave → uso
     */
    public Map<String, KeyUsage> getRuntimeUsage() {
        return Collections.unmodifiableMap(runtimeUsage);
    }
    
    /**
     * Obtém chaves usadas mas não registradas.
     *
     * @return Set de chaves não registradas
     */
    public Set<String> getUnregisteredKeys() {
        Set<String> unregistered = new HashSet<>();
        for (String key : runtimeUsage.keySet()) {
            if (!allKeys.containsKey(key)) {
                unregistered.add(key);
            }
        }
        return unregistered;
    }
    
    /**
     * Obtém chaves registradas mas nunca usadas.
     *
     * @return Set de chaves não utilizadas
     */
    public Set<String> getUnusedKeys() {
        Set<String> unused = new HashSet<>(allKeys.keySet());
        unused.removeAll(runtimeUsage.keySet());
        return unused;
    }
    
    // ============================================
    // UTILITY METHODS
    // ============================================
    
    /**
     * Limpa todos os registros.
     */
    public void clear() {
        allKeys.clear();
        keysByModule.clear();
        runtimeUsage.clear();
    }
    
    /**
     * Limpa apenas os registros de uso em runtime.
     */
    public void clearUsage() {
        runtimeUsage.clear();
    }
    
    /**
     * Habilita/desabilita modo debug.
     *
     * @param debug true para habilitar
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Retorna estatísticas do registry.
     *
     * @return String com estatísticas
     */
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MessageRegistry Stats ===\n");
        sb.append("Total Keys Registered: ").append(allKeys.size()).append("\n");
        sb.append("Modules: ").append(keysByModule.size()).append("\n");
        sb.append("Runtime Usage Tracked: ").append(runtimeUsage.size()).append("\n");
        sb.append("Unregistered Keys Used: ").append(getUnregisteredKeys().size()).append("\n");
        sb.append("Unused Registered Keys: ").append(getUnusedKeys().size()).append("\n");
        
        sb.append("\nKeys per Module:\n");
        for (Map.Entry<String, Set<MessageKey>> entry : keysByModule.entrySet()) {
            sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue().size()).append("\n");
        }
        
        return sb.toString();
    }
    
    // ============================================
    // INNER CLASSES
    // ============================================
    
    /**
     * Representa o uso de uma chave em runtime.
     */
    public static class KeyUsage {
        private final String key;
        private final List<UsageLocation> locations = new ArrayList<>();
        private int usageCount = 0;
        
        public KeyUsage(String key) {
            this.key = key;
        }
        
        public void addUsage(String sourceClass, int line) {
            usageCount++;
            
            // Limitar locais armazenados para economizar memória
            if (locations.size() < 10) {
                locations.add(new UsageLocation(sourceClass, line));
            }
        }
        
        public String getKey() {
            return key;
        }
        
        public int getUsageCount() {
            return usageCount;
        }
        
        public List<UsageLocation> getLocations() {
            return Collections.unmodifiableList(locations);
        }
        
        @Override
        public String toString() {
            return String.format("KeyUsage{key='%s', count=%d, locations=%d}", 
                    key, usageCount, locations.size());
        }
    }
    
    /**
     * Representa um local de uso no código.
     */
    public static class UsageLocation {
        private final String className;
        private final int line;
        
        public UsageLocation(String className, int line) {
            this.className = className;
            this.line = line;
        }
        
        public String getClassName() {
            return className;
        }
        
        public int getLine() {
            return line;
        }
        
        public String getSimpleClassName() {
            if (className == null) {
                return "Unknown";
            }
            int lastDot = className.lastIndexOf('.');
            return lastDot >= 0 ? className.substring(lastDot + 1) : className;
        }
        
        @Override
        public String toString() {
            return getSimpleClassName() + ":" + line;
        }
    }
}
