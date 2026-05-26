package me.ray.midgard.bot.core.command;

public enum CommandCategory {

    GENERAL("⚙️", "Geral", "Comandos gerais do bot"),
    MODERATION("🛡️", "Moderação", "Comandos de moderação"),
    ADMINISTRATION("👑", "Administração", "Comandos administrativos"),
    ECONOMY("💰", "Economia", "Comandos de economia"),
    RPG("⚔️", "RPG", "Comandos de RPG"),
    FUN("🎮", "Diversão", "Comandos de diversão"),
    UTILITY("🔧", "Utilidades", "Comandos úteis"),
    MUSIC("🎵", "Música", "Comandos de música"),
    INFO("📊", "Informação", "Comandos informativos"),
    DEVELOPMENT("🛠️", "Desenvolvimento", "Comandos de desenvolvimento");

    private final String emoji;
    private final String displayName;
    private final String description;

    CommandCategory(String emoji, String displayName, String description) {
        this.emoji = emoji;
        this.displayName = displayName;
        this.description = description;
    }

    public String getEmoji() { return emoji; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return emoji + " " + displayName;
    }
}
