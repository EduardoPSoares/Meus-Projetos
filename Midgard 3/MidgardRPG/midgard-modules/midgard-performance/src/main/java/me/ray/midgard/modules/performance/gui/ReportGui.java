package me.ray.midgard.modules.performance.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.performance.PerformanceModule;
import me.ray.midgard.modules.performance.spark.PerformanceReport;
import me.ray.midgard.modules.performance.spark.PerformanceReport.*;
import me.ray.midgard.modules.performance.spark.SparkPerformanceManager.HealthLevel;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI de relatório completo de performance.
 * Exibe score, issues e recomendações.
 */
public class ReportGui extends BaseGui {

    private final PerformanceModule module;
    private final FullReport report;

    public ReportGui(Player player, PerformanceModule module) {
        super(player, 6, module.getMessage("gui.report.title"));
        this.module = module;
        this.report = PerformanceReport.generateFullReport();
    }

    @Override
    public void initializeItems() {
        fillBackground();
        addScoreDisplay();
        addMetricsSummary();
        addIssuesSummary();
        addRecommendations();
        addNavigationItems();
    }

    private void fillBackground() {
        ItemStack darkPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        
        ItemStack goldPane = new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, darkPane);
        }

        // Top accent
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, goldPane);
        }

        // Bottom accent
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, goldPane);
        }

        // Side accents
        int[] sideSlots = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : sideSlots) {
            inventory.setItem(slot, goldPane);
        }
    }

    private void addScoreDisplay() {
        int score = report.overallScore();
        String scoreColor = report.getScoreColor();
        String grade = report.getScoreGrade();

        Material scoreMaterial = score >= 90 ? Material.EMERALD_BLOCK :
                                 score >= 70 ? Material.GOLD_BLOCK :
                                 score >= 50 ? Material.COPPER_BLOCK : Material.REDSTONE_BLOCK;

        List<String> scoreLore = new ArrayList<>();
        scoreLore.add("");
        scoreLore.add("<gray>" + msg("score.description"));
        scoreLore.add("");
        scoreLore.add(scoreColor + "<bold>" + score + "/100</bold> <dark_gray>[" + scoreColor + grade + "<dark_gray>]");
        scoreLore.add("");
        scoreLore.add(createScoreBar(score));
        scoreLore.add("");
        scoreLore.add("<dark_gray>" + msg("score.generated-at") + " <white>" + report.timestamp());
        scoreLore.add("<dark_gray>" + msg("score.spark") + " " + (report.sparkAvailable() ? "<green>✔ " + msg("score.spark-active") : "<red>✘ " + msg("score.spark-inactive")));
        scoreLore.add("");
        
        // Quick stats
        if (report.metrics() != null && report.metrics().available()) {
            var metrics = report.metrics();
            if (metrics.tps().available()) {
                String tpsColor = getTpsColor(metrics.tps().last5s());
                scoreLore.add("<dark_gray>" + msg("score.tps-label") + " " + tpsColor + String.format("%.1f", metrics.tps().last5s()));
            }
            if (metrics.memory().available()) {
                String memColor = getMemoryColor(metrics.memory().usedPercent());
                scoreLore.add("<dark_gray>" + msg("score.ram-label") + " " + memColor + String.format("%.1f%%", metrics.memory().usedPercent()));
            }
        }

        inventory.setItem(4, new ItemBuilder(scoreMaterial)
                .setName(scoreColor + msg("score.item-title"))
                .lore(parseLore(scoreLore))
                .glow()
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    private void addMetricsSummary() {
        if (report.diagnosis() == null) {
            return;
        }

        var diagnosis = report.diagnosis();

        // TPS Status (Slot 19)
        var tpsDiag = diagnosis.tps();
        List<String> tpsLore = new ArrayList<>();
        tpsLore.add("");
        tpsLore.add(tpsDiag.getColor() + tpsDiag.getIcon() + " " + tpsDiag.message());

        inventory.setItem(19, new ItemBuilder(Material.CLOCK)
                .setName(tpsDiag.getColor() + msg("metrics.tps"))
                .lore(parseLore(tpsLore))
                .glowIf(tpsDiag.level().ordinal() >= HealthLevel.WARNING.ordinal())
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // MSPT Status (Slot 20)
        var msptDiag = diagnosis.mspt();
        List<String> msptLore = new ArrayList<>();
        msptLore.add("");
        msptLore.add(msptDiag.getColor() + msptDiag.getIcon() + " " + msptDiag.message());

        inventory.setItem(20, new ItemBuilder(Material.LIGHTNING_ROD)
                .setName(msptDiag.getColor() + msg("metrics.mspt"))
                .lore(parseLore(msptLore))
                .glowIf(msptDiag.level().ordinal() >= HealthLevel.WARNING.ordinal())
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // CPU Status (Slot 21)
        var cpuDiag = diagnosis.cpu();
        List<String> cpuLore = new ArrayList<>();
        cpuLore.add("");
        cpuLore.add(cpuDiag.getColor() + cpuDiag.getIcon() + " " + cpuDiag.message());

        inventory.setItem(21, new ItemBuilder(Material.REDSTONE_TORCH)
                .setName(cpuDiag.getColor() + msg("metrics.cpu"))
                .lore(parseLore(cpuLore))
                .glowIf(cpuDiag.level().ordinal() >= HealthLevel.WARNING.ordinal())
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // Memory Status (Slot 23)
        var memDiag = diagnosis.memory();
        List<String> memLore = new ArrayList<>();
        memLore.add("");
        memLore.add(memDiag.getColor() + memDiag.getIcon() + " " + memDiag.message());

        inventory.setItem(23, new ItemBuilder(Material.EMERALD)
                .setName(memDiag.getColor() + msg("metrics.memory"))
                .lore(parseLore(memLore))
                .glowIf(memDiag.level().ordinal() >= HealthLevel.WARNING.ordinal())
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // GC Status (Slot 24)
        var gcDiag = diagnosis.gc();
        List<String> gcLore = new ArrayList<>();
        gcLore.add("");
        gcLore.add(gcDiag.getColor() + gcDiag.getIcon() + " " + gcDiag.message());

        inventory.setItem(24, new ItemBuilder(Material.HOPPER)
                .setName(gcDiag.getColor() + msg("metrics.gc"))
                .lore(parseLore(gcLore))
                .glowIf(gcDiag.level().ordinal() >= HealthLevel.WARNING.ordinal())
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // Overall Health (Slot 25)
        HealthLevel overall = diagnosis.overallHealth();
        List<String> overallLore = new ArrayList<>();
        overallLore.add("");
        overallLore.add(overall.getColor() + overall.getIcon() + " " + overall.getLabel());

        inventory.setItem(25, new ItemBuilder(getHealthMaterial(overall))
                .setName(overall.getColor() + msg("metrics.overall-health"))
                .lore(parseLore(overallLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    private void addIssuesSummary() {
        var issues = report.issues();

        Material issueMat = issues.isEmpty() ? Material.EMERALD : Material.TNT;
        String issueColor = issues.isEmpty() ? "<green>" : "<red>";

        List<String> issueLore = new ArrayList<>();
        issueLore.add("");
        
        if (issues.isEmpty()) {
            issueLore.add("<green>" + msg("issues.none"));
            issueLore.add("");
            issueLore.add("<gray>" + msg("issues.server-ok"));
        } else {
            issueLore.add("<red>⚠ " + issues.size() + " " + msg("issues.found"));
            issueLore.add("");
            
            // Top 5 issues
            int count = 0;
            for (Issue issue : issues) {
                if (count >= 5) {
                    break;
                }
                String color = issue.level().getColor();
                issueLore.add(color + "• " + issue.title());
                count++;
            }
            
            if (issues.size() > 5) {
                issueLore.add("<dark_gray>... " + msg("issues.and-more") + " " + (issues.size() - 5));
            }
        }
        
        issueLore.add("");
        issueLore.add("<yellow>" + msg("issues.click-view") + "</yellow>");

        inventory.setItem(30, new ItemBuilder(issueMat)
                .setName(issueColor + msg("issues.title"))
                .lore(parseLore(issueLore))
                .glowIf(!issues.isEmpty())
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    private void addRecommendations() {
        var recommendations = report.recommendations();

        List<String> recLore = new ArrayList<>();
        recLore.add("");
        
        if (recommendations.isEmpty()) {
            recLore.add("<green>" + msg("recommendations.none"));
            recLore.add("");
            recLore.add("<gray>" + msg("recommendations.server-optimized"));
        } else {
            recLore.add("<aqua>📋 " + recommendations.size() + " " + msg("recommendations.found"));
            recLore.add("");
            
            // Top 4 recommendations
            int count = 0;
            for (Recommendation rec : recommendations) {
                if (count >= 4) {
                    break;
                }
                String color = rec.priority().getColor();
                recLore.add(color + "• " + rec.title());
                count++;
            }
            
            if (recommendations.size() > 4) {
                recLore.add("<dark_gray>... " + msg("recommendations.and-more") + " " + (recommendations.size() - 4));
            }
        }
        
        recLore.add("");
        recLore.add("<yellow>" + msg("recommendations.click-details") + "</yellow>");

        inventory.setItem(32, new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName("<yellow>" + msg("recommendations.title") + "</yellow>")
                .lore(parseLore(recLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // Detailed recommendations in slots 37-43
        if (!recommendations.isEmpty()) {
            int slot = 37;
            for (Recommendation rec : recommendations) {
                if (slot > 43) {
                    break;
                }

                List<String> detailLore = new ArrayList<>();
                detailLore.add("");
                detailLore.add(rec.priority().getColor() + msg("recommendations.priority") + " " + rec.priority().name());
                detailLore.add("");
                detailLore.add("<gray>" + msg("recommendations.actions"));
                
                for (String step : rec.steps()) {
                    if (step.length() > 35) {
                        for (String line : wrapText(step, 33)) {
                            detailLore.add("<dark_gray>• " + line);
                        }
                    } else {
                        detailLore.add("<dark_gray>• " + step);
                    }
                }

                Material recMat = switch (rec.priority()) {
                    case HIGH -> Material.REDSTONE;
                    case MEDIUM -> Material.GOLD_INGOT;
                    case LOW -> Material.IRON_INGOT;
                };

                inventory.setItem(slot++, new ItemBuilder(recMat)
                        .setName(rec.priority().getColor() + "📌 " + rec.title())
                        .lore(parseLore(detailLore))
                        .flags(ItemFlag.HIDE_ATTRIBUTES)
                        .build());
            }
        }
    }

    private void addNavigationItems() {
        // Back button
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
                .setName("<yellow>" + msg("nav.back"))
                .addLore("")
                .addLore("<gray>" + msg("nav.back-lore"))
                .build());

        // Issues shortcut
        inventory.setItem(48, new ItemBuilder(Material.PAPER)
                .setName("<red>" + msg("nav.view-issues"))
                .addLore("")
                .addLore("<gray>" + msg("nav.view-issues-lore"))
                .build());

        // Modules shortcut
        inventory.setItem(50, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName("<light_purple>" + msg("nav.view-modules"))
                .addLore("")
                .addLore("<gray>" + msg("nav.view-modules-lore"))
                .build());

        // Refresh
        inventory.setItem(53, new ItemBuilder(Material.SUNFLOWER)
                .setName("<yellow>" + msg("nav.refresh") + "</yellow>")
                .addLore("")
                .addLore("<gray>" + msg("nav.refresh-lore"))
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

        switch (slot) {
            case 30 -> { // Issues
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new IssuesGui(clicker, module).open();
            }
            case 45 -> { // Back
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new PerformanceMainGui(clicker, module).open();
            }
            case 48 -> { // Issues shortcut
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new IssuesGui(clicker, module).open();
            }
            case 50 -> { // Modules shortcut
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                new ModulesAnalysisGui(clicker, module).open();
            }
            case 53 -> { // Refresh
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                new ReportGui(clicker, module).open();
            }
        }
    }

    // ===== HELPER METHODS =====

    private String msg(String key) {
        return module.getMessage("gui.report." + key);
    }

    private List<net.kyori.adventure.text.Component> parseLore(List<String> lore) {
        List<net.kyori.adventure.text.Component> components = new ArrayList<>();
        for (String line : lore) {
            components.add(MessageUtils.parse(line));
        }
        return components;
    }

    private String createScoreBar(int score) {
        int filled = score / 5; // 0-20
        String color = score >= 90 ? "<green>" : score >= 70 ? "<yellow>" : score >= 50 ? "<gold>" : "<red>";
        
        StringBuilder bar = new StringBuilder("<dark_gray>[");
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                bar.append(color).append("█");
            } else {
                bar.append("<dark_gray>░");
            }
        }
        bar.append("<dark_gray>]");
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

    private List<String> wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLength) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                }
            }
            currentLine.append(word).append(" ");
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }

        return lines;
    }
}
