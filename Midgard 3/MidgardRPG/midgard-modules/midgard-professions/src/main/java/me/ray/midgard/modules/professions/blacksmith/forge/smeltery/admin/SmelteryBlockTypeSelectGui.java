package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.admin;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.blacksmith.forge.shared.AbstractCreationSession.ScannedBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryBlockType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * GUI para selecionar um SmelteryBlockType como papel de um bloco específico.
 */
public class SmelteryBlockTypeSelectGui extends BaseGui {

    private final ScannedBlock<SmelteryBlockType> block;
    private final SmelteryCreationSession session;

    private BiConsumer<Player, SmelteryBlockType> onSelect;
    private Consumer<Player> onBack;

    private static final SmelteryBlockType[] SELECTABLE_TYPES = {
            SmelteryBlockType.WALL,
            SmelteryBlockType.CONTROLLER,
            SmelteryBlockType.DRAIN,
            SmelteryBlockType.TANK_WINDOW,
            SmelteryBlockType.ITEM_INPUT,
            SmelteryBlockType.FUEL_INPUT,
            SmelteryBlockType.CASTING_TABLE,
            SmelteryBlockType.CASTING_BASIN,
            SmelteryBlockType.AIR
    };

    public SmelteryBlockTypeSelectGui(Player player, ScannedBlock<SmelteryBlockType> block,
                                      SmelteryCreationSession session) {
        super(player, 4, "<dark_gray>⚗ Selecionar Função");
        this.block = block;
        this.session = session;
    }

    @Override
    public void initializeItems() {
        var border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, border);
        }

        Material displayMat = block.material().isItem() ? block.material() : Material.STONE;
        SmelteryBlockType currentType = session.getAssignedRole(block.locationKey());

        inventory.setItem(4, new ItemBuilder(displayMat)
                .setName("<white>" + block.material().name().toLowerCase().replace("_", " "))
                .addLore("<gray>Posição: <white>" + block.worldX() + ", " + block.worldY() + ", " + block.worldZ())
                .addLore("<gray>Função atual: <yellow>" + SmelterySetupGui.getTypeName(currentType))
                .addLore("")
                .addLore("<gray>Selecione a nova função abaixo:")
                .build());

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20};

        for (int i = 0; i < SELECTABLE_TYPES.length && i < slots.length; i++) {
            SmelteryBlockType type = SELECTABLE_TYPES[i];
            Material icon = getTypeIcon(type);
            boolean isSelected = type == currentType;

            inventory.setItem(slots[i], new ItemBuilder(icon)
                    .setName((isSelected ? "<green>" : "<yellow>") + SmelterySetupGui.getTypeName(type))
                    .addLore(isSelected ? "<green>✔ Selecionado" : "<gray>Clique para selecionar")
                    .glowIf(isSelected)
                    .build());
        }

        inventory.setItem(27, new ItemBuilder(Material.DARK_OAK_DOOR)
                .setName("<yellow>← Voltar")
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) { return; }

        int slot = event.getRawSlot();

        if (slot == 27) {
            clicker.closeInventory();
            if (onBack != null) { onBack.accept(clicker); }
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20};
        for (int i = 0; i < slots.length && i < SELECTABLE_TYPES.length; i++) {
            if (slot == slots[i]) {
                clicker.closeInventory();
                if (onSelect != null) { onSelect.accept(clicker, SELECTABLE_TYPES[i]); }
                return;
            }
        }
    }

    private Material getTypeIcon(SmelteryBlockType type) {
        return switch (type) {
            case WALL -> Material.NETHER_BRICKS;
            case CONTROLLER -> Material.BLAST_FURNACE;
            case DRAIN -> Material.HOPPER;
            case TANK_WINDOW -> Material.TINTED_GLASS;
            case ITEM_INPUT -> Material.DROPPER;
            case FUEL_INPUT -> Material.BARREL;
            case CASTING_TABLE -> Material.SMOOTH_STONE_SLAB;
            case CASTING_BASIN -> Material.CAULDRON;
            case AIR -> Material.GLASS;
        };
    }

    public void setOnSelect(BiConsumer<Player, SmelteryBlockType> cb) { this.onSelect = cb; }
    public void setOnBack(Consumer<Player> cb) { this.onBack = cb; }
}
