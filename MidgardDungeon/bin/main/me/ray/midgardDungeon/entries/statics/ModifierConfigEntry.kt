package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry

@Entry("modifier_config", "Define um modificador de dungeon", Colors.ORANGE, "mdi:tune-variant")
@Tags("modifier_config")
/**
 * A entry `Modifier Config` define um modificador que altera dificuldade e recompensas.
 *
 * ## Como isso pode ser usado?
 * Crie modificadores como "Chuva de Fogo", "Mobs Resistentes" ou "Loot Dobrado"
 * para variar a experiência da dungeon.
 */
class ModifierConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Nome de exibição do modificador.")
    val displayName: String = "",
    @Help("Descrição do modificador.")
    val description: String = "",
    @Help("Ícone para exibição (emoji ou código).")
    val icon: String = "⚙",
    @Help("Multiplicador de vida dos mobs.")
    val healthMultiplier: Double = 1.0,
    @Help("Multiplicador de dano dos mobs.")
    val damageMultiplier: Double = 1.0,
    @Help("Multiplicador de velocidade dos mobs.")
    val speedMultiplier: Double = 1.0,
    @Help("Multiplicador de loot recebido.")
    val lootMultiplier: Double = 1.0,
    @Help("Multiplicador de experiência recebida.")
    val expMultiplier: Double = 1.0,
    @Help("Mobs extras por wave.")
    val extraMobs: Int = 0,
) : StaticEntry
