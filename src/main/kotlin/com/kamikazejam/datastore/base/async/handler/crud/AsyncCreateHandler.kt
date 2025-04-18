package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.DefiniteResult
import com.kamikazejam.datastore.base.async.result.Failure
import com.kamikazejam.datastore.base.async.result.Success
import com.kamikazejam.datastore.base.metrics.MetricsListener
import com.kamikazejam.datastore.store.Store

@Suppress("unused")
class AsyncCreateHandler<K : Any, X : Store<X, K>>(
    collection: Collection<*, *>,
    block: suspend () -> X?
) : AsyncHandler<X, DefiniteResult<X>>(collection, block) {

    override fun wrapResult(result: Result<X?>): DefiniteResult<X> = result.fold(
        onSuccess = { store -> 
            if (store != null) Success(store)
            else {
                DataStoreSource.metricsListeners.forEach(MetricsListener::onCreateFail)
                Failure(AsyncHandlerException("Create operation failed",
                    IllegalStateException("Create operation returned null")
                ))
            }
        },
        onFailure = { ex ->
            DataStoreSource.metricsListeners.forEach(MetricsListener::onCreateFail)
            Failure(AsyncHandlerException("Create operation failed", ex))
        }
    )
}