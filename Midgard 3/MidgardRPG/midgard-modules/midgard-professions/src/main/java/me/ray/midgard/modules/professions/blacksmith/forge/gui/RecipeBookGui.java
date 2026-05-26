package me.ray.midgard.modules.professions.blacksmith.forge.gui;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipe;
import me.ray.midgard.modules.item.model.MidgardItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Paginated recipe book with search and filter support.
 * Shows available forge recipes to the player.
 */
public class RecipeBookGui extends PaginatedGui<ForgeRecipe> {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.recipe_book." + key); }

    private static final int SLOT_SPEC_FILTER = 2;
    private static final int SLOT_SEARCH = 4;
    private static final int SLOT_CRAFTABLE_TOGGLE = 6;

    private final int playerLevel;
    private final ForgeTier forgeTier;
    private final List<ForgeRecipe> allRecipes;
    private BiConsumer<Player, ForgeRecipe> onRecipeSelected;
    private Consumer<Player> onSearchRequested;

    // Filter state
    private String activeSpecFilter; // null = all
    private boolean craftableOnly;
    private String searchQuery; // null = no filter

    // Known specializations extracted from recipes
    private final List<String> specializations;

    public RecipeBookGui(Player player, List<ForgeRecipe> recipes, int playerLevel, ForgeTier forgeTier) {
        super(player, msg("title"), recipes);
        this.playerLevel = playerLevel;
        this.forgeTier = forgeTier;
        this.allRecipes = new ArrayList<>(recipes);

        // Extract unique specializations
        this.specializations = recipes.stream()
                .map(ForgeRecipe::getSpecialization)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
        applyFilters();
    }

    public void setOnSearchRequested(Consumer<Player> cb) { this.onSearchRequested = cb; }

    private void applyFilters() {
        List<ForgeRecipe> filtered = allRecipes.stream()
                .filter(r -> {
                    if (activeSpecFilter != null) {
                        String spec = r.getSpecialization();
                        if (spec == null || !spec.equalsIgnoreCase(activeSpecFilter)) { return false; }
                    }
                    if (craftableOnly) {
                        if (r.getRequiredLevel() > playerLevel) { return false; }
                        if (r.getRequiredForgeTier().getLevel() > forgeTier.getLevel()) { return false; }
                    }
                    if (searchQuery != null && !searchQuery.isEmpty()) {
                        String q = searchQuery.toLowerCase();
                        String name = r.getDisplayName() != null ? r.getDisplayName().toLowerCase() : "";
                        String id = r.getId() != null ? r.getId().toLowerCase() : "";
                        if (!name.contains(q) && !id.contains(q)) { return false; }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        this.items = filtered;
        this.page = 0;
        initializeItems();
    }

    @Override
    public void initializeItems() {
        super.initializeItems();
        addFilterButtons();
    }

    private void addFilterButtons() {
        // Specialization filter
        String specLabel = activeSpecFilter != null ? activeSpecFilter : msg("all");
        inventory.setItem(SLOT_SPEC_FILTER, new ItemBuilder(Material.COMPASS)
                .setName(msg("spec_label") + specLabel)
                .addLore(msg("click_change_filter"))
                .addLore("")
                .addLore(activeSpecFilter == null ? "<green>▸ " + msg("all") : "<gray>  " + msg("all"))
                .addLore(specializations.stream()
                        .map(s -> (s.equalsIgnoreCase(activeSpecFilter != null ? activeSpecFilter : "") ? "<green>▸ " : "<gray>  ") + s)
                        .collect(Collectors.joining("\n")).isEmpty()
                        ? "" : null)
                .build());

        // Rebuild spec filter lore properly
        ItemBuilder specBuilder = new ItemBuilder(Material.COMPASS)
                .setName(msg("spec_label") + specLabel)
                .addLore(msg("click_change_filter"))
                .addLore("");
        specBuilder.addLore(activeSpecFilter == null ? "<green>▸ " + msg("all") : "<gray>  " + msg("all"));
        for (String s : specializations) {
            specBuilder.addLore(s.equalsIgnoreCase(activeSpecFilter != null ? activeSpecFilter : "") ? "<green>▸ " + s : "<gray>  " + s);
        }
        inventory.setItem(SLOT_SPEC_FILTER, specBuilder.build());

        // Search button
        ItemBuilder searchBuilder = new ItemBuilder(Material.SPYGLASS)
                .setName(msg("search_title"));
        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchBuilder.addLore(msg("filter_active") + searchQuery);
            searchBuilder.addLore(msg("shift_click_clear"));
        } else {
            searchBuilder.addLore(msg("click_to_search"));
        }
        inventory.setItem(SLOT_SEARCH, searchBuilder.build());

        // Craftable only toggle
        inventory.setItem(SLOT_CRAFTABLE_TOGGLE, new ItemBuilder(craftableOnly ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(craftableOnly ? msg("craftable_on") : msg("craftable_off"))
                .addLore(craftableOnly ? msg("toggle_show_all") : msg("toggle_filter_craftable"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == SLOT_SPEC_FILTER) {
            event.setCancelled(true);
            cycleSpecialization();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            return;
        }
        if (slot == SLOT_SEARCH) {
            event.setCancelled(true);
            if (event.isShiftClick() && searchQuery != null) {
                searchQuery = null;
                applyFilters();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            } else if (onSearchRequested != null) {
                onSearchRequested.accept(player);
            }
            return;
        }
        if (slot == SLOT_CRAFTABLE_TOGGLE) {
            event.setCancelled(true);
            craftableOnly = !craftableOnly;
            applyFilters();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            return;
        }

        super.onClick(event);
    }

    private void cycleSpecialization() {
        if (specializations.isEmpty()) {
            activeSpecFilter = null;
            applyFilters();
            return;
        }
        if (activeSpecFilter == null) {
            activeSpecFilter = specializations.get(0);
        } else {
            int idx = specializations.indexOf(activeSpecFilter);
            if (idx < 0 || idx >= specializations.size() - 1) {
                activeSpecFilter = null; // cycle back to "All"
            } else {
                activeSpecFilter = specializations.get(idx + 1);
            }
        }
        applyFilters();
    }

    @Override
    public ItemStack createItem(ForgeRecipe recipe) {
        boolean canCraft = recipe.getRequiredLevel() <= playerLevel
                && recipe.getRequiredForgeTier().getLevel() <= forgeTier.getLevel();

        String nameColor = canCraft ? "<green>" : "<red>";

        // Resolve the actual result item for display
        ItemStack baseItem = resolveResultItem(recipe);
        ItemBuilder builder;
        if (baseItem != null && canCraft) {
            builder = new ItemBuilder(baseItem.clone())
                    .setName(nameColor + recipe.getDisplayName());
        } else {
            builder = new ItemBuilder(canCraft ? Material.PAPER : Material.BARRIER)
                    .setName(nameColor + recipe.getDisplayName());
        }

        // Level requirement
        String levelColor = recipe.getRequiredLevel() <= playerLevel ? "<green>" : "<red>";
        builder.addLore(msg("level_label") + levelColor + recipe.getRequiredLevel());

        // Forge tier requirement
        String tierColor = recipe.getRequiredForgeTier().getLevel() <= forgeTier.getLevel() ? "<green>" : "<red>";
        builder.addLore(msg("forge_label") + tierColor + recipe.getRequiredForgeTier().getDisplayName());

        builder.addLore("");

        // Materials
        builder.addLore(msg("materials_label"));
        if (recipe.getPrimaryMetal() != null) {
            builder.addLore("  <gray>• <white>" + recipe.getPrimaryMetal() + " x" + recipe.getPrimaryMetalAmount());
        }
        for (Map.Entry<String, Integer> mat : recipe.getSecondaryMaterials().entrySet()) {
            builder.addLore("  <gray>• <white>" + mat.getKey() + " x" + mat.getValue());
        }

        builder.addLore("");
        builder.addLore(msg("difficulty_label") + String.format("%.1f", recipe.getDifficultyMultiplier()) + "x");
        builder.addLore(msg("xp_base_label") + recipe.getBaseXP());

        if (recipe.getSpecialization() != null) {
            builder.addLore(msg("specialization_label") + recipe.getSpecialization());
        }

        builder.addLore("");
        if (canCraft) {
            builder.addLore(msg("click_to_select"));
            builder.glow();
        } else {
            builder.addLore(msg("requirements_not_met"));
        }

        return builder.build();
    }

    @Override
    public void onItemClick(Player player, int slot) {
        // Determine which recipe was clicked
        int startIndex = page * maxItemsPerPage;
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 3 || col < 1 || col > 7) { return; }

        int itemIndex = startIndex + (row - 1) * 7 + (col - 1);
        if (itemIndex < 0 || itemIndex >= items.size()) { return; }

        ForgeRecipe recipe = items.get(itemIndex);
        boolean canCraft = recipe.getRequiredLevel() <= playerLevel
                && recipe.getRequiredForgeTier().getLevel() <= forgeTier.getLevel();

        if (canCraft && onRecipeSelected != null) {
            player.closeInventory();
            onRecipeSelected.accept(player, recipe);
        }
    }

    public void setOnRecipeSelected(BiConsumer<Player, ForgeRecipe> callback) {
        this.onRecipeSelected = callback;
    }

    private ItemStack resolveResultItem(ForgeRecipe recipe) {
        String resultId = recipe.getResultItemId();
        if (resultId == null) { return null; }
        ItemModule itemModule = ItemModule.getInstance();
        if (itemModule == null) { return null; }
        var itemManager = itemModule.getItemManager();
        if (itemManager == null) { return null; }
        MidgardItem midgardItem = itemManager.getItem(resultId);
        if (midgardItem == null) { return null; }
        return midgardItem.build();
    }
}
