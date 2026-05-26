package me.ray.midgard.modules.races.model;

import me.ray.midgard.modules.races.RacesModule;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

/**
 * Condição composta para ativação de traits.
 * Suporta: horário (dia/noite), biomas, mundos, clima e altitude.
 * Imutável e thread-safe.
 */
public record TraitCondition(
        TimeRule time,
        Set<String> biomes,
        Set<String> worlds,
        WeatherRule weather,
        int minY,
        int maxY
) {

    public static final TraitCondition ALWAYS = new TraitCondition(
            TimeRule.ALWAYS, Set.of(), Set.of(), WeatherRule.ANY, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public enum TimeRule { ALWAYS, DAY, NIGHT }

    public enum WeatherRule { ANY, CLEAR, RAIN, THUNDER }

    public boolean isMet(Player player) {
        World world = player.getWorld();

        // Horário
        if (time != TimeRule.ALWAYS) {
            boolean isDay = RacesModule.isDayTime(world.getTime());
            if (time == TimeRule.DAY && !isDay) { return false; }
            if (time == TimeRule.NIGHT && isDay) { return false; }
        }

        // Biomas
        if (!biomes.isEmpty()) {
            Biome current = player.getLocation().getBlock().getBiome();
            String currentName = current.name();
            boolean match = false;
            for (String b : biomes) {
                if (currentName.equals(b) || currentName.contains(b)) {
                    match = true;
                    break;
                }
            }
            if (!match) { return false; }
        }

        // Mundos
        if (!worlds.isEmpty()) {
            String worldName = world.getName();
            if (!worlds.contains(worldName)) { return false; }
        }

        // Clima
        if (weather != WeatherRule.ANY) {
            if (weather == WeatherRule.THUNDER && !world.isThundering()) { return false; }
            if (weather == WeatherRule.RAIN && !world.hasStorm()) { return false; }
            if (weather == WeatherRule.CLEAR && world.hasStorm()) { return false; }
        }

        // Altitude
        int y = player.getLocation().getBlockY();
        if (y < minY || y > maxY) { return false; }

        return true;
    }

    /**
     * Verifica se esta condição é trivial (sempre verdadeira).
     */
    public boolean isAlways() {
        return time == TimeRule.ALWAYS
                && biomes.isEmpty()
                && worlds.isEmpty()
                && weather == WeatherRule.ANY
                && minY == Integer.MIN_VALUE
                && maxY == Integer.MAX_VALUE;
    }

    /**
     * Parse a partir de uma string simples (retrocompatível: "DAY", "NIGHT", "ALWAYS").
     */
    public static TraitCondition fromString(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("ALWAYS")) {
            return ALWAYS;
        }
        try {
            TimeRule rule = TimeRule.valueOf(value.toUpperCase());
            return new TraitCondition(rule, Set.of(), Set.of(), WeatherRule.ANY, Integer.MIN_VALUE, Integer.MAX_VALUE);
        } catch (IllegalArgumentException e) {
            return ALWAYS;
        }
    }

    /**
     * Parse a partir de uma ConfigurationSection YAML com campos opcionais.
     */
    public static TraitCondition fromSection(org.bukkit.configuration.ConfigurationSection section) {
        if (section == null) { return ALWAYS; }

        // time
        String timeStr = section.getString("time", "ALWAYS").toUpperCase();
        TimeRule timeRule;
        try {
            timeRule = TimeRule.valueOf(timeStr);
        } catch (IllegalArgumentException e) {
            timeRule = TimeRule.ALWAYS;
        }

        // biomes
        List<String> biomeList = section.getStringList("biomes");
        Set<String> biomeSet = biomeList.isEmpty() ? Set.of()
                : Set.copyOf(biomeList.stream().map(String::toUpperCase).toList());

        // worlds
        List<String> worldList = section.getStringList("worlds");
        Set<String> worldSet = worldList.isEmpty() ? Set.of() : Set.copyOf(worldList);

        // weather
        String weatherStr = section.getString("weather", "ANY").toUpperCase();
        WeatherRule weatherRule;
        try {
            weatherRule = WeatherRule.valueOf(weatherStr);
        } catch (IllegalArgumentException e) {
            weatherRule = WeatherRule.ANY;
        }

        // altitude
        int minY = section.getInt("min-y", Integer.MIN_VALUE);
        int maxY = section.getInt("max-y", Integer.MAX_VALUE);

        TraitCondition cond = new TraitCondition(timeRule, biomeSet, worldSet, weatherRule, minY, maxY);
        return cond.isAlways() ? ALWAYS : cond;
    }
}
