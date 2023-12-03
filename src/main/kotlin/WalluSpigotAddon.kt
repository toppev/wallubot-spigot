package com.wallubot.addons.spigot

import com.squareup.moshi.adapter
import com.wallubot.addons.apis.DefaultApi
import com.wallubot.addons.infrastructure.ApiClient
import com.wallubot.addons.infrastructure.Serializer
import com.wallubot.addons.models.*
import com.wallubot.addons.models.OnMessageRequestBodyConfiguration.EmojiType
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

var DEBUG = false
val PREFIX = "${ChatColor.GRAY}[${ChatColor.GREEN}Wallu${ChatColor.GRAY}] "
val DEBUG_PREFIX: String = ChatColor.stripColor(PREFIX) + "debug: "
val hexPattern = Regex("#[a-fA-F\\d]{6}")

@Suppress("unused")
class WalluSpigotAddon : JavaPlugin(), CommandExecutor, Listener {

    private lateinit var updateChecker: UpdateChecker
    private val api: DefaultApi by lazy { DefaultApi() }

    override fun onEnable() {
        super.onEnable()
        saveDefaultConfig()
        getCommand("wallu").executor = this
        initializePlugin()
        updateChecker = UpdateChecker(this)
//        try {
//            val metrics = Metrics(this, TODO())
//        } catch (e: Exception) {
//            Bukkit.getLogger().warning("Failed to start bStats metrics...")
//            e.printStackTrace()
//        }
    }

    private fun initializePlugin() {
        // (re)loading the config
        Bukkit.getScheduler().cancelTasks(this)
        HandlerList.unregisterAll(this as Plugin)
        Bukkit.getPluginManager().registerEvents(this, this)
        ApiClient.apiKey["X-API-Key"] = config.getString("api-key")
    }


    private fun reloadPluginConfig() {
        Bukkit.getLogger().info("Reloading config...")
        reloadConfig()
        initializePlugin()
        Bukkit.getLogger().info("Plugin config reloaded successfully.")
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val admin = sender.isOp || sender.hasPermission("wallubot.admin")
        debug { "messageplayer command: ${args.joinToString(" ")}" }
        if (args.firstOrNull() == "messageplayer" && admin) {
            debug { "messageplayer command: ${args.joinToString(" ")}" }
            // Internal command (default command)
            val target = Bukkit.getPlayer(args.getOrNull(1))
            target?.sendMessage(args.drop(2).joinToString(" ").translateColors())
            return true
        }
        if (!admin && DEBUG) Bukkit.getLogger().info("No permission: wallubot.admin")
        if (admin && args.isNotEmpty()) {
            if (arrayOf("reload", "rl").contains(args[0].lowercase())) {
                reloadPluginConfig()
                sender.sendMessage("$PREFIX${ChatColor.GREEN}Config reloaded!")
                return true
            }
            if (args[0].equals("debug", true)) {
                DEBUG = !DEBUG
                if (DEBUG) {
                    sender.sendMessage("$PREFIX${ChatColor.RED}Debug logging enabled")
                } else {
                    sender.sendMessage("${PREFIX}Debug logging disabled")
                }
                Bukkit.getLogger().info("Wallu debug mode set to $DEBUG")
                return true
            }
            if (args[0].equals("update", true)) {
                CompletableFuture.runAsync {
                    sender.sendMessage("${ChatColor.GRAY}Checking for updates...")
                    try {
                        updateChecker.checkForUpdates()
                        val messages = updateChecker.joinMessages
                        if (messages.isEmpty()) {
                            sender.sendMessage("${ChatColor.GREEN}No updates found.")
                        } else {
                            messages.forEach { sender.sendMessage(it) }
                        }
                    } catch (e: Exception) {
                        sender.sendMessage("${ChatColor.RED}Failed to check for updates. Check console.")
                        sender.sendMessage("${ChatColor.GRAY}${e.message}")
                        e.printStackTrace()
                    }
                }
                return true
            }
        }
        sender.sendMessage("${PREFIX}${ChatColor.GOLD}WallubotSpigot ${description.version} - ${this.description.description}")
        if (admin) {
            sender.sendMessage("${PREFIX}${ChatColor.YELLOW}/wallu reload${ChatColor.GRAY} - reload the config")
            sender.sendMessage("${PREFIX}${ChatColor.YELLOW}/wallu debug${ChatColor.GRAY} - toggle debug logging")
            sender.sendMessage("${PREFIX}${ChatColor.YELLOW}/wallu update${ChatColor.GRAY} - check for updates")
            if (System.currentTimeMillis() - updateChecker.lastChecked > TimeUnit.MINUTES.toMillis(10)) {
                CompletableFuture.runAsync { updateChecker.checkForUpdates() }
            }
        }
        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onChat(event: AsyncPlayerChatEvent) {
        if (!event.isAsynchronous) {
            Bukkit.getLogger().warning("$PREFIX${ChatColor.RED}Chat event is not asynchronous and will be ignored!")
            return
        }
        // We don't want to block other events (afaik, even these async methods are in order)
        Bukkit.getScheduler().runTaskAsynchronously(this) {
            try {
                val debugId = "(${event.player.name}) ${event.message}"
                debug { "$debugId -> received" }

                val apiKey = config.getString("api-key")
                val validApiKey = apiKey.length > 10
                if (!validApiKey) {
                    Bukkit.getLogger().warning("$PREFIX${ChatColor.RED}Invalid API key. Please set it in config.yml")
                    return@runTaskAsynchronously
                }

                val body = OnMessageRequestBody(
                    addon = OnMessageRequestBodyAddon(
                        name = description.name,
                        version = description.version,
                    ),
                    user = OnMessageRequestBodyUser(
                        id = event.player.uniqueId.toString(),
                        username = event.player.displayName,
                        isStaffMember = event.player.isOp || event.player.hasPermission("wallubot.admin"),
                    ),
                    channel = OnMessageRequestBodyChannel(
                        id = "spigot-${Bukkit.getServer().worlds.first().seed}",
                        name = config.getString("server-name")
                    ),
                    message = OnMessageRequestBodyMessage(
                        id = UUID.randomUUID().toString(),
                        content = event.message,
                        // E.g., if we private messages the bot (API checks if the bot's name is mentioned)
                        isBotMentioned = false
                    ),
                    configuration = OnMessageRequestBodyConfiguration(
                        emojiType = EmojiType.NONE
                    )
                )
                debug { "$debugId -> sending request: $body" }
                debug { "$debugId -> json: ${Serializer.moshi.adapter<OnMessageRequestBody>().toJson(body)}" }
                val st = System.currentTimeMillis()
                val res = api.onMessagePost(body)
                debug { "$debugId -> response: $res (took ${System.currentTimeMillis() - st}ms)" }
                Bukkit.getScheduler().runTask(this) {
                    res.response?.message?.toString()?.split("\n")
                        ?.filter { it.isNotBlank() }
                        ?.forEach { msg ->
                            debug { "$debugId -> sending message part: $msg" }
                            config.getStringList("response-commands").forEach { command ->
                                Bukkit.dispatchCommand(
                                    Bukkit.getConsoleSender(),
                                    command
                                        .replace("<player>", event.player.name)
                                        .replace("<message>", msg)
                                        .translateColors()
                                )
                            }
                        }
                }
            } catch (e: Exception) {
                Bukkit.getLogger().warning("$PREFIX${ChatColor.RED}Failed to handle chat event: ${e.message}")
                if (DEBUG) e.printStackTrace()
            }
        }
    }
}


fun String.translateColors(): String {
    var translated = this
    hexPattern.findAll(this).forEach { matchResult ->
        val builder = StringBuilder()
        matchResult.value.replace('#', 'x').toCharArray().forEach { builder.append("§").append(it) }
        translated = translated.replace(matchResult.value, builder.toString())
    }
    return ChatColor.translateAlternateColorCodes('&', translated)
}

inline fun debug(message: () -> String) {
    if (DEBUG) {
        Bukkit.getLogger().info(DEBUG_PREFIX + message.invoke())
    }
}
