package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.CollectionDeleteResult

@Suppress("unused")
class AsyncDeleteHandler(
    collection: Collection<*, *>,
    block: suspend () -> Boolean?
) : AsyncHandler<Boolean, CollectionDeleteResult>(collection, block) {

    override fun wrapResult(result: Result<Boolean?>): CollectionDeleteResult = result.fold(
        onSuccess = { store ->
            return CollectionDeleteResult.Success(store ?: false)
        },
        onFailure = { ex -> CollectionDeleteResult.Failure(AsyncHandlerException("Delete operation failed", ex)) }
    )
}