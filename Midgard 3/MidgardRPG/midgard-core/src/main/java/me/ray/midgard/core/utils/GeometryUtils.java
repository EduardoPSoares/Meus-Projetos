package me.ray.midgard.core.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class GeometryUtils {

    /**
     * Returns a list of locations forming a circle.
     */
    public static List<Location> getCircle(Location center, double radius, int points) {
        List<Location> locations = new ArrayList<>();
        double increment = (2 * Math.PI) / points;

        for (int i = 0; i < points; i++) {
            double angle = i * increment;
            double x = center.getX() + (radius * Math.cos(angle));
            double z = center.getZ() + (radius * Math.sin(angle));
            locations.add(new Location(center.getWorld(), x, center.getY(), z));
        }
        return locations;
    }

    /**
     * Returns a list of locations forming a hollow sphere.
     */
    public static List<Location> getHollowSphere(Location center, double radius, int points) {
        List<Location> locations = new ArrayList<>();
        double phi = Math.PI * (3. - Math.sqrt(5.)); // Golden angle

        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) (points - 1)) * 2; // y goes from 1 to -1
            double radiusAtY = Math.sqrt(1 - y * y); // radius at y

            double theta = phi * i; // golden angle increment

            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;

            locations.add(center.clone().add(x * radius, y * radius, z * radius));
        }
        return locations;
    }

    /**
     * Returns a list of locations forming a line between two points.
     */
    public static List<Location> getLine(Location start, Location end, double step) {
        List<Location> locations = new ArrayList<>();
        Vector vector = end.toVector().subtract(start.toVector());
        double length = vector.length();
        vector.normalize();

        for (double i = 0; i < length; i += step) {
            locations.add(start.clone().add(vector.clone().multiply(i)));
        }
        return locations;
    }
    
    /**
     * Returns points in a cone shape (e.g. for breath attacks).
     */
    public static List<Location> getCone(Location start, Vector direction, double length, double angleDegrees, int points) {
        // This is a simplified cone generation (random points inside cone)
        // For particle effects, usually we want random points or structured rings.
        // Let's do structured rings.
        
        direction.normalize();
        
        // For now, returning just the center line.
        return getLine(start, start.clone().add(direction.multiply(length)), 0.5);
    }
}
