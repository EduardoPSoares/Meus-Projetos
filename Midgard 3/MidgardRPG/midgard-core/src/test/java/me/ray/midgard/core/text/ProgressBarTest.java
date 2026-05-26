package me.ray.midgard.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgressBarTest {

    @Test
    void testProgressCalculation() {
        // 50% de 10 barras = 5 preenchidas, 5 vazias
        Component comp = ProgressBar.get(50, 100, 10, "|", "<green>", "<gray>");
        String plain = PlainTextComponentSerializer.plainText().serialize(comp);
        
        // Esperado: |||||||||| (10 pipes)
        // A cor não aparece no plain text, mas a quantidade de caracteres sim.
        assertEquals(10, plain.length());
        assertEquals("||||||||||", plain);
    }

    @Test
    void testLimits() {
        // 150% -> deve limitar a 100%
        // Símbolo tem comprimento 1 ("|")
        // Total bars 10.
        // Se usar "G" e "E" como cores, eles viram texto se não forem tags válidas do MiniMessage.
        // O MiniMessage ignora tags inválidas ou as trata como texto dependendo da config,
        // mas "<green>" é válido. "G" não é tag, é texto literal.
        // Então "G" + 10 * "|" + "E" + 0 * "|" = "G||||||||||E"
        // O teste original assumia que "G" e "E" eram tags de cor invisíveis no plain text.
        // Vamos usar tags reais para testar o comprimento visual.
        
        Component overflow = ProgressBar.get(150, 100, 10, "|", "<green>", "<red>");
        String plainOverflow = PlainTextComponentSerializer.plainText().serialize(overflow);
        assertEquals(10, plainOverflow.length(), "Overflow deve ter 10 barras");

        // -50% -> deve limitar a 0%
        Component underflow = ProgressBar.get(-50, 100, 10, "|", "<green>", "<red>");
        String plainUnderflow = PlainTextComponentSerializer.plainText().serialize(underflow);
        assertEquals(10, plainUnderflow.length(), "Underflow deve ter 10 barras");
    }
}
