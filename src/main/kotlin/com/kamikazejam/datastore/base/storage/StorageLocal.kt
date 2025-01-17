package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.util.DataStoreFileLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.stream.Collectors

abstract class StorageLocal<K, X : Store<X, K>> : StorageMethods<K, X> {
    val localStorage: ConcurrentMap<K, X> = ConcurrentHashMap()

    // ----------------------------------------------------- //
    //                     Store Methods                     //
    // ----------------------------------------------------- //
    override operator fun get(key: K): X? {
        return localStorage[key]
    }

    override fun save(store: X): Boolean {
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

    override fun has(key: K): Boolean {
        return localStorage.containsKey(key)
    }

    override fun has(store: X): Boolean {
        return this.has(store.id)
    }

    override fun remove(key: K): Boolean {
        val x = localStorage.remove(key)
        x?.invalidate()
        return x != null
    }

    override fun remove(store: X): Boolean {
        return this.remove(store.id)
    }

    override val all: Iterable<X>
        get() = localStorage.values

    override val keys: Iterable<K>
        get() = localStorage.keys

    override fun getKeyStrings(collection: Collection<K, X>): Set<String> {
        return localStorage.keys.stream().map { key: K -> collection.keyToString(key) }.collect(Collectors.toSet())
    }

    override fun clear(): Long {
        val size = localStorage.size
        localStorage.clear()
        return size.toLong()
    }

    override fun size(): Long {
        return localStorage.size.toLong()
    }

    override val isDatabase: Boolean
        get() = false
}
