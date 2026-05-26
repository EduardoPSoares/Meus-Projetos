package me.ray.midgard.modules.item.gui;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.gui.PaginatedGui;
import me.ray.midgard.core.utils.ItemBuilder;
import me.ray.midgard.core.text.MessageUtils;
import me.ray.midgard.modules.item.ItemModule;
import me.ray.midgard.modules.item.model.MidgardItem;
import me.ray.midgard.core.gui.BaseGui;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MaterialSelectionGui extends PaginatedGui<Material> {

    private final Consumer<Material> onSelect;
    private final BaseGui parentGui;
    
    private final Map<String, List<Material>> categories = new LinkedHashMap<>();
    private String currentCategory = null;

    public MaterialSelectionGui(Player player, ItemModule module, MidgardItem item, ItemEditionGui parent) {
        this(player, module, (mat) -> {
            item.setMaterial(mat);
            player.sendMessage(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.gui.material_selection.updated").replace("%s", mat.name())));
            parent.open();
        }, parent);
    }

    public MaterialSelectionGui(Player player, ItemModule module, Consumer<Material> onSelect, BaseGui parent) {
        super(player, MidgardCore.getLanguageManager().getRawMessage("item.gui.material_selection.title"), new ArrayList<>());
        this.onSelect = onSelect;
        this.parentGui = parent;
        
        loadCategories();
    }

    private void loadCategories() {
        categories.clear();
        categories.put("Weapons", new ArrayList<>());
        categories.put("Armor", new ArrayList<>());
        categories.put("Tools", new ArrayList<>());
        categories.put("Consumables", new ArrayList<>());
        categories.put("Blocks", new ArrayList<>());
        categories.put("Resources", new ArrayList<>());
        categories.put("Miscellaneous", new ArrayList<>());

        for (Material mat : Material.values()) {
            if (!mat.isItem() || mat.isAir()) {
                continue;
            }
            
            String name = mat.name();
            if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_BOW") || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("TRIDENT") || name.equals("SHIELD") || name.equals("MACE") || name.equals("WIND_CHARGE")) {
                categories.get("Weapons").add(mat);
            } else if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.equals("ELYTRA") || name.equals("WOLF_ARMOR")) {
                categories.get("Armor").add(mat);
            } else if (name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_ROD") || name.equals("FLINT_AND_STEEL") || name.equals("SHEARS") || name.equals("BRUSH") || name.equals("SPYGLASS")) {
                categories.get("Tools").add(mat);
            } else if (mat.isEdible() || name.contains("POTION") || name.equals("MILK_BUCKET") || name.equals("OMINOUS_BOTTLE")) {
                categories.get("Consumables").add(mat);
            } else if (mat.isBlock()) {
                categories.get("Blocks").add(mat);
            } else if (name.endsWith("_INGOT") || name.endsWith("_DIAMOND") || name.endsWith("_EMERALD") || name.endsWith("_COAL") || name.endsWith("_DUST") || name.endsWith("_BALL") || name.endsWith("_NUGGET") || name.endsWith("_SHARD") || name.endsWith("_SCRAP") || name.equals("NETHER_STAR") || name.equals("TRIAL_KEY") || name.equals("OMINOUS_TRIAL_KEY") || name.equals("BREEZE_ROD")) {
                categories.get("Resources").add(mat);
            } else {
                categories.get("Miscellaneous").add(mat);
            }
        }
        
        // Sort each list
        categories.values().forEach(list -> list.sort(Comparator.comparing(Material::name)));
    }

    private String getPrettyName(String name) {
        if (name == null) {
            return "";
        }
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public ItemStack createItem(Material material) {
        return new ItemBuilder(material)
                .name(MessageUtils.parse("<yellow>" + getPrettyName(material.name())))
                .lore(MidgardCore.getLanguageManager().getStringList("item.gui.material_selection.item_lore").stream().map(MessageUtils::parse).collect(Collectors.toList()))
                .build();
    }

    @Override
    public void initializeItems() {
        inventory.clear();
        
        if (currentCategory == null) {
            initializeCategories();
        } else {
            initializeMaterialList();
        }
        
        addMenuBorder();
    }

    private void initializeCategories() {
        // Display categories
        int[] slots = {10, 12, 14, 16, 28, 30, 32};
        String[] categoryIds = {"Weapons", "Armor", "Tools", "Consumables", "Blocks", "Resources", "Miscellaneous"};
        Material[] icons = {Material.DIAMOND_SWORD, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_PICKAXE, Material.COOKED_BEEF, Material.GRASS_BLOCK, Material.DIAMOND, Material.LAVA_BUCKET};

        for (int i = 0; i < categoryIds.length; i++) {
            if (i >= slots.length) {
                break;
            }
            
            String catId = categoryIds[i];
            int count = categories.get(catId).size();
            
            String langKey = "item.gui.material_selection.categories." + catId.toLowerCase();
            String displayName = MidgardCore.getLanguageManager().getRawMessage(langKey);
            if (displayName == null) {
                displayName = catId;
            }
            
            String loreFormat = MidgardCore.getLanguageManager().getRawMessage("item.gui.material_selection.category_lore");
            if (loreFormat == null) {
                loreFormat = "<gray>Click to view <white>%count% <gray>items.";
            }
            String lore = loreFormat.replace("%count%", String.valueOf(count));

            inventory.setItem(slots[i], new ItemBuilder(icons[i])
                    .name(MessageUtils.parse("<gold><bold>" + displayName))
                    .lore(MessageUtils.parse(lore))
                    .build());
        }
    }

    private void initializeMaterialList() {
        this.items = categories.get(currentCategory);
        
        // Use 45 slots for items (rows 1-5)
        int maxItemsPerPage = 45;
        
        // Calculate start index
        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            // Map list index to inventory slot (0-44)
            int slot = i - startIndex;
            inventory.setItem(slot, createItem(items.get(i)));
        }
    }

    @Override
    public void addMenuBorder() {
        // Fill bottom row with glass panes for decoration
        ItemStack glass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(MessageUtils.parse(" ")).build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        // Navigation Bar (Row 6, slots 45-53)
        
        if (currentCategory != null) {
            // Slot 45: Previous Page
            if (page > 0) {
                inventory.setItem(45, new ItemBuilder(Material.ARROW)
                        .name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.common.previous_page")))
                        .build());
            }

            // Slot 53: Next Page
            int maxItemsPerPage = 45;
            if ((page + 1) * maxItemsPerPage < items.size()) {
                inventory.setItem(53, new ItemBuilder(Material.ARROW)
                        .name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.common.next_page")))
                        .build());
            }
        }

        // Slot 49: Back
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .name(MessageUtils.parse(MidgardCore.getLanguageManager().getRawMessage("item.common.back")))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        // Back Button Logic (Slot 49)
        if (slot == 49) {
            if (currentCategory != null) {
                // Go back to categories
                currentCategory = null;
                page = 0;
                initializeItems();
            } else {
                // Go back to parent GUI
                if (parentGui != null) {
                    parentGui.open();
                } else {
                    player.closeInventory();
                }
            }
            return;
        }

        if (currentCategory == null) {
            // Category Selection Logic
            int[] slots = {10, 12, 14, 16, 28, 30, 32};
            String[] categoryNames = {"Weapons", "Armor", "Tools", "Consumables", "Blocks", "Resources", "Miscellaneous"};
            
            for (int i = 0; i < slots.length; i++) {
                if (slot == slots[i]) {
                    currentCategory = categoryNames[i];
                    page = 0;
                    initializeItems();
                    return;
                }
            }
        } else {
            // Material Selection Logic
            
            // Navigation
            if (slot == 45 && page > 0) {
                page--;
                initializeItems();
                return;
            } else if (slot == 53) {
                int maxItemsPerPage = 45;
                if ((page + 1) * maxItemsPerPage < items.size()) {
                    page++;
                    initializeItems();
                }
                return;
            }

            // Item Selection (Slots 0-44)
            if (slot >= 0 && slot < 45) {
                int maxItemsPerPage = 45;
                int index = slot + (page * maxItemsPerPage);
                
                if (index >= 0 && index < items.size()) {
                    Material selected = items.get(index);
                    if (onSelect != null) {
                        onSelect.accept(selected);
                    }
                }
            }
        }
    }
}
