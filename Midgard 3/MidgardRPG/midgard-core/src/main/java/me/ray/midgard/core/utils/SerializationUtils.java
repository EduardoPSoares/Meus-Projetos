package me.ray.midgard.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class SerializationUtils {

    /**
     * Serializes an object (like ItemStack) to a Base64 string.
     */
    public static String toBase64(Object object) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(object);
            dataOutput.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save object.", e);
        }
    }

    /**
     * Deserializes an object from a Base64 string.
     */
    public static Object fromBase64(String data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return dataInput.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load object.", e);
        }
    }

    public static String locationToString(Location loc) {
        if (loc == null || loc.getWorld() == null) { return null; }
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getYaw() + ";" + loc.getPitch();
    }

    public static Location stringToLocation(String str) {
        if (str == null || str.isEmpty()) { return null; }
        String[] parts = str.split(";");
        if (parts.length < 4) { return null; }

        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) { return null; }

            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;

            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
