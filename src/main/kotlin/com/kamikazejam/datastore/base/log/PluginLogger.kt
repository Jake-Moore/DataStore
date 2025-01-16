package com.kamikazejam.datastore.base.log

import com.kamikazejam.datastore.DataStoreSource
import org.bukkit.plugin.Plugin

class PluginLogger : LoggerService() {
    override val loggerName: String
        get() = "DataStore"

    override val plugin: Plugin
        get() = DataStoreSource.get()

    override val isDebug: Boolean
        get() = DataStoreSource.isDebug()
}
