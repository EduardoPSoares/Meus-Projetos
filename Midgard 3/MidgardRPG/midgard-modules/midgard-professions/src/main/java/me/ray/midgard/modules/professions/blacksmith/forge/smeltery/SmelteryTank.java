package me.ray.midgard.modules.professions.blacksmith.forge.smeltery;

import me.ray.midgard.modules.professions.ProfessionsModule;

import java.util.*;

/**
 * Tanque interno da Smeltery que armazena metais fundidos.
 * Cada smeltery tem seu próprio tanque com capacidade baseada no tamanho.
 * Funciona como um container de fluidos estilo Tinkers' Construct.
 */
public class SmelteryTank {

    private final int capacity; // capacidade total em mb (millibuckets)
    private final Map<MoltenMetal, Integer> contents; // metal → quantidade em mb
    private int temperature; // temperatura atual do tanque em °C

    public SmelteryTank(int capacity) {
        this.capacity = capacity;
        this.contents = new LinkedHashMap<>();
        this.temperature = 0;
    }

    // ── Gerenciamento de conteúdo ──

    public int getCapacity() { return capacity; }
    public int getTemperature() { return temperature; }
    public void setTemperature(int temperature) { this.temperature = Math.max(0, temperature); }

    /**
     * Volume total de metal fundido no tanque.
     */
    public int getTotalVolume() {
        int total = 0;
        for (int amount : contents.values()) {
            total += amount;
        }
        return total;
    }

    /**
     * Espaço livre no tanque.
     */
    public int getFreeSpace() {
        return Math.max(0, capacity - getTotalVolume());
    }

    /**
     * Porcentagem de preenchimento (0.0 a 1.0).
     */
    public float getFillPercent() {
        if (capacity <= 0) { return 0f; }
        return (float) getTotalVolume() / capacity;
    }

    public boolean isEmpty() {
        return getTotalVolume() == 0;
    }

    public boolean isFull() {
        return getFreeSpace() <= 0;
    }

    /**
     * Conteúdo do tanque (read-only).
     */
    public Map<MoltenMetal, Integer> getContents() {
        return Collections.unmodifiableMap(contents);
    }

    /**
     * Quantidade de um metal específico no tanque.
     */
    public int getAmount(MoltenMetal metal) {
        return contents.getOrDefault(metal, 0);
    }

    /**
     * Adiciona metal fundido ao tanque. Retorna a quantidade realmente adicionada.
     */
    public int addMetal(MoltenMetal metal, int amount) {
        int free = getFreeSpace();
        int toAdd = Math.min(amount, free);
        if (toAdd <= 0) { return 0; }

        contents.merge(metal, toAdd, Integer::sum);
        return toAdd;
    }

    /**
     * Remove metal fundido do tanque. Retorna a quantidade realmente removida.
     */
    public int removeMetal(MoltenMetal metal, int amount) {
        int available = contents.getOrDefault(metal, 0);
        int toRemove = Math.min(amount, available);
        if (toRemove <= 0) { return 0; }

        int remaining = available - toRemove;
        if (remaining <= 0) {
            contents.remove(metal);
        } else {
            contents.put(metal, remaining);
        }
        return toRemove;
    }

    /**
     * Verifica se tem metal suficiente para uma quantidade específica.
     */
    public boolean hasMetal(MoltenMetal metal, int amount) {
        return getAmount(metal) >= amount;
    }

    /**
     * Esvazia completamente o tanque.
     */
    public void clear() {
        contents.clear();
        temperature = 0;
    }

    // ── Sistema de Ligas Automáticas ──

    /**
     * Tenta formar ligas automaticamente com os metais disponíveis.
     * Chamado a cada tick da smeltery quando há metal fundido.
     * Retorna lista de ligas formadas neste tick.
     */
    public List<AlloyResult> processAlloys(AlloyRecipeManager alloyRecipeManager) {
        List<AlloyResult> results = new ArrayList<>();
        List<AlloyRecipe> formable = alloyRecipeManager.findFormableAlloys(contents, temperature);

        for (AlloyRecipe recipe : formable) {
            int crafts = recipe.getMaxCrafts(contents);
            if (crafts <= 0) { continue; }

            // Limita para não ultrapassar a capacidade
            int freeForResult = getFreeSpace();
            // Precisa considerar que ingredientes são consumidos, liberando espaço
            int ingredientVolume = 0;
            for (int amount : recipe.getIngredients().values()) {
                ingredientVolume += amount;
            }
            int netVolumePerCraft = recipe.getResultAmount() - ingredientVolume;
            int maxCrafts;
            if (netVolumePerCraft <= 0) {
                maxCrafts = crafts; // liga reduz volume, sempre cabem
            } else {
                maxCrafts = Math.min(crafts, freeForResult / netVolumePerCraft);
            }

            if (maxCrafts <= 0) { continue; }

            // Consumir ingredientes
            for (var entry : recipe.getIngredients().entrySet()) {
                removeMetal(entry.getKey(), entry.getValue() * maxCrafts);
            }

            // Produzir liga
            int produced = addMetal(recipe.getResult(), recipe.getResultAmount() * maxCrafts);
            if (produced > 0) {
                results.add(new AlloyResult(recipe, maxCrafts, produced));
            }
        }

        return results;
    }

    // ── Utilitários ──

    /**
     * Lista os metais no tanque ordenados por quantidade (maior primeiro).
     */
    public List<Map.Entry<MoltenMetal, Integer>> getSortedContents() {
        List<Map.Entry<MoltenMetal, Integer>> sorted = new ArrayList<>(contents.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return sorted;
    }

    /**
     * Metal com maior quantidade no tanque (usado para display visual).
     */
    public MoltenMetal getDominantMetal() {
        MoltenMetal dominant = null;
        int maxAmount = 0;
        for (var entry : contents.entrySet()) {
            if (entry.getValue() > maxAmount) {
                maxAmount = entry.getValue();
                dominant = entry.getKey();
            }
        }
        return dominant;
    }

    /**
     * Resultado da formação de liga.
     */
    public record AlloyResult(AlloyRecipe recipe, int crafts, int totalProduced) {
        public String getDisplayMessage() {
            return ProfessionsModule.getInstance().getMessage("smeltery.alloy.formed")
                    .replace("%metal%", recipe.getResult().getFormattedName())
                    .replace("%amount%", String.valueOf(totalProduced));
        }
    }

    @Override
    public String toString() {
        return "SmelteryTank{capacity=" + capacity + ", volume=" + getTotalVolume() +
                ", temp=" + temperature + "°C, metals=" + contents.size() + "}";
    }
}
