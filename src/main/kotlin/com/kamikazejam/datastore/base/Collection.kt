package com.kamikazejam.datastore.base

import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.base.loader.StoreLoader
import com.kamikazejam.datastore.base.exception.DuplicateCollectionException
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.result.AsyncStoreHandler
import com.kamikazejam.datastore.base.storage.StorageDatabase
import com.kamikazejam.datastore.base.storage.StorageLocal
import com.kamikazejam.datastore.base.storage.StorageMethods
import com.kamikazejam.datastore.base.store.StoreInstantiator
import com.kamikazejam.datastore.mode.`object`.ObjectCollection
import com.kamikazejam.datastore.mode.profile.ProfileCollection
import com.mongodb.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.bukkit.plugin.Plugin
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext

/**
 * A [Collection] holds Store objects and manages their retrieval, caching, and saving.
 * Getters vary by Store type, they are defined in the store-specific interfaces:
 * [ObjectCollection] and [ProfileCollection]
 */
@Suppress("unused")
interface Collection<K, X : Store<X, K>> : Service, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    // ----------------------------------------------------- //
    //                     CRUD Helpers                      //
    // ----------------------------------------------------- //
    // create(initializer) are in ObjectCollection
    // additional player methods are in ProfileCollection

    /**
     * Create a new Store object with the provided key & initializer.<br></br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the collection. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @Throws(DuplicateKeyException::class)
    fun create(key: K, initializer: Consumer<X>): AsyncStoreHandler<K, X>

    /**
     * Read a Store from this collection (or the database if it doesn't exist in the cache)
     * @param key The key of the Store to read.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store object. (READ-ONLY) (optional)
     */
    fun read(key: K, cacheStore: Boolean = true): AsyncStoreHandler<K, X>

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    fun update(key: K, updateFunction: Consumer<X>): AsyncStoreHandler<K, X>

    /**
     * Deletes a Store by ID (removes from both cache and database collection)
     * @return True if the Store was deleted, false if it was not found (does not exist)
     */
    fun delete(key: K): Deferred<Boolean>

    /**
     * Retrieves ALL Stores, including cached values and additional values from database.
     * @param cacheStores If true, any additional Store fetched from db will be cached.
     * @return An Iterable of all Store, for sequential processing. (READ-ONLY)
     */
    @Blocking
    fun readAll(cacheStores: Boolean): Iterable<X>


    // ------------------------------------------------------ //
    // Database Methods                                       //
    // ------------------------------------------------------ //
    @get:Blocking
    val iDs: Iterable<K>

    /**
     * Loads all Stores directly from db, bypassing the cache.
     * Unless you have a reason to use this, please use [.readAll] instead.
     * @param cacheStores If true, stores loaded from the database will be cached.
     * @return An Iterable of all Stores, for sequential processing. (READ-ONLY)
     */
    @Blocking
    fun readAllFromDatabase(cacheStores: Boolean): Iterable<X?>


    // ------------------------------------------------------ //
    // Collection Methods                                     //
    // ------------------------------------------------------ //
    /**
     * Get the name of this collection (set by the end user, should be unique)
     * A [DuplicateCollectionException] error will be thrown if another collection
     * exists with the same name or ID during creation.
     *
     * @return String: Collection Name
     */
    val name: String

    /**
     * Retrieve a Store from the local cache. (Does not query the database)
     *
     * @return The Store if it was found in the cache, null otherwise.
     */
    fun readFromCache(key: K): X?

    /**
     * Retrieve a Store from the database. (Force queries the database, and updates this Collection)
     *
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store if it was found in the database.
     */
    fun readFromDatabase(key: K, cacheStore: Boolean = true): X?

    /**
     * Adds a Store to the local cache.
     */
    fun cache(store: X)

    /**
     * Removes a Store from the local cache.
     */
    fun uncache(key: K)

    /**
     * Removes a Store from the local cache.
     */
    fun uncache(store: X)

    /**
     * Checks if a Store is in this Collection.
     *
     * @return True if the Store is locally cached. False if not (for instance if it was deleted)
     */
    fun isCached(key: K): Boolean

    /**
     * Gets all Store objects that are in the local cache.
     */
    val cached: kotlin.collections.Collection<X>

    /**
     * Gets the [LoggerService] for this collection. For logging purposes.
     */
    fun getLoggerService(): LoggerService

    /**
     * Sets the [LoggerService] for this collection. For logging purposes.
     */
    fun setLoggerService(loggerService: LoggerService)

    /**
     * Gets the [StorageMethods] that handles local storage for this collection.
     */
    val localStore: StorageLocal<K, X>

    /**
     * Gets the [StorageMethods] that handles database storage for this collection.
     */
    val databaseStore: StorageDatabase<K, X>

    /**
     * Gets the plugin that set up this collection.
     */
    val plugin: Plugin

    /**
     * Gets the registration the parent plugin used to create this collection.
     */
    val registration: DataStoreRegistration

    /**
     * Return the name of actual the MongoDB database this Collection is stored in
     * This is different from the developer supplied db name, and is calculated from
     * [com.kamikazejam.datastore.DataStoreAPI.getFullDatabaseName].
     */
    val databaseName: String

    /**
     * Converts a Collection key to a string. Key uniqueness should be maintained.
     */
    fun keyToString(key: K): String

    /**
     * Converts a string to a Collection key. Key uniqueness should be maintained.
     */
    fun keyFromString(key: String): K

    /**
     * Add a dependency on another Collection. This Collection will be loaded after the dependency.
     */
    fun addDepend(collection: Collection<*, *>)

    /**
     * Check if this Collection is dependent on the provided collection.
     */
    fun isDependentOn(collection: Collection<*, *>): Boolean

    /**
     * Check if this Collection is dependent on the provided collection.
     */
    fun isDependentOn(collName: String): Boolean

    /**
     * Gets the name of all [Collection] objects this Collection is dependent on.
     */
    val dependencyNames: Set<String?>

    /**
     * Helper method to use the [.getPlugin] plugin to run an async bukkit task.
     */
    @ApiStatus.Internal
    fun runAsync(runnable: Runnable)

    /**
     * Helper method to use the [.getPlugin] plugin to run a sync bukkit task.
     */
    @ApiStatus.Internal
    fun runSync(runnable: Runnable)

    /**
     * Helper method to use the [.getPlugin] plugin to attempt an Async task
     * If the plugin is not allowed to run async tasks (like on disable), a sync task will be run instead.
     */
    @ApiStatus.Internal
    fun tryAsync(runnable: Runnable)

    /**
     * Get the number of Store objects currently stored in the local cache
     */
    val localCacheSize: Long

    /**
     * @return True iff this Collection contains a Store with the provided key. (checks database too)
     */
    fun hasKey(key: K): Deferred<Boolean>

    /**
     * Gets the [StoreLoader] for the provided key.
     */
    @ApiStatus.Internal
    fun loader(key: K): StoreLoader<X>

    @get:ApiStatus.Internal
    val storeClass: Class<X>

    /**
     * Returns the StoreInstantiator for the Store object in this Collection.
     */
    val instantiator: StoreInstantiator<K, X>

    /**
     * Internal method used by DataStore to forcefully update a local instance of a Store object with a newer one,
     * allowing your references to the existing Store to remain intact and up-to-date.
     * Note that this only effects persistent (non-transient) fields.
     *
     * @param store   The Store to update
     * @param update The newer version of said Store to replace the values of {@param Store} with.
     */
    @ApiStatus.Internal
    fun updateStoreFromNewer(store: X, update: X)


    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    /**
     * Register an index for this Collection.
     * @return The registered index (for chaining)
     */
    fun <T> registerIndex(field: IndexedField<X, T>): IndexedField<X, T>

    /**
     * Updates the indexes cache with the provided Store object.
     */
    @ApiStatus.Internal
    fun cacheIndexes(store: X, save: Boolean)

    /**
     * Updates the indexes cache with the provided Store object.
     */
    @ApiStatus.Internal
    fun invalidateIndexes(key: K, save: Boolean)

    /**
     * Saves the index cache to storage.
     */
    @ApiStatus.Internal
    fun saveIndexCache()

    fun <T> getStoreIdByIndex(index: IndexedField<X, T>, value: T): AsyncStoreHandler<K?>? {
        return AsyncStoreHandler.of<K?>(CompletableFuture.supplyAsync<K?> {
            getStoreIdByIndexSync(
                index,
                value
            )
        }, this)
    }

    fun <T> getStoreIdByIndexSync(index: IndexedField<X, T>, value: T): K?

    /**
     * Retrieves an object by the provided index field and its value.
     */
    fun <T> getByIndex(field: IndexedField<X, T>, value: T): AsyncStoreHandler<X?> {
        return AsyncStoreHandler.of<X?>(CompletableFuture.supplyAsync<X?> {
            getByIndexSync(
                field,
                value
            )
        }, this)
    }

    /**
     * Retrieves an object by the provided index field and its value.
     */
    fun <T> getByIndexSync(field: IndexedField<X, T>, value: T): X?
}

