package me.ray.midgard.modules.professions.blacksmith.forge.admin;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.data.ForgeRepository;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeTemplate;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/**
 * GUI for editing a forge template's properties.
 * Allows changing name, tier, required level, toggling active, and deleting.
 */
public class ForgeEditGui extends BaseGui {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.forge_edit." + key); }

    private static final int SLOT_INFO = 4;
    private static final int SLOT_NAME = 20;
    private static final int SLOT_TIER = 22;
    private static final int SLOT_LEVEL = 24;
    private static final int SLOT_TOGGLE_ACTIVE = 30;
    private static final int SLOT_DELETE = 32;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_CLOSE = 49;

    private final ForgeTemplate template;
    private final ForgeRepository repository;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private Consumer<Player> onBack;
    private Consumer<Player> onDelete;

    public ForgeEditGui(Player player, ForgeTemplate template, ForgeRepository repository) {
        super(player, 6, ProfessionsModule.getInstance().getMessage("gui.forge_edit.title").replace("%name%", template.getName()));
        this.template = template;
        this.repository = repository;
    }

    @Override
    public void initializeItems() {
        var border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        String shortId = template.getTemplateId().toString().substring(0, 8);
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(template.getCreatedAt()));

        // Info header
        var infoBuilder = new ItemBuilder(Material.ANVIL)
                .setName("<gold><bold>⚒ " + template.getName() + "</bold>")
                .addLore("<dark_gray>ID: " + shortId)
                .addLore("")
                .addLore(msg("lore_type") + template.getTier().getDisplayName())
                .addLore(msg("lore_required_level") + template.getRequiredLevel())
                .addLore(msg("lore_created_at") + date)
                .addLore(msg("lore_status") + (template.isActive() ? msg("status_active") : msg("status_inactive")));

        if (template.getSchematic() != null) {
            var sch = template.getSchematic();
            infoBuilder.addLore(msg("lore_dimensions") + sch.getWidth() + "x" + sch.getHeight() + "x" + sch.getDepth());
            infoBuilder.addLore(msg("lore_blocks") + sch.getSolidBlocks().size());
        }
        inventory.setItem(SLOT_INFO, infoBuilder.build());

        // Name
        inventory.setItem(SLOT_NAME, new ItemBuilder(Material.NAME_TAG)
                .setName(msg("rename_title"))
                .addLore(msg("lore_current_name") + template.getName())
                .addLore("")
                .addLore(msg("click_to_rename"))
                .addLore(msg("type_new_name_in_chat"))
                .build());

        // Tier
        inventory.setItem(SLOT_TIER, new ItemBuilder(Material.NETHER_STAR)
                .setName(msg("change_tier_title"))
                .addLore(msg("lore_current_tier") + template.getTier().getDisplayName())
                .addLore("")
                .addLore(msg("click_left_next"))
                .addLore(msg("click_right_prev"))
                .glow().build());

        // Level
        inventory.setItem(SLOT_LEVEL, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(msg("change_level_title"))
                .addLore(msg("lore_required_level") + template.getRequiredLevel())
                .addLore("")
                .addLore(msg("click_left_plus"))
                .addLore(msg("click_right_minus"))
                .addLore(msg("shift_click_ten"))
                .build());

        // Toggle active
        boolean active = template.isActive();
        inventory.setItem(SLOT_TOGGLE_ACTIVE, new ItemBuilder(active ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(active ? msg("toggle_active_title") : msg("toggle_inactive_title"))
                .addLore(active
                        ? msg("toggle_active_desc")
                        : msg("toggle_inactive_desc"))
                .addLore("")
                .addLore(active ? msg("click_to_deactivate") : msg("click_to_activate"))
                .build());

        // Delete
        inventory.setItem(SLOT_DELETE, new ItemBuilder(Material.TNT)
                .setName(msg("delete_title"))
                .addLore(msg("delete_desc"))
                .addLore("")
                .addLore(msg("delete_warning"))
                .addLore(msg("shift_click_delete"))
                .build());

        // Back
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.DARK_OAK_DOOR)
                .setName(msg("back"))
                .addLore(msg("back_desc"))
                .build());

        // Close
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .setName(msg("close"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player clicker)) { return; }

        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_NAME -> {
                clicker.closeInventory();
                clicker.sendMessage(mm.deserialize(ProfessionsModule.getInstance().getMessage("forge.admin.enter_new_name")));
                ForgeCreationManager manager = ForgeCreationManager.getInstance();
                if (manager != null) {
                    manager.setAwaitingNameInput(clicker.getUniqueId(), template, p -> {
                        saveTemplate();
                        openRefreshed(p);
                    });
                }
            }
            case SLOT_TIER -> {
                ForgeTier[] tiers = ForgeTier.values();
                int idx = template.getTier().ordinal();
                if (event.isLeftClick()) {
                    idx = (idx + 1) % tiers.length;
                } else if (event.isRightClick()) {
                    idx = (idx - 1 + tiers.length) % tiers.length;
                }
                template.setTier(tiers[idx]);
                saveTemplate();
                initializeItems();
            }
            case SLOT_LEVEL -> {
                int delta = event.isShiftClick() ? 10 : 1;
                if (event.isRightClick()) { delta = -delta; }
                int newLevel = Math.max(1, Math.min(100, template.getRequiredLevel() + delta));
                template.setRequiredLevel(newLevel);
                saveTemplate();
                initializeItems();
            }
            case SLOT_TOGGLE_ACTIVE -> {
                template.setActive(!template.isActive());
                saveTemplate();
                clicker.sendMessage(mm.deserialize(template.isActive()
                        ? ProfessionsModule.getInstance().getMessage("forge.admin.template_toggled_on")
                        : ProfessionsModule.getInstance().getMessage("forge.admin.template_toggled_off")));
                initializeItems();
            }
            case SLOT_DELETE -> {
                if (!event.isShiftClick()) {
                    clicker.sendMessage(mm.deserialize(ProfessionsModule.getInstance().getMessage("forge.admin.confirm_delete")));
                    return;
                }
                clicker.closeInventory();
                if (repository != null) {
                    repository.deleteTemplate(template.getTemplateId());
                }
                clicker.sendMessage(mm.deserialize(ProfessionsModule.getInstance().getMessage("forge.admin.template_deleted").replace("%name%", template.getName())));
                if (onDelete != null) { onDelete.accept(clicker); }
            }
            case SLOT_BACK -> {
                clicker.closeInventory();
                if (onBack != null) { onBack.accept(clicker); }
            }
            case SLOT_CLOSE -> clicker.closeInventory();
        }
    }

    private void saveTemplate() {
        if (repository != null) {
            repository.saveTemplate(template);
        }
    }

    private void openRefreshed(Player p) {
        ForgeEditGui refreshed = new ForgeEditGui(p, template, repository);
        refreshed.setOnBack(this.onBack);
        refreshed.setOnDelete(this.onDelete);
        refreshed.open();
    }

    public void setOnBack(Consumer<Player> cb) { this.onBack = cb; }
    public void setOnDelete(Consumer<Player> cb) { this.onDelete = cb; }
}
