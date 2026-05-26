package me.ray.midgard.core;

/**
 * Define a prioridade de carregamento dos módulos.
 * Módulos com prioridade maior são carregados primeiro.
 */
public enum ModulePriority {
    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4);

    private final int value;

    ModulePriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
