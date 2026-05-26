package me.ray.midgard.modules.item.ability;

/**
 * Tipos de trigger para ativar abilities de itens.
 * Compatível com MMOItems/MMOCore.
 */
public enum AbilityTrigger {
    
    /** Clique esquerdo (ataque) */
    LEFT_CLICK,
    
    /** Clique direito (usar) */
    RIGHT_CLICK,
    
    /** Shift + Clique esquerdo */
    SHIFT_LEFT_CLICK,
    
    /** Shift + Clique direito */
    SHIFT_RIGHT_CLICK,
    
    /** Agachar (toggle) */
    SNEAK,
    
    /** Ao equipar o item */
    ON_EQUIP,
    
    /** Ao desequipar o item */
    ON_UNEQUIP,
    
    /** Timer passivo (executa a cada X ticks) */
    PASSIVE_TIMER,
    
    /** Ao receber dano */
    ON_DAMAGE_TAKEN,
    
    /** Ao causar dano */
    ON_DAMAGE_DEALT,
    
    /** Ao matar uma entidade */
    ON_KILL,
    
    /** Desconhecido/fallback */
    UNKNOWN;

    /**
     * Converte uma string para AbilityTrigger.
     * Suporta formatos do MMOItems e MMOCore.
     */
    public static AbilityTrigger fromString(String str) {
        if (str == null || str.isEmpty()) {
            return LEFT_CLICK;
        }
        
        String upper = str.toUpperCase().replace("-", "_").replace(" ", "_");
        
        return switch (upper) {
            case "LEFT_CLICK", "LEFTCLICK", "SWING", "ATTACK" -> LEFT_CLICK;
            case "RIGHT_CLICK", "RIGHTCLICK", "USE", "INTERACT" -> RIGHT_CLICK;
            case "SHIFT_LEFT_CLICK", "SHIFTLEFTCLICK", "SNEAK_LEFT_CLICK" -> SHIFT_LEFT_CLICK;
            case "SHIFT_RIGHT_CLICK", "SHIFTRIGHTCLICK", "SNEAK_RIGHT_CLICK" -> SHIFT_RIGHT_CLICK;
            case "SNEAK", "CROUCH", "SHIFT" -> SNEAK;
            case "ON_EQUIP", "EQUIP" -> ON_EQUIP;
            case "ON_UNEQUIP", "UNEQUIP" -> ON_UNEQUIP;
            case "TIMER", "PASSIVE_TIMER", "PASSIVE" -> PASSIVE_TIMER;
            case "ON_DAMAGE_TAKEN", "WHEN_HIT", "DAMAGED" -> ON_DAMAGE_TAKEN;
            case "ON_DAMAGE_DEALT", "WHEN_ATTACK", "HIT" -> ON_DAMAGE_DEALT;
            case "ON_KILL", "KILL" -> ON_KILL;
            default -> {
                try {
                    yield AbilityTrigger.valueOf(upper);
                } catch (IllegalArgumentException e) {
                    yield UNKNOWN;
                }
            }
        };
    }

    /**
     * Verifica se este trigger requer interação do jogador.
     */
    public boolean isInteractionTrigger() {
        return this == LEFT_CLICK || this == RIGHT_CLICK || 
               this == SHIFT_LEFT_CLICK || this == SHIFT_RIGHT_CLICK;
    }

    /**
     * Verifica se este trigger é baseado em combate.
     */
    public boolean isCombatTrigger() {
        return this == ON_DAMAGE_TAKEN || this == ON_DAMAGE_DEALT || this == ON_KILL;
    }
}
