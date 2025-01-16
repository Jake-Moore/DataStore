package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.connections.storage.iterator.TransformingIterator
import java.util.function.Consumer

/**
 * Wraps up the StorageService with the Cache backing the Stores, and exposes ObjectStore methods
 *
 * @param <X>
</X> */
abstract class StorageDatabase<K, X : Store<X, K>>(cache: Cache<K, X>) :
    StorageDatabaseAdapter<K, X>(cache) {
    private val storageService = DataStoreSource.storageService


    // ----------------------------------------------------- //
    //                     Store Methods                     //
    // ----------------------------------------------------- //
    override fun clear(): Long {
        // For safety reasons...
        throw UnsupportedOperationException("Cannot clear a MongoDB database from within DataStore.")
    }

    override val isDatabase: Boolean
        get() = true


    // ---------------------------------------------------------------- //
    //               Map StoreStorage to StorageService                 //
    // ---------------------------------------------------------------- //
    override fun get(cache: Cache<K, X>, key: K): X? {
        // Fetch the Store from the database
        val o = storageService.get(cache, key)
        // Ensure Store knows its Cache
        o?.setCache(cache)
        // Return the Store
        return o
    }

    override fun save(cache: Cache<K, X>, store: X): Boolean {
        // All saves to Database Storage run through here
        return storageService.save(cache, store)
    }

    override fun updateSync(cache: Cache<K, X>, store: X, updateFunction: Consumer<X>): Boolean {
        return storageService.updateSync(cache, store, updateFunction)
    }

    override fun has(cache: Cache<K, X>, key: K): Boolean {
        return storageService.has(cache, key)
    }

    override fun remove(cache: Cache<K, X>, key: K): Boolean {
        return storageService.remove(cache, key)
    }

    public override fun getAll(cache: Cache<K, X>): Iterable<X> {
        // Fetch the storageService's Iterable
        val storage: Iterator<X> = storageService.getAll(cache).iterator()
        return Iterable {
            TransformingIterator(storage) { x: X ->
                // Make sure to set the cache and cacheCopy as we load the Stores
                x.also { it.setCache(cache) }
            }
        }
    }

    override val keys: Iterable<K>
        get() = storageService.getKeys(cache)

    override fun getKeyStrings(cache: Cache<K, X>): Iterable<String> {
        val keys: Iterator<K> = storageService.getKeys(cache).iterator()
        return Iterable {
            TransformingIterator(keys) {
                key: K -> cache.keyToString(key)
            }
        }
    }

    override fun size(cache: Cache<K, X>): Long {
        return storageService.size(cache)
    }
}
