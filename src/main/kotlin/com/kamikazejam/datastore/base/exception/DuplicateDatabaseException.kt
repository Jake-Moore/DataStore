package com.kamikazejam.datastore.base.exception

import com.kamikazejam.datastore.base.database.DatabaseRegistration
import org.bukkit.plugin.Plugin

@Suppress("unused")
class DuplicateDatabaseException(existingRegistration: DatabaseRegistration, plugin: Plugin) :
    Exception(
        "Plugin '" + plugin.name + "' tried to register a database with the name '" +
                existingRegistration.databaseName + "' when it already exists. It has already been registered by '" +
                existingRegistration.owningPlugin.name + "'"
    )
