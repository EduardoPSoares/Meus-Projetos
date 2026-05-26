package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry
import me.ray.midgardDungeon.engine.TrapManager

@Entry("trap_config", "Define uma armadilha para dungeon", Colors.RED, "mdi:mine")
@Tags("trap_config")
/**
 * A entry `Trap Config` define uma armadilha que pode ser colocada nas salas.
 *
 * ## Como isso pode ser usado?
 * Configure armadilhas de dano, veneno, lentidão ou teleporte
 * para tornar a dungeon mais desafiadora.
 */
class TrapConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Tipo da armadilha (DAMAGE, SLOWNESS, POISON, BLINDNESS, TELEPORT, MOB_SPAWN).")
    val trapType: String = "DAMAGE",
    @Help("Raio de ativação da armadilha em blocos.")
    val radius: Double = 2.0,
    @Help("Dano causado (para armadilhas de dano).")
    val damage: Double = 4.0,
    @Help("Duração do efeito em ticks.")
    val effectDuration: Int = 60,
    @Help("Amplificador do efeito (nível).")
    val effectAmplifier: Int = 0,
    @Help("Se pode ser ativada apenas uma vez.")
    val oneTime: Boolean = true,
) : StaticEntry
