package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.store.Store
import com.kamikazejam.datastore.util.DataStoreFileLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

abstract class StorageLocal<K : Any, X : Store<X, K>> {
    val localStorage: ConcurrentMap<K, X> = ConcurrentHashMap()

    // ----------------------------------------------------- //
    //                     Store Methods                     //
    // ----------------------------------------------------- //
    /**
     * Retrieve a Store from this store.
     */
    fun get(key: K): X? {
        return localStorage[key]
    }

    /**
     * Save a Store to this store.
     * @return If the Store was saved.
     */
    fun save(store: X): Boolean {
        // Ensure we don't re-cache an invalid Store
        if (!store.valid) {
            DataStoreFileLogger.warn(
                store.getCollection(),
                "StoreLocal.save(X) w/ Invalid Store. Collection: " + store.getCollection().name + ", Id: " + store.id
            )
            return false
        }

        // If not called already, call initialized (since we're caching it)
        localStorage[store.id] = store
        return true
    }

    /**
     * Check if a Store is stored in this store.
     */
    fun has(key: K): Boolean {
        return localStorage.containsKey(key)
    }

    /**
     * Check if a Store is stored in this store.
     */
    fun has(store: X): Boolean {
        return this.has(store.id)
    }

    /**
     * Remove a Store from this store.
     * @return If the Store existed, and was removed.
     */
    fun remove(key: K): Boolean {
        val x = localStorage.remove(key)
        x?.invalidate()
        return x != null
    }

    /**
     * Remove a Store from this store.
     * @return If the Store existed, and was removed.
     */
    fun remove(store: X): Boolean {
        return this.remove(store.id)
    }

    /**
     * Removes all Stores from this storage. In the local case they are removed from cache, in the database case they are DELETED
     * @return How many objects were removed.
     */
    fun removeAll(): Long {
        val size = localStorage.size
        localStorage.clear()
        return size.toLong()
    }

    /**
     * Retrieve all Stores from this store.
     */
    fun getAll(): List<X> {
        return localStorage.values.toList()
    }

    /**
     * Retrieve all Store keys from this store.
     */
    fun getKeys(): List<K> {
        return localStorage.keys.toList()
    }

    /**
     * Retrieve all Store keys (in string form) from this store.
     * Uses [Collection.keyToString] to convert keys to strings.
     */
    fun getKeyStrings(collection: Collection<K, X>): List<String> {
        return localStorage.keys.map { collection.keyToString(it) }
    }

    /**
     * @return How many objects are in this Store
     */
    fun size(): Long {
        return localStorage.size.toLong()
    }

    /**
     * Gets the name of this storage layer.
     */
    abstract val layerName: String
}
