package com.midgard.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Paginated GUI menu with automatic page navigation.
 */
public abstract class PaginatedMenu extends GuiMenu {

    private final List<ItemStack> items = new ArrayList<>();
    private final List<Consumer<InventoryClickEvent>> itemActions = new ArrayList<>();
    private int currentPage = 0;

    protected PaginatedMenu(String title, int rows) {
        super(title, rows);
    }

    public abstract ItemStack getPreviousPageItem();
    public abstract ItemStack getNextPageItem();

    public void addPageItem(ItemStack item, Consumer<InventoryClickEvent> action) {
        items.add(item);
        itemActions.add(action);
    }

    public void addPageItem(ItemStack item) {
        addPageItem(item, null);
    }

    public void clearPageItems() {
        items.clear();
        itemActions.clear();
    }

    @Override
    public void setup(Player player) {
        setupDecoration(player);
        populatePage(player);
    }

    public abstract void setupDecoration(Player player);

    protected void populatePage(Player player) {
        int itemsPerPage = getItemsPerPage();
        int startIndex = currentPage * itemsPerPage;

        List<Integer> contentSlots = getContentSlots();

        for (int i = 0; i < contentSlots.size(); i++) {
            int dataIndex = startIndex + i;
            int slot = contentSlots.get(i);

            if (dataIndex < items.size()) {
                Consumer<InventoryClickEvent> action = itemActions.get(dataIndex);
                setItem(slot, items.get(dataIndex), action != null ? action : e -> {});
            } else {
                setItem(slot, null);
            }
        }

        // Navigation buttons
        if (currentPage > 0) {
            setItem(getRows() * 9 - 9, getPreviousPageItem(), e -> {
                currentPage--;
                open(player);
            });
        }

        if (startIndex + itemsPerPage < items.size()) {
            setItem(getRows() * 9 - 1, getNextPageItem(), e -> {
                currentPage++;
                open(player);
            });
        }
    }

    protected List<Integer> getContentSlots() {
        List<Integer> slots = new ArrayList<>();
        int size = getRows() * 9;
        for (int i = 10; i < size - 10; i++) {
            if (i % 9 != 0 && i % 9 != 8) {
                slots.add(i);
            }
        }
        return slots;
    }

    protected int getItemsPerPage() {
        return getContentSlots().size();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        int perPage = getItemsPerPage();
        return perPage == 0 ? 1 : (int) Math.ceil((double) items.size() / perPage);
    }

    public void setPage(int page) {
        this.currentPage = Math.max(0, Math.min(page, getTotalPages() - 1));
    }
}
