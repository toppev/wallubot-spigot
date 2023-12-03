package com.wallubot.addons.spigot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Type
import java.net.URL
import java.util.concurrent.TimeUnit

private const val UPDATE_URL = "https://wallubot.com/addons/wallubot-spigot/version.json"
private val MAP_TYPE: Type = object : TypeToken<Map<String?, String?>>() {}.type

class UpdateChecker(private val plugin: JavaPlugin) : Listener {

    val joinMessages = mutableSetOf<String>()
    var lastChecked: Long = 0

    init {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, {
            try {
                checkForUpdates()
            } catch (e: Exception) {
                Bukkit.getLogger().warning("Wallu failed to check for updates.")
                if (DEBUG) e.printStackTrace()
            }
        }, 0, TimeUnit.HOURS.toSeconds(12) * 20)
    }

    /**
     * Checks for updates.
     * WARNING: this is blocking!!
     */
    fun checkForUpdates() {
        Bukkit.getLogger().info("Checking for wallubot-spigot updates...")
        val content = URL(UPDATE_URL).readText()
        val json: Map<String, String> = Gson().fromJson(content, MAP_TYPE)
        val latestVer = json["version"] ?: throw Exception("No 'version' in response: $content")
        val currentVer = plugin.description.version
        if (latestVer > currentVer) {
            joinMessages.clear()
            joinMessages.apply {
                add("There's a new update available: https://github.com/toppev/wallubot-spigot/releases")
                add("You're on $currentVer and the latest version is $latestVer.")
                // Filter updates since current version
                json.filter { it.key.startsWith("message-v") && it.key > "message-v$currentVer" }
                    .toSortedMap()
                    .forEach {
                        val ver = it.key.substringAfter("message-")
                        add("$PREFIX$ver: ${it.value.translateColors()}")
                    }
                forEach { Bukkit.getLogger().info(ChatColor.stripColor(it)) }
            }
        }
        lastChecked = System.currentTimeMillis()
    }


    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val p = event.player
        if (p.isOp || p.hasPermission("wallubot.admin")) {
            joinMessages.forEach { p.sendMessage(it) }
        }
    }

}