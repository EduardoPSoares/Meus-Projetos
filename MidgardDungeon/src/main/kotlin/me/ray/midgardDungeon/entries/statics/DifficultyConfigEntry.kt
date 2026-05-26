package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry

@Entry("difficulty_config", "Define o escalonamento de dificuldade para grupos", Colors.ORANGE, "mdi:speedometer")
@Tags("difficulty_config")
class DifficultyConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Multiplicador de vida por jogador extra além do primeiro (ex: 0.5 = +50% de vida por jogador).")
    val healthPerPlayer: Double = 0.5,
    @Help("Multiplicador de dano por jogador extra.")
    val damagePerPlayer: Double = 0.3,
    @Help("Mobs extras por wave por jogador extra.")
    val extraMobsPerPlayer: Int = 1,
    @Help("Nível base de dificuldade (usado para escalonamento por nível).")
    val baseDifficulty: Int = 1,
) : StaticEntry
