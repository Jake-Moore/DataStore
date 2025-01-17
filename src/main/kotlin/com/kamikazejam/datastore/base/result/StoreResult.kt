package com.kamikazejam.datastore.base.result

import com.kamikazejam.datastore.base.Store

sealed interface StoreResult<K, X : Store<X, K>> {
    data class Success<K, X : Store<X, K>>(val store: X) : StoreResult<K, X>
    data object Empty : StoreResult<Nothing, Nothing>
    data class Failure<K, X : Store<X, K>>(val error: Throwable) : StoreResult<K, X>

    fun getOrNull(): X? {
        return when (this) {
            is Success -> store
            else -> null
        }
    }
}