package me.ray.midgard.modules.spells.data;

public record SpellSound(String sound, float volume, float pitch) {

    public static final SpellSound DEFAULT_CAST_START = new SpellSound("ENTITY_ENDER_DRAGON_FLAP", 0.5f, 1.5f);
    public static final SpellSound DEFAULT_CAST_FINISH = new SpellSound("ENTITY_PLAYER_LEVELUP", 0.5f, 2.0f);
    public static final SpellSound DEFAULT_CAST_FAIL = new SpellSound("BLOCK_NOTE_BLOCK_BASS", 1.0f, 0.5f);
}
