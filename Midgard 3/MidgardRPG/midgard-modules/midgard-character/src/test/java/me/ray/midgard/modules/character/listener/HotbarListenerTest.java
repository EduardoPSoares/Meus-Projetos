package me.ray.midgard.modules.character.listener;

import me.ray.midgard.modules.character.CharacterModule;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotbarListenerTest {

    @Mock
    private CharacterModule module;

    @Mock
    private Player player;

    @Mock
    private NamespacedKey compassKey;

    private HotbarListener listener;

    @BeforeEach
    void setUp() {
        lenient().when(module.getCompassKey()).thenReturn(compassKey);
        lenient().when(module.getMessage(anyString())).thenReturn("test message");
        lenient().when(module.getMessageList(anyString())).thenReturn(Collections.emptyList());
        listener = new HotbarListener(module);
    }

    // ============================================
    // isCompass — testes internos via comportamento de eventos
    // ============================================

    private ItemStack createMockCompass(boolean isCompass) {
        ItemStack item = mock(ItemStack.class);
        lenient().when(item.getType()).thenReturn(Material.COMPASS);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        lenient().when(meta.getPersistentDataContainer()).thenReturn(pdc);
        lenient().when(pdc.has(eq(compassKey), eq(PersistentDataType.BYTE))).thenReturn(isCompass);
        lenient().when(item.getItemMeta()).thenReturn(meta);
        return item;
    }

    private ItemStack createNullMetaItem() {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.STONE);
        when(item.getItemMeta()).thenReturn(null);
        return item;
    }

    // ============================================
    // onDrop
    // ============================================

    @Test
    void onDropShouldCancelWhenDroppingCompass() {
        PlayerDropItemEvent event = mock(PlayerDropItemEvent.class);
        Item droppedItem = mock(Item.class);
        ItemStack compassItem = createMockCompass(true);

        when(event.getItemDrop()).thenReturn(droppedItem);
        when(droppedItem.getItemStack()).thenReturn(compassItem);

        listener.onDrop(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onDropShouldNotCancelWhenDroppingNormalItem() {
        PlayerDropItemEvent event = mock(PlayerDropItemEvent.class);
        Item droppedItem = mock(Item.class);
        ItemStack normalItem = createMockCompass(false);

        when(event.getItemDrop()).thenReturn(droppedItem);
        when(droppedItem.getItemStack()).thenReturn(normalItem);

        listener.onDrop(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void onDropShouldNotCancelWhenItemHasNullMeta() {
        PlayerDropItemEvent event = mock(PlayerDropItemEvent.class);
        Item droppedItem = mock(Item.class);
        ItemStack nullMetaItem = createNullMetaItem();

        when(event.getItemDrop()).thenReturn(droppedItem);
        when(droppedItem.getItemStack()).thenReturn(nullMetaItem);

        listener.onDrop(event);

        verify(event, never()).setCancelled(true);
    }

    // ============================================
    // onSwap
    // ============================================

    @Test
    void onSwapShouldCancelWhenHeldSlotIsCompassSlot() {
        PlayerSwapHandItemsEvent event = mock(PlayerSwapHandItemsEvent.class);
        PlayerInventory inv = mock(PlayerInventory.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getHeldItemSlot()).thenReturn(8); // SLOT_INDEX

        listener.onSwap(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onSwapShouldCancelWhenMainHandIsCompass() {
        PlayerSwapHandItemsEvent event = mock(PlayerSwapHandItemsEvent.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack compass = createMockCompass(true);

        when(event.getPlayer()).thenReturn(player);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getHeldItemSlot()).thenReturn(0); // Not compass slot
        when(event.getMainHandItem()).thenReturn(compass);

        listener.onSwap(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onSwapShouldCancelWhenOffHandIsCompass() {
        PlayerSwapHandItemsEvent event = mock(PlayerSwapHandItemsEvent.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack normalItem = createMockCompass(false);
        ItemStack compass = createMockCompass(true);

        when(event.getPlayer()).thenReturn(player);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getHeldItemSlot()).thenReturn(0);
        when(event.getMainHandItem()).thenReturn(normalItem);
        when(event.getOffHandItem()).thenReturn(compass);

        listener.onSwap(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onSwapShouldNotCancelWhenNormalSwap() {
        PlayerSwapHandItemsEvent event = mock(PlayerSwapHandItemsEvent.class);
        PlayerInventory inv = mock(PlayerInventory.class);
        ItemStack normalItem1 = createMockCompass(false);
        ItemStack normalItem2 = createMockCompass(false);

        when(event.getPlayer()).thenReturn(player);
        when(player.getInventory()).thenReturn(inv);
        when(inv.getHeldItemSlot()).thenReturn(0);
        when(event.getMainHandItem()).thenReturn(normalItem1);
        when(event.getOffHandItem()).thenReturn(normalItem2);

        listener.onSwap(event);

        verify(event, never()).setCancelled(true);
    }

    // ============================================
    // onDeath
    // ============================================

    @Test
    void onDeathShouldRemoveCompassFromDrops() {
        PlayerDeathEvent event = mock(PlayerDeathEvent.class);
        ItemStack compass = createMockCompass(true);
        ItemStack normalItem = createMockCompass(false);
        List<ItemStack> drops = new ArrayList<>();
        drops.add(compass);
        drops.add(normalItem);

        when(event.getDrops()).thenReturn(drops);

        listener.onDeath(event);

        assertEquals(1, drops.size());
        assertSame(normalItem, drops.get(0));
    }

    @Test
    void onDeathShouldNotRemoveNormalItemsFromDrops() {
        PlayerDeathEvent event = mock(PlayerDeathEvent.class);
        ItemStack item1 = createMockCompass(false);
        ItemStack item2 = createMockCompass(false);
        List<ItemStack> drops = new ArrayList<>();
        drops.add(item1);
        drops.add(item2);

        when(event.getDrops()).thenReturn(drops);

        listener.onDeath(event);

        assertEquals(2, drops.size());
    }

    @Test
    void onDeathShouldHandleEmptyDrops() {
        PlayerDeathEvent event = mock(PlayerDeathEvent.class);
        List<ItemStack> drops = new ArrayList<>();

        when(event.getDrops()).thenReturn(drops);

        listener.onDeath(event);

        assertTrue(drops.isEmpty());
    }

    // ============================================
    // onInventoryClick
    // ============================================

    @Test
    void onClickShouldDoNothingWhenClickedInventoryIsNull() {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getClickedInventory()).thenReturn(null);

        listener.onInventoryClick(event);

        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    void onClickShouldDoNothingWhenNotPlayer() {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        Inventory clickedInv = mock(Inventory.class);
        org.bukkit.entity.HumanEntity nonPlayer = mock(org.bukkit.entity.HumanEntity.class);

        when(event.getClickedInventory()).thenReturn(clickedInv);
        when(event.getWhoClicked()).thenReturn(nonPlayer);

        listener.onInventoryClick(event);

        verify(event, never()).setCancelled(anyBoolean());
    }

    // ============================================
    // onInteract
    // ============================================

    @Test
    void onInteractShouldNotCancelWhenItemIsNull() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getItem()).thenReturn(null);

        listener.onInteract(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void onInteractShouldNotCancelWhenItemIsNotCompass() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        ItemStack normalItem = createMockCompass(false);
        when(event.getItem()).thenReturn(normalItem);

        listener.onInteract(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void onInteractShouldNotCancelOnLeftClick() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        ItemStack compass = createMockCompass(true);
        when(event.getItem()).thenReturn(compass);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_AIR);

        listener.onInteract(event);

        verify(event, never()).setCancelled(true);
    }

    // ============================================
    // giveCompass
    // ============================================

    @Test
    void giveCompassShouldHandleNullPlayer() {
        // Não deve lançar exceção
        assertDoesNotThrow(() -> listener.giveCompass(null));
    }

    // ============================================
    // onInventoryDrag
    // ============================================

    @Test
    void onDragShouldCancelWhenOldCursorIsCompass() {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        ItemStack compass = createMockCompass(true);

        when(event.getOldCursor()).thenReturn(compass);

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onDragShouldCancelWhenCursorIsCompass() {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        ItemStack normalItem = createMockCompass(false);
        ItemStack compass = createMockCompass(true);

        when(event.getOldCursor()).thenReturn(normalItem);
        when(event.getCursor()).thenReturn(compass);

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onDragShouldCheckRawSlotsForCompass() {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        ItemStack normalItem = createMockCompass(false);
        ItemStack compass = createMockCompass(true);
        InventoryView view = mock(InventoryView.class);
        Inventory topInv = mock(Inventory.class);

        when(event.getOldCursor()).thenReturn(normalItem);
        when(event.getCursor()).thenReturn(normalItem);
        when(event.getRawSlots()).thenReturn(Set.of(5));
        when(event.getView()).thenReturn(view);
        when(view.getItem(5)).thenReturn(compass);

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onDragShouldCancelWhenDraggingToCompassSlot() {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        ItemStack normalItem = createMockCompass(false);
        InventoryView view = mock(InventoryView.class);
        Inventory topInv = mock(Inventory.class);

        when(event.getOldCursor()).thenReturn(normalItem);
        when(event.getCursor()).thenReturn(normalItem);
        when(event.getRawSlots()).thenReturn(Set.of(35)); // raw slot that maps to player slot 8
        when(event.getView()).thenReturn(view);
        when(view.getItem(35)).thenReturn(null); // No item there
        when(view.getTopInventory()).thenReturn(topInv);
        when(topInv.getSize()).thenReturn(27); // top inventory size
        when(view.convertSlot(35)).thenReturn(8); // converts to SLOT_INDEX

        listener.onInventoryDrag(event);

        verify(event).setCancelled(true);
    }

    @Test
    void onDragShouldNotCancelForNormalDrag() {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        ItemStack normalItem = createMockCompass(false);
        InventoryView view = mock(InventoryView.class);
        Inventory topInv = mock(Inventory.class);

        when(event.getOldCursor()).thenReturn(normalItem);
        when(event.getCursor()).thenReturn(normalItem);
        when(event.getRawSlots()).thenReturn(Set.of(5));
        when(event.getView()).thenReturn(view);
        when(view.getItem(5)).thenReturn(normalItem); // Normal item in slot
        when(view.getTopInventory()).thenReturn(topInv);
        when(topInv.getSize()).thenReturn(27);
        // raw slot 5 < topSize 27, so it's in top inventory — no bottom inventory check
        // This means no cancel for top inventory slot

        listener.onInventoryDrag(event);

        verify(event, never()).setCancelled(true);
    }
}
