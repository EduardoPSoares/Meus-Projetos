package com.midgard.fooddecay.multiblock;

import com.midgard.fooddecay.FoodTrait;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.*;

/**
 * Defines all multiblock structures for the TFC-style food preservation system.
 * Each machine type has 3 tiers of increasing complexity unlocked by player level.
 */
public enum MultiblockType {

    // =====================================================================
    //  DRYING RACK — Linear structure
    // =====================================================================
    DRYING_RACK("secador", FoodTrait.DRIED, true,
            Particle.SMOKE, Sound.BLOCK_FIRE_EXTINGUISH,
            // T1 — Secador Rústico (5 blocks): 3 spruce slabs on 2 spruce fences
            new TierDef(1, "Secador Rústico", Material.SPRUCE_SLAB, Material.SPRUCE_SLAB, 30,
                    new RB[]{
                            new RB(-1, 0, 0, Material.SPRUCE_SLAB), new RB(1, 0, 0, Material.SPRUCE_SLAB),
                            new RB(-1, -1, 0, Material.SPRUCE_FENCE), new RB(1, -1, 0, Material.SPRUCE_FENCE)
                    },
                    new String[]{"&7Dois postes de cerca e três", "&7lajes de pinheiro no topo.", "",
                            "&fBlocos:", "&8▸ &f3x Spruce Slab", "&8▸ &f2x Spruce Fence"}),
            // T2 — Secador Aprimorado (8 blocks): 5 slabs, 2 fences, chain
            new TierDef(2, "Secador Aprimorado", Material.DARK_OAK_SLAB, Material.DARK_OAK_SLAB, 25,
                    new RB[]{
                            new RB(-2, 0, 0, Material.DARK_OAK_SLAB), new RB(-1, 0, 0, Material.DARK_OAK_SLAB),
                            new RB(1, 0, 0, Material.DARK_OAK_SLAB), new RB(2, 0, 0, Material.DARK_OAK_SLAB),
                            new RB(-2, -1, 0, Material.DARK_OAK_FENCE), new RB(2, -1, 0, Material.DARK_OAK_FENCE),
                            new RB(0, -1, 0, Material.CHAIN)
                    },
                    new String[]{"&7Secador largo com corrente", "&7central e cercas de carvalho escuro.", "",
                            "&fBlocos:", "&8▸ &f5x Dark Oak Slab", "&8▸ &f2x Dark Oak Fence", "&8▸ &f1x Chain"}),
            // T3 — Secador de Mestre (12 blocks): grand frame with logs, trapdoors, chains
            new TierDef(3, "Secador de Mestre", Material.MANGROVE_SLAB, Material.MANGROVE_SLAB, 20,
                    new RB[]{
                            new RB(-3, 1, 0, Material.MANGROVE_LOG), new RB(3, 1, 0, Material.MANGROVE_LOG),
                            new RB(-3, 0, 0, Material.MANGROVE_TRAPDOOR), new RB(-2, 0, 0, Material.MANGROVE_SLAB),
                            new RB(-1, 0, 0, Material.MANGROVE_SLAB), new RB(1, 0, 0, Material.MANGROVE_SLAB),
                            new RB(2, 0, 0, Material.MANGROVE_SLAB), new RB(3, 0, 0, Material.MANGROVE_TRAPDOOR),
                            new RB(-1, -1, 0, Material.CHAIN), new RB(1, -1, 0, Material.CHAIN)
                    },
                    new String[]{"&7Grande estrutura com toras de", "&7mangue, alçapões e correntes.", "",
                            "&fBlocos:", "&8▸ &f5x Mangrove Slab", "&8▸ &f2x Mangrove Log",
                            "&8▸ &f2x Mangrove Trapdoor", "&8▸ &f2x Chain"})
    ),

    // =====================================================================
    //  SMOKEHOUSE — Radial structure
    // =====================================================================
    SMOKEHOUSE("defumeiro", FoodTrait.SMOKED, false,
            Particle.CAMPFIRE_COSY_SMOKE, Sound.BLOCK_CAMPFIRE_CRACKLE,
            // T1 — Defumeiro Rústico (10 blocks): campfire + brick walls + slab cap
            new TierDef(1, "Defumeiro Rústico", Material.CAMPFIRE, Material.CAMPFIRE, 20,
                    new RB[]{
                            new RB(1, 0, 0, Material.BRICKS), new RB(-1, 0, 0, Material.BRICKS),
                            new RB(0, 0, 1, Material.BRICKS), new RB(0, 0, -1, Material.BRICKS),
                            new RB(1, 1, 0, Material.BRICK_WALL), new RB(-1, 1, 0, Material.BRICK_WALL),
                            new RB(0, 1, 1, Material.BRICK_WALL), new RB(0, 1, -1, Material.BRICK_WALL),
                            new RB(0, 2, 0, Material.BRICK_SLAB)
                    },
                    new String[]{"&7Câmara de defumação com", "&7fogueira central e paredes de tijolo.", "",
                            "&fBlocos:", "&8▸ &f1x Campfire", "&8▸ &f4x Bricks",
                            "&8▸ &f4x Brick Wall", "&8▸ &f1x Brick Slab"}),
            // T2 — Defumeiro Artesanal (14 blocks): soul campfire + deepslate + 4-slab cap + chimney
            new TierDef(2, "Defumeiro Artesanal", Material.SOUL_CAMPFIRE, Material.SOUL_CAMPFIRE, 16,
                    new RB[]{
                            new RB(1, 0, 0, Material.DEEPSLATE_BRICKS), new RB(-1, 0, 0, Material.DEEPSLATE_BRICKS),
                            new RB(0, 0, 1, Material.DEEPSLATE_BRICKS), new RB(0, 0, -1, Material.DEEPSLATE_BRICKS),
                            new RB(1, 1, 0, Material.DEEPSLATE_BRICK_WALL), new RB(-1, 1, 0, Material.DEEPSLATE_BRICK_WALL),
                            new RB(0, 1, 1, Material.DEEPSLATE_BRICK_WALL), new RB(0, 1, -1, Material.DEEPSLATE_BRICK_WALL),
                            new RB(1, 2, 0, Material.DEEPSLATE_BRICK_SLAB), new RB(-1, 2, 0, Material.DEEPSLATE_BRICK_SLAB),
                            new RB(0, 2, 1, Material.DEEPSLATE_BRICK_SLAB), new RB(0, 2, -1, Material.DEEPSLATE_BRICK_SLAB),
                            new RB(0, 3, 0, Material.IRON_BARS)
                    },
                    new String[]{"&7Defumeiro de ardósia profunda", "&7com chaminé de barras de ferro.", "",
                            "&fBlocos:", "&8▸ &f1x Soul Campfire", "&8▸ &f4x Deepslate Bricks",
                            "&8▸ &f4x Deepslate Brick Wall", "&8▸ &f4x Deepslate Brick Slab",
                            "&8▸ &f1x Iron Bars"}),
            // T3 — Defumeiro de Mestre (18 blocks): tall double-wall + tile roof + chain chimney
            new TierDef(3, "Defumeiro de Mestre", Material.SOUL_CAMPFIRE, Material.SOUL_CAMPFIRE, 12,
                    new RB[]{
                            new RB(1, 0, 0, Material.POLISHED_DEEPSLATE), new RB(-1, 0, 0, Material.POLISHED_DEEPSLATE),
                            new RB(0, 0, 1, Material.POLISHED_DEEPSLATE), new RB(0, 0, -1, Material.POLISHED_DEEPSLATE),
                            new RB(1, 1, 0, Material.DEEPSLATE_BRICK_WALL), new RB(-1, 1, 0, Material.DEEPSLATE_BRICK_WALL),
                            new RB(0, 1, 1, Material.DEEPSLATE_BRICK_WALL), new RB(0, 1, -1, Material.DEEPSLATE_BRICK_WALL),
                            new RB(1, 2, 0, Material.DEEPSLATE_BRICK_WALL), new RB(-1, 2, 0, Material.DEEPSLATE_BRICK_WALL),
                            new RB(0, 2, 1, Material.DEEPSLATE_BRICK_WALL), new RB(0, 2, -1, Material.DEEPSLATE_BRICK_WALL),
                            new RB(1, 3, 0, Material.DEEPSLATE_TILE_SLAB), new RB(-1, 3, 0, Material.DEEPSLATE_TILE_SLAB),
                            new RB(0, 3, 1, Material.DEEPSLATE_TILE_SLAB), new RB(0, 3, -1, Material.DEEPSLATE_TILE_SLAB),
                            new RB(0, 4, 0, Material.CHAIN)
                    },
                    new String[]{"&7Grande defumeiro com dupla", "&7camada de paredes e telhado ornamentado.", "",
                            "&fBlocos:", "&8▸ &f1x Soul Campfire", "&8▸ &f4x Polished Deepslate",
                            "&8▸ &f8x Deepslate Brick Wall", "&8▸ &f4x Deepslate Tile Slab",
                            "&8▸ &f1x Chain"})
    ),

    // =====================================================================
    //  SALT BARREL — Radial structure
    // =====================================================================
    SALT_BARREL("barril-de-sal", FoodTrait.SALTED, false,
            Particle.END_ROD, Sound.BLOCK_BREWING_STAND_BREW,
            // T1 — Barril de Sal Rústico (6 blocks): barrel + sand ring + trapdoor
            new TierDef(1, "Barril de Sal Rústico", Material.BARREL, Material.BARREL, 15,
                    new RB[]{
                            new RB(1, 0, 0, Material.SAND), new RB(-1, 0, 0, Material.SAND),
                            new RB(0, 0, 1, Material.SAND), new RB(0, 0, -1, Material.SAND),
                            new RB(0, 1, 0, Material.SPRUCE_TRAPDOOR)
                    },
                    new String[]{"&7Barril cercado de areia", "&7com tampa de alçapão.", "",
                            "&fBlocos:", "&8▸ &f1x Barrel", "&8▸ &f4x Sand",
                            "&8▸ &f1x Spruce Trapdoor"}),
            // T2 — Barril de Sal Artesanal (10 blocks): soul sand + dark oak fence frame + trapdoor
            new TierDef(2, "Barril de Sal Artesanal", Material.BARREL, Material.BARREL, 12,
                    new RB[]{
                            new RB(1, 0, 0, Material.SOUL_SAND), new RB(-1, 0, 0, Material.SOUL_SAND),
                            new RB(0, 0, 1, Material.SOUL_SAND), new RB(0, 0, -1, Material.SOUL_SAND),
                            new RB(1, 1, 0, Material.DARK_OAK_FENCE), new RB(-1, 1, 0, Material.DARK_OAK_FENCE),
                            new RB(0, 1, 1, Material.DARK_OAK_FENCE), new RB(0, 1, -1, Material.DARK_OAK_FENCE),
                            new RB(0, 2, 0, Material.DARK_OAK_TRAPDOOR)
                    },
                    new String[]{"&7Barril com moldura de carvalho", "&7escuro e areia das almas.", "",
                            "&fBlocos:", "&8▸ &f1x Barrel", "&8▸ &f4x Soul Sand",
                            "&8▸ &f4x Dark Oak Fence", "&8▸ &f1x Dark Oak Trapdoor"}),
            // T3 — Barril de Sal de Mestre (12 blocks): foundation + soul sand + mangue frame + chain
            new TierDef(3, "Barril de Sal de Mestre", Material.BARREL, Material.BARREL, 9,
                    new RB[]{
                            new RB(0, -1, 0, Material.DEEPSLATE_BRICK_SLAB),
                            new RB(1, 0, 0, Material.SOUL_SAND), new RB(-1, 0, 0, Material.SOUL_SAND),
                            new RB(0, 0, 1, Material.SOUL_SAND), new RB(0, 0, -1, Material.SOUL_SAND),
                            new RB(1, 1, 0, Material.MANGROVE_FENCE), new RB(-1, 1, 0, Material.MANGROVE_FENCE),
                            new RB(0, 1, 1, Material.MANGROVE_FENCE), new RB(0, 1, -1, Material.MANGROVE_FENCE),
                            new RB(0, 2, 0, Material.MANGROVE_TRAPDOOR),
                            new RB(0, 3, 0, Material.CHAIN)
                    },
                    new String[]{"&7Barril ornamentado de mangue", "&7sobre base de ardósia com corrente.", "",
                            "&fBlocos:", "&8▸ &f1x Barrel", "&8▸ &f1x Deepslate Brick Slab",
                            "&8▸ &f4x Soul Sand", "&8▸ &f4x Mangrove Fence",
                            "&8▸ &f1x Mangrove Trapdoor", "&8▸ &f1x Chain"})
    ),

    // =====================================================================
    //  PICKLING CAULDRON — Radial structure
    // =====================================================================
    PICKLING_CAULDRON("tina-de-conserva", FoodTrait.PICKLED, false,
            Particle.SPLASH, Sound.ENTITY_GENERIC_SPLASH,
            // T1 — Tina Rústica (6 blocks): cauldron + campfire below + spruce logs
            new TierDef(1, "Tina de Conserva Rústica", Material.CAULDRON, Material.CAULDRON, 10,
                    new RB[]{
                            new RB(0, -1, 0, Material.CAMPFIRE),
                            new RB(1, 0, 0, Material.STRIPPED_SPRUCE_LOG), new RB(-1, 0, 0, Material.STRIPPED_SPRUCE_LOG),
                            new RB(0, 0, 1, Material.STRIPPED_SPRUCE_LOG), new RB(0, 0, -1, Material.STRIPPED_SPRUCE_LOG)
                    },
                    new String[]{"&7Caldeirão sobre fogueira", "&7com moldura de toras de pinheiro.", "",
                            "&fBlocos:", "&8▸ &f1x Cauldron", "&8▸ &f1x Campfire",
                            "&8▸ &f4x Stripped Spruce Log"}),
            // T2 — Tina Artesanal (10 blocks): soul campfire + dark oak logs + trapdoor rim
            new TierDef(2, "Tina de Conserva Artesanal", Material.CAULDRON, Material.CAULDRON, 8,
                    new RB[]{
                            new RB(0, -1, 0, Material.SOUL_CAMPFIRE),
                            new RB(1, 0, 0, Material.STRIPPED_DARK_OAK_LOG), new RB(-1, 0, 0, Material.STRIPPED_DARK_OAK_LOG),
                            new RB(0, 0, 1, Material.STRIPPED_DARK_OAK_LOG), new RB(0, 0, -1, Material.STRIPPED_DARK_OAK_LOG),
                            new RB(1, 1, 0, Material.DARK_OAK_TRAPDOOR), new RB(-1, 1, 0, Material.DARK_OAK_TRAPDOOR),
                            new RB(0, 1, 1, Material.DARK_OAK_TRAPDOOR), new RB(0, 1, -1, Material.DARK_OAK_TRAPDOOR)
                    },
                    new String[]{"&7Tina com fogueira das almas", "&7e borda de alçapões de carvalho escuro.", "",
                            "&fBlocos:", "&8▸ &f1x Cauldron", "&8▸ &f1x Soul Campfire",
                            "&8▸ &f4x Stripped Dark Oak Log", "&8▸ &f4x Dark Oak Trapdoor"}),
            // T3 — Tina de Mestre (11 blocks): deepslate brick base + soul campfire + mangue logs + trapdoor lid
            new TierDef(3, "Tina de Conserva de Mestre", Material.CAULDRON, Material.CAULDRON, 6,
                    new RB[]{
                            new RB(1, -1, 0, Material.DEEPSLATE_BRICKS), new RB(-1, -1, 0, Material.DEEPSLATE_BRICKS),
                            new RB(0, -1, 1, Material.DEEPSLATE_BRICKS), new RB(0, -1, -1, Material.DEEPSLATE_BRICKS),
                            new RB(0, -1, 0, Material.SOUL_CAMPFIRE),
                            new RB(1, 0, 0, Material.STRIPPED_MANGROVE_LOG), new RB(-1, 0, 0, Material.STRIPPED_MANGROVE_LOG),
                            new RB(0, 0, 1, Material.STRIPPED_MANGROVE_LOG), new RB(0, 0, -1, Material.STRIPPED_MANGROVE_LOG),
                            new RB(0, 1, 0, Material.MANGROVE_TRAPDOOR)
                    },
                    new String[]{"&7Tina refinada com base de ardósia,", "&7toras de mangue e tampa ornamentada.", "",
                            "&fBlocos:", "&8▸ &f1x Cauldron", "&8▸ &f4x Deepslate Bricks",
                            "&8▸ &f1x Soul Campfire", "&8▸ &f4x Stripped Mangrove Log",
                            "&8▸ &f1x Mangrove Trapdoor"})
    ),

    // =====================================================================
    //  SEALING PRESS — Radial structure
    // =====================================================================
    SEALING_PRESS("prensa-de-selagem", FoodTrait.PRESERVED, false,
            Particle.CRIT, Sound.BLOCK_ANVIL_USE,
            // T1 — Prensa Rústica (6 blocks): cobblestone cross + piston
            new TierDef(1, "Prensa Rústica", Material.COBBLESTONE, Material.PISTON, 15,
                    new RB[]{
                            new RB(1, 0, 0, Material.COBBLESTONE), new RB(-1, 0, 0, Material.COBBLESTONE),
                            new RB(0, 0, 1, Material.COBBLESTONE), new RB(0, 0, -1, Material.COBBLESTONE),
                            new RB(0, 2, 0, Material.PISTON)
                    },
                    new String[]{"&7Base de pedregulho com", "&7pistão de prensagem no topo.", "",
                            "&fBlocos:", "&8▸ &f5x Cobblestone", "&8▸ &f1x Piston"}),
            // T2 — Prensa Artesanal (10 blocks): smooth stone + wall guides + sticky piston
            new TierDef(2, "Prensa Artesanal", Material.SMOOTH_STONE, Material.PISTON, 12,
                    new RB[]{
                            new RB(1, 0, 0, Material.SMOOTH_STONE), new RB(-1, 0, 0, Material.SMOOTH_STONE),
                            new RB(0, 0, 1, Material.SMOOTH_STONE), new RB(0, 0, -1, Material.SMOOTH_STONE),
                            new RB(1, 1, 0, Material.STONE_BRICK_WALL), new RB(-1, 1, 0, Material.STONE_BRICK_WALL),
                            new RB(0, 1, 1, Material.STONE_BRICK_WALL), new RB(0, 1, -1, Material.STONE_BRICK_WALL),
                            new RB(0, 2, 0, Material.STICKY_PISTON)
                    },
                    new String[]{"&7Prensa com guias de tijolos de", "&7pedra e pistão aderente.", "",
                            "&fBlocos:", "&8▸ &f5x Smooth Stone", "&8▸ &f4x Stone Brick Wall",
                            "&8▸ &f1x Sticky Piston"}),
            // T3 — Prensa de Mestre (14 blocks): polished deepslate + wall guides + slab frame + sticky piston
            new TierDef(3, "Prensa de Mestre", Material.POLISHED_DEEPSLATE, Material.PISTON, 9,
                    new RB[]{
                            new RB(1, 0, 0, Material.POLISHED_DEEPSLATE), new RB(-1, 0, 0, Material.POLISHED_DEEPSLATE),
                            new RB(0, 0, 1, Material.POLISHED_DEEPSLATE), new RB(0, 0, -1, Material.POLISHED_DEEPSLATE),
                            new RB(1, 1, 0, Material.DEEPSLATE_BRICK_WALL), new RB(-1, 1, 0, Material.DEEPSLATE_BRICK_WALL),
                            new RB(0, 1, 1, Material.DEEPSLATE_BRICK_WALL), new RB(0, 1, -1, Material.DEEPSLATE_BRICK_WALL),
                            new RB(1, 2, 0, Material.DEEPSLATE_BRICK_SLAB), new RB(-1, 2, 0, Material.DEEPSLATE_BRICK_SLAB),
                            new RB(0, 2, 1, Material.DEEPSLATE_BRICK_SLAB), new RB(0, 2, -1, Material.DEEPSLATE_BRICK_SLAB),
                            new RB(0, 3, 0, Material.STICKY_PISTON)
                    },
                    new String[]{"&7Prensa imperial de ardósia polida", "&7com moldura e pistão aderente.", "",
                            "&fBlocos:", "&8▸ &f5x Polished Deepslate", "&8▸ &f4x Deepslate Brick Wall",
                            "&8▸ &f4x Deepslate Brick Slab", "&8▸ &f1x Sticky Piston"})
    );

    // =====================================================================
    //  Tier definition record
    // =====================================================================

    /** Per-tier data: pattern, materials, display info, and processing time. */
    public record TierDef(int tier, String displayName, Material anchor, Material icon,
                          int processingMinutes, RB[] pattern, String[] description) {}

    // =====================================================================
    //  Fields (shared across tiers)
    // =====================================================================

    private final String configKey;
    private final FoodTrait resultTrait;
    private final boolean linear;
    private final Particle processParticle;
    private final Sound completeSound;
    private final List<TierDef> tiers;
    private final Map<Integer, List<List<RB>>> tierRotations;

    MultiblockType(String configKey, FoodTrait resultTrait, boolean linear,
                   Particle processParticle, Sound completeSound,
                   TierDef... tierDefs) {
        this.configKey = configKey;
        this.resultTrait = resultTrait;
        this.linear = linear;
        this.processParticle = processParticle;
        this.completeSound = completeSound;
        this.tiers = List.of(tierDefs);

        Map<Integer, List<List<RB>>> rotMap = new HashMap<>();
        for (TierDef td : tierDefs) {
            List<RB> base = List.of(td.pattern());
            rotMap.put(td.tier(), buildRotations(base, linear));
        }
        this.tierRotations = Map.copyOf(rotMap);
    }

    private static List<List<RB>> buildRotations(List<RB> basePattern, boolean linear) {
        if (!linear) {
            return List.of(basePattern);
        }
        List<RB> rotated = basePattern.stream()
                .map(rb -> new RB(-rb.z, rb.y, rb.x, rb.material))
                .toList();
        return List.of(basePattern, rotated);
    }

    // =====================================================================
    //  Tier lookup
    // =====================================================================

    /** Returns the TierDef for the given tier, falling back to tier 1. */
    public TierDef getTier(int tier) {
        for (TierDef td : tiers) {
            if (td.tier() == tier) return td;
        }
        return tiers.getFirst();
    }

    /** Returns all tier definitions for this machine. */
    public List<TierDef> getTiers() { return tiers; }

    /** Maximum tier number available. */
    public int getMaxTier() { return tiers.getLast().tier(); }

    // =====================================================================
    //  Tier-aware getters
    // =====================================================================

    public String getDisplayName(int tier)           { return getTier(tier).displayName(); }
    public Material getAnchorMaterial(int tier)      { return getTier(tier).anchor(); }
    public Material getIcon(int tier)                { return getTier(tier).icon(); }
    public int getDefaultProcessingMinutes(int tier) { return getTier(tier).processingMinutes(); }
    public List<String> getDescription(int tier)     { return List.of(getTier(tier).description()); }
    public List<RB> getBasePattern(int tier)         { return tierRotations.get(getTier(tier).tier()).getFirst(); }
    public List<List<RB>> getRotations(int tier)     { return tierRotations.getOrDefault(tier, tierRotations.get(1)); }

    public int getMinY(int tier) {
        int min = 0;
        for (RB rb : getBasePattern(tier)) {
            if (rb.y() < min) min = rb.y();
        }
        return min;
    }

    public int getTotalBlocks(int tier) { return getBasePattern(tier).size() + 1; }

    // =====================================================================
    //  Backward-compatible getters (default to tier 1)
    // =====================================================================

    public String getDisplayName()           { return getDisplayName(1); }
    public Material getAnchorMaterial()      { return getAnchorMaterial(1); }
    public Material getIcon()                { return getIcon(1); }
    public int getDefaultProcessingMinutes() { return getDefaultProcessingMinutes(1); }
    public List<String> getDescription()     { return getDescription(1); }
    public List<RB> getBasePattern()         { return getBasePattern(1); }
    public List<List<RB>> getRotations()     { return getRotations(1); }
    public int getMinY()                     { return getMinY(1); }
    public int getTotalBlocks()              { return getTotalBlocks(1); }

    // =====================================================================
    //  Shared (tier-independent) getters
    // =====================================================================

    public String getConfigKey()        { return configKey; }
    public FoodTrait getResultTrait()   { return resultTrait; }
    public boolean isLinear()           { return linear; }
    public Particle getProcessParticle() { return processParticle; }
    public Sound getCompleteSound()     { return completeSound; }

    /** Checks if a material is used in any tier's pattern. */
    public boolean usesPatternMaterial(Material mat) {
        for (TierDef td : tiers) {
            for (RB rb : td.pattern()) {
                if (rb.material == mat) return true;
            }
        }
        return false;
    }

    /** A relative block offset in the multiblock pattern. */
    public record RB(int x, int y, int z, Material material) {}

    /** Find by config key or enum name (case-insensitive). */
    public static MultiblockType fromKey(String key) {
        for (MultiblockType type : values()) {
            if (type.configKey.equalsIgnoreCase(key) || type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
