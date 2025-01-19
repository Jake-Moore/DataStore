@file:Suppress("unused")

package com.kamikazejam.datastore.base.async.result

import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException


// ------------------------------------------------------------ //
//                      Results Providing T                     //
// ------------------------------------------------------------ //
sealed interface AsyncResult<T>



// ------------------------------------------------------------ //
//                    Results Providing Store                   //
// ------------------------------------------------------------ //

sealed interface StoreResult<K, X : Store<X, K>> : AsyncResult<X> {
    data class Success<K, X : Store<X, K>>(val value: X) : StoreResult<K, X>, CollectionReadResult<K, X>, CollectionCreateResult<K, X>, CollectionUpdateResult<K, X>
    data class Failure<K, X : Store<X, K>>(val exception: AsyncHandlerException) : StoreResult<K, X>, CollectionReadResult<K, X>, CollectionCreateResult<K, X>, CollectionUpdateResult<K, X>

    fun getOrNull(): X? {
        return when (this) {
            is Success -> value
            else -> null
        }
    }
}

sealed interface CollectionReadResult<K, X : Store<X, K>> : StoreResult<K, X> {
    class Empty<K, X : Store<X, K>> : CollectionReadResult<K, X>
}

sealed interface CollectionCreateResult<K, X : Store<X, K>> : StoreResult<K, X>

sealed interface CollectionUpdateResult<K, X : Store<X, K>> : StoreResult<K, X>



// ------------------------------------------------------------ //
//                        Deletion Result                       //
// ------------------------------------------------------------ //

sealed interface CollectionDeleteResult : AsyncResult<Boolean> {
    data class Success(val value: Boolean) : CollectionDeleteResult
    data class Failure(val exception: AsyncHandlerException) : CollectionDeleteResult
}



// ------------------------------------------------------------ //
//                 Misc Results from Collections                //
// ------------------------------------------------------------ //
sealed interface AsyncBoolResult : AsyncResult<Boolean> {
    data class Success(val value: Boolean) : AsyncBoolResult
    data class Failure(val exception: AsyncHandlerException) : AsyncBoolResult
}

sealed interface CollectionReadIdResult<K> : AsyncResult<K> {
    data class Success<K>(val value: K) : CollectionReadIdResult<K>
    data class Failure<K>(val exception: AsyncHandlerException) : CollectionReadIdResult<K>
    class Empty<K> : CollectionReadIdResult<K>
}