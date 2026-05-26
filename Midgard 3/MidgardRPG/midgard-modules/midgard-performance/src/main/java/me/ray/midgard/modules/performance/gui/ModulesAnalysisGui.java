package me.ray.midgard.modules.performance.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.performance.PerformanceModule;
import me.ray.midgard.modules.performance.spark.MidgardAnalyzer;
import me.ray.midgard.modules.performance.spark.MidgardAnalyzer.*;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GUI para análise detalhada de módulos Midgard.
 * Exibe estatísticas de performance de cada módulo.
 */
public class ModulesAnalysisGui extends BaseGui {

    private static final int ITEMS_PER_PAGE = 21;

    private final PerformanceModule module;
    private final List<ModuleAnalysis> modules;
    private int currentPage = 0;

    public ModulesAnalysisGui(Player player, PerformanceModule module) {
        super(player, 6, module.getMessage("gui.modules.title"));
        this.module = module;
        
        // Carrega módulos ordenados por tempo de inicialização
        var analyzer = MidgardAnalyzer.getInstance();
        if (analyzer != null) {
            var analysis = analyzer.analyze();
            this.modules = new ArrayList<>(analysis.modules());
            this.modules.sort(Comparator.comparingLong(ModuleAnalysis::enableTime).reversed());
        } else {
            this.modules = new ArrayList<>();
        }
    }

    @Override
    public void initializeItems() {
        fillBackground();
        addModuleItems();
        addStatsPanel();
        addNavigationItems();
    }

    private void fillBackground() {
        ItemStack darkPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(" ")
                .build();
        
        ItemStack accentPane = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
                .setName(" ")
                .build();

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, darkPane);
        }

        // Top accent
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, accentPane);
        }

        // Bottom accent
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, accentPane);
        }
    }

    private void addModuleItems() {
        if (modules.isEmpty()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(msg("no-modules.not-found"));
            lore.add("");
            lore.add(msg("no-modules.no-analyzer"));
            lore.add(msg("no-modules.not-initialized"));

            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .setName(msg("no-modules.title"))
                    .lore(parseLore(lore))
                    .build());
            return;
        }

        // Slots disponíveis para módulos (linhas 2-4)
        int[] availableSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, modules.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex >= availableSlots.length) {
                break;
            }

            ModuleAnalysis mod = modules.get(i);
            int slot = availableSlots[slotIndex];

            inventory.setItem(slot, createModuleItem(mod));
        }
    }

    private ItemStack createModuleItem(ModuleAnalysis mod) {
        Material material = mod.enabled() ? Material.COMMAND_BLOCK : Material.STRUCTURE_VOID;
        String statusIcon = mod.enabled() ? "<green>✔" : "<red>✘";
        String healthColor = mod.health().getColor();
        String healthIcon = mod.health().getIcon();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(msg("module-item.status-label") + statusIcon + " " + (mod.enabled() ? msg("module-item.active") : msg("module-item.inactive")));
        lore.add(msg("module-item.health-label") + healthColor + healthIcon + " " + mod.health().name());
        lore.add("");
        lore.add("<dark_gray>═══════════════════");
        lore.add("");
        
        // Tempo de inicialização
        String timeColor = getTimeColor(mod.enableTime());
        lore.add(msg("module-item.init-time") + timeColor + mod.enableTime() + "ms");
        
        // Operações
        lore.add(msg("module-item.operations") + mod.totalOperations());
        lore.add(msg("module-item.listeners") + mod.listenerCount());
        
        // Tempo total de profiling
        if (mod.totalTime() > 0) {
            String profileColor = getTimeColor(mod.totalTime());
            lore.add(msg("module-item.profiled-time") + profileColor + mod.totalTime() + "ms");
        }

        // Top 3 operações lentas
        if (!mod.operations().isEmpty()) {
            lore.add("");
            lore.add(msg("module-item.slowest-ops"));
            
            var slowOps = mod.operations().stream()
                    .sorted(Comparator.comparingLong(OperationAnalysis::maxTime).reversed())
                    .limit(3)
                    .toList();
            
            for (var op : slowOps) {
                String opColor = op.severity().getColor();
                String opIcon = op.severity().getIcon();
                String shortName = shortenOperationName(op.name());
                lore.add("  " + opColor + opIcon + " <gray>" + shortName + ": <white>" + op.maxTime() + "ms");
            }
        }

        return new ItemBuilder(material)
                .setName(healthColor + mod.name())
                .lore(parseLore(lore))
                .glowIf(mod.health().ordinal() >= me.ray.midgard.modules.performance.spark.SparkPerformanceManager.HealthLevel.WARNING.ordinal())
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private void addStatsPanel() {
        var analyzer = MidgardAnalyzer.getInstance();
        if (analyzer == null) {
            return;
        }

        var analysis = analyzer.analyze();

        // Summary stats in slot 4
        int totalModules = modules.size();
        long enabledModules = modules.stream().filter(ModuleAnalysis::enabled).count();
        long totalOps = analysis.profiler().totalExecutions();
        long totalTime = analysis.profiler().totalTime();

        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("");
        summaryLore.add(msg("summary.description"));
        summaryLore.add("");
        summaryLore.add(msg("summary.active-modules").replace("{0}", String.valueOf(enabledModules)).replace("{1}", String.valueOf(totalModules)));
        summaryLore.add(msg("summary.total-operations").replace("{0}", String.valueOf(totalOps)));
        summaryLore.add(msg("summary.total-time").replace("{0}", String.valueOf(totalTime)));
        summaryLore.add(msg("summary.total-listeners").replace("{0}", String.valueOf(analysis.events().totalListeners())));
        summaryLore.add("");
        
        // Warnings
        long slowModules = modules.stream()
                .filter(m -> m.enableTime() > 500)
                .count();
        
        if (slowModules > 0) {
            summaryLore.add(msg("summary.slow-init").replace("{0}", String.valueOf(slowModules)));
        }
        
        long criticalOps = modules.stream()
                .flatMap(m -> m.operations().stream())
                .filter(op -> op.severity() == Severity.CRITICAL || op.severity() == Severity.SEVERE)
                .count();
        
        if (criticalOps > 0) {
            summaryLore.add(msg("summary.critical-ops").replace("{0}", String.valueOf(criticalOps)));
        }

        inventory.setItem(4, new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName(msg("summary.title"))
                .lore(parseLore(summaryLore))
                .glow()
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    private void addNavigationItems() {
        // Back button
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
                .setName(msg("navigation.back"))
                .addLore("")
                .addLore(msg("navigation.back-lore"))
                .build());

        // Page info
        int totalPages = Math.max(1, (int) Math.ceil(modules.size() / (double) ITEMS_PER_PAGE));
        List<String> pageLore = new ArrayList<>();
        pageLore.add("");
        pageLore.add(msg("navigation.page-lore").replace("{0}", String.valueOf(currentPage + 1)).replace("{1}", String.valueOf(totalPages)));
        pageLore.add("");
        pageLore.add(msg("navigation.total-modules").replace("{0}", String.valueOf(modules.size())));

        inventory.setItem(49, new ItemBuilder(Material.PAPER)
                .setName(msg("navigation.page-info").replace("{0}", String.valueOf(currentPage + 1)).replace("{1}", String.valueOf(totalPages)))
                .lore(parseLore(pageLore))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        // Previous page
        if (currentPage > 0) {
            inventory.setItem(48, new ItemBuilder(Material.SPECTRAL_ARROW)
                    .setName(msg("navigation.previous"))
                    .addLore("")
                    .addLore(msg("navigation.go-to-page").replace("{0}", String.valueOf(currentPage)))
                    .build());
        }

        // Next page
        if ((currentPage + 1) * ITEMS_PER_PAGE < modules.size()) {
            inventory.setItem(50, new ItemBuilder(Material.SPECTRAL_ARROW)
                    .setName(msg("navigation.next"))
                    .addLore("")
                    .addLore(msg("navigation.go-to-page").replace("{0}", String.valueOf(currentPage + 2)))
                    .build());
        }

        // Refresh
        inventory.setItem(53, new ItemBuilder(Material.SUNFLOWER)
                .setName(msg("navigation.refresh"))
                .addLore("")
                .addLore(msg("navigation.refresh-lore"))
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
            case 45 -> { // Back
                clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                new PerformanceMainGui(clicker, module).open();
            }
            case 48 -> { // Previous page
                if (currentPage > 0) {
                    clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    currentPage--;
                    initializeItems();
                }
            }
            case 50 -> { // Next page
                if ((currentPage + 1) * ITEMS_PER_PAGE < modules.size()) {
                    clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                    currentPage++;
                    initializeItems();
                }
            }
            case 53 -> { // Refresh
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                // Recarrega dados
                var analyzer = MidgardAnalyzer.getInstance();
                if (analyzer != null) {
                    var analysis = analyzer.analyze();
                    modules.clear();
                    modules.addAll(analysis.modules());
                    modules.sort(Comparator.comparingLong(ModuleAnalysis::enableTime).reversed());
                }
                initializeItems();
            }
        }
    }

    // ===== HELPER METHODS =====

    private String msg(String key) {
        return module.getMessage("gui.modules." + key);
    }

    private List<net.kyori.adventure.text.Component> parseLore(List<String> lore) {
        List<net.kyori.adventure.text.Component> components = new ArrayList<>();
        for (String line : lore) {
            components.add(MessageUtils.parse(line));
        }
        return components;
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

    private String shortenOperationName(String name) {
        // Encurta nomes longos de operações
        if (name.length() <= 25) {
            return name;
        }
        
        // Tenta extrair a parte mais relevante
        if (name.contains(":")) {
            String[] parts = name.split(":");
            return parts[parts.length - 1];
        }
        
        return name.substring(0, 22) + "...";
    }
}
