@file:Suppress("unused")

package com.kamikazejam.datastore.base.extensions

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.result.AsyncHandler
import com.kamikazejam.datastore.base.result.CollectionResult
import org.jetbrains.annotations.NonBlocking
import java.util.function.Consumer

/**
 * Get a Store object from the Collection or create a new one if it doesn't exist.<br></br>
 * This specific method will override any key set in the initializer. Since the key is an argument.
 * @param key The key of the Store to get or create.
 * @param initializer The initializer for the Store if it doesn't exist.
 * @return The Store object. (READ-ONLY) (fetched or created)
 */
fun <K, X : Store<X, K>> Collection<K, X>.readOrCreate(key: K, initializer: Consumer<X> = Consumer {}): AsyncHandler<X> {
    return AsyncHandler(this) {
        when (val readResult = read(key).await()) {
            is CollectionResult.Success -> return@AsyncHandler readResult.value
            is CollectionResult.Failure -> throw readResult.error
            is CollectionResult.Empty -> {
                when (val createResult = create(key, initializer).await()) {
                    is CollectionResult.Success -> return@AsyncHandler createResult.value
                    is CollectionResult.Failure -> throw createResult.error
                    is CollectionResult.Empty -> throw IllegalStateException("Failed to create a new Store object.")
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
fun <K, X : Store<X, K>> Collection<K, X>.update(store: X, updateFunction: Consumer<X>): AsyncHandler<X> {
    return this.update(store.id, updateFunction)
}

/**
 * Deletes a Store by ID (removes from both cache and database collection)
 * @return True if the Store was deleted, false if it was not found (does not exist)
 */
fun <K, X : Store<X, K>> Collection<K, X>.delete(store: X): AsyncHandler<Boolean> {
    return this.delete(store.id)
}