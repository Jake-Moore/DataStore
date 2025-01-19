package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.UpdateResult

@Suppress("unused")
class AsyncUpdateHandler<K, X : Store<X, K>>(
    collection: Collection<*, *>,
    block: suspend () -> X?
) : AsyncHandler<X, UpdateResult<K, X>>(collection, block) {

    override fun wrapResult(result: Result<X?>): UpdateResult<K, X> = result.fold(
        onSuccess = { store -> 
            if (store != null) UpdateResult.Success(store)
            else UpdateResult.Failure(AsyncHandlerException("Update operation failed",
                IllegalStateException("Update operation returned null")
            ))
        },
        onFailure = { ex -> UpdateResult.Failure(AsyncHandlerException("Update operation failed", ex)) }
    )
}