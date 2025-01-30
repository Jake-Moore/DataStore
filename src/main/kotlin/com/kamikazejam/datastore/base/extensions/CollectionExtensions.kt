@file:Suppress("unused")

package com.kamikazejam.datastore.base.extensions

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.handler.crud.AsyncCreateHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.base.async.result.Empty
import com.kamikazejam.datastore.base.async.result.Failure
import com.kamikazejam.datastore.base.async.result.Success
import com.kamikazejam.datastore.mode.store.Store
import org.jetbrains.annotations.NonBlocking

/**
 * Get a Store object from the Collection or create a new one if it doesn't exist.<br></br>
 * This specific method will override any key set in the initializer. Since the key is an argument.
 * @param key The key of the Store to get or create.
 * @param initializer The initializer for the Store if it doesn't exist.
 * @return The Store object. (READ-ONLY) (fetched or created)
 */
fun <K : Any, X : Store<X, K>> Collection<K, X>.readOrCreate(key: K, initializer: (X) -> X = { it -> it }): AsyncCreateHandler<K, X> {
    return AsyncCreateHandler(this) {
        when (val readResult = read(key).await()) {
            is Success -> return@AsyncCreateHandler readResult.value
            is Failure -> throw readResult.error
            is Empty -> {
                when (val createResult = create(key, initializer).await()) {
                    is Success -> return@AsyncCreateHandler createResult.value
                    is Failure -> throw createResult.error
                }
            }
        }
    }
}

/**
 * Modifies a Store in a controlled environment where modifications are allowed
 * @throws NoSuchElementException if the Store (by this key) is not found
 * @return The updated Store object. (READ-ONLY)
 */
@NonBlocking
fun <K : Any, X : Store<X, K>> Collection<K, X>.update(store: X, updateFunction: (X) -> X): AsyncUpdateHandler<K, X> {
    return this.update(store.id, updateFunction)
}

/**
 * Deletes a Store by ID (removes from both cache and database collection)
 * @return True if the Store was deleted, false if it was not found (does not exist)
 */
fun <K : Any, X : Store<X, K>> Collection<K, X>.delete(store: X): AsyncDeleteHandler {
    return this.delete(store.id)
}