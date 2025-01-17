package com.kamikazejam.datastore.base.result

sealed interface CollectionResult<T> {
    data class Success<T>(val value: T) : CollectionResult<T>
    data object Empty : CollectionResult<Nothing>
    data class Failure<T>(val error: Throwable) : CollectionResult<T>

    fun getOrNull(): T? {
        return when (this) {
            is Success -> value
            else -> null
        }
    }
}