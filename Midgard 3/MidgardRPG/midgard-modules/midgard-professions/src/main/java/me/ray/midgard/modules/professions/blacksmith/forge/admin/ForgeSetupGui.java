package me.ray.midgard.modules.professions.blacksmith.forge.admin;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeBlock;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Main GUI for admin forge template creation.
 * Flow: set name/type/level → select area positions → scan → validate → confirm.
 */
public class ForgeSetupGui extends BaseGui {

    private String msg(String key) { return ProfessionsModule.getInstance().getMessage("gui.forge_setup." + key); }

    // Row 0: info header
    private static final int SLOT_INFO = 4;

    // Row 1: name, tier, level
    private static final int SLOT_NAME = 11;
    private static final int SLOT_TIER = 13;
    private static final int SLOT_LEVEL = 15;

    // Row 2: positions + scan
    private static final int SLOT_POS1 = 20;
    private static final int SLOT_SCAN = 22;
    private static final int SLOT_POS2 = 24;

    // Row 3: blocks + validation
    private static final int SLOT_BLOCKS_SUMMARY = 29;
    private static final int SLOT_ASSIGN_BLOCKS = 31;
    private static final int SLOT_VALIDATION = 33;

    // Row 5: actions
    private static final int SLOT_CANCEL = 45;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_CONFIRM = 53;

    private final ForgeCreationSession session;

    private Consumer<Player> onOpenBlockAssign;
    private Consumer<Player> onConfirm;
    private Consumer<Player> onCancel;
    private Consumer<Player> onSetName;

    public ForgeSetupGui(Player player, ForgeCreationSession session) {
        super(player, 6, ProfessionsModule.getInstance().getMessage("gui.forge_setup.title"));
        this.session = session;
    }

    @Override
    public void initializeItems() {
        var border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        boolean hasPos1 = session.getPos1() != null;
        boolean hasPos2 = session.getPos2() != null;
        boolean scanned = session.isScanned();

        // --- Info header ---
        if (scanned) {
            Map<ForgeBlock.ForgeBlockType, Integer> roles = session.getRoleCounts();
            int interactiveCount = roles.values().stream().mapToInt(Integer::intValue).sum();
            inventory.setItem(SLOT_INFO, new ItemBuilder(Material.ANVIL)
                    .setName("<gold><bold>⚒ " + session.getName() + "</bold>")
                    .addLore(msg("lore_type") + session.getTier().getDisplayName())
                    .addLore(msg("lore_required_level") + session.getRequiredLevel())
                    .addLore(msg("lore_dimensions") + session.getWidth() + "x" + session.getHeight() + "x" + session.getDepth())
                    .addLore(msg("lore_blocks") + session.getScannedBlocks().size())
                    .addLore(msg("lore_interactive_blocks") + interactiveCount)
                    .build());
        } else {
            inventory.setItem(SLOT_INFO, new ItemBuilder(Material.PAPER)
                    .setName(msg("how_to_create"))
                    .addLore(msg("step_1"))
                    .addLore(msg("step_2"))
                    .addLore(msg("step_3"))
                    .addLore(msg("step_4"))
                    .addLore(msg("step_5"))
                    .build());
        }

        // --- Row 1: Name, Tier, Level ---

        // Name
        inventory.setItem(SLOT_NAME, new ItemBuilder(Material.NAME_TAG)
                .setName(msg("name_title"))
                .addLore(msg("lore_current") + session.getName())
                .addLore("")
                .addLore(msg("click_to_change"))
                .addLore(msg("type_name_in_chat"))
                .build());

        // Tier
        ForgeTier tier = session.getTier();
        inventory.setItem(SLOT_TIER, new ItemBuilder(Material.NETHER_STAR)
                .setName(msg("tier_title"))
                .addLore(msg("lore_current_tier") + tier.getDisplayName())
                .addLore("")
                .addLore(msg("click_left_next"))
                .addLore(msg("click_right_prev"))
                .glow().build());

        // Level
        inventory.setItem(SLOT_LEVEL, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(msg("level_title"))
                .addLore(msg("lore_required_level") + session.getRequiredLevel())
                .addLore("")
                .addLore(msg("click_left_plus"))
                .addLore(msg("click_right_minus"))
                .addLore(msg("shift_click_ten"))
                .build());

        // --- Row 2: Positions + Scan ---

        // Pos1
        if (hasPos1) {
            inventory.setItem(SLOT_POS1, new ItemBuilder(Material.LIME_CONCRETE)
                    .setName(msg("pos1_set_title"))
                    .addLore("<white>" + formatLoc(session.getPos1()))
                    .addLore("")
                    .addLore(msg("click_to_redefine"))
                    .build());
        } else {
            inventory.setItem(SLOT_POS1, new ItemBuilder(Material.RED_CONCRETE)
                    .setName(msg("pos1_unset_title"))
                    .addLore(msg("pos1_desc_1"))
                    .addLore(msg("pos1_desc_2"))
                    .addLore("")
                    .addLore(msg("click_to_capture"))
                    .build());
        }

        // Scan
        if (scanned) {
            inventory.setItem(SLOT_SCAN, new ItemBuilder(Material.ENDER_EYE)
                    .setName(msg("scan_done_title"))
                    .addLore(msg("lore_blocks_detected") + session.getScannedBlocks().size())
                    .addLore("")
                    .addLore(msg("click_to_rescan"))
                    .glow().build());
        } else if (hasPos1 && hasPos2) {
            inventory.setItem(SLOT_SCAN, new ItemBuilder(Material.ENDER_EYE)
                    .setName(msg("scan_ready_title"))
                    .addLore(msg("scan_desc_1"))
                    .addLore(msg("scan_desc_2"))
                    .addLore("")
                    .addLore(msg("click_to_scan"))
                    .glow().build());
        } else {
            inventory.setItem(SLOT_SCAN, new ItemBuilder(Material.GRAY_DYE)
                    .setName(msg("scan_disabled_title"))
                    .addLore(msg("scan_disabled_desc"))
                    .build());
        }

        // Pos2
        if (hasPos2) {
            inventory.setItem(SLOT_POS2, new ItemBuilder(Material.LIME_CONCRETE)
                    .setName(msg("pos2_set_title"))
                    .addLore("<white>" + formatLoc(session.getPos2()))
                    .addLore("")
                    .addLore(msg("click_to_redefine"))
                    .build());
        } else {
            inventory.setItem(SLOT_POS2, new ItemBuilder(Material.RED_CONCRETE)
                    .setName(msg("pos2_unset_title"))
                    .addLore(msg("pos2_desc_1"))
                    .addLore(msg("pos2_desc_2"))
                    .addLore("")
                    .addLore(msg("click_to_capture"))
                    .build());
        }

        // --- Row 3: Blocks + Validation (only after scan) ---
        if (scanned) {
            Map<ForgeBlock.ForgeBlockType, Integer> roles = session.getRoleCounts();

            // Blocks summary
            var summaryBuilder = new ItemBuilder(Material.BOOKSHELF)
                    .setName(msg("blocks_summary_title"))
                    .addLore("");
            for (ForgeBlock.ForgeBlockType type : ForgeBlock.ForgeBlockType.values()) {
                if (type == ForgeBlock.ForgeBlockType.AIR || type == ForgeBlock.ForgeBlockType.STRUCTURE) { continue; }
                int count = roles.getOrDefault(type, 0);
                String color = count > 0 ? "<green>" : "<red>";
                summaryBuilder.addLore(color + getTypeName(type) + ": <white>" + count);
            }
            inventory.setItem(SLOT_BLOCKS_SUMMARY, summaryBuilder.build());

            // Assign blocks
            inventory.setItem(SLOT_ASSIGN_BLOCKS, new ItemBuilder(Material.WRITABLE_BOOK)
                    .setName(msg("assign_blocks_title"))
                    .addLore(msg("assign_blocks_desc_1"))
                    .addLore(msg("assign_blocks_desc_2"))
                    .addLore("")
                    .addLore(msg("click_to_open"))
                    .glow().build());

            // Validation
            boolean valid = session.hasRequiredRoles();
            if (valid) {
                inventory.setItem(SLOT_VALIDATION, new ItemBuilder(Material.LIME_DYE)
                        .setName(msg("validation_ok_title"))
                        .addLore(msg("validation_ok_desc"))
                        .addLore(msg("ready_to_create"))
                        .build());
            } else {
                var vb = new ItemBuilder(Material.RED_DYE)
                        .setName(msg("validation_fail_title"))
                        .addLore(msg("validation_missing"));
                for (ForgeBlock.ForgeBlockType missing : session.getMissingRoles()) {
                    vb.addLore("<red> • " + getTypeName(missing));
                }
                vb.addLore("");
                vb.addLore(msg("click_receive_blocks"));
                inventory.setItem(SLOT_VALIDATION, vb.build());
            }
        }

        // Confirm
        boolean canConfirm = scanned && session.hasRequiredRoles();
        inventory.setItem(SLOT_CONFIRM, new ItemBuilder(canConfirm ? Material.LIME_WOOL : Material.GRAY_WOOL)
                .setName(canConfirm ? msg("confirm_title") : msg("confirm_incomplete_title"))
                .addLore(canConfirm ? msg("confirm_desc") : msg("confirm_incomplete_desc"))
                .build());

        // Cancel
        inventory.setItem(SLOT_CANCEL, new ItemBuilder(Material.RED_WOOL)
                .setName(msg("cancel_title"))
                .addLore(msg("cancel_desc"))
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

        var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        int slot = event.getRawSlot();

        switch (slot) {
            case SLOT_NAME -> {
                clicker.closeInventory();
                clicker.sendMessage(mm.deserialize(ProfessionsModule.getInstance().getMessage("forge.admin.enter_template_name")));
                if (onSetName != null) { onSetName.accept(clicker); }
            }
            case SLOT_TIER -> {
                ForgeTier current = session.getTier();
                ForgeTier[] tiers = ForgeTier.values();
                int idx = current.ordinal();
                if (event.isLeftClick()) {
                    idx = (idx + 1) % tiers.length;
                } else if (event.isRightClick()) {
                    idx = (idx - 1 + tiers.length) % tiers.length;
                }
                session.setTier(tiers[idx]);
                initializeItems();
            }
            case SLOT_LEVEL -> {
                int delta = event.isShiftClick() ? 10 : 1;
                if (event.isRightClick()) { delta = -delta; }
                int newLevel = Math.max(1, Math.min(100, session.getRequiredLevel() + delta));
                session.setRequiredLevel(newLevel);
                initializeItems();
            }
            case SLOT_POS1 -> {
                session.setPos1(clicker.getLocation().toBlockLocation());
                clicker.sendMessage(mm.deserialize(
                        ProfessionsModule.getInstance().getMessage("forge.admin.pos1_set").replace("%loc%", formatLoc(session.getPos1()))));
                session.startVisualization(clicker);
                initializeItems();
            }
            case SLOT_POS2 -> {
                if (session.getPos1() != null && !clicker.getWorld().equals(session.getPos1().getWorld())) {
                    clicker.sendMessage(mm.deserialize(
                            ProfessionsModule.getInstance().getMessage("forge.admin.different_worlds")));
                    return;
                }
                session.setPos2(clicker.getLocation().toBlockLocation());
                clicker.sendMessage(mm.deserialize(
                        ProfessionsModule.getInstance().getMessage("forge.admin.pos2_set").replace("%loc%", formatLoc(session.getPos2()))));
                session.startVisualization(clicker);
                initializeItems();
            }
            case SLOT_SCAN -> {
                if (session.hasBothPositions()) {
                    int count = session.scanArea();
                    clicker.sendMessage(mm.deserialize(
                            ProfessionsModule.getInstance().getMessage("forge.admin.area_scanned").replace("%count%", String.valueOf(count))));
                    session.startVisualization(clicker);
                    initializeItems();
                }
            }
            case SLOT_ASSIGN_BLOCKS -> {
                if (!session.isScanned()) { return; }
                clicker.closeInventory();
                if (onOpenBlockAssign != null) { onOpenBlockAssign.accept(clicker); }
            }
            case SLOT_VALIDATION -> {
                if (!session.isScanned() || session.hasRequiredRoles()) { return; }
                var missing = session.getMissingRoles();
                for (ForgeBlock.ForgeBlockType type : missing) {
                    Material mat = getRepresentativeMaterial(type);
                    if (mat != null && !mat.isAir()) {
                        clicker.getInventory().addItem(new ItemStack(mat, 1));
                    }
                }
                clicker.sendMessage(mm.deserialize(
                        ProfessionsModule.getInstance().getMessage("forge.admin.missing_blocks_given").replace("%count%", String.valueOf(missing.size()))));
            }
            case SLOT_CONFIRM -> {
                if (session.isScanned() && session.hasRequiredRoles()) {
                    clicker.closeInventory();
                    if (onConfirm != null) { onConfirm.accept(clicker); }
                }
            }
            case SLOT_CANCEL -> {
                clicker.closeInventory();
                if (onCancel != null) { onCancel.accept(clicker); }
            }
            case SLOT_CLOSE -> clicker.closeInventory();
        }
    }

    // === Callbacks ===

    public void setOnOpenBlockAssign(Consumer<Player> cb) { this.onOpenBlockAssign = cb; }
    public void setOnConfirm(Consumer<Player> cb) { this.onConfirm = cb; }
    public void setOnCancel(Consumer<Player> cb) { this.onCancel = cb; }
    public void setOnSetName(Consumer<Player> cb) { this.onSetName = cb; }

    // === Helpers ===

    private String formatLoc(org.bukkit.Location loc) {
        if (loc == null) { return msg("not_defined"); }
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    static String getTypeName(ForgeBlock.ForgeBlockType type) {
        ProfessionsModule mod = ProfessionsModule.getInstance();
        return switch (type) {
            case FURNACE -> mod.getMessage("gui.forge_setup.block_furnace");
            case ANVIL -> mod.getMessage("gui.forge_setup.block_anvil");
            case CAULDRON -> mod.getMessage("gui.forge_setup.block_cauldron");
            case GRINDSTONE -> mod.getMessage("gui.forge_setup.block_grindstone");
            case SMITHING_TABLE -> mod.getMessage("gui.forge_setup.block_smithing_table");
            case CAMPFIRE -> mod.getMessage("gui.forge_setup.block_campfire");
            case BLAST_FURNACE -> mod.getMessage("gui.forge_setup.block_blast_furnace");
            case ENCHANTING_TABLE -> mod.getMessage("gui.forge_setup.block_enchanting_table");
            case FUEL_ZONE -> mod.getMessage("gui.forge_setup.block_fuel_zone");
            case STRUCTURE -> mod.getMessage("gui.forge_setup.block_structure");
            case AIR -> mod.getMessage("gui.forge_setup.block_air");
        };
    }

    private static Material getRepresentativeMaterial(ForgeBlock.ForgeBlockType type) {
        return switch (type) {
            case FURNACE -> Material.FURNACE;
            case ANVIL -> Material.ANVIL;
            case CAULDRON -> Material.CAULDRON;
            case GRINDSTONE -> Material.GRINDSTONE;
            case SMITHING_TABLE -> Material.SMITHING_TABLE;
            case CAMPFIRE -> Material.CAMPFIRE;
            case BLAST_FURNACE -> Material.BLAST_FURNACE;
            case ENCHANTING_TABLE -> Material.ENCHANTING_TABLE;
            case FUEL_ZONE -> Material.MAGMA_BLOCK;
            default -> null;
        };
    }
}
