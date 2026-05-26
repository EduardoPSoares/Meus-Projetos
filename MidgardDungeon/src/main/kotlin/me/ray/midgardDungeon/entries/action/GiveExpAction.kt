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
import me.ray.midgardDungeon.engine.MMOCoreManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("give_exp_action", "Dá pontos de experiência aos jogadores da dungeon", Colors.GREEN, "mdi:star")
class GiveExpAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Quantidade de experiência a dar.")
    val experience: Int = 100,
    @Help("Se deve dar níveis de experiência ao invés de pontos.")
    val giveAsLevels: Boolean = false,
    @Help("Se deve dar para todo o grupo.")
    val giveToParty: Boolean = true,
    @Help("Se deve usar MMOCore ao invés de experiência vanilla (requer MMOCore).")
    val useMMOCore: Boolean = false,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val targets = if (giveToParty) {
            val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)
            instance?.getOnlinePlayers() ?: listOf(player)
        } else {
            listOf(player)
        }

        targets.forEach { p ->
            if (useMMOCore && MMOCoreManager.isAvailable()) {
                MMOCoreManager.rewardExperience(p, experience.toDouble())
            } else {
                if (giveAsLevels) {
                    p.giveExpLevels(experience)
                } else {
                    p.giveExp(experience)
                }
                p.sendMessage(
                    Component.text("+$experience ${if (giveAsLevels) "níveis" else "XP"}!", NamedTextColor.GREEN)
                )
            }
        }
    }
}
