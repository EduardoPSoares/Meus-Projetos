package me.ray.midgard.modules.professions.blacksmith.forge.event;

import me.ray.midgard.modules.professions.blacksmith.forge.ForgeRotation;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeTier;
import me.ray.midgard.modules.professions.blacksmith.forge.quality.QualityTier;
import me.ray.midgard.modules.professions.blacksmith.forge.recipe.ForgeRecipe;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.MoltenMetal;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryStructure;
import me.ray.midgard.modules.professions.blacksmith.forge.smeltery.SmelteryTier;
import me.ray.midgard.modules.professions.blacksmith.forge.structure.ForgeStructure;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForgeEventTest {

    private final Player player = mock(Player.class);

    private ForgeStructure createForge() {
        return new ForgeStructure(UUID.randomUUID(), UUID.randomUUID(), "world",
                100, 64, 200, ForgeTier.BASIC, ForgeRotation.NORTH);
    }

    private SmelteryStructure createSmeltery() {
        return new SmelteryStructure(UUID.randomUUID(), UUID.randomUUID(),
                "world", 50, 64, 50, SmelteryTier.SMALL);
    }

    private ForgeRecipe createRecipe() {
        return new ForgeRecipe("test_sword");
    }

    // ═══ ForgeStartEvent ═══

    @Test
    @DisplayName("ForgeStartEvent: deve armazenar player, forge e recipe")
    void forgeStartEvent_shouldStoreData() {
        ForgeStructure forge = createForge();
        ForgeRecipe recipe = createRecipe();
        ForgeStartEvent event = new ForgeStartEvent(player, forge, recipe);

        assertSame(player, event.getPlayer());
        assertSame(forge, event.getForge());
        assertSame(recipe, event.getRecipe());
    }

    @Test
    @DisplayName("ForgeStartEvent: deve ser cancelável")
    void forgeStartEvent_shouldBeCancellable() {
        ForgeStartEvent event = new ForgeStartEvent(player, createForge(), createRecipe());
        assertFalse(event.isCancelled());
        event.setCancelled(true);
        assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("ForgeStartEvent: HandlerList não deve ser null")
    void forgeStartEvent_shouldHaveHandlerList() {
        ForgeStartEvent event = new ForgeStartEvent(player, createForge(), createRecipe());
        assertNotNull(event.getHandlers());
        assertNotNull(ForgeStartEvent.getHandlerList());
        assertSame(ForgeStartEvent.getHandlerList(), event.getHandlers());
    }

    // ═══ ForgeCompleteEvent ═══

    @Test
    @DisplayName("ForgeCompleteEvent: deve armazenar todos os dados")
    void forgeCompleteEvent_shouldStoreData() {
        ForgeStructure forge = createForge();
        ForgeRecipe recipe = createRecipe();
        ItemStack result = mock(ItemStack.class);

        ForgeCompleteEvent event = new ForgeCompleteEvent(
                player, forge, recipe, result,
                QualityTier.MASTERPIECE, 0.95, 150.0);

        assertSame(player, event.getPlayer());
        assertSame(forge, event.getForge());
        assertSame(recipe, event.getRecipe());
        assertSame(result, event.getResult());
        assertEquals(QualityTier.MASTERPIECE, event.getQualityTier());
        assertEquals(0.95, event.getQualityScore(), 0.001);
        assertEquals(150.0, event.getXpGained(), 0.001);
    }

    @Test
    @DisplayName("ForgeCompleteEvent: HandlerList não deve ser null")
    void forgeCompleteEvent_shouldHaveHandlerList() {
        ForgeCompleteEvent event = new ForgeCompleteEvent(
                player, createForge(), createRecipe(), mock(ItemStack.class),
                QualityTier.COMMON, 0.5, 50.0);
        assertNotNull(event.getHandlers());
        assertNotNull(ForgeCompleteEvent.getHandlerList());
        assertSame(ForgeCompleteEvent.getHandlerList(), event.getHandlers());
    }

    // ═══ AlloyFormedEvent ═══

    @Test
    @DisplayName("AlloyFormedEvent: deve armazenar dados da liga")
    void alloyFormedEvent_shouldStoreData() {
        SmelteryStructure smeltery = createSmeltery();
        AlloyFormedEvent event = new AlloyFormedEvent(smeltery, MoltenMetal.BRONZE, 576);

        assertSame(smeltery, event.getSmeltery());
        assertEquals(MoltenMetal.BRONZE, event.getAlloy());
        assertEquals(576, event.getAmountProduced());
        assertEquals(smeltery.getOwnerUuid(), event.getOwnerUuid());
    }

    @Test
    @DisplayName("AlloyFormedEvent: HandlerList não deve ser null")
    void alloyFormedEvent_shouldHaveHandlerList() {
        AlloyFormedEvent event = new AlloyFormedEvent(createSmeltery(), MoltenMetal.STEEL, 288);
        assertNotNull(event.getHandlers());
        assertNotNull(AlloyFormedEvent.getHandlerList());
        assertSame(AlloyFormedEvent.getHandlerList(), event.getHandlers());
    }

    // ═══ SmelteryActivateEvent ═══

    @Test
    @DisplayName("SmelteryActivateEvent: deve armazenar player e smeltery")
    void smelteryActivateEvent_shouldStoreData() {
        SmelteryStructure smeltery = createSmeltery();
        SmelteryActivateEvent event = new SmelteryActivateEvent(player, smeltery);

        assertSame(player, event.getPlayer());
        assertSame(smeltery, event.getSmeltery());
    }

    @Test
    @DisplayName("SmelteryActivateEvent: HandlerList não deve ser null")
    void smelteryActivateEvent_shouldHaveHandlerList() {
        SmelteryActivateEvent event = new SmelteryActivateEvent(player, createSmeltery());
        assertNotNull(event.getHandlers());
        assertNotNull(SmelteryActivateEvent.getHandlerList());
        assertSame(SmelteryActivateEvent.getHandlerList(), event.getHandlers());
    }

    // ═══ SmeltingCompleteEvent ═══

    @Test
    @DisplayName("SmeltingCompleteEvent: deve armazenar dados do smelting")
    void smeltingCompleteEvent_shouldStoreData() {
        SmelteryStructure smeltery = createSmeltery();
        SmeltingCompleteEvent event = new SmeltingCompleteEvent(smeltery, MoltenMetal.IRON, 144);

        assertSame(smeltery, event.getSmeltery());
        assertEquals(MoltenMetal.IRON, event.getMetal());
        assertEquals(144, event.getAmountProduced());
        assertEquals(smeltery.getOwnerUuid(), event.getOwnerUuid());
    }

    @Test
    @DisplayName("SmeltingCompleteEvent: HandlerList não deve ser null")
    void smeltingCompleteEvent_shouldHaveHandlerList() {
        SmeltingCompleteEvent event = new SmeltingCompleteEvent(createSmeltery(), MoltenMetal.GOLD, 288);
        assertNotNull(event.getHandlers());
        assertNotNull(SmeltingCompleteEvent.getHandlerList());
        assertSame(SmeltingCompleteEvent.getHandlerList(), event.getHandlers());
    }
}
