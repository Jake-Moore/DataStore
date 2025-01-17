package com.kamikazejam.datastore.base.result

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import kotlinx.coroutines.*
import org.jetbrains.annotations.Blocking

/**
 * A wrapper around coroutines that provides convenient methods for handling
 * both async and sync completions in the Bukkit/Spigot environment.
*/
@Suppress("unused")
class AsyncStoreHandler<K, X : Store<X, K>>(
    private val collection: Collection<K, X>,
    private val block: suspend () -> X?
) : CoroutineScope {
    override val coroutineContext = Dispatchers.IO
    private val deferred = async { runCatching { block() } }

    /**
     * Handle the [StoreResult] when it completes.
     * @param onMainThread Whether to run the closure on the main bukkit thread (true) or async (false).
     */
    fun handle(onMainThread: Boolean, closure: (result: StoreResult<out K, out X>) -> Unit) {
        launch {
            val result = deferred.await()
            val storeResult = result.fold(
                onSuccess = { store -> 
                    if (store != null) StoreResult.Success(store) 
                    else StoreResult.Empty 
                },
                onFailure = { ex -> StoreResult.Failure(ex) }
            )
            if (onMainThread) collection.runSync{ closure(storeResult) } else closure(storeResult)
        }
    }

    /**
     * Blocks until the result is available and returns it.
     * This should only be used when you absolutely need to wait for the result.
     */
    @Blocking
    suspend fun await(): StoreResult<out K, out X> {
        val result = deferred.await()
        return result.fold(
            onSuccess = { store -> 
                if (store != null) StoreResult.Success(store) 
                else StoreResult.Empty 
            },
            onFailure = { ex -> StoreResult.Failure(ex) }
        )
    }
}