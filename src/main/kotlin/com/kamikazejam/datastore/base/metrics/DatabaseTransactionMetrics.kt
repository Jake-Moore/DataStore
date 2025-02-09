package com.kamikazejam.datastore.base.metrics

sealed interface DatabaseTransactionMetrics {
    fun onTryUpdateTransaction()
    fun onUpdateTransactionLimitReached()

    fun onTimerUpdateTransaction(milliseconds: Long)
    fun onTimerUpdatesSuccess(milliseconds: Long)
}