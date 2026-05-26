package me.ray.midgardDungeon.engine

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import java.util.*

/**
 * Integração opcional com MythicMobs para spawnar mobs customizados.
 * Detecta MythicMobs automaticamente via reflection.
 * Centraliza toda a lógica de spawn para evitar duplicação.
 */
object MythicMobsManager {

    @Volatile
    private var available = false

    // Instância do MythicBukkit
    private var mythicBukkitClass: Class<*>? = null
    private var mythicInstance: Any? = null

    // MobManager
    private var mobManager: Any? = null
    private var spawnMobMethod: java.lang.reflect.Method? = null
    private var spawnMobWithLevelMethod: java.lang.reflect.Method? = null

    // APIHelper
    private var apiHelper: Any? = null
    private var isMythicMobMethod: java.lang.reflect.Method? = null
    private var getMythicMobInstanceMethod: java.lang.reflect.Method? = null

    // ActiveMob
    private var getEntityMethod: java.lang.reflect.Method? = null
    private var getBukkitEntityMethod: java.lang.reflect.Method? = null
    private var getActiveMobUUIDMethod: java.lang.reflect.Method? = null

    // BukkitAPIHelper
    private var apiHelperClass: Class<*>? = null

    fun initialize() {
        try {
            val mmPlugin = Bukkit.getPluginManager().getPlugin("MythicMobs")
            if (mmPlugin == null) {
                Bukkit.getLogger().info("[MidgardDungeon] MythicMobs não encontrado — integração desativada.")
                return
            }

            // MythicBukkit.inst()
            mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit")
            val instMethod = mythicBukkitClass!!.getMethod("inst")
            mythicInstance = instMethod.invoke(null)

            // MobManager
            val getMobManagerMethod = mythicBukkitClass!!.getMethod("getMobManager")
            mobManager = getMobManagerMethod.invoke(mythicInstance)

            // spawnMob(String, Location) -> ActiveMob
            spawnMobMethod = mobManager!!.javaClass.getMethod(
                "spawnMob",
                String::class.java,
                Location::class.java
            )

            // spawnMob(String, Location, double) -> ActiveMob (com nível)
            try {
                spawnMobWithLevelMethod = mobManager!!.javaClass.getMethod(
                    "spawnMob",
                    String::class.java,
                    Location::class.java,
                    Double::class.java
                )
            } catch (_: NoSuchMethodException) {
                // Versão sem suporte a nível no spawn
            }

            // BukkitAPIHelper
            try {
                apiHelperClass = Class.forName("io.lumine.mythic.bukkit.BukkitAPIHelper")
                val getAPIHelperMethod = mythicBukkitClass!!.getMethod("getAPIHelper")
                apiHelper = getAPIHelperMethod.invoke(mythicInstance)

                isMythicMobMethod = apiHelperClass!!.getMethod("isMythicMob", Entity::class.java)
                getMythicMobInstanceMethod = apiHelperClass!!.getMethod("getMythicMobInstance", Entity::class.java)
            } catch (_: Exception) {
                // APIHelper pode não estar disponível em todas as versões
            }

            available = true
            Bukkit.getLogger().info("[MidgardDungeon] MythicMobs integrado com sucesso!")
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[MidgardDungeon] Erro ao integrar MythicMobs: ${e.message}")
            available = false
        }
    }

    fun isAvailable(): Boolean = available

    fun shutdown() {
        mythicBukkitClass = null
        mythicInstance = null
        mobManager = null
        spawnMobMethod = null
        spawnMobWithLevelMethod = null
        apiHelper = null
        isMythicMobMethod = null
        getMythicMobInstanceMethod = null
        getEntityMethod = null
        getBukkitEntityMethod = null
        getActiveMobUUIDMethod = null
        apiHelperClass = null
        available = false
    }

    /**
     * Spawna um mob MythicMobs pelo ID.
     * Retorna a entidade Bukkit ou null se falhar.
     */
    fun spawnMob(mobId: String, location: Location): Entity? {
        if (!available || mobId.isEmpty()) return null
        return try {
            val activeMob = spawnMobMethod?.invoke(mobManager, mobId, location) ?: return null
            extractBukkitEntity(activeMob)
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[MidgardDungeon] Erro ao spawnar MythicMob '$mobId': ${e.message}")
            null
        }
    }

    /**
     * Spawna um mob MythicMobs pelo ID com nível específico.
     * Retorna a entidade Bukkit ou null se falhar.
     */
    fun spawnMob(mobId: String, location: Location, level: Double): Entity? {
        if (!available || mobId.isEmpty()) return null
        return try {
            val method = spawnMobWithLevelMethod ?: return spawnMob(mobId, location)
            val activeMob = method.invoke(mobManager, mobId, location, level) ?: return null
            extractBukkitEntity(activeMob)
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[MidgardDungeon] Erro ao spawnar MythicMob '$mobId' (nível $level): ${e.message}")
            null
        }
    }

    /**
     * Verifica se uma entidade é um mob MythicMobs.
     */
    fun isMythicMob(entity: Entity): Boolean {
        if (!available || isMythicMobMethod == null) return false
        return try {
            isMythicMobMethod?.invoke(apiHelper, entity) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Retorna o ID interno do MythicMob de uma entidade.
     */
    fun getMythicMobId(entity: Entity): String? {
        if (!available || getMythicMobInstanceMethod == null) return null
        return try {
            val activeMob = getMythicMobInstanceMethod?.invoke(apiHelper, entity) ?: return null
            val getTypeMethod = activeMob.javaClass.getMethod("getMobType")
            val mobType = getTypeMethod.invoke(activeMob) ?: return null
            mobType.toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retorna o nível de um MythicMob.
     */
    fun getMythicMobLevel(entity: Entity): Double {
        if (!available || getMythicMobInstanceMethod == null) return 0.0
        return try {
            val activeMob = getMythicMobInstanceMethod?.invoke(apiHelper, entity) ?: return 0.0
            val getLevelMethod = activeMob.javaClass.getMethod("getLevel")
            getLevelMethod.invoke(activeMob) as? Double ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    /**
     * Executa uma skill do MythicMobs em uma entidade.
     */
    fun castSkill(entity: Entity, skillName: String): Boolean {
        if (!available) return false
        return try {
            val mmClass = mythicBukkitClass ?: return false
            val getSkillManagerMethod = mmClass.getMethod("getSkillManager")
            val skillManager = getSkillManagerMethod.invoke(mythicInstance) ?: return false
            // Tenta usar a API de skills — varia entre versões
            val apiHelperInst = apiHelper ?: return false
            val castSkillMethod = apiHelperInst.javaClass.getMethod(
                "castSkill",
                Entity::class.java,
                String::class.java
            )
            castSkillMethod.invoke(apiHelperInst, entity, skillName) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extrai a entidade Bukkit de um ActiveMob do MythicMobs.
     */
    private fun extractBukkitEntity(activeMob: Any): Entity? {
        return try {
            val getEntityMeth = activeMob.javaClass.getMethod("getEntity")
            val entityWrapper = getEntityMeth.invoke(activeMob) ?: return null
            val getBukkitMeth = entityWrapper.javaClass.getMethod("getBukkitEntity")
            getBukkitMeth.invoke(entityWrapper) as? Entity
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retorna uma lista de todos os IDs de MythicMobs registrados.
     */
    fun getRegisteredMobIds(): List<String> {
        if (!available) return emptyList()
        return try {
            val getMobNamesMethod = mobManager?.javaClass?.getMethod("getMobNames")
            val names = getMobNamesMethod?.invoke(mobManager)
            if (names is Collection<*>) {
                names.filterIsInstance<String>()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Verifica se um ID de MythicMob existe.
     */
    fun mobExists(mobId: String): Boolean {
        if (!available || mobId.isEmpty()) return false
        return getRegisteredMobIds().any { it.equals(mobId, ignoreCase = true) }
    }
}
