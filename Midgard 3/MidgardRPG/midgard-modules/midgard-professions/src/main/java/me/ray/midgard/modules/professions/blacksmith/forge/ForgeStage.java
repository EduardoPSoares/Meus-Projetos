package me.ray.midgard.modules.professions.blacksmith.forge;

import me.ray.midgard.modules.professions.ProfessionsModule;

/**
 * Represents the current stage of a forge session.
 */
public enum ForgeStage {

    SELECTING("Selecionando Receita"),
    PREPARING("Preparando Materiais"),
    HEATING("Aquecendo Metal"),
    HAMMERING("Martelando"),
    QUENCHING("Temperando"),
    SHARPENING("Afiando"),
    FINALIZING("Finalizando"),
    COMPLETED("Concluído"),
    FAILED("Falhou"),
    EXPIRED("Expirado");

    private final String displayName;

    ForgeStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        var m = ProfessionsModule.getInstance();
        return m != null ? m.getMessage("forge_stage." + name().toLowerCase()) : displayName;
    }

    public boolean isActive() {
        return this != COMPLETED && this != FAILED && this != EXPIRED;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == EXPIRED;
    }
}
