package me.ray.midgard.core.i18n.validation;

import me.ray.midgard.core.i18n.LanguageManager;
import me.ray.midgard.core.i18n.MessageKey;
import me.ray.midgard.core.i18n.MessageRegistry;
import me.ray.midgard.core.debug.MidgardLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validador de mensagens que detecta chaves faltantes e gera relatórios.
 * <p>
 * Funcionalidades:
 * <ul>
 *     <li>Validação de chaves registradas vs YAMLs</li>
 *     <li>Detecção de chaves usadas em runtime não existentes</li>
 *     <li>Escaneamento de código fonte para encontrar usos</li>
 *     <li>Geração automática de stubs para chaves faltantes</li>
 *     <li>Verificação de placeholders</li>
 * </ul>
 *
 * @since 2.0.0
 */
public class MessageValidator {
    
    private final JavaPlugin plugin;
    private final LanguageManager languageManager;
    private final MessageRegistry registry;
    
    // Padrões para encontrar usos de mensagens no código
    private static final Pattern PATTERN_GET_MESSAGE = Pattern.compile(
            "getMessage\\s*\\(\\s*[\"']([^\"']+)[\"']"
    );
    private static final Pattern PATTERN_GET_RAW = Pattern.compile(
            "getRawMessage\\s*\\(\\s*[\"']([^\"']+)[\"']"
    );
    private static final Pattern PATTERN_MESSAGE_KEY = Pattern.compile(
            "MessageKey\\.of\\s*\\(\\s*[\"']([^\"']+)[\"']"
    );
    private static final Pattern PATTERN_PLACEHOLDER = Pattern.compile(
            "%([a-zA-Z_][a-zA-Z0-9_]*)%"
    );
    
    // ============================================
    // FILTROS DE FALSOS POSITIVOS
    // ============================================
    
    /**
     * Lista de chaves que devem ser ignoradas (exemplos, variáveis, etc).
     */
    private static final Set<String> IGNORED_KEYS = Set.of(
            // Palavras genéricas usadas como exemplos/variáveis
            "key", "chave", "message", "mensagem", "value", "valor",
            "text", "texto", "prefix", "prefixo", "suffix", "sufixo",
            "name", "nome", "description", "descricao", "title", "titulo",
            // Termos técnicos
            "error", "erro", "success", "sucesso", "warning", "aviso",
            "info", "debug", "trace", "test", "teste"
    );
    
    /**
     * Verifica se uma chave encontrada é válida ou um falso positivo.
     * 
     * @param key A chave a verificar
     * @param line A linha de código onde foi encontrada (para contexto)
     * @return true se é uma chave válida, false se é falso positivo
     */
    private boolean isValidMessageKey(String key, String line) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        // 1. Ignorar chaves na lista de exclusão
        if (IGNORED_KEYS.contains(key.toLowerCase())) {
            return false;
        }
        
        // 2. Ignorar chaves que terminam com '.' (são prefixos para concatenação)
        if (key.endsWith(".")) {
            return false;
        }
        
        // 3. Ignorar chaves muito curtas (menos de 3 caracteres ou sem '.')
        if (key.length() < 3) {
            return false;
        }
        
        // 4. Ignorar chaves que não contêm '.' (provavelmente variáveis)
        if (!key.contains(".")) {
            return false;
        }
        
        // 5. Ignorar chaves com caracteres inválidos
        if (key.contains(" ") || key.contains("$") || key.contains("{") || key.contains("}")) {
            return false;
        }
        
        // 6. Ignorar se a linha é um comentário
        String trimmedLine = line.trim();
        if (trimmedLine.startsWith("//") || trimmedLine.startsWith("*") || trimmedLine.startsWith("/*")) {
            return false;
        }
        
        // 7. Ignorar chaves que começam com caractere inválido
        char firstChar = key.charAt(0);
        if (!Character.isLetter(firstChar)) {
            return false;
        }
        
        return true;
    }
    
    public MessageValidator(JavaPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.registry = MessageRegistry.getInstance();
    }
    
    // ============================================
    // VALIDATION METHODS
    // ============================================
    
    /**
     * Executa validação completa e retorna um relatório.
     *
     * @return Relatório de validação
     */
    public MissingKeyReport validate() {
        MissingKeyReport report = new MissingKeyReport();
        
        // 1. Validar chaves registradas
        validateRegisteredKeys(report);
        
        // 2. Validar uso em runtime
        validateRuntimeUsage(report);
        
        // 3. Encontrar chaves não utilizadas
        findUnusedKeys(report);
        
        // 4. Validar placeholders
        validatePlaceholders(report);
        
        return report;
    }
    
    /**
     * Valida apenas um módulo específico.
     *
     * @param moduleName Nome do módulo
     * @return Relatório do módulo
     */
    public MissingKeyReport validateModule(String moduleName) {
        MissingKeyReport report = new MissingKeyReport(moduleName);
        
        Set<MessageKey> moduleKeys = registry.getKeysByModule(moduleName);
        for (MessageKey key : moduleKeys) {
            if (!languageManager.hasKey(key.getKey())) {
                report.addMissingKey(key.getKey(), key.getExpectedFilePath());
            }
        }
        
        return report;
    }
    
    /**
     * Valida chaves registradas contra os YAMLs carregados.
     */
    private void validateRegisteredKeys(MissingKeyReport report) {
        for (MessageKey key : registry.getAllKeys()) {
            String keyStr = key.getKey();
            
            if (!languageManager.hasKey(keyStr)) {
                // Verificar fallback
                if (key.hasFallback() && languageManager.hasKey(key.getFallbackKey())) {
                    report.addWarning(String.format(
                            "Chave '%s' não encontrada, usando fallback '%s'", 
                            keyStr, key.getFallbackKey()));
                } else {
                    report.addMissingKey(
                            keyStr,
                            key.getSourceClass(),
                            key.getSourceLine(),
                            key.getExpectedFilePath()
                    );
                }
            }
        }
    }
    
    /**
     * Valida chaves usadas em runtime.
     */
    private void validateRuntimeUsage(MissingKeyReport report) {
        Map<String, MessageRegistry.KeyUsage> usage = registry.getRuntimeUsage();
        
        for (Map.Entry<String, MessageRegistry.KeyUsage> entry : usage.entrySet()) {
            String key = entry.getKey();
            MessageRegistry.KeyUsage keyUsage = entry.getValue();
            
            if (!languageManager.hasKey(key)) {
                String sourceClass = null;
                int sourceLine = 0;
                
                if (!keyUsage.getLocations().isEmpty()) {
                    MessageRegistry.UsageLocation loc = keyUsage.getLocations().get(0);
                    sourceClass = loc.getClassName();
                    sourceLine = loc.getLine();
                }
                
                // Inferir arquivo esperado
                String expectedFile = inferExpectedFile(key);
                
                report.addMissingKey(key, sourceClass, sourceLine, expectedFile);
            }
        }
    }
    
    /**
     * Encontra chaves definidas nos YAMLs mas nunca utilizadas.
     */
    private void findUnusedKeys(MissingKeyReport report) {
        Set<String> unusedKeys = registry.getUnusedKeys();
        
        for (String key : unusedKeys) {
            String file = inferExpectedFile(key);
            report.addUnusedKey(key, file);
        }
    }
    
    /**
     * Valida se os placeholders esperados estão presentes nas mensagens.
     */
    private void validatePlaceholders(MissingKeyReport report) {
        for (MessageKey key : registry.getAllKeys()) {
            if (key.getExpectedPlaceholders().isEmpty()) {
                continue;
            }
            
            if (!languageManager.hasKey(key.getKey())) {
                continue;
            }
            
            String rawMessage = languageManager.getRawMessage(key.getKey());
            Set<String> foundPlaceholders = extractPlaceholders(rawMessage);
            Set<String> expected = key.getExpectedPlaceholders();
            
            // Verificar se todos os esperados estão presentes
            if (!foundPlaceholders.containsAll(expected)) {
                report.addPlaceholderIssue(key.getKey(), expected, foundPlaceholders);
            }
        }
    }
    
    /**
     * Extrai placeholders de uma mensagem.
     */
    private Set<String> extractPlaceholders(String message) {
        Set<String> placeholders = new HashSet<>();
        if (message == null) {
            return placeholders;
        }
        
        Matcher matcher = PATTERN_PLACEHOLDER.matcher(message);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
        
        return placeholders;
    }
    
    /**
     * Infere o arquivo YAML esperado baseado na chave.
     */
    private String inferExpectedFile(String key) {
        if (key == null || !key.contains(".")) {
            return "lang/messages.yml";
        }
        
        String module = key.substring(0, key.indexOf('.'));
        return "modules/" + module + "/lang/messages.yml";
    }
    
    // ============================================
    // SOURCE CODE SCANNING
    // ============================================
    
    /**
     * Escaneia o código fonte para encontrar todos os usos de mensagens.
     * Útil para análise offline/desenvolvimento.
     *
     * @param sourceDir Diretório raiz do código fonte
     * @return Mapa de arquivo → chaves usadas
     */
    public Map<String, Set<String>> scanSourceCode(Path sourceDir) {
        Map<String, Set<String>> usageMap = new HashMap<>();
        
        try {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        Set<String> keys = scanJavaFile(file);
                        if (!keys.isEmpty()) {
                            usageMap.put(file.toString(), keys);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            MidgardLogger.error("Erro ao escanear código fonte: %s", e.getMessage());
        }
        
        return usageMap;
    }
    
    /**
     * Escaneia um arquivo Java para encontrar usos de mensagens.
     */
    private Set<String> scanJavaFile(Path file) {
        Set<String> keys = new HashSet<>();
        
        try {
            List<String> lines = Files.readAllLines(file);
            
            for (String line : lines) {
                // Procurar getMessage("key")
                Matcher matcher1 = PATTERN_GET_MESSAGE.matcher(line);
                while (matcher1.find()) {
                    String key = matcher1.group(1);
                    if (isValidMessageKey(key, line)) {
                        keys.add(key);
                    }
                }
                
                // Procurar getRawMessage("key")
                Matcher matcher2 = PATTERN_GET_RAW.matcher(line);
                while (matcher2.find()) {
                    String key = matcher2.group(1);
                    if (isValidMessageKey(key, line)) {
                        keys.add(key);
                    }
                }
                
                // Procurar MessageKey.of("key")
                Matcher matcher3 = PATTERN_MESSAGE_KEY.matcher(line);
                while (matcher3.find()) {
                    String key = matcher3.group(1);
                    if (isValidMessageKey(key, line)) {
                        keys.add(key);
                    }
                }
            }
            
        } catch (IOException e) {
            MidgardLogger.warn("Erro ao ler arquivo %s: %s", file, e.getMessage());
        }
        
        return keys;
    }
    
    // ============================================
    // AUTO-GENERATION
    // ============================================
    
    /**
     * Gera automaticamente stubs para chaves faltantes nos arquivos YAML.
     *
     * @param report O relatório com chaves faltantes
     * @param autoCreate Se true, cria os arquivos; se false, apenas simula
     * @return Número de chaves geradas
     */
    public int generateMissingKeys(MissingKeyReport report, boolean autoCreate) {
        if (report.getMissingKeys().isEmpty()) {
            return 0;
        }
        
        // Agrupar por arquivo
        Map<String, List<MissingKeyReport.MissingKeyEntry>> byFile = new HashMap<>();
        for (MissingKeyReport.MissingKeyEntry entry : report.getMissingKeys()) {
            byFile.computeIfAbsent(entry.expectedFile(), k -> new ArrayList<>()).add(entry);
        }
        
        int generated = 0;
        
        for (Map.Entry<String, List<MissingKeyReport.MissingKeyEntry>> fileEntry : byFile.entrySet()) {
            String filePath = fileEntry.getKey();
            List<MissingKeyReport.MissingKeyEntry> entries = fileEntry.getValue();
            
            if (autoCreate) {
                generated += appendToYaml(filePath, entries);
            } else {
                // Apenas logar o que seria gerado
                MidgardLogger.info("[SIMULAÇÃO] Geraria %d chaves em %s", entries.size(), filePath);
                for (MissingKeyReport.MissingKeyEntry e : entries) {
                    MidgardLogger.info("  • %s", e.key());
                }
                generated += entries.size();
            }
        }
        
        return generated;
    }
    
    /**
     * Adiciona chaves faltantes a um arquivo YAML.
     */
    private int appendToYaml(String relativePath, List<MissingKeyReport.MissingKeyEntry> entries) {
        File file = new File(plugin.getDataFolder(), relativePath);
        
        try {
            // Garantir que o diretório existe
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            
            // Carregar configuração existente ou criar nova
            YamlConfiguration config = file.exists() 
                    ? YamlConfiguration.loadConfiguration(file)
                    : new YamlConfiguration();
            
            // Adicionar header se novo arquivo
            if (!file.exists()) {
                config.options().setHeader(java.util.Collections.singletonList(
                        "Arquivo de mensagens gerado automaticamente\n" +
                        "TODO: Substitua os valores padrão pelas mensagens reais"
                ));
            }
            
            int added = 0;
            for (MissingKeyReport.MissingKeyEntry entry : entries) {
                String key = entry.key();
                
                // Remover prefixo do módulo se já estiver no path
                String yamlKey = key;
                String module = inferModuleFromPath(relativePath);
                if (module != null && key.startsWith(module + ".")) {
                    yamlKey = key.substring(module.length() + 1);
                }
                
                if (!config.contains(yamlKey)) {
                    String defaultValue = String.format("<red>TODO: %s</red>", key);
                    config.set(yamlKey, defaultValue);
                    config.setComments(yamlKey, List.of("TODO: Traduza esta mensagem"));
                    added++;
                }
            }
            
            if (added > 0) {
                config.save(file);
                MidgardLogger.info("§a✔ §fGeradas §b%d §fchaves em §e%s", added, relativePath);
            }
            
            return added;
            
        } catch (IOException e) {
            MidgardLogger.error("§cErro ao salvar YAML §e%s§c: %s", relativePath, e.getMessage());
            return 0;
        }
    }
    
    private String inferModuleFromPath(String path) {
        // modules/combat/messages/messages.yml → combat
        if (path.startsWith("modules/") && path.contains("/")) {
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return null;
    }
    
    // ============================================
    // FULL PROJECT SCAN
    // ============================================
    
    /**
     * Escaneia TODOS os módulos do projeto e retorna um relatório completo
     * de chaves usadas no código mas faltantes nos YAMLs.
     *
     * @param projectRoot Diretório raiz do projeto (onde está o build.gradle)
     * @return Relatório com todas as chaves faltantes
     */
    public MissingKeyReport scanAllModules(Path projectRoot) {
        MissingKeyReport report = new MissingKeyReport();
        
        MidgardLogger.info("");
        MidgardLogger.info("§e⚡ Escaneando código fonte de todos os módulos...");
        MidgardLogger.info("");
        
        // Encontrar todos os diretórios de módulos
        List<Path> modulePaths = findModulePaths(projectRoot);
        
        int totalKeys = 0;
        int totalMissing = 0;
        
        for (Path modulePath : modulePaths) {
            String moduleName = modulePath.getFileName().toString();
            Path srcPath = modulePath.resolve("src/main/java");
            
            if (!Files.exists(srcPath)) {
                continue;
            }
            
            MidgardLogger.info("  §7Escaneando módulo: §b%s", moduleName);
            
            // Escanear todos os arquivos Java do módulo
            Map<Path, Set<SourceKeyUsage>> moduleUsages = scanModuleSource(srcPath);
            
            for (Map.Entry<Path, Set<SourceKeyUsage>> entry : moduleUsages.entrySet()) {
                Path javaFile = entry.getKey();
                Set<SourceKeyUsage> usages = entry.getValue();
                
                for (SourceKeyUsage usage : usages) {
                    totalKeys++;
                    
                    // Verificar se a chave existe
                    if (!languageManager.hasKey(usage.key)) {
                        totalMissing++;
                        
                        String expectedFile = inferExpectedFile(usage.key);
                        String relativePath = projectRoot.relativize(javaFile).toString().replace("\\", "/");
                        
                        report.addMissingKey(
                                usage.key,
                                relativePath,
                                usage.line,
                                expectedFile
                        );
                    }
                }
            }
        }
        
        MidgardLogger.info("");
        MidgardLogger.info("§e⚡ Escaneamento concluído:");
        MidgardLogger.info("   §7Total de chaves encontradas: §b%d", totalKeys);
        MidgardLogger.info("   §7Chaves faltantes: §c%d", totalMissing);
        MidgardLogger.info("");
        
        return report;
    }
    
    /**
     * Encontra todos os caminhos de módulos no projeto.
     */
    private List<Path> findModulePaths(Path projectRoot) {
        List<Path> modules = new ArrayList<>();
        
        // midgard-core
        Path corePath = projectRoot.resolve("midgard-core");
        if (Files.exists(corePath)) {
            modules.add(corePath);
        }
        
        // midgard-loader
        Path loaderPath = projectRoot.resolve("midgard-loader");
        if (Files.exists(loaderPath)) {
            modules.add(loaderPath);
        }
        
        // midgard-nms/api e v1_21
        Path nmsPath = projectRoot.resolve("midgard-nms");
        if (Files.exists(nmsPath)) {
            try {
                Files.list(nmsPath)
                        .filter(Files::isDirectory)
                        .filter(p -> Files.exists(p.resolve("src")))
                        .forEach(modules::add);
            } catch (IOException e) {
                MidgardLogger.warn("Erro ao listar módulos NMS: %s", e.getMessage());
            }
        }
        
        // midgard-modules/*
        Path modulesPath = projectRoot.resolve("midgard-modules");
        if (Files.exists(modulesPath)) {
            try {
                Files.list(modulesPath)
                        .filter(Files::isDirectory)
                        .filter(p -> Files.exists(p.resolve("src")))
                        .forEach(modules::add);
            } catch (IOException e) {
                MidgardLogger.warn("Erro ao listar módulos: %s", e.getMessage());
            }
        }
        
        return modules;
    }
    
    /**
     * Escaneia o código fonte de um módulo e retorna todos os usos de mensagens.
     */
    private Map<Path, Set<SourceKeyUsage>> scanModuleSource(Path srcPath) {
        Map<Path, Set<SourceKeyUsage>> usageMap = new HashMap<>();
        
        try {
            Files.walkFileTree(srcPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        Set<SourceKeyUsage> usages = scanJavaFileWithLineNumbers(file);
                        if (!usages.isEmpty()) {
                            usageMap.put(file, usages);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            MidgardLogger.warn("Erro ao escanear diretório: %s", srcPath);
        }
        
        return usageMap;
    }
    
    /**
     * Escaneia um arquivo Java e retorna usos de mensagens com números de linha.
     */
    private Set<SourceKeyUsage> scanJavaFileWithLineNumbers(Path file) {
        Set<SourceKeyUsage> usages = new HashSet<>();
        
        try {
            List<String> lines = Files.readAllLines(file);
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lineNumber = i + 1;
                
                // Procurar getMessage("key")
                Matcher matcher1 = PATTERN_GET_MESSAGE.matcher(line);
                while (matcher1.find()) {
                    String key = matcher1.group(1);
                    if (isValidMessageKey(key, line)) {
                        usages.add(new SourceKeyUsage(key, lineNumber));
                    }
                }
                
                // Procurar getRawMessage("key")
                Matcher matcher2 = PATTERN_GET_RAW.matcher(line);
                while (matcher2.find()) {
                    String key = matcher2.group(1);
                    if (isValidMessageKey(key, line)) {
                        usages.add(new SourceKeyUsage(key, lineNumber));
                    }
                }
                
                // Procurar MessageKey.of("key") e MessageKey.builder("key")
                Matcher matcher3 = PATTERN_MESSAGE_KEY.matcher(line);
                while (matcher3.find()) {
                    String key = matcher3.group(1);
                    if (isValidMessageKey(key, line)) {
                        usages.add(new SourceKeyUsage(key, lineNumber));
                    }
                }
            }
            
        } catch (IOException e) {
            MidgardLogger.warn("Erro ao ler arquivo: %s", file);
        }
        
        return usages;
    }
    
    /**
     * Registro de uso de uma chave no código fonte.
     */
    private record SourceKeyUsage(String key, int line) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SourceKeyUsage that = (SourceKeyUsage) o;
            return key.equals(that.key);
        }
        
        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
    
    // ============================================
    // LOGGING METHODS
    // ============================================
    
    /**
     * Loga o relatório no console.
     *
     * @param report O relatório a logar
     */
    public void logReport(MissingKeyReport report) {
        if (!report.hasIssues()) {
            MidgardLogger.info("§a✔ §fValidação de mensagens: Nenhum problema encontrado!");
            return;
        }
        
        String formatted = report.toConsoleFormat();
        for (String line : formatted.split("\n")) {
            if (line.contains("MISSING")) {
                MidgardLogger.warn(line);
            } else if (line.contains("UNUSED") || line.contains("WARNING")) {
                MidgardLogger.info(line);
            } else {
                MidgardLogger.info(line);
            }
        }
    }
    
    /**
     * Salva o relatório em um arquivo.
     *
     * @param report O relatório
     * @param fileName Nome do arquivo
     */
    public void saveReport(MissingKeyReport report, String fileName) {
        File file = new File(plugin.getDataFolder(), "reports/" + fileName);
        file.getParentFile().mkdirs();
        
        try {
            if (fileName.endsWith(".json")) {
                Files.writeString(file.toPath(), report.toJson());
            } else {
                Files.writeString(file.toPath(), report.toConsoleFormat());
            }
            MidgardLogger.info("§a✔ §fRelatório salvo em: §b%s", file.getPath());
        } catch (IOException e) {
            MidgardLogger.error("§cErro ao salvar relatório: %s", e.getMessage());
        }
    }
}
