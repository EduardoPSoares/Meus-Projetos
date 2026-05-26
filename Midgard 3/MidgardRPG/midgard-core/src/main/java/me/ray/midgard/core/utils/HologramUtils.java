package me.ray.midgard.core.utils;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Location;
import org.bukkit.entity.Display;

import java.util.List;

public class HologramUtils {

    public static void createHologram(String name, Location location, List<String> lines) {
        HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
        
        // Remove existing if any
        manager.getHologram(name).ifPresent(manager::removeHologram);

        // Correct API usage for FancyHolograms 2.x
        TextHologramData data = new TextHologramData(name, location);
        data.setText(lines);
        data.setBillboard(Display.Billboard.CENTER);

        Hologram hologram = manager.create(data);
        manager.addHologram(hologram);
    }

    public static void removeHologram(String name) {
        FancyHologramsPlugin.get().getHologramManager().getHologram(name)
                .ifPresent(FancyHologramsPlugin.get().getHologramManager()::removeHologram);
    }
}
