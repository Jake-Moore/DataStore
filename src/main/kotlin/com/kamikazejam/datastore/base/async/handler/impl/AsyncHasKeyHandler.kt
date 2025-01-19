package com.kamikazejam.datastore.base.async.handler.impl

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.BoolResult

@Suppress("unused")
class AsyncHasKeyHandler(
    collection: Collection<*, *>,
    block: suspend () -> Boolean
) : AsyncHandler<Boolean, BoolResult>(collection, block) {

    override fun wrapResult(result: Result<Boolean?>): BoolResult = result.fold(
        onSuccess = { b ->
            return BoolResult.Success(b ?: false)
        },
        onFailure = { ex -> BoolResult.Failure(AsyncHandlerException("hasKey operation failed", ex)) }
    )
}