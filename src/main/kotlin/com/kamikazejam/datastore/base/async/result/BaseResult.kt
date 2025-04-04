@file:Suppress("unused")

package com.kamikazejam.datastore.base.async.result

import com.kamikazejam.datastore.base.exception.update.RejectUpdateException

// Base interface with common functionality
sealed interface BaseResult<T> {
    fun isSuccess(): Boolean
    fun isFailure(): Boolean
    fun getOrNull(): T?
    fun exceptionOrNull(): Throwable?
}

// For operations that cannot return Empty
sealed interface DefiniteResult<T> : BaseResult<T>

/**
 * For update operations (cannot return empty)
 * Update operations may be rejected by throwing an instance of [RejectUpdateException] in the update function
 */
sealed interface UpdateResult<T> : BaseResult<T> {
    fun isRejected(): Boolean
}

// For operations that can return Empty
sealed interface OptionalResult<T> : BaseResult<T> {
    fun isEmpty(): Boolean
}

// Helper function for non-empty results
inline fun <T, R> DefiniteResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R
): R = when (this) {
    is Success -> onSuccess(value)
    is Failure -> onFailure(error)
}

// Helper function for emptyable results
inline fun <T, R> OptionalResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R,
    onEmpty: () -> R
): R = when (this) {
    is Success -> onSuccess(value)
    is Failure -> onFailure(error)
    is Empty -> onEmpty()
}