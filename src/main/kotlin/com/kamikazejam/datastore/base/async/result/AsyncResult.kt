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
    data class Success<K, X : Store<X, K>>(val value: X) : StoreResult<K, X>
    data class Failure<K, X : Store<X, K>>(val exception: AsyncHandlerException) : StoreResult<K, X>

    fun getOrNull(): X? {
        return when (this) {
            is Success -> value
            else -> null
        }
    }
}

sealed interface ReadResult<K, X : Store<X, K>> : StoreResult<K, X> {
    data class Success<K, X : Store<X, K>>(val value: X) : ReadResult<K, X>
    data class Failure<K, X : Store<X, K>>(val exception: AsyncHandlerException) : ReadResult<K, X>
    class Empty<K, X : Store<X, K>> : ReadResult<K, X>
}

sealed interface CreateResult<K, X : Store<X, K>> : StoreResult<K, X> {
    data class Success<K, X : Store<X, K>>(val value: X) : CreateResult<K, X>
    data class Failure<K, X : Store<X, K>>(val exception: AsyncHandlerException) : CreateResult<K, X>
}

sealed interface UpdateResult<K, X : Store<X, K>> : StoreResult<K, X> {
    data class Success<K, X : Store<X, K>>(val value: X) : UpdateResult<K, X>
    data class Failure<K, X : Store<X, K>>(val exception: AsyncHandlerException) : UpdateResult<K, X>
}



// ------------------------------------------------------------ //
//                        Deletion Result                       //
// ------------------------------------------------------------ //

sealed interface DeleteResult : AsyncResult<Boolean> {
    data class Success(val value: Boolean) : DeleteResult
    data class Failure(val exception: AsyncHandlerException) : DeleteResult
}



// ------------------------------------------------------------ //
//                 Misc Results from Collections                //
// ------------------------------------------------------------ //
sealed interface BoolResult : AsyncResult<Boolean> {
    data class Success(val value: Boolean) : BoolResult
    data class Failure(val exception: AsyncHandlerException) : BoolResult
}

sealed interface ReadIdResult<K> : AsyncResult<K> {
    data class Success<K>(val value: K) : ReadIdResult<K>
    data class Failure<K>(val exception: AsyncHandlerException) : ReadIdResult<K>
    class Empty<K> : ReadIdResult<K>
}