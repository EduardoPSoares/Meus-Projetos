package me.ray.midgard.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultFontInfoTest {

    @Test
    void shouldResolveKnownCharacters() {
        assertEquals(DefaultFontInfo.A, DefaultFontInfo.getDefaultFontInfo('A'));
        assertEquals(DefaultFontInfo.a, DefaultFontInfo.getDefaultFontInfo('a'));
        assertEquals(DefaultFontInfo.SPACE, DefaultFontInfo.getDefaultFontInfo(' '));
    }

    @Test
    void shouldReturnDefaultForUnknownCharacter() {
        DefaultFontInfo info = DefaultFontInfo.getDefaultFontInfo('★');
        assertEquals(DefaultFontInfo.DEFAULT, info);
    }

    @Test
    void shouldHaveCorrectLength() {
        assertEquals(5, DefaultFontInfo.A.getLength());
        assertEquals(1, DefaultFontInfo.i.getLength());
        assertEquals(3, DefaultFontInfo.SPACE.getLength());
        assertEquals(1, DefaultFontInfo.l.getLength());
        assertEquals(4, DefaultFontInfo.f.getLength());
    }

    @Test
    void shouldGetBoldLength() {
        // Bold adds 1 pixel to non-space characters
        assertEquals(6, DefaultFontInfo.A.getBoldLength());
        assertEquals(2, DefaultFontInfo.i.getBoldLength());
    }

    @Test
    void shouldNotAddBoldToSpace() {
        // SPACE is special case: bold doesn't change width
        assertEquals(3, DefaultFontInfo.SPACE.getBoldLength());
        assertEquals(DefaultFontInfo.SPACE.getLength(), DefaultFontInfo.SPACE.getBoldLength());
    }

    @Test
    void shouldHaveCorrectCharacters() {
        assertEquals('A', DefaultFontInfo.A.getCharacter());
        assertEquals(' ', DefaultFontInfo.SPACE.getCharacter());
        assertEquals('.', DefaultFontInfo.PERIOD.getCharacter());
    }

    @Test
    void shouldResolvePunctuation() {
        assertEquals(DefaultFontInfo.PERIOD, DefaultFontInfo.getDefaultFontInfo('.'));
        assertEquals(DefaultFontInfo.COMMA, DefaultFontInfo.getDefaultFontInfo(','));
        assertEquals(DefaultFontInfo.EXCLAMATION_POINT, DefaultFontInfo.getDefaultFontInfo('!'));
        assertEquals(DefaultFontInfo.QUESTION_MARK, DefaultFontInfo.getDefaultFontInfo('?'));
    }

    @Test
    void shouldResolveDigits() {
        assertEquals(DefaultFontInfo.NUM_0, DefaultFontInfo.getDefaultFontInfo('0'));
        assertEquals(DefaultFontInfo.NUM_9, DefaultFontInfo.getDefaultFontInfo('9'));
    }

    @Test
    void shouldResolveSpecialChars() {
        assertEquals(DefaultFontInfo.AT_SYMBOL, DefaultFontInfo.getDefaultFontInfo('@'));
        assertEquals(DefaultFontInfo.LEFT_BRACKET, DefaultFontInfo.getDefaultFontInfo('['));
        assertEquals(DefaultFontInfo.RIGHT_BRACKET, DefaultFontInfo.getDefaultFontInfo(']'));
    }
}
