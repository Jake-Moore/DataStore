package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import kotlinx.coroutines.flow.Flow
import org.jetbrains.annotations.ApiStatus

/**
 * Methods that all storage layers must implement.
 */
@Suppress("unused")
interface StorageMethods<K, X : Store<X, K>> {
    /**
     * Retrieve a Store from this database.
     */
    suspend fun get(key: K): X?

    /**
     * Save a Store to this database.
     * @return If the Store was saved.
     */
    suspend fun save(store: X): Boolean

    /**
     * Check if a Store is stored in this database.
     */
    suspend fun has(key: K): Boolean

    /**
     * Check if a Store is stored in this database.
     */
    suspend fun has(store: X): Boolean

    /**
     * Remove a Store from this database.
     * @return If the Store existed, and was removed.
     */
    suspend fun remove(key: K): Boolean

    /**
     * Remove a Store from this database.
     * @return If the Store existed, and was removed.
     */
    suspend fun remove(store: X): Boolean

    /**
     * Removes all Stores from this storage. In the local case they are removed from cache, in the database case they are DELETED
     * @return How many objects were removed.
     */
    suspend fun removeAll(): Long

    /**
     * Retrieve all Stores from this database.
     */
    suspend fun getAll(): Flow<X>

    /**
     * Retrieve all Store keys from this database.
     */
    suspend fun getKeys(): Flow<K>

    /**
     * Retrieve all Store keys (in string form) from this database.
     * Uses [Collection.keyToString] to convert keys to strings.
     */
    suspend fun getKeyStrings(collection: Collection<K, X>): Flow<String>

    /**
     * Gets the name of this storage layer.
     */
    val layerName: String

    /**
     * @return How many objects are in this database
     */
    suspend fun size(): Long

    @get:ApiStatus.Internal
    val isDatabase: Boolean
}
