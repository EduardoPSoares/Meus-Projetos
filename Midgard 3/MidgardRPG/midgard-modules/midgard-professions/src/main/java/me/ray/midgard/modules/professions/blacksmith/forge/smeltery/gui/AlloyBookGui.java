package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.gui;

import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.modules.item.model.MidgardRecipe;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * GUI mostrando todas as receitas de ligas (alloys) e receitas de fundição (smelting).
 * Receitas de fundição são clicáveis — consome metais do tanque e entrega o item ao jogador.
 */
public class AlloyBookGui implements InventoryHolder {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }
    private static String guiMsg(String key) { return ProfessionsModule.getInstance().getMessage("gui.alloy_book." + key); }

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final int GUI_SIZE = 54;

    private static final int BACK_SLOT = 49;
    private static final int FILTER_SLOT = 4;
    private static final int REFRESH_INTERVAL = 40; // 2 segundos

    private final Player player;
    private final SmelteryTank tank;
    private final SmelteryManager manager;
    private final SmelteryStructure smeltery;
    private Inventory inventory;
    private BukkitTask refreshTask;

    // Cache de receitas smelting (carregadas uma vez)
    private List<SmeltingCraftEntry> cachedSmeltingRecipes;

    // Mapeamento slot → SmeltingCraftEntry para cliques de crafting
    private final Map<Integer, SmeltingCraftEntry> slotSmeltingMap = new HashMap<>();

    // Filter state
    private boolean craftableOnly = false;

    public AlloyBookGui(Player player, SmelteryTank tank, SmelteryManager manager, SmelteryStructure smeltery) {
        this.player = player;
        this.tank = tank;
        this.manager = manager;
        this.smeltery = smeltery;
    }

    public void open() {
        inventory = Bukkit.createInventory(this, GUI_SIZE,
                mm.deserialize(guiMsg("title")));

        cachedSmeltingRecipes = loadSmeltingRecipes();

        fillBackground();
        displayAlloyRecipes();

        player.openInventory(inventory);
        startAutoRefresh();
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == BACK_SLOT) {
            stopAutoRefresh();
            new SmelteryGui(manager, player, smeltery).open();
            return;
        }

        if (slot == FILTER_SLOT) {
            craftableOnly = !craftableOnly;
            displayAlloyRecipes();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            return;
        }

        // Tentativa de craft de receita smelting
        SmeltingCraftEntry entry = slotSmeltingMap.get(slot);
        if (entry != null) {
            attemptSmeltingCraft(entry);
        }
    }

    /**
     * Tenta fabricar uma receita de fundição:
     * 1. Verifica temperatura
     * 2. Verifica metais no tanque
     * 3. Consome metais
     * 4. Entrega item ao jogador
     */
    private void attemptSmeltingCraft(SmeltingCraftEntry entry) {
        // Verificar temperatura
        if (tank.getTemperature() < entry.minTemp) {
            player.sendMessage(mm.deserialize(msg("smeltery.alloy.temp_insufficient")
                    .replace("%temp%", String.valueOf(entry.minTemp))
                    .replace("%current%", String.valueOf(tank.getTemperature()))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        // Verificar e coletar metais necessários
        for (var metalEntry : entry.metals.entrySet()) {
            MoltenMetal molten = parseMoltenMetal(metalEntry.getKey());
            if (molten == null) {
                player.sendMessage(mm.deserialize(msg("smeltery.alloy.invalid_metal")
                        .replace("%name%", metalEntry.getKey())));
                return;
            }
            if (!tank.hasMetal(molten, metalEntry.getValue())) {
                int available = tank.getAmount(molten);
                player.sendMessage(mm.deserialize(msg("smeltery.alloy.insufficient_metal")
                        .replace("%metal%", molten.getFormattedName())
                        .replace("%needed%", String.valueOf(metalEntry.getValue()))
                        .replace("%available%", String.valueOf(available))));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
                return;
            }
        }

        // Consumir metais do tanque
        for (var metalEntry : entry.metals.entrySet()) {
            MoltenMetal molten = parseMoltenMetal(metalEntry.getKey());
            tank.removeMetal(molten, metalEntry.getValue());
        }

        // Criar item de output
        ItemStack output = entry.buildOutput();

        // Entregar ao jogador
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(output);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.sendMessage(mm.deserialize(msg("smeltery.alloy.crafted")
                .replace("%amount%", String.valueOf(entry.outputAmount))
                .replace("%item%", entry.itemId)));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.7f, 1.2f);

        // Atualizar display
        displayAlloyRecipes();
    }

    public void handleClose(InventoryCloseEvent event) {
        stopAutoRefresh();
    }

    private void startAutoRefresh() {
        refreshTask = Task.syncTimer(player, () -> {
            if (player == null || !player.isOnline()) {
                stopAutoRefresh();
                return;
            }
            if (player.getOpenInventory().getTopInventory() != inventory) {
                stopAutoRefresh();
                return;
            }
            displayAlloyRecipes();
        }, REFRESH_INTERVAL, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    private void fillBackground() {
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, bg);
        }
    }

    private void displayAlloyRecipes() {
        slotSmeltingMap.clear();

        // Filter toggle button
        inventory.setItem(FILTER_SLOT, createItem(craftableOnly ? Material.LIME_DYE : Material.GRAY_DYE,
                craftableOnly ? guiMsg("filter_on") : guiMsg("filter_off"),
                craftableOnly ? guiMsg("filter_tooltip_show_all") : guiMsg("filter_tooltip_filter")));

        List<AlloyRecipe> recipes = manager.getAlloyRecipeManager().getAllRecipes();
        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43};

        int slotIndex = 0;

        // 1. Receitas de ligas (AlloyRecipe) — display-only (formam automaticamente)
        for (int i = 0; i < recipes.size() && slotIndex < contentSlots.length; i++) {
            AlloyRecipe recipe = recipes.get(i);
            MoltenMetal result = recipe.getResult();

            boolean canForm = recipe.canForm(tank.getContents(), tank.getTemperature());
            if (craftableOnly && !canForm) {
                continue;
            }

            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(guiMsg("ingredients"));

            for (var entry : recipe.getIngredients().entrySet()) {
                MoltenMetal ingredient = entry.getKey();
                int amount = entry.getValue();
                boolean hasEnough = tank.hasMetal(ingredient, amount);
                String check = hasEnough ? "<green>✔" : "<red>✘";
                lore.add("  " + check + " " + ingredient.getFormattedName() +
                        " <gray>— <white>" + amount + "mb" +
                        (hasEnough ? "" : " <red>(" + guiMsg("missing") + " " + (amount - tank.getAmount(ingredient)) + "mb)"));
            }

            lore.add(" ");
            lore.add(guiMsg("result") + " " + result.getFormattedName() +
                    " <gray>— <white>" + recipe.getResultAmount() + "mb");
            lore.add(guiMsg("min_temp") + " <white>" + recipe.getMinSmelteryTemperature() + "°C");
            lore.add(" ");
            lore.add(guiMsg("hardness") + " <white>" + String.format("%.1f", result.getHardness()));

            lore.add(canForm ? guiMsg("can_form") : guiMsg("insufficient_ingredients"));
            lore.add(" ");
            lore.add(guiMsg("auto_form"));

            Material displayMat = result.getVisualBlock();
            ItemStack item = createItem(displayMat,
                    result.getFormattedName() + " " + guiMsg("alloy_suffix"),
                    lore.toArray(new String[0]));

            inventory.setItem(contentSlots[slotIndex++], item);
        }

        // 2. Receitas de smelting (do módulo de itens) — clicáveis para fabricar
        if (cachedSmeltingRecipes != null) {
            for (SmeltingCraftEntry entry : cachedSmeltingRecipes) {
                if (slotIndex >= contentSlots.length) {
                    break;
                }

                // Pre-check if craftable for filter
                boolean allMetPreCheck = true;
                boolean tempOkPreCheck = tank.getTemperature() >= entry.minTemp;
                for (var me : entry.metals.entrySet()) {
                    MoltenMetal m = parseMoltenMetal(me.getKey());
                    if (m == null || !tank.hasMetal(m, me.getValue())) { allMetPreCheck = false; break; }
                }
                if (craftableOnly && !(allMetPreCheck && tempOkPreCheck)) {
                    continue;
                }

                List<String> lore = new ArrayList<>();
                lore.add(" ");
                lore.add(guiMsg("smelting_recipe"));
                lore.add(" ");
                lore.add(guiMsg("metals_required"));

                boolean allMet = true;
                for (var metalEntry : entry.metals.entrySet()) {
                    String metalName = metalEntry.getKey();
                    int amount = metalEntry.getValue();

                    MoltenMetal molten = parseMoltenMetal(metalName);
                    boolean hasEnough = molten != null && tank.hasMetal(molten, amount);
                    if (!hasEnough) {
                        allMet = false;
                    }

                    String formattedName = molten != null ? molten.getFormattedName() : "<white>" + metalName;
                    String check = hasEnough ? "<green>✔" : "<red>✘";
                    int available = molten != null ? tank.getAmount(molten) : 0;
                    lore.add("  " + check + " " + formattedName +
                            " <gray>— <white>" + amount + "mb" +
                            (hasEnough ? "" : " <red>(" + guiMsg("missing") + " " + (amount - available) + "mb)"));
                }

                lore.add(" ");
                lore.add(guiMsg("min_temp") + " <white>" + entry.minTemp + "°C");
                lore.add(guiMsg("produces") + " <white>" + entry.outputAmount + "x");
                lore.add(" ");

                boolean tempOk = tank.getTemperature() >= entry.minTemp;
                boolean canCraft = allMet && tempOk;
                if (!tempOk) {
                    lore.add(guiMsg("insufficient_temp") + " (" + tank.getTemperature() + "°C)");
                } else {
                    lore.add(canCraft ? guiMsg("can_craft") : guiMsg("insufficient_metals"));
                }

                lore.add(" ");
                lore.add(canCraft ? guiMsg("click_to_craft") : guiMsg("click_to_craft_unavailable"));

                // Criar display item com lore atualizada
                ItemStack displayItem = entry.buildOutput();
                ItemMeta meta = displayItem.getItemMeta();
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(mm.deserialize(line));
                }
                meta.lore(loreComponents);
                displayItem.setItemMeta(meta);

                int currentSlot = contentSlots[slotIndex++];
                inventory.setItem(currentSlot, displayItem);
                slotSmeltingMap.put(currentSlot, entry);
            }
        }

        // Limpar slots não usados
        for (int i = slotIndex; i < contentSlots.length; i++) {
            inventory.setItem(contentSlots[i], createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        // Botão de voltar
        inventory.setItem(BACK_SLOT, createItem(Material.ARROW, guiMsg("back"),
                guiMsg("back_lore")));
    }

    // ── Smelting Recipes do ItemModule ──

    private record SmeltingCraftEntry(String itemId, Map<String, Integer> metals,
                                      int minTemp, int outputAmount) {
        /**
         * Constrói o ItemStack de output fresco a partir do ItemModule.
         */
        ItemStack buildOutput() {
            try {
                ItemModule itemModule = ItemModule.getInstance();
                if (itemModule != null && itemModule.getItemManager() != null) {
                    MidgardItem item = itemModule.getItemManager().getItem(itemId);
                    if (item != null) {
                        ItemStack result = item.build();
                        result.setAmount(Math.max(1, outputAmount));
                        return result;
                    }
                }
            } catch (Exception ignored) { /* fallback to default item */ }
            // Fallback
            return new ItemStack(Material.IRON_INGOT, Math.max(1, outputAmount));
        }
    }

    private List<SmeltingCraftEntry> loadSmeltingRecipes() {
        List<SmeltingCraftEntry> entries = new ArrayList<>();
        try {
            ItemModule itemModule = ItemModule.getInstance();
            if (itemModule == null || itemModule.getItemManager() == null) {
                return entries;
            }

            for (String itemId : itemModule.getItemManager().getItemIds()) {
                MidgardItem item = itemModule.getItemManager().getItem(itemId);
                if (item == null) {
                    continue;
                }

                for (MidgardRecipe recipe : item.getRecipes()) {
                    if (recipe.getType() != MidgardRecipe.RecipeType.SMELTING) {
                        continue;
                    }
                    if (recipe.isHiddenFromBook()) {
                        continue;
                    }

                    Map<String, Integer> metals = recipe.getSmeltingMetals();
                    if (metals == null || metals.isEmpty()) {
                        continue;
                    }

                    entries.add(new SmeltingCraftEntry(
                            itemId,
                            metals,
                            recipe.getSmeltingMinTemperature(),
                            recipe.getOutputAmount()
                    ));
                }
            }
        } catch (Exception ignored) {
            // ItemModule pode não estar carregado
        }
        return entries;
    }

    private MoltenMetal parseMoltenMetal(String name) {
        try {
            return MoltenMetal.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize(name));

        if (lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(mm.deserialize(line));
            }
            meta.lore(loreComponents);
        }

        item.setItemMeta(meta);
        return item;
    }
}
