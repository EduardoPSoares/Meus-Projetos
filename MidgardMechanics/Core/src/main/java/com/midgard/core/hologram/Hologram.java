package com.midgard.core.hologram;

import com.midgard.core.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple hologram using ArmorStand entities.
 */
public class Hologram {

    private final Location origin;
    private final List<ArmorStand> stands = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private double lineSpacing = 0.25;
    private boolean spawned = false;

    public Hologram(Location origin) {
        this.origin = origin.clone();
    }

    public Hologram addLine(String text) {
        lines.add(text);
        return this;
    }

    public Hologram setLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, text);
            if (spawned && index < stands.size()) {
                stands.get(index).customName(MessageUtils.toComponent(lines.get(index)));
            }
        }
        return this;
    }

    public Hologram removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
            if (spawned) {
                destroy();
                spawn();
            }
        }
        return this;
    }

    public Hologram lineSpacing(double spacing) {
        this.lineSpacing = spacing;
        return this;
    }

    public void spawn() {
        World world = origin.getWorld();
        if (world == null) return;
        destroy();

        for (int i = 0; i < lines.size(); i++) {
            Location loc = origin.clone().add(0, -i * lineSpacing, 0);
            ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomNameVisible(true);
            stand.customName(MessageUtils.toComponent(lines.get(i)));
            stand.setMarker(true);
            stand.setInvulnerable(true);
            stand.setSmall(true);
            stands.add(stand);
        }
        spawned = true;
    }

    public void destroy() {
        stands.forEach(stand -> {
            if (stand.isValid()) stand.remove();
        });
        stands.clear();
        spawned = false;
    }

    public void teleport(Location newOrigin) {
        origin.setWorld(newOrigin.getWorld());
        origin.setX(newOrigin.getX());
        origin.setY(newOrigin.getY());
        origin.setZ(newOrigin.getZ());

        for (int i = 0; i < stands.size(); i++) {
            ArmorStand stand = stands.get(i);
            if (stand.isValid()) {
                stand.teleport(origin.clone().add(0, -i * lineSpacing, 0));
            }
        }
    }

    public boolean isSpawned() {
        return spawned;
    }

    public boolean isValid() {
        return spawned && !stands.isEmpty() && stands.stream().allMatch(ArmorStand::isValid);
    }

    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public Location getOrigin() {
        return origin.clone();
    }
}
