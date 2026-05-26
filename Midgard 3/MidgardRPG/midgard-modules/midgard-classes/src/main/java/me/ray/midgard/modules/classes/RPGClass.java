package me.ray.midgard.modules.classes;

import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Map;

/**
 * Representa uma classe (profissão) no RPG.
 * Define os atributos base e o crescimento de atributos por nível.
 */
public class RPGClass {

    private final String id;
    private final String displayName;
    private final ItemStack icon; // ItemStack (supports Nexo/CMD)
    private final List<String> lore;
    private final Map<String, Double> baseAttributes;
    private final Map<String, Double> attributesPerLevel;
    private final double healthPerLevel;
    private final double manaPerLevel;
    private final double baseMana;
    private final double baseHealth;
    private final java.util.List<ClassSkillLink> skills; // Lista de links de skills

    /**
     * Construtor da RPGClass.
     *
     * @param id ID único da classe.
     * @param displayName Nome de exibição.
     * @param icon Ícone da classe (ItemStack).
     * @param lore Descrição da classe.
     * @param baseAttributes Atributos iniciais.
     * @param attributesPerLevel Atributos ganhos por nível.
     * @param baseHealth Vida base.
     * @param healthPerLevel Vida ganha por nível.
     * @param baseMana Mana base.
     * @param manaPerLevel Mana ganha por nível.
     * @param skills Lista de skills e níveis de desbloqueio.
     */
    public RPGClass(String id, String displayName, ItemStack icon, java.util.List<String> lore,
                   java.util.Map<String, Double> baseAttributes,
                   java.util.Map<String, Double> attributesPerLevel,
                   double baseHealth, double healthPerLevel, 
                   double baseMana, double manaPerLevel,
                   java.util.List<ClassSkillLink> skills) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.lore = lore;
        this.baseAttributes = baseAttributes;
        this.attributesPerLevel = attributesPerLevel;
        this.baseHealth = baseHealth;
        this.healthPerLevel = healthPerLevel;
        this.baseMana = baseMana;
        this.manaPerLevel = manaPerLevel;
        this.skills = skills;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public ItemStack getIcon() {
        return icon != null ? icon.clone() : null;
    }

    public List<String> getLore() {
        return lore;
    }

    public Map<String, Double> getBaseAttributes() {
        return baseAttributes;
    }

    public Map<String, Double> getAttributesPerLevel() {
        return attributesPerLevel;
    }

    public double getHealthPerLevel() {
        return healthPerLevel;
    }

    public double getManaPerLevel() {
        return manaPerLevel;
    }
    
    public double getBaseHealth() {
        return baseHealth;
    }
    
    public double getBaseMana() {
        return baseMana;
    }
    
    public java.util.List<ClassSkillLink> getSkills() {
        return skills;
    }
}
