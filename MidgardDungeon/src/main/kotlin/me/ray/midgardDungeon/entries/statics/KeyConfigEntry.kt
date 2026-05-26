package me.ray.midgardDungeon.entries.statics

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.Tags
import com.typewritermc.engine.paper.entry.StaticEntry

@Entry("key_config", "Define uma chave para desbloquear áreas", Colors.YELLOW, "mdi:key-variant")
@Tags("key_config")
/**
 * A entry `Key Config` define uma chave usada para desbloquear salas ou baús.
 *
 * ## Como isso pode ser usado?
 * Crie chaves como "Chave do Boss", "Chave Secreta" que o grupo precisa
 * coletar para progredir ou acessar áreas escondidas.
 */
class KeyConfigEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("Nome de exibição da chave.")
    val displayName: String = "",
    @Help("Descrição da chave.")
    val description: String = "",
    @Help("Ícone para exibição.")
    val icon: String = "🔑",
    @Help("Se a chave é consumida ao usar.")
    val consumeOnUse: Boolean = true,
    @Help("ID da sala ou área que esta chave desbloqueia.")
    val unlocksAreaId: String = "",
) : StaticEntry
