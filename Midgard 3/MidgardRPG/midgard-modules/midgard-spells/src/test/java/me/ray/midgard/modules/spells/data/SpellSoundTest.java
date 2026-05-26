package me.ray.midgard.modules.spells.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class SpellSoundTest {

    @Test
    @DisplayName("Deve armazenar campos do record corretamente")
    void shouldStoreRecordFields() {
        var sound = new SpellSound("ENTITY_BLAZE_HURT", 0.8f, 1.2f);
        assertEquals("ENTITY_BLAZE_HURT", sound.sound());
        assertEquals(0.8f, sound.volume(), 0.001f);
        assertEquals(1.2f, sound.pitch(), 0.001f);
    }

    @Test
    @DisplayName("DEFAULT_CAST_START deve ter valores corretos")
    void shouldHaveCorrectDefaultCastStart() {
        var d = SpellSound.DEFAULT_CAST_START;
        assertEquals("ENTITY_ENDER_DRAGON_FLAP", d.sound());
        assertEquals(0.5f, d.volume(), 0.001f);
        assertEquals(1.5f, d.pitch(), 0.001f);
    }

    @Test
    @DisplayName("DEFAULT_CAST_FINISH deve ter valores corretos")
    void shouldHaveCorrectDefaultCastFinish() {
        var d = SpellSound.DEFAULT_CAST_FINISH;
        assertEquals("ENTITY_PLAYER_LEVELUP", d.sound());
        assertEquals(0.5f, d.volume(), 0.001f);
        assertEquals(2.0f, d.pitch(), 0.001f);
    }

    @Test
    @DisplayName("DEFAULT_CAST_FAIL deve ter valores corretos")
    void shouldHaveCorrectDefaultCastFail() {
        var d = SpellSound.DEFAULT_CAST_FAIL;
        assertEquals("BLOCK_NOTE_BLOCK_BASS", d.sound());
        assertEquals(1.0f, d.volume(), 0.001f);
        assertEquals(0.5f, d.pitch(), 0.001f);
    }

    @Test
    @DisplayName("Igualdade de records com mesmos valores")
    void shouldBeEqual_forSameValues() {
        var a = new SpellSound("TEST", 1.0f, 1.0f);
        var b = new SpellSound("TEST", 1.0f, 1.0f);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("Records diferentes não são iguais")
    void shouldNotBeEqual_forDifferentValues() {
        var a = new SpellSound("A", 1.0f, 1.0f);
        var b = new SpellSound("B", 1.0f, 1.0f);
        assertNotEquals(a, b);
    }
}
