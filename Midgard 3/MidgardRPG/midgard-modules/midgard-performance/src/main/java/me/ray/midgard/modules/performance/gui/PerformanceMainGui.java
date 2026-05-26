package me.ray.midgard.modules.performance.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.performance.PerformanceModule;
import me.ray.midgard.modules.performance.spark.MidgardAnalyzer;
import me.ray.midgard.modules.performance.spark.PerformanceReport;
import me.ray.midgard.modules.performance.spark.SparkPerformanceManager;
import me.ray.midgard.modules.performance.spark.SparkPerformanceManager.*;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard principal do módulo de Performance.
 * Exibe visão geral e navegação para submenus detalhados.
 */
public class PerformanceMainGui extends BaseGui {

    private final PerformanceModule module;
    // private int refreshTaskId = -1;

    public PerformanceMainGui(Player player, PerformanceModule module) {
        super(player, 6, module.getMessage("gui.main.title"));
        this.module = module;
    }

    private String msg(String key) {
        return module.getMessage("gui.main." + key);
    }

    @Override
    public void initializeItems() {
        // fillBackground(); // Removed per user request
        updateMetrics();
        addNavigationItems();
        addInfoItems();
    }


    private void updateMetrics() {
        if (!SparkPerformanceManager.isAvailable()) {
            addSparkWarning();
            return;
        }

        var manager = SparkPerformanceManager.getInstance();
        var metrics = manager.getMetrics();
        var diagnosis = manager.diagnose();

        // ===== HEALTH STATUS (Slot 4 - Topo central) =====
        HealthLevel health = diagnosis.overallHealth();
        Material healthMaterial = getHealthMaterial(health);
        
        List<String> healthLore = new ArrayList<>();
        healthLore.add("");
        healthLore.add(msg("health.status_description"));
        healthLore.add("");
        healthLore.add(health.getColor() + health.getIcon() + " " + health.getLabel());
        healthLore.add("");
        healthLore.add(msg("health.tps_label") + diagnosis.tps().getColor() + diagnosis.tps().message());
        healthLore.add(msg("health.memory_label") + diagnosis.memory().getColor() + diagnosis.memory().message());
        healthLore.add(msg("health.cpu_label") + diagnosis.cpu().getColor() + diagnosis.cpu().message());
        healthLore.add("");
        healthLore.add(msg("health.click"));

        inventory.setItem(4, new ItemBuilder(healthMaterial)
                .setName(health.getColor() + msg("health.name"))
                .lore(parseLore(healthLore))
                .glow()
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // ===== TPS GAUGE (Slot 20) =====
        var tps = metrics.tps();
        double tpsValue = tps.available() ? tps.last5s() : 0;
        String tpsColor = getTpsColor(tpsValue);
        Material tpsMaterial = getTpsMaterial(tpsValue);

        List<String> tpsLore = new ArrayList<>();
        tpsLore.add("");
        if (tps.available()) {
            tpsLore.add(msg("tps.description"));
            tpsLore.add("");
            tpsLore.add(tpsColor + msg("tps.value_format").replace("{value}", String.format("%.1f", tpsValue)));
            tpsLore.add("");
            tpsLore.add(msg("tps.label_5s") + getTpsColor(tps.last5s()) + String.format("%.2f", tps.last5s()));
            tpsLore.add(msg("tps.label_1m") + getTpsColor(tps.last1m()) + String.format("%.2f", tps.last1m()));
            tpsLore.add(msg("tps.label_5m") + getTpsColor(tps.last5m()) + String.format("%.2f", tps.last5m()));
            tpsLore.add("");
            tpsLore.add(createProgressBar(tpsValue / 20.0, tpsColor));
        } else {
            tpsLore.add(msg("tps.unavailable"));
        }
        tpsLore.add("");
        tpsLore.add(msg("click_details"));

        inventory.setItem(20, new ItemBuilder(tpsMaterial)
                .setName(msg("tps.name"))
                .lore(parseLore(tpsLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // ===== MSPT GAUGE (Slot 21) =====
        var mspt = metrics.mspt();
        double msptValue = mspt.available() ? mspt.last10s().median() : 0;
        String msptColor = getMsptColor(msptValue);

        List<String> msptLore = new ArrayList<>();
        msptLore.add("");
        if (mspt.available()) {
            msptLore.add(msg("mspt.description"));
            msptLore.add("");
            msptLore.add(msptColor + msg("mspt.value_format").replace("{value}", String.format("%.1f", msptValue)));
            msptLore.add("");
            var w = mspt.last10s();
            msptLore.add(msg("mspt.label_min") + getMsptColor(w.min()) + String.format("%.1f", w.min()) + "ms");
            msptLore.add(msg("mspt.label_p95") + getMsptColor(w.p95()) + String.format("%.1f", w.p95()) + "ms");
            msptLore.add(msg("mspt.label_max") + getMsptColor(w.max()) + String.format("%.1f", w.max()) + "ms");
            msptLore.add("");
            msptLore.add(createProgressBar(1 - (msptValue / 50.0), msptColor));
        } else {
            msptLore.add(msg("mspt.unavailable"));
        }
        msptLore.add("");
        msptLore.add(msg("click_details"));

        inventory.setItem(21, new ItemBuilder(Material.CLOCK)
                .setName(msg("mspt.name"))
                .lore(parseLore(msptLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // ===== MEMORY GAUGE (Slot 23) =====
        var mem = metrics.memory();
        double memPercent = mem.available() ? mem.usedPercent() : 0;
        String memColor = getMemoryColor(memPercent);

        List<String> memLore = new ArrayList<>();
        memLore.add("");
        if (mem.available()) {
            memLore.add(msg("memory.description"));
            memLore.add("");
            memLore.add(memColor + msg("memory.value_format").replace("{used}", String.valueOf(mem.usedMB())).replace("{max}", String.valueOf(mem.maxMB())));
            memLore.add("");
            memLore.add(msg("memory.used_label") + memColor + String.format("%.1f%%", memPercent));
            memLore.add(msg("memory.free_format").replace("{value}", String.valueOf(mem.freeMB())));
            memLore.add(msg("memory.allocated_format").replace("{value}", String.valueOf(mem.totalMB())));
            memLore.add("");
            memLore.add(createProgressBar(memPercent / 100.0, memColor));
        } else {
            memLore.add(msg("memory.unavailable"));
        }
        memLore.add("");
        memLore.add(msg("click_details"));

        Material memMaterial = memPercent > 85 ? Material.REDSTONE_BLOCK : 
                               memPercent > 70 ? Material.GOLD_BLOCK : Material.EMERALD_BLOCK;

        inventory.setItem(23, new ItemBuilder(memMaterial)
                .setName(msg("memory.name"))
                .lore(parseLore(memLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // ===== CPU GAUGE (Slot 24) =====
        var cpu = metrics.cpu();
        double cpuPercent = cpu.available() ? cpu.process().seconds10() * 100 : 0;
        String cpuColor = getCpuColor(cpuPercent);

        List<String> cpuLore = new ArrayList<>();
        cpuLore.add("");
        if (cpu.available()) {
            cpuLore.add(msg("cpu.description"));
            cpuLore.add("");
            cpuLore.add(cpuColor + msg("cpu.value_format").replace("{value}", String.format("%.1f%%", cpuPercent)));
            cpuLore.add("");
            var p = cpu.process();
            cpuLore.add(msg("cpu.label_10s") + getCpuColor(p.seconds10() * 100) + p.formatPercent(p.seconds10()));
            cpuLore.add(msg("cpu.label_1m") + getCpuColor(p.minutes1() * 100) + p.formatPercent(p.minutes1()));
            cpuLore.add(msg("cpu.label_15m") + getCpuColor(p.minutes15() * 100) + p.formatPercent(p.minutes15()));
            cpuLore.add("");
            cpuLore.add(createProgressBar(cpuPercent / 100.0, cpuColor));
        } else {
            cpuLore.add(msg("cpu.unavailable"));
        }
        cpuLore.add("");
        cpuLore.add(msg("click_details"));

        inventory.setItem(24, new ItemBuilder(Material.REDSTONE_TORCH)
                .setName(msg("cpu.name"))
                .lore(parseLore(cpuLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    private void addNavigationItems() {
        // ===== MÓDULOS (Slot 29) =====
        List<String> modulesLore = new ArrayList<>();
        modulesLore.add("");
        modulesLore.add(msg("modules.description"));
        modulesLore.add("");
        
        var analyzer = MidgardAnalyzer.getInstance();
        if (analyzer != null) {
            var analysis = analyzer.analyze();
            int total = analysis.modules().size();
            long enabled = analysis.modules().stream().filter(m -> m.enabled()).count();
            modulesLore.add(msg("modules.active_format").replace("{enabled}", String.valueOf(enabled)).replace("{total}", String.valueOf(total)));
            modulesLore.add(msg("modules.total_ops_format").replace("{value}", String.valueOf(analysis.profiler().totalExecutions())));
        } else {
            modulesLore.add(msg("modules.analyzer_unavailable"));
        }
        modulesLore.add("");
        modulesLore.add(msg("modules.click"));

        inventory.setItem(29, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(msg("modules.name"))
                .lore(parseLore(modulesLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // ===== ISSUES (Slot 31) =====
        var report = PerformanceReport.generateQuickReport();
        int criticalIssues = report.criticalIssues();
        Material issueMaterial = criticalIssues > 0 ? Material.TNT : Material.EMERALD;

        List<String> issuesLore = new ArrayList<>();
        issuesLore.add("");
        issuesLore.add(msg("issues.description"));
        issuesLore.add("");
        if (criticalIssues > 0) {
            issuesLore.add(msg("issues.critical_count").replace("{count}", String.valueOf(criticalIssues)));
        } else {
            issuesLore.add(msg("issues.no_critical"));
        }
        issuesLore.add("");
        issuesLore.add(msg("issues.click"));

        inventory.setItem(31, new ItemBuilder(issueMaterial)
                .setName(msg("issues.name"))
                .lore(parseLore(issuesLore))
                .glowIf(criticalIssues > 0)
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // ===== RELATÓRIO (Slot 33) =====
        var fullReport = PerformanceReport.generateFullReport();
        int score = fullReport.overallScore();
        String scoreColor = getScoreColor(score);

        List<String> reportLore = new ArrayList<>();
        reportLore.add("");
        reportLore.add(msg("report.description"));
        reportLore.add("");
        reportLore.add(msg("report.score_format").replace("{color}", scoreColor).replace("{score}", String.valueOf(score)).replace("{grade}", fullReport.getScoreGrade()));
        reportLore.add("");
        reportLore.add(msg("report.issues_format").replace("{count}", String.valueOf(fullReport.issues().size())));
        reportLore.add(msg("report.recommendations_format").replace("{count}", String.valueOf(fullReport.recommendations().size())));
        reportLore.add("");
        reportLore.add(msg("report.click"));

        inventory.setItem(33, new ItemBuilder(Material.BOOK)
                .setName(msg("report.name"))
                .lore(parseLore(reportLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    private void addInfoItems() {
        // ===== GC INFO (Slot 40) =====
        if (SparkPerformanceManager.isAvailable()) {
            var gc = SparkPerformanceManager.getInstance().getMetrics().gc();
            
            List<String> gcLore = new ArrayList<>();
            gcLore.add("");
            if (gc.available()) {
                gcLore.add(msg("gc.description"));
                gcLore.add("");
                gcLore.add(msg("gc.collections_format").replace("{value}", String.valueOf(gc.totalCollections())));
                gcLore.add(msg("gc.total_time_format").replace("{value}", gc.formatTime()));
                gcLore.add(msg("gc.avg_format").replace("{value}", String.format("%.2f", gc.avgTime())));
            } else {
                gcLore.add(msg("gc.unavailable"));
            }
            gcLore.add("");
            gcLore.add(msg("click_details"));

            inventory.setItem(40, new ItemBuilder(Material.HOPPER)
                    .setName(msg("gc.name"))
                    .lore(parseLore(gcLore))
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }

        // ===== WATCHER STATUS (Slot 49) =====
        var watcher = module.getHealthWatcher();
        List<String> watcherLore = new ArrayList<>();
        watcherLore.add("");
        if (watcher != null) {
            watcherLore.add(msg("watcher.description"));
            watcherLore.add("");
            watcherLore.add(msg("watcher.checks_format").replace("{value}", String.valueOf(watcher.getChecksPerformed())));
            watcherLore.add(msg("watcher.alerts_format").replace("{value}", String.valueOf(watcher.getAlertsTriggered())));
            watcherLore.add("");
            watcherLore.add(msg("watcher.tps_label") + (watcher.isTpsAlertActive() ? msg("watcher.alert") : msg("watcher.ok")));
            watcherLore.add(msg("watcher.ram_label") + (watcher.isMemoryAlertActive() ? msg("watcher.alert") : msg("watcher.ok")));
            watcherLore.add(msg("watcher.cpu_label") + (watcher.isCpuAlertActive() ? msg("watcher.alert") : msg("watcher.ok")));
        } else {
            watcherLore.add(msg("watcher.not_initialized"));
        }

        inventory.setItem(49, new ItemBuilder(Material.ENDER_EYE)
                .setName(msg("watcher.name"))
                .lore(parseLore(watcherLore))
                .glowIf(watcher != null && (watcher.isTpsAlertActive() || watcher.isMemoryAlertActive() || watcher.isCpuAlertActive()))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // ===== REFRESH (Slot 53) =====
        List<String> refreshLore = new ArrayList<>();
        refreshLore.add("");
        refreshLore.add(msg("refresh.description"));
        refreshLore.add("");
        refreshLore.add(msg("refresh.auto_disabled"));
        refreshLore.add("");
        refreshLore.add(msg("refresh.click"));

        inventory.setItem(53, new ItemBuilder(Material.SUNFLOWER)
                .setName(msg("refresh.name"))
                .lore(parseLore(refreshLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // ===== FECHAR (Slot 45) =====
        inventory.setItem(45, new ItemBuilder(Material.BARRIER)
                .setName(msg("close.name"))
                .addLore("")
                .addLore(msg("close.click"))
                .build());
    }

    private void addSparkWarning() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(msg("spark_warning.not_detected"));
        lore.add("");
        lore.add(msg("spark_warning.requires_line1"));
        lore.add(msg("spark_warning.requires_line2"));
        lore.add("");
        lore.add(msg("spark_warning.download"));
        lore.add("");

        inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .setName(msg("spark_warning.name"))
                .lore(parseLore(lore))
                .glow()
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player clicker)) {
            return;
        }
        if (!clicker.equals(this.player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        switch (slot) {
            case 4 -> { // Health Status -> Diagnóstico
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new MetricsDetailGui(clicker, module, MetricsDetailGui.MetricType.DIAGNOSE).open();
            }
            case 20 -> { // TPS
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new MetricsDetailGui(clicker, module, MetricsDetailGui.MetricType.TPS).open();
            }
            case 21 -> { // MSPT
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new MetricsDetailGui(clicker, module, MetricsDetailGui.MetricType.MSPT).open();
            }
            case 23 -> { // Memory
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new MetricsDetailGui(clicker, module, MetricsDetailGui.MetricType.MEMORY).open();
            }
            case 24 -> { // CPU
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new MetricsDetailGui(clicker, module, MetricsDetailGui.MetricType.CPU).open();
            }
            case 29 -> { // Módulos
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new ModulesAnalysisGui(clicker, module).open();
            }
            case 31 -> { // Issues
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new IssuesGui(clicker, module).open();
            }
            case 33 -> { // Relatório
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new ReportGui(clicker, module).open();
            }
            case 40 -> { // GC
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new MetricsDetailGui(clicker, module, MetricsDetailGui.MetricType.GC).open();
            }
            case 45 -> { // Fechar
                clicker.playSound(clicker.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
                clicker.closeInventory();
            }
            case 53 -> { // Refresh
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                updateMetrics();
                addNavigationItems();
            }
        }
    }

    // ===== HELPER METHODS =====

    private List<net.kyori.adventure.text.Component> parseLore(List<String> lore) {
        List<net.kyori.adventure.text.Component> components = new ArrayList<>();
        for (String line : lore) {
            components.add(MessageUtils.parse(line));
        }
        return components;
    }

    private String createProgressBar(double percent, String color) {
        percent = Math.max(0, Math.min(1, percent));
        int filled = (int) (percent * 20);
        StringBuilder bar = new StringBuilder("<dark_gray>[</dark_gray>");
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                bar.append(color).append("█");
            } else {
                bar.append("<dark_gray>░</dark_gray>");
            }
        }
        bar.append("<dark_gray>]</dark_gray>");
        return bar.toString();
    }

    private Material getHealthMaterial(HealthLevel health) {
        return switch (health) {
            case EXCELLENT -> Material.EMERALD;
            case GOOD -> Material.LIME_DYE;
            case WARNING -> Material.GOLD_INGOT;
            case CRITICAL -> Material.REDSTONE;
            case SEVERE -> Material.NETHER_STAR;
            default -> Material.GRAY_DYE;
        };
    }

    private String getTpsColor(double tps) {
        if (tps >= 19) {
            return "<green>";
        }
        if (tps >= 17) {
            return "<yellow>";
        }
        if (tps >= 15) {
            return "<gold>";
        }
        return "<red>";
    }

    private Material getTpsMaterial(double tps) {
        if (tps >= 19) {
            return Material.EMERALD;
        }
        if (tps >= 17) {
            return Material.GOLD_INGOT;
        }
        if (tps >= 15) {
            return Material.COPPER_INGOT;
        }
        return Material.REDSTONE;
    }

    private String getMsptColor(double mspt) {
        if (mspt <= 30) {
            return "<green>";
        }
        if (mspt <= 40) {
            return "<yellow>";
        }
        if (mspt <= 50) {
            return "<gold>";
        }
        return "<red>";
    }

    private String getMemoryColor(double percent) {
        if (percent <= 60) {
            return "<green>";
        }
        if (percent <= 75) {
            return "<yellow>";
        }
        if (percent <= 85) {
            return "<gold>";
        }
        return "<red>";
    }

    private String getCpuColor(double percent) {
        if (percent <= 50) {
            return "<green>";
        }
        if (percent <= 70) {
            return "<yellow>";
        }
        if (percent <= 85) {
            return "<gold>";
        }
        return "<red>";
    }

    private String getScoreColor(int score) {
        if (score >= 90) {
            return "<green>";
        }
        if (score >= 70) {
            return "<yellow>";
        }
        if (score >= 50) {
            return "<gold>";
        }
        return "<red>";
    }
}
