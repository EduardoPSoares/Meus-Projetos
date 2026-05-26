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
import me.ray.midgardDungeon.engine.DifficultyVoteManager
import me.ray.midgardDungeon.engine.DungeonManager

@Entry("start_difficulty_vote_action", "Inicia uma votação de dificuldade para a dungeon", Colors.RED, "mdi:vote")
/**
 * A ação `Start Difficulty Vote` inicia uma votação de dificuldade entre os membros do grupo.
 *
 * ## Como isso pode ser usado?
 * Use antes de iniciar a dungeon para que o grupo escolha a dificuldade.
 */
class StartDifficultyVoteAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Tempo limite para votação em segundos.")
    val timeoutSeconds: Int = 20,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return

        DifficultyVoteManager.startVote(
            instanceId = instance.id,
            players = instance.party.memberIds,
            timeoutSeconds = timeoutSeconds,
        ) { difficulty ->
            DifficultyVoteManager.applyDifficulty(instance.id, difficulty)
        }
    }
}
