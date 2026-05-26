package me.ray.midgardDungeon.engine

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

/**
 * Integração opcional com MMOCore para classes, níveis e experiência.
 * Detecta MMOCore automaticamente via reflection.
 */
object MMOCoreManager {

    @Volatile
    private var available = false

    // Classes e métodos cacheados via reflection
    private var playerDataClass: Class<*>? = null
    private var getPlayerDataMethod: java.lang.reflect.Method? = null
    private var getLevelMethod: java.lang.reflect.Method? = null
    private var getClassNameMethod: java.lang.reflect.Method? = null
    private var giveExperienceMethod: java.lang.reflect.Method? = null
    private var getExperienceMethod: java.lang.reflect.Method? = null
    private var getAttributeMethod: java.lang.reflect.Method? = null
    private var getPartyMethod: java.lang.reflect.Method? = null
    private var expSourceClass: Class<*>? = null
    private var expSourceOther: Any? = null

    // Classe PlayerClass
    private var professionGetNameMethod: java.lang.reflect.Method? = null

    fun initialize() {
        try {
            val mmocorePlugin = Bukkit.getPluginManager().getPlugin("MMOCore")
            if (mmocorePlugin == null) {
                Bukkit.getLogger().info("[MidgardDungeon] MMOCore não encontrado — integração desativada.")
                return
            }

            // PlayerData
            playerDataClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData")
            getPlayerDataMethod = playerDataClass!!.getMethod("get", UUID::class.java)
            getLevelMethod = playerDataClass!!.getMethod("getLevel")
            getExperienceMethod = playerDataClass!!.getMethod("getExperience")

            // Classe do jogador (getProfess() retorna PlayerClass)
            val getProfessMethod = playerDataClass!!.getMethod("getProfess")
            val playerClassClass = getProfessMethod.returnType
            professionGetNameMethod = playerClassClass.getMethod("getName")
            getClassNameMethod = getProfessMethod

            // Experiência — EXPSource enum
            expSourceClass = Class.forName("net.Indyuce.mmocore.api.player.EXPSource")
            expSourceOther = java.lang.Enum.valueOf(
                expSourceClass as Class<out Enum<*>>,
                "OTHER"
            ) as Any

            // giveExperience(double, EXPSource)
            giveExperienceMethod = playerDataClass!!.getMethod(
                "giveExperience",
                Double::class.java,
                expSourceClass
            )

            // Party
            try {
                getPartyMethod = playerDataClass!!.getMethod("getParty")
            } catch (_: NoSuchMethodException) {
                // Party pode não estar disponível em todas as versões
            }

            // Atributos
            try {
                getAttributeMethod = playerDataClass!!.getMethod("getAttributes")
            } catch (_: NoSuchMethodException) {}

            available = true
            Bukkit.getLogger().info("[MidgardDungeon] MMOCore integrado com sucesso!")
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[MidgardDungeon] Erro ao integrar MMOCore: ${e.message}")
            available = false
        }
    }

    fun isAvailable(): Boolean = available

    fun shutdown() {
        playerDataClass = null
        getPlayerDataMethod = null
        getLevelMethod = null
        getClassNameMethod = null
        giveExperienceMethod = null
        getExperienceMethod = null
        getAttributeMethod = null
        getPartyMethod = null
        expSourceClass = null
        expSourceOther = null
        professionGetNameMethod = null
        available = false
    }

    private fun getPlayerData(playerId: UUID): Any? {
        if (!available) return null
        return try {
            getPlayerDataMethod?.invoke(null, playerId)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retorna o nível do jogador no MMOCore.
     */
    fun getLevel(player: Player): Int {
        if (!available) return 0
        return try {
            val data = getPlayerData(player.uniqueId) ?: return 0
            getLevelMethod?.invoke(data) as? Int ?: 0
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Retorna o nome da classe do jogador no MMOCore.
     */
    fun getClassName(player: Player): String {
        if (!available) return ""
        return try {
            val data = getPlayerData(player.uniqueId) ?: return ""
            val playerClass = getClassNameMethod?.invoke(data) ?: return ""
            professionGetNameMethod?.invoke(playerClass) as? String ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Retorna a experiência atual do jogador no MMOCore.
     */
    fun getExperience(player: Player): Double {
        if (!available) return 0.0
        return try {
            val data = getPlayerData(player.uniqueId) ?: return 0.0
            getExperienceMethod?.invoke(data) as? Double ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    /**
     * Concede experiência ao jogador via MMOCore.
     */
    fun giveExperience(player: Player, amount: Double): Boolean {
        if (!available || amount <= 0) return false
        return try {
            val data = getPlayerData(player.uniqueId) ?: return false
            giveExperienceMethod?.invoke(data, amount, expSourceOther)
            true
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[MidgardDungeon] Erro ao dar EXP MMOCore: ${e.message}")
            false
        }
    }

    /**
     * Concede experiência com mensagem no chat.
     */
    fun rewardExperience(player: Player, amount: Double, reason: String = "") {
        if (!available || amount <= 0) return
        if (giveExperience(player, amount)) {
            val msg = if (reason.isNotEmpty()) {
                Component.text("+${amount.toInt()} EXP MMOCore ($reason)", NamedTextColor.GREEN)
            } else {
                Component.text("+${amount.toInt()} EXP MMOCore", NamedTextColor.GREEN)
            }
            player.sendMessage(msg)
        }
    }

    /**
     * Verifica se o jogador tem o nível mínimo no MMOCore.
     */
    fun hasMinLevel(player: Player, minLevel: Int): Boolean {
        if (!available) return true // Se MMOCore não está disponível, permite
        return getLevel(player) >= minLevel
    }

    /**
     * Verifica se o jogador é da classe especificada.
     */
    fun hasClass(player: Player, className: String): Boolean {
        if (!available || className.isEmpty()) return true
        return getClassName(player).equals(className, ignoreCase = true)
    }

    /**
     * Verifica se o jogador está em um party do MMOCore.
     */
    fun isInParty(player: Player): Boolean {
        if (!available || getPartyMethod == null) return false
        return try {
            val data = getPlayerData(player.uniqueId) ?: return false
            val party = getPartyMethod?.invoke(data)
            party != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Retorna os membros do party do MMOCore.
     */
    fun getPartyMembers(player: Player): List<UUID> {
        if (!available || getPartyMethod == null) return emptyList()
        return try {
            val data = getPlayerData(player.uniqueId) ?: return emptyList()
            val party = getPartyMethod?.invoke(data) ?: return emptyList()
            val getMembersMethod = party.javaClass.getMethod("getMembers")
            val members = getMembersMethod.invoke(party)
            if (members is Collection<*>) {
                members.mapNotNull { memberData ->
                    val getUUIDMethod = memberData?.javaClass?.getMethod("getUniqueId")
                    getUUIDMethod?.invoke(memberData) as? UUID
                }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
