package me.ray.midgard.modules.combat;

import me.ray.midgard.core.profile.ModuleData;

/**
 * Classe de dados persistentes do módulo de combate.
 * <p>
 * Armazena o estado atual dos atributos vitais do jogador (Vida, Mana, Stamina)
 * e informações de progresso (Nível, Experiência).
 * Esta classe é serializada e salva no perfil do jogador.
 */
public class CombatData implements ModuleData {

    private volatile double currentHealth;
    private volatile double currentMana;
    private volatile double currentStamina;
    private volatile double currentAbsorption;
    private volatile long lastRegenTick;
    
    private volatile int level;
    private volatile double experience;

    /**
     * Construtor padrão.
     * Inicializa os valores com padrões básicos.
     */
    public CombatData() {
        this.currentHealth = 100; // Vida inicial padrão
        this.currentMana = 0;
        this.currentStamina = 0;
        this.currentAbsorption = 0;
        this.lastRegenTick = System.currentTimeMillis();
        this.level = 1;
        this.experience = 0;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(double currentHealth) {
        this.currentHealth = currentHealth;
    }

    public double getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(double currentMana) {
        this.currentMana = currentMana;
    }

    public double getCurrentStamina() {
        return currentStamina;
    }

    public void setCurrentStamina(double currentStamina) {
        this.currentStamina = currentStamina;
    }

    public long getLastRegenTick() {
        return lastRegenTick;
    }

    public void setLastRegenTick(long lastRegenTick) {
        this.lastRegenTick = lastRegenTick;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getExperience() {
        return experience;
    }

    public void setExperience(double experience) {
        this.experience = experience;
    }

    public synchronized void addExperience(double amount) {
        this.experience += amount;
    }

    public double getCurrentAbsorption() {
        return currentAbsorption;
    }

    public void setCurrentAbsorption(double currentAbsorption) {
        this.currentAbsorption = currentAbsorption;
    }
}
