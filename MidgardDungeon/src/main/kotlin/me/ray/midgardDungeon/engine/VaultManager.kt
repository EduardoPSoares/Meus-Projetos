package me.ray.midgardDungeon.engine

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

/**
 * Integração opcional com Vault para economia.
 * Detecta Vault automaticamente via ServiceProvider.
 */
object VaultManager {

    private var economy: Any? = null
    private var economyClass: Class<*>? = null
    private var depositMethod: java.lang.reflect.Method? = null
    private var withdrawMethod: java.lang.reflect.Method? = null
    private var balanceMethod: java.lang.reflect.Method? = null
    private var hasMethod: java.lang.reflect.Method? = null

    @Volatile
    private var available = false

    fun initialize() {
        try {
            val vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault")
            if (vaultPlugin == null) {
                Bukkit.getLogger().info("[MidgardDungeon] Vault não encontrado — economia desativada.")
                return
            }

            val ecoClass = Class.forName("net.milkbowl.vault.economy.Economy")
            economyClass = ecoClass
            val rsp = Bukkit.getServicesManager().getRegistration(ecoClass)
            if (rsp == null) {
                Bukkit.getLogger().warning("[MidgardDungeon] Vault encontrado, mas nenhum provedor de economia registrado.")
                return
            }

            economy = rsp.provider
            // Cache dos métodos via reflection
            val offlinePlayerClass = org.bukkit.OfflinePlayer::class.java
            depositMethod = ecoClass.getMethod("depositPlayer", offlinePlayerClass, Double::class.java)
            withdrawMethod = ecoClass.getMethod("withdrawPlayer", offlinePlayerClass, Double::class.java)
            balanceMethod = ecoClass.getMethod("getBalance", offlinePlayerClass)
            hasMethod = ecoClass.getMethod("has", offlinePlayerClass, Double::class.java)

            available = true
            Bukkit.getLogger().info("[MidgardDungeon] Vault integrado com sucesso!")
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[MidgardDungeon] Erro ao integrar Vault: ${e.message}")
            available = false
        }
    }

    fun isAvailable(): Boolean = available

    fun deposit(player: Player, amount: Double): Boolean {
        if (!available || economy == null) return false
        return try {
            val result = depositMethod?.invoke(economy, player as org.bukkit.OfflinePlayer, amount)
            val successMethod = result?.javaClass?.getMethod("transactionSuccess")
            successMethod?.invoke(result) as? Boolean ?: false
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[MidgardDungeon] Erro ao depositar: ${e.message}")
            false
        }
    }

    fun withdraw(player: Player, amount: Double): Boolean {
        if (!available || economy == null) return false
        return try {
            val result = withdrawMethod?.invoke(economy, player as org.bukkit.OfflinePlayer, amount)
            val successMethod = result?.javaClass?.getMethod("transactionSuccess")
            successMethod?.invoke(result) as? Boolean ?: false
        } catch (e: Exception) {
            Bukkit.getLogger().warning("[MidgardDungeon] Erro ao sacar: ${e.message}")
            false
        }
    }

    fun getBalance(player: Player): Double {
        if (!available || economy == null) return 0.0
        return try {
            balanceMethod?.invoke(economy, player as org.bukkit.OfflinePlayer) as? Double ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    fun has(player: Player, amount: Double): Boolean {
        if (!available || economy == null) return false
        return try {
            hasMethod?.invoke(economy, player as org.bukkit.OfflinePlayer, amount) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Recompensa um jogador com moedas e envia mensagem no chat.
     */
    fun reward(player: Player, amount: Double, reason: String = "") {
        if (!available || amount <= 0) return
        if (deposit(player, amount)) {
            val msg = if (reason.isNotEmpty()) {
                Component.text("+${"%.2f".format(amount)} moedas ($reason)", NamedTextColor.GOLD)
            } else {
                Component.text("+${"%.2f".format(amount)} moedas", NamedTextColor.GOLD)
            }
            player.sendMessage(msg)
        }
    }

    /**
     * Cobra uma taxa de entrada de um jogador.
     * Retorna true se conseguiu cobrar, false se saldo insuficiente.
     */
    fun charge(player: Player, amount: Double): Boolean {
        if (!available || amount <= 0) return true // Sem cobrança
        if (!has(player, amount)) {
            player.sendMessage(
                Component.text("Saldo insuficiente! Precisa de ${"%.2f".format(amount)} moedas.", NamedTextColor.RED)
            )
            return false
        }
        return withdraw(player, amount)
    }

    fun shutdown() {
        economy = null
        economyClass = null
        depositMethod = null
        withdrawMethod = null
        balanceMethod = null
        hasMethod = null
        available = false
    }
}
