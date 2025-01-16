package com.kamikazejam.datastore.base.storage

import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.Store
import org.jetbrains.annotations.Blocking
import java.util.function.Consumer

/**
 * Adapts the cache-based api in [StorageMethods] to produce the simple api of [StorageDatabase].
 */
abstract class StorageDatabaseAdapter<K, X : Store<X, K>>(protected val cache: Cache<K, X>) :
    StorageMethods<K, X> {
    // ---------------------------------------------------------------- //
    //                     Abstraction Conversion                       //
    // ---------------------------------------------------------------- //
    protected abstract fun get(cache: Cache<K, X>, key: K): X?

    protected abstract fun save(cache: Cache<K, X>, store: X): Boolean

    @Blocking
    protected abstract fun updateSync(cache: Cache<K, X>, store: X, updateFunction: Consumer<X>): Boolean

    protected abstract fun has(cache: Cache<K, X>, key: K): Boolean

    protected abstract fun remove(cache: Cache<K, X>, key: K): Boolean

    protected abstract fun getAll(cache: Cache<K, X>): Iterable<X>

    protected abstract fun size(cache: Cache<K, X>): Long


    // ---------------------------------------------------------------- //
    //                           StoreMethods                           //
    // ---------------------------------------------------------------- //
    override operator fun get(key: K): X? {
        return this.get(this.cache, key)
    }

    override fun save(store: X): Boolean {
        return this.save(this.cache, store)
    }

    /**
     * Replace a Store in this database. This requires that we can find the Store in the database.<br></br>
     * If found, then the document in the database is replaced using a transaction. (providing atomicity)
     * @param updateFunction The function to update the Store with.
     * @return If the Store was replaced. (if the db was updated)
     */
    fun updateSync(store: X, updateFunction: Consumer<X>): Boolean {
        return this.updateSync(this.cache, store, updateFunction)
    }

    override fun has(key: K): Boolean {
        return this.has(this.cache, key)
    }

    override fun has(store: X): Boolean {
        return this.has(this.cache, store.id)
    }

    override fun remove(key: K): Boolean {
        return this.remove(this.cache, key)
    }

    override fun remove(store: X): Boolean {
        return this.remove(this.cache, store.id)
    }

    override val all: Iterable<X>
        get() = this.getAll(this.cache)

    override fun size(): Long {
        return this.size(this.cache)
    }
}
