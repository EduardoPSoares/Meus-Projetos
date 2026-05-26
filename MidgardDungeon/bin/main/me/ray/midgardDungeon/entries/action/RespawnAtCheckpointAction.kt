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
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@Entry("respawn_checkpoint_action", "Respawna o jogador no checkpoint da dungeon", Colors.GREEN, "mdi:flag-checkered")
class RespawnAtCheckpointAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("Se deve curar o jogador com vida cheia.")
    val healPlayer: Boolean = true,
    @Help("Se deve respawnar todo o grupo ou apenas o jogador que acionou.")
    val respawnParty: Boolean = false,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId) ?: return
        val checkpoint = instance.getCheckpoint()

        val targets = if (respawnParty) {
            instance.getOnlinePlayers()
        } else {
            listOf(player)
        }

        targets.forEach { p ->
            p.teleport(checkpoint)
            if (healPlayer) {
                p.health = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.baseValue ?: 20.0
                p.foodLevel = 20
                p.saturation = 20f
            }
            p.sendMessage(Component.text("Respawnado no checkpoint!", NamedTextColor.GREEN))
        }
    }
}
