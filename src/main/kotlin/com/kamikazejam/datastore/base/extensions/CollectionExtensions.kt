@file:Suppress("unused")

package com.kamikazejam.datastore.base.extensions

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.result.AsyncStoreHandler
import com.kamikazejam.datastore.base.result.StoreResult
import java.util.function.Consumer

/**
 * Get a Store object from the Collection or create a new one if it doesn't exist.<br></br>
 * This specific method will override any key set in the initializer. Since the key is an argument.
 * @param key The key of the Store to get or create.
 * @param initializer The initializer for the Store if it doesn't exist.
 * @return The Store object. (READ-ONLY) (fetched or created)
 */
fun <K, X : Store<X, K>> Collection<K, X>.readOrCreate(key: K, initializer: Consumer<X>): AsyncStoreHandler<K, X> {
    return AsyncStoreHandler(this) {
        when (val readResult = read(key).await()) {
            is StoreResult.Success -> return@AsyncStoreHandler readResult.store
            is StoreResult.Failure -> throw readResult.error
            is StoreResult.Empty -> {
                when (val createResult = create(key, initializer).await()) {
                    is StoreResult.Success -> return@AsyncStoreHandler createResult.store
                    is StoreResult.Failure -> throw createResult.error
                    is StoreResult.Empty -> throw IllegalStateException("Failed to create a new Store object.")
                }
            }
        }
    }
}