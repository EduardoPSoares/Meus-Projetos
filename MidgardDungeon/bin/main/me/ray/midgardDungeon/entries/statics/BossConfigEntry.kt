package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.core.entries.Query
import com.typewritermc.engine.paper.entry.StaticEntry

enum class BossPhaseType {
    NORMAL,
    ENRAGED,
    FINAL_STAND
}

@Entry("boss_config", "Define um encontro de boss", Colors.RED, "mdi:crown")
@Tags("boss_config")
/**
 * A entry `Boss Config` define um encontro de boss com múltiplas fases.
 *
 * ## Como isso pode ser usado?
 * Configure um boss com múltiplas fases, habilidades diferentes por fase,
 * mecânicas de enrage e loot especial.
 */
class BossConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("A configuração base do mob para este boss.")
    val baseMob: Ref<MobConfigEntry> = emptyRef(),
    @Help("Fases do boss com limites de vida.")
    val phases: List<BossPhaseConfig> = emptyList(),
    @Help("Loot concedido quando o boss é derrotado.")
    val bossLoot: Ref<LootTableEntry> = emptyRef(),
    @Help("Se deve mostrar uma barra de boss personalizada.")
    val showBossBar: Boolean = true,
    @Help("Cor da barra de boss.")  
    val bossBarColor: String = "RED",
) : StaticEntry

data class BossPhaseConfig(
    @Help("Nome da fase.")
    val phaseName: String = "",
    @Help("Tipo da fase (NORMAL, ENRAGED, FINAL_STAND).")
    val phaseType: String = "NORMAL",
    @Help("Limite de porcentagem de vida para entrar nesta fase (0.0 - 1.0).")
    val healthThreshold: Double = 1.0,
    @Help("Multiplicador de dano durante esta fase.")
    val damageMultiplier: Double = 1.0,
    @Help("Multiplicador de velocidade durante esta fase.")
    val speedMultiplier: Double = 1.0,
    @Help("IDs dos mobs adicionais para spawnar ao entrar nesta fase.")
    val additionalMobIds: List<String> = emptyList(),
) {
    fun getPhaseType(): BossPhaseType = try {
        BossPhaseType.valueOf(phaseType.uppercase())
    } catch (_: Exception) {
        BossPhaseType.NORMAL
    }

    fun getAdditionalMobs(): List<MobConfigEntry> {
        if (additionalMobIds.isEmpty()) return emptyList()
        val allMobs = Query.find<MobConfigEntry>().toList()
        return additionalMobIds.mapNotNull { id -> allMobs.firstOrNull { it.id == id } }
    }
}
