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
import me.ray.midgardDungeon.engine.CutsceneManager

@Entry("skip_cutscene_action", "Pula a cutscene atual do jogador", Colors.MEDIUM_PURPLE, "mdi:skip-forward")
/**
 * A `Skip Cutscene Action` pula a cutscene em andamento.
 *
 * ## Como isso pode ser usado?
 * Use como ação de um botão ou evento para permitir que jogadores pulem cutscenes.
 */
class SkipCutsceneAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Se deve pular para todo o grupo (true) ou só para o jogador (false).")
    val forEntireParty: Boolean = false,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        if (forEntireParty) {
            val instance = me.ray.midgardDungeon.engine.DungeonManager.getInstanceByPlayer(player.uniqueId)
            if (instance != null) {
                CutsceneManager.stopCutsceneForInstance(instance.id)
            }
        } else {
            CutsceneManager.skipCutscene(player)
        }
    }
}
