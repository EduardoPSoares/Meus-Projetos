package me.ray.midgard.modules.performance.spark;

import me.ray.midgard.modules.performance.spark.MidgardAnalyzer.*;
import me.ray.midgard.modules.performance.spark.SparkPerformanceManager.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import me.ray.midgard.modules.performance.PerformanceModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerador de relatórios de performance detalhados.
 * Combina dados do Spark com análise do MidgardRPG.
 */
public class PerformanceReport {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Gera relatório completo de performance.
     */
    public static FullReport generateFullReport() {
        SparkPerformanceManager sparkManager = SparkPerformanceManager.getInstance();
        MidgardAnalyzer analyzer = MidgardAnalyzer.getInstance();

        ServerMetrics metrics = sparkManager != null ? sparkManager.getMetrics() : ServerMetrics.unavailable();
        HealthDiagnosis diagnosis = sparkManager != null ? sparkManager.diagnose() : null;
        AnalysisSnapshot analysis = analyzer != null ? analyzer.analyze() : null;

        List<Issue> issues = collectIssues(metrics, diagnosis, analysis);
        List<Recommendation> recommendations = generateRecommendations(issues, metrics, analysis);

        return new FullReport(
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            SparkPerformanceManager.isAvailable(),
            metrics,
            diagnosis,
            analysis,
            issues,
            recommendations,
            calculateOverallScore(metrics, diagnosis, issues)
        );
    }

    /**
     * Gera relatório rápido (apenas métricas essenciais).
     */
    public static QuickReport generateQuickReport() {
        SparkPerformanceManager sparkManager = SparkPerformanceManager.getInstance();
        
        if (sparkManager == null || !SparkPerformanceManager.isAvailable()) {
            return QuickReport.unavailable();
        }

        ServerMetrics metrics = sparkManager.getMetrics();
        HealthDiagnosis diagnosis = sparkManager.diagnose();

        return new QuickReport(
            metrics.tps().last5s(),
            metrics.mspt().available() ? metrics.mspt().last10s().median() : -1,
            metrics.memory().usedPercent(),
            metrics.cpu().available() ? metrics.cpu().process().seconds10() * 100 : -1,
            diagnosis.overallHealth(),
            countCriticalIssues(diagnosis),
            LocalDateTime.now().format(TIMESTAMP_FORMAT)
        );
    }

    // ========== I18N HELPER ==========

    private static String msg(String key, String fallback) {
        PerformanceModule mod = PerformanceModule.getInstance();
        if (mod != null) {
            String val = mod.getMessage(key);
            if (val != null && !val.isEmpty() && !val.equals(key)) {
                return val;
            }
        }
        return fallback;
    }

    // ========== COLETA DE PROBLEMAS ==========

    private static List<Issue> collectIssues(ServerMetrics metrics, HealthDiagnosis diagnosis, AnalysisSnapshot analysis) {
        List<Issue> issues = new ArrayList<>();

        // Issues de métricas do servidor
        if (diagnosis != null) {
            if (diagnosis.tps().level().ordinal() >= HealthLevel.WARNING.ordinal()) {
                issues.add(new Issue(
                    IssueCategory.TPS,
                    diagnosis.tps().level(),
                    msg("performance.report.issue.tps-low.title", "TPS Baixo"),
                    diagnosis.tps().message(),
                    msg("performance.report.issue.tps-low.suggestion", "Verifique entidades, redstone, hoppers e plugins pesados")
                ));
            }

            if (diagnosis.mspt().level().ordinal() >= HealthLevel.WARNING.ordinal()) {
                issues.add(new Issue(
                    IssueCategory.MSPT,
                    diagnosis.mspt().level(),
                    msg("performance.report.issue.mspt-high.title", "MSPT Alto"),
                    diagnosis.mspt().message(),
                    msg("performance.report.issue.mspt-high.suggestion", "Ticks estão demorando demais - identifique operações síncronas pesadas")
                ));
            }

            if (diagnosis.memory().level().ordinal() >= HealthLevel.WARNING.ordinal()) {
                issues.add(new Issue(
                    IssueCategory.MEMORY,
                    diagnosis.memory().level(),
                    msg("performance.report.issue.memory-high.title", "Memória Alta"),
                    diagnosis.memory().message(),
                    msg("performance.report.issue.memory-high.suggestion", "Considere aumentar heap ou verificar memory leaks")
                ));
            }

            if (diagnosis.cpu().level().ordinal() >= HealthLevel.WARNING.ordinal()) {
                issues.add(new Issue(
                    IssueCategory.CPU,
                    diagnosis.cpu().level(),
                    msg("performance.report.issue.cpu-high.title", "CPU Alta"),
                    diagnosis.cpu().message(),
                    msg("performance.report.issue.cpu-high.suggestion", "Identifique processos intensivos ou loops infinitos")
                ));
            }

            if (diagnosis.gc().level().ordinal() >= HealthLevel.WARNING.ordinal()) {
                issues.add(new Issue(
                    IssueCategory.GC,
                    diagnosis.gc().level(),
                    msg("performance.report.issue.gc-frequent.title", "GC Frequente"),
                    diagnosis.gc().message(),
                    msg("performance.report.issue.gc-frequent.suggestion", "Muitas alocações de objetos - otimize uso de memória")
                ));
            }
        }

        // Issues de análise do MidgardRPG
        if (analysis != null) {
            // Módulos lentos
            for (ModuleAnalysis module : analysis.modules()) {
                if (module.enableTime() > 1000) {
                    issues.add(new Issue(
                        IssueCategory.MODULE,
                        HealthLevel.CRITICAL,
                        msg("performance.report.issue.slow-module.title", "Módulo Lento: ") + module.name(),
                        String.format(msg("performance.report.issue.slow-module.init-time", "Tempo de inicialização: %dms"), module.enableTime()),
                        msg("performance.report.issue.slow-module.suggestion", "Otimize onEnable() ou mova operações para async")
                    ));
                } else if (module.enableTime() > 500) {
                    issues.add(new Issue(
                        IssueCategory.MODULE,
                        HealthLevel.WARNING,
                        msg("performance.report.issue.moderate-module.title", "Módulo Moderado: ") + module.name(),
                        String.format(msg("performance.report.issue.slow-module.init-time", "Tempo de inicialização: %dms"), module.enableTime()),
                        msg("performance.report.issue.moderate-module.suggestion", "Considere lazy loading ou async init")
                    ));
                }

                // Operações críticas dentro do módulo
                for (OperationAnalysis op : module.operations()) {
                    if (op.severity() == Severity.CRITICAL || op.severity() == Severity.SEVERE) {
                        issues.add(new Issue(
                            IssueCategory.OPERATION,
                            op.severity() == Severity.SEVERE ? HealthLevel.SEVERE : HealthLevel.CRITICAL,
                            msg("performance.report.issue.slow-operation.title", "Operação Lenta: ") + op.name(),
                            String.format(msg("performance.report.issue.slow-operation.description", "Tempo máximo: %dms (%d execuções)"), op.maxTime(), op.count()),
                            msg("performance.report.issue.slow-operation.suggestion", "Otimize ou mova para thread async")
                        ));
                    }
                }
            }

            // Eventos lentos
            for (RegisteredListenerInfo listener : analysis.events().slowest()) {
                if (listener.maxTime() > 50) {
                    issues.add(new Issue(
                        IssueCategory.EVENT,
                        HealthLevel.CRITICAL,
                        msg("performance.report.issue.slow-listener.title", "Listener Lento: ") + listener.eventName(),
                        String.format(msg("performance.report.issue.slow-listener.description", "%s - %dms máximo"), listener.listenerClass(), listener.maxTime()),
                        msg("performance.report.issue.slow-listener.suggestion", "Mova lógica pesada para async ou otimize")
                    ));
                }
            }

            // Comandos lentos
            for (CommandInfo cmd : analysis.commands().slowest()) {
                if (cmd.maxTime() > 100) {
                    issues.add(new Issue(
                        IssueCategory.COMMAND,
                        HealthLevel.WARNING,
                        msg("performance.report.issue.slow-command.title", "Comando Lento: /") + cmd.name(),
                        String.format(msg("performance.report.issue.slow-command.description", "Tempo máximo: %dms"), cmd.maxTime()),
                        msg("performance.report.issue.slow-command.suggestion", "Comandos devem executar rapidamente - use async para operações pesadas")
                    ));
                }
            }
        }

        // Ordena por severidade
        issues.sort((a, b) -> b.level().ordinal() - a.level().ordinal());
        return issues;
    }

    // ========== RECOMENDAÇÕES ==========

    private static List<Recommendation> generateRecommendations(List<Issue> issues, ServerMetrics metrics, AnalysisSnapshot analysis) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Recomendações baseadas em issues
        boolean hasTPSIssue = issues.stream().anyMatch(i -> i.category() == IssueCategory.TPS);
        boolean hasMemoryIssue = issues.stream().anyMatch(i -> i.category() == IssueCategory.MEMORY);
        boolean hasGCIssue = issues.stream().anyMatch(i -> i.category() == IssueCategory.GC);
        boolean hasModuleIssue = issues.stream().anyMatch(i -> i.category() == IssueCategory.MODULE);

        if (hasTPSIssue) {
            recommendations.add(new Recommendation(
                RecommendationPriority.HIGH,
                msg("performance.report.recommendation.tps-optimization.title", "Otimização de TPS"),
                List.of(
                    msg("performance.report.recommendation.tps-optimization.step1", "Execute /spark profiler para identificar gargalos"),
                    msg("performance.report.recommendation.tps-optimization.step2", "Verifique entidades excessivas em chunks"),
                    msg("performance.report.recommendation.tps-optimization.step3", "Reduza hoppers e redstone complexa"),
                    msg("performance.report.recommendation.tps-optimization.step4", "Use Paper's async chunk loading")
                )
            ));
        }

        if (hasMemoryIssue || hasGCIssue) {
            recommendations.add(new Recommendation(
                RecommendationPriority.HIGH,
                msg("performance.report.recommendation.memory-optimization.title", "Otimização de Memória"),
                List.of(
                    msg("performance.report.recommendation.memory-optimization.step1", "Execute /spark heapsummary para análise de heap"),
                    msg("performance.report.recommendation.memory-optimization.step2", "Verifique cache de dados não liberados"),
                    msg("performance.report.recommendation.memory-optimization.step3", "Use object pooling para objetos frequentes"),
                    msg("performance.report.recommendation.memory-optimization.step4", "Considere aumentar -Xmx se necessário")
                )
            ));
        }

        if (hasModuleIssue) {
            recommendations.add(new Recommendation(
                RecommendationPriority.MEDIUM,
                msg("performance.report.recommendation.module-optimization.title", "Otimização de Módulos"),
                List.of(
                    msg("performance.report.recommendation.module-optimization.step1", "Mova I/O para CompletableFuture"),
                    msg("performance.report.recommendation.module-optimization.step2", "Implemente lazy loading para recursos pesados"),
                    msg("performance.report.recommendation.module-optimization.step3", "Cache resultados de operações frequentes"),
                    msg("performance.report.recommendation.module-optimization.step4", "Use BukkitScheduler para tarefas não-críticas")
                )
            ));
        }

        // Recomendações gerais baseadas em análise
        if (analysis != null) {
            if (analysis.events().totalListeners() > 50) {
                recommendations.add(new Recommendation(
                    RecommendationPriority.LOW,
                    msg("performance.report.recommendation.many-listeners.title", "Muitos Event Listeners"),
                    List.of(
                        msg("performance.report.recommendation.many-listeners.step1", "Considere combinar listeners relacionados"),
                        msg("performance.report.recommendation.many-listeners.step2", "Use event priorities corretamente"),
                        msg("performance.report.recommendation.many-listeners.step3", "Evite listeners em eventos de alta frequência")
                    )
                ));
            }

            if (analysis.profiler().trackedOperations() > 100) {
                recommendations.add(new Recommendation(
                    RecommendationPriority.LOW,
                    msg("performance.report.recommendation.many-operations.title", "Muitas Operações Rastreadas"),
                    List.of(
                        msg("performance.report.recommendation.many-operations.step1", "Limite uso do profiler em produção"),
                        msg("performance.report.recommendation.many-operations.step2", "Use sampling ao invés de tracing completo")
                    )
                ));
            }
        }

        // Recomendação de Spark se não disponível
        if (!SparkPerformanceManager.isAvailable()) {
            recommendations.add(new Recommendation(
                RecommendationPriority.HIGH,
                msg("performance.report.recommendation.install-spark.title", "Instale o Spark Profiler"),
                List.of(
                    msg("performance.report.recommendation.install-spark.step1", "Spark fornece métricas precisas de TPS/MSPT/CPU"),
                    msg("performance.report.recommendation.install-spark.step2", "Permite profiling detalhado de threads"),
                    msg("performance.report.recommendation.install-spark.step3", "Análise de heap e GC em tempo real"),
                    msg("performance.report.recommendation.install-spark.step4", "Download: https://spark.lucko.me/")
                )
            ));
        }

        recommendations.sort((a, b) -> a.priority().ordinal() - b.priority().ordinal());
        return recommendations;
    }

    // ========== HELPERS ==========

    private static int countCriticalIssues(HealthDiagnosis diagnosis) {
        if (diagnosis == null) {
            return 0;
        }
        
        int count = 0;
        if (diagnosis.tps().level().ordinal() >= HealthLevel.CRITICAL.ordinal()) {
            count++;
        }
        if (diagnosis.mspt().level().ordinal() >= HealthLevel.CRITICAL.ordinal()) {
            count++;
        }
        if (diagnosis.memory().level().ordinal() >= HealthLevel.CRITICAL.ordinal()) {
            count++;
        }
        if (diagnosis.cpu().level().ordinal() >= HealthLevel.CRITICAL.ordinal()) {
            count++;
        }
        if (diagnosis.gc().level().ordinal() >= HealthLevel.CRITICAL.ordinal()) {
            count++;
        }
        return count;
    }

    private static int calculateOverallScore(ServerMetrics metrics, HealthDiagnosis diagnosis, List<Issue> issues) {
        if (!metrics.available() || diagnosis == null) {
            return 0;
        }

        int score = 100;

        // Penalidades por issues
        for (Issue issue : issues) {
            switch (issue.level()) {
                case SEVERE -> score -= 20;
                case CRITICAL -> score -= 15;
                case WARNING -> score -= 8;
                case GOOD -> score -= 3;
                default -> {}
            }
        }

        // Bônus por saúde geral
        switch (diagnosis.overallHealth()) {
            case EXCELLENT -> score += 10;
            case GOOD -> score += 5;
            default -> {}
        }

        return Math.max(0, Math.min(100, score));
    }

    // ========== RECORDS ==========

    public record FullReport(
        String timestamp,
        boolean sparkAvailable,
        ServerMetrics metrics,
        HealthDiagnosis diagnosis,
        AnalysisSnapshot analysis,
        List<Issue> issues,
        List<Recommendation> recommendations,
        int overallScore
    ) {
        public String getScoreColor() {
            if (overallScore >= 90) {
                return "<green>";
            }
            if (overallScore >= 75) {
                return "<yellow>";
            }
            if (overallScore >= 50) {
                return "<gold>";
            }
            return "<red>";
        }

        public String getScoreGrade() {
            if (overallScore >= 95) {
                return "S+";
            }
            if (overallScore >= 90) {
                return "S";
            }
            if (overallScore >= 85) {
                return "A+";
            }
            if (overallScore >= 80) {
                return "A";
            }
            if (overallScore >= 75) {
                return "B+";
            }
            if (overallScore >= 70) {
                return "B";
            }
            if (overallScore >= 60) {
                return "C";
            }
            if (overallScore >= 50) {
                return "D";
            }
            return "F";
        }
    }

    public record QuickReport(
        double tps,
        double mspt,
        double memoryPercent,
        double cpuPercent,
        HealthLevel health,
        int criticalIssues,
        String timestamp
    ) {
        public static QuickReport unavailable() {
            return new QuickReport(-1, -1, -1, -1, HealthLevel.UNKNOWN, 0, 
                LocalDateTime.now().format(TIMESTAMP_FORMAT));
        }

        public boolean isAvailable() {
            return tps >= 0;
        }
    }

    public record Issue(
        IssueCategory category,
        HealthLevel level,
        String title,
        String description,
        String suggestion
    ) {}

    public record Recommendation(
        RecommendationPriority priority,
        String title,
        List<String> steps
    ) {}

    public enum IssueCategory {
        TPS("⏱", "TPS"),
        MSPT("⚡", "MSPT"),
        MEMORY("💾", "Memória"),
        CPU("💻", "CPU"),
        GC("🗑", "GC"),
        MODULE("📦", "Módulo"),
        EVENT("📡", "Evento"),
        COMMAND("⌨", "Comando"),
        OPERATION("⚙", "Operação");

        private final String icon;
        private final String label;

        IssueCategory(String icon, String label) {
            this.icon = icon;
            this.label = label;
        }

        public String getIcon() { return icon; }
        public String getLabel() {
            return switch (this) {
                case MEMORY -> msg("performance.report.category.memory", label);
                case MODULE -> msg("performance.report.category.module", label);
                case EVENT -> msg("performance.report.category.event", label);
                case COMMAND -> msg("performance.report.category.command", label);
                case OPERATION -> msg("performance.report.category.operation", label);
                default -> label;
            };
        }
    }

    public enum RecommendationPriority {
        HIGH("<red>", "Alta"),
        MEDIUM("<yellow>", "Média"),
        LOW("<gray>", "Baixa");

        private final String color;
        private final String label;

        RecommendationPriority(String color, String label) {
            this.color = color;
            this.label = label;
        }

        public String getColor() { return color; }
        public String getLabel() {
            return switch (this) {
                case HIGH -> msg("performance.report.priority.high", label);
                case MEDIUM -> msg("performance.report.priority.medium", label);
                case LOW -> msg("performance.report.priority.low", label);
            };
        }
    }
}
