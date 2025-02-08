package com.kamikazejam.datastore.base.async.handler.impl

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.Empty
import com.kamikazejam.datastore.base.async.result.Failure
import com.kamikazejam.datastore.base.async.result.OptionalResult
import com.kamikazejam.datastore.base.async.result.Success
import com.kamikazejam.datastore.base.metrics.MetricsListener

@Suppress("unused")
class AsyncReadIdHandler<K>(
    collection: Collection<*, *>,
    block: suspend () -> K?
) : AsyncHandler<K, OptionalResult<K>>(collection, block) {
    override fun wrapResult(result: Result<K?>): OptionalResult<K> = result.fold(
        onSuccess = { id ->
            if (id == null) return Empty()
            return Success(id)
        },
        onFailure = { ex ->
            DataStoreSource.metricsListeners.forEach(MetricsListener::onReadIdFail)
            Failure(AsyncHandlerException("Read ID operation failed", ex))
        }
    )
}