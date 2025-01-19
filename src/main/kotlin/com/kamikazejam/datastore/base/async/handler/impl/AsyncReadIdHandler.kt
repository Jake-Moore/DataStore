package com.kamikazejam.datastore.base.async.handler.impl

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.ReadIdResult

@Suppress("unused")
class AsyncReadIdHandler<K>(
    collection: Collection<*, *>,
    block: suspend () -> K?
) : AsyncHandler<K, ReadIdResult<K>>(collection, block) {
    override fun wrapResult(result: Result<K?>): ReadIdResult<K> = result.fold(
        onSuccess = { id ->
            if (id == null) return ReadIdResult.Empty()
            return ReadIdResult.Success(id)
        },
        onFailure = { ex -> ReadIdResult.Failure(AsyncHandlerException("Read ID operation failed", ex)) }
    )
}