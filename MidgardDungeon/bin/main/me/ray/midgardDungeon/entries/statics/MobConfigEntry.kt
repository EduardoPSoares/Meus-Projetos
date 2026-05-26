package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry
import org.bukkit.entity.EntityType

@Entry("mob_config", "Define um tipo de mob para spawnar em waves", Colors.RED, "mdi:skull")
@Tags("mob_config")
/**
 * A entry `Mob Config` define um tipo de mob para waves de dungeon.
 *
 * ## Como isso pode ser usado?
 * Configure guerreiros zumbis com vida, dano, velocidade e equipamento personalizados.
 * Vincule estes a entries de Wave para popular as salas da dungeon.
 */
class MobConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("O tipo de entidade do Bukkit.")
    val entityType: EntityType = EntityType.ZOMBIE,
    @Help("Nome de exibição acima da cabeça do mob.")
    val displayName: String = "",
    @Help("Vida máxima do mob.")
    val maxHealth: Double = 20.0,
    @Help("Dano de ataque.")
    val attackDamage: Double = 4.0,
    @Help("Multiplicador de velocidade de movimento (1.0 = normal).")
    val speedMultiplier: Double = 1.0,
    @Help("Quantos deste mob spawnar por wave.")
    val spawnCount: Int = 3,
    @Help("ID do mob MythicMobs (deixe vazio para usar o tipo de entidade vanilla).")
    val mythicMobId: String = "",
    @Help("Pontos de experiência concedidos ao matar.")
    val experienceReward: Int = 0,
) : StaticEntry
