package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.core.utils.point.Position
import com.typewritermc.engine.paper.entry.StaticEntry

@Entry("cutscene_config", "Define uma cutscene cinematográfica com pontos de câmera", Colors.MEDIUM_PURPLE, "mdi:movie-open")
@Tags("cutscene_config")
/**
 * A entry `Cutscene Config` define uma sequência cinematográfica.
 *
 * ## Como isso pode ser usado?
 * Crie cutscenes para a entrada em salas de boss, revelação de áreas secretas,
 * apresentação de bosses ou narrativa entre fases da dungeon.
 *
 * Cada keyframe define um ponto de câmera com duração, texto e efeitos.
 * A câmera se move suavemente entre os pontos com interpolação.
 */
class CutsceneConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Lista de keyframes (pontos de câmera) da cutscene em ordem.")
    val keyframes: List<CutsceneKeyframeData> = emptyList(),
    @Help("Se os jogadores podem pular a cutscene (agachando).")
    val skippable: Boolean = true,
    @Help("Tipo da cutscene: BOSS_INTRO, ROOM_ENTER, NARRATIVE, CUSTOM.")
    val cutsceneType: String = "CUSTOM",
) : StaticEntry

/**
 * Um ponto de câmera dentro de uma cutscene.
 */
data class CutsceneKeyframeData(
    @Help("Posição e rotação da câmera neste keyframe.")
    val cameraPosition: Position = Position.ORIGIN,
    @Help("Duração em ticks que a câmera fica neste ponto (20 ticks = 1 segundo).")
    val durationTicks: Int = 40,
    @Help("Título exibido durante este keyframe (MiniMessage).")
    val title: String = "",
    @Help("Subtítulo exibido durante este keyframe (MiniMessage).")
    val subtitle: String = "",
    @Help("Som ao chegar neste keyframe (ex: ENTITY_ENDER_DRAGON_GROWL). Vazio = sem som.")
    val sound: String = "",
    @Help("Volume do som (0.0 a 2.0).")
    val soundVolume: Double = 1.0,
    @Help("Pitch do som (0.5 a 2.0).")
    val soundPitch: Double = 1.0,
    @Help("Partícula exibida no ponto (ex: FLAME, SOUL_FIRE_FLAME). Vazio = sem partícula.")
    val particle: String = "",
    @Help("Quantidade de partículas.")
    val particleCount: Int = 30,
)
