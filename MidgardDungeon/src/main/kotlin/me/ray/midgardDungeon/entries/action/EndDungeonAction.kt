package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import me.ray.midgardDungeon.engine.DungeonManager
import me.ray.midgardDungeon.engine.DungeonState
import me.ray.midgardDungeon.engine.LeaderboardManager
import me.ray.midgardDungeon.engine.AchievementManager
import me.ray.midgardDungeon.engine.StatsManager
import me.ray.midgardDungeon.engine.DailyDungeonManager
import me.ray.midgardDungeon.engine.PersistentCooldownManager
import me.ray.midgardDungeon.engine.ProgressionManager
import me.ray.midgardDungeon.engine.VaultManager
import me.ray.midgardDungeon.engine.MMOCoreManager
import me.ray.midgardDungeon.entries.event.fireDungeonCompleteEvent
import me.ray.midgardDungeon.entries.event.fireDungeonFailEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit

enum class DungeonEndReason {
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entry("end_dungeon_action", "Finaliza a instância atual da dungeon", Colors.RED, "mdi:stop-circle")
/**
 * A ação `End Dungeon` finaliza a dungeon atual com estado de sucesso ou falha.
 *
 * ## Como isso pode ser usado?
 * Acione após o boss morrer (COMPLETED) ou quando o tempo acabar (FAILED).
 */
class EndDungeonAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("O motivo pelo qual a dungeon terminou.")
    val reason: DungeonEndReason = DungeonEndReason.COMPLETED,
    @Help("Teleportar jogadores de volta ao spawn do servidor após terminar.")
    val teleportOut: Boolean = true,
    @Help("Cooldown em segundos após a dungeon (padrão: 3600).")
    val cooldownSeconds: Int = 3600,
    @Help("Recompensa em moedas ao completar (0 = sem recompensa). Requer Vault.")
    val moneyReward: Double = 0.0,
    @Help("Experiência MMOCore ao completar (0 = sem recompensa). Requer MMOCore.")
    val mmocoreExpReward: Double = 0.0,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        val completed = reason == DungeonEndReason.COMPLETED

        when (reason) {
            DungeonEndReason.COMPLETED -> {
                instance.transition(DungeonState.COMPLETED)
                StatsManager.showEndScreen(instance, true)

                // Submeter ao ranking
                val elapsed = instance.getElapsedSeconds()
                instance.party.memberIds.forEach { memberId ->
                    val playerName = Bukkit.getPlayer(memberId)?.name ?: "Desconhecido"
                    val stats = StatsManager.getRunStats(instance.id, memberId)
                    val modifiers = me.ray.midgardDungeon.engine.ModifierManager.getModifiers(instance.id)

                    // Score composto: tempo base + bônus de kills - penalidade de mortes + dano + segredos + modificadores
                    val timeScore = maxOf(0, (2000 - elapsed * 2).toInt())
                    val killScore = (stats?.kills ?: 0) * 50
                    val deathPenalty = (stats?.deaths ?: 0) * 100
                    val damageScore = ((stats?.damageDealt ?: 0.0) / 10).toInt()
                    val secretScore = (stats?.secretsFound ?: 0) * 200
                    val modifierBonus = modifiers.sumOf { ((it.lootMultiplier - 1.0) * 500).toInt() }
                    val totalScore = maxOf(0, timeScore + killScore - deathPenalty + damageScore + secretScore + modifierBonus)

                    LeaderboardManager.submitScore(LeaderboardManager.LeaderboardEntry(
                        playerName = playerName,
                        playerId = memberId,
                        dungeonId = instance.dungeonId,
                        timeSeconds = elapsed,
                        score = totalScore,
                    ))
                }

                // Verificar conquistas
                instance.getOnlinePlayers().forEach { p ->
                    AchievementManager.grant(p.uniqueId, "first_dungeon")
                    if (elapsed <= 300) {
                        AchievementManager.grant(p.uniqueId, "speed_runner")
                    }
                    val stats = StatsManager.getRunStats(instance.id, p.uniqueId)
                    if (stats != null && stats.deaths == 0) {
                        AchievementManager.grant(p.uniqueId, "no_deaths")
                    }
                }

                // Marcar conclusão diária/semanal
                if (DailyDungeonManager.isDailyDungeon(instance.dungeonId)) {
                    DailyDungeonManager.markDailyComplete(player.uniqueId.toString())
                }
                if (DailyDungeonManager.isWeeklyDungeon(instance.dungeonId)) {
                    DailyDungeonManager.markWeeklyComplete(player.uniqueId.toString())
                }

                // Progressão: marcar dungeon completada e conceder EXP
                instance.getOnlinePlayers().forEach { p ->
                    ProgressionManager.markCompleted(p.uniqueId, instance.dungeonId)
                    val expGained = 50 + (elapsed / 6).toInt().coerceAtMost(100)
                    val leveledUp = ProgressionManager.addExperience(p.uniqueId, expGained)
                    p.sendMessage(
                        Component.text("+$expGained EXP de dungeon", NamedTextColor.GREEN)
                    )
                    if (leveledUp) {
                        val newLevel = ProgressionManager.getLevel(p.uniqueId)
                        p.sendMessage(
                            Component.text("⬆ Subiu para nível $newLevel!", NamedTextColor.GOLD)
                        )
                    }

                    // Recompensa em moedas via Vault
                    if (moneyReward > 0 && VaultManager.isAvailable()) {
                        VaultManager.reward(p, moneyReward, "Dungeon completa")
                    }

                    // Recompensa em EXP via MMOCore
                    if (mmocoreExpReward > 0 && MMOCoreManager.isAvailable()) {
                        MMOCoreManager.rewardExperience(p, mmocoreExpReward, "Dungeon completa")
                    }
                }

                instance.getOnlinePlayers().forEach { p ->
                    p.sendMessage(Component.text("Dungeon completa!", NamedTextColor.GOLD))
                    fireDungeonCompleteEvent(p, instance)
                }
            }
            DungeonEndReason.FAILED -> {
                instance.transition(DungeonState.FAILED)
                StatsManager.showEndScreen(instance, false)
                instance.getOnlinePlayers().forEach { p ->
                    p.sendMessage(Component.text("Dungeon falhou!", NamedTextColor.RED))
                    fireDungeonFailEvent(p, instance)
                }
            }
            DungeonEndReason.CANCELLED -> {
                instance.transition(DungeonState.FAILED)
            }
        }

        // Finalizar estatísticas da run
        StatsManager.finishRun(instance.id, instance.dungeonId, instance.startTime, completed)

        if (teleportOut) {
            val spawn = Bukkit.getWorlds().firstOrNull()?.spawnLocation
            if (spawn != null) {
                instance.getOnlinePlayers().forEach { it.teleport(spawn) }
            }
        }

        // Aplicar cooldown persistente
        if (reason != DungeonEndReason.CANCELLED) {
            instance.party.memberIds.forEach { memberId ->
                PersistentCooldownManager.setCooldown(memberId, instance.dungeonId, cooldownSeconds)
            }
        }

        // Cleanup centralizado
        DungeonManager.fullCleanup(instance)
    }
}
