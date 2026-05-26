package me.ray.midgard.modules.spells.obj;

/**
 * Tipo de habilidade no sistema de spells.
 * 
 * <ul>
 *   <li>{@link #PASSIVE} - Habilidade passiva, sempre ativa. Não pode ser conjurada manualmente.</li>
 *   <li>{@link #COMMON} - Habilidade comum, conjurada ativamente pelo jogador.</li>
 *   <li>{@link #ULTIMATE} - Habilidade suprema, poderosa com cooldown alto.</li>
 * </ul>
 */
public enum SpellType {
    
    /**
     * Habilidade passiva - sempre ativa, não pode ser conjurada manualmente.
     * Aplicada automaticamente quando a classe é selecionada.
     */
    PASSIVE,
    
    /**
     * Habilidade comum - conjurada ativamente pelo jogador.
     * Forma o núcleo do kit de combate da classe.
     */
    COMMON,
    
    /**
     * Habilidade suprema - a habilidade mais poderosa da classe.
     * Geralmente possui cooldown alto e efeitos devastadores.
     */
    ULTIMATE;

    /**
     * Converte uma string para SpellType, com fallback para COMMON.
     * 
     * @param value Nome do tipo (case-insensitive)
     * @return O SpellType correspondente, ou COMMON se inválido
     */
    public static SpellType fromString(String value) {
        if (value == null || value.isEmpty()) { return COMMON; }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}
