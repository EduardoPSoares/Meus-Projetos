package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.gui;

import me.ray.midgard.core.utils.Task;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * GUI principal da Smeltery mostrando:
 * - Tanque visual de metais fundidos (barras coloridas estilo Tinkers)
 * - Fila de fundição atual
 * - Temperatura e fuel status
 * - Slot de inserção de itens
 * - Botão de despejo/drain
 */
public class SmelteryGui implements InventoryHolder {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }
    private static String guiMsg(String key) { return ProfessionsModule.getInstance().getMessage("gui.smeltery." + key); }

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final int GUI_SIZE = 54; // 6 rows

    // Layout slots
    private static final int[] TANK_SLOTS = {0, 9, 18, 27, 36, 45, 1, 10, 19, 28, 37, 46}; // 2 colunas × 6 rows
    private static final int TEMP_SLOT = 4;
    private static final int FUEL_SLOT = 13;
    private static final int STATUS_SLOT = 3;
    private static final int[] QUEUE_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17}; // fila de fundição
    private static final int INPUT_LABEL_SLOT = 22; // label acima do input
    private static final int INPUT_SLOT = 31; // slot para inserir item
    private static final int DRAIN_SLOT = 40; // botão de despejo
    private static final int ALLOY_BOOK_SLOT = 49; // livro de ligas
    private static final int INFO_SLOT = 50; // info da smeltery
    private static final int CLOSE_SLOT = 53;

    private static final int REFRESH_INTERVAL = 20; // 1 segundo

    private final SmelteryManager manager;
    private final Player player;
    private final SmelteryStructure smeltery;
    private Inventory inventory;
    private BukkitTask refreshTask;

    // Estado anterior para dirty checking (evitar refresh desnecessário)
    private int lastTankVolume = -1;
    private int lastTemperature = -1;
    private int lastFuelRemaining = -1;
    private int lastQueueSize = -1;
    private boolean lastHeated;

    public SmelteryGui(SmelteryManager manager, Player player, SmelteryStructure smeltery) {
        this.manager = manager;
        this.player = player;
        this.smeltery = smeltery;
    }

    public void open() {
        inventory = Bukkit.createInventory(this, GUI_SIZE,
                mm.deserialize("<dark_red><bold>⚗ " + smeltery.getName()));

        fillBackground();
        updateTankDisplay();
        updateTemperature();
        updateFuelStatus();
        updateSmeltingQueue();
        updateStatusDisplay();
        updateActionButtons();

        player.openInventory(inventory);
        manager.showStatusBar(player, smeltery);
        startAutoRefresh();
    }

    @Override
    public Inventory getInventory() { return inventory; }

    // ── Atualização do Display ──

    private void fillBackground() {
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            inventory.setItem(i, bg);
        }
        // Limpar slots de input
        inventory.setItem(INPUT_SLOT, null);
    }

    /**
     * Exibe o tanque de metais fundidos como barras coloridas.
     * Cada metal é representado por um vidro colorido proporcional ao volume.
     */
    public void updateTankDisplay() {
        SmelteryTank tank = smeltery.getTank();

        // Limpar slots do tanque
        ItemStack emptyTank = createItem(Material.GRAY_STAINED_GLASS_PANE,
                guiMsg("tank_empty"));
        for (int slot : TANK_SLOTS) {
            inventory.setItem(slot, emptyTank);
        }

        if (tank.isEmpty()) {
            return;
        }

        // Distribuir metais nos slots do tanque proporcionalmente
        List<Map.Entry<MoltenMetal, Integer>> sorted = tank.getSortedContents();
        int totalSlots = TANK_SLOTS.length;
        int totalVolume = tank.getTotalVolume();
        int slotIndex = totalSlots - 1; // começa de baixo para cima

        for (var entry : sorted) {
            MoltenMetal metal = entry.getKey();
            int amount = entry.getValue();

            // Quantos slots esse metal ocupa proporcionalmente
            int metalSlots = Math.max(1, (int) Math.round((double) amount / totalVolume * totalSlots));
            metalSlots = Math.min(metalSlots, slotIndex + 1);

            Material glass = getColoredGlass(metal);
            for (int i = 0; i < metalSlots && slotIndex >= 0; i++) {
                ItemStack metalItem = createItem(glass,
                        metal.getFormattedName(),
                        "<gray>" + guiMsg("volume") + " <white>" + amount + "mb",
                        "<gray>" + guiMsg("hardness") + " <white>" + String.format("%.1f", metal.getHardness()),
                        metal.isAlloy() ? guiMsg("alloy") : guiMsg("base_metal"));
                inventory.setItem(TANK_SLOTS[slotIndex], metalItem);
                slotIndex--;
            }
        }
    }

    public void updateTemperature() {
        int temp = smeltery.getTank().getTemperature();
        int maxTemp = smeltery.getTier().getMaxTemperature();
        String tempColor = temp < 500 ? "<blue>" : temp < 1000 ? "<yellow>" : temp < 1500 ? "<gold>" : "<red>";

        ItemStack item = createItem(Material.MAGMA_CREAM,
                guiMsg("temperature"),
                tempColor + temp + "°C <gray>/ " + maxTemp + "°C",
                " ",
                smeltery.isHeated() ? guiMsg("heated") : guiMsg("cold"),
                guiMsg("fuel_remaining").replace("%seconds%", String.valueOf(smeltery.getFuelRemaining() / 20)));
        inventory.setItem(TEMP_SLOT, item);
    }

    public void updateFuelStatus() {
        boolean hasFuel = smeltery.getFuelRemaining() > 0;
        ItemStack item = createItem(hasFuel ? Material.BLAZE_POWDER : Material.GUNPOWDER,
                guiMsg("fuel"),
                hasFuel ?
                        guiMsg("fuel_active").replace("%seconds%", String.valueOf(smeltery.getFuelRemaining() / 20)) :
                        guiMsg("no_fuel"),
                " ",
                guiMsg("accepted_fuels"),
                guiMsg("fuel_list_1"),
                guiMsg("fuel_list_2"),
                " ",
                guiMsg("fuel_hint"));
        inventory.setItem(FUEL_SLOT, item);
    }

    public void updateSmeltingQueue() {
        List<SmelteryStructure.SmeltingEntry> queue = smeltery.getSmeltingQueue();
        int i = 0;
        for (int slot : QUEUE_SLOTS) {
            if (i < queue.size()) {
                SmelteryStructure.SmeltingEntry entry = queue.get(i);
                float progress = entry.getProgressPercent();
                String progressBar = buildProgressBar(progress, 10);

                ItemStack item = createItem(entry.getSourceMaterial(),
                        guiMsg("smelting").replace("%count%", String.valueOf(entry.getRemainingItems())),
                        entry.getOutputMetal().getFormattedName(),
                        " ",
                        "<gray>" + guiMsg("progress") + " " + progressBar + " <white>" +
                                String.format("%.0f%%", progress * 100),
                        "<gray>" + guiMsg("time_remaining") + " <white>" +
                                String.format("%.1f", (entry.getSmeltTimePerItem() - entry.getCurrentProgress()) / 20.0) + "s");
                inventory.setItem(slot, item);
            } else {
                inventory.setItem(slot, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                        guiMsg("empty_slot"),
                        guiMsg("input_items")));
            }
            i++;
        }
    }

    public void updateStatusDisplay() {
        SmelteryTank tank = smeltery.getTank();
        int metals = tank.getContents().size();
        int volume = tank.getTotalVolume();
        int capacity = tank.getCapacity();
        float fillPct = tank.getFillPercent() * 100;

        ItemStack item = createItem(Material.NETHER_BRICKS,
                "<dark_red><bold>⚗ " + smeltery.getTier().getFormattedName(),
                " ",
                "<gray>" + guiMsg("tank") + " <white>" + volume + "/" + capacity + "mb " +
                        "<gray>(" + String.format("%.0f%%", fillPct) + ")",
                "<gray>" + guiMsg("different_metals") + " <white>" + metals,
                "<gray>" + guiMsg("items_smelted") + " <white>" + smeltery.getTotalItemsSmelted(),
                " ",
                "<gray>" + guiMsg("owner") + " <white>" + Bukkit.getOfflinePlayer(smeltery.getOwnerUuid()).getName());
        inventory.setItem(STATUS_SLOT, item);
    }

    private void updateActionButtons() {
        // Botão de input — clicável para inserir material do cursor
        ItemStack inputBtn = createItem(Material.DROPPER,
                guiMsg("insert_material"),
                guiMsg("insert_hint_1"),
                guiMsg("insert_hint_2"),
                " ",
                guiMsg("smeltable_materials"),
                guiMsg("material_ores"),
                guiMsg("material_gems"),
                guiMsg("material_tools"),
                " ",
                guiMsg("or_slot_below"));
        inventory.setItem(INPUT_LABEL_SLOT, inputBtn);

        // Slot vazio alternativo para drag-and-drop
        inventory.setItem(INPUT_SLOT, null);

        // Botão de drain
        ItemStack drainBtn = createItem(Material.HOPPER,
                guiMsg("pour_metal"),
                guiMsg("pour_hint_1"),
                guiMsg("pour_hint_2"),
                " ",
                guiMsg("pour_table_rate"),
                guiMsg("pour_basin_rate"),
                " ",
                smeltery.getTank().isEmpty() ?
                        guiMsg("tank_empty_warning") :
                        guiMsg("metal_available") + " " + smeltery.getTank().getDominantMetal().getFormattedName());
        inventory.setItem(DRAIN_SLOT, drainBtn);

        // Livro de ligas
        ItemStack alloyBook = createItem(Material.BOOK,
                guiMsg("alloy_book"),
                guiMsg("alloy_book_desc_1"),
                guiMsg("alloy_book_desc_2"),
                " ",
                guiMsg("click_to_open"));
        inventory.setItem(ALLOY_BOOK_SLOT, alloyBook);

        // Info
        ItemStack info = createItem(Material.NETHER_BRICK,
                guiMsg("info"),
                "<gray>Tier: " + smeltery.getTier().getFormattedName(),
                "<gray>" + guiMsg("max_temperature") + " <white>" + smeltery.getTier().getMaxTemperature() + "°C",
                "<gray>" + guiMsg("drains") + " <white>" + smeltery.getTier().getMaxDrains());
        inventory.setItem(INFO_SLOT, info);

        // Fechar
        inventory.setItem(CLOSE_SLOT, createItem(Material.BARRIER, guiMsg("close")));
    }

    // ── Handlers de Clique ──

    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int guiSize = inventory.getSize();

        // Clique no inventário do jogador (bottom inventory) — permitir para poder pegar itens
        if (slot >= guiSize) {
            // Bloquear shift-click e double-click para evitar exploits
            if (event.isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK) {
                event.setCancelled(true);
            }
            return;
        }

        // Se clicou no slot de input, permitir colocar/retirar item
        if (slot == INPUT_SLOT) {
            return;
        }

        // Todo outro clique na GUI é cancelado (slots decorativos e botões)
        event.setCancelled(true);

        if (slot == INPUT_LABEL_SLOT) {
            handleInputClick(event);
        } else if (slot == DRAIN_SLOT) {
            handleDrain();
        } else if (slot == ALLOY_BOOK_SLOT) {
            openAlloyBook();
        } else if (slot == CLOSE_SLOT) {
            player.closeInventory();
        }
    }

    private void handleInputClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR) {
            player.sendMessage(mm.deserialize(
                    msg("smeltery.gui.input_hint")));
            return;
        }

        // Processar o item do cursor como input
        ItemStack toProcess = cursor.clone();
        manager.onItemInput(player, smeltery, toProcess);

        // Se o item foi consumido (amount zerado), limpar cursor
        if (toProcess.getAmount() <= 0) {
            event.getView().setCursor(null);
        } else {
            // Devolver o que sobrou ao cursor
            cursor.setAmount(toProcess.getAmount());
        }

        refreshAll();
    }

    public void handleClose(InventoryCloseEvent event) {
        stopAutoRefresh();

        // Se jogador deixou item no slot de input, processar
        ItemStack inputItem = inventory.getItem(INPUT_SLOT);
        if (inputItem != null && inputItem.getType() != Material.AIR) {
            manager.onItemInput(player, smeltery, inputItem);
            // Se o item não foi totalmente consumido, devolver
            if (inputItem.getAmount() > 0) {
                player.getInventory().addItem(inputItem);
            }
        }

        manager.hideBossBar(player);
    }

    private void handleDrain() {
        List<Location> drains = smeltery.getInteractiveLocations()
                .getOrDefault(SmelteryBlockType.DRAIN, Collections.emptyList());
        if (drains.isEmpty()) {
            player.sendMessage(mm.deserialize(msg("smeltery.gui.no_drain")));
            return;
        }

        manager.onDrainInteract(player, smeltery, drains.get(0));
        refreshAll();
    }

    private void openAlloyBook() {
        AlloyBookGui alloyGui = new AlloyBookGui(player, smeltery.getTank(), manager, smeltery);
        alloyGui.open();
    }

    // ── Auto-Refresh ──

    private void startAutoRefresh() {
        refreshTask = Task.syncTimer(player, () -> {
            if (player == null || !player.isOnline()) {
                stopAutoRefresh();
                return;
            }
            // Verificar se o inventário ainda está aberto
            if (player.getOpenInventory().getTopInventory() != inventory) {
                stopAutoRefresh();
                return;
            }
            smartRefresh();
        }, REFRESH_INTERVAL, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    /**
     * Refresh inteligente — só atualiza seções que mudaram.
     * Reduz pacotes enviados ao cliente para zero lag.
     */
    private void smartRefresh() {
        SmelteryTank tank = smeltery.getTank();
        int curVolume = tank.getTotalVolume();
        int curTemp = tank.getTemperature();
        int curFuel = smeltery.getFuelRemaining();
        int curQueue = smeltery.getSmeltingQueue().size();
        boolean curHeated = smeltery.isHeated();

        boolean tankChanged = curVolume != lastTankVolume;
        boolean tempChanged = curTemp != lastTemperature || curHeated != lastHeated;
        boolean fuelChanged = curFuel != lastFuelRemaining;
        boolean queueChanged = curQueue != lastQueueSize;

        // Progresso da fila muda todo tick, então sempre atualiza se há queue
        boolean queueProgress = curQueue > 0;

        if (tankChanged) {
            updateTankDisplay();
        }
        if (tempChanged) {
            updateTemperature();
        }
        if (fuelChanged) {
            updateFuelStatus();
        }
        if (queueChanged || queueProgress) {
            updateSmeltingQueue();
        }
        if (tankChanged || tempChanged) {
            updateStatusDisplay();
        }
        if (tankChanged) {
            updateActionButtons();
        }

        lastTankVolume = curVolume;
        lastTemperature = curTemp;
        lastFuelRemaining = curFuel;
        lastQueueSize = curQueue;
        lastHeated = curHeated;
    }

    /**
     * Atualiza todos os elementos da GUI.
     */
    public void refreshAll() {
        updateTankDisplay();
        updateTemperature();
        updateFuelStatus();
        updateSmeltingQueue();
        updateStatusDisplay();
        updateActionButtons();
    }

    // ── Utilitários de Item ──

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

    private Material getColoredGlass(MoltenMetal metal) {
        return switch (metal) {
            case IRON, STEEL -> Material.WHITE_STAINED_GLASS_PANE;
            case GOLD, ELECTRUM, ROSE_GOLD -> Material.YELLOW_STAINED_GLASS_PANE;
            case COPPER, BRONZE -> Material.ORANGE_STAINED_GLASS_PANE;
            case NETHERITE_SCRAP, OBSIDIAN_ALLOY -> Material.BLACK_STAINED_GLASS_PANE;
            case DIAMOND -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case EMERALD -> Material.LIME_STAINED_GLASS_PANE;
            case AMETHYST, MANYULLYN, KNIGHTSLIME -> Material.PURPLE_STAINED_GLASS_PANE;
            case QUARTZ -> Material.WHITE_STAINED_GLASS_PANE;
            case LAPIS -> Material.BLUE_STAINED_GLASS_PANE;
            case REDSTONE -> Material.RED_STAINED_GLASS_PANE;
        };
    }

    private String buildProgressBar(float progress, int length) {
        int filled = (int) (progress * length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                sb.append("<green>█");
            } else {
                sb.append("<dark_gray>█");
            }
        }
        return sb.toString();
    }
}
