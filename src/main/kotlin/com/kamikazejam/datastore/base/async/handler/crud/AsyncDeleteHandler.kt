package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.DefiniteResult
import com.kamikazejam.datastore.base.async.result.Failure
import com.kamikazejam.datastore.base.async.result.Success

@Suppress("unused")
class AsyncDeleteHandler(
    collection: Collection<*, *>,
    block: suspend () -> Boolean?
) : AsyncHandler<Boolean, DefiniteResult<Boolean>>(collection, block) {

    override fun wrapResult(result: Result<Boolean?>): DefiniteResult<Boolean> = result.fold(
        onSuccess = { store ->
            return Success(store ?: false)
        },
        onFailure = { ex -> Failure(AsyncHandlerException("Delete operation failed", ex)) }
    )
}