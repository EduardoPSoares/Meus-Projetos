package me.ray.midgard.modules.races.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.races.RacesModule;
import me.ray.midgard.modules.races.data.RaceData;
import me.ray.midgard.modules.races.model.EvolutionRequirement;
import me.ray.midgard.modules.races.model.Race;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI de Evolução de Linhagem — Árvore Visual
 *
 * Layout 6 linhas (54 slots):
 *   Row 0 (0–8):   Cabeçalho decorativo (roxo/magenta)
 *   Row 1 (9–17):  Ancestrais (acentos roxos nas bordas)
 *   Row 2 (18–26): Conectores visuais ↓
 *   Row 3 (27–35): Raça atual (destaque cyan)
 *   Row 4 (36–44): Evoluções disponíveis
 *   Row 5 (45–53): Navegação
 */
public class RaceEvolutionGui extends BaseGui {

    private final RacesModule module;
    private final Race currentRace;
    private final BaseGui parentGui;
    private final MidgardProfile profile;
    private final RaceData raceData;

    private List<Race> cachedEvolutions;
    private List<Race> ancestorChain;

    // ─── Slots fixos ─────────────────────────────────────────────────
    private static final int SLOT_ANCESTOR_ROOT  = 13;
    private static final int[] ANCESTOR_SLOTS    = {11, 12, 13, 14, 15};
    private static final int SLOT_CONNECTOR_CENTER = 22;
    private static final int SLOT_CURRENT        = 31;
    private static final int[] EVOLUTION_SLOTS   = {37, 38, 39, 40, 41, 42, 43};

    // Navegação (linha 5)
    private static final int SLOT_BACK  = 45;
    private static final int SLOT_INFO  = 49;
    private static final int SLOT_CLOSE = 53;

    public RaceEvolutionGui(Player player, Race currentRace, BaseGui parentGui) {
        super(player, 6, getTitle());
        if (currentRace == null) {
            throw new IllegalArgumentException("currentRace cannot be null");
        }
        this.module = RacesModule.getInstance();
        this.currentRace = currentRace;
        this.parentGui = parentGui;
        this.profile = MidgardCore.getProfileManager().getProfile(player);
        this.raceData = (profile != null) ? profile.getData(RaceData.class) : null;
    }

    private static String getTitle() {
        return RacesModule.getInstance().getGuiMessage("evolution.title");
    }

    private String gui(String key) {
        return module.getGuiMessage("evolution." + key);
    }

    @Override
    public void initializeItems() {
        inventory.clear();

        // ── Row 0: Cabeçalho decorativo ──────────────────────────────
        var purple = RaceGuiTheme.createPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
        var magenta = RaceGuiTheme.createPane(Material.MAGENTA_STAINED_GLASS_PANE, " ");
        // Cantos roxos, centro magenta
        for (int i = 0; i <= 8; i++) {
            inventory.setItem(i, (i == 0 || i == 8 || i == 1 || i == 7) ? purple : magenta);
        }

        // ── Row 1 (9-17): Ancestrais ─────────────────────────────────
        var darkPane = RaceGuiTheme.createDarkPane();
        var purpleAccent = RaceGuiTheme.createPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
        inventory.setItem(9, purpleAccent);
        inventory.setItem(10, purpleAccent);
        inventory.setItem(16, purpleAccent);
        inventory.setItem(17, purpleAccent);

        ancestorChain = buildAncestorChain(currentRace);
        displayAncestors();

        // ── Row 2 (18-26): Conectores ────────────────────────────────
        for (int i = 18; i <= 26; i++) {
            inventory.setItem(i, darkPane);
        }
        inventory.setItem(SLOT_CONNECTOR_CENTER, createConnector());

        // ── Row 3 (27-35): Raça atual ────────────────────────────────
        var cyanAccent = RaceGuiTheme.createPane(Material.CYAN_STAINED_GLASS_PANE, " ");
        inventory.setItem(27, darkPane);
        inventory.setItem(28, cyanAccent);
        inventory.setItem(29, cyanAccent);
        inventory.setItem(30, cyanAccent);
        inventory.setItem(31, createCurrentRaceItem()); // SLOT_CURRENT
        inventory.setItem(32, cyanAccent);
        inventory.setItem(33, cyanAccent);
        inventory.setItem(34, cyanAccent);
        inventory.setItem(35, darkPane);

        // ── Row 4 (36-44): Evoluções ─────────────────────────────────
        for (int i = 36; i <= 44; i++) {
            inventory.setItem(i, darkPane);
        }

        cachedEvolutions = module.getRaceManager().getRaces().stream()
                .filter(r -> r.isSubRace() && r.getParentRace().equals(currentRace.getId()))
                .toList();

        displayConnectorsBelowCurrent();

        if (cachedEvolutions.isEmpty()) {
            inventory.setItem(40, createNoEvolutionsItem());
        } else {
            displayEvolutions();
        }

        // ── Row 5 (45-53): Navegação ─────────────────────────────────
        for (int i = 45; i <= 53; i++) {
            inventory.setItem(i, darkPane);
        }
        displayNavigation();
    }

    // ─── Árvore de Ancestrais ────────────────────────────────────────

    private List<Race> buildAncestorChain(Race race) {
        List<Race> chain = new ArrayList<>();
        Race current = race;
        int maxDepth = 10;
        while (current.isSubRace() && maxDepth-- > 0) {
            Race parent = module.getRaceManager().getRace(current.getParentRace());
            if (parent == null) { break; }
            chain.addFirst(parent);
            current = parent;
        }
        return chain;
    }

    private void displayAncestors() {
        if (ancestorChain.isEmpty()) {
            // Sem ancestrais — raça raiz
            inventory.setItem(SLOT_ANCESTOR_ROOT, createAncestorItem(null, true));
            return;
        }

        // Centralizar ancestrais nos slots disponíveis
        int count = Math.min(ancestorChain.size(), ANCESTOR_SLOTS.length);
        int startIndex = (ANCESTOR_SLOTS.length - count) / 2;

        for (int i = 0; i < count; i++) {
            Race ancestor = ancestorChain.get(ancestorChain.size() - count + i);
            boolean isRoot = (i == 0 && ancestorChain.size() <= ANCESTOR_SLOTS.length);
            inventory.setItem(ANCESTOR_SLOTS[startIndex + i], createAncestorItem(ancestor, isRoot));
        }
    }

    private ItemStack createAncestorItem(Race ancestor, boolean isRoot) {
        if (ancestor == null) {
            return new ItemBuilder(Material.OAK_SAPLING)
                    .setName(gui("root.name").replace("%race%", currentRace.getDisplayName()))
                    .setLoreMultiline(gui("root.lore"))
                    .build();
        }

        String nameKey = isRoot ? "ancestor_root" : "ancestor";
        return new ItemBuilder(ancestor.getIcon())
                .setName(gui(nameKey + ".name").replace("%race%", ancestor.getDisplayName()))
                .setLoreMultiline(gui(nameKey + ".lore").replace("%race%", ancestor.getDisplayName()))
                .build();
    }

    private ItemStack createConnector() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName("<dark_gray>▼")
                .build();
    }

    private void displayConnectorsBelowCurrent() {
        if (cachedEvolutions == null || cachedEvolutions.isEmpty()) { return; }

        int count = Math.min(cachedEvolutions.size(), EVOLUTION_SLOTS.length);
        int startIndex = (EVOLUTION_SLOTS.length - count) / 2;

        ItemStack downConnector = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName("<dark_gray>▼")
                .build();

        for (int i = 0; i < count; i++) {
            int evoSlot = EVOLUTION_SLOTS[startIndex + i];
            int connectorSlot = evoSlot - 9; // linha acima (row 3)
            if (connectorSlot != SLOT_CURRENT) {
                inventory.setItem(connectorSlot, downConnector);
            }
        }
    }

    // ─── Raça Atual ──────────────────────────────────────────────────

    private ItemStack createCurrentRaceItem() {
        int level = (raceData != null) ? raceData.getLevel() : 1;
        double xp = (raceData != null) ? raceData.getExperience() : 0;
        double requiredXp = module.getLevelManager().getRequiredExperience(level);
        double percent = (requiredXp > 0) ? (xp / requiredXp) * 100 : 100;
        String progressBar = RaceGuiTheme.progressBar(percent, 10, "22c55e", "a855f7");

        String lore = (gui("current.name") + "\n" + gui("current.lore"))
                .replace("%race%", currentRace.getDisplayName())
                .replace("%level%", String.valueOf(level))
                .replace("%xp%", String.format("%.0f", xp))
                .replace("%required%", String.format("%.0f", requiredXp))
                .replace("%bar%", progressBar);

        return new ItemBuilder(currentRace.getIcon())
                .setName("<gradient:#a855f7:#ec4899><bold>" + currentRace.getDisplayName())
                .setLoreMultiline(lore)
                .glow()
                .build();
    }

    // ─── Evoluções ───────────────────────────────────────────────────

    private ItemStack createNoEvolutionsItem() {
        return new ItemBuilder(Material.GRAY_DYE)
                .setName(gui("no_evolutions.name").replace("%race%", currentRace.getDisplayName()))
                .setLoreMultiline(gui("no_evolutions.lore"))
                .build();
    }

    private void displayEvolutions() {
        int count = Math.min(cachedEvolutions.size(), EVOLUTION_SLOTS.length);
        int startIndex = (EVOLUTION_SLOTS.length - count) / 2;

        for (int i = 0; i < count; i++) {
            Race evolution = cachedEvolutions.get(i);
            inventory.setItem(EVOLUTION_SLOTS[startIndex + i], createEvolutionItem(evolution));
        }
    }

    private ItemStack createEvolutionItem(Race evolution) {
        boolean requirementsMet = module.getRaceManager().checkEvolutionRequirements(player, evolution);
        boolean branchLocked = isBranchLocked(evolution);

        if (branchLocked) {
            return new ItemBuilder(Material.BARRIER)
                    .setName(gui("branch_locked.name").replace("%race%", evolution.getDisplayName()))
                    .setLoreMultiline(gui("branch_locked.lore")
                            .replace("%branch%", evolution.getExclusionBranch() != null ? evolution.getExclusionBranch() : ""))
                    .build();
        }

        if (requirementsMet) {
            String description = getShortDescription(evolution);
            int abilities = (evolution.getTraits() != null) ? evolution.getTraits().size() : 0;
            int attributes = (evolution.getAttributes() != null) ? evolution.getAttributes().size() : 0;

            return new ItemBuilder(evolution.getIcon())
                    .setName(gui("evolution_available.name").replace("%race%", evolution.getDisplayName()))
                    .setLoreMultiline(gui("evolution_available.lore")
                            .replace("%description%", description)
                            .replace("%abilities%", String.valueOf(abilities))
                            .replace("%attributes%", String.valueOf(attributes))
                            .replace("%requirements%", buildRequirementsLore(evolution, true)))
                    .glow()
                    .build();
        } else {
            return new ItemBuilder(Material.GRAY_DYE)
                    .setName(gui("evolution_locked.name").replace("%race%", evolution.getDisplayName()))
                    .setLoreMultiline(gui("evolution_locked.lore")
                            .replace("%requirements%", buildRequirementsLore(evolution, false)))
                    .build();
        }
    }

    private boolean isBranchLocked(Race evolution) {
        if (evolution.getExclusionBranch() == null || raceData == null) { return false; }
        for (Race sibling : module.getRaceManager().getRaces()) {
            if (sibling.getId().equals(evolution.getId())) { continue; }
            if (!sibling.isSubRace() || !sibling.getParentRace().equals(currentRace.getId())) { continue; }
            if (evolution.getExclusionBranch().equals(sibling.getExclusionBranch())) {
                if (raceData.getRaceHistory().contains(sibling.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String buildRequirementsLore(Race evolution, boolean allMet) {
        if (!evolution.hasEvolutionRequirements()) { return ""; }
        var sb = new StringBuilder();
        for (EvolutionRequirement req : evolution.getEvolutionRequirements()) {
            boolean met = (raceData != null) && req.isMet(player, raceData);
            String icon = met ? "<green>✔" : "<red>✖";
            String display = req.displayName().isEmpty() ? req.type().name() : req.displayName();
            sb.append("\n<dark_gray>  ").append(icon).append(" <gray>").append(display);
        }
        return sb.toString();
    }

    private String getShortDescription(Race race) {
        if (race.getDescription() == null || race.getDescription().isEmpty()) {
            return module.getGuiMessage("general.evolution_default_description");
        }
        int maxLines = Math.min(2, race.getDescription().size());
        var sb = new StringBuilder("<gray>");
        for (int i = 0; i < maxLines; i++) {
            if (i > 0) { sb.append("\n<gray>"); }
            sb.append(race.getDescription().get(i));
        }
        return sb.toString();
    }

    // ─── Navegação ───────────────────────────────────────────────────

    private void displayNavigation() {
        // Voltar
        if (parentGui != null) {
            inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                    .setName(module.getGuiMessage("general.back"))
                    .build());
        }

        // Info
        inventory.setItem(SLOT_INFO, new ItemBuilder(Material.BOOK)
                .setName(gui("info.name"))
                .setLoreMultiline(gui("info.lore"))
                .build());

        // Fechar
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(module.getGuiMessage("general.close"))
                .build());
    }

    // ─── Interação ───────────────────────────────────────────────────

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event == null) { return; }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) { return; }
        if (!player.equals(event.getWhoClicked())) { return; }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) { return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) { return; }

        try {
            switch (slot) {
                case SLOT_BACK -> {
                    if (parentGui != null) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                        parentGui.open();
                    }
                }
                case SLOT_CLOSE -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
                    player.closeInventory();
                }
                default -> handleEvolutionClick(slot);
            }
        } catch (Exception e) {
            me.ray.midgard.core.debug.MidgardLogger.error(
                    "Erro no RaceEvolutionGui para %s no slot %d",
                    player.getName(), slot, e);
            player.closeInventory();
        }
    }

    private void handleEvolutionClick(int slot) {
        if (cachedEvolutions == null || cachedEvolutions.isEmpty()) { return; }

        int count = Math.min(cachedEvolutions.size(), EVOLUTION_SLOTS.length);
        int startIndex = (EVOLUTION_SLOTS.length - count) / 2;

        for (int i = 0; i < count; i++) {
            if (slot == EVOLUTION_SLOTS[startIndex + i]) {
                Race evolution = cachedEvolutions.get(i);

                if (isBranchLocked(evolution)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                if (module.getRaceManager().checkEvolutionRequirements(player, evolution)) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                    new RacePreviewGui(player, evolution, this).open();
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                return;
            }
        }

        // Verificar clique em ancestral (abrir árvore do ancestral)
        if (!ancestorChain.isEmpty()) {
            int ancestorCount = Math.min(ancestorChain.size(), ANCESTOR_SLOTS.length);
            int ancestorStart = (ANCESTOR_SLOTS.length - ancestorCount) / 2;
            for (int i = 0; i < ancestorCount; i++) {
                if (slot == ANCESTOR_SLOTS[ancestorStart + i]) {
                    Race ancestor = ancestorChain.get(ancestorChain.size() - ancestorCount + i);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new RaceEvolutionGui(player, ancestor, parentGui).open();
                    return;
                }
            }
        }
    }

}
