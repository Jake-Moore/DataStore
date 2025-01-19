package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.DeleteResult

@Suppress("unused")
class AsyncDeleteHandler(
    collection: Collection<*, *>,
    block: suspend () -> Boolean?
) : AsyncHandler<Boolean, DeleteResult>(collection, block) {

    override fun wrapResult(result: Result<Boolean?>): DeleteResult = result.fold(
        onSuccess = { store ->
            return DeleteResult.Success(store ?: false)
        },
        onFailure = { ex -> DeleteResult.Failure(AsyncHandlerException("Delete operation failed", ex)) }
    )
}