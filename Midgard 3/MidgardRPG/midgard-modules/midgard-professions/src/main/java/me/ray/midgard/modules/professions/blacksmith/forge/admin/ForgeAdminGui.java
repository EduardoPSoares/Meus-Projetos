package me.ray.midgard.modules.professions.blacksmith.forge.admin;

import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeTemplate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Main admin panel listing all forge templates with pagination.
 * Templates are blueprints that players can obtain and build.
 */
public class ForgeAdminGui extends PaginatedGui<ForgeTemplate> {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.forge_admin." + key); }

    private BiConsumer<Player, ForgeTemplate> onTemplateClick;
    private Consumer<Player> onCreateNew;
    private final List<ForgeTemplate> sourceTemplates;

    public ForgeAdminGui(Player player, List<ForgeTemplate> templates) {
        super(player, ProfessionsModule.getInstance().getMessage("gui.forge_admin.title"), templates);
        this.sourceTemplates = templates;
    }

    @Override
    public ItemStack createItem(ForgeTemplate template) {
        String shortId = template.getTemplateId().toString().substring(0, 8);
        String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date(template.getCreatedAt()));
        String status = template.isActive() ? msg("status_active") : msg("status_inactive");

        Material icon = switch (template.getTier()) {
            case BASIC -> Material.STONE;
            case INTERMEDIATE -> Material.IRON_BLOCK;
            case ADVANCED -> Material.GOLD_BLOCK;
            case MASTER -> Material.DIAMOND_BLOCK;
            case LEGENDARY -> Material.NETHERITE_BLOCK;
        };

        var builder = new ItemBuilder(icon)
                .setName("<gold>" + template.getName())
                .addLore("<dark_gray>" + shortId)
                .addLore("")
                .addLore(msg("lore_type") + template.getTier().getDisplayName())
                .addLore(msg("lore_required_level") + template.getRequiredLevel())
                .addLore(msg("lore_created_at") + date)
                .addLore(msg("lore_status") + status);

        if (template.getSchematic() != null) {
            var sch = template.getSchematic();
            builder.addLore(msg("lore_dimensions") + sch.getWidth() + "x" + sch.getHeight() + "x" + sch.getDepth());
            builder.addLore(msg("lore_blocks") + sch.getSolidBlocks().size());
        }

        builder.addLore("")
                .addLore(msg("click_to_edit"))
                .glowIf(template.isActive());

        return builder.build();
    }

    @Override
    public void onItemClick(Player player, int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 3 || col < 1 || col > 7) { return; }

        int itemIndex = page * maxItemsPerPage + (row - 1) * 7 + (col - 1);
        if (itemIndex < 0 || itemIndex >= items.size()) { return; }

        ForgeTemplate template = items.get(itemIndex);
        if (onTemplateClick != null) {
            player.closeInventory();
            onTemplateClick.accept(player, template);
        }
    }

    @Override
    public void addMenuBorder() {
        super.addMenuBorder();

        // Stats info (slot 4)
        int total = items.size();
        long active = items.stream().filter(ForgeTemplate::isActive).count();
        inventory.setItem(4, new ItemBuilder(Material.BOOK)
                .setName(msg("stats_title"))
                .addLore(msg("lore_total") + total)
                .addLore(msg("lore_active") + active)
                .addLore(msg("lore_inactive") + (total - active))
                .addLore("")
                .addLore(msg("desc_line1"))
                .addLore(msg("desc_line2"))
                .build());

        // Create new template (slot 45)
        inventory.setItem(45, new ItemBuilder(Material.EMERALD)
                .setName(msg("create_new"))
                .addLore(msg("create_desc_1"))
                .addLore(msg("create_desc_2"))
                .addLore("")
                .addLore(msg("click_to_create"))
                .glow().build());

        // Refresh (slot 53)
        inventory.setItem(53, new ItemBuilder(Material.SUNFLOWER)
                .setName(msg("refresh"))
                .addLore(msg("refresh_desc"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45) {
            if (onCreateNew != null) {
                player.closeInventory();
                onCreateNew.accept(player);
            }
            return;
        }

        if (slot == 53) {
            this.items = sourceTemplates;
            this.page = 0;
            initializeItems();
            return;
        }

        super.onClick(event);
    }

    public void setOnTemplateClick(BiConsumer<Player, ForgeTemplate> cb) { this.onTemplateClick = cb; }
    public void setOnCreateNew(Consumer<Player> cb) { this.onCreateNew = cb; }
}
