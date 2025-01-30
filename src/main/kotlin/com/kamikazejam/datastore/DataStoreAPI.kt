package com.kamikazejam.datastore

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.exception.DuplicateCollectionException
import com.kamikazejam.datastore.base.exception.DuplicateDatabaseException
import com.kamikazejam.datastore.base.database.DatabaseRegistration
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

/**
 * Main class of DataStore. This project does not work as a plugin, only a shade-able library.
 */
@Suppress("unused")
object DataStoreAPI {
    // ------------------------------------------------------ //
    // Prefix Methods                                         //
    // ------------------------------------------------------ //
    private val dataStorePrefix: AtomicReference<String> = AtomicReference<String>("global")
    private fun setDataStorePrefix(prefix: String) {
        Preconditions.checkNotNull(prefix)
        dataStorePrefix.set(prefix)
    }

    private fun getDataStorePrefix(): String {
        return dataStorePrefix.get()
    }


    // ------------------------------------------------------ //
    // Registration Methods                                   //
    // ------------------------------------------------------ //
    /**
     * Register your plugin and reserve a database name for your plugin's collections.
     * @return Your [DatabaseRegistration] (to be passed into your collection constructors)
     * @throws DuplicateDatabaseException - if this databaseName is already in use
     */
    @Throws(DuplicateDatabaseException::class)
    fun register(plugin: JavaPlugin, databaseName: String): DataStoreRegistration {
        Preconditions.checkNotNull(databaseName)

        registerDatabase(plugin, getFullDatabaseName(databaseName))
        return DataStoreRegistration(plugin, databaseName)
    }


    // ------------------------------------------------------ //
    // Database Methods                                       //
    // ------------------------------------------------------ //
    // Key is databaseName stored lowercase for uniqueness checks
    val databases: ConcurrentMap<String, DatabaseRegistration> = ConcurrentHashMap()

    @Throws(DuplicateDatabaseException::class)
    private fun registerDatabase(plugin: Plugin, databaseName: String) {
        val registration = getDatabaseRegistration(databaseName)
        if (registration != null) {
            throw DuplicateDatabaseException(registration, plugin)
        }
        databases[databaseName.lowercase(Locale.getDefault())] = DatabaseRegistration(databaseName, plugin)
    }

    private fun getDatabaseRegistration(databaseName: String): DatabaseRegistration? {
        return databases[databaseName.lowercase(Locale.getDefault())]
    }

    /**
     * Check if a database name is already registered
     */
    fun isDatabaseNameRegistered(databaseName: String): Boolean {
        return databases.containsKey(databaseName.lowercase(Locale.getDefault()))
    }

    /**
     * Adds the DataStore prefix ([DataStoreAPI.getDataStorePrefix]) and a '_' char to the beginning of the dbName,
     * to allow multiple servers running DataStore to operate on the same MongoDB instance
     * (Assuming the prefix is unique to each server)
     */
    fun getFullDatabaseName(dbName: String): String {
        // Just in case, don't add the prefix twice
        val prefix: String = getDataStorePrefix()
        if (dbName.startsWith(prefix + "_")) {
            return dbName
        }
        return prefix + "_" + dbName
    }


    // ------------------------------------------------------ //
    // Collection Methods                                     //
    // ------------------------------------------------------ //
    val collections: ConcurrentMap<String, Collection<*, *>> = ConcurrentHashMap()

    /**
     * Get a collection by name
     *
     * @param name Name of the collection
     * @return The Collection or null if not found
     */
    fun getCollection(name: String): Collection<*, *>? {
        return collections[convertCollectionName(name)]
    }

    /**
     * Register a collection w/ a hook
     *
     * @param collection [Collection]
     */
    @Throws(DuplicateCollectionException::class)
    fun saveCollection(collection: Collection<*, *>) {
        if (collections.containsKey(convertCollectionName(collection.name))) {
            throw DuplicateCollectionException(collection)
        }
        collections[convertCollectionName(collection.name)] = collection
    }

    /**
     * Unregister a collection w/ a hook
     */
    fun removeCollection(collection: Collection<*, *>) {
        collections.remove(convertCollectionName(collection.name))
    }

    /**
     * Removes all spaces from the name and converts it to lowercase.
     */
    private fun convertCollectionName(name: String): String {
        return name.lowercase(Locale.getDefault()).replace(" ", "")
    }


    private var _sortedCollectionsByDependsReversed: List<Collection<*, *>>? = null
    val sortedCollectionsByDependsReversed: List<Collection<*, *>>
        /**
         * Retrieve the collections in sorted order by dependencies (load order)
         */
        get() {
            _sortedCollectionsByDependsReversed?.let {
                if (!hasBeenModified()) {
                    return it
                }
            }

            val s = collections.values.stream().sorted().collect(Collectors.toList())
            _sortedCollectionsByDependsReversed = s
            return s
        }

    private fun hasBeenModified(): Boolean {
        return _sortedCollectionsByDependsReversed?.let { collections.size != it.size } ?: false
    }


    // ------------------------------------------------------ //
    // Miscellaneous Methods                                  //
    // ------------------------------------------------------ //
    /**
     * @return if DataStore is in debug mode
     */
    fun isDebug(): Boolean {
        return DataStoreSource.isDebug()
    }
}
