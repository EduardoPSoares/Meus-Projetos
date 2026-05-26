package me.ray.midgard.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ProgressBar {

    /**
     * Generates a progress bar component.
     * @param current Current value (e.g., 50)
     * @param max Max value (e.g., 100)
     * @param totalBars Total number of bars (e.g., 10)
     * @param symbol The symbol to use (e.g., "|")
     * @param colorFilled Color for filled bars (e.g., "<green>")
     * @param colorEmpty Color for empty bars (e.g., "<gray>")
     * @return The formatted Component
     */
    public static Component get(double current, double max, int totalBars, String symbol, String colorFilled, String colorEmpty) {
        double percent = Math.min(1.0, Math.max(0.0, current / max));
        int filledBars = (int) (totalBars * percent);
        int emptyBars = totalBars - filledBars;

        StringBuilder sb = new StringBuilder();
        sb.append(colorFilled);
        sb.append(symbol.repeat(filledBars));
        sb.append(colorEmpty);
        sb.append(symbol.repeat(emptyBars));

        return MiniMessage.miniMessage().deserialize(sb.toString());
    }

    /**
     * Generates a standard HP bar style.
     */
    public static Component hp(double current, double max) {
        return get(current, max, 20, "|", "<gradient:red:dark_red>", "<gray>");
    }
    
    /**
     * Generates a standard XP bar style.
     */
    public static Component xp(double current, double max) {
        return get(current, max, 20, "|", "<gradient:green:dark_green>", "<gray>");
    }
}
