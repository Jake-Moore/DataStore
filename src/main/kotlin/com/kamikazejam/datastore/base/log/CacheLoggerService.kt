package com.kamikazejam.datastore.base.log

import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.base.Cache
import org.bukkit.plugin.Plugin

open class CacheLoggerService(protected val cache: Cache<*, *>) : LoggerService() {
    override val isDebug: Boolean
        get() = DataStoreAPI.isDebug()

    override val loggerName: String
        get() = "C: " + cache.name

    override val plugin: Plugin
        get() = cache.plugin
}
