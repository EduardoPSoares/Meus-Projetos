package me.ray.midgardDungeon.engine

enum class DungeonState {
    /** Aguardando jogadores entrarem / fila */
    WAITING,
    /** Contagem regressiva antes de iniciar */
    STARTING,
    /** Ondas normais em progresso */
    IN_PROGRESS,
    /** Fase de luta contra o boss */
    BOSS_FIGHT,
    /** Dungeon completada com sucesso */
    COMPLETED,
    /** Grupo morreu / tempo esgotou */
    FAILED;

    fun isActive(): Boolean = this == IN_PROGRESS || this == BOSS_FIGHT
    fun isFinished(): Boolean = this == COMPLETED || this == FAILED
}
