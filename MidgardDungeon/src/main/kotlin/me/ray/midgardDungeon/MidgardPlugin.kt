package me.ray.midgardDungeon

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

/**
 * Mantém uma referência segura à instância do plugin Typewriter.
 * Inicializado uma vez quando o MidgardDungeon é iniciado.
 */
object MidgardPlugin {
    var instance: Plugin? = null
        private set

    fun init() {
        instance = Bukkit.getPluginManager().getPlugin("Typewriter")
    }

    fun clear() {
        instance = null
    }
}

