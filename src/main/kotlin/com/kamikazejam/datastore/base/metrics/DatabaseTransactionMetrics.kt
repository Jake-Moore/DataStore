package com.kamikazejam.datastore.base.metrics

sealed interface DatabaseTransactionMetrics {
    fun onTryUpdateTransaction() {}
}