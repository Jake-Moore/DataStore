package com.kamikazejam.datastore.mode.`object`

import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.result.StoreResult
import com.mongodb.*
import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.NonBlocking
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * Defines Object-specific getters for StoreObjects. They return non-null Optionals.
 */
@Suppress("unused", "BlockingMethodInNonBlockingContext")
interface ObjectCache<X : StoreObject<X>> : Cache<String, X> {
    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //
    /**
     * Create a new Store object with the provided initializer.<br></br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the cache. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @NonBlocking
    @Throws(DuplicateKeyException::class)
    fun create(initializer: Consumer<X>): StoreResult<X> {
        return StoreResult.of(CompletableFuture.supplyAsync { createSync(initializer) }, this)
    }

    // ----------------------------------------------------- //
    //                  CRUD Helpers (sync)                  //
    // ----------------------------------------------------- //
    /**
     * Create a new Store object with the provided initializer.<br></br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the cache. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @Blocking
    @Throws(DuplicateKeyException::class)
    fun createSync(initializer: Consumer<X>): X
}
