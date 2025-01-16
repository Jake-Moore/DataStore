package com.kamikazejam.datastore.connections.storage

import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.Service
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.StoreCache
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.log.LoggerService
import org.jetbrains.annotations.Blocking
import java.util.function.Consumer

/**
 * Defines the minimum set of methods all Storage services must implement.
 */
@Suppress("unused")
abstract class StorageService : LoggerService(), Service {
    /**
     * Save a Store to this store. Requires the cache it belongs to.
     * Implementations of this class should handle optimistic versioning and throw errors accordingly.
     * @return If the Store was saved.
     */
    abstract fun <K, X : Store<X, K>> save(cache: Cache<K, X>, store: X): Boolean

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br></br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @return If the Store was replaced. (if the db was updated)
     */
    @Blocking
    abstract fun <K, X : Store<X, K>> updateSync(cache: Cache<K, X>, store: X, updateFunction: Consumer<X>): Boolean

    /**
     * Retrieve a Store from this store. Requires the cache to fetch it from.
     */
    abstract fun <K, X : Store<X, K>> get(cache: Cache<K, X>, key: K): X?

    /**
     * @return How many Stores are stored in a cache within this store.
     */
    abstract fun <K, X : Store<X, K>> size(cache: Cache<K, X>): Long

    /**
     * Check if a Store is stored in a given cache.
     */
    abstract fun <K, X : Store<X, K>> has(cache: Cache<K, X>, key: K): Boolean

    /**
     * Remove a Store from a given cache.
     */
    abstract fun <K, X : Store<X, K>> remove(cache: Cache<K, X>, key: K): Boolean

    /**
     * Retrieve all Stores from a specific cache.
     */
    abstract fun <K, X : Store<X, K>> getAll(cache: Cache<K, X>): Iterable<X>

    /**
     * Retrieve all Store keys from a specific cache.
     */
    abstract fun <K, X : Store<X, K>> getKeys(cache: Cache<K, X>): Iterable<K>

    /**
     * @return If the StorageService is ready to be used for a cache.
     */
    abstract fun canCache(): Boolean

    /**
     * Called when a cache is registered with the StoreEngine -> meant for internal initialization.
     */
    abstract fun <K, X : Store<X, K>> onRegisteredCache(cache: Cache<K, X>?)

    /**
     * Get the average ping to the storage service (round trip). (cached value from last heartbeat)
     */
    abstract val averagePingNanos: Long

    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    abstract fun <K, X : Store<X, K>, T> registerIndex(cache: StoreCache<K, X>, index: IndexedField<X, T>)
    abstract fun <K, X : Store<X, K>> cacheIndexes(cache: StoreCache<K, X>, store: X, updateFile: Boolean)
    abstract fun <K, X : Store<X, K>> saveIndexCache(cache: StoreCache<K, X>)
    abstract fun <K, X : Store<X, K>, T> getStoreIdByIndex(
        cache: StoreCache<K, X>,
        index: IndexedField<X, T>,
        value: T
    ): K?

    abstract fun <K, X : Store<X, K>> invalidateIndexes(cache: StoreCache<K, X>, key: K, updateFile: Boolean)
}
