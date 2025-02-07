package com.kamikazejam.datastore

import com.kamikazejam.datastore.base.coroutine.GlobalDataStoreScope
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.log.PluginLogger
import com.kamikazejam.datastore.base.mode.StorageMode
import com.kamikazejam.datastore.command.DataStoreCommand
import com.kamikazejam.datastore.connections.storage.StorageService
import com.kamikazejam.datastore.store.profile.listener.ProfileListener
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
    suspend fun onEnable(pl: JavaPlugin): Boolean {
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
        pl.getCommand("datastore").executor = DataStoreCommand()

        // Register ProfileListener
        pl.server.pluginManager.registerEvents(ProfileListener(), pl)

        onEnableTime = System.currentTimeMillis()
        return true
    }

    /**
     * @return true IFF this call triggered the singleton disable sequence, false it already disabled
     */
    suspend fun onDisable(): Boolean {
        if (!enabled) {
            return false
        }

        // Shutdown Services
        storageMode.disableServices()

        // Wait for all Coroutines to Finish
        colorLogger.info("&aWaiting for all coroutines to finish...")
        runBlocking {
            try {
                withTimeout(60.toDuration(DurationUnit.SECONDS)) {
                    GlobalDataStoreScope.awaitAllChildrenCompletion(colorLogger)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                colorLogger.severe("&cFailed to wait for all coroutines to finish!")
                GlobalDataStoreScope.logActiveCoroutines()
            }
            GlobalDataStoreScope.cancelAll()
        }
        colorLogger.info("&aAll coroutines finished!")

        // Ensure all Collections are shutdown
        if (DataStoreAPI.registrations.isNotEmpty()) {
            // This should be safe (although not ideal) since any plugin depending on DataStore should be disabled before this
            DataStoreAPI.registrations.forEach {
                colorLogger.warning("DataStoreRegistration for ${it.databaseName} (backed by plugin '${it.plugin.name}') was not shutdown before plugin disable.")
                colorLogger.warning("Manually shutting it down! (PLEASE CONTACT AUTHORS OF ${it.plugin.name} TO FIX THIS)")
                runBlocking { it.shutdown() }
            }
            DataStoreAPI.registrations.clear()
        }

        // Set to disabled
        val prev = enabled
        enabled = false
        return prev
    }

    fun get(): JavaPlugin {
        return pluginSource ?: throw RuntimeException("Plugin source not set")
    }

    fun info(msg: String) {
        val pl = pluginSource
        if (pl == null) {
            println("[INFO] $msg")
        } else {
            pl.logger.info(msg)
        }
    }

    fun warning(msg: String) {
        val pl = pluginSource
        if (pl == null) {
            println("[WARNING] $msg")
        } else {
            pl.logger.warning(msg)
        }
    }

    fun error(msg: String) {
        val pl = pluginSource
        if (pl == null) {
            println("[ERROR] $msg")
        } else {
            pl.logger.severe(msg)
        }
    }

    // KamiConfig access of
    private var storeCfg: FileConfiguration? = null
    val config: FileConfiguration
        get() {
            val plugin = get()
            storeCfg?.let { return it }
            val cfg = createConfig(plugin)
            storeCfg = cfg
            return cfg
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
