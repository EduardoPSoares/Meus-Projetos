package me.ray.midgard.core.gui;

import org.bukkit.Material;

/**
 * Enum para estados visuais padronizados em GUIs.
 * Facilita a criação de indicadores de estado consistentes.
 */
public enum VisualState {
    
    /**
     * Estado Disponível/Ativo - Verde
     */
    AVAILABLE(Material.LIME_DYE, "§a"),
    
    /**
     * Estado Selecionado/Em Uso - Amarelo
     */
    SELECTED(Material.YELLOW_DYE, "§e"),
    
    /**
     * Estado Bloqueado/Desabilitado - Cinza
     */
    LOCKED(Material.GRAY_DYE, "§7"),
    
    /**
     * Estado de Erro/Inválido - Vermelho
     */
    ERROR(Material.RED_DYE, "§c"),
    
    /**
     * Estado Informativo - Azul Claro
     */
    INFO(Material.LIGHT_BLUE_DYE, "§b"),
    
    /**
     * Estado Especial/Raro - Roxo
     */
    SPECIAL(Material.PURPLE_DYE, "§d"),
    
    /**
     * Estado de Sucesso - Verde Escuro
     */
    SUCCESS(Material.GREEN_DYE, "§2"),
    
    /**
     * Estado de Aviso - Laranja
     */
    WARNING(Material.ORANGE_DYE, "§6");
    
    private final Material material;
    private final String colorCode;
    
    VisualState(Material material, String colorCode) {
        this.material = material;
        this.colorCode = colorCode;
    }
    
    /**
     * Obtém o material associado a este estado visual.
     * @return Material do estado
     */
    public Material getMaterial() {
        return material;
    }
    
    /**
     * Obtém o código de cor associado a este estado.
     * @return Código de cor (ex: §a para verde)
     */
    public String getColorCode() {
        return colorCode;
    }
    
    /**
     * Formata um texto com a cor deste estado.
     * @param text Texto a ser formatado
     * @return Texto com código de cor aplicado
     */
    public String format(String text) {
        return colorCode + text;
    }
}
