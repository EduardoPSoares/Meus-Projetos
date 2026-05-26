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
import net.kyori.adventure.sound.Sound

@Entry("dungeon_sound_action", "Toca um som para os jogadores da dungeon", Colors.PURPLE, "mdi:volume-high")
class DungeonSoundAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A chave do som (ex: entity.ender_dragon.growl, ui.toast.challenge_complete).")
    val sound: String = "entity.experience_orb.pickup",
    @Help("Volume do som (1.0 = normal).")
    val volume: Float = 1.0f,
    @Help("Tom do som (1.0 = normal).")
    val pitch: Float = 1.0f,
    @Help("Se deve tocar para todo o grupo.")
    val playForParty: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val adventureSound = Sound.sound(
            net.kyori.adventure.key.Key.key(sound),
            Sound.Source.MASTER,
            volume,
            pitch
        )

        val targets = if (playForParty) {
            val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)
            instance?.getOnlinePlayers() ?: listOf(player)
        } else {
            listOf(player)
        }

        targets.forEach { it.playSound(adventureSound) }
    }
}
