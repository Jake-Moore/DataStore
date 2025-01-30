package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.Empty
import com.kamikazejam.datastore.base.async.result.Failure
import com.kamikazejam.datastore.base.async.result.OptionalResult
import com.kamikazejam.datastore.base.async.result.Success
import com.kamikazejam.datastore.mode.store.Store

@Suppress("unused")
class AsyncReadHandler<K : Any, X : Store<X, K>>(
    collection: Collection<*, *>,
    block: suspend () -> X?
) : AsyncHandler<X, OptionalResult<X>>(collection, block) {
    override fun wrapResult(result: Result<X?>): OptionalResult<X> = result.fold(
        onSuccess = { store -> 
            if (store != null) Success(store)
            else Empty()
        },
        onFailure = { ex -> Failure(AsyncHandlerException("Read operation failed", ex)) }
    )
}