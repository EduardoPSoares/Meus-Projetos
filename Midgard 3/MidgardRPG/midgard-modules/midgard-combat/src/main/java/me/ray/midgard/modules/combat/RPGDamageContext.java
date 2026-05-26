package me.ray.midgard.modules.combat;

import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.EnumSet;
import java.util.Set;

/**
 * Contexto de dano RPG.
 * <p>
 * Analisa um evento de dano do Bukkit (EntityDamageEvent) e determina quais categorias de dano RPG se aplicam.
 * Permite que o sistema de combate trate diferentes fontes de dano de maneira apropriada (ex: aplicar resistência mágica contra poções).
 */
@SuppressWarnings("deprecation")
public class RPGDamageContext {

    @SuppressWarnings("unused")
    private final EntityDamageEvent event;
    private final Set<RPGDamageCategory> categories;

    /**
     * Cria um novo contexto de dano a partir de um evento.
     *
     * @param event O evento de dano original.
     */
    public RPGDamageContext(EntityDamageEvent event) {
        this.event = event;
        this.categories = determineCategories(event);
    }

    /**
     * Obtém as categorias de dano identificadas.
     *
     * @return Um conjunto de categorias de dano.
     */
    public Set<RPGDamageCategory> getCategories() {
        return categories;
    }

    /**
     * Verifica se o contexto possui uma categoria específica.
     *
     * @param category A categoria a ser verificada.
     * @return true se a categoria estiver presente, false caso contrário.
     */
    public boolean hasCategory(RPGDamageCategory category) {
        return categories.contains(category);
    }

    /**
     * Adiciona uma categoria ao contexto.
     *
     * @param category A categoria a ser adicionada.
     */
    public void addCategory(RPGDamageCategory category) {
        categories.add(category);
    }

    /**
     * Determina as categorias de dano baseadas na causa e no damager do evento.
     * Segue a ordem de verificação:
     * 1. Físico ou Mágico
     * 2. Ambiental, Armado ou Desarmado
     * 3. Projétil
     * 4. Explosão
     *
     * @param event O evento de dano.
     * @return Um conjunto de categorias determinadas.
     */
    private Set<RPGDamageCategory> determineCategories(EntityDamageEvent event) {
        Set<RPGDamageCategory> cats = EnumSet.noneOf(RPGDamageCategory.class);
        DamageCause cause = event.getCause();

        // Etapa 1: Dano Físico ou Mágico
        boolean isPhysical = false;
        boolean isMagical = false;

        // Causas Físicas
        if (cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.ENTITY_SWEEP_ATTACK || 
            cause == DamageCause.PROJECTILE || cause == DamageCause.CONTACT || 
            cause == DamageCause.FALL || cause == DamageCause.FLY_INTO_WALL || 
            cause == DamageCause.FALLING_BLOCK || cause == DamageCause.BLOCK_EXPLOSION || 
            cause == DamageCause.ENTITY_EXPLOSION || cause == DamageCause.SUFFOCATION ||
            cause == DamageCause.CRAMMING) {
            isPhysical = true;
        }

        // Causas Mágicas
        if (cause == DamageCause.MAGIC || cause == DamageCause.POISON || cause == DamageCause.WITHER || 
            cause == DamageCause.DRAGON_BREATH || cause == DamageCause.THORNS || cause == DamageCause.SONIC_BOOM ||
            cause == DamageCause.FREEZE || cause == DamageCause.LIGHTNING) {
            isMagical = true;
        }
        
        // Verificações adicionais baseadas na entidade causadora (para casos mistos)
        if (event instanceof EntityDamageByEntityEvent subEvent) {
             Entity damager = subEvent.getDamager();
             
             // Projéteis mágicos
             if (damager instanceof ThrownPotion || damager instanceof AreaEffectCloud || 
                 damager instanceof EvokerFangs || damager instanceof WitherSkull || 
                 damager instanceof DragonFireball || damager instanceof Fireball) {
                 isMagical = true;
             }
             
             // Tridente e Flechas são físicos (já cobertos por PROJECTILE/ENTITY_ATTACK, mas reforçando)
             if (damager instanceof Trident || damager instanceof Arrow || damager instanceof SpectralArrow) {
                 isPhysical = true;
             }
        }

        if (isPhysical) {
            cats.add(RPGDamageCategory.PHYSICAL);
        }
        if (isMagical) {
            cats.add(RPGDamageCategory.MAGICAL);
        }


        // Etapa 2: Dano Ambiente, Armado ou Desarmado
        if (isEnvironmental(cause)) {
            cats.add(RPGDamageCategory.ENVIRONMENTAL);
        } else if (cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.ENTITY_SWEEP_ATTACK) {
            // Verifica se é Armado ou Desarmado
            boolean armed = false;
            if (event instanceof EntityDamageByEntityEvent subEvent) {
                Entity damager = subEvent.getDamager();
                if (damager instanceof LivingEntity livingDamager) {
                    org.bukkit.inventory.EntityEquipment equipment = livingDamager.getEquipment();
                    if (equipment != null) {
                        org.bukkit.inventory.ItemStack mainHand = equipment.getItemInMainHand();
                        if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
                            armed = true;
                        }
                    }
                }
            }
            
            if (armed) {
                cats.add(RPGDamageCategory.ARMED);
            } else {
                cats.add(RPGDamageCategory.UNARMED);
            }
        }

        // Etapa 3: Dano Projetil
        if (cause == DamageCause.PROJECTILE) {
            cats.add(RPGDamageCategory.PROJECTILE);
        } else if (event instanceof EntityDamageByEntityEvent subEvent) {
             if (subEvent.getDamager() instanceof Projectile) {
                 cats.add(RPGDamageCategory.PROJECTILE);
             }
        }

        // Etapa 4: Dano Explosão
        if (cause == DamageCause.BLOCK_EXPLOSION || cause == DamageCause.ENTITY_EXPLOSION) {
            cats.add(RPGDamageCategory.EXPLOSION);
            cats.add(RPGDamageCategory.AOE); // Explosões são AoE
        }
        
        // Etapa 5: Dano AoE (Outros)
        if (event instanceof EntityDamageByEntityEvent subEvent) {
            Entity damager = subEvent.getDamager();
            if (damager instanceof ThrownPotion || 
                damager instanceof AreaEffectCloud || 
                damager instanceof EvokerFangs || 
                damager instanceof DragonFireball ||
                damager instanceof WitherSkull ||
                damager.getType().name().contains("POTION")) {
                cats.add(RPGDamageCategory.AOE);
            }
        }
        
        // Regra de Ouro: Se for Mágico e Projétil, é AoE (Poções)
        if (cats.contains(RPGDamageCategory.MAGICAL) && cats.contains(RPGDamageCategory.PROJECTILE)) {
            cats.add(RPGDamageCategory.AOE);
        }
        
        // Etapa 6: DOT (Damage Over Time)
        if (cause == DamageCause.POISON || cause == DamageCause.WITHER || 
            cause == DamageCause.FIRE_TICK || cause == DamageCause.STARVATION ||
            cause == DamageCause.DRYOUT) {
            cats.add(RPGDamageCategory.DOT);
        }

        // Etapa 7: MINION (projéteis ou mobs com dono)
        if (event instanceof EntityDamageByEntityEvent subEvent) {
            Entity damagerEntity = subEvent.getDamager();
            // Projétil lançado por mob que tem dono
            if (damagerEntity instanceof Projectile proj && proj.getShooter() instanceof org.bukkit.entity.Tameable tameable) {
                if (tameable.getOwner() != null) {
                    cats.add(RPGDamageCategory.MINION);
                }
            } else if (damagerEntity instanceof org.bukkit.entity.Tameable tameable && tameable.getOwner() != null) {
                cats.add(RPGDamageCategory.MINION);
            }
        }

        // Etapa 8: SKILL — detectado via metadata (setado pelo módulo de spells/MythicMobs)
        if (event.getEntity().hasMetadata("midgard_forced_type")) {
            String forcedType = event.getEntity().getMetadata("midgard_forced_type").get(0).asString();
            if ("SKILL".equalsIgnoreCase(forcedType)) {
                cats.add(RPGDamageCategory.SKILL);
            }
        }

        // Global (Mantido)
        if (cause == DamageCause.CUSTOM || cause.name().equals("KILL") || cause.name().equals("GENERIC_KILL")) {
            cats.add(RPGDamageCategory.GLOBAL);
        }

        return cats;
    }

    private boolean isEnvironmental(DamageCause cause) {
        switch (cause) {
            case CONTACT:
            case FIRE:
            case FIRE_TICK:
            case LAVA:
            case DROWNING:
            case STARVATION:
            case SUFFOCATION:
            case FALL:
            case FLY_INTO_WALL:
            case VOID:
            case LIGHTNING:
            case FALLING_BLOCK:
            case HOT_FLOOR:
            case CRAMMING:
            case DRYOUT:
            case WORLD_BORDER:
            case MELTING:
            case CAMPFIRE:
                return true;
            default:
                return false;
        }
    }
}
