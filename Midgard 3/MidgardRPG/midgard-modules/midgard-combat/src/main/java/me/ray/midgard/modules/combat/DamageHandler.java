package me.ray.midgard.modules.combat;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.debug.DebugCategory;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.attribute.AttributeInstance;
import me.ray.midgard.core.attribute.CoreAttributeData;
import me.ray.midgard.core.integration.MythicMobsIntegration;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.modules.combat.mechanics.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manipulador central de cálculo e aplicação de dano.
 * <p>
 * Esta classe orquestra todo o fluxo de dano do RPG, incluindo:
 * <ul>
 *     <li>Identificação do tipo de ataque (Físico, Mágico, Projétil).</li>
 *     <li>Cálculo de dano base e escalonamento por atributos.</li>
 *     <li>Aplicação de mecânicas defensivas (Esquiva, Bloqueio, Aparar).</li>
 *     <li>Cálculo de mitigação de dano (Armadura, Resistência Mágica).</li>
 *     <li>Aplicação de efeitos pós-dano (Roubo de Vida, Espinhos).</li>
 * </ul>
 */
public class DamageHandler {

    private final CombatManager combatManager;
    private final CombatConfig config;
    private final DamageIndicatorManager indicatorManager;

    // Mechanics & Calculators
    private final PhysicalDamageCalculator physicalCalculator;
    private final MagicalDamageCalculator magicalCalculator;
    private final ElementalDamageCalculator elementalCalculator;
    private final MitigationHandler mitigationHandler;
    private final DodgeMechanic dodgeMechanic;
    private final ParryMechanic parryMechanic;
    private final BlockMechanic blockMechanic;
    private final ThornsMechanic thornsMechanic;
    private final LifeStealMechanic lifeStealMechanic;

    /** Cache de atributos de mobs para evitar alocação/consulta repetida a cada evento de dano. */
    private static final long MOB_CACHE_TTL_MS = 1000L;
    private final Map<UUID, CachedMobAttributes> mobAttributeCache = new ConcurrentHashMap<>();

    private record CachedMobAttributes(CoreAttributeData data, long timestamp) {}

    /** Remove entradas expiradas do cache de mobs. */
    public void cleanupMobCache() {
        long now = System.currentTimeMillis();
        mobAttributeCache.entrySet().removeIf(e -> (now - e.getValue().timestamp()) >= MOB_CACHE_TTL_MS);
    }

    /**
     * Construtor do DamageHandler.
     *
     * @param combatManager Gerenciador de combate.
     * @param config Configuração de combate.
     * @param indicatorManager Gerenciador de indicadores de dano.
     */
    public DamageHandler(CombatManager combatManager, CombatConfig config, DamageIndicatorManager indicatorManager) {
        this.combatManager = combatManager;
        this.config = config;
        this.indicatorManager = indicatorManager;

        // Initialize components
        this.physicalCalculator = new PhysicalDamageCalculator();
        this.magicalCalculator = new MagicalDamageCalculator();
        this.elementalCalculator = new ElementalDamageCalculator(config);
        this.mitigationHandler = new MitigationHandler(config);
        this.dodgeMechanic = new DodgeMechanic(indicatorManager);
        this.parryMechanic = new ParryMechanic(indicatorManager);
        this.blockMechanic = new BlockMechanic(indicatorManager);
        this.thornsMechanic = new ThornsMechanic();
        this.lifeStealMechanic = new LifeStealMechanic(combatManager);
    }

    // --- Pipeline State ---

    /**
     * Estado mutável que flui pelo pipeline de cálculo de dano.
     */
    private static class DamageState {
        double damage;
        double elementalDamage;
        double lifeSteal;
        double manaSteal;
        boolean isCritical;
        Player attackerPlayer;
        LivingEntity attackerEntity;
        CoreAttributeData attackerAttributes;
        int attackerLevel = 1;
        final Map<String, Double> damageMap = new LinkedHashMap<>();
        String mainDamageKey;
        String forcedElement;
        String forcedType;
        String damageSource; // "SKILL", "MINION", or null
        Player minionOwner; // resolved owner if MINION category
    }

    // --- Main Entry Point ---

    /**
     * Processa um evento de dano do Bukkit.
     *
     * @param event O evento de dano original.
     */
    public void handleDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        try {
            RPGDamageContext context = new RPGDamageContext(event);
            DamageState state = initializeDamageState(event, victim);

            // Injeta categorias a partir de damageSource (setado por módulos externos)
            if ("SKILL".equalsIgnoreCase(state.damageSource)) {
                context.addCategory(RPGDamageCategory.SKILL);
            } else if ("MINION".equalsIgnoreCase(state.damageSource)) {
                context.addCategory(RPGDamageCategory.MINION);
            }

            // Fase 1: Resolução do atacante e cálculo de dano
            if (event instanceof EntityDamageByEntityEvent entityEvent) {
                processAttackerDamage(state, entityEvent, context, victim);
            }

            // Fase 2: Categorização fallback para dano sem entidade
            if (state.damageMap.isEmpty()) {
                categorizeFallbackDamage(state, context);
            }

            // Fase 3: Mecânicas defensivas (Esquiva/Aparar/Bloqueio) — apenas contra entidades
            CoreAttributeData victimAttributes = getEntityAttributes(victim);
            if (state.attackerEntity != null) {
                if (config.dodgeEnabled && dodgeMechanic.apply(event, victim, victimAttributes, state.attackerAttributes)) {
                    return;
                }
                if (config.parryEnabled && parryMechanic.apply(event, victim, victimAttributes, state.attackerAttributes)) {
                    return;
                }
                if (config.blockEnabled) {
                    state.damage = blockMechanic.apply(state.damage, victim, victimAttributes, state.attackerAttributes);
                }
            }

            // Fase 3.5: Bônus de dano por fonte (SKILL / MINION)
            if (context.hasCategory(RPGDamageCategory.SKILL) && config.skillDamageBonusEnabled && state.attackerAttributes != null) {
                AttributeInstance skillBonusAttr = state.attackerAttributes.getInstance(CombatAttributes.SKILL_DAMAGE_BONUS);
                if (skillBonusAttr != null && skillBonusAttr.getValue() > 0) {
                    state.damage *= (1.0 + skillBonusAttr.getValue() / 100.0);
                }
            }
            if (context.hasCategory(RPGDamageCategory.MINION) && config.minionDamageBonusEnabled) {
                // Busca atributos do dono do minion
                Player owner = state.minionOwner;
                if (owner != null) {
                    CoreAttributeData ownerAttrs = getEntityAttributes(owner);
                    AttributeInstance minionBonusAttr = ownerAttrs.getInstance(CombatAttributes.MINION_DAMAGE);
                    if (minionBonusAttr != null && minionBonusAttr.getValue() > 0) {
                        state.damage *= (1.0 + minionBonusAttr.getValue() / 100.0);
                    }
                }
            }

            // Fase 4: Mitigação (Defesa, Resistência, Reduções)
            state.damage = mitigationHandler.applyMitigation(state.damage, victimAttributes, state.attackerAttributes,
                    state.attackerLevel, context, event.getCause(), state.attackerPlayer != null);

            // Aplica redução geral ao dano elemental ANTES de thorns
            if (state.elementalDamage > 0 && victimAttributes != null) {
                double generalReduction = getGeneralDamageReduction(victimAttributes, state.attackerPlayer != null);
                if (generalReduction > 0) {
                    state.elementalDamage *= Math.max(0.0, 1.0 - (generalReduction / 100.0));
                }
            }

            // Fase 5: Efeitos pós-mitigação (Espinhos)
            if (config.thornsEnabled && state.attackerPlayer != null) {
                thornsMechanic.apply(state.attackerPlayer, state.damage, state.elementalDamage, victimAttributes, context);
            }

            // Atualiza mapa com dano mitigado e adiciona elemental
            if (state.mainDamageKey != null) {
                state.damageMap.put(state.mainDamageKey, state.damage);
            }
            state.damage += state.elementalDamage;

            // Fase 6: Debug
            logDebugInfo(state, context, event, victim);

            // Fase 7: Aplicação do dano final
            applyFinalDamage(state, event, victim, victimAttributes);

            // Fase 8: Indicadores de dano
            spawnDamageIndicators(state, victim);

        } catch (Exception e) {
            MidgardLogger.error("Erro no evento onDamage", e);
        }
    }

    // --- Pipeline Phases ---

    private DamageState initializeDamageState(EntityDamageEvent event, LivingEntity victim) {
        DamageState state = new DamageState();
        state.damage = event.getDamage();
        state.forcedElement = DamageOverrideContext.getForcedElement(victim);
        state.forcedType = DamageOverrideContext.getForcedType(victim);
        state.damageSource = DamageOverrideContext.getDamageSource(victim);
        Double forcedDamage = DamageOverrideContext.getForcedDamage(victim);

        if (forcedDamage != null) {
            state.damage = forcedDamage;
        }

        DamageOverrideContext.clear(victim);

        // Adiciona categorias baseado no damageSource (se definido por módulo externo)
        // Isso garante que setDamageSource("SKILL") adicione RPGDamageCategory.SKILL
        // independentemente de setForcedType também ser chamado.
        // As categorias serão usadas pelo pipeline para bônus e reduções.
        // (As categorias são adicionadas ao RPGDamageContext após o retorno deste método,
        //  então armazenamos no state e aplicamos no handleDamage principal.)

        MidgardLogger.debug(DebugCategory.COMBAT, "Calculando Dano: %f -> %f (Base: %.2f | For\u00e7ado: %.2f | Tipo: %s | Elem: %s)",
                event.getDamage(), forcedDamage != null ? forcedDamage : 0.0,
                state.damage,
                forcedDamage != null ? forcedDamage : 0.0,
                state.forcedType != null ? state.forcedType : "VANILLA",
                state.forcedElement != null ? state.forcedElement : "NENHUM");

        return state;
    }

    private void processAttackerDamage(DamageState state, EntityDamageByEntityEvent event,
                                       RPGDamageContext context, LivingEntity victim) {
        Entity damager = event.getDamager();

        // Resolução de identidade do atacante
        if (damager instanceof Player p) {
            state.attackerPlayer = p;
            state.attackerEntity = p;
        } else if (damager instanceof Projectile proj && proj.getShooter() instanceof LivingEntity shooter) {
            state.attackerEntity = shooter;
            if (shooter instanceof Player p) {
                state.attackerPlayer = p;
            }
            // Minion: projétil lançado por mob com dono
            if (shooter instanceof org.bukkit.entity.Tameable tameable && tameable.getOwner() instanceof Player owner) {
                state.minionOwner = owner;
            }
        } else if (damager instanceof LivingEntity le) {
            state.attackerEntity = le;
            // Minion: mob com dono direto
            if (le instanceof org.bukkit.entity.Tameable tameable && tameable.getOwner() instanceof Player owner) {
                state.minionOwner = owner;
            }
        }

        if (state.attackerEntity == null) {
            return;
        }
        state.attackerAttributes = getEntityAttributes(state.attackerEntity);
        if (state.attackerAttributes == null) {
            return;
        }

        // Nível do atacante
        if (state.attackerPlayer != null) {
            MidgardProfile attackerProfile = MidgardCore.getProfileManager().getProfile(state.attackerPlayer.getUniqueId());
            if (attackerProfile != null) {
                CombatData attackerCombatData = attackerProfile.getOrCreateData(CombatData.class);
                state.attackerLevel = attackerCombatData.getLevel();
            }
        }

        // Atributos de combate do atacante
        AttributeInstance lifeStealAttr = state.attackerAttributes.getInstance(CombatAttributes.LIFE_STEAL);
        state.lifeSteal = lifeStealAttr != null ? lifeStealAttr.getValue() : 0.0;

        AttributeInstance manaStealAttr = state.attackerAttributes.getInstance(CombatAttributes.MANA_STEAL);
        state.manaSteal = manaStealAttr != null ? manaStealAttr.getValue() : 0.0;

        AttributeInstance spellVampAttr = state.attackerAttributes.getInstance(CombatAttributes.SPELL_VAMPIRISM);
        double spellVamp = spellVampAttr != null ? spellVampAttr.getValue() : 0.0;

        // Resolve elemento forçado via tags do scoreboard (MythicMobs/Plugins)
        if (state.forcedElement == null) {
            for (String tag : damager.getScoreboardTags()) {
                if (tag.toLowerCase().startsWith("midgard.damage.")) {
                    state.forcedElement = tag.substring("midgard.damage.".length());
                    break;
                }
            }
        }

        // Seleção de calculadora e cálculo de dano
        DamageResult result = selectDamageCalculator(state, context, victim, damager, spellVamp);

        state.damage = result.getDamage();
        state.isCritical = result.isCritical();
        state.mainDamageKey = result.getDamageKey();
        state.damageMap.put(state.mainDamageKey, state.damage);

        // Cálculo elemental
        double[] totalElemental = {state.elementalDamage};
        elementalCalculator.calculateAndApply(state.attackerAttributes, victim, state.damageMap, totalElemental, state.attackerLevel);
        state.elementalDamage = totalElemental[0];

        // Combat Tag do atacante
        if (state.attackerPlayer != null) {
            UUID attackerId = state.attackerPlayer.getUniqueId();
            Long expiry = combatManager.getCombatTag().get(attackerId);
            boolean wasInCombat = expiry != null && expiry > System.currentTimeMillis();
            combatManager.updateCombatTag(attackerId);

            if (!wasInCombat && CombatModule.getInstance() != null) {
                String combatMsg = CombatModule.getInstance().getMessage("combat_mode.enabled");
                me.ray.midgard.core.text.MessageUtils.send(state.attackerPlayer, combatMsg);
            }
        }
    }

    private DamageResult selectDamageCalculator(DamageState state, RPGDamageContext context,
                                                 LivingEntity victim, Entity damager, double spellVamp) {
        boolean isTipped = isTippedArrow(damager);
        boolean isPhysicalAttack = !isTipped && (context.hasCategory(RPGDamageCategory.PHYSICAL) ||
                (context.hasCategory(RPGDamageCategory.PROJECTILE) && !context.hasCategory(RPGDamageCategory.MAGICAL)));

        if (state.forcedElement != null) {
            MidgardLogger.debug(DebugCategory.COMBAT, "Elemento for\u00e7ado detectado: %s", state.forcedElement);
        }

        // Aplicação de tipo forçado
        if (state.forcedType != null) {
            try {
                RPGDamageCategory cat = RPGDamageCategory.valueOf(state.forcedType.toUpperCase());
                context.addCategory(cat);
                if (cat == RPGDamageCategory.MAGICAL) {
                    isPhysicalAttack = false;
                }
            } catch (IllegalArgumentException ignored) { /* Tipo forçado inválido, ignorar */ }
        }

        if (state.forcedElement != null) {
            double mitigated = elementalCalculator.calculateMitigatedDamage(state.forcedElement, state.damage, victim, state.attackerLevel);
            state.elementalDamage += mitigated;
            state.damageMap.put(state.forcedElement.toLowerCase() + "_damage", mitigated);
            return new DamageResult(0, false, "Elemental");
        } else if (state.attackerPlayer == null) {
            String typeLabel = isPhysicalAttack ? "Physical" : "Magical";
            if (context.hasCategory(RPGDamageCategory.GLOBAL)) {
                typeLabel = "True";
            }
            return new DamageResult(state.damage, false, typeLabel);
        } else if (isPhysicalAttack) {
            return physicalCalculator.calculate(state.attackerPlayer, victim, state.attackerAttributes, context, state.damage);
        } else if (context.hasCategory(RPGDamageCategory.MAGICAL) || isTipped) {
            DamageResult result = magicalCalculator.calculate(state.attackerPlayer, victim, state.attackerAttributes, context, state.damage);
            if (spellVamp > 0) {
                state.lifeSteal += spellVamp;
            }
            return result;
        } else {
            List<String> types = new ArrayList<>();
            if (context.hasCategory(RPGDamageCategory.PROJECTILE)) {
                types.add("Projectile");
            }
            if (context.hasCategory(RPGDamageCategory.PHYSICAL)) {
                types.add("Physical");
            }
            if (types.isEmpty()) {
                types.add("Physical");
            }
            return new DamageResult(state.damage, false, String.join("+", types));
        }
    }

    private void categorizeFallbackDamage(DamageState state, RPGDamageContext context) {
        List<String> types = new ArrayList<>();
        if (context.hasCategory(RPGDamageCategory.PROJECTILE)) {
            types.add("Projectile");
        }
        if (context.hasCategory(RPGDamageCategory.PHYSICAL)) {
            types.add("Physical");
        }
        if (context.hasCategory(RPGDamageCategory.MAGICAL)) {
            types.add("Magical");
        }
        if (context.hasCategory(RPGDamageCategory.ENVIRONMENTAL)) {
            types.add("Environment");
        }
        if (context.hasCategory(RPGDamageCategory.GLOBAL)) {
            types.add("True");
        }
        if (types.isEmpty()) {
            types.add("Physical");
        }

        String key = String.join("+", types);
        state.mainDamageKey = key;
        state.damageMap.put(key, state.damage);
    }

    private boolean isTippedArrow(Entity damager) {
        if (!(damager instanceof org.bukkit.entity.Arrow arrow)) {
            return false;
        }
        if (arrow.hasCustomEffects()) {
            return true;
        }
        try {
            var potionType = arrow.getBasePotionType();
            if (potionType == null) {
                return false;
            }
            @SuppressWarnings("deprecation")
            var effectType = potionType.getEffectType();
            return effectType != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void logDebugInfo(DamageState state, RPGDamageContext context, EntityDamageEvent event, LivingEntity victim) {
        if (victim instanceof Player p && combatManager.isDebugging(p.getUniqueId())) {
            String attackerName = (state.attackerEntity != null ? state.attackerEntity.getName() : CombatModule.getInstance().getMessage("debug.environment"));
            String causeName = event.getCause().name();
            String cats = context.getCategories().toString();
            String dmgStr = String.format("%.2f", state.damage);
            String elemStr = String.format("%.2f", state.elementalDamage);
            String forcedStr = (state.forcedType != null ? state.forcedType : "") + (state.forcedElement != null ? " " + state.forcedElement : "");

            combatManager.getDebugScoreboard().update(p, attackerName, causeName, cats, dmgStr, elemStr, forcedStr, state.damageMap);
        }
        if (state.attackerPlayer != null && combatManager.isDebugging(state.attackerPlayer.getUniqueId())) {
            MidgardLogger.debug(DebugCategory.COMBAT, "Causado: %.2f em %s (Atacante: %s)", state.damage, victim.getName(), state.attackerPlayer.getName());
        }
    }

    private void applyFinalDamage(DamageState state, EntityDamageEvent event, LivingEntity victim, CoreAttributeData victimAttributes) {
        if (Double.isNaN(state.damage) || Double.isInfinite(state.damage)) {
            MidgardLogger.warn("Dano inv\u00e1lido (NaN/Infinity) detectado para " + victim.getName() + ". Resetando para 0.");
            state.damage = 0.0;
        }
        if (state.damage < 0) {
            state.damage = 0;
        }

        if (victim instanceof Player player) {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null) {
                CombatData combatData = profile.getOrCreateData(CombatData.class);
                AttributeInstance maxHealthAttr = victimAttributes.getInstance(CombatAttributes.MAX_HEALTH);
                double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 100;

                double remainingDamage = state.damage;

                // Absorption Shield — absorve dano antes da vida
                if (config.absorptionEnabled && combatData.getCurrentAbsorption() > 0) {
                    double absorption = combatData.getCurrentAbsorption();
                    if (absorption >= remainingDamage) {
                        combatData.setCurrentAbsorption(absorption - remainingDamage);
                        remainingDamage = 0;
                    } else {
                        remainingDamage -= absorption;
                        combatData.setCurrentAbsorption(0);
                    }
                }

                double oldHealth = combatData.getCurrentHealth();
                double newHealth = Math.max(0, oldHealth - remainingDamage);
                combatData.setCurrentHealth(newHealth);

                if (combatManager.isDebugging(player.getUniqueId())) {
                    MidgardLogger.debug(DebugCategory.COMBAT, "Vida de %s: %.2f -> %.2f (Max: %.2f | Absor\u00e7\u00e3o: %.2f)", player.getName(), oldHealth, newHealth, maxHealth, combatData.getCurrentAbsorption());
                }

                // Life steal — atacante rouba vida (ou dono do minion)
                if (config.lifeStealEnabled) {
                    if (state.attackerPlayer != null) {
                        lifeStealMechanic.apply(state.attackerPlayer, state.damage, state.lifeSteal);
                    } else if (config.minionOwnerLifeSteal && state.minionOwner != null && state.lifeSteal > 0) {
                        lifeStealMechanic.apply(state.minionOwner, state.damage, state.lifeSteal);
                    }
                }

                // Mana steal — atacante rouba mana
                if (state.manaSteal > 0 && state.attackerPlayer != null) {
                    applyManaSteal(state.attackerPlayer, state.damage, state.manaSteal);
                }

                // Notify victim of combat mode if not already in combat
                Long victimExpiry = combatManager.getCombatTag().get(player.getUniqueId());
                boolean victimWasInCombat = victimExpiry != null && victimExpiry > System.currentTimeMillis();
                combatManager.updateCombatTag(player.getUniqueId());
                if (!victimWasInCombat && CombatModule.getInstance() != null) {
                    String combatMsg = CombatModule.getInstance().getMessage("combat_mode.enabled");
                    me.ray.midgard.core.text.MessageUtils.send(player, combatMsg);
                }
                combatManager.syncHealth(player, newHealth, maxHealth);

                event.setDamage(0);

                if (newHealth <= 0) {
                    player.setHealth(0);
                }
            }
        } else {
            if (config.lifeStealEnabled) {
                if (state.attackerPlayer != null) {
                    lifeStealMechanic.apply(state.attackerPlayer, state.damage, state.lifeSteal);
                } else if (config.minionOwnerLifeSteal && state.minionOwner != null && state.lifeSteal > 0) {
                    lifeStealMechanic.apply(state.minionOwner, state.damage, state.lifeSteal);
                }
            }
            // Mana steal (non-player victim)
            if (state.manaSteal > 0 && state.attackerPlayer != null) {
                applyManaSteal(state.attackerPlayer, state.damage, state.manaSteal);
            }
            event.setDamage(state.damage);
        }
    }

    private void spawnDamageIndicators(DamageState state, LivingEntity victim) {
        MidgardLogger.debug(DebugCategory.COMBAT, "Verificando indicadores: Config=%s, Enabled=%s, Damage=%.2f",
                config != null,
                (config != null && config.indicatorEnabled),
                state.damage);

        if (config != null && config.indicatorEnabled && state.damage > 0.05) {
            MidgardLogger.debug(DebugCategory.COMBAT, "Chamando spawnIndicator para %s", victim.getName());
            indicatorManager.spawnIndicator(victim, state.damageMap, state.isCritical);
        } else {
            MidgardLogger.debug(DebugCategory.COMBAT, "SpawnIndicator ignorado. Motivo: %s",
                    (config == null) ? "Config NULL" :
                    (!config.indicatorEnabled) ? "Desativado" :
                    (state.damage <= 0.05) ? "Dano Baixo" : "Desconhecido");
        }
    }

    private double getGeneralDamageReduction(CoreAttributeData victimAttributes, boolean isAttackerPlayer) {
        double totalReduction = 0.0;
        AttributeInstance dmgRedAttr = victimAttributes.getInstance(CombatAttributes.DAMAGE_REDUCTION);
        if (dmgRedAttr != null) {
            totalReduction += dmgRedAttr.getValue();
        }
        if (isAttackerPlayer) {
            AttributeInstance pvpRedAttr = victimAttributes.getInstance(CombatAttributes.PVP_DAMAGE_REDUCTION);
            if (pvpRedAttr != null) {
                totalReduction += pvpRedAttr.getValue();
            }
        } else {
            AttributeInstance pveRedAttr = victimAttributes.getInstance(CombatAttributes.PVE_DAMAGE_REDUCTION);
            if (pveRedAttr != null) {
                totalReduction += pveRedAttr.getValue();
            }
        }
        return totalReduction;
    }

    private void applyManaSteal(Player attacker, double damage, double manaStealPercent) {
        if (attacker == null || manaStealPercent <= 0 || damage <= 0) {
            return;
        }
        double manaGain = damage * (manaStealPercent / 100.0);
        if (manaGain <= 0) {
            return;
        }
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(attacker.getUniqueId());
        if (profile == null) {
            return;
        }
        CombatData data = profile.getOrCreateData(CombatData.class);
        CoreAttributeData attrs = profile.getOrCreateData(CoreAttributeData.class);
        AttributeInstance maxManaAttr = attrs.getInstance(CombatAttributes.MAX_MANA);
        double maxMana = maxManaAttr != null ? maxManaAttr.getValue() : 100;
        double newMana = Math.min(maxMana, data.getCurrentMana() + manaGain);
        data.setCurrentMana(newMana);
    }

    private CoreAttributeData getEntityAttributes(LivingEntity entity) {
        if (entity instanceof Player player) {
            MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player.getUniqueId());
            if (profile != null) {
                return profile.getOrCreateData(CoreAttributeData.class);
            }
        } else {
            // Mob - Use cached attribute data to avoid repeated allocations
            UUID entityId = entity.getUniqueId();
            long now = System.currentTimeMillis();

            // Lazy eviction when cache grows
            if (mobAttributeCache.size() > 200) {
                cleanupMobCache();
            }

            CachedMobAttributes cached = mobAttributeCache.get(entityId);
            if (cached != null && (now - cached.timestamp()) < MOB_CACHE_TTL_MS) {
                return cached.data();
            }

            CoreAttributeData data = new CoreAttributeData();
            Map<String, Double> attributes = MythicMobsIntegration.getAttributes(entity);
            
            for (Map.Entry<String, Double> entry : attributes.entrySet()) {
                AttributeInstance instance = data.getInstance(entry.getKey());
                if (instance != null) {
                    instance.setBaseValue(entry.getValue());
                }
            }

            mobAttributeCache.put(entityId, new CachedMobAttributes(data, now));
            return data;
        }
        return new CoreAttributeData(); // Empty fallback
    }
}