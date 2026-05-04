package io.github.Earth1283.fixLag.managers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStreamReader

class MessageManager(private val plugin: JavaPlugin) {

    private lateinit var messagesConfig: FileConfiguration
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()
    private val legacySerializer: LegacyComponentSerializer = LegacyComponentSerializer.legacySection()

    init {
        loadMessages()
    }

    fun loadMessages() {
        val messagesFile = File(plugin.dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile)

        // Update messages with missing keys
        try {
            val defConfigStream = plugin.getResource("messages.yml")
            if (defConfigStream != null) {
                val defConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defConfigStream))
                messagesConfig.setDefaults(defConfig)
                messagesConfig.options().copyDefaults(true)
                messagesConfig.save(messagesFile)
            }
        } catch (e: Exception) {
            plugin.logger.warning("Could not update messages.yml: ${e.message}")
        }
    }

    fun getMessage(key: String, vararg replacements: String): String {
        return getMessage(key, true, *replacements)
    }

    fun getMessage(key: String, includePrefix: Boolean, vararg replacements: String): String {
        var raw = (messagesConfig.getString(key) ?: "Error: Message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)

        if (includePrefix) {
            val prefixRaw = messagesConfig.getString("prefix") ?: "<gray>[<green>FixLag<gray>] <reset>"
            raw = prefixRaw + raw
        }
        val comp = miniMessage.deserialize(raw)
        return legacySerializer.serialize(comp)
    }

    fun getComponentMessage(key: String, vararg replacements: String): Component {
        return getComponentMessage(key, true, *replacements)
    }

    fun getComponentMessage(key: String, includePrefix: Boolean, vararg replacements: String): Component {
        var raw = (messagesConfig.getString(key) ?: "Error: Message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)

        if (includePrefix) {
            val prefixRaw = messagesConfig.getString("prefix") ?: "<gray>[<green>FixLag<gray>] <reset>"
            raw = prefixRaw + raw
        }
        return miniMessage.deserialize(raw)
    }

    fun getLogMessage(key: String, vararg replacements: String): String {
        val raw = (messagesConfig.getString(key) ?: "Error: Log message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)
        val comp = miniMessage.deserialize(raw)
        return PlainTextComponentSerializer.plainText().serialize(comp)
    }

    fun logInfo(key: String, vararg replacements: String) {
        val raw = (messagesConfig.getString(key) ?: "Error: Log message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)
        plugin.componentLogger.info(miniMessage.deserialize(raw))
    }

    fun logWarn(key: String, vararg replacements: String) {
        val raw = (messagesConfig.getString(key) ?: "Error: Log message key '$key' not found in messages.yml")
            .applyReplacements(*replacements)
        plugin.componentLogger.warn(miniMessage.deserialize(raw))
    }

    private fun String.applyReplacements(vararg replacements: String): String {
        var result = this
        for (i in replacements.indices step 2) {
            if (i + 1 < replacements.size) {
                result = result.replace(replacements[i], replacements[i + 1])
            }
        }
        return result
    }

    fun getRawMessage(key: String): String {
        return messagesConfig.getString(key) ?: ""
    }
}
