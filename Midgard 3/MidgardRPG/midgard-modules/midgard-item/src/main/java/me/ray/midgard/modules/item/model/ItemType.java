package me.ray.midgard.modules.item.model;

public enum ItemType {
    SWORD,
    AXE,
    BOW,
    STAFF,
    WAND,
    ARMOR,
    CONSUMABLE,
    MATERIAL,
    GEM_STONE,
    MISC;

    public static ItemType fromString(String type) {
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MISC;
        }
    }
}
