package com.kamikazejam.datastore

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.StoreCollection
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class DataStoreRegistration internal constructor(plugin: JavaPlugin, dbNameShort: String) {
    val plugin: JavaPlugin

    /**
     * The full database name as it would appear in MongoDB
     * This includes the DataStore prefix, described in [DataStoreAPI.getFullDatabaseName] (String)}
     * All plugin collections will be stored in this database
     */
    val databaseName: String
    private val dbNameShort: String

    private val collections: MutableList<Collection<*, *>> = ArrayList()

    // package-private because DataStore is the only one allowed to create this
    init {
        Preconditions.checkNotNull(plugin)
        Preconditions.checkNotNull(dbNameShort)
        this.plugin = plugin
        this.dbNameShort = dbNameShort
        this.databaseName = DataStoreAPI.getFullDatabaseName(dbNameShort)
    }

    fun registerCollection(clazz: Class<out StoreCollection<*, *>>) {
        // Find a constructor that takes a DataStoreRegistration
        try {
            // Find the constructor (regardless of visibility)
            val constructor = clazz.getDeclaredConstructor(
                DataStoreRegistration::class.java
            )
            constructor.isAccessible = true
            val collection = constructor.newInstance(this)
            collections.add(collection)
            DataStoreSource.storageService.onRegisteredCollection(collection)
            collection.getLoggerService().info("Collection Registered.")
        } catch (ex1: NoSuchMethodException) {
            DataStoreSource.error("Failed to register collection " + clazz.name + " - No constructor that takes a DataStoreRegistration")
        } catch (t: Throwable) {
            DataStoreSource.error("Failed to register collection " + clazz.name + " - " + t.javaClass.getName() + ": " + t.message)
        }
    }

    suspend fun shutdown() {
        collections.forEach { coll: Collection<*, *> -> coll.shutdown() }
        collections.clear()
        DataStoreAPI.registrations.remove(this)
    }
}
