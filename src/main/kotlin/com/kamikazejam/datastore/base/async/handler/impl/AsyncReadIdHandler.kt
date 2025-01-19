package com.kamikazejam.datastore.base.async.handler.impl

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.CollectionReadIdResult

@Suppress("unused")
class AsyncReadIdHandler<K>(
    collection: Collection<*, *>,
    block: suspend () -> K?
) : AsyncHandler<K, CollectionReadIdResult<K>>(collection, block) {
    override fun wrapResult(result: Result<K?>): CollectionReadIdResult<K> = result.fold(
        onSuccess = { id ->
            if (id == null) return CollectionReadIdResult.Empty()
            return CollectionReadIdResult.Success(id)
        },
        onFailure = { ex -> CollectionReadIdResult.Failure(AsyncHandlerException("Read ID operation failed", ex)) }
    )
}