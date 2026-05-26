package me.ray.midgard.modules.professions;

import me.ray.midgard.core.profile.ModuleData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dados de profissões de um jogador — armazenado no MidgardProfile.
 * Contém o progresso (nível/XP) de cada profissão que o jogador possui.
 */
public class ProfessionData implements ModuleData {

    private final Map<ProfessionType, ProfessionProgress> professions = new ConcurrentHashMap<>();
    private volatile ProfessionType activeProfession;

    /**
     * Retorna a profissão ativa (escolhida) do jogador, ou null se não escolheu.
     */
    public ProfessionType getActiveProfession() {
        return activeProfession;
    }

    /**
     * Define a profissão ativa do jogador.
     */
    public void setActiveProfession(ProfessionType type) {
        this.activeProfession = type;
    }

    /**
     * Verifica se o jogador já escolheu uma profissão.
     */
    public boolean hasActiveProfession() {
        return activeProfession != null;
    }

    /**
     * Retorna o progresso de uma profissão, ou null se o jogador não a possui.
     */
    public ProfessionProgress getProgress(ProfessionType type) {
        return professions.get(type);
    }

    /**
     * Retorna o progresso de uma profissão, criando se não existir.
     */
    public ProfessionProgress getOrCreateProgress(ProfessionType type) {
        return professions.computeIfAbsent(type, ProfessionProgress::new);
    }

    /**
     * Define o progresso de uma profissão.
     */
    public void setProgress(ProfessionProgress progress) {
        professions.put(progress.getType(), progress);
    }

    /**
     * Verifica se o jogador possui uma profissão (nível > 0 ou já iniciou).
     */
    public boolean hasProfession(ProfessionType type) {
        return professions.containsKey(type);
    }

    /**
     * Retorna o nível de uma profissão (0 se não possuir).
     */
    public int getLevel(ProfessionType type) {
        ProfessionProgress progress = professions.get(type);
        return progress != null ? progress.getLevel() : 0;
    }

    /**
     * Retorna todas as profissões que o jogador possui.
     */
    public Map<ProfessionType, ProfessionProgress> getAllProfessions() {
        return Collections.unmodifiableMap(professions);
    }

    /**
     * Retorna as profissões ativas (nível > 0).
     */
    public List<ProfessionProgress> getActiveProfessions() {
        return professions.values().stream()
                .filter(p -> p.getLevel() > 0)
                .toList();
    }
}
