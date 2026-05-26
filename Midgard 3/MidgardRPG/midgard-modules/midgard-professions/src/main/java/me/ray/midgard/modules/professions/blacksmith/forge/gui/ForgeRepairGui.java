package me.ray.midgard.modules.professions.blacksmith.forge.gui;

import me.ray.midgard.core.gui.BaseGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI para reparar itens danificados na forja.
 * O jogador coloca um item danificado no slot de entrada,
 * a GUI calcula o custo de materiais necessários para reparo,
 * e ao confirmar, consome materiais do inventário e repara o item.
 */
public class ForgeRepairGui extends BaseGui {

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage(key); }
    private static String guiMsg(String key) { return ProfessionsModule.getInstance().getMessage("gui.repair." + key); }

    private static final int SLOT_INPUT = 20;
    private static final int SLOT_COST_INFO = 22;
    private static final int SLOT_CONFIRM = 24;
    private static final int SLOT_BACK = 49;

    private static final Map<Material, Material> REPAIR_MATERIALS = Map.ofEntries(
            Map.entry(Material.WOODEN_SWORD, Material.OAK_PLANKS),
            Map.entry(Material.WOODEN_AXE, Material.OAK_PLANKS),
            Map.entry(Material.WOODEN_PICKAXE, Material.OAK_PLANKS),
            Map.entry(Material.WOODEN_SHOVEL, Material.OAK_PLANKS),
            Map.entry(Material.WOODEN_HOE, Material.OAK_PLANKS),
            Map.entry(Material.STONE_SWORD, Material.COBBLESTONE),
            Map.entry(Material.STONE_AXE, Material.COBBLESTONE),
            Map.entry(Material.STONE_PICKAXE, Material.COBBLESTONE),
            Map.entry(Material.STONE_SHOVEL, Material.COBBLESTONE),
            Map.entry(Material.STONE_HOE, Material.COBBLESTONE),
            Map.entry(Material.IRON_SWORD, Material.IRON_INGOT),
            Map.entry(Material.IRON_AXE, Material.IRON_INGOT),
            Map.entry(Material.IRON_PICKAXE, Material.IRON_INGOT),
            Map.entry(Material.IRON_SHOVEL, Material.IRON_INGOT),
            Map.entry(Material.IRON_HOE, Material.IRON_INGOT),
            Map.entry(Material.IRON_HELMET, Material.IRON_INGOT),
            Map.entry(Material.IRON_CHESTPLATE, Material.IRON_INGOT),
            Map.entry(Material.IRON_LEGGINGS, Material.IRON_INGOT),
            Map.entry(Material.IRON_BOOTS, Material.IRON_INGOT),
            Map.entry(Material.GOLDEN_SWORD, Material.GOLD_INGOT),
            Map.entry(Material.GOLDEN_AXE, Material.GOLD_INGOT),
            Map.entry(Material.GOLDEN_PICKAXE, Material.GOLD_INGOT),
            Map.entry(Material.GOLDEN_SHOVEL, Material.GOLD_INGOT),
            Map.entry(Material.GOLDEN_HOE, Material.GOLD_INGOT),
            Map.entry(Material.GOLDEN_HELMET, Material.GOLD_INGOT),
            Map.entry(Material.GOLDEN_CHESTPLATE, Material.GOLD_INGOT),
            Map.entry(Material.GOLDEN_LEGGINGS, Material.GOLD_INGOT),
            Map.entry(Material.GOLDEN_BOOTS, Material.GOLD_INGOT),
            Map.entry(Material.DIAMOND_SWORD, Material.DIAMOND),
            Map.entry(Material.DIAMOND_AXE, Material.DIAMOND),
            Map.entry(Material.DIAMOND_PICKAXE, Material.DIAMOND),
            Map.entry(Material.DIAMOND_SHOVEL, Material.DIAMOND),
            Map.entry(Material.DIAMOND_HOE, Material.DIAMOND),
            Map.entry(Material.DIAMOND_HELMET, Material.DIAMOND),
            Map.entry(Material.DIAMOND_CHESTPLATE, Material.DIAMOND),
            Map.entry(Material.DIAMOND_LEGGINGS, Material.DIAMOND),
            Map.entry(Material.DIAMOND_BOOTS, Material.DIAMOND),
            Map.entry(Material.NETHERITE_SWORD, Material.NETHERITE_INGOT),
            Map.entry(Material.NETHERITE_AXE, Material.NETHERITE_INGOT),
            Map.entry(Material.NETHERITE_PICKAXE, Material.NETHERITE_INGOT),
            Map.entry(Material.NETHERITE_SHOVEL, Material.NETHERITE_INGOT),
            Map.entry(Material.NETHERITE_HOE, Material.NETHERITE_INGOT),
            Map.entry(Material.NETHERITE_HELMET, Material.NETHERITE_INGOT),
            Map.entry(Material.NETHERITE_CHESTPLATE, Material.NETHERITE_INGOT),
            Map.entry(Material.NETHERITE_LEGGINGS, Material.NETHERITE_INGOT),
            Map.entry(Material.NETHERITE_BOOTS, Material.NETHERITE_INGOT),
            Map.entry(Material.LEATHER_HELMET, Material.LEATHER),
            Map.entry(Material.LEATHER_CHESTPLATE, Material.LEATHER),
            Map.entry(Material.LEATHER_LEGGINGS, Material.LEATHER),
            Map.entry(Material.LEATHER_BOOTS, Material.LEATHER),
            Map.entry(Material.CHAINMAIL_HELMET, Material.IRON_NUGGET),
            Map.entry(Material.CHAINMAIL_CHESTPLATE, Material.IRON_NUGGET),
            Map.entry(Material.CHAINMAIL_LEGGINGS, Material.IRON_NUGGET),
            Map.entry(Material.CHAINMAIL_BOOTS, Material.IRON_NUGGET),
            Map.entry(Material.SHIELD, Material.OAK_PLANKS),
            Map.entry(Material.BOW, Material.STRING),
            Map.entry(Material.CROSSBOW, Material.STRING),
            Map.entry(Material.TRIDENT, Material.PRISMARINE_SHARD),
            Map.entry(Material.ELYTRA, Material.PHANTOM_MEMBRANE),
            Map.entry(Material.TURTLE_HELMET, Material.TURTLE_SCUTE)
    );

    private final ForgeStructure forge;
    private Consumer<Player> onBack;
    private ItemStack inputItem;

    public ForgeRepairGui(Player player, ForgeStructure forge) {
        super(player, 6, guiMsg("title"));
        this.forge = forge;
    }

    public void setOnBack(Consumer<Player> onBack) { this.onBack = onBack; }

    @Override
    public void initializeItems() {
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 54; i++) { inventory.setItem(i, border); }

        // Input slot — empty
        inventory.setItem(SLOT_INPUT, null);

        // Cost info
        inventory.setItem(SLOT_COST_INFO, new ItemBuilder(Material.PAPER)
                .setName(guiMsg("cost_title"))
                .addLore(guiMsg("place_item_line1"))
                .addLore(guiMsg("place_item_line2"))
                .build());

        // Confirm — disabled
        inventory.setItem(SLOT_CONFIRM, new ItemBuilder(Material.BARRIER)
                .setName(guiMsg("no_item"))
                .build());

        // Back
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .setName(guiMsg("back"))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) { return; }
        int slot = event.getRawSlot();

        // Allow interaction with input slot
        if (slot == SLOT_INPUT) {
            // Let the click happen, then update on next tick
            me.ray.midgard.core.utils.Task.sync(this::updateDisplay);
            return;
        }

        event.setCancelled(true);

        if (slot == SLOT_BACK) {
            returnInputItem(clicker);
            clicker.closeInventory();
            if (onBack != null) { onBack.accept(clicker); }
            return;
        }

        if (slot == SLOT_CONFIRM) {
            attemptRepair(clicker);
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player p) {
            returnInputItem(p);
        }
    }

    private void returnInputItem(Player player) {
        ItemStack item = inventory.getItem(SLOT_INPUT);
        if (item != null && item.getType() != Material.AIR) {
            player.getInventory().addItem(item).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            inventory.setItem(SLOT_INPUT, null);
        }
    }

    private void updateDisplay() {
        inputItem = inventory.getItem(SLOT_INPUT);

        if (inputItem == null || inputItem.getType() == Material.AIR) {
            inventory.setItem(SLOT_COST_INFO, new ItemBuilder(Material.PAPER)
                    .setName(guiMsg("cost_title"))
                    .addLore(guiMsg("place_item_line1"))
                    .addLore(guiMsg("place_item_line2"))
                    .build());
            inventory.setItem(SLOT_CONFIRM, new ItemBuilder(Material.BARRIER)
                    .setName(guiMsg("no_item"))
                    .build());
            return;
        }

        if (!(inputItem.getItemMeta() instanceof Damageable damageable) || !damageable.hasDamage()) {
            inventory.setItem(SLOT_COST_INFO, new ItemBuilder(Material.PAPER)
                    .setName(guiMsg("not_damaged"))
                    .addLore(guiMsg("full_durability"))
                    .build());
            inventory.setItem(SLOT_CONFIRM, new ItemBuilder(Material.BARRIER)
                    .setName(guiMsg("no_repair_needed"))
                    .build());
            return;
        }

        Material repairMat = REPAIR_MATERIALS.get(inputItem.getType());
        if (repairMat == null) {
            inventory.setItem(SLOT_COST_INFO, new ItemBuilder(Material.PAPER)
                    .setName(guiMsg("not_repairable"))
                    .addLore(guiMsg("not_repairable_lore1"))
                    .addLore(guiMsg("not_repairable_lore2"))
                    .build());
            inventory.setItem(SLOT_CONFIRM, new ItemBuilder(Material.BARRIER)
                    .setName(guiMsg("not_repairable"))
                    .build());
            return;
        }

        int cost = calculateRepairCost(inputItem);
        int available = countMaterial(player, repairMat);
        boolean canRepair = available >= cost;
        String matName = repairMat.name().toLowerCase().replace("_", " ");

        inventory.setItem(SLOT_COST_INFO, new ItemBuilder(repairMat)
                .setName(guiMsg("cost_title_active"))
                .addLore(guiMsg("material_label") + matName)
                .addLore(guiMsg("required_label") + cost)
                .addLore(guiMsg("available_label") + (canRepair ? "<green>" : "<red>") + available)
                .addLore("")
                .addLore(guiMsg("durability_label") +
                        String.format("%.0f%%", getDurabilityPercent(inputItem) * 100) +
                        " <gray>→ <green>100%")
                .build());

        inventory.setItem(SLOT_CONFIRM, canRepair
                ? new ItemBuilder(Material.LIME_CONCRETE)
                .setName(guiMsg("confirm"))
                .addLore(guiMsg("cost_label") + cost + "x " + matName)
                .addLore(guiMsg("click_to_repair"))
                .build()
                : new ItemBuilder(Material.RED_CONCRETE)
                .setName(guiMsg("insufficient"))
                .addLore(guiMsg("missing_prefix") + (cost - available) + "x <gray>" + matName)
                .build());
    }

    private void attemptRepair(Player clicker) {
        if (inputItem == null || inputItem.getType() == Material.AIR) { return; }
        if (!(inputItem.getItemMeta() instanceof Damageable damageable) || !damageable.hasDamage()) { return; }

        Material repairMat = REPAIR_MATERIALS.get(inputItem.getType());
        if (repairMat == null) { return; }

        int cost = calculateRepairCost(inputItem);
        if (countMaterial(clicker, repairMat) < cost) {
            clicker.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize(msg("forge.repair.insufficient_materials")));
            clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }

        // Consume materials
        removeMaterial(clicker, repairMat, cost);

        // Repair the item
        damageable.setDamage(0);
        inputItem.setItemMeta(damageable);

        forge.incrementItemsForged();

        clicker.playSound(clicker.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
        clicker.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(msg("forge.repair.success")));

        updateDisplay();
    }

    private int calculateRepairCost(ItemStack item) {
        if (!(item.getItemMeta() instanceof Damageable damageable)) { return 0; }
        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) { return 0; }
        double damagePercent = 1.0 - getDurabilityPercent(item);
        // 1 material per ~25% damage, minimum 1
        return Math.max(1, (int) Math.ceil(damagePercent * 4));
    }

    private double getDurabilityPercent(ItemStack item) {
        if (!(item.getItemMeta() instanceof Damageable damageable)) { return 1.0; }
        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) { return 1.0; }
        return 1.0 - ((double) damageable.getDamage() / maxDurability);
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) { count += item.getAmount(); }
        }
        return count;
    }

    private void removeMaterial(Player player, Material material, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() != material) { continue; }
            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);
            remaining -= take;
        }
    }
}
