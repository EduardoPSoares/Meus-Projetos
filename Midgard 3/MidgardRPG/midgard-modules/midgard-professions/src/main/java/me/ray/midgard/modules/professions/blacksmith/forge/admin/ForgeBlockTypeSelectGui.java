package me.ray.midgard.modules.professions.blacksmith.forge.admin;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.blacksmith.forge.shared.AbstractCreationSession.ScannedBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Small GUI for selecting a ForgeBlockType role for a specific block.
 */
public class ForgeBlockTypeSelectGui extends BaseGui {

    private final ScannedBlock<ForgeBlock.ForgeBlockType> block;
    private final ForgeCreationSession session;

    private BiConsumer<Player, ForgeBlock.ForgeBlockType> onSelect;
    private Consumer<Player> onBack;

    // Layout: 3 rows, types in middle slots
    private static final ForgeBlock.ForgeBlockType[] SELECTABLE_TYPES = {
            ForgeBlock.ForgeBlockType.STRUCTURE,
            ForgeBlock.ForgeBlockType.FURNACE,
            ForgeBlock.ForgeBlockType.ANVIL,
            ForgeBlock.ForgeBlockType.CAULDRON,
            ForgeBlock.ForgeBlockType.GRINDSTONE,
            ForgeBlock.ForgeBlockType.SMITHING_TABLE,
            ForgeBlock.ForgeBlockType.CAMPFIRE,
            ForgeBlock.ForgeBlockType.BLAST_FURNACE,
            ForgeBlock.ForgeBlockType.ENCHANTING_TABLE,
            ForgeBlock.ForgeBlockType.FUEL_ZONE
    };

    public ForgeBlockTypeSelectGui(Player player, ScannedBlock<ForgeBlock.ForgeBlockType> block,
                                   ForgeCreationSession session) {
        super(player, 4, "<dark_gray>⚒ Selecionar Função");
        this.block = block;
        this.session = session;
    }

    @Override
    public void initializeItems() {
        var border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, border);
        }

        // Info about selected block at top center
        Material displayMat = block.material().isItem() ? block.material() : Material.STONE;
        ForgeBlock.ForgeBlockType currentType = session.getAssignedRole(block.locationKey());

        inventory.setItem(4, new ItemBuilder(displayMat)
                .setName("<white>" + block.material().name().toLowerCase().replace("_", " "))
                .addLore("<gray>Posição: <white>" + block.worldX() + ", " + block.worldY() + ", " + block.worldZ())
                .addLore("<gray>Função atual: <yellow>" + ForgeSetupGui.getTypeName(currentType))
                .addLore("")
                .addLore("<gray>Selecione a nova função abaixo:")
                .build());

        // Type buttons in rows 1-2 (slots 10-16, 19-25)
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};

        for (int i = 0; i < SELECTABLE_TYPES.length && i < slots.length; i++) {
            ForgeBlock.ForgeBlockType type = SELECTABLE_TYPES[i];
            Material icon = getTypeIcon(type);
            boolean isSelected = type == currentType;

            inventory.setItem(slots[i], new ItemBuilder(icon)
                    .setName((isSelected ? "<green>" : "<yellow>") + ForgeSetupGui.getTypeName(type))
                    .addLore(isSelected ? "<green>✔ Selecionado" : "<gray>Clique para selecionar")
                    .glowIf(isSelected)
                    .build());
        }

        // Back button
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

        // Check if a type button was clicked
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < slots.length && i < SELECTABLE_TYPES.length; i++) {
            if (slot == slots[i]) {
                clicker.closeInventory();
                if (onSelect != null) { onSelect.accept(clicker, SELECTABLE_TYPES[i]); }
                return;
            }
        }
    }

    private Material getTypeIcon(ForgeBlock.ForgeBlockType type) {
        return switch (type) {
            case STRUCTURE -> Material.STONE_BRICKS;
            case FURNACE -> Material.FURNACE;
            case ANVIL -> Material.ANVIL;
            case CAULDRON -> Material.CAULDRON;
            case GRINDSTONE -> Material.GRINDSTONE;
            case SMITHING_TABLE -> Material.SMITHING_TABLE;
            case CAMPFIRE -> Material.CAMPFIRE;
            case BLAST_FURNACE -> Material.BLAST_FURNACE;
            case ENCHANTING_TABLE -> Material.ENCHANTING_TABLE;
            case FUEL_ZONE -> Material.MAGMA_BLOCK;
            case AIR -> Material.GLASS;
        };
    }

    public void setOnSelect(BiConsumer<Player, ForgeBlock.ForgeBlockType> cb) { this.onSelect = cb; }
    public void setOnBack(Consumer<Player> cb) { this.onBack = cb; }
}
