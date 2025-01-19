package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.ReadResult

@Suppress("unused")
class AsyncReadHandler<K, X : Store<X, K>>(
    collection: Collection<*, *>,
    block: suspend () -> X?
) : AsyncHandler<X, ReadResult<K, X>>(collection, block) {
    override fun wrapResult(result: Result<X?>): ReadResult<K, X> = result.fold(
        onSuccess = { store -> 
            if (store != null) ReadResult.Success(store)
            else ReadResult.Empty()
        },
        onFailure = { ex -> ReadResult.Failure(AsyncHandlerException("Read operation failed", ex)) }
    )
}