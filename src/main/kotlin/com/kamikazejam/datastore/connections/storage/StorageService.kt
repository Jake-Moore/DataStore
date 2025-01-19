package com.kamikazejam.datastore.connections.storage

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Service
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.StoreCollection
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.log.LoggerService
import kotlinx.coroutines.flow.Flow
import java.util.function.Consumer

/**
 * Defines the minimum set of methods all Storage services must implement.
 */
@Suppress("unused")
abstract class StorageService : LoggerService(), Service {
    /**
     * Save a Store to this store. Requires the collection it belongs to.
     * Implementations of this class should handle optimistic versioning and throw errors accordingly.
     * @return If the Store was saved.
     */
    abstract suspend fun <K, X : Store<X, K>> save(collection: Collection<K, X>, store: X): Boolean

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br></br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @return If the Store was replaced. (if the db was updated)
     */
    abstract suspend fun <K, X : Store<X, K>> updateSync(collection: Collection<K, X>, store: X, updateFunction: Consumer<X>): Boolean

    /**
     * Retrieve a Store from this store. Requires the collection to fetch it from.
     */
    abstract suspend fun <K, X : Store<X, K>> get(collection: Collection<K, X>, key: K): X?

    /**
     * @return How many Stores are stored in a collection within this store.
     */
    abstract suspend fun <K, X : Store<X, K>> size(collection: Collection<K, X>): Long

    /**
     * Check if a Store is stored in a given collection.
     */
    abstract suspend fun <K, X : Store<X, K>> has(collection: Collection<K, X>, key: K): Boolean

    /**
     * Remove a Store from a given collection.
     */
    abstract suspend fun <K, X : Store<X, K>> remove(collection: Collection<K, X>, key: K): Boolean

    /**
     * Remove ALL Stores from a given collection.
     * @return The number of Stores removed.
     */
    abstract suspend fun <K, X : Store<X, K>> removeAll(collection: Collection<K, X>): Long

    /**
     * Retrieve all Stores from a specific collection.
     */
    abstract suspend fun <K, X : Store<X, K>> getAll(collection: Collection<K, X>): Flow<X>

    /**
     * Retrieve all Store keys from a specific collection.
     */
    abstract suspend fun <K, X : Store<X, K>> getKeys(collection: Collection<K, X>): Flow<K>

    /**
     * @return If the StorageService is ready for writes.
     */
    abstract fun canWrite(): Boolean

    /**
     * Called when a collection is registered with the StoreEngine -> meant for internal initialization.
     */
    abstract fun <K, X : Store<X, K>> onRegisteredCollection(collection: Collection<K, X>?)

    /**
     * Get the average ping to the storage service (round trip). (cached value from last heartbeat)
     */
    abstract val averagePingNanos: Long

    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    abstract suspend fun <K, X : Store<X, K>, T> registerIndex(collection: StoreCollection<K, X>, index: IndexedField<X, T>)
    abstract suspend fun <K, X : Store<X, K>> cacheIndexes(collection: StoreCollection<K, X>, store: X, updateFile: Boolean)
    abstract suspend fun <K, X : Store<X, K>> saveIndexCache(collection: StoreCollection<K, X>)
    abstract suspend fun <K, X : Store<X, K>, T> getStoreIdByIndex(
        collection: StoreCollection<K, X>,
        index: IndexedField<X, T>,
        value: T
    ): K?

    abstract suspend fun <K, X : Store<X, K>> invalidateIndexes(collection: StoreCollection<K, X>, key: K, updateFile: Boolean)
}
