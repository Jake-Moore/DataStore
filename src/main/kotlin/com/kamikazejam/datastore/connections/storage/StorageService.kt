package com.kamikazejam.datastore.connections.storage

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Service
import com.kamikazejam.datastore.base.StoreCollection
import com.kamikazejam.datastore.base.exception.update.UpdateException
import com.kamikazejam.datastore.base.index.IndexedField
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.base.metrics.MetricsListener
import com.kamikazejam.datastore.store.Store
import kotlinx.coroutines.flow.Flow

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
    suspend fun <K : Any, X : Store<X, K>> save(collection: Collection<K, X>, store: X): Boolean {
        DataStoreSource.metricsListeners.forEach(MetricsListener::onSave)
        return saveToStorage(collection, store)
    }

    protected abstract suspend fun <K : Any, X : Store<X, K>> saveToStorage(collection: Collection<K, X>, store: X): Boolean

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br></br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @return If the Store was replaced. (if the db was updated)
     */
    suspend fun <Y : Store<Y, K>, K : Any> update(collection: Collection<K, Y>, store: Y, updateFunction: (Y) -> Y): Y {
        DataStoreSource.metricsListeners.forEach(MetricsListener::onUpdate)
        return updateToStorage(collection, store, updateFunction)
    }

    @Throws(UpdateException::class)
    protected abstract suspend fun <K : Any, X : Store<X, K>> updateToStorage(collection: Collection<K, X>, store: X, updateFunction: (X) -> X): X

    /**
     * Retrieve a Store from this store. Requires the collection to fetch it from.
     */
    suspend fun <K : Any, X : Store<X, K>> read(collection: Collection<K, X>, key: K): X? {
        DataStoreSource.metricsListeners.forEach(MetricsListener::onRead)
        return readFromStorage(collection, key)
    }

    protected abstract suspend fun <K : Any, X : Store<X, K>> readFromStorage(collection: Collection<K, X>, key: K): X?

    /**
     * @return How many Stores are stored in a collection within this store.
     */
    suspend fun <K : Any, X : Store<X, K>> size(collection: Collection<K, X>): Long {
        DataStoreSource.metricsListeners.forEach(MetricsListener::onSize)
        return sizeOfStorage(collection)
    }

    protected abstract suspend fun <K : Any, X : Store<X, K>> sizeOfStorage(collection: Collection<K, X>): Long

    /**
     * Check if a Store is stored in a given collection.
     */
    suspend fun <K : Any, X : Store<X, K>> has(collection: Collection<K, X>, key: K): Boolean {
        DataStoreSource.metricsListeners.forEach(MetricsListener::onHas)
        return hasInStorage(collection, key)
    }

    protected abstract suspend fun <K : Any, X : Store<X, K>> hasInStorage(collection: Collection<K, X>, key: K): Boolean

    /**
     * Remove a Store from a given collection.
     */
    suspend fun <K : Any, X : Store<X, K>> delete(collection: Collection<K, X>, key: K): Boolean {
        DataStoreSource.metricsListeners.forEach(MetricsListener::onDelete)
        return removeFromStorage(collection, key)
    }

    protected abstract suspend fun <K : Any, X : Store<X, K>> removeFromStorage(collection: Collection<K, X>, key: K): Boolean

    /**
     * Remove ALL Stores from a given collection.
     * @return The number of Stores removed.
     */
    suspend fun <K : Any, X : Store<X, K>> deleteAll(collection: Collection<K, X>): Long {
        DataStoreSource.metricsListeners.forEach(MetricsListener::onDeleteAll)
        return removeAllFromStorage(collection)
    }

    protected abstract suspend fun <K : Any, X : Store<X, K>> removeAllFromStorage(collection: Collection<K, X>): Long

    /**
     * Retrieve all Stores from a specific collection.
     */
    suspend fun <K : Any, X : Store<X, K>> readAll(collection: Collection<K, X>): Flow<X> {
        DataStoreSource.metricsListeners.forEach(MetricsListener::onReadAll)
        return readAllFromStorage(collection)
    }

    protected abstract suspend fun <K : Any, X : Store<X, K>> readAllFromStorage(collection: Collection<K, X>): Flow<X>

    /**
     * Retrieve all Store keys from a specific collection.
     */
    suspend fun <K : Any, X : Store<X, K>> readKeys(collection: Collection<K, X>): Flow<K> {
        DataStoreSource.metricsListeners.forEach(MetricsListener::readKeys)
        return readKeysFromStorage(collection)
    }

    protected abstract suspend fun <K : Any, X : Store<X, K>> readKeysFromStorage(collection: Collection<K, X>): Flow<K>

    /**
     * @return If the StorageService is ready for writes.
     */
    abstract fun canWriteToStorage(): Boolean

    /**
     * Called when a collection is registered with the StoreEngine -> meant for internal initialization.
     */
    abstract fun <K : Any, X : Store<X, K>> onRegisteredCollection(collection: Collection<K, X>?)

    /**
     * Get the average ping to the storage service (round trip). (cached value from last heartbeat)
     */
    abstract val averagePingNanos: Long

    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    abstract suspend fun <K : Any, X : Store<X, K>, T> registerIndex(collection: StoreCollection<K, X>, index: IndexedField<X, T>)
    abstract suspend fun <K : Any, X : Store<X, K>> cacheIndexes(collection: StoreCollection<K, X>, store: X, updateFile: Boolean)
    abstract suspend fun <K : Any, X : Store<X, K>> saveIndexCache(collection: StoreCollection<K, X>)
    abstract suspend fun <K : Any, X : Store<X, K>, T> getStoreByIndex(
        collection: StoreCollection<K, X>,
        index: IndexedField<X, T>,
        value: T
    ): X?

    abstract suspend fun <K : Any, X : Store<X, K>> invalidateIndexes(collection: StoreCollection<K, X>, key: K, updateFile: Boolean)
}
