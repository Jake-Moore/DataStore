package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.connections.storage.iterator.TransformingIterator
import java.util.function.Consumer

/**
 * Wraps up the StorageService with the Collection backing the Stores, and exposes ObjectStore methods
 *
 * @param <X>
</X> */
abstract class StorageDatabase<K, X : Store<X, K>>(collection: Collection<K, X>) :
    StorageDatabaseAdapter<K, X>(collection) {
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
    override fun get(collection: Collection<K, X>, key: K): X? {
        // Fetch the Store from the database
        val o = storageService.get(collection, key)
        // Ensure Store knows its Collection
        o?.setCollection(collection)
        // Return the Store
        return o
    }

    override fun save(collection: Collection<K, X>, store: X): Boolean {
        // All saves to Database Storage run through here
        return storageService.save(collection, store)
    }

    override fun updateSync(collection: Collection<K, X>, store: X, updateFunction: Consumer<X>): Boolean {
        return storageService.updateSync(collection, store, updateFunction)
    }

    override fun has(collection: Collection<K, X>, key: K): Boolean {
        return storageService.has(collection, key)
    }

    override fun remove(collection: Collection<K, X>, key: K): Boolean {
        return storageService.remove(collection, key)
    }

    public override fun getAll(collection: Collection<K, X>): Iterable<X> {
        // Fetch the storageService's Iterable
        val storage: Iterator<X> = storageService.getAll(collection).iterator()
        return Iterable {
            TransformingIterator(storage) { x: X ->
                // Make sure to set the collection as we load the Stores
                x.also { it.setCollection(collection) }
            }
        }
    }

    override val keys: Iterable<K>
        get() = storageService.getKeys(collection)

    override fun getKeyStrings(collection: Collection<K, X>): Iterable<String> {
        val keys: Iterator<K> = storageService.getKeys(collection).iterator()
        return Iterable {
            TransformingIterator(keys) {
                key: K -> collection.keyToString(key)
            }
        }
    }

    override fun size(collection: Collection<K, X>): Long {
        return storageService.size(collection)
    }
}
