package me.ray.midgard.core.attribute;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Representa uma instância de atributo com valor base, modificadores e cache.
 * Thread-safe para acesso concorrente em Folia.
 */
public class AttributeInstance {

    private final Attribute attribute;
    private volatile double baseValue;
    private final List<AttributeModifier> modifiers = new CopyOnWriteArrayList<>();
    private volatile double cachedValue;
    private volatile boolean dirty = true;

    public AttributeInstance(Attribute attribute) {
        this.attribute = attribute;
        this.baseValue = attribute.getBaseValue();
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public double getBaseValue() {
        return baseValue;
    }

    public synchronized void setBaseValue(double baseValue) {
        this.baseValue = baseValue;
        this.dirty = true;
    }

    public synchronized void addModifier(AttributeModifier modifier) {
        modifiers.add(modifier);
        dirty = true;
    }

    public synchronized void removeModifier(AttributeModifier modifier) {
        modifiers.remove(modifier);
        dirty = true;
    }
    
    public synchronized void removeModifier(UUID uuid) {
        modifiers.removeIf(m -> m.getUuid().equals(uuid));
        dirty = true;
    }

    public synchronized void removeModifier(String name) {
        modifiers.removeIf(m -> m.getName().equals(name));
        dirty = true;
    }
    
    /**
     * Verifica se existe um modificador com o nome especificado.
     */
    public boolean hasModifier(String name) {
        for (AttributeModifier m : modifiers) {
            if (m.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public synchronized double getValue() {
        if (dirty) {
            calculateValue();
        }
        return cachedValue;
    }

    private void calculateValue() {
        double value = baseValue;
        double additive = 0;
        double scalar = 1;

        // Snapshot for thread safety (CopyOnWriteArrayList iterator is safe)
        for (AttributeModifier modifier : modifiers) {
            switch (modifier.getOperation()) {
                case ADD_NUMBER -> value += modifier.getAmount();
                case MULTIPLY_PERCENTAGE_ADDITIVE -> additive += modifier.getAmount();
                case MULTIPLY_SCALAR -> scalar *= modifier.getAmount();
            }
        }

        value = value * (1 + additive) * scalar;
        
        // Clamp
        value = Math.max(attribute.getMinValue(), Math.min(attribute.getMaxValue(), value));
        
        // Set dirty=false BEFORE writing cachedValue so concurrent addModifier()
        // re-sets dirty=true and the stale value gets recalculated on next access
        this.dirty = false;
        this.cachedValue = value;
    }
}
