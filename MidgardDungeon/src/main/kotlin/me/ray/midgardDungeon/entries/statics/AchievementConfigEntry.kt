package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry

@Entry("achievement_config", "Define uma conquista desbloqueável", Colors.DARK_ORANGE, "mdi:trophy-award")
@Tags("achievement_config")
/**
 * A entry `Achievement Config` define uma conquista personalizada.
 *
 * ## Como isso pode ser usado?
 * Crie conquistas como "Primeiro Boss", "Velocista", "Imortal"
 * que são desbloqueadas automaticamente por condições do jogo.
 */
class AchievementConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Nome de exibição da conquista.")
    val displayName: String = "",
    @Help("Descrição da conquista.")
    val description: String = "",
    @Help("Ícone para exibição (emoji).")
    val icon: String = "⭐",
    @Help("Condição para desbloquear (ex: 'kills>=10', 'deaths==0', 'time<=300').")
    val condition: String = "",
) : StaticEntry
