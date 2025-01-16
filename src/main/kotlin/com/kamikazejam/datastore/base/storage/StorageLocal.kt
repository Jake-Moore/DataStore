package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.util.DataStoreFileLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.stream.Collectors

abstract class StorageLocal<K, X : Store<X, K>> : StorageMethods<K, X> {
    val localCache: ConcurrentMap<K, X> = ConcurrentHashMap()

    // ----------------------------------------------------- //
    //                     Store Methods                     //
    // ----------------------------------------------------- //
    override operator fun get(key: K): X? {
        return localCache[key]
    }

    override fun save(store: X): Boolean {
        // Ensure we don't re-cache an invalid Store
        if (!store.valid) {
            DataStoreFileLogger.warn(
                store.getCache(),
                "StoreLocal.save(X) w/ Invalid Store. Cache: " + store.getCache().name + ", Id: " + store.id
            )
            return false
        }

        // If not called already, call initialized (since we're caching it)
        localCache[store.id] = store
        return true
    }

    override fun has(key: K): Boolean {
        return localCache.containsKey(key)
    }

    override fun has(store: X): Boolean {
        return this.has(store.id)
    }

    override fun remove(key: K): Boolean {
        val x = localCache.remove(key)
        x?.invalidate()
        return x != null
    }

    override fun remove(store: X): Boolean {
        return this.remove(store.id)
    }

    override val all: Iterable<X>
        get() = localCache.values

    override val keys: Iterable<K>
        get() = localCache.keys

    override fun getKeyStrings(cache: Cache<K, X>): Set<String> {
        return localCache.keys.stream().map { key: K -> cache.keyToString(key) }.collect(Collectors.toSet())
    }

    override fun clear(): Long {
        val size = localCache.size
        localCache.clear()
        return size.toLong()
    }

    override fun size(): Long {
        return localCache.size.toLong()
    }

    override val isDatabase: Boolean
        get() = false
}
