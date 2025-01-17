package com.kamikazejam.datastore.base.log

import com.kamikazejam.datastore.DataStoreAPI
import com.kamikazejam.datastore.base.Collection
import org.bukkit.plugin.Plugin

open class CollectionLoggerService(protected val collection: Collection<*, *>) : LoggerService() {
    override val isDebug: Boolean
        get() = DataStoreAPI.isDebug()

    override val loggerName: String
        get() = "C: " + collection.name

    override val plugin: Plugin
        get() = collection.plugin
}
