package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry

@Entry("wave_config", "Define uma wave de mobs para spawnar", Colors.ORANGE, "mdi:sword-cross")
@Tags("wave_config")
/**
 * A entry `Wave Config` define uma wave de inimigos.
 * Múltiplas waves podem ser vinculadas a uma sala.
 *
 * ## Como isso pode ser usado?
 * Defina waves com dificuldade crescente. Cada wave especifica quais mobs spawnam,
 * quantos e o tempo de espera antes da próxima wave começar.
 */
class WaveConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Os mobs para spawnar nesta wave.")
    val mobs: List<Ref<MobConfigEntry>> = emptyList(),
    @Help("Atraso em ticks antes desta wave começar (após a wave anterior ser limpa).")
    val delayTicks: Int = 60,
    @Help("Se todos os mobs devem ser mortos antes de avançar para a próxima wave.")
    val requireAllKilled: Boolean = true,
    @Help("Raio de spawn ao redor do ponto de spawn da sala.")
    val spawnRadius: Double = 5.0,
) : StaticEntry
