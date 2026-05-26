package me.ray.midgard.core.i18n;

import me.ray.midgard.core.i18n.validation.MessageValidator;
import me.ray.midgard.core.i18n.validation.MissingKeyReport;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.text.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o sistema de mensagens unificado com recursos avançados.
 * <p>
 * Funcionalidades:
 * <ul>
 *     <li>Carregamento de mensagens organizadas por módulos</li>
 *     <li>Suporte a MessageKey tipado</li>
 *     <li>Fallback inteligente hierárquico</li>
 *     <li>Validação e detecção de chaves faltantes</li>
 *     <li>Hot-reload com diff de mudanças</li>
 *     <li>Modo debug para desenvolvimento</li>
 *     <li>Rastreamento de uso de chaves</li>
 * </ul>
 *
 * @since 2.0.0
 */
public class LanguageManager {

    private final JavaPlugin plugin;
    
    // Map<Key, Value> - Single language system
    private final Map<String, String> messages = new ConcurrentHashMap<>();
    private final Map<String, List<String>> messageLists = new ConcurrentHashMap<>();
    private final Map<String, Component> componentCache = new ConcurrentHashMap<>();
    
    // Rastreamento de origem das chaves
    private final Map<String, String> keySourceFiles = new ConcurrentHashMap<>();
    
    // Snapshot para detecção de mudanças no reload
    private Map<String, String> previousMessages = new HashMap<>();
    
    // Configurações
    private boolean debugMode = false;
    private boolean trackUsage = true;
    private boolean autoGenerateMissing = false;
    
    // Validador
    private MessageValidator validator;
    
    // Módulos conhecidos (pode ser expandido dinamicamente)
    private final Set<String> knownModules = new HashSet<>(Arrays.asList(
            "combat", "classes", "essentials", "item", "character", 
            "spells", "territory", "security", "performance", "mythicmobs"
    ));

    /**
     * Construtor do LanguageManager.
     *
     * @param plugin Instância do plugin.
     */
    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.validator = new MessageValidator(plugin, this);
    }

    /**
     * Carrega todas as mensagens da pasta 'messages'.
     * @param ignoredLocale Ignorado no novo sistema.
     */
    public void load(String ignoredLocale) {
        // Salvar snapshot para detecção de mudanças
        previousMessages = new HashMap<>(messages);
        
        messages.clear();
        messageLists.clear();
        componentCache.clear();
        keySourceFiles.clear();

        // MidgardLogger.info("Carregando sistema de mensagens avançado...");

        // 1. Load legacy/global messages
        File messagesFolder = new File(plugin.getDataFolder(), "messages");
        if (!messagesFolder.exists()) {
            messagesFolder.mkdirs();
        }
        scanAndLoad(messagesFolder, "", "messages/");

        // 2. Load module messages dynamically
        File modulesFolder = new File(plugin.getDataFolder(), "modules");
        if (modulesFolder.exists() && modulesFolder.isDirectory()) {
            File[] modulesDirs = modulesFolder.listFiles(File::isDirectory);
            if (modulesDirs != null) {
                for (File modDir : modulesDirs) {
                    String modName = modDir.getName();
                    knownModules.add(modName);
                    
                    // Check modules/{module}/messages.yml
                    File modMsgFile = new File(modDir, "messages.yml");
                    if (modMsgFile.exists()) {
                        loadYaml(modMsgFile, modName + ".", "modules/" + modName + "/messages.yml");
                    }
                    
                    // Check modules/{module}/lang/ folder (new structure)
                    File modLangFolder = new File(modDir, "lang");
                    if (modLangFolder.exists() && modLangFolder.isDirectory()) {
                        scanAndLoad(modLangFolder, modName + ".", "modules/" + modName + "/lang/");
                    }
                    
                    // Fallback: Check modules/{module}/messages/ folder (legacy)
                    File modMsgFolder = new File(modDir, "messages");
                    if (modMsgFolder.exists() && modMsgFolder.isDirectory()) {
                        scanAndLoad(modMsgFolder, modName + ".", "modules/" + modName + "/messages/");
                    }
                }
            }
        } else {
            MidgardLogger.warn("Pasta 'modules' não encontrada em %s. Nenhuma mensagem de módulo foi carregada.", modulesFolder.getPath());
        }
        
        // Log resultados
        // MidgardLogger.info("<green>✔ <white>Carregadas <aqua>%d <white>mensagens de <yellow>%d <white>módulos.", messages.size(), knownModules.size());
        
        // Detectar mudanças se for reload
        if (!previousMessages.isEmpty()) {
            logChanges();
            previousMessages = null; // Release memory after diff
        }
        
        // SEMPRE validar e expor mensagens faltantes no console
        validateAndExposeToConsole();
    }
    
    /**
     * Registra um novo módulo para carregamento de mensagens.
     *
     * @param moduleName Nome do módulo
     */
    public void registerModule(String moduleName) {
        if (moduleName != null && !moduleName.isEmpty()) {
            knownModules.add(moduleName.toLowerCase());
        }
    }
    
    /**
     * Loga as mudanças detectadas entre reloads.
     */
    private void logChanges() {
        int added = 0;
        int removed = 0;
        int modified = 0;
        
        // Chaves adicionadas
        for (String key : messages.keySet()) {
            if (!previousMessages.containsKey(key)) {
                added++;
                if (debugMode) {
                    MidgardLogger.debug("[+] Nova chave: %s", key);
                }
            } else if (!messages.get(key).equals(previousMessages.get(key))) {
                modified++;
                if (debugMode) {
                    MidgardLogger.debug("[~] Modificada: %s", key);
                }
            }
        }
        
        // Chaves removidas
        for (String key : previousMessages.keySet()) {
            if (!messages.containsKey(key)) {
                removed++;
                if (debugMode) {
                    MidgardLogger.debug("[-] Removida: %s", key);
                }
            }
        }
        
        if (added > 0 || removed > 0 || modified > 0) {
            MidgardLogger.info("Reload: +%d novas, -%d removidas, ~%d modificadas", added, removed, modified);
        }
    }
    
    /**
     * Executa validação e loga o resultado.
     */
    public void validateAndLog() {
        MissingKeyReport report = validator.validate();
        validator.logReport(report);
        
        if (autoGenerateMissing && !report.getMissingKeys().isEmpty()) {
            int generated = validator.generateMissingKeys(report, true);
            if (generated > 0) {
                MidgardLogger.info("§a✔ §fAuto-geradas §b%d §fchaves faltantes", generated);
            }
        }
    }
    
    /**
     * Valida mensagens e expõe no console quaisquer problemas encontrados.
     * Chamado automaticamente ao carregar mensagens.
     */
    private void validateAndExposeToConsole() {
        MissingKeyReport report = validator.validate();
        
        if (!report.hasIssues()) {
            // MidgardLogger.info("§a✔ §fTodas as chaves de mensagem estão configuradas corretamente!");
            return;
        }
        
        // Expor mensagens faltantes
        if (!report.getMissingKeys().isEmpty()) {
            MidgardLogger.warn("");
            MidgardLogger.warn("§c╔══════════════════════════════════════════════════════════════╗");
            MidgardLogger.warn("§c║          §4⚠ CHAVES DE MENSAGEM FALTANTES §c                     ║");
            MidgardLogger.warn("§c╚══════════════════════════════════════════════════════════════╝");
            MidgardLogger.warn("");
            
            for (MissingKeyReport.MissingKeyEntry entry : report.getMissingKeys()) {
                MidgardLogger.warn("  §c✖ §f%s", entry.key());
                MidgardLogger.warn("    §7└─ Arquivo esperado: §e%s", entry.expectedFile());
                if (entry.usedInClass() != null) {
                    String simpleClass = entry.usedInClass();
                    if (simpleClass.contains(".")) {
                        simpleClass = simpleClass.substring(simpleClass.lastIndexOf('.') + 1);
                    }
                    MidgardLogger.warn("    §7└─ Usado em: §b%s §7linha §e%d", simpleClass, entry.usedAtLine());
                }
            }
            
            MidgardLogger.warn("");
            MidgardLogger.warn("§e💡 Total: §c%d §echaves faltantes. Adicione-as nos arquivos YAML correspondentes.", 
                    report.getMissingKeys().size());
            MidgardLogger.warn("");
        }
        
        // Expor problemas de placeholder
        if (!report.getPlaceholderIssues().isEmpty()) {
            MidgardLogger.warn("§6⚠ Problemas de Placeholder (%d):", report.getPlaceholderIssues().size());
            for (MissingKeyReport.PlaceholderIssue issue : report.getPlaceholderIssues()) {
                MidgardLogger.warn("  §6✖ §f%s", issue.getKey());
                MidgardLogger.warn("    §7└─ Esperados: §e%s", issue.getExpectedPlaceholders());
                MidgardLogger.warn("    §7└─ Encontrados: §a%s", issue.getFoundPlaceholders());
                MidgardLogger.warn("    §7└─ Faltando: §c%s", issue.getMissingPlaceholders());
            }
        }
        
        // Expor avisos
        if (!report.getWarnings().isEmpty()) {
            MidgardLogger.warn("§e⚠ Avisos (%d):", report.getWarnings().size());
            for (String warning : report.getWarnings()) {
                MidgardLogger.warn("  §e• §7%s", warning);
            }
        }
        
        // Auto-gerar se habilitado
        if (autoGenerateMissing && !report.getMissingKeys().isEmpty()) {
            int generated = validator.generateMissingKeys(report, true);
            if (generated > 0) {
                MidgardLogger.info("§a✔ §fAuto-geradas §b%d §fchaves faltantes", generated);
            }
        }
    }
    
    /**
     * Escaneia todo o código fonte do projeto para encontrar mensagens faltantes.
     * Este método analisa TODOS os módulos e encontra chamadas a getMessage, 
     * getRawMessage, MessageKey.of, MessageKey.builder etc.
     *
     * @param projectRoot Diretório raiz do projeto (ex: C:/Users/.../MidgardRPG)
     */
    public void scanAndExposeAllMissingKeys(java.nio.file.Path projectRoot) {
        MissingKeyReport report = validator.scanAllModules(projectRoot);
        
        if (!report.hasIssues()) {
            MidgardLogger.info("§a✔ §fTodas as chaves de mensagem do projeto estão configuradas!");
            return;
        }
        
        // Agrupar por módulo/arquivo para melhor visualização
        Map<String, List<MissingKeyReport.MissingKeyEntry>> byExpectedFile = new LinkedHashMap<>();
        for (MissingKeyReport.MissingKeyEntry entry : report.getMissingKeys()) {
            byExpectedFile.computeIfAbsent(entry.expectedFile(), k -> new ArrayList<>()).add(entry);
        }
        
        MidgardLogger.warn("");
        MidgardLogger.warn("§c╔═══════════════════════════════════════════════════════════════════════════╗");
        MidgardLogger.warn("§c║              §4⚠ CHAVES FALTANTES ENCONTRADAS NO CÓDIGO §c                     ║");
        MidgardLogger.warn("§c╚═══════════════════════════════════════════════════════════════════════════╝");
        
        for (Map.Entry<String, List<MissingKeyReport.MissingKeyEntry>> fileEntry : byExpectedFile.entrySet()) {
            String expectedFile = fileEntry.getKey();
            List<MissingKeyReport.MissingKeyEntry> entries = fileEntry.getValue();
            
            MidgardLogger.warn("");
            MidgardLogger.warn("§e📁 %s §7(%d chaves faltantes)", expectedFile, entries.size());
            MidgardLogger.warn("§7─────────────────────────────────────────────────────────────────────");
            
            for (MissingKeyReport.MissingKeyEntry entry : entries) {
                MidgardLogger.warn("  §c✖ §f%s", entry.key());
                if (entry.usedInClass() != null) {
                    String simpleFile = entry.usedInClass();
                    if (simpleFile.contains("/")) {
                        simpleFile = simpleFile.substring(simpleFile.lastIndexOf('/') + 1);
                    }
                    MidgardLogger.warn("    §7└─ Usado em: §b%s §7linha §e%d", simpleFile, entry.usedAtLine());
                }
            }
        }
        
        MidgardLogger.warn("");
        MidgardLogger.warn("§e═══════════════════════════════════════════════════════════════════════════");
        MidgardLogger.warn("§e💡 RESUMO: §c%d §echaves faltantes em §c%d §earquivos YAML.", 
                report.getMissingKeys().size(), byExpectedFile.size());
        MidgardLogger.warn("§e💡 Adicione as chaves nos arquivos YAML correspondentes.");
        MidgardLogger.warn("§e═══════════════════════════════════════════════════════════════════════════");
        MidgardLogger.warn("");
    }
    
    /**
     * Obtém o validador de mensagens.
     *
     * @return O MessageValidator
     */
    public MessageValidator getValidator() {
        return validator;
    }

    private void scanAndLoad(File folder, String prefix, String relativePath) {
        if (folder == null || !folder.exists()) {
            return;
        }
        
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            try {
                if (file.isDirectory()) {
                    scanAndLoad(file, prefix + file.getName() + ".", 
                            relativePath + file.getName() + "/");
                } else if (file.getName().endsWith(".yml")) {
                    String fileName = file.getName().replace(".yml", "");
                    String filePrefix = prefix;
                    
                    // Se o arquivo se chama "messages", não adicionamos ao prefixo
                    // para manter compatibilidade com messages.yml na raiz
                    if (!fileName.equals("messages")) {
                        filePrefix += fileName + ".";
                    }
                    
                    loadYaml(file, filePrefix, relativePath + file.getName());
                }
            } catch (Exception e) {
                MidgardLogger.warn("Erro ao escanear arquivo de mensagem: %s", file.getName());
            }
        }
    }
    
    // Sobrecarga para compatibilidade
    // private void scanAndLoad(File folder, String prefix) {
    //    scanAndLoad(folder, prefix, "");
    // }

    private void loadYaml(File file, String prefix, String sourceFile) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            int loaded = 0;
            
            for (String key : config.getKeys(true)) {
                if (key == null) {
                    continue;
                }
                String fullKey = prefix + key;
                
                if (config.isString(key)) {
                    messages.put(fullKey, config.getString(key));
                    keySourceFiles.put(fullKey, sourceFile);
                    loaded++;
                } else if (config.isList(key)) {
                    messageLists.put(fullKey, config.getStringList(key));
                    keySourceFiles.put(fullKey, sourceFile);
                    loaded++;
                }
            }
            
            if (debugMode && loaded > 0) {
                MidgardLogger.debug("Carregadas %d chaves de %s", loaded, sourceFile);
            }
        } catch (Exception e) {
            MidgardLogger.error("Erro ao carregar arquivo de mensagem: %s", file.getName(), e);
        }
    }
    
    // Sobrecarga para compatibilidade
    // private void loadYaml(File file, String prefix) {
    //    loadYaml(file, prefix, file.getName());
    // }

    public boolean hasKey(String key) {
        if (key == null) {
            return false;
        }
        return messages.containsKey(key) || messageLists.containsKey(key);
    }
    
    /**
     * Obtém o arquivo fonte onde a chave está definida.
     *
     * @param key A chave da mensagem
     * @return Caminho relativo do arquivo ou null
     */
    public String getKeySourceFile(String key) {
        return keySourceFiles.get(key);
    }

    /**
     * Obtém a mensagem bruta (sem formatação) pela chave.
     *
     * @param key Chave da mensagem.
     * @return Mensagem bruta ou aviso de chave ausente.
     */
    public String getRawMessage(String key) {
        if (key == null) {
            return "<red>Internal Error: Null Key";
        }
        
        // Rastrear uso se habilitado
        if (trackUsage) {
            MessageRegistry.getInstance().trackUsage(key);
        }
        
        // Tentar encontrar a chave
        String message = messages.get(key);
        if (message != null) {
            if (debugMode) {
                MidgardLogger.debug("[MSG] %s → %s", key, getKeySourceFile(key));
            }
            return message;
        }
        
        // Fallback hierárquico: combat.mode.enabled_pvp → combat.mode.enabled → combat.generic
        String fallback = findFallback(key);
        if (fallback != null) {
            if (debugMode) {
                MidgardLogger.debug("[MSG-FALLBACK] %s → %s", key, fallback);
            }
            return messages.get(fallback);
        }
        
        // Chave não encontrada - logar aviso
        String expectedFile = inferExpectedFile(key);
        MidgardLogger.warn("§c[CHAVE FALTANTE] §f%s §7→ Esperada em: §e%s", key, expectedFile);
        
        return "<red>Chave não encontrada: " + key + "</red>";
    }
    
    /**
     * Obtém a mensagem bruta por MessageKey, respeitando fallbacks definidos.
     *
     * @param messageKey A MessageKey tipada
     * @return Mensagem bruta
     */
    public String getRawMessage(MessageKey messageKey) {
        if (messageKey == null) {
            return "<red>Internal Error: Null MessageKey</red>";
        }
        
        String key = messageKey.getKey();
        
        // Tentar a chave principal
        if (messages.containsKey(key)) {
            if (trackUsage) {
                MessageRegistry.getInstance().trackUsage(key);
            }
            return messages.get(key);
        }
        
        // Tentar fallback definido na MessageKey
        if (messageKey.hasFallback()) {
            String fallbackKey = messageKey.getFallbackKey();
            if (messages.containsKey(fallbackKey)) {
                if (debugMode) {
                    MidgardLogger.debug("[MSG-FALLBACK] %s → %s", key, fallbackKey);
                }
                return messages.get(fallbackKey);
            }
        }
        
        // Tentar fallback hierárquico automático
        String autoFallback = findFallback(key);
        if (autoFallback != null) {
            return messages.get(autoFallback);
        }
        
        // Usar valor padrão se definido
        if (messageKey.hasDefaultValue()) {
            return messageKey.getDefaultValue();
        }
        
        // Logar e retornar erro
        MidgardLogger.warn("[MISSING KEY] %s → Expected in: %s", key, messageKey.getExpectedFilePath());
        return "<red>Key not found: " + key + "</red>";
    }

    /**
     * Encontra um fallback hierárquico para a chave.
     * Ex: combat.mode.enabled_pvp → combat.mode.enabled → combat.generic → generic
     */
    private String findFallback(String key) {
        if (key == null || !key.contains(".")) {
            return null;
        }
        
        // Tentar remover o último segmento
        int lastDot = key.lastIndexOf('.');
        while (lastDot > 0) {
            String parent = key.substring(0, lastDot);
            
            // Tentar parent.generic
            String genericKey = parent + ".generic";
            if (messages.containsKey(genericKey)) {
                return genericKey;
            }
            
            // Tentar parent diretamente
            if (messages.containsKey(parent)) {
                return parent;
            }
            
            lastDot = parent.lastIndexOf('.');
        }
        
        // Tentar chave "generic" global
        if (messages.containsKey("generic")) {
            return "generic";
        }
        
        return null;
    }
    
    /**
     * Infere o arquivo esperado baseado na chave.
     */
    private String inferExpectedFile(String key) {
        if (key == null || !key.contains(".")) {
            return "lang/messages.yml";
        }
        String module = key.substring(0, key.indexOf('.'));
        if (knownModules.contains(module)) {
            return "modules/" + module + "/lang/messages.yml";
        }
        return "lang/" + module + ".yml";
    }

    /**
     * Deprecated: Player parameter is ignored.
     */
    @Deprecated
    public String getRawMessage(Player player, String key) {
        return getRawMessage(key);
    }

    /**
     * Obtém uma lista de mensagens pela chave.
     *
     * @param key Chave da lista.
     * @return Lista de mensagens ou lista vazia se não encontrada.
     */
    public List<String> getStringList(String key) {
        if (key == null) {
            return Collections.emptyList();
        }
        return messageLists.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Deprecated: Player parameter is ignored.
     */
    @Deprecated
    public List<String> getStringList(Player player, String key) {
        return getStringList(key);
    }

    /**
     * Obtém a mensagem formatada como Component.
     *
     * @param key Chave da mensagem.
     * @return Componente de texto formatado.
     */
    public Component getMessage(String key) {
        if (key == null) {
            return Component.text("Internal Error: Null Key");
        }
        
        String cacheKey = key;
        
        if (componentCache.containsKey(cacheKey)) {
            return componentCache.get(cacheKey);
        }

        Component comp = MessageUtils.parse(getRawMessage(key));
        componentCache.put(cacheKey, comp);
        return comp;
    }
    
    /**
     * Deprecated: Player parameter is ignored.
     */
    @Deprecated
    public Component getMessage(Player player, String key) {
        return getMessage(key);
    }

    /**
     * Obtém mensagem com substituição de placeholders simples.
     * Ex: getMessage("erro.dinheiro", "%quantia%", "100")
     */
    public Component getMessage(String key, String... placeholders) {
        if (key == null) {
            return Component.text("Internal Error: Null Key");
        }
        String raw = getRawMessage(key);
        
        if (placeholders != null) {
            for (int i = 0; i < placeholders.length; i += 2) {
                if (i + 1 < placeholders.length) {
                    String placeholder = placeholders[i];
                    String value = placeholders[i + 1];
                    if (placeholder != null && value != null) {
                        raw = raw.replace(placeholder, value);
                    }
                }
            }
        }
        return MessageUtils.parse(raw);
    }

    /**
     * Deprecated: Player parameter is ignored.
     */
    @Deprecated
    public Component getMessage(Player player, String key, String... placeholders) {
        return getMessage(key, placeholders);
    }

    /**
     * Obtém uma lista de mensagens formatadas como Component.
     *
     * @param key Chave da lista.
     * @return Lista de componentes.
     */
    public List<Component> getMessageList(String key) {
        List<String> rawList = getStringList(key);
        List<Component> componentList = new java.util.ArrayList<>();
        for (String line : rawList) {
            componentList.add(MessageUtils.parse(line));
        }
        return componentList;
    }

    /**
     * Deprecated: Player parameter is ignored.
     */
    @Deprecated
    public List<Component> getMessageList(Player player, String key) {
        return getMessageList(key);
    }

    /**
     * Obtém a mensagem formatada com placeholders substituídos.
     *
     * @param key Chave da mensagem.
     * @param placeholders Mapa de placeholders e valores.
     * @return Componente de texto formatado.
     */
    public Component getMessage(String key, Map<String, String> placeholders) {
        String raw = getRawMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return MessageUtils.parse(raw);
    }
    
    // ============================================
    // MESSAGEKEY SUPPORT
    // ============================================
    
    /**
     * Obtém mensagem por MessageKey tipada.
     *
     * @param messageKey A MessageKey
     * @return Component formatado
     */
    public Component getMessage(MessageKey messageKey) {
        if (messageKey == null) {
            return Component.text("Internal Error: Null MessageKey");
        }
        return MessageUtils.parse(getRawMessage(messageKey));
    }
    
    /**
     * Obtém mensagem por MessageKey com placeholders.
     *
     * @param messageKey A MessageKey
     * @param placeholders Os placeholders a substituir
     * @return Component formatado
     */
    public Component getMessage(MessageKey messageKey, Placeholder... placeholders) {
        if (messageKey == null) {
            return Component.text("Internal Error: Null MessageKey");
        }
        
        String raw = getRawMessage(messageKey);
        raw = Placeholder.applyAll(raw, placeholders);
        
        return MessageUtils.parse(raw);
    }
    
    /**
     * Obtém mensagem por MessageKey com mapa de placeholders.
     *
     * @param messageKey A MessageKey
     * @param placeholders Mapa de placeholders
     * @return Component formatado
     */
    public Component getMessage(MessageKey messageKey, Map<String, String> placeholders) {
        if (messageKey == null) {
            return Component.text("Internal Error: Null MessageKey");
        }
        
        String raw = getRawMessage(messageKey);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        
        return MessageUtils.parse(raw);
    }
    
    // ============================================
    // CONFIGURATION
    // ============================================
    
    /**
     * Habilita/desabilita modo debug.
     * No modo debug, cada acesso a mensagem é logado com o arquivo fonte.
     *
     * @param debug true para habilitar
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        MessageRegistry.getInstance().setDebugMode(debug);
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Habilita/desabilita rastreamento de uso.
     *
     * @param track true para habilitar
     */
    public void setTrackUsage(boolean track) {
        this.trackUsage = track;
    }
    
    public boolean isTrackUsage() {
        return trackUsage;
    }
    
    /**
     * Habilita/desabilita geração automática de chaves faltantes.
     *
     * @param auto true para habilitar
     */
    public void setAutoGenerateMissing(boolean auto) {
        this.autoGenerateMissing = auto;
    }
    
    public boolean isAutoGenerateMissing() {
        return autoGenerateMissing;
    }
    
    /**
     * Obtém o total de mensagens carregadas.
     *
     * @return Número de mensagens
     */
    public int getMessageCount() {
        return messages.size();
    }
    
    /**
     * Obtém o total de listas carregadas.
     *
     * @return Número de listas
     */
    public int getListCount() {
        return messageLists.size();
    }
    
    /**
     * Obtém todos os módulos conhecidos.
     *
     * @return Set de nomes de módulos
     */
    public Set<String> getKnownModules() {
        return Collections.unmodifiableSet(knownModules);
    }
    
    /**
     * Obtém todas as chaves carregadas.
     *
     * @return Set de chaves
     */
    public Set<String> getAllKeys() {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(messages.keySet());
        allKeys.addAll(messageLists.keySet());
        return allKeys;
    }
    
    /**
     * Limpa o cache de componentes.
     * Útil após modificar mensagens em runtime.
     */
    public void clearCache() {
        componentCache.clear();
    }
}
