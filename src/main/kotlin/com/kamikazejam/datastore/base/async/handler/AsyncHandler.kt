package com.kamikazejam.datastore.base.async.handler

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.result.AsyncResult
import com.kamikazejam.datastore.base.coroutine.DataStoreScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * A wrapper around coroutines that provides convenient methods for handling
 * both async and sync completions in the Bukkit/Spigot environment.
*/
@Suppress("unused")
abstract class AsyncHandler<T, R : AsyncResult<T>>(
    protected val collection: Collection<*, *>,
    protected val block: suspend () -> T?
) : DataStoreScope {
    private val deferred = async { runCatching { block() } }

    protected abstract fun wrapResult(result: Result<T?>): R

    /**
     * Handle the result when it completes.
     * @param onMainThread Whether to run the closure on the main bukkit thread (true) or async (false).
     */
    fun handle(onMainThread: Boolean, closure: (result: R) -> Unit) {
        launch {
            val result = deferred.await()
            val collectionResult = wrapResult(result)
            if (onMainThread) collection.runSync{ closure(collectionResult) } else closure(collectionResult)
        }
    }

    /**
     * Blocks until the result is available and returns it.
     * This should only be used when you absolutely need to wait for the result.
     */
    suspend fun await(): R {
        val result = deferred.await()
        return wrapResult(result)
    }
} 