package me.ray.midgard.modules.performance;

import me.ray.midgard.core.command.MidgardCommand;
import me.ray.midgard.core.debug.MidgardProfiler;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.performance.gui.PerformanceMainGui;
import me.ray.midgard.modules.performance.spark.*;
import me.ray.midgard.modules.performance.spark.SparkPerformanceManager.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando principal do módulo de Performance.
 * Baseado 100% no Spark para métricas precisas.
 * 
 * Subcomandos:
 * - /perf - Dashboard completo
 * - /perf tps - TPS detalhado
 * - /perf mspt - MSPT detalhado
 * - /perf memory - Uso de memória
 * - /perf cpu - Uso de CPU
 * - /perf gc - Garbage Collection
 * - /perf modules - Análise de módulos
 * - /perf events - Análise de eventos
 * - /perf commands - Análise de comandos
 * - /perf profiler - Top operações
 * - /perf report - Relatório completo
 * - /perf diagnose - Diagnóstico de saúde
 * - /perf issues - Lista problemas detectados
 * - /perf clear - Limpa estatísticas
 */
public class PerformanceCommand extends MidgardCommand {

    private final PerformanceModule module;

    public PerformanceCommand(PerformanceModule module) {
        super("performance", "midgard.admin.performance", false);
        this.module = module;
    }

    private String getPrefix() {
        return module.getMessage("prefix");
    }

    private String getHeader() {
        return module.getMessage("command.header");
    }


    @Override
    public List<String> getAliases() {
        return java.util.Collections.singletonList("perf");
    }

    @Override
    public String getDescription() {
        return module.getMessage("command.description");
    }

    @Override
    public String getUsage() {
        return module.getMessage("command.usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendDashboard(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gui", "menu" -> openGui(sender);
            case "tps" -> sendTPS(sender);
            case "mspt" -> sendMSPT(sender);
            case "memory", "mem" -> sendMemory(sender);
            case "cpu" -> sendCPU(sender);
            case "gc" -> sendGC(sender);
            case "modules", "mods" -> sendModules(sender);
            case "events" -> sendEvents(sender);
            case "commands", "cmds" -> sendCommands(sender);
            case "profiler", "profile" -> sendProfiler(sender);
            case "report" -> sendFullReport(sender);
            case "diagnose", "diag" -> sendDiagnose(sender);
            case "issues" -> sendIssues(sender);
            case "watcher" -> sendWatcher(sender);
            case "clear" -> clearStats(sender);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }
    }

    // ========== GUI ==========
    
    private void openGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, getPrefix() + module.getMessage("command.only_players"));
            return;
        }
        
        new PerformanceMainGui(player, module).open();
    }

    // ========== DASHBOARD ==========
    
    private void sendDashboard(CommandSender sender) {
        if (!checkSpark(sender)) {
            return;
        }
        
        var manager = SparkPerformanceManager.getInstance();
        var metrics = manager.getMetrics();
        var diagnosis = manager.diagnose();
        // var report = PerformanceReport.generateQuickReport();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getHeader());
        MessageUtils.send(sender, module.getMessage("dashboard.title"));
        MessageUtils.send(sender, getHeader());
        MessageUtils.send(sender, "");
        
        // Status geral
        String healthIcon = diagnosis.overallHealth().getIcon();
        String healthColor = diagnosis.overallHealth().getColor();
        MessageUtils.send(sender, "  " + healthColor + healthIcon + module.getMessage("dashboard.health") + 
            healthColor + diagnosis.overallHealth().getLabel());
        MessageUtils.send(sender, "");

        
        // TPS
        var tps = metrics.tps();
        if (tps.available()) {
            MessageUtils.send(sender, module.getMessage("dashboard.tps_label"));
            MessageUtils.send(sender, "    " + tps.getColor(tps.last5s()) + String.format("%.1f", tps.last5s()) + 
                " <dark_gray>(5s) │ " + tps.getColor(tps.last1m()) + String.format("%.1f", tps.last1m()) + 
                " <dark_gray>(1m) │ " + tps.getColor(tps.last5m()) + String.format("%.1f", tps.last5m()) + " <dark_gray>(5m)");
            sendTPSBar(sender, tps.last5s());
        }
        
        MessageUtils.send(sender, "");
        
        // MSPT
        var mspt = metrics.mspt();
        if (mspt.available()) {
            var w = mspt.last10s();
            MessageUtils.send(sender, module.getMessage("dashboard.mspt_label") + 
                w.getColor(w.median()) + String.format("%.1f", w.median()) + "ms " + module.getMessage("dashboard.mspt_median") +
                w.getColor(w.p95()) + String.format("%.1f", w.p95()) + "ms " + module.getMessage("dashboard.mspt_p95") +
                w.getColor(w.max()) + String.format("%.1f", w.max()) + "ms " + module.getMessage("dashboard.mspt_max"));
        }
        
        // Memory
        var mem = metrics.memory();
        if (mem.available()) {
            MessageUtils.send(sender, module.getMessage("dashboard.ram_label") + 
                mem.getColor() + mem.usedMB() + "MB" + 
                module.getMessage("dashboard.ram_suffix") + mem.maxMB() + "MB <dark_gray>(" + 
                mem.getColor() + String.format("%.1f%%", mem.usedPercent()) + "<dark_gray>)");
        }
        
        // CPU
        var cpu = metrics.cpu();
        if (cpu.available()) {
            var p = cpu.process();
            MessageUtils.send(sender, module.getMessage("dashboard.cpu_label") + 
                p.getColor(p.seconds10()) + p.formatPercent(p.seconds10()) + 
                module.getMessage("dashboard.cpu_process") +
                cpu.system().getColor(cpu.system().seconds10()) + cpu.system().formatPercent(cpu.system().seconds10()) + 
                module.getMessage("dashboard.cpu_system"));
        }
        
        // GC
        var gc = metrics.gc();
        if (gc.available()) {
            MessageUtils.send(sender, module.getMessage("dashboard.gc_label") + gc.totalCollections() + 
                " <gray>coletas │ <white>" + gc.formatTime() + module.getMessage("dashboard.gc_total"));
        }
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, module.getMessage("command.help_footer"));
        MessageUtils.send(sender, "");
    }

    // ========== TPS DETALHADO ==========
    
    private void sendTPS(CommandSender sender) {
        if (!checkSpark(sender)) {
            return;
        }
        
        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var tps = metrics.tps();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("tps.title"));
        MessageUtils.send(sender, "");
        
        if (!tps.available()) {
            MessageUtils.send(sender, "  " + module.getMessage("errors.unavailable"));
            return;
        }
        
        MessageUtils.send(sender, module.getMessage("tps.windows"));
        MessageUtils.send(sender, "    <dark_gray>5s:  " + tps.getColor(tps.last5s()) + String.format("%.2f", tps.last5s()));
        MessageUtils.send(sender, "    <dark_gray>10s: " + tps.getColor(tps.last10s()) + String.format("%.2f", tps.last10s()));
        MessageUtils.send(sender, "    <dark_gray>1m:  " + tps.getColor(tps.last1m()) + String.format("%.2f", tps.last1m()));
        MessageUtils.send(sender, "    <dark_gray>5m:  " + tps.getColor(tps.last5m()) + String.format("%.2f", tps.last5m()));
        MessageUtils.send(sender, "    <dark_gray>15m: " + tps.getColor(tps.last15m()) + String.format("%.2f", tps.last15m()));
        MessageUtils.send(sender, "");
        
        sendTPSBar(sender, tps.last5s());
        
        // Análise
        MessageUtils.send(sender, "");
        var diagnosis = SparkPerformanceManager.getInstance().diagnose().tps();
        MessageUtils.send(sender, module.getMessage("tps.diagnosis")
            .replace("%color%", diagnosis.getColor())
            .replace("%message%", diagnosis.message()));
        MessageUtils.send(sender, "");
    }

    // ========== MSPT DETALHADO ==========
    
    private void sendMSPT(CommandSender sender) {
        if (!checkSpark(sender)) {
            return;
        }
        
        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var mspt = metrics.mspt();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("mspt.title"));
        MessageUtils.send(sender, "");
        
        if (!mspt.available()) {
            MessageUtils.send(sender, "  " + module.getMessage("errors.unavailable"));
            return;
        }
        
        // Últimos 10 segundos
        var w10s = mspt.last10s();
        MessageUtils.send(sender, module.getMessage("mspt.last_10s"));
        sendMsptStats(sender, "Min", w10s.min(), w10s);
        sendMsptStats(sender, "Mediana", w10s.median(), w10s);
        sendMsptStats(sender, "P95", w10s.p95(), w10s);
        sendMsptStats(sender, "Max", w10s.max(), w10s);
        MessageUtils.send(sender, "");
        
        // Último minuto
        var w1m = mspt.last1m();
        MessageUtils.send(sender, module.getMessage("mspt.last_1m"));
        sendMsptStats(sender, "Min", w1m.min(), w1m);
        sendMsptStats(sender, "Mediana", w1m.median(), w1m);
        sendMsptStats(sender, "P95", w1m.p95(), w1m);
        sendMsptStats(sender, "Max", w1m.max(), w1m);
        MessageUtils.send(sender, "");
        
        // Nota explicativa
        MessageUtils.send(sender, module.getMessage("mspt.warning"));
        MessageUtils.send(sender, "");
    }
    
    private void sendMsptStats(CommandSender sender, String type, double val, me.ray.midgard.modules.performance.spark.SparkPerformanceManager.MSPTWindow w) {
         MessageUtils.send(sender, module.getMessage("mspt.stats")
            .replace("%type%", type)
            .replace("%color%", w.getColor(val))
            .replace("%value%", String.format("%.2f", val)));
    }

    // ========== MEMORY ==========
    
    private void sendMemory(CommandSender sender) {
        if (!checkSpark(sender)) {
            return;
        }
        
        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var mem = metrics.memory();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("memory.title"));
        MessageUtils.send(sender, "");
        
        MessageUtils.send(sender, module.getMessage("memory.used").replace("%color%", mem.getColor()).replace("%value%", String.valueOf(mem.usedMB())));
        MessageUtils.send(sender, module.getMessage("memory.free").replace("%value%", String.valueOf(mem.freeMB())));
        MessageUtils.send(sender, module.getMessage("memory.allocated").replace("%value%", String.valueOf(mem.totalMB())));
        MessageUtils.send(sender, module.getMessage("memory.max").replace("%value%", String.valueOf(mem.maxMB())));
        MessageUtils.send(sender, "");
        
        // Barra visual
        sendMemoryBar(sender, mem.usedPercent());
        
        // GC Info
        var gc = metrics.gc();
        if (gc.available()) {
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, module.getMessage("memory.gc_title"));
            MessageUtils.send(sender, module.getMessage("memory.gc_collections").replace("%value%", String.valueOf(gc.totalCollections())));
            MessageUtils.send(sender, module.getMessage("memory.gc_total_time").replace("%value%", gc.formatTime()));
            MessageUtils.send(sender, module.getMessage("memory.gc_avg_time").replace("%value%", String.format("%.2f", gc.avgTime())));
        }
        MessageUtils.send(sender, "");
    }

    // ========== CPU ==========
    
    private void sendCPU(CommandSender sender) {
        if (!checkSpark(sender)) {
            return;
        }
        
        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var cpu = metrics.cpu();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("cpu.title"));
        MessageUtils.send(sender, "");
        
        if (!cpu.available()) {
            MessageUtils.send(sender, "  " + module.getMessage("errors.unavailable"));
            return;
        }
        
        var p = cpu.process();
        var s = cpu.system();
        
        MessageUtils.send(sender, module.getMessage("cpu.process"));
        MessageUtils.send(sender, "    <dark_gray>10s: " + p.getColor(p.seconds10()) + p.formatPercent(p.seconds10()));
        MessageUtils.send(sender, "    <dark_gray>1m:  " + p.getColor(p.minutes1()) + p.formatPercent(p.minutes1()));
        MessageUtils.send(sender, "    <dark_gray>15m: " + p.getColor(p.minutes15()) + p.formatPercent(p.minutes15()));
        MessageUtils.send(sender, "");
        
        MessageUtils.send(sender, module.getMessage("cpu.system"));
        MessageUtils.send(sender, "    <dark_gray>10s: " + s.getColor(s.seconds10()) + s.formatPercent(s.seconds10()));
        MessageUtils.send(sender, "    <dark_gray>1m:  " + s.getColor(s.minutes1()) + s.formatPercent(s.minutes1()));
        MessageUtils.send(sender, "    <dark_gray>15m: " + s.getColor(s.minutes15()) + s.formatPercent(s.minutes15()));
        MessageUtils.send(sender, "");
    }

    // ========== GC ==========
    
    private void sendGC(CommandSender sender) {
        if (!checkSpark(sender)) {
            return;
        }
        
        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var gc = metrics.gc();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("gc.title"));
        MessageUtils.send(sender, "");
        
        if (!gc.available()) {
            MessageUtils.send(sender, "  " + module.getMessage("errors.unavailable"));
            return;
        }
        
        MessageUtils.send(sender, module.getMessage("gc.total_collections").replace("%value%", String.valueOf(gc.totalCollections())));
        MessageUtils.send(sender, module.getMessage("gc.total_time").replace("%value%", gc.formatTime()));
        MessageUtils.send(sender, module.getMessage("gc.avg_time").replace("%value%", String.format("%.2f", gc.avgTime())));
        MessageUtils.send(sender, module.getMessage("gc.frequency").replace("%value%", String.format("%.1f", gc.avgFrequency() / 1000.0)));
        MessageUtils.send(sender, "");
        
        // Coletores individuais
        if (!gc.collectors().isEmpty()) {
            MessageUtils.send(sender, module.getMessage("gc.collectors"));
            for (var entry : gc.collectors().entrySet()) {
                var collector = entry.getValue();
                MessageUtils.send(sender, module.getMessage("gc.collector_stats")
                    .replace("%name%", entry.getKey())
                    .replace("%count%", String.valueOf(collector.totalCollections()))
                    .replace("%avg%", String.format("%.2f", collector.avgTime())));
            }
        }
        MessageUtils.send(sender, "");
    }

    // ========== MODULES ==========
    
    private void sendModules(CommandSender sender) {
        var analyzer = MidgardAnalyzer.getInstance();
        if (analyzer == null) {
            MessageUtils.send(sender, getPrefix() + module.getMessage("modules.analyzer_error"));
            return;
        }
        
        var analysis = analyzer.analyze();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("modules.title"));
        MessageUtils.send(sender, "");
        
        if (analysis.modules().isEmpty()) {
            MessageUtils.send(sender, module.getMessage("dashboard.modules_none"));
            return;
        }
        
        for (var mod : analysis.modules()) {
            String status = mod.enabled() ? "<green>✔" : "<red>✘";
            String healthIcon = mod.health().getIcon();
            String healthColor = mod.health().getColor();
            
            MessageUtils.send(sender, "  " + status + " " + healthColor + healthIcon + " <white>" + mod.name());
            MessageUtils.send(sender, module.getMessage("modules.stats")
                .replace("%color%", getTimeColor(mod.enableTime()))
                .replace("%time%", String.valueOf(mod.enableTime()))
                .replace("%ops%", String.valueOf(mod.totalOperations()))
                .replace("%listeners%", String.valueOf(mod.listenerCount())));
            
            // Top 3 operações lentas do módulo
            if (!mod.operations().isEmpty()) {
                var slowOps = mod.operations().stream().limit(3).toList();
                for (var op : slowOps) {
                    if (op.maxTime() > 10) {
                        MessageUtils.send(sender, "        " + op.severity().getColor() + op.severity().getIcon() + 
                            " <gray>" + op.name() + ": " + op.maxTime() + "ms");
                    }
                }
            }
        }
        MessageUtils.send(sender, "");
    }

    // ========== EVENTS ==========
    
    private void sendEvents(CommandSender sender) {
        var analyzer = MidgardAnalyzer.getInstance();
        if (analyzer == null) {
            return;
        }
        
        var analysis = analyzer.analyze();
        var events = analysis.events();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("events.title"));
        MessageUtils.send(sender, "");
        
        MessageUtils.send(sender, module.getMessage("events.total_listeners").replace("%value%", String.valueOf(events.totalListeners())));
        MessageUtils.send(sender, module.getMessage("events.unique_events").replace("%value%", String.valueOf(events.uniqueEvents())));
        MessageUtils.send(sender, "");
        
        if (!events.slowest().isEmpty()) {
            MessageUtils.send(sender, module.getMessage("events.slowest"));
            for (var listener : events.slowest()) {
                MessageUtils.send(sender, "    " + listener.severity().getColor() + listener.severity().getIcon() + 
                    " <white>" + listener.eventName() + " <dark_gray>(" + listener.listenerClass() + ")");
                MessageUtils.send(sender, module.getMessage("events.stats")
                    .replace("%max%", String.valueOf(listener.maxTime()))
                    .replace("%count%", String.valueOf(listener.executions())));
            }
        } else {
            MessageUtils.send(sender, module.getMessage("dashboard.events_none"));
        }
        MessageUtils.send(sender, "");
    }

    // ========== COMMANDS ==========
    
    private void sendCommands(CommandSender sender) {
        var analyzer = MidgardAnalyzer.getInstance();
        if (analyzer == null) {
            return;
        }
        
        var analysis = analyzer.analyze();
        var commands = analysis.commands();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("commands.title"));
        MessageUtils.send(sender, "");
        
        MessageUtils.send(sender, module.getMessage("commands.total_commands").replace("%value%", String.valueOf(commands.totalCommands())));
        MessageUtils.send(sender, module.getMessage("commands.total_executions").replace("%value%", String.valueOf(commands.totalExecutions())));
        MessageUtils.send(sender, "");
        
        if (!commands.slowest().isEmpty()) {
            MessageUtils.send(sender, module.getMessage("commands.slowest"));
            for (var cmd : commands.slowest()) {
                MessageUtils.send(sender, "    " + cmd.severity().getColor() + cmd.severity().getIcon() + 
                    " <white>/" + cmd.name());
                MessageUtils.send(sender, module.getMessage("commands.stats")
                    .replace("%max%", String.valueOf(cmd.maxTime()))
                    .replace("%count%", String.valueOf(cmd.executions())));
            }
        } else {
            MessageUtils.send(sender, module.getMessage("dashboard.commands_none"));
        }
        MessageUtils.send(sender, "");
    }

    // ========== PROFILER ==========
    
    private void sendProfiler(CommandSender sender) {
        var analyzer = MidgardAnalyzer.getInstance();
        if (analyzer == null) {
            return;
        }
        
        var analysis = analyzer.analyze();
        var profiler = analysis.profiler();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("profiler.title"));
        MessageUtils.send(sender, "");
        
        MessageUtils.send(sender, module.getMessage("profiler.tracked").replace("%value%", String.valueOf(profiler.trackedOperations())));
        MessageUtils.send(sender, module.getMessage("profiler.total_exec").replace("%value%", String.valueOf(profiler.totalExecutions())));
        MessageUtils.send(sender, module.getMessage("profiler.total_time").replace("%value%", String.valueOf(profiler.totalTime())));
        MessageUtils.send(sender, "");
        
        // Top 10 mais lentas
        MessageUtils.send(sender, module.getMessage("profiler.slowest"));
        int i = 1;
        for (var op : profiler.slowest().stream().limit(10).toList()) {
            MessageUtils.send(sender, "    <gray>" + i++ + ". " + op.severity().getColor() + op.name());
            MessageUtils.send(sender, module.getMessage("profiler.stats")
                .replace("%max%", String.valueOf(op.maxTime()))
                .replace("%last%", String.valueOf(op.lastTime()))
                .replace("%count%", String.valueOf(op.count())));
        }
        MessageUtils.send(sender, "");
    }

    // ========== FULL REPORT ==========
    
    private void sendFullReport(CommandSender sender) {
        var report = PerformanceReport.generateFullReport();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getHeader());
        MessageUtils.send(sender, module.getMessage("report.title"));
        MessageUtils.send(sender, getHeader());
        MessageUtils.send(sender, "");
        
        // Score geral
        MessageUtils.send(sender, module.getMessage("report.score")
            .replace("%color%", report.getScoreColor())
            .replace("%score%", String.valueOf(report.overallScore()))
            .replace("%grade%", report.getScoreGrade()));
        MessageUtils.send(sender, module.getMessage("report.generated").replace("%time%", report.timestamp()));
        MessageUtils.send(sender, module.getMessage("report.spark_status").replace("%status%", report.sparkAvailable() ? module.getMessage("report.spark_active") : module.getMessage("report.spark_inactive")));
        MessageUtils.send(sender, "");
        
        // Issues
        if (!report.issues().isEmpty()) {
            MessageUtils.send(sender, module.getMessage("report.issues_detected").replace("%count%", String.valueOf(report.issues().size())));
            for (var issue : report.issues().stream().limit(5).toList()) {
                MessageUtils.send(sender, "    " + issue.level().getColor() + issue.category().getIcon() + 
                    " " + issue.title());
            }
            if (report.issues().size() > 5) {
                MessageUtils.send(sender, module.getMessage("report.more_issues").replace("%count%", String.valueOf(report.issues().size() - 5)));
            }
        } else {
            MessageUtils.send(sender, module.getMessage("report.issues_none"));
        }
        
        MessageUtils.send(sender, "");
        
        // Recomendações
        if (!report.recommendations().isEmpty()) {
            MessageUtils.send(sender, module.getMessage("report.recommendations"));
            for (var rec : report.recommendations().stream().limit(3).toList()) {
                MessageUtils.send(sender, "    " + rec.priority().getColor() + "● " + rec.title());
            }
        }
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, module.getMessage("report.footer"));
        MessageUtils.send(sender, "");
    }

    // ========== DIAGNOSE ==========
    
    private void sendDiagnose(CommandSender sender) {
        if (!checkSpark(sender)) {
            return;
        }
        
        var diagnosis = SparkPerformanceManager.getInstance().diagnose();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("diagnose.title"));
        MessageUtils.send(sender, "");
        
        MessageUtils.send(sender, module.getMessage("diagnose.status")
            .replace("%color%", diagnosis.overallHealth().getColor())
            .replace("%icon%", diagnosis.overallHealth().getIcon())
            .replace("%label%", diagnosis.overallHealth().getLabel()));
        MessageUtils.send(sender, "");
        
        // Cada componente
        sendDiagnosisLine(sender, "TPS", diagnosis.tps());
        sendDiagnosisLine(sender, "MSPT", diagnosis.mspt());
        sendDiagnosisLine(sender, "CPU", diagnosis.cpu());
        sendDiagnosisLine(sender, "Memória", diagnosis.memory());
        sendDiagnosisLine(sender, "GC", diagnosis.gc());
        
        MessageUtils.send(sender, "");
    }

    private void sendDiagnosisLine(CommandSender sender, String name, HealthIssue issue) {
        MessageUtils.send(sender, module.getMessage("diagnose.line")
            .replace("%color%", issue.getColor())
            .replace("%icon%", issue.getIcon())
            .replace("%name%", name)
            .replace("%message%", issue.message()));
    }

    // ========== ISSUES ==========
    
    private void sendIssues(CommandSender sender) {
        var report = PerformanceReport.generateFullReport();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("issues.title"));
        MessageUtils.send(sender, "");
        
        if (report.issues().isEmpty()) {
            MessageUtils.send(sender, module.getMessage("report.issues_none"));
            MessageUtils.send(sender, "");
            return;
        }
        
        for (var issue : report.issues()) {
            MessageUtils.send(sender, "  " + issue.level().getColor() + issue.category().getIcon() + 
                " <bold>" + issue.title() + "</bold>");
            MessageUtils.send(sender, "    <gray>" + issue.description());
            MessageUtils.send(sender, module.getMessage("issues.suggestion").replace("%suggestion%", issue.suggestion()));
            MessageUtils.send(sender, "");
        }
    }

    // ========== WATCHER ==========
    
    private void sendWatcher(CommandSender sender) {
        var watcher = module.getHealthWatcher();
        
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getPrefix() + module.getMessage("watcher.title"));
        MessageUtils.send(sender, "");
        
        if (watcher == null) {
            MessageUtils.send(sender, module.getMessage("watcher.not_init"));
            return;
        }
        
        MessageUtils.send(sender, module.getMessage("watcher.checks").replace("%value%", String.valueOf(watcher.getChecksPerformed())));
        MessageUtils.send(sender, module.getMessage("watcher.alerts").replace("%value%", String.valueOf(watcher.getAlertsTriggered())));
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, module.getMessage("watcher.active_alerts"));
        MessageUtils.send(sender, module.getMessage("watcher.alert_status").replace("%name%", "TPS").replace("%status%", watcher.isTpsAlertActive() ? module.getMessage("watcher.active") : module.getMessage("watcher.ok")));
        MessageUtils.send(sender, module.getMessage("watcher.alert_status").replace("%name%", "Memória").replace("%status%", watcher.isMemoryAlertActive() ? module.getMessage("watcher.active") : module.getMessage("watcher.ok")));
        MessageUtils.send(sender, module.getMessage("watcher.alert_status").replace("%name%", "CPU").replace("%status%", watcher.isCpuAlertActive() ? module.getMessage("watcher.active") : module.getMessage("watcher.ok")));
        MessageUtils.send(sender, "");
    }

    // ========== CLEAR ==========
    
    private void clearStats(CommandSender sender) {
        MidgardProfiler.clearStats();
        var analyzer = MidgardAnalyzer.getInstance();
        if (analyzer != null) {
            analyzer.clearTracking();
        }
        
        MessageUtils.send(sender, getPrefix() + module.getMessage("clear.success"));
    }

    // ========== HELP ==========
    
    private void sendHelp(CommandSender sender) {
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, getHeader());
        MessageUtils.send(sender, module.getMessage("help.title"));
        MessageUtils.send(sender, getHeader());
        MessageUtils.send(sender, "");
        
        // Categoria: Interface Gráfica
        MessageUtils.send(sender, module.getMessage("help.gui"));
        MessageUtils.send(sender, module.getMessage("help.line_gui"));
        MessageUtils.send(sender, "");
        
        // Categoria: Dashboard
        MessageUtils.send(sender, module.getMessage("help.dashboard"));
        MessageUtils.send(sender, module.getMessage("help.line_dashboard"));
        MessageUtils.send(sender, "");
        
        // Categoria: Métricas Spark
        MessageUtils.send(sender, module.getMessage("help.spark"));
        MessageUtils.send(sender, module.getMessage("help.line_tps"));
        MessageUtils.send(sender, module.getMessage("help.line_mspt"));
        MessageUtils.send(sender, module.getMessage("help.line_memory"));
        MessageUtils.send(sender, module.getMessage("help.line_cpu"));
        MessageUtils.send(sender, module.getMessage("help.line_gc"));
        MessageUtils.send(sender, "");
        
        // Categoria: Análise Midgard
        MessageUtils.send(sender, module.getMessage("help.analysis"));
        MessageUtils.send(sender, module.getMessage("help.line_modules"));
        MessageUtils.send(sender, module.getMessage("help.line_events"));
        MessageUtils.send(sender, module.getMessage("help.line_commands"));
        MessageUtils.send(sender, module.getMessage("help.line_profiler"));
        MessageUtils.send(sender, "");
        
        // Categoria: Diagnóstico
        MessageUtils.send(sender, module.getMessage("help.diagnosis"));
        MessageUtils.send(sender, module.getMessage("help.line_report"));
        MessageUtils.send(sender, module.getMessage("help.line_diagnose"));
        MessageUtils.send(sender, module.getMessage("help.line_issues"));
        MessageUtils.send(sender, "");
        
        // Categoria: Utilitários
        MessageUtils.send(sender, module.getMessage("help.utils"));
        MessageUtils.send(sender, module.getMessage("help.line_watcher"));
        MessageUtils.send(sender, module.getMessage("help.line_clear"));
        MessageUtils.send(sender, module.getMessage("help.line_help"));
        MessageUtils.send(sender, "");
        
        // Nota sobre Spark
        if (!SparkPerformanceManager.isAvailable()) {
            MessageUtils.send(sender, module.getMessage("help.spark_warn"));
            MessageUtils.send(sender, module.getMessage("help.download"));
            MessageUtils.send(sender, "");
        }
        
        MessageUtils.send(sender, getHeader());
    }

    // ========== HELPERS ==========
    
    private boolean checkSpark(CommandSender sender) {
        if (!SparkPerformanceManager.isAvailable()) {
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, getPrefix() + module.getMessage("errors.spark_required"));
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, module.getMessage("errors.spark_desc_1"));
            MessageUtils.send(sender, module.getMessage("errors.spark_desc_2"));
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, module.getMessage("help.download"));
            MessageUtils.send(sender, "");
            return false;
        }
        return true;
    }

    private void sendTPSBar(CommandSender sender, double tps) {
        int filled = (int) (tps / 20.0 * 20);
        StringBuilder bar = new StringBuilder("    <dark_gray>[");
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                if (tps >= 19) {
                    bar.append("<green>");
                } else if (tps >= 17) {
                    bar.append("<yellow>");
                } else if (tps >= 15) {
                    bar.append("<gold>");
                } else {
                    bar.append("<red>");
                }
                bar.append("█");
            } else {
                bar.append("<dark_gray>░");
            }
        }
        bar.append("<dark_gray>]");
        MessageUtils.send(sender, bar.toString());
    }

    private void sendMemoryBar(CommandSender sender, double percent) {
        int filled = (int) (percent / 100.0 * 20);
        StringBuilder bar = new StringBuilder("    <dark_gray>[");
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                if (percent <= 60) {
                    bar.append("<green>");
                } else if (percent <= 75) {
                    bar.append("<yellow>");
                } else if (percent <= 85) {
                    bar.append("<gold>");
                } else {
                    bar.append("<red>");
                }
                bar.append("█");
            } else {
                bar.append("<dark_gray>░");
            }
        }
        bar.append("<dark_gray>] ");
        
        String color = percent <= 60 ? "<green>" : (percent <= 75 ? "<yellow>" : (percent <= 85 ? "<gold>" : "<red>"));
        bar.append(color).append(String.format("%.1f%%", percent));
        MessageUtils.send(sender, bar.toString());
    }

    private String getTimeColor(long ms) {
        if (ms < 100) {
            return "<green>";
        }
        if (ms < 300) {
            return "<yellow>";
        }
        if (ms < 500) {
            return "<gold>";
        }
        return "<red>";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = List.of(
                // Interface Gráfica
                "gui", "menu",
                // Dashboard
                "help",
                // Métricas Spark
                "tps", "mspt", "memory", "mem", "cpu", "gc",
                // Análise Midgard
                "modules", "mods", "events", "commands", "cmds", "profiler", "profile",
                // Diagnóstico
                "report", "diagnose", "diag", "issues",
                // Utilitários
                "watcher", "clear"
            );
            return subcommands.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }
}

