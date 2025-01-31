package com.kamikazejam.datastore.base

import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.base.async.handler.crud.AsyncCreateHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncReadHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.base.async.handler.impl.AsyncHasKeyHandler
import com.kamikazejam.datastore.base.async.handler.impl.AsyncReadIdHandler
import com.kamikazejam.datastore.base.coroutine.DataStoreScope
import com.kamikazejam.datastore.base.exception.DuplicateCollectionException
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.loader.StoreLoader
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.storage.StorageDatabase
import com.kamikazejam.datastore.base.storage.StorageLocal
import com.kamikazejam.datastore.store.`object`.ObjectCollection
import com.kamikazejam.datastore.store.profile.ProfileCollection
import com.kamikazejam.datastore.store.Store
import com.mongodb.*
import kotlinx.coroutines.flow.Flow
import org.bukkit.plugin.Plugin
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking
import java.util.*

/**
 * A [Collection] holds Store objects and manages their retrieval, caching, and saving.
 * Getters vary by Store type, they are defined in the store-specific interfaces:
 * [ObjectCollection] and [ProfileCollection]
 */
@Suppress("unused")
interface Collection<K : Any, X : Store<X, K>> : Service, DataStoreScope {

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
    fun create(key: K, initializer: (X) -> X = { it -> it }): AsyncCreateHandler<K, X>

    /**
     * Read a Store from this collection (or the database if it doesn't exist in the cache)
     * @param key The key of the Store to read.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store object. (READ-ONLY) (optional)
     */
    fun read(key: K, cacheStore: Boolean = true): AsyncReadHandler<K, X>

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    fun update(key: K, updateFunction: (X) -> X): AsyncUpdateHandler<K, X>

    /**
     * Deletes a Store by ID (removes from both cache and database collection)
     * @return True if the Store was deleted, false if it was not found (does not exist)
     */
    fun delete(key: K): AsyncDeleteHandler

    /**
     * Retrieves ALL Stores, including cached values and additional values from database.
     * @param cacheStores If true, any additional Store fetched from db will be cached.
     * @return An Iterable of all Store, for sequential processing. (READ-ONLY)
     */
    @Blocking
    suspend fun readAll(cacheStores: Boolean): Flow<X>


    // ------------------------------------------------------ //
    // Database Methods                                       //
    // ------------------------------------------------------ //
    suspend fun getIDs(): Flow<K>

    /**
     * Loads all Stores directly from db, bypassing the cache.
     * Unless you have a reason to use this, please use [.readAll] instead.
     * @param cacheStores If true, stores loaded from the database will be cached.
     * @return An Iterable of all Stores, for sequential processing. (READ-ONLY)
     */
    @Blocking
    suspend fun readAllFromDatabase(cacheStores: Boolean): Flow<X>


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
    suspend fun readFromDatabase(key: K, cacheStore: Boolean = true): X?

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
     * Gets the [StorageLocal] that handles local storage for this collection.
     */
    val localStore: StorageLocal<K, X>

    /**
     * Gets the [StorageDatabase] that handles database storage for this collection.
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
     * A non-functional and mostly visual/informative identifier combining the key string and collection name
     *
     * Mostly used for logging purposes
     */
    fun getKeyStringIdentifier(key: K): String

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
    val dependencyNames: Set<String>

    /**
     * Helper method to use the [Collection]'s [Plugin] to run an async bukkit task.
     */
    @ApiStatus.Internal
    fun runAsync(runnable: Runnable)

    /**
     * Helper method to use the [Collection]'s [Plugin] to run a sync bukkit task.
     */
    @ApiStatus.Internal
    fun runSync(runnable: Runnable)

    /**
     * Helper method to use the [Collection]'s [Plugin] to attempt an Async task
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
    fun hasKey(key: K): AsyncHasKeyHandler

    /**
     * Gets the [StoreLoader] for the provided key.
     */
    @ApiStatus.Internal
    fun loader(key: K): StoreLoader<X>

    @get:ApiStatus.Internal
    val storeClass: Class<X>

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
    suspend fun <T> registerIndex(field: IndexedField<X, T>): IndexedField<X, T>

    /**
     * Updates the indexes cache with the provided Store object.
     */
    @ApiStatus.Internal
    suspend fun cacheIndexes(store: X, save: Boolean)

    /**
     * Updates the indexes cache with the provided Store object.
     */
    @ApiStatus.Internal
    suspend fun invalidateIndexes(key: K, save: Boolean)

    /**
     * Saves the index cache to storage.
     */
    @ApiStatus.Internal
    suspend fun saveIndexCache()


    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    /**
     * Retrieves a [Store] ([X]) by the provided index field and its value.
     */
    fun <T> readIdByIndex(field: IndexedField<X, T>, value: T): AsyncReadIdHandler<K>

    /**
     * Retrieves a [Store] ([X]) by the provided index field and its value.
     */
    fun <T> readByIndex(field: IndexedField<X, T>, value: T): AsyncReadHandler<K, X>

    /**
     * Retrieves a [Store] ([X]) by the provided index field and its value. (only checks cache)
     */
    fun <T> readFromCacheByIndex(field: IndexedField<X, T>, value: T): X?
}

