package com.kamikazejam.datastore.base

import com.kamikazejam.datastore.DataStoreRegistration
import com.kamikazejam.datastore.base.cache.StoreLoader
import com.kamikazejam.datastore.base.exception.DuplicateCacheException
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.result.StoreResult
import com.kamikazejam.datastore.base.storage.StorageDatabase
import com.kamikazejam.datastore.base.storage.StorageLocal
import com.kamikazejam.datastore.base.storage.StorageMethods
import com.kamikazejam.datastore.base.store.StoreInstantiator
import com.kamikazejam.datastore.mode.`object`.ObjectCache
import com.kamikazejam.datastore.mode.profile.ProfileCache
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
 * [ObjectCache] and [ProfileCache]
 */
@Suppress("unused", "BlockingMethodInNonBlockingContext")
interface Cache<K, X : Store<X, K>> : Service {
    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //
    // create(initializer) & createAsync are in ObjectCache
    // additional player methods are in ProfileCache
    /**
     * Read a Store from this cache (or the database if it doesn't exist in the cache)
     * @param key The key of the Store to read.
     * @return The Store object. (READ-ONLY) (optional)
     */
    @NonBlocking
    fun read(key: K): StoreResult<X?> {
        return StoreResult.of<X?>(CompletableFuture.supplyAsync<X?> { readSync(key) }, this)
    }

    /**
     * Read a Store from this cache (or the database if it doesn't exist in the cache)
     * @param key The key of the Store to read.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store object. (READ-ONLY) (optional)
     */
    @NonBlocking
    fun read(key: K, cacheStore: Boolean): StoreResult<X?> {
        return StoreResult.of<X?>(CompletableFuture.supplyAsync<X?> { readSync(key, cacheStore) }, this)
    }

    /**
     * Get a Store object from the cache or create a new one if it doesn't exist.<br></br>
     * This specific method will override any key set in the initializer. Since the key is an argument.
     * @param key The key of the Store to get or create.
     * @param initializer The initializer for the Store if it doesn't exist.
     * @return The Store object. (READ-ONLY) (fetched or created)
     */
    @NonBlocking
    fun readOrCreate(key: K, initializer: Consumer<X>): StoreResult<X> {
        return StoreResult.of(CompletableFuture.supplyAsync { readOrCreateSync(key, initializer) }, this)
    }

    /**
     * Create a new Store object with the provided key & initializer.<br></br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the cache. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @NonBlocking
    @Throws(DuplicateKeyException::class)
    fun create(key: K, initializer: Consumer<X>): StoreResult<X> {
        return StoreResult.of(CompletableFuture.supplyAsync { createSync(key, initializer) }, this)
    }

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    fun update(key: K, updateFunction: Consumer<X>): StoreResult<X> {
        return StoreResult.of(CompletableFuture.supplyAsync { updateSync(key, updateFunction) }, this)
    }

    /**
     * Modifies a Store in a controlled environment where modifications are allowed
     * @throws NoSuchElementException if the Store (by this key) is not found
     * @return The updated Store object. (READ-ONLY)
     */
    @NonBlocking
    fun update(store: X, updateFunction: Consumer<X>): StoreResult<X> {
        return this.update(store.id, updateFunction)
    }

    /**
     * Deletes a Store by ID (removes from both cache and database)
     */
    @NonBlocking
    fun delete(key: K): StoreResult<Void> {
        return StoreResult.of<Void>(CompletableFuture.runAsync { deleteSync(key) }, this)
    }

    /**
     * Deletes a Store (removes from both cache and database)
     */
    @NonBlocking
    fun delete(store: X): StoreResult<Void> {
        return StoreResult.of<Void>(CompletableFuture.runAsync { deleteSync(store) }, this)
    }

    /**
     * Retrieves ALL Stores, including cached values and additional values from database.
     * @param cacheStores If true, any additional Store fetched from db will be cached.
     * @return An Iterable of all Store, for sequential processing. (READ-ONLY)
     */
    @Blocking
    fun readAll(cacheStores: Boolean): Iterable<X>

    // ----------------------------------------------------- //
    //                  CRUD Helpers (sync)                  //
    // ----------------------------------------------------- //
    /**
     * Read a Store from this cache (or the database if it doesn't exist in the cache)
     * @param key The key of the Store to read.
     * @return The Store object. (READ-ONLY) (optional)
     */
    @Blocking
    fun readSync(key: K): X?

    /**
     * Read a Store from this cache (or the database if it doesn't exist in the cache)
     * @param key The key of the Store to read.
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store object. (READ-ONLY) (optional)
     */
    @Blocking
    fun readSync(key: K, cacheStore: Boolean): X?

    /**
     * Get a Store object from the cache or create a new one if it doesn't exist.<br></br>
     * This specific method will override any key set in the initializer. Since the key is an argument.
     * @param key The key of the Store to get or create.
     * @param initializer The initializer for the Store if it doesn't exist.
     * @return The Store object. (READ-ONLY) (fetched or created)
     */
    @Blocking
    fun readOrCreateSync(key: K, initializer: Consumer<X>): X

    /**
     * Create a new Store object with the provided key & initializer.<br></br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the cache. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @Blocking
    @Throws(DuplicateKeyException::class)
    fun createSync(key: K, initializer: Consumer<X>): X

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
     * A [DuplicateCacheException] error will be thrown if another cache
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
    fun getFromCache(key: K): X?

    /**
     * Retrieve a Store from the database. (Force queries the database, and updates this cache)
     *
     * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
     * @return The Store if it was found in the database.
     */
    fun getFromDatabase(key: K, cacheStore: Boolean): X?

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
    val cached: Collection<X>

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
    fun addDepend(cache: Cache<*, *>)

    /**
     * Check if this Cache is dependent on the provided cache.
     */
    fun isDependentOn(cache: Cache<*, *>): Boolean

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
    fun hasKey(key: K): StoreResult<Boolean> {
        return StoreResult.of(CompletableFuture.supplyAsync {
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

    fun <T> getStoreIdByIndex(index: IndexedField<X, T>, value: T): StoreResult<K?>? {
        return StoreResult.of<K?>(CompletableFuture.supplyAsync<K?> {
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
    fun <T> getByIndex(field: IndexedField<X, T>, value: T): StoreResult<X?> {
        return StoreResult.of<X?>(CompletableFuture.supplyAsync<X?> {
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

