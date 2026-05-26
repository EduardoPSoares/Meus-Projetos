package com.midgard.fooddecay.gui;

import com.midgard.core.gui.PaginatedMenu;
import com.midgard.core.item.ItemBuilder;
import com.midgard.fooddecay.FoodDecayModule;
import com.midgard.fooddecay.multiblock.ItemsAdderHook;
import com.midgard.fooddecay.multiblock.MMOItemsHook;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

import static com.midgard.core.utils.MessageUtils.sc;

/**
 * GUI for selecting items in the recipe editor.
 * Supports vanilla materials, MMOItems and ItemsAdder.
 */
public class ItemSelectorGui extends PaginatedMenu {

    private final FoodDecayModule module;
    private final String label;
    private final ItemSelectedCallback callback;
    private final Runnable backAction;

    @FunctionalInterface
    public interface ItemSelectedCallback {
        void onSelect(Material material, String mmoType, String mmoId, String itemsAdderId);
    }

    public ItemSelectorGui(FoodDecayModule module, String label, ItemSelectedCallback callback) {
        this(module, label, callback, null);
    }

    public ItemSelectorGui(FoodDecayModule module, String label, ItemSelectedCallback callback, Runnable backAction) {
        super(sc("&8Selecionar Item: " + label), 6);
        this.module = module;
        this.label = label;
        this.callback = callback;
        this.backAction = backAction;
    }

    @Override
    public ItemStack getPreviousPageItem() {
        return new ItemBuilder(Material.ARROW).name(sc("&ePagina anterior")).build();
    }

    @Override
    public ItemStack getNextPageItem() {
        return new ItemBuilder(Material.ARROW).name(sc("&eProxima pagina")).build();
    }

    @Override
    public void setupDecoration(Player player) {
        fillBorder(ItemBuilder.placeholder());
        clearPageItems();

        setupMainPage(player);

        setItem(4, new ItemBuilder(Material.DARK_OAK_DOOR)
                .name(sc("&cVoltar"))
                .build(), e -> {
            if (backAction != null) {
                backAction.run();
            } else {
                player.closeInventory();
            }
        });
    }

    private void setupMainPage(Player player) {
        if (MMOItemsHook.isAvailable()) {
            addPageItem(new ItemBuilder(Material.ENDER_CHEST)
                    .name(sc("&5&lMMOItems"))
                    .lore(sc("&7Clique para navegar"),
                            sc("&7pelos tipos de MMOItems"))
                    .glow()
                    .build(), e -> new MMOTypeSelectorGui(module, label, callback, backAction).open(player));
        }

        if (ItemsAdderHook.isAvailable()) {
            addPageItem(new ItemBuilder(Material.NOTE_BLOCK)
                    .name(sc("&b&lItemsAdder"))
                    .lore(sc("&7Clique para navegar"),
                            sc("&7pelos namespaces do ItemsAdder"))
                    .glow()
                    .build(), e -> new ItemsAdderNamespaceSelectorGui(module, label, callback, backAction).open(player));
        }

        for (Material material : Material.values()) {
            if (material.isEdible() && material.isItem() && !material.isAir()) {
                addPageItem(new ItemBuilder(material)
                        .name(sc("&f" + formatMaterial(material.name())))
                        .lore(sc("&7" + material.name()),
                                "",
                                sc("&aClique para selecionar"))
                        .build(), e -> callback.onSelect(material, null, null, null));
            }
        }
    }

    private static String formatMaterial(String name) {
        String[] parts = name.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0)))
                    .append(lower.substring(1));
        }
        return builder.toString();
    }

    static class MMOTypeSelectorGui extends PaginatedMenu {

        private final FoodDecayModule module;
        private final String label;
        private final ItemSelectedCallback callback;
        private final Runnable backAction;

        MMOTypeSelectorGui(FoodDecayModule module, String label, ItemSelectedCallback callback, Runnable backAction) {
            super(sc("&8MMOItems: Selecionar Tipo"), 6);
            this.module = module;
            this.label = label;
            this.callback = callback;
            this.backAction = backAction;
        }

        @Override
        public ItemStack getPreviousPageItem() {
            return new ItemBuilder(Material.ARROW).name(sc("&ePagina anterior")).build();
        }

        @Override
        public ItemStack getNextPageItem() {
            return new ItemBuilder(Material.ARROW).name(sc("&eProxima pagina")).build();
        }

        @Override
        public void setupDecoration(Player player) {
            fillBorder(ItemBuilder.placeholder());
            clearPageItems();

            setItem(4, new ItemBuilder(Material.ENDER_CHEST)
                    .name(sc("&5&lTipos MMOItems"))
                    .lore(sc("&7Selecione um tipo"))
                    .build());

            setItem(getRows() * 9 - 5, new ItemBuilder(Material.DARK_OAK_DOOR)
                    .name(sc("&cVoltar"))
                    .build(), e -> new ItemSelectorGui(module, label, callback, backAction).open(player));

            List<String> types = MMOItemsHook.getTypeNames();
            for (String typeName : types) {
                addPageItem(new ItemBuilder(Material.CHEST)
                        .name(sc("&e" + typeName))
                        .lore(sc("&7Clique para ver os itens"),
                                sc("&7registrados neste tipo"))
                        .build(), e -> new MMOItemListGui(module, label, typeName, callback, backAction).open(player));
            }
        }
    }

    static class MMOItemListGui extends PaginatedMenu {

        private final FoodDecayModule module;
        private final String label;
        private final String mmoType;
        private final ItemSelectedCallback callback;
        private final Runnable backAction;

        MMOItemListGui(FoodDecayModule module, String label, String mmoType, ItemSelectedCallback callback, Runnable backAction) {
            super(sc("&8MMOItems: " + mmoType), 6);
            this.module = module;
            this.label = label;
            this.mmoType = mmoType;
            this.callback = callback;
            this.backAction = backAction;
        }

        @Override
        public ItemStack getPreviousPageItem() {
            return new ItemBuilder(Material.ARROW).name(sc("&ePagina anterior")).build();
        }

        @Override
        public ItemStack getNextPageItem() {
            return new ItemBuilder(Material.ARROW).name(sc("&eProxima pagina")).build();
        }

        @Override
        public void setupDecoration(Player player) {
            fillBorder(ItemBuilder.placeholder());
            clearPageItems();

            setItem(4, new ItemBuilder(Material.CHEST)
                    .name(sc("&e&l" + mmoType))
                    .lore(sc("&7Selecione um item"))
                    .build());

            setItem(getRows() * 9 - 5, new ItemBuilder(Material.DARK_OAK_DOOR)
                    .name(sc("&cVoltar"))
                    .build(), e -> new MMOTypeSelectorGui(module, label, callback, backAction).open(player));

            List<String> itemIds = MMOItemsHook.getItemIds(mmoType);
            for (String itemId : itemIds) {
                ItemStack preview = MMOItemsHook.createItem(mmoType, itemId);
                Material icon = preview != null ? preview.getType() : Material.PAPER;
                String displayName = MMOItemsHook.getItemDisplayName(mmoType, itemId);

                ItemBuilder itemBuilder = new ItemBuilder(preview != null ? preview : new ItemStack(icon))
                        .name(sc("&a" + displayName))
                        .lore(sc("&7Tipo: &f" + mmoType),
                                sc("&7ID: &f" + itemId),
                                "",
                                sc("&aClique para selecionar"));
                if (preview != null && preview.hasItemMeta() && preview.getItemMeta().hasDisplayName()) {
                    itemBuilder.glow();
                }

                addPageItem(itemBuilder.build(), e -> callback.onSelect(icon, mmoType, itemId, null));
            }
        }
    }

    static class ItemsAdderNamespaceSelectorGui extends PaginatedMenu {

        private final FoodDecayModule module;
        private final String label;
        private final ItemSelectedCallback callback;
        private final Runnable backAction;

        ItemsAdderNamespaceSelectorGui(FoodDecayModule module, String label, ItemSelectedCallback callback, Runnable backAction) {
            super(sc("&8ItemsAdder: Namespace"), 6);
            this.module = module;
            this.label = label;
            this.callback = callback;
            this.backAction = backAction;
        }

        @Override
        public ItemStack getPreviousPageItem() {
            return new ItemBuilder(Material.ARROW).name(sc("&ePagina anterior")).build();
        }

        @Override
        public ItemStack getNextPageItem() {
            return new ItemBuilder(Material.ARROW).name(sc("&eProxima pagina")).build();
        }

        @Override
        public void setupDecoration(Player player) {
            fillBorder(ItemBuilder.placeholder());
            clearPageItems();

            setItem(4, new ItemBuilder(Material.NOTE_BLOCK)
                    .name(sc("&b&lNamespaces ItemsAdder"))
                    .lore(sc("&7Selecione um namespace"))
                    .build());

            setItem(getRows() * 9 - 5, new ItemBuilder(Material.DARK_OAK_DOOR)
                    .name(sc("&cVoltar"))
                    .build(), e -> new ItemSelectorGui(module, label, callback, backAction).open(player));

            for (String namespace : ItemsAdderHook.getNamespaces()) {
                addPageItem(new ItemBuilder(Material.BOOKSHELF)
                        .name(sc("&b" + namespace))
                        .lore(sc("&7Clique para ver os itens"),
                                sc("&7deste namespace"))
                        .build(), e -> new ItemsAdderItemListGui(module, label, namespace, callback, backAction).open(player));
            }
        }
    }

    static class ItemsAdderItemListGui extends PaginatedMenu {

        private final FoodDecayModule module;
        private final String label;
        private final String namespace;
        private final ItemSelectedCallback callback;
        private final Runnable backAction;

        ItemsAdderItemListGui(FoodDecayModule module, String label, String namespace,
                              ItemSelectedCallback callback, Runnable backAction) {
            super(sc("&8ItemsAdder: " + namespace), 6);
            this.module = module;
            this.label = label;
            this.namespace = namespace;
            this.callback = callback;
            this.backAction = backAction;
        }

        @Override
        public ItemStack getPreviousPageItem() {
            return new ItemBuilder(Material.ARROW).name(sc("&ePagina anterior")).build();
        }

        @Override
        public ItemStack getNextPageItem() {
            return new ItemBuilder(Material.ARROW).name(sc("&eProxima pagina")).build();
        }

        @Override
        public void setupDecoration(Player player) {
            fillBorder(ItemBuilder.placeholder());
            clearPageItems();

            setItem(4, new ItemBuilder(Material.NOTE_BLOCK)
                    .name(sc("&b&l" + namespace))
                    .lore(sc("&7Selecione um item"))
                    .build());

            setItem(getRows() * 9 - 5, new ItemBuilder(Material.DARK_OAK_DOOR)
                    .name(sc("&cVoltar"))
                    .build(), e -> new ItemsAdderNamespaceSelectorGui(module, label, callback, backAction).open(player));

            for (String itemId : ItemsAdderHook.getItemIds(namespace)) {
                String namespacedId = namespace + ":" + itemId;
                ItemStack preview = ItemsAdderHook.createItem(namespacedId);
                Material icon = preview != null ? preview.getType() : Material.PAPER;
                String displayName = ItemsAdderHook.getItemDisplayName(namespacedId);

                ItemBuilder itemBuilder = new ItemBuilder(preview != null ? preview : new ItemStack(icon))
                        .name(sc("&a" + displayName))
                        .lore(sc("&7Namespace: &f" + namespace),
                                sc("&7ID: &f" + itemId),
                                "",
                                sc("&aClique para selecionar"));
                if (preview != null && preview.hasItemMeta() && preview.getItemMeta().hasDisplayName()) {
                    itemBuilder.glow();
                }

                addPageItem(itemBuilder.build(), e -> callback.onSelect(icon, null, null, namespacedId));
            }
        }
    }
}
