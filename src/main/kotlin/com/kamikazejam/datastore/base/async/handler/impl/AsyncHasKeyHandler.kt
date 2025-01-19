package com.kamikazejam.datastore.base.async.handler.impl

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.AsyncBoolResult

@Suppress("unused")
class AsyncHasKeyHandler(
    collection: Collection<*, *>,
    block: suspend () -> Boolean
) : AsyncHandler<Boolean, AsyncBoolResult>(collection, block) {

    override fun wrapResult(result: Result<Boolean?>): AsyncBoolResult = result.fold(
        onSuccess = { b ->
            return AsyncBoolResult.Success(b ?: false)
        },
        onFailure = { ex -> AsyncBoolResult.Failure(AsyncHandlerException("hasKey operation failed", ex)) }
    )
}