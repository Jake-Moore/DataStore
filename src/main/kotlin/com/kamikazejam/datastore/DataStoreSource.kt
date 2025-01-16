package com.kamikazejam.datastore

import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.log.PluginLogger
import com.kamikazejam.datastore.base.mode.StorageMode
import com.kamikazejam.datastore.command.DataStoreCommand
import com.kamikazejam.datastore.connections.storage.StorageService
import com.kamikazejam.datastore.mode.profile.listener.ProfileListener
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

@Suppress("unused", "MemberVisibilityCanBePrivate")
object DataStoreSource {
    private const val STORE_CONFIG_NAME = "datastore.yml"
    val colorLogger: LoggerService = PluginLogger()
    private var pluginSource: JavaPlugin? = null
    private var enabled = false

    var onEnableTime: Long = 0

    // Modes
    var storageMode: StorageMode = StorageMode.MONGODB

    // Server Identification
    var storeDbPrefix: String? = null

    /**
     * @return true IFF a plugin source was NEEDED and used for registration
     */
    fun onEnable(pl: JavaPlugin): Boolean {
        if (enabled) {
            return false
        }
        pluginSource = pl
        enabled = true

        // ----------------------------- //
        //      DataStore onEnable      //
        // ----------------------------- //
        // Load Plugin Modes
        storageMode = StorageMode.MONGODB // For now, if we add more db types, we can change this
        info("Running in $storageMode storage mode.")

        // Load DataStore prefix
        storeDbPrefix = config.getString("datastore-database-prefix", "global")

        // Enable Services
        storageMode.enableServices()

        // Load Commands
        pluginSource!!.getCommand("datastore").executor = DataStoreCommand()

        // Register ProfileListener
        pl.server.pluginManager.registerEvents(ProfileListener(), pl)

        onEnableTime = System.currentTimeMillis()
        return true
    }

    /**
     * @return true IFF this call triggered the singleton disable sequence, false it already disabled
     */
    fun onDisable(): Boolean {
        if (!enabled) {
            return false
        }

        // Shutdown Services
        storageMode.disableServices()

        // Set to disabled
        val prev = enabled
        enabled = false
        return prev
    }

    fun get(): JavaPlugin {
        if (pluginSource == null) {
            throw RuntimeException("Plugin source not set")
        }
        return pluginSource!!
    }

    fun info(msg: String) {
        if (pluginSource == null) {
            println("[INFO] $msg")
        } else {
            pluginSource!!.logger.info(msg)
        }
    }

    fun warning(msg: String) {
        if (pluginSource == null) {
            println("[WARNING] $msg")
        } else {
            pluginSource!!.logger.warning(msg)
        }
    }

    fun error(msg: String) {
        if (pluginSource == null) {
            println("[ERROR] $msg")
        } else {
            pluginSource!!.logger.severe(msg)
        }
    }

    // KamiConfig access of
    private var storeCfg: FileConfiguration? = null
    val config: FileConfiguration
        get() {
            val plugin = get()
            if (storeCfg == null) {
                storeCfg = createConfig(plugin)
            }
            return storeCfg!!
        }

    private fun createConfig(plugin: JavaPlugin): FileConfiguration {
        val file = File(plugin.dataFolder, STORE_CONFIG_NAME)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            plugin.saveResource(STORE_CONFIG_NAME, false)
        }
        val config: FileConfiguration = YamlConfiguration()
        config.load(file)
        return config
    }

    /**
     * @return If the plugin has debug logging enabled
     */
    fun isDebug(): Boolean {
        return config.getBoolean("debug", false)
    }

    val storageService: StorageService
        // --------------------------------------------------------------------------------------- //
        get() = storageMode.storageService
}
