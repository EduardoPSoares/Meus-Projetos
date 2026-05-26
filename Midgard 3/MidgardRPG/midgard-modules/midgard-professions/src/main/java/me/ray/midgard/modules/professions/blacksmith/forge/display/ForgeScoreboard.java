package me.ray.midgard.modules.professions.blacksmith.forge.display;

import me.ray.midgard.core.scoreboard.MidgardScoreboard;
import me.ray.midgard.core.scoreboard.ScoreboardLines;
import me.ray.midgard.core.scoreboard.ScoreboardManager;
import me.ray.midgard.modules.professions.ProfessionsModule;
import me.ray.midgard.modules.professions.blacksmith.forge.ForgeStage;
import me.ray.midgard.modules.professions.blacksmith.forge.session.ForgeSession;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Scoreboard da forja, agora usando o framework global ScoreboardManager.
 * Prioridade 20 (gameplay) — sobreposta por debug(95) ou eventos(50+).
 */
public class ForgeScoreboard {

    private static final String BOARD_ID = "forge";

    private static String msg(String key) { return ProfessionsModule.getInstance().getMessage("display.scoreboard." + key); }

    private final MidgardScoreboard boardTemplate = new MidgardScoreboard() {
        @Override public String getId() { return BOARD_ID; }
        @Override public String getTitle() { return "  <gold><bold>" + msg("title") + "</bold>  "; }
        @Override public int getPriority() { return 20; }

        @Override
        public void onApply(Player player, ScoreboardLines lines) {
            lines.set("sep_top",   14, "<dark_gray><st>                    ");
            lines.set("recipe",    13, " <gray>" + msg("recipe") + " <dark_gray>—");
            lines.set("stage",     12, " <gray>" + msg("stage") + " <dark_gray>—");
            lines.set("spacer1",   11, "");
            lines.set("lbl_score", 10, "<gold>" + msg("scores_header"));
            lines.set("s_heat",     9, " <gray>" + msg("heating") + " <dark_gray>—");
            lines.set("s_hammer",   8, " <gray>" + msg("hammering") + " <dark_gray>—");
            lines.set("s_quench",   7, " <gray>" + msg("quenching") + " <dark_gray>—");
            lines.set("s_sharp",    6, " <gray>" + msg("sharpening") + " <dark_gray>—");
            lines.set("spacer2",    5, "");
            lines.set("lbl_qual",   4, "<gold>" + msg("quality_header"));
            lines.set("quality",    3, " <gray>" + msg("estimate") + " <dark_gray>—");
            lines.set("qbar",       2, " <dark_gray>░░░░░░░░░░");
            lines.set("sep_bot",    1, "<dark_gray><st>                    ");
        }
    };

    /**
     * Mostra a scoreboard da forja para o jogador e preenche com dados da sessão.
     */
    public void show(Player player, ForgeSession session) {
        ScoreboardManager.getInstance().show(player, boardTemplate);

        // Preenche com dados iniciais da sessão
        ScoreboardLines lines = ScoreboardManager.getInstance().getLines(player, BOARD_ID);
        if (lines != null) {
            lines.update("recipe", " <gray>" + msg("recipe") + " <white>" + limit(session.getRecipe().getDisplayName(), 14));
            lines.update("stage", " <gray>" + msg("stage") + " <yellow>" + stageIcon(session.getCurrentStage()) + session.getCurrentStage().getDisplayName());
        }
    }

    /**
     * Atualiza a scoreboard com o estado atual da sessão.
     */
    public void update(Player player, ForgeSession session) {
        ScoreboardLines lines = ScoreboardManager.getInstance().getLines(player, BOARD_ID);
        if (lines == null) { return; }

        ForgeStage stage = session.getCurrentStage();

        lines.update("stage", " <gray>" + msg("stage") + " <yellow>" + stageIcon(stage) + stage.getDisplayName());

        if (session.getHeatingScore() > 0) {
            lines.update("s_heat", " <gray>" + msg("heating") + " " + scoreColor(session.getHeatingScore()) + String.format("%.0f%%", session.getHeatingScore() * 100));
        }
        if (session.getHammeringScore() > 0) {
            lines.update("s_hammer", " <gray>" + msg("hammering") + " " + scoreColor(session.getHammeringScore()) + String.format("%.0f%%", session.getHammeringScore() * 100));
        }
        if (session.getQuenchingScore() > 0) {
            lines.update("s_quench", " <gray>" + msg("quenching") + " " + scoreColor(session.getQuenchingScore()) + String.format("%.0f%%", session.getQuenchingScore() * 100));
        }
        if (session.getSharpeningScore() > 0) {
            lines.update("s_sharp", " <gray>" + msg("sharpening") + " " + scoreColor(session.getSharpeningScore()) + String.format("%.0f%%", session.getSharpeningScore() * 100));
        }

        highlightActiveLine(lines, stage);

        double estimate = estimateQuality(session);
        if (estimate > 0) {
            lines.update("quality", " <gray>" + msg("estimate") + " " + scoreColor(estimate) + String.format("%.0f%%", estimate * 100));
            lines.update("qbar", " " + buildQualityBar(estimate));
        }
    }

    /**
     * Remove a scoreboard da forja (delega ao ScoreboardManager).
     */
    public void hide(Player player) {
        ScoreboardManager.getInstance().hide(player, BOARD_ID);
    }

    /**
     * Cleanup no disconnect — ScoreboardManager.cleanup(uuid) cuida disso globalmente.
     */
    public void cleanup(UUID uuid) {
        // Gerenciado pelo ScoreboardManager global
    }

    public boolean isShowing(UUID uuid) {
        return ScoreboardManager.getInstance().hasBoard(org.bukkit.Bukkit.getPlayer(uuid), BOARD_ID);
    }

    // === Internal ===

    private void highlightActiveLine(ScoreboardLines lines, ForgeStage stage) {
        switch (stage) {
            case HEATING -> lines.update("s_heat", " <white>▸ <gray>" + msg("heating") + " <yellow>...");
            case HAMMERING -> lines.update("s_hammer", " <white>▸ <gray>" + msg("hammering") + " <yellow>...");
            case QUENCHING -> lines.update("s_quench", " <white>▸ <gray>" + msg("quenching") + " <yellow>...");
            case SHARPENING -> lines.update("s_sharp", " <white>▸ <gray>" + msg("sharpening") + " <yellow>...");
            default -> {}
        }
    }

    private double estimateQuality(ForgeSession session) {
        int count = 0;
        double total = 0;
        if (session.getHeatingScore() > 0) { total += session.getHeatingScore(); count++; }
        if (session.getHammeringScore() > 0) { total += session.getHammeringScore(); count++; }
        if (session.getQuenchingScore() > 0) { total += session.getQuenchingScore(); count++; }
        if (session.getSharpeningScore() > 0) { total += session.getSharpeningScore(); count++; }
        return count > 0 ? total / count : 0;
    }

    private String scoreColor(double score) {
        if (score >= 0.9) { return "<green>"; }
        if (score >= 0.7) { return "<yellow>"; }
        if (score >= 0.5) { return "<gold>"; }
        return "<red>";
    }

    private String stageIcon(ForgeStage stage) {
        return switch (stage) {
            case HEATING -> "🔥 ";
            case HAMMERING -> "⚒ ";
            case QUENCHING -> "💧 ";
            case SHARPENING -> "🔪 ";
            case FINALIZING -> "✨ ";
            case COMPLETED -> "✔ ";
            case FAILED -> "✘ ";
            default -> "";
        };
    }

    private String buildQualityBar(double score) {
        int filled = (int) (score * 10);
        int empty = 10 - filled;
        String color;
        if (score >= 0.9) { color = "<green>"; }
        else if (score >= 0.7) { color = "<yellow>"; }
        else if (score >= 0.5) { color = "<gold>"; }
        else { color = "<red>"; }
        return color + "█".repeat(Math.max(0, filled)) + "<dark_gray>" + "░".repeat(Math.max(0, empty));
    }

    private String limit(String s, int max) {
        if (s == null) { return "???"; }
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
