package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import org.jetbrains.annotations.Blocking
import java.util.function.Consumer

/**
 * Adapts the collection-based api in [StorageMethods] to produce the simple api of [StorageDatabase].
 */
abstract class StorageDatabaseAdapter<K, X : Store<X, K>>(protected val collection: Collection<K, X>) :
    StorageMethods<K, X> {
    // ---------------------------------------------------------------- //
    //                     Abstraction Conversion                       //
    // ---------------------------------------------------------------- //
    protected abstract fun get(collection: Collection<K, X>, key: K): X?

    protected abstract fun save(collection: Collection<K, X>, store: X): Boolean

    @Blocking
    protected abstract fun updateSync(collection: Collection<K, X>, store: X, updateFunction: Consumer<X>): Boolean

    protected abstract fun has(collection: Collection<K, X>, key: K): Boolean

    protected abstract fun remove(collection: Collection<K, X>, key: K): Boolean

    protected abstract fun getAll(collection: Collection<K, X>): Iterable<X>

    protected abstract fun size(collection: Collection<K, X>): Long


    // ---------------------------------------------------------------- //
    //                           StoreMethods                           //
    // ---------------------------------------------------------------- //
    override operator fun get(key: K): X? {
        return this.get(this.collection, key)
    }

    override fun save(store: X): Boolean {
        return this.save(this.collection, store)
    }

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br></br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @param updateFunction The function to update the Store with.
     * @return If the Store was replaced. (if the db was updated)
     */
    fun updateSync(store: X, updateFunction: Consumer<X>): Boolean {
        return this.updateSync(this.collection, store, updateFunction)
    }

    override fun has(key: K): Boolean {
        return this.has(this.collection, key)
    }

    override fun has(store: X): Boolean {
        return this.has(this.collection, store.id)
    }

    override fun remove(key: K): Boolean {
        return this.remove(this.collection, key)
    }

    override fun remove(store: X): Boolean {
        return this.remove(this.collection, store.id)
    }

    override val all: Iterable<X>
        get() = this.getAll(this.collection)

    override fun size(): Long {
        return this.size(this.collection)
    }
}
