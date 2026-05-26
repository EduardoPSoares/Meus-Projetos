package com.midgard.core.particle;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pre-built particle shape animations.
 */
public final class ParticleShapes {

    private ParticleShapes() {
    }

    public static void circle(Location center, Particle particle, double radius, int points) {
        if (center.getWorld() == null) return;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location point = new Location(center.getWorld(), x, center.getY(), z);
            new ParticleBuilder(particle).location(point).count(1).extra(0).spawn();
        }
    }

    public static void circle(Location center, Color color, double radius, int points) {
        if (center.getWorld() == null) return;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location point = new Location(center.getWorld(), x, center.getY(), z);
            new ParticleBuilder(Particle.DUST).location(point).color(color).spawn();
        }
    }

    public static void sphere(Location center, Particle particle, double radius, int density) {
        if (center.getWorld() == null) return;
        for (int i = 0; i < density; i++) {
            double theta = ThreadLocalRandom.current().nextDouble() * Math.PI;
            double phi = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double x = center.getX() + radius * Math.sin(theta) * Math.cos(phi);
            double y = center.getY() + radius * Math.cos(theta);
            double z = center.getZ() + radius * Math.sin(theta) * Math.sin(phi);
            Location point = new Location(center.getWorld(), x, y, z);
            new ParticleBuilder(particle).location(point).count(1).extra(0).spawn();
        }
    }

    public static void helix(Location center, Particle particle, double radius, double height, int points, double rotations) {
        if (center.getWorld() == null) return;
        for (int i = 0; i < points; i++) {
            double ratio = (double) i / points;
            double angle = rotations * 2 * Math.PI * ratio;
            double x = center.getX() + radius * Math.cos(angle);
            double y = center.getY() + height * ratio;
            double z = center.getZ() + radius * Math.sin(angle);
            Location point = new Location(center.getWorld(), x, y, z);
            new ParticleBuilder(particle).location(point).count(1).extra(0).spawn();
        }
    }

    public static void line(Location from, Location to, Particle particle, double density) {
        if (from.getWorld() == null) return;
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        direction.normalize();

        for (double d = 0; d < length; d += 1.0 / density) {
            Location point = from.clone().add(direction.clone().multiply(d));
            new ParticleBuilder(particle).location(point).count(1).extra(0).spawn();
        }
    }

    public static void cube(Location center, Particle particle, double size, double density) {
        if (center.getWorld() == null) return;
        double half = size / 2;
        for (double d = -half; d <= half; d += 1.0 / density) {
            // 4 edges along X
            spawn(center, particle, d, -half, -half);
            spawn(center, particle, d, -half, half);
            spawn(center, particle, d, half, -half);
            spawn(center, particle, d, half, half);
            // 4 edges along Y
            spawn(center, particle, -half, d, -half);
            spawn(center, particle, -half, d, half);
            spawn(center, particle, half, d, -half);
            spawn(center, particle, half, d, half);
            // 4 edges along Z
            spawn(center, particle, -half, -half, d);
            spawn(center, particle, -half, half, d);
            spawn(center, particle, half, -half, d);
            spawn(center, particle, half, half, d);
        }
    }

    private static void spawn(Location center, Particle particle, double dx, double dy, double dz) {
        Location point = center.clone().add(dx, dy, dz);
        new ParticleBuilder(particle).location(point).count(1).extra(0).spawn();
    }
}
