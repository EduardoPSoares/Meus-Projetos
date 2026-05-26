package me.ray.midgard.core.attribute;

public class Attribute {

    private final String id;
    private final String name;
    private final double baseValue;
    private final double minValue;
    private final double maxValue;
    private final String icon;
    private final String format;

    public Attribute(String id, String name, double baseValue, double minValue, double maxValue, String icon, String format) {
        this.id = id;
        this.name = name;
        this.baseValue = baseValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.icon = icon != null ? icon : "";
        this.format = format != null ? format : "0.0";
    }

    // Constructor for backward compatibility
    public Attribute(String id, String name, double baseValue, double minValue, double maxValue) {
        this(id, name, baseValue, minValue, maxValue, "", "0.0");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getBaseValue() {
        return baseValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public String getIcon() {
        return icon;
    }

    public String getFormat() {
        return format;
    }
}
