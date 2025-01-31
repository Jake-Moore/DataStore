package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.exception.update.UpdateException
import com.kamikazejam.datastore.store.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Middleware class to adapt [StorageDatabase] methods (which include a [Collection] param) to a simpler api (without the [Collection] param).
 */
abstract class StorageDatabaseAdapter<K : Any, X : Store<X, K>>(protected val collection: Collection<K, X>) {
    // ---------------------------------------------------------------- //
    //                     Abstraction Conversion                       //
    // ---------------------------------------------------------------- //
    protected abstract suspend fun get(collection: Collection<K, X>, key: K): X?

    protected abstract suspend fun save(collection: Collection<K, X>, store: X): Boolean

    @Throws(UpdateException::class)
    protected abstract suspend fun updateSync(collection: Collection<K, X>, store: X, updateFunction: (X) -> X): X

    protected abstract suspend fun has(collection: Collection<K, X>, key: K): Boolean

    protected abstract suspend fun remove(collection: Collection<K, X>, key: K): Boolean

    protected abstract suspend fun removeAll(collection: Collection<K, X>): Long

    protected abstract suspend fun getAll(collection: Collection<K, X>): Flow<X>

    protected abstract suspend fun getKeys(collection: Collection<K, X>): Flow<K>

    protected abstract suspend fun size(collection: Collection<K, X>): Long


    // ---------------------------------------------------------------- //
    //                     StorageDatabase Methods                      //
    // ---------------------------------------------------------------- //
    /**
     * Retrieve a Store from this database.
     */
    suspend fun get(key: K): X? {
        return this.get(this.collection, key)
    }

    /**
     * Save a Store to this database.
     * @return If the Store was saved.
     */
    suspend fun save(store: X): Boolean {
        return this.save(this.collection, store)
    }

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br></br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @param updateFunction The function to update the Store with.
     * @return If the Store was replaced. (if the db was updated)
     */
    @Throws(UpdateException::class)
    suspend fun updateSync(store: X, updateFunction: (X) -> X): X {
        return this.updateSync(this.collection, store, updateFunction)
    }

    /**
     * Check if a Store is stored in this database.
     */
    suspend fun has(key: K): Boolean {
        return this.has(this.collection, key)
    }

    /**
     * Check if a Store is stored in this database.
     */
    suspend fun has(store: X): Boolean {
        return this.has(this.collection, store.id)
    }

    /**
     * Remove a Store from this database.
     * @return If the Store existed, and was removed.
     */
    suspend fun remove(key: K): Boolean {
        return this.remove(this.collection, key)
    }

    /**
     * Remove a Store from this database.
     * @return If the Store existed, and was removed.
     */
    suspend fun remove(store: X): Boolean {
        return this.remove(this.collection, store.id)
    }

    /**
     * Removes all Stores from this storage. In the local case they are removed from cache, in the database case they are DELETED
     * @return How many objects were removed.
     */
    suspend fun removeAll(): Long {
        return this.removeAll(this.collection)
    }

    /**
     * Retrieve all Stores from this database.
     */
    suspend fun getAll(): Flow<X> {
        return this.getAll(this.collection)
    }

    /**
     * Retrieve all Store keys from this database.
     */
    suspend fun getKeys(): Flow<K> {
        return this.getKeys(this.collection)
    }

    /**
     * Retrieve all Store keys (in string form) from this database.
     * Uses [Collection.keyToString] to convert keys to strings.
     */
    suspend fun getKeyStrings(): Flow<String> {
        return this.getKeys(collection).map { collection.keyToString(it) }
    }

    /**
     * @return How many objects are in this database
     */
    suspend fun size(): Long {
        return this.size(this.collection)
    }

    /**
     * Gets the name of this storage layer.
     */
    abstract val layerName: String
}
