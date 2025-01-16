package com.kamikazejam.datastore.base.result

import com.kamikazejam.datastore.base.Cache
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function

/**
 * A wrapper around CompletableFuture that provides convenient methods for handling
 * both async and sync completions in the Bukkit/Spigot environment.
*/
@Suppress("unused")
class StoreResult<T> private constructor(private val future: CompletableFuture<T>, private val cache: Cache<*, *>) {

    /**
     * Handle the result asynchronously when it completes.
     * This is similar to CompletableFuture's whenComplete but with a more descriptive name.
     */
    fun handleAsync(handler: BiConsumer<T?, Throwable?>): StoreResult<T> {
        future.whenComplete(handler)
        return this
    }

    /**
     * Handle the result on the main thread when it completes.
     * This is particularly useful when you need to do Bukkit API calls that must be sync.
     */
    fun handleSync(handler: BiConsumer<T?, Throwable?>): StoreResult<T> {
        future.whenComplete { result: T?, throwable: Throwable? -> cache.runSync { handler.accept(result, throwable) } }
        return this
    }

    /**
     * Handle only successful completion asynchronously.
     */
    fun onSuccessAsync(handler: Consumer<T?>): StoreResult<T> {
        future.thenAccept(handler)
        return this
    }

    /**
     * Handle only successful completion on the main thread.
     */
    fun onSuccessSync(handler: Consumer<T?>): StoreResult<T> {
        future.thenAccept { result: T? -> cache.runSync { handler.accept(result) } }
        return this
    }

    /**
     * Handle only failures asynchronously.
     */
    fun onFailureAsync(handler: Consumer<Throwable?>): StoreResult<T> {
        future.exceptionally { throwable: Throwable? ->
            handler.accept(throwable)
            null
        }
        return this
    }

    /**
     * Handle only failures on the main thread.
     */
    fun onFailureSync(handler: Consumer<Throwable?>): StoreResult<T> {
        future.exceptionally { throwable: Throwable? ->
            cache.runSync { handler.accept(throwable) }
            null
        }
        return this
    }

    /**
     * Transform the result into another type asynchronously.
     */
    fun <U> transformAsync(transformer: Function<T?, U>): StoreResult<U> {
        return StoreResult(future.thenApply(transformer), cache)
    }

    /**
     * Transform the result into another type on the main thread.
     */
    fun <U> transformSync(transformer: Function<T?, U>): StoreResult<U> {
        val transformed = CompletableFuture<U>()
        future.whenComplete { result: T?, throwable: Throwable? ->
            if (throwable != null) {
                transformed.completeExceptionally(throwable)
            } else {
                cache.runSync {
                    try {
                        transformed.complete(transformer.apply(result))
                    } catch (t: Throwable) {
                        transformed.completeExceptionally(t)
                    }
                }
            }
        }
        return StoreResult(transformed, cache)
    }

    companion object {
        /**
         * Creates a new StoreResult from a CompletableFuture and a Cache.
         * The Cache is used to schedule sync tasks on the main thread when needed.
         */
        fun <T> of(future: CompletableFuture<T>, cache: Cache<*, *>): StoreResult<T> {
            return StoreResult(future, cache)
        }

        /**
         * Creates a completed StoreResult with the given value.
         */
        fun <T> completedResult(value: T, cache: Cache<*, *>): StoreResult<T> {
            return StoreResult(CompletableFuture.completedFuture(value), cache)
        }

        /**
         * Creates a failed StoreResult with the given exception.
         */
        fun <T> failedResult(ex: Throwable, cache: Cache<*, *>): StoreResult<T> {
            val future = CompletableFuture<T>()
            future.completeExceptionally(ex)
            return StoreResult(future, cache)
        }
    }
}