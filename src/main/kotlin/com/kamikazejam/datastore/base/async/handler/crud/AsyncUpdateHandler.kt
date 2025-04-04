package com.kamikazejam.datastore.base.async.handler.crud

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException
import com.kamikazejam.datastore.base.async.handler.AsyncHandler
import com.kamikazejam.datastore.base.async.result.Failure
import com.kamikazejam.datastore.base.async.result.Reject
import com.kamikazejam.datastore.base.async.result.Success
import com.kamikazejam.datastore.base.async.result.UpdateResult
import com.kamikazejam.datastore.base.exception.update.RejectUpdateException
import com.kamikazejam.datastore.base.exception.update.UpdateException
import com.kamikazejam.datastore.base.metrics.MetricsListener
import com.kamikazejam.datastore.store.Store

@Suppress("unused")
class AsyncUpdateHandler<K : Any, X : Store<X, K>>(
    collection: Collection<*, *>,
    block: suspend () -> X?
) : AsyncHandler<X, UpdateResult<X>>(collection, block) {

    override fun wrapResult(result: Result<X?>): UpdateResult<X> = result.fold(
        onSuccess = { store -> 
            if (store != null) Success(store)
            else {
                DataStoreSource.metricsListeners.forEach(MetricsListener::onUpdateFail)
                Failure(AsyncHandlerException("Update operation failed",
                    IllegalStateException("Update operation returned null")
                ))
            }
        },
        onFailure = { ex ->
            val rejectException = getRejectException(ex)
            if (rejectException != null) {
                Reject(rejectException)
            } else {
                // This is a full failure, not a rejection
                DataStoreSource.metricsListeners.forEach(MetricsListener::onUpdateFail)
                if (ex is UpdateException) {
                    Failure(AsyncHandlerException(ex.message, ex))
                } else {
                    Failure(AsyncHandlerException("Update operation failed", ex))
                }
            }
        }
    )

    private fun getRejectException(exception: Throwable): RejectUpdateException? {
        val cause = exception.cause
        return if (exception is RejectUpdateException) {
            exception
        } else if (cause is RejectUpdateException) {
            cause
        } else if (cause != null && cause.cause is RejectUpdateException) {
            // If its more than 2 levels deep, I feel like that's an issue with the user's code
            cause.cause as RejectUpdateException
        } else {
            null
        }
    }
}