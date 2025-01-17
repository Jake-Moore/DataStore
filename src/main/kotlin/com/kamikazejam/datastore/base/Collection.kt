package com.kamikazejam.datastore.base

import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.base.cache.StoreLoader
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
import org.bukkit.plugin.Plugin
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * A Cache holds Store objects and manages their retrieval, caching, and saving.
 * Getters vary by Store type, they are defined in the store-specific interfaces:
 * [ObjectCollection] and [ProfileCollection]
 */
@Suppress("unused", "BlockingMethodInNonBlockingContext")
interface Collection<K, X : Store<X, K>> : Service {
    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //
    // create(initializer) & createAsync are in ObjectCache
    // additional player methods are in ProfileCache

    /**
     * Read a Store from this cache (or the database if it doesn't exist in the cache)
     * @param key The key of the Store to read.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store object. (READ-ONLY) (optional)
     */
    @NonBlocking
    fun read(key: K, cacheStore: Boolean = true): AsyncStoreHandler<K, X>

    /**
     * Create a new Store object with the provided key & initializer.<br></br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the cache. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @NonBlocking
    @Throws(DuplicateKeyException::class)
    fun create(key: K, initializer: Consumer<X>): AsyncStoreHandler<K, X>

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    fun update(key: K, updateFunction: Consumer<X>): AsyncStoreHandler<X> {
        return AsyncStoreHandler.of(CompletableFuture.supplyAsync { updateSync(key, updateFunction) }, this)
    }

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    fun update(store: X, updateFunction: Consumer<X>): AsyncStoreHandler<X> {
        return this.update(store.id, updateFunction)
    }

    /**
     * Deletes a Store by ID (removes from both cache and database)
     */
    @NonBlocking
    fun delete(key: K): AsyncStoreHandler<Void> {
        return AsyncStoreHandler.of<Void>(CompletableFuture.runAsync { deleteSync(key) }, this)
    }

    /**
     * Deletes a Store (removes from both cache and database)
     */
    @NonBlocking
    fun delete(store: X): AsyncStoreHandler<Void> {
        return AsyncStoreHandler.of<Void>(CompletableFuture.runAsync { deleteSync(store) }, this)
    }

    /**
     * Retrieves ALL Stores, including cached values and additional values from database.
     * @param cacheStores If true, any additional Store fetched from db will be cached.
     * @return An Iterable of all Store, for sequential processing. (READ-ONLY)
     */
    @Blocking
    fun readAll(cacheStores: Boolean): Iterable<X>




    // TODO DElEte ALL SYNC

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @Blocking
    fun updateSync(key: K, updateFunction: Consumer<X>): X

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @Blocking
    fun updateSync(store: X, updateFunction: Consumer<X>): X {
        return this.updateSync(store.id, updateFunction)
    }

    /**
     * Deletes a Store by ID (removes from both cache and database)
     */
    @Blocking
    fun deleteSync(key: K)

    /**
     * Deletes a Store (removes from both cache and database)
     */
    @Blocking
    fun deleteSync(store: X)


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
    // Cache Methods                                          //
    // ------------------------------------------------------ //
    /**
     * Get the name of this cache (set by the end user, should be unique)
     * A [DuplicateCollectionException] error will be thrown if another cache
     * exists with the same name or ID during creation.
     *
     * @return String: Cache Name
     */
    val name: String

    /**
     * Retrieve a Store from this cache. (Does not query the database)
     *
     * @return The Store if it was cached.
     */
    fun readFromCache(key: K): X?

    /**
     * Retrieve a Store from the database. (Force queries the database, and updates this cache)
     *
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store if it was found in the database.
     */
    fun readFromDatabase(key: K, cacheStore: Boolean = true): X?

    /**
     * Adds a Store to this cache.
     */
    fun cache(store: X)

    /**
     * Removes a Store from this cache.
     */
    fun uncache(key: K)

    /**
     * Removes a Store from this cache.
     */
    fun uncache(store: X)

    /**
     * Checks if a Store is in this Cache.
     *
     * @return True if the Store is cached. False if not (for instance if it was deleted)
     */
    fun isCached(key: K): Boolean

    /**
     * Gets all Store objects that are in this cache.
     */
    val cached: kotlin.collections.Collection<X>

    /**
     * Gets the [LoggerService] for this cache. For logging purposes.
     */
    fun getLoggerService(): LoggerService

    /**
     * Sets the [LoggerService] for this cache. For logging purposes.
     */
    fun setLoggerService(loggerService: LoggerService)

    /**
     * Gets the [StorageMethods] that handles local storage for this cache.
     */
    val localStore: StorageLocal<K, X>

    /**
     * Gets the [StorageMethods] that handles database storage for this cache.
     */
    val databaseStore: StorageDatabase<K, X>

    /**
     * Gets the plugin that set up this cache.
     */
    val plugin: Plugin

    /**
     * Gets the registration the parent plugin used to create this cache.
     */
    val registration: DataStoreRegistration

    /**
     * Return the name of actual the MongoDB database this cache is stored in
     * This is different from the developer supplied db name, and is calculated from
     * [com.kamikazejam.datastore.DataStoreAPI.getFullDatabaseName].
     */
    val databaseName: String

    /**
     * Converts a Cache key to a string. Key uniqueness should be maintained.
     */
    fun keyToString(key: K): String

    /**
     * Converts a string to a Cache key. Key uniqueness should be maintained.
     */
    fun keyFromString(key: String): K

    /**
     * Add a dependency on another Cache. This Cache will be loaded after the dependency.
     */
    fun addDepend(collection: Collection<*, *>)

    /**
     * Check if this Cache is dependent on the provided cache.
     */
    fun isDependentOn(collection: Collection<*, *>): Boolean

    /**
     * Check if this Cache is dependent on the provided cache.
     */
    fun isDependentOn(cacheName: String): Boolean

    /**
     * Gets the name of all Cache objects this Cache is dependent on.
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
     * Get the number of Store objects currently stored locally in this cache
     */
    val localCacheSize: Long

    /**
     * @return True iff the cache contains a Store with the provided key.
     */
    fun hasKey(key: K): AsyncStoreHandler<Boolean> {
        return AsyncStoreHandler.of(CompletableFuture.supplyAsync {
            hasKeySync(
                key
            )
        }, this)
    }

    /**
     * @return True iff the cache contains a Store with the provided key.
     */
    fun hasKeySync(key: K): Boolean

    /**
     * Gets the [StoreLoader] for the provided key.
     */
    @ApiStatus.Internal
    fun loader(key: K): StoreLoader<X>

    @get:ApiStatus.Internal
    val storeClass: Class<X>

    /**
     * Returns the StoreInstantiator for the Store object in this cache.
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
     * Register an index for this cache.
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

