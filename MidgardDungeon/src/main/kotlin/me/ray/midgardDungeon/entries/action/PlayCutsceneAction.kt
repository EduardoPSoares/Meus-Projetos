package me.ray.midgardDungeon.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import me.ray.midgardDungeon.engine.CutsceneManager
import me.ray.midgardDungeon.engine.DungeonManager
import me.ray.midgardDungeon.entries.statics.CutsceneConfigEntry
import org.bukkit.Location

@Entry("play_cutscene_action", "Reproduz uma cutscene cinematográfica para o grupo", Colors.MEDIUM_PURPLE, "mdi:movie-open")
/**
 * A `Play Cutscene Action` inicia uma cutscene para os jogadores da dungeon.
 *
 * ## Como isso pode ser usado?
 * Use para criar intros de boss épicas, revelar novas salas,
 * ou criar momentos narrativos entre fases da dungeon.
 *
 * Durante a cutscene, os jogadores ficam em modo espectador com a câmera
 * se movendo entre pontos definidos. Ao final, retornam à posição original.
 */
class PlayCutsceneAction(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("A configuração da cutscene a reproduzir.")
    val cutsceneConfig: Ref<CutsceneConfigEntry> = emptyRef(),
    @Help("Se deve tocar para todo o grupo (true) ou apenas para o jogador que acionou (false).")
    val forEntireParty: Boolean = true,
) : ActionEntry {
    override fun ActionTrigger.execute() {
        val config = cutsceneConfig.get() ?: return
        if (config.keyframes.isEmpty()) return

        val instance = DungeonManager.getInstanceByPlayer(player.uniqueId)

        // Converter keyframes da config para keyframes do manager
        val world = instance?.world ?: player.world
        val keyframes = config.keyframes.map { kf ->
            CutsceneManager.CutsceneKeyframe(
                location = Location(
                    world,
                    kf.cameraPosition.x,
                    kf.cameraPosition.y,
                    kf.cameraPosition.z,
                    kf.cameraPosition.yaw,
                    kf.cameraPosition.pitch,
                ),
                durationTicks = kf.durationTicks,
                title = kf.title,
                subtitle = kf.subtitle,
                sound = kf.sound,
                soundVolume = kf.soundVolume.toFloat(),
                soundPitch = kf.soundPitch.toFloat(),
                particle = kf.particle,
                particleCount = kf.particleCount,
            )
        }

        val cutsceneId = config.id.ifEmpty { config.name }

        if (forEntireParty && instance != null) {
            CutsceneManager.startCutsceneForInstance(instance, cutsceneId, keyframes)
        } else {
            CutsceneManager.startCutscene(player, cutsceneId, keyframes, instance?.id)
        }
    }
}
