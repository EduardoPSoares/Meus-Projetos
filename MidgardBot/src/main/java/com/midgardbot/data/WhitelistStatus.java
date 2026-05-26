package com.midgardbot.data;

public enum WhitelistStatus {
    PENDING("Em Análise", "⏳"),
    REVIEWING("Em Revisão", "👀"),
    NEEDS_REVIEW("Precisa Revisão", "❓"),
    FLAGGED("Sinalizada", "⚠️"),
    PRIORITY("Prioritária", "🔥"),
    APPROVED("Aprovado", "✅"),
    REJECTED("Reprovado", "❌"),
    EXCELLENT("Excepcional", "⭐"),
    STANDBY("Em Espera", "⏸️");

    public final String label;
    public final String icon;

    WhitelistStatus(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }
}
