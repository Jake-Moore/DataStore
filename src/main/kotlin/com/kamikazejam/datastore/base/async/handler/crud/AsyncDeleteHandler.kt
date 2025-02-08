package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.DefiniteResult
import com.kamikazejam.datastore.base.async.result.Failure
import com.kamikazejam.datastore.base.async.result.Success
import com.kamikazejam.datastore.base.metrics.MetricsListener

@Suppress("unused")
class AsyncDeleteHandler(
    collection: Collection<*, *>,
    block: suspend () -> Boolean?
) : AsyncHandler<Boolean, DefiniteResult<Boolean>>(collection, block) {

    override fun wrapResult(result: Result<Boolean?>): DefiniteResult<Boolean> = result.fold(
        onSuccess = { store ->
            return Success(store ?: false)
        },
        onFailure = { ex ->
            DataStoreSource.metricsListeners.forEach(MetricsListener::onDeleteFail)
            Failure(AsyncHandlerException("Delete operation failed", ex))
        }
    )
}