package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.CollectionUpdateResult
import com.kamikazejam.datastore.base.async.result.StoreResult

@Suppress("unused")
class AsyncUpdateHandler<K, X : Store<X, K>>(
    collection: Collection<*, *>,
    block: suspend () -> X?
) : AsyncHandler<X, CollectionUpdateResult<K, X>>(collection, block) {

    override fun wrapResult(result: Result<X?>): CollectionUpdateResult<K, X> = result.fold(
        onSuccess = { store -> 
            if (store != null) StoreResult.Success(store)
            else StoreResult.Failure(AsyncHandlerException("Update operation failed",
                IllegalStateException("Update operation returned null")
            ))
        },
        onFailure = { ex -> StoreResult.Failure(AsyncHandlerException("Update operation failed", ex)) }
    )
}