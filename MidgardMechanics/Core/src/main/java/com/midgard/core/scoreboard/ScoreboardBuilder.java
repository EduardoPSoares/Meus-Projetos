package com.midgard.core.scoreboard;

import com.midgard.core.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent scoreboard builder with flicker-free updates.
 */
public class ScoreboardBuilder {

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final List<String> lines = new ArrayList<>();

    public ScoreboardBuilder(String title) {
        org.bukkit.scoreboard.ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) throw new IllegalStateException("ScoreboardManager not yet available");
        this.scoreboard = mgr.getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("midgard", Criteria.DUMMY,
                MessageUtils.toComponent(title));
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public ScoreboardBuilder line(String text) {
        lines.add(text);
        return this;
    }

    public ScoreboardBuilder blank() {
        // Each blank line needs unique invisible chars
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= lines.size(); i++) {
            sb.append("§r");
        }
        lines.add(sb.toString());
        return this;
    }

    public void apply(Player player) {
        // Clear old teams
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }

        for (int i = 0; i < lines.size(); i++) {
            int score = lines.size() - i;
            String entry = getUniqueEntry(i);
            String text = lines.get(i);

            Team team = scoreboard.registerNewTeam("line_" + i);

            // Use Adventure Component for prefix/suffix
            Component textComponent = MessageUtils.toComponent(text);
            team.prefix(textComponent);

            team.addEntry(entry);
            objective.getScore(entry).setScore(score);
        }

        player.setScoreboard(scoreboard);
    }

    public void remove(Player player) {
        var mgr = Bukkit.getScoreboardManager();
        if (mgr != null) {
            player.setScoreboard(mgr.getNewScoreboard());
        }
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    private String getUniqueEntry(int index) {
        // Use color code pairs to create unique invisible entries (supports >16 lines)
        StringBuilder sb = new StringBuilder();
        sb.append("\u00a7").append("abcdefghijklmnop".charAt(index % 16));
        if (index >= 16) {
            sb.append("\u00a7").append("abcdefghijklmnop".charAt((index / 16) % 16));
        }
        sb.append("\u00a7r");
        return sb.toString();
    }
}
