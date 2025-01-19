package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.CollectionReadResult
import com.kamikazejam.datastore.base.async.result.StoreResult

@Suppress("unused")
class AsyncReadHandler<K, X : Store<X, K>>(
    collection: Collection<*, *>,
    block: suspend () -> X?
) : AsyncHandler<X, CollectionReadResult<K, X>>(collection, block) {
    override fun wrapResult(result: Result<X?>): CollectionReadResult<K, X> = result.fold(
        onSuccess = { store -> 
            if (store != null) StoreResult.Success(store)
            else CollectionReadResult.Empty()
        },
        onFailure = { ex -> StoreResult.Failure(AsyncHandlerException("Read operation failed", ex)) }
    )
}