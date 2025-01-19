package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.CreateResult

@Suppress("unused")
class AsyncCreateHandler<K, X : Store<X, K>>(
    collection: Collection<*, *>,
    block: suspend () -> X?
) : AsyncHandler<X, CreateResult<K, X>>(collection, block) {

    override fun wrapResult(result: Result<X?>): CreateResult<K, X> = result.fold(
        onSuccess = { store -> 
            if (store != null) CreateResult.Success(store)
            else CreateResult.Failure(AsyncHandlerException("Create operation failed",
                IllegalStateException("Create operation returned null")
            ))
        },
        onFailure = { ex -> CreateResult.Failure(AsyncHandlerException("Create operation failed", ex)) }
    )
}