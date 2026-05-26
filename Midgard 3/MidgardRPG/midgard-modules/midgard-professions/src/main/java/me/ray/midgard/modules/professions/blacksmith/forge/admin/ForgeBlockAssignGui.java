package me.ray.midgard.modules.professions.blacksmith.forge.admin;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.blacksmith.forge.shared.AbstractCreationSession.ScannedBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * Paginated GUI showing all scanned blocks in the forge area.
 * Admin clicks a block to change its assigned role (ForgeBlockType).
 */
public class ForgeBlockAssignGui extends PaginatedGui<ScannedBlock<ForgeBlock.ForgeBlockType>> {

    private final ForgeCreationSession session;
    private Consumer<Player> onBack;

    public ForgeBlockAssignGui(Player player, ForgeCreationSession session) {
        super(player, "<dark_gray>⚒ Atribuir Blocos", session.getScannedBlocks());
        this.session = session;
    }

    @Override
    public ItemStack createItem(ScannedBlock<ForgeBlock.ForgeBlockType> block) {
        ForgeBlock.ForgeBlockType assigned = session.getAssignedRole(block.locationKey());
        Material displayMat = block.material();
        // Some materials can't be display items
        if (!displayMat.isItem()) { displayMat = Material.STONE; }

        String typeName = ForgeSetupGui.getTypeName(assigned);
        String typeColor = assigned == ForgeBlock.ForgeBlockType.STRUCTURE ? "<gray>" : "<green>";

        return new ItemBuilder(displayMat)
                .setName("<white>" + formatMaterialName(block.material()))
                .addLore("<gray>Posição: <white>" + block.worldX() + ", " + block.worldY() + ", " + block.worldZ())
                .addLore("<gray>Função: " + typeColor + typeName)
                .addLore("")
                .addLore(assigned != ForgeBlock.ForgeBlockType.STRUCTURE
                        ? "<green>✔ Atribuído"
                        : "<gray>Sem função especial")
                .addLore("")
                .addLore("<yellow>Clique para alterar função")
                .glowIf(assigned != ForgeBlock.ForgeBlockType.STRUCTURE)
                .build();
    }

    @Override
    public void onItemClick(Player player, int slot) {
        // Calculate which item was clicked
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 3 || col < 1 || col > 7) { return; }

        int itemIndex = page * maxItemsPerPage + (row - 1) * 7 + (col - 1);
        if (itemIndex < 0 || itemIndex >= items.size()) { return; }

        ScannedBlock<ForgeBlock.ForgeBlockType> block = items.get(itemIndex);

        // Open type selector
        ForgeBlockTypeSelectGui typeGui = new ForgeBlockTypeSelectGui(player, block, session);
        typeGui.setOnSelect((p, type) -> {
            session.assignRole(block.locationKey(), type);
            // Re-open this GUI at same page
            ForgeBlockAssignGui refreshed = new ForgeBlockAssignGui(p, session);
            refreshed.page = this.page;
            refreshed.setOnBack(this.onBack);
            refreshed.open();
        });
        typeGui.setOnBack(p -> {
            ForgeBlockAssignGui refreshed = new ForgeBlockAssignGui(p, session);
            refreshed.page = this.page;
            refreshed.setOnBack(this.onBack);
            refreshed.open();
        });
        player.closeInventory();
        typeGui.open();
    }

    @Override
    public void addMenuBorder() {
        super.addMenuBorder();

        // Add back button at slot 45
        inventory.setItem(45, new ItemBuilder(Material.DARK_OAK_DOOR)
                .setName("<yellow>← Voltar")
                .addLore("<gray>Voltar ao menu principal")
                .build());
    }

    @Override
    public void onClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45) {
            player.closeInventory();
            if (onBack != null) { onBack.accept(player); }
            return;
        }

        super.onClick(event);
    }

    public void setOnBack(Consumer<Player> cb) { this.onBack = cb; }

    private String formatMaterialName(Material mat) {
        return mat.name().toLowerCase().replace("_", " ");
    }
}
