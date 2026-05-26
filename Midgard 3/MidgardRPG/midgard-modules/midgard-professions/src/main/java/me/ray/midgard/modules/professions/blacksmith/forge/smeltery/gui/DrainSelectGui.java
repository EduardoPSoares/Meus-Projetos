package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.gui;

import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI de seleção de metal para despejo no drain.
 * Mostra todos os metais no tanque e permite escolher qual despejar.
 * Clique esquerdo = lingote (144mb via mesa), Clique direito = bloco (1296mb via bacia).
 */
public class DrainSelectGui implements InventoryHolder {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }
    private static String guiMsg(String key) { return ProfessionsModule.getInstance().getMessage("gui.drain." + key); }

    private static final MiniMessage mm = MiniMessage.miniMessage();

    private final SmelteryManager manager;
    private final Player player;
    private final SmelteryStructure smeltery;
    private final Location drainLoc;
    private Inventory inventory;

    // Mapeamento slot → metal para identificar cliques
    private final Map<Integer, MoltenMetal> slotMetalMap = new HashMap<>();

    public DrainSelectGui(SmelteryManager manager, Player player,
                          SmelteryStructure smeltery, Location drainLoc) {
        this.manager = manager;
        this.player = player;
        this.smeltery = smeltery;
        this.drainLoc = drainLoc;
    }

    public void open() {
        SmelteryTank tank = smeltery.getTank();
        int metalCount = tank.getContents().size();

        // Tamanho dinâmico: 1 row por 7 metais + row de botões
        int rows = Math.min(6, Math.max(2, (int) Math.ceil(metalCount / 7.0) + 1));
        inventory = Bukkit.createInventory(this, rows * 9,
                mm.deserialize(guiMsg("title")));

        fillBackground(rows * 9);
        displayMetals();

        // Botão de voltar
        inventory.setItem(rows * 9 - 5, createItem(Material.ARROW,
                guiMsg("back"),
                guiMsg("back_desc")));

        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        // Botão de voltar
        if (slot == inventory.getSize() - 5) {
            new SmelteryGui(manager, player, smeltery).open();
            return;
        }

        MoltenMetal selected = slotMetalMap.get(slot);
        if (selected == null) {
            return;
        }

        boolean rightClick = event.isRightClick();
        pourMetal(selected, rightClick);
    }

    private void pourMetal(MoltenMetal metal, boolean isBlock) {
        SmelteryTank tank = smeltery.getTank();

        // Determinar mesa ou bacia
        SmelteryBlockType targetType = isBlock
                ? SmelteryBlockType.CASTING_BASIN
                : SmelteryBlockType.CASTING_TABLE;
        int pourAmount = isBlock ? 1296 : 144;
        String outputName = isBlock ? "Bloco" : "Lingote";

        // Verificar metal disponível
        int available = tank.getAmount(metal);
        if (available < pourAmount) {
            player.sendMessage(mm.deserialize(msg("smeltery.drain.insufficient_metal")
                    .replace("%needed%", String.valueOf(pourAmount))
                    .replace("%available%", String.valueOf(available))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        // Buscar mesa/bacia adjacente ao dreno
        Location target = manager.findAdjacentCastingBlock(drainLoc, targetType);
        if (target == null) {
            String blockName = isBlock ? "caldeirão (bacia)" : "laje de pedra (mesa)";
            player.sendMessage(mm.deserialize(msg("smeltery.drain.no_casting_block")
                    .replace("%block%", blockName)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        // Remover do tanque
        tank.removeMetal(metal, pourAmount);

        // Criar item
        ItemStack output = isBlock
                ? SmelteryOutputItem.createBlock(metal)
                : SmelteryOutputItem.createIngot(metal);

        // Entregar ao jogador
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(output);
        for (ItemStack leftover : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        player.sendMessage(mm.deserialize(msg("smeltery.drain.poured")
                .replace("%metal%", metal.getFormattedName())
                .replace("%output%", outputName)
                .replace("%amount%", String.valueOf(pourAmount))));

        // Efeitos visuais
        SmelteryVisualManager vis = manager.getVisualManager();
        if (vis != null) {
            vis.playPouringAnimation(drainLoc, target, metal);
        }
        player.playSound(drainLoc, Sound.BLOCK_LAVA_POP, 1.0f, 0.5f);

        // Re-abrir a GUI atualizada (ou fechar se tanque esvaziou)
        if (tank.isEmpty()) {
            player.closeInventory();
            player.sendMessage(mm.deserialize(msg("smeltery.drain.tank_emptied")));
        } else {
            open();
        }
    }

    // ── Display ──

    private void fillBackground(int size) {
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, bg);
        }
    }

    private void displayMetals() {
        slotMetalMap.clear();
        SmelteryTank tank = smeltery.getTank();
        List<Map.Entry<MoltenMetal, Integer>> sorted = tank.getSortedContents();

        // Verificar disponibilidade de mesa e bacia próximas ao drain
        boolean hasTable = manager.findAdjacentCastingBlock(drainLoc, SmelteryBlockType.CASTING_TABLE) != null;
        boolean hasBasin = manager.findAdjacentCastingBlock(drainLoc, SmelteryBlockType.CASTING_BASIN) != null;

        // Slots centrais (colunas 1-7, começando da row 0)
        int[] contentSlots = {10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34};

        // Para GUI pequena (2 rows), usar row 0 diretamente
        int rows = inventory.getSize() / 9;
        int[] slots;
        if (rows <= 2) {
            slots = new int[]{1, 2, 3, 4, 5, 6, 7};
        } else {
            slots = contentSlots;
        }

        for (int i = 0; i < sorted.size() && i < slots.length; i++) {
            var entry = sorted.get(i);
            MoltenMetal metal = entry.getKey();
            int amount = entry.getValue();

            Material glass = getColoredGlass(metal);
            boolean canIngot = amount >= 144 && hasTable;
            boolean canBlock = amount >= 1296 && hasBasin;
            int ingots = amount / 144;
            int blocks = amount / 1296;

            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("<gray>" + guiMsg("volume") + " <white>" + amount + "mb");
            lore.add("<gray>" + guiMsg("hardness") + " <white>" + String.format("%.1f", metal.getHardness()));
            lore.add(metal.isAlloy() ? guiMsg("alloy") : guiMsg("base_metal"));
            lore.add(" ");

            if (!hasTable) {
                lore.add(guiMsg("no_table"));
            } else if (amount >= 144) {
                lore.add(guiMsg("click_ingot") + " <dark_gray>×" + ingots);
            } else {
                lore.add(guiMsg("insufficient_ingot"));
            }

            if (!hasBasin) {
                lore.add(guiMsg("no_basin"));
            } else if (amount >= 1296) {
                lore.add(guiMsg("click_block") + " <dark_gray>×" + blocks);
            } else {
                lore.add(guiMsg("insufficient_block"));
            }

            ItemStack item = createItem(glass,
                    metal.getFormattedName() + " <gray>— <white>" + amount + "mb",
                    lore.toArray(new String[0]));

            int slot = slots[i];
            inventory.setItem(slot, item);
            slotMetalMap.put(slot, metal);
        }
    }

    // ── Utilitários ──

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

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.deserialize(name));
        if (lore.length > 0) {
            List<Component> components = new ArrayList<>();
            for (String line : lore) {
                components.add(mm.deserialize(line));
            }
            meta.lore(components);
        }
        item.setItemMeta(meta);
        return item;
    }
}
