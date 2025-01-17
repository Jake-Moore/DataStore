package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import org.jetbrains.annotations.ApiStatus

/**
 * Methods that all storage layers must implement.
 */
@Suppress("unused")
interface StorageMethods<K, X : Store<X, K>> {
    /**
     * Retrieve a Store from this store.
     */
    fun get(key: K): X?

    /**
     * Save a Store to this store.
     * @return If the Store was saved.
     */
    fun save(store: X): Boolean

    /**
     * Check if a Store is stored in this store.
     */
    fun has(key: K): Boolean

    /**
     * Check if a Store is stored in this store.
     */
    fun has(store: X): Boolean

    /**
     * Remove a Store from this store.
     *
     * @return If the Store existed, and was removed.
     */
    fun remove(key: K): Boolean

    /**
     * Remove a Store from this store.
     *
     * @return If the Store existed, and was removed.
     */
    fun remove(store: X): Boolean

    /**
     * Retrieve all Stores from this store.
     */
    val all: Iterable<X>

    /**
     * Retrieve all Store keys from this store.
     */
    val keys: Iterable<K>

    /**
     * Retrieve all Store keys (in string form) from this store.
     * Uses [Collection.keyToString] to convert keys to strings.
     */
    fun getKeyStrings(collection: Collection<K, X>): Iterable<String>

    /**
     * Clear all Stores from this store. No Stores are deleted, just removed from memory.
     */
    fun clear(): Long

    /**
     * Gets the name of this storage layer.
     */
    val layerName: String

    /**
     * @return How many objects are in this Store
     */
    fun size(): Long

    @get:ApiStatus.Internal
    val isDatabase: Boolean
}
