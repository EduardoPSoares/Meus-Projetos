package com.midgard.core.particle;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Fluent particle builder for easy particle creation.
 */
public class ParticleBuilder {

    private Particle particle;
    private Location location;
    private int count = 1;
    private double offsetX = 0;
    private double offsetY = 0;
    private double offsetZ = 0;
    private double extra = 0;
    private Object data;

    public ParticleBuilder(Particle particle) {
        this.particle = particle;
    }

    public ParticleBuilder location(Location location) {
        this.location = location;
        return this;
    }

    public ParticleBuilder count(int count) {
        this.count = count;
        return this;
    }

    public ParticleBuilder offset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }

    public ParticleBuilder extra(double extra) {
        this.extra = extra;
        return this;
    }

    public ParticleBuilder data(Object data) {
        this.data = data;
        return this;
    }

    public ParticleBuilder color(Color color, float size) {
        this.particle = Particle.DUST;
        this.data = new Particle.DustOptions(color, size);
        return this;
    }

    public ParticleBuilder color(Color color) {
        return color(color, 1.0f);
    }

    public void spawn() {
        if (location == null || location.getWorld() == null) return;
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data);
    }

    public void spawn(Player player) {
        if (location == null) return;
        player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data);
    }

    public void spawn(Collection<? extends Player> players) {
        players.forEach(this::spawn);
    }
}
