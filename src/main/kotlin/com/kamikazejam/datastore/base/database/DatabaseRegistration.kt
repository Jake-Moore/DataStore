package com.kamikazejam.datastore.base.database

import org.bukkit.plugin.Plugin

data class DatabaseRegistration(val databaseName: String, val owningPlugin: Plugin) {
    // databaseName = Actual Database name (case sensitive)
    // owningPlugin = Plugin that registered this database
}
