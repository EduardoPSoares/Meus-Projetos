package me.ray.midgard.modules.professions;

import me.ray.midgard.core.MidgardCore;
import me.ray.midgard.core.database.DatabaseManager;
import me.ray.midgard.core.debug.MidgardLogger;
import me.ray.midgard.core.profile.MidgardProfile;
import me.ray.midgard.core.utils.Task;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Gerenciador central do sistema de profissões.
 * Responsável por carregar/salvar/acessar dados de profissões dos jogadores.
 */
public class ProfessionManager {

    private final ProfessionsModule module;
    private final ProfessionRepository repository;

    public ProfessionManager(ProfessionsModule module, DatabaseManager databaseManager) {
        this.module = module;
        this.repository = new ProfessionRepository(databaseManager);
    }

    /**
     * Carrega os dados de profissões de um jogador no seu profile.
     * Chamado no login do jogador.
     */
    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }

        Map<ProfessionType, ProfessionProgress> data = repository.loadPlayer(uuid);
        ProfessionData profData = profile.getOrCreateData(ProfessionData.class);
        for (var entry : data.entrySet()) {
            profData.setProgress(entry.getValue());
        }

        // Carregar profissão ativa
        repository.loadActiveProfession(uuid).ifPresent(profData::setActiveProfession);
    }

    /**
     * Carrega os dados de profissões de um jogador de forma assíncrona.
     * Faz a query em thread async e aplica os dados na region thread do jogador.
     */
    public void loadPlayerDataAsync(Player player) {
        UUID uuid = player.getUniqueId();
        Task.async(() -> {
            try {
                Map<ProfessionType, ProfessionProgress> data = repository.loadPlayer(uuid);
                Optional<ProfessionType> activeProfession = repository.loadActiveProfession(uuid);

                Task.sync(player, () -> {
                    if (!player.isOnline()) { return; }
                    MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
                    if (profile == null) { return; }

                    ProfessionData profData = profile.getOrCreateData(ProfessionData.class);
                    for (var entry : data.entrySet()) {
                        profData.setProgress(entry.getValue());
                    }
                    activeProfession.ifPresent(profData::setActiveProfession);
                    MidgardLogger.debug("Dados de profissão carregados para %s (ativa: %s)",
                            player.getName(), activeProfession.map(ProfessionType::getId).orElse("nenhuma"));
                });
            } catch (Exception e) {
                MidgardLogger.error("Erro ao carregar dados de profissão async para %s", player.getName(), e);
            }
        });
    }

    /**
     * Salva os dados de profissões de um jogador (async).
     */
    public CompletableFuture<Void> savePlayerData(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return CompletableFuture.completedFuture(null); }

        ProfessionData profData = profile.getData(ProfessionData.class);
        if (profData == null) { return CompletableFuture.completedFuture(null); }

        return repository.saveAll(player.getUniqueId(), profData.getAllProfessions());
    }

    /**
     * Salva os dados de profissões de um jogador por UUID (async).
     */
    public CompletableFuture<Void> savePlayerData(UUID uuid, ProfessionData profData) {
        if (profData == null) { return CompletableFuture.completedFuture(null); }
        return repository.saveAll(uuid, profData.getAllProfessions());
    }

    /**
     * Adiciona XP a uma profissão de um jogador. Retorna níveis ganhos.
     * Só premia XP se a profissão for a profissão ativa do jogador.
     */
    public int addXp(Player player, ProfessionType type, double amount) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return 0; }

        ProfessionData profData = profile.getOrCreateData(ProfessionData.class);

        // Bloquear XP se não é a profissão ativa
        if (!isActiveProfession(profData, type)) {
            return 0;
        }

        ProfessionProgress progress = profData.getOrCreateProgress(type);
        int levelsGained = progress.addXp(amount);

        // Salvar async
        repository.saveProgress(player.getUniqueId(), progress)
                .exceptionally(e -> {
                    MidgardLogger.error("Erro ao salvar XP da profissão %s para %s", type.getId(), player.getName(), e);
                    return null;
                });

        return levelsGained;
    }

    /**
     * Define o nível de uma profissão de um jogador.
     */
    public void setLevel(Player player, ProfessionType type, int level) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return; }

        ProfessionData profData = profile.getOrCreateData(ProfessionData.class);
        ProfessionProgress progress = profData.getOrCreateProgress(type);
        progress.setLevel(level);
        progress.setXp(0);

        repository.saveProgress(player.getUniqueId(), progress)
                .exceptionally(e -> {
                    MidgardLogger.error("Erro ao salvar nível da profissão %s para %s", type.getId(), player.getName(), e);
                    return null;
                });
    }

    /**
     * Retorna o progresso de uma profissão de um jogador.
     */
    public ProfessionProgress getProgress(Player player, ProfessionType type) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return null; }

        ProfessionData profData = profile.getData(ProfessionData.class);
        if (profData == null) { return null; }

        return profData.getProgress(type);
    }

    /**
     * Retorna o nível de uma profissão de um jogador (0 se não possui).
     */
    public int getLevel(Player player, ProfessionType type) {
        ProfessionProgress progress = getProgress(player, type);
        return progress != null ? progress.getLevel() : 0;
    }

    public ProfessionRepository getRepository() { return repository; }

    // ==========================================
    // Profissão Ativa
    // ==========================================

    /**
     * Define a profissão ativa de um jogador.
     * Reseta o XP e nível da profissão anterior se resetOnChange estiver habilitado.
     */
    public CompletableFuture<Void> chooseProfession(Player player, ProfessionType type) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return CompletableFuture.completedFuture(null); }

        ProfessionData profData = profile.getOrCreateData(ProfessionData.class);
        profData.setActiveProfession(type);

        return repository.saveActiveProfession(player.getUniqueId(), type)
                .exceptionally(e -> {
                    MidgardLogger.error("Erro ao salvar profissão ativa %s para %s", type.getId(), player.getName(), e);
                    return null;
                });
    }

    /**
     * Retorna a profissão ativa de um jogador, ou null se não escolheu.
     */
    public ProfessionType getActiveProfession(Player player) {
        MidgardProfile profile = MidgardCore.getProfileManager().getProfile(player);
        if (profile == null) { return null; }

        ProfessionData profData = profile.getData(ProfessionData.class);
        if (profData == null) { return null; }

        return profData.getActiveProfession();
    }

    /**
     * Verifica se o tipo informado é a profissão ativa do jogador.
     */
    private boolean isActiveProfession(ProfessionData profData, ProfessionType type) {
        ProfessionType active = profData.getActiveProfession();
        return active != null && active == type;
    }
}
