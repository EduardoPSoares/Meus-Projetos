package me.ray.midgard.modules.combat;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RPGDamageContextTest {

    // --- Physical Damage Causes ---

    @Test
    void shouldCategorizeEntityAttackAsPhysical() {
        EntityDamageEvent event = mockEvent(DamageCause.ENTITY_ATTACK);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
    }

    @Test
    void shouldCategorizeSweepAttackAsPhysical() {
        EntityDamageEvent event = mockEvent(DamageCause.ENTITY_SWEEP_ATTACK);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
    }

    @Test
    void shouldCategorizeProjectileCauseAsPhysicalAndProjectile() {
        EntityDamageEvent event = mockEvent(DamageCause.PROJECTILE);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.PROJECTILE));
    }

    @Test
    void shouldCategorizeFallAsPhysicalAndEnvironmental() {
        EntityDamageEvent event = mockEvent(DamageCause.FALL);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.ENVIRONMENTAL));
    }

    @Test
    void shouldCategorizeFlyIntoWallAsPhysicalAndEnvironmental() {
        EntityDamageEvent event = mockEvent(DamageCause.FLY_INTO_WALL);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.ENVIRONMENTAL));
    }

    @Test
    void shouldCategorizeSuffocationAsPhysicalAndEnvironmental() {
        EntityDamageEvent event = mockEvent(DamageCause.SUFFOCATION);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.ENVIRONMENTAL));
    }

    @Test
    void shouldCategorizeContactAsPhysical() {
        EntityDamageEvent event = mockEvent(DamageCause.CONTACT);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
    }

    // --- Magical Damage Causes ---

    @Test
    void shouldCategorizeMagicAsMagical() {
        EntityDamageEvent event = mockEvent(DamageCause.MAGIC);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
    }

    @Test
    void shouldCategorizePoisonAsMagicalAndDot() {
        EntityDamageEvent event = mockEvent(DamageCause.POISON);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.DOT));
    }

    @Test
    void shouldCategorizeWitherAsMagicalAndDot() {
        EntityDamageEvent event = mockEvent(DamageCause.WITHER);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.DOT));
    }

    @Test
    void shouldCategorizeDragonBreathAsMagical() {
        EntityDamageEvent event = mockEvent(DamageCause.DRAGON_BREATH);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
    }

    @Test
    void shouldCategorizeThornsAsMagical() {
        EntityDamageEvent event = mockEvent(DamageCause.THORNS);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
    }

    @Test
    void shouldCategorizeSonicBoomAsMagical() {
        EntityDamageEvent event = mockEvent(DamageCause.SONIC_BOOM);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
    }

    @Test
    void shouldCategorizeFreezeAsMagical() {
        EntityDamageEvent event = mockEvent(DamageCause.FREEZE);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
    }

    @Test
    void shouldCategorizeLightningAsMagical() {
        EntityDamageEvent event = mockEvent(DamageCause.LIGHTNING);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
    }

    // --- DOT Damage Causes ---

    @Test
    void shouldCategorizeFireTickAsDot() {
        EntityDamageEvent event = mockEvent(DamageCause.FIRE_TICK);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.DOT));
    }

    @Test
    void shouldCategorizeStarvationAsDot() {
        EntityDamageEvent event = mockEvent(DamageCause.STARVATION);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.DOT));
    }

    @Test
    void shouldCategorizeDryoutAsDot() {
        EntityDamageEvent event = mockEvent(DamageCause.DRYOUT);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.DOT));
    }

    // --- Explosion Causes ---

    @Test
    void shouldCategorizeBlockExplosionAsExplosionAndAoe() {
        EntityDamageEvent event = mockEvent(DamageCause.BLOCK_EXPLOSION);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.EXPLOSION));
        assertTrue(ctx.hasCategory(RPGDamageCategory.AOE));
    }

    @Test
    void shouldCategorizeEntityExplosionAsExplosionAndAoe() {
        EntityDamageEvent event = mockEvent(DamageCause.ENTITY_EXPLOSION);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.EXPLOSION));
        assertTrue(ctx.hasCategory(RPGDamageCategory.AOE));
    }

    // --- Environmental Causes ---

    @Test
    void shouldCategorizeVoidAsEnvironmental() {
        EntityDamageEvent event = mockEvent(DamageCause.VOID);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.ENVIRONMENTAL));
    }

    @Test
    void shouldCategorizeDrowningAsEnvironmental() {
        EntityDamageEvent event = mockEvent(DamageCause.DROWNING);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.ENVIRONMENTAL));
    }

    @Test
    void shouldCategorizeFireAsEnvironmental() {
        EntityDamageEvent event = mockEvent(DamageCause.FIRE);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.ENVIRONMENTAL));
    }

    @Test
    void shouldCategorizeLavaAsEnvironmental() {
        EntityDamageEvent event = mockEvent(DamageCause.LAVA);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.ENVIRONMENTAL));
    }

    // --- addCategory ---

    @Test
    void shouldAllowAddingCategory() {
        EntityDamageEvent event = mockEvent(DamageCause.ENTITY_ATTACK);
        RPGDamageContext ctx = new RPGDamageContext(event);

        assertFalse(ctx.hasCategory(RPGDamageCategory.SKILL));
        ctx.addCategory(RPGDamageCategory.SKILL);
        assertTrue(ctx.hasCategory(RPGDamageCategory.SKILL));
    }

    @Test
    void shouldAllowAddingMultipleCategories() {
        EntityDamageEvent event = mockEvent(DamageCause.ENTITY_ATTACK);
        RPGDamageContext ctx = new RPGDamageContext(event);

        ctx.addCategory(RPGDamageCategory.SKILL);
        ctx.addCategory(RPGDamageCategory.MINION);

        assertTrue(ctx.hasCategory(RPGDamageCategory.SKILL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.MINION));
    }

    @Test
    void shouldNotDuplicateExistingCategory() {
        EntityDamageEvent event = mockEvent(DamageCause.ENTITY_ATTACK);
        RPGDamageContext ctx = new RPGDamageContext(event);

        ctx.addCategory(RPGDamageCategory.PHYSICAL);
        // EnumSet ignores duplicates
        Set<RPGDamageCategory> cats = ctx.getCategories();
        long count = cats.stream().filter(c -> c == RPGDamageCategory.PHYSICAL).count();
        assertEquals(1, count);
    }

    // --- getCategories ---

    @Test
    void shouldReturnNonNullCategories() {
        EntityDamageEvent event = mockEvent(DamageCause.VOID);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertNotNull(ctx.getCategories());
        assertFalse(ctx.getCategories().isEmpty());
    }

    // --- Entity Damage By Entity (projectile mocks) ---

    @Test
    void shouldCategorizeArrowAsProjectileAndPhysical() {
        EntityDamageByEntityEvent event = mockEntityEvent(DamageCause.PROJECTILE, Arrow.class);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.PROJECTILE));
        assertTrue(ctx.hasCategory(RPGDamageCategory.PHYSICAL));
    }

    @Test
    void shouldCategorizeThrownPotionAsMagicalAndAoe() {
        EntityDamageByEntityEvent event = mockEntityEvent(DamageCause.ENTITY_ATTACK, ThrownPotion.class);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.AOE));
    }

    @Test
    void shouldCategorizeWitherSkullAsMagicalAndAoe() {
        EntityDamageByEntityEvent event = mockEntityEvent(DamageCause.PROJECTILE, WitherSkull.class);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.AOE));
    }

    @Test
    void shouldCategorizeEvokerFangsAsMagicalAndAoe() {
        EntityDamageByEntityEvent event = mockEntityEvent(DamageCause.ENTITY_ATTACK, EvokerFangs.class);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MAGICAL));
        assertTrue(ctx.hasCategory(RPGDamageCategory.AOE));
    }

    // --- Armed vs Unarmed ---

    @Test
    void shouldCategorizeAsArmed_whenAttackerHasWeapon() {
        Player attacker = mock(Player.class);
        when(attacker.getType()).thenReturn(EntityType.PLAYER);
        EntityEquipment equipment = mock(EntityEquipment.class);
        ItemStack sword = mock(ItemStack.class);
        when(sword.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(equipment.getItemInMainHand()).thenReturn(sword);
        when(attacker.getEquipment()).thenReturn(equipment);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getCause()).thenReturn(DamageCause.ENTITY_ATTACK);
        when(event.getDamager()).thenReturn(attacker);
        Entity victim = mock(Entity.class);
        when(event.getEntity()).thenReturn(victim);

        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.ARMED));
        assertFalse(ctx.hasCategory(RPGDamageCategory.UNARMED));
    }

    @Test
    void shouldCategorizeAsUnarmed_whenAttackerHasNoWeapon() {
        Player attacker = mock(Player.class);
        when(attacker.getType()).thenReturn(EntityType.PLAYER);
        EntityEquipment equipment = mock(EntityEquipment.class);
        ItemStack air = mock(ItemStack.class);
        when(air.getType()).thenReturn(Material.AIR);
        when(equipment.getItemInMainHand()).thenReturn(air);
        when(attacker.getEquipment()).thenReturn(equipment);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getCause()).thenReturn(DamageCause.ENTITY_ATTACK);
        when(event.getDamager()).thenReturn(attacker);
        Entity victim = mock(Entity.class);
        when(event.getEntity()).thenReturn(victim);

        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.UNARMED));
        assertFalse(ctx.hasCategory(RPGDamageCategory.ARMED));
    }

    // --- GLOBAL ---

    @Test
    void shouldCategorizeCustomAsGlobal() {
        EntityDamageEvent event = mockEvent(DamageCause.CUSTOM);
        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.GLOBAL));
    }

    // --- MINION ---

    @Test
    void shouldCategorizeTameableAttackerAsMinion() {
        Wolf wolf = mock(Wolf.class);
        Player owner = mock(Player.class);
        when(wolf.getOwner()).thenReturn(owner);
        lenient().when(wolf.getType()).thenReturn(EntityType.WOLF);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getCause()).thenReturn(DamageCause.ENTITY_ATTACK);
        when(event.getDamager()).thenReturn(wolf);
        Entity victim = mock(Entity.class);
        when(event.getEntity()).thenReturn(victim);

        RPGDamageContext ctx = new RPGDamageContext(event);
        assertTrue(ctx.hasCategory(RPGDamageCategory.MINION));
    }

    // --- Helpers ---

    private EntityDamageEvent mockEvent(DamageCause cause) {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getCause()).thenReturn(cause);
        Entity entity = mock(Entity.class);
        when(event.getEntity()).thenReturn(entity);
        return event;
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> EntityDamageByEntityEvent mockEntityEvent(DamageCause cause, Class<T> damagerType) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getCause()).thenReturn(cause);
        T damager = mock(damagerType);
        EntityType entityType = EntityType.ZOMBIE;
        lenient().when(damager.getType()).thenReturn(entityType);
        when(event.getDamager()).thenReturn(damager);
        Entity victim = mock(Entity.class);
        when(event.getEntity()).thenReturn(victim);
        return event;
    }
}
