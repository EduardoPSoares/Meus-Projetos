package me.ray.midgard.modules.professions.blacksmith.forge.smeltery.admin;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.blacksmith.forge.shared.AbstractCreationSession.ScannedBlock;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryBlockType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * GUI paginada mostrando todos os blocos escaneados na área da smeltery.
 * O admin clica em um bloco para alterar seu papel (SmelteryBlockType).
 */
public class SmelteryBlockAssignGui extends PaginatedGui<ScannedBlock<SmelteryBlockType>> {

    private final SmelteryCreationSession session;
    private Consumer<Player> onBack;

    public SmelteryBlockAssignGui(Player player, SmelteryCreationSession session) {
        super(player, "<dark_gray>⚗ Atribuir Blocos", session.getScannedBlocks());
        this.session = session;
    }

    @Override
    public ItemStack createItem(ScannedBlock<SmelteryBlockType> block) {
        SmelteryBlockType assigned = session.getAssignedRole(block.locationKey());
        Material displayMat = block.material();
        if (!displayMat.isItem()) { displayMat = Material.STONE; }

        String typeName = SmelterySetupGui.getTypeName(assigned);
        String typeColor = assigned == SmelteryBlockType.WALL ? "<gray>" : "<green>";

        return new ItemBuilder(displayMat)
                .setName("<white>" + formatMaterialName(block.material()))
                .addLore("<gray>Posição: <white>" + block.worldX() + ", " + block.worldY() + ", " + block.worldZ())
                .addLore("<gray>Função: " + typeColor + typeName)
                .addLore("")
                .addLore(assigned != SmelteryBlockType.WALL
                        ? "<green>✔ Atribuído"
                        : "<gray>Sem função especial")
                .addLore("")
                .addLore("<yellow>Clique para alterar função")
                .glowIf(assigned != SmelteryBlockType.WALL)
                .build();
    }

    @Override
    public void onItemClick(Player player, int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 3 || col < 1 || col > 7) { return; }

        int itemIndex = page * maxItemsPerPage + (row - 1) * 7 + (col - 1);
        if (itemIndex < 0 || itemIndex >= items.size()) { return; }

        ScannedBlock<SmelteryBlockType> block = items.get(itemIndex);

        SmelteryBlockTypeSelectGui typeGui = new SmelteryBlockTypeSelectGui(player, block, session);
        typeGui.setOnSelect((p, type) -> {
            session.assignRole(block.locationKey(), type);
            SmelteryBlockAssignGui refreshed = new SmelteryBlockAssignGui(p, session);
            refreshed.page = this.page;
            refreshed.setOnBack(this.onBack);
            refreshed.open();
        });
        typeGui.setOnBack(p -> {
            SmelteryBlockAssignGui refreshed = new SmelteryBlockAssignGui(p, session);
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

        inventory.setItem(45, new ItemBuilder(Material.DARK_OAK_DOOR)
                .setName("<yellow>← Voltar")
                .addLore("<gray>Voltar ao menu principal")
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
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
