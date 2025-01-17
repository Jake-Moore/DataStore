package com.kamikazejam.datastore.base.result

import com.kamikazejam.datastore.base.Collection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * A wrapper around coroutines that provides convenient methods for handling
 * both async and sync completions in the Bukkit/Spigot environment.
*/
@Suppress("unused")
class AsyncHandler<T>(
    private val collection: Collection<*, *>,
    private val block: suspend () -> T?
) : CoroutineScope {
    override val coroutineContext = Dispatchers.IO
    private val deferred = async { runCatching { block() } }

    /**
     * Handle the [CollectionResult] when it completes.
     * @param onMainThread Whether to run the closure on the main bukkit thread (true) or async (false).
     */
    fun handle(onMainThread: Boolean, closure: (result: CollectionResult<out T>) -> Unit) {
        launch {
            val result = deferred.await()
            val collectionResult = result.fold(
                onSuccess = { store -> 
                    if (store != null) CollectionResult.Success(store)
                    else CollectionResult.Empty
                },
                onFailure = { ex -> CollectionResult.Failure(ex) }
            )
            if (onMainThread) collection.runSync{ closure(collectionResult) } else closure(collectionResult)
        }
    }

    /**
     * Blocks until the result is available and returns it.
     * This should only be used when you absolutely need to wait for the result.
     */
    suspend fun await(): CollectionResult<out T> {
        val result = deferred.await()
        return result.fold(
            onSuccess = { store -> 
                if (store != null) CollectionResult.Success(store)
                else CollectionResult.Empty
            },
            onFailure = { ex -> CollectionResult.Failure(ex) }
        )
    }
}