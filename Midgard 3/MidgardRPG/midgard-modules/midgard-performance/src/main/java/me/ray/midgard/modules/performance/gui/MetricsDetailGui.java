package me.ray.midgard.modules.performance.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.performance.PerformanceModule;
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
 * GUI detalhada para cada tipo de métrica.
 * Permite visualização aprofundada de TPS, MSPT, Memory, CPU, GC e Diagnóstico.
 */
public class MetricsDetailGui extends BaseGui {

    public enum MetricType {
        TPS("⏱ TPS", Material.CLOCK),
        MSPT("⚡ MSPT", Material.LIGHTNING_ROD),
        MEMORY("💾 Memória", Material.EMERALD_BLOCK),
        CPU("💻 CPU", Material.REDSTONE_TORCH),
        GC("🗑 GC", Material.HOPPER),
        DIAGNOSE("🩺 Diagnóstico", Material.GOLDEN_APPLE);

        private final String title;
        private final Material icon;

        MetricType(String title, Material icon) {
            this.title = title;
            this.icon = icon;
        }

        public String getTitle() { return title; }
        public Material getIcon() { return icon; }
    }

    private final PerformanceModule module;
    private final MetricType type;

    public MetricsDetailGui(Player player, PerformanceModule module, MetricType type) {
        super(player, 6, "<yellow>" + type.getTitle() + "</yellow>");
        this.module = module;
        this.type = type;
    }

    private String msg(String key) {
        return module.getMessage("gui.metrics." + key);
    }

    @Override
    public void initializeItems() {
        fillBackground();
        
        switch (type) {
            case TPS -> buildTPSView();
            case MSPT -> buildMSPTView();
            case MEMORY -> buildMemoryView();
            case CPU -> buildCPUView();
            case GC -> buildGCView();
            case DIAGNOSE -> buildDiagnoseView();
        }

        addNavigationItems();
    }

    private void fillBackground() {
        ItemStack darkPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        
        ItemStack accentPane = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, darkPane);
        }

        // Top accent line
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, accentPane);
        }
    }

    // ===== TPS VIEW =====
    private void buildTPSView() {
        if (!SparkPerformanceManager.isAvailable()) {
            addSparkWarning();
            return;
        }

        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var tps = metrics.tps();

        if (!tps.available()) {
            addUnavailableMessage("TPS");
            return;
        }

        // Main TPS display (center)
        double currentTps = tps.last5s();
        String tpsColor = getTpsColor(currentTps);

        List<String> mainLore = new ArrayList<>();
        mainLore.add("");
        mainLore.add(msg("tps.current-lore"));
        mainLore.add("");
        mainLore.add(tpsColor + "<bold>" + String.format("%.2f", currentTps) + " TPS</bold>");
        mainLore.add("");
        mainLore.add(createLargeProgressBar(currentTps / 20.0, tpsColor));
        mainLore.add("");
        mainLore.add(msg("tps.target"));

        inventory.setItem(13, new ItemBuilder(getTpsMaterial(currentTps))
                .setName(tpsColor + msg("tps.current-name"))
                .lore(parseLore(mainLore))
                .glow()
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // Time windows
        int[] slots = {29, 30, 31, 32, 33};
        double[] values = {tps.last5s(), tps.last10s(), tps.last1m(), tps.last5m(), tps.last15m()};
        String[] labels = {msg("time.5s"), msg("time.10s"), msg("time.1m"), msg("time.5m"), msg("time.15m")};
        Material[] materials = {Material.CLOCK, Material.CLOCK, Material.CLOCK, Material.CLOCK, Material.CLOCK};

        for (int i = 0; i < 5; i++) {
            String color = getTpsColor(values[i]);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(msg("tps.window-lore"));
            lore.add("<white>" + labels[i]);
            lore.add("");
            lore.add(color + String.format("%.2f", values[i]) + " TPS");
            lore.add("");
            lore.add(createProgressBar(values[i] / 20.0, color));

            inventory.setItem(slots[i], new ItemBuilder(materials[i])
                    .setName("<yellow>⏱ " + labels[i])
                    .lore(parseLore(lore))
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }

        // Info panel
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(msg("tps.info-desc1"));
        infoLore.add(msg("tps.info-desc2"));
        infoLore.add(msg("tps.info-desc3"));
        infoLore.add("");
        infoLore.add(msg("tps.info-excellent"));
        infoLore.add(msg("tps.info-good"));
        infoLore.add(msg("tps.info-moderate"));
        infoLore.add(msg("tps.info-severe"));

        inventory.setItem(22, new ItemBuilder(Material.BOOK)
                .setName(msg("tps.info-name"))
                .lore(parseLore(infoLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    // ===== MSPT VIEW =====
    private void buildMSPTView() {
        if (!SparkPerformanceManager.isAvailable()) {
            addSparkWarning();
            return;
        }

        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var mspt = metrics.mspt();

        if (!mspt.available()) {
            addUnavailableMessage("MSPT");
            return;
        }

        var last10s = mspt.last10s();
        var last1m = mspt.last1m();

        // Main MSPT (median)
        double medianMspt = last10s.median();
        String msptColor = getMsptColor(medianMspt);

        List<String> mainLore = new ArrayList<>();
        mainLore.add("");
        mainLore.add(msg("mspt.current-lore"));
        mainLore.add("");
        mainLore.add(msptColor + "<bold>" + String.format("%.2f", medianMspt) + "ms</bold>");
        mainLore.add("");
        mainLore.add(createLargeProgressBar(1 - (medianMspt / 50.0), msptColor));
        mainLore.add("");
        mainLore.add(msg("mspt.limit"));

        inventory.setItem(13, new ItemBuilder(Material.LIGHTNING_ROD)
                .setName(msptColor + msg("mspt.current-name"))
                .lore(parseLore(mainLore))
                .glow()
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // 10 seconds window details
        String[] metricNames = {msg("mspt.stat-min"), msg("mspt.stat-median"), msg("mspt.stat-p95"), msg("mspt.stat-max")};
        double[] values10s = {last10s.min(), last10s.median(), last10s.p95(), last10s.max()};
        Material[] mats = {Material.LIME_CONCRETE, Material.YELLOW_CONCRETE, Material.ORANGE_CONCRETE, Material.RED_CONCRETE};
        int[] slots10s = {28, 29, 30, 31};

        for (int i = 0; i < 4; i++) {
            String color = getMsptColor(values10s[i]);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(msg("mspt.window-10s"));
            lore.add("");
            lore.add(color + String.format("%.2f", values10s[i]) + "ms");

            inventory.setItem(slots10s[i], new ItemBuilder(mats[i])
                    .setName("<yellow>" + metricNames[i] + " (10s)")
                    .lore(parseLore(lore))
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }

        // 1 minute window details
        double[] values1m = {last1m.min(), last1m.median(), last1m.p95(), last1m.max()};
        int[] slots1m = {33, 34, 35, 36};

        for (int i = 0; i < 4; i++) {
            String color = getMsptColor(values1m[i]);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(msg("mspt.window-1m"));
            lore.add("");
            lore.add(color + String.format("%.2f", values1m[i]) + "ms");

            inventory.setItem(slots1m[i], new ItemBuilder(mats[i])
                    .setName("<gold>" + metricNames[i] + " (1m)")
                    .lore(parseLore(lore))
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }

        // Info panel
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(msg("mspt.info-desc1"));
        infoLore.add(msg("mspt.info-desc2"));
        infoLore.add(msg("mspt.info-desc3"));
        infoLore.add("");
        infoLore.add(msg("mspt.info-excellent"));
        infoLore.add(msg("mspt.info-good"));
        infoLore.add(msg("mspt.info-moderate"));
        infoLore.add(msg("mspt.info-severe"));

        inventory.setItem(22, new ItemBuilder(Material.BOOK)
                .setName(msg("mspt.info-name"))
                .lore(parseLore(infoLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    // ===== MEMORY VIEW =====
    private void buildMemoryView() {
        if (!SparkPerformanceManager.isAvailable()) {
            addSparkWarning();
            return;
        }

        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var mem = metrics.memory();

        if (!mem.available()) {
            addUnavailableMessage("Memory");
            return;
        }

        double percent = mem.usedPercent();
        String memColor = getMemoryColor(percent);

        // Main memory display
        List<String> mainLore = new ArrayList<>();
        mainLore.add("");
        mainLore.add(msg("memory.current-lore"));
        mainLore.add("");
        mainLore.add(memColor + "<bold>" + String.format("%.1f%%", percent) + "</bold>");
        mainLore.add("");
        mainLore.add(createLargeProgressBar(percent / 100.0, memColor));
        mainLore.add("");
        mainLore.add(memColor + mem.usedMB() + "MB <gray>/ <white>" + mem.maxMB() + "MB");

        Material memMat = percent > 85 ? Material.REDSTONE_BLOCK : 
                          percent > 70 ? Material.GOLD_BLOCK : Material.EMERALD_BLOCK;

        inventory.setItem(13, new ItemBuilder(memMat)
                .setName(memColor + msg("memory.current-name"))
                .lore(parseLore(mainLore))
                .glow()
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // Memory breakdown
        List<String> usedLore = new ArrayList<>();
        usedLore.add("");
        usedLore.add(msg("memory.used-lore"));
        usedLore.add("");
        usedLore.add("<red>" + mem.usedMB() + " MB");

        inventory.setItem(29, new ItemBuilder(Material.RED_CONCRETE)
                .setName(msg("memory.used-name"))
                .lore(parseLore(usedLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        List<String> freeLore = new ArrayList<>();
        freeLore.add("");
        freeLore.add(msg("memory.free-lore"));
        freeLore.add("");
        freeLore.add("<green>" + mem.freeMB() + " MB");

        inventory.setItem(30, new ItemBuilder(Material.LIME_CONCRETE)
                .setName(msg("memory.free-name"))
                .lore(parseLore(freeLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        List<String> allocLore = new ArrayList<>();
        allocLore.add("");
        allocLore.add(msg("memory.allocated-lore"));
        allocLore.add("");
        allocLore.add("<aqua>" + mem.totalMB() + " MB");

        inventory.setItem(32, new ItemBuilder(Material.LIGHT_BLUE_CONCRETE)
                .setName(msg("memory.allocated-name"))
                .lore(parseLore(allocLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        List<String> maxLore = new ArrayList<>();
        maxLore.add("");
        maxLore.add(msg("memory.max-lore"));
        maxLore.add("");
        maxLore.add("<white>" + mem.maxMB() + " MB");

        inventory.setItem(33, new ItemBuilder(Material.WHITE_CONCRETE)
                .setName(msg("memory.max-name"))
                .lore(parseLore(maxLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // GC shortcut
        var gc = metrics.gc();
        if (gc.available()) {
            List<String> gcLore = new ArrayList<>();
            gcLore.add("");
            gcLore.add(msg("memory.gc-collections") + gc.totalCollections());
            gcLore.add(msg("memory.gc-time") + gc.formatTime());
            gcLore.add("");
            gcLore.add(msg("memory.gc-click"));

            inventory.setItem(40, new ItemBuilder(Material.HOPPER)
                    .setName(msg("memory.gc-name"))
                    .lore(parseLore(gcLore))
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }
    }

    // ===== CPU VIEW =====
    private void buildCPUView() {
        if (!SparkPerformanceManager.isAvailable()) {
            addSparkWarning();
            return;
        }

        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var cpu = metrics.cpu();

        if (!cpu.available()) {
            addUnavailableMessage("CPU");
            return;
        }

        var process = cpu.process();
        var system = cpu.system();

        double processPercent = process.seconds10() * 100;
        String cpuColor = getCpuColor(processPercent);

        // Main CPU display
        List<String> mainLore = new ArrayList<>();
        mainLore.add("");
        mainLore.add(msg("cpu.current-lore"));
        mainLore.add("");
        mainLore.add(cpuColor + "<bold>" + String.format("%.1f%%", processPercent) + "</bold>");
        mainLore.add("");
        mainLore.add(createLargeProgressBar(processPercent / 100.0, cpuColor));

        inventory.setItem(13, new ItemBuilder(Material.REDSTONE_TORCH)
                .setName(cpuColor + msg("cpu.current-name"))
                .lore(parseLore(mainLore))
                .glow()
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // Process CPU windows
        String[] labels = {msg("time.10s"), msg("time.1m"), msg("time.15m")};
        double[] processValues = {process.seconds10() * 100, process.minutes1() * 100, process.minutes15() * 100};
        int[] processSlots = {28, 29, 30};

        for (int i = 0; i < 3; i++) {
            String color = getCpuColor(processValues[i]);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(msg("cpu.process-avg"));
            lore.add("<white>" + labels[i]);
            lore.add("");
            lore.add(color + String.format("%.1f%%", processValues[i]));
            lore.add("");
            lore.add(createProgressBar(processValues[i] / 100.0, color));

            inventory.setItem(processSlots[i], new ItemBuilder(Material.COMPARATOR)
                    .setName(msg("cpu.process-label") + " <dark_gray>- " + labels[i] + "</dark_gray>")
                    .lore(parseLore(lore))
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }

        // System CPU windows
        double[] systemValues = {system.seconds10() * 100, system.minutes1() * 100, system.minutes15() * 100};
        int[] systemSlots = {32, 33, 34};

        for (int i = 0; i < 3; i++) {
            String color = getCpuColor(systemValues[i]);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(msg("cpu.system-avg"));
            lore.add("<white>" + labels[i]);
            lore.add("");
            lore.add(color + String.format("%.1f%%", systemValues[i]));
            lore.add("");
            lore.add(createProgressBar(systemValues[i] / 100.0, color));

            inventory.setItem(systemSlots[i], new ItemBuilder(Material.REPEATER)
                    .setName(msg("cpu.system-label") + " <dark_gray>- " + labels[i] + "</dark_gray>")
                    .lore(parseLore(lore))
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }
    }

    // ===== GC VIEW =====
    private void buildGCView() {
        if (!SparkPerformanceManager.isAvailable()) {
            addSparkWarning();
            return;
        }

        var metrics = SparkPerformanceManager.getInstance().getMetrics();
        var gc = metrics.gc();

        if (!gc.available()) {
            addUnavailableMessage("Garbage Collector");
            return;
        }

        // Main GC display
        List<String> mainLore = new ArrayList<>();
        mainLore.add("");
        mainLore.add(msg("gc.main-lore"));
        mainLore.add("");
        mainLore.add(msg("gc.total-collections") + gc.totalCollections());
        mainLore.add(msg("gc.total-time") + gc.formatTime());
        mainLore.add(msg("gc.avg-time") + String.format("%.2f", gc.avgTime()) + "ms");
        mainLore.add("");
        
        double freq = gc.avgFrequency() / 1000.0;
        String freqColor = freq < 10 ? "<red>" : freq < 30 ? "<yellow>" : "<green>";
        mainLore.add(msg("gc.frequency") + freqColor + String.format("%.1f", freq) + msg("gc.frequency-suffix"));

        inventory.setItem(13, new ItemBuilder(Material.HOPPER)
                .setName(msg("gc.main-name"))
                .lore(parseLore(mainLore))
                .glow()
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // Individual collectors
        var collectors = gc.collectors();
        if (!collectors.isEmpty()) {
            int slot = 29;
            for (var entry : collectors.entrySet()) {
                if (slot > 35) {
                    break;
                }
                
                var collector = entry.getValue();
                List<String> collectorLore = new ArrayList<>();
                collectorLore.add("");
                collectorLore.add(msg("gc.collector-lore"));
                collectorLore.add("");
                collectorLore.add(msg("gc.collector-collections") + collector.totalCollections());
                collectorLore.add(msg("gc.collector-total-time") + collector.totalTime() + "ms");
                collectorLore.add(msg("gc.collector-avg-time") + String.format("%.2f", collector.avgTime()) + "ms");

                inventory.setItem(slot++, new ItemBuilder(Material.MINECART)
                        .setName("<yellow>" + entry.getKey())
                        .lore(parseLore(collectorLore))
                        .flags(ItemFlag.HIDE_ATTRIBUTES)
                        .build());
            }
        }

        // Info panel
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(msg("gc.info-desc1"));
        infoLore.add(msg("gc.info-desc2"));
        infoLore.add("");
        infoLore.add(msg("gc.info-normal"));
        infoLore.add(msg("gc.info-moderate"));
        infoLore.add(msg("gc.info-pressure"));

        inventory.setItem(22, new ItemBuilder(Material.BOOK)
                .setName(msg("gc.info-name"))
                .lore(parseLore(infoLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    // ===== DIAGNOSE VIEW =====
    private void buildDiagnoseView() {
        if (!SparkPerformanceManager.isAvailable()) {
            addSparkWarning();
            return;
        }

        var diagnosis = SparkPerformanceManager.getInstance().diagnose();

        // Overall health
        HealthLevel overall = diagnosis.overallHealth();
        List<String> overallLore = new ArrayList<>();
        overallLore.add("");
        overallLore.add(msg("diagnose.health-lore"));
        overallLore.add("");
        overallLore.add(overall.getColor() + overall.getIcon() + " " + overall.getLabel());

        inventory.setItem(13, new ItemBuilder(getHealthMaterial(overall))
                .setName(overall.getColor() + msg("diagnose.health-name"))
                .lore(parseLore(overallLore))
                .glow()
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // Individual diagnostics
        HealthIssue[] issues = {diagnosis.tps(), diagnosis.mspt(), diagnosis.cpu(), diagnosis.memory(), diagnosis.gc()};
        String[] names = {msg("diagnose.comp-tps"), msg("diagnose.comp-mspt"), msg("diagnose.comp-cpu"), msg("diagnose.comp-memory"), msg("diagnose.comp-gc")};
        Material[] mats = {Material.CLOCK, Material.LIGHTNING_ROD, Material.REDSTONE_TORCH, Material.EMERALD_BLOCK, Material.HOPPER};
        int[] slots = {29, 30, 31, 32, 33};

        for (int i = 0; i < 5; i++) {
            HealthIssue issue = issues[i];
            List<String> issueLore = new ArrayList<>();
            issueLore.add("");
            issueLore.add(issue.getColor() + issue.getIcon() + " " + issue.message());
            issueLore.add("");
            issueLore.add(msg("diagnose.component") + issue.category());
            issueLore.add(msg("diagnose.level") + issue.level().getColor() + issue.level().getLabel());

            inventory.setItem(slots[i], new ItemBuilder(mats[i])
                    .setName(issue.getColor() + names[i])
                    .lore(parseLore(issueLore))
                    .glowIf(issue.level().ordinal() >= HealthLevel.WARNING.ordinal())
                    .flags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
        }
    }

    private void addNavigationItems() {
        // Back button
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
                .setName(msg("nav.back"))
                .addLore("")
                .addLore(msg("nav.back-lore"))
                .build());

        // Refresh
        inventory.setItem(53, new ItemBuilder(Material.SUNFLOWER)
                .setName(msg("nav.refresh"))
                .addLore("")
                .addLore(msg("nav.refresh-lore"))
                .build());
    }

    private void addSparkWarning() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(msg("spark.not-detected"));
        lore.add("");
        lore.add(msg("spark.requires1"));
        lore.add(msg("spark.requires2"));
        lore.add("");
        lore.add(msg("spark.download"));

        inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .setName(msg("spark.warning-name"))
                .lore(parseLore(lore))
                .glow()
                .build());
    }

    private void addUnavailableMessage(String metric) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(msg("unavailable.not-available").replace("{metric}", metric));
        lore.add("");
        lore.add(msg("unavailable.not-collected1"));
        lore.add(msg("unavailable.not-collected2"));

        inventory.setItem(22, new ItemBuilder(Material.GRAY_DYE)
                .setName(msg("unavailable.name"))
                .lore(parseLore(lore))
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
            case 40 -> { // GC shortcut from memory view
                if (type == MetricType.MEMORY) {
                    clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    new MetricsDetailGui(clicker, module, MetricType.GC).open();
                }
            }
            case 45 -> { // Back
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new PerformanceMainGui(clicker, module).open();
            }
            case 53 -> { // Refresh
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                initializeItems();
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

    private String createLargeProgressBar(double percent, String color) {
        percent = Math.max(0, Math.min(1, percent));
        int filled = (int) (percent * 30);
        StringBuilder bar = new StringBuilder("<dark_gray>▐");
        for (int i = 0; i < 30; i++) {
            if (i < filled) {
                bar.append(color).append("█");
            } else {
                bar.append("<dark_gray>░");
            }
        }
        bar.append("<dark_gray>▌");
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
}
