package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.exception.update.UpdateException
import com.kamikazejam.datastore.store.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Wraps up the StorageService with the Collection backing the Stores, and exposes ObjectStore methods
 *
 * @param <X>
</X> */
abstract class StorageDatabase<K : Any, X : Store<X, K>>(collection: Collection<K, X>) : StorageDatabaseAdapter<K, X>(collection) {
    private val storageService = DataStoreSource.storageService

    // ---------------------------------------------------------------- //
    //               Map StoreStorage to StorageService                 //
    // ---------------------------------------------------------------- //
    override suspend fun read(collection: Collection<K, X>, key: K): X? {
        // Fetch the Store from the database
        val o = storageService.read(collection, key)
        // Ensure Store knows its Collection
        o?.initialize(collection)
        // Return the Store
        return o
    }

    override suspend fun save(collection: Collection<K, X>, store: X): Boolean {
        // All saves to Database Storage run through here
        return storageService.save(collection, store)
    }

    @Throws(UpdateException::class)
    override suspend fun update(collection: Collection<K, X>, store: X, updateFunction: (X) -> X): X {
        return storageService.update(collection, store, updateFunction)
    }

    override suspend fun has(collection: Collection<K, X>, key: K): Boolean {
        return storageService.has(collection, key)
    }

    override suspend fun delete(collection: Collection<K, X>, key: K): Boolean {
        return storageService.delete(collection, key)
    }

    override suspend fun deleteAll(collection: Collection<K, X>): Long {
        return storageService.deleteAll(collection)
    }

    public override suspend fun readAll(collection: Collection<K, X>): Flow<X> {
        // Fetch the storageService's Iterable
        return storageService.readAll(collection).map { store: X ->
            // Make sure to set the collection as we load the Stores
            store.also { it.initialize(collection) }
        }
    }

    override suspend fun readKeys(collection: Collection<K, X>): Flow<K> {
        return storageService.readKeys(collection)
    }

    override suspend fun size(collection: Collection<K, X>): Long {
        return storageService.size(collection)
    }
}
