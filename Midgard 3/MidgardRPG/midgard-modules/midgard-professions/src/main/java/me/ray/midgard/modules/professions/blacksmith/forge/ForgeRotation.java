package me.ray.midgard.modules.professions.blacksmith.forge;

/**
 * Represents the rotation of a forge structure in the world.
 */
public enum ForgeRotation {

    NORTH(0),
    EAST(90),
    SOUTH(180),
    WEST(270);

    private final int degrees;

    ForgeRotation(int degrees) {
        this.degrees = degrees;
    }

    public int getDegrees() { return degrees; }

    public ForgeRotation next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public ForgeRotation previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    /**
     * Rotates relative X/Z coordinates based on this rotation.
     * Assumes anchor is at origin.
     */
    public int[] rotate(int relX, int relZ, int width, int depth) {
        return switch (this) {
            case NORTH -> new int[]{relX, relZ};
            case EAST -> new int[]{depth - 1 - relZ, relX};
            case SOUTH -> new int[]{width - 1 - relX, depth - 1 - relZ};
            case WEST -> new int[]{relZ, width - 1 - relX};
        };
    }

    /**
     * Gets the rotation matching a player's facing direction (yaw).
     */
    public static ForgeRotation fromYaw(float yaw) {
        float normalized = ((yaw % 360) + 360) % 360;
        if (normalized >= 315 || normalized < 45) { return SOUTH; }
        if (normalized >= 45 && normalized < 135) { return WEST; }
        if (normalized >= 135 && normalized < 225) { return NORTH; }
        return EAST;
    }
}
