package me.ray.midgard.core.attribute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttributeInstanceTest {

    @Test
    void testAttributeCalculationLogic() {
        // 1. Setup: Um atributo de Força com base 100, min 0, max 1000
        // Construtor: id, name, baseValue, minValue, maxValue
        Attribute strength = new Attribute("strength", "Força", 100, 0, 1000);
        AttributeInstance instance = new AttributeInstance(strength);

        // Verifica base inicial
        assertEquals(100.0, instance.getValue(), "Valor inicial deve ser a base");

        // 2. Cenário: Jogador equipa um capacete (+10 Força)
        // Operation: ADD_NUMBER
        instance.addModifier(new AttributeModifier("Helmet", 10.0, AttributeOperation.ADD_NUMBER));
        
        // Esperado: 100 + 10 = 110
        assertEquals(110.0, instance.getValue(), 0.001, "Adição simples falhou");

        // 3. Cenário: Jogador ativa um Buff (+50% Força)
        // Operation: MULTIPLY_PERCENTAGE_ADDITIVE (ex: 0.5)
        // A lógica do seu código é: value * (1 + additive)
        // (100 + 10) * (1 + 0.5) = 110 * 1.5 = 165
        instance.addModifier(new AttributeModifier("Buff", 0.5, AttributeOperation.MULTIPLY_PERCENTAGE_ADDITIVE));

        assertEquals(165.0, instance.getValue(), 0.001, "Multiplicador percentual falhou");

        // 4. Cenário: Outro item de +20% (deve somar ao buff anterior, não multiplicar o total)
        // (100 + 10) * (1 + 0.5 + 0.2) = 110 * 1.7 = 187
        instance.addModifier(new AttributeModifier("Ring", 0.2, AttributeOperation.MULTIPLY_PERCENTAGE_ADDITIVE));

        assertEquals(187.0, instance.getValue(), 0.001, "Acúmulo de percentuais (stacking) falhou");

        // 5. Cenário: Um debuff global ou multiplicador final (x0.5 - metade do dano)
        // Operation: MULTIPLY_SCALAR
        // 187 * 0.5 = 93.5
        instance.addModifier(new AttributeModifier("Cursed", 0.5, AttributeOperation.MULTIPLY_SCALAR));

        assertEquals(93.5, instance.getValue(), 0.001, "Multiplicador scalar final falhou");
    }

    @Test
    void testMinMaxLimits() {
        // Teste para garantir que o atributo nunca ultrapasse os limites configurados
        // Base 50, Min 0, Max 100
        Attribute health = new Attribute("hp", "Vida", 50, 0, 100); 
        AttributeInstance instance = new AttributeInstance(health);

        // Tenta adicionar +1000 de vida
        instance.addModifier(new AttributeModifier("GodMode", 1000.0, AttributeOperation.ADD_NUMBER));

        // Deve travar em 100 (Max)
        assertEquals(100.0, instance.getValue(), "O atributo deveria respeitar o valor máximo");
        
        // Tenta reduzir para -50
        instance.setBaseValue(-50);
        // Remove modifiers to test pure base
        instance.removeModifier("GodMode");
        
        // Deve travar em 0 (Min)
        assertEquals(0.0, instance.getValue(), "O atributo deveria respeitar o valor mínimo");
    }
}
