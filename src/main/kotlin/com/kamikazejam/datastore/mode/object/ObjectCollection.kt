package com.kamikazejam.datastore.mode.`object`

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.handler.crud.AsyncCreateHandler
import com.mongodb.*
import java.util.function.Consumer

/**
 * Defines Object-specific getters for StoreObjects. They return non-null Optionals.
 */
@Suppress("unused")
interface ObjectCollection<X : StoreObject<X>> : Collection<String, X> {
    // ----------------------------------------------------- //
    //                 CRUD Helpers (Async)                  //
    // ----------------------------------------------------- //
    /**
     * Create a new Store object with the provided initializer.<br></br>
     * If you have a specific key for this Store, set it in the initializer.
     * @throws DuplicateKeyException If the key already exists in the Collection. (failed to create)
     * @return The created Store object. (READ-ONLY)
     */
    @Throws(DuplicateKeyException::class)
    fun create(initializer: Consumer<X> = Consumer {}): AsyncCreateHandler<String, X>
}
