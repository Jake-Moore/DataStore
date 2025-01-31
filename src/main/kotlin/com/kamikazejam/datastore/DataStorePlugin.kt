package com.kamikazejam.datastore

import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

/**
 * This class is nothing more than a loader for all DataStore logic
 * It supplies [DataStoreSource] with this plugin object so that DataStore can be initialized
 * DataStore can be shaded into your own project, where you'll just have to mirror these method
 * calls in your own plugin, to initialize DataStore
 */
@Suppress("unused")
class DataStorePlugin : JavaPlugin() {
    override fun onEnable() {
        runBlocking { DataStoreSource.onEnable(this@DataStorePlugin) }
    }

    override fun onDisable() {
        runBlocking { DataStoreSource.onDisable() }
    }
}
