package me.ray.midgard.core.attribute;

import java.util.UUID;

public class AttributeModifier {

    private final UUID uuid;
    private final String name;
    private final double amount;
    private final AttributeOperation operation;

    public AttributeModifier(String name, double amount, AttributeOperation operation) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.amount = amount;
        this.operation = operation;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public double getAmount() {
        return amount;
    }

    public AttributeOperation getOperation() {
        return operation;
    }
}
