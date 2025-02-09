package com.kamikazejam.datastore.base.metrics

@Suppress("unused")
open class MetricsListenerPartial : MetricsListener {
    // StorageMetrics - Normal Usage
    override fun onRead() {}
    override fun onReadAll() {}
    override fun onSave() {}
    override fun onUpdate() {}
    override fun onHasKey() {}
    override fun onDelete() {}
    override fun onDeleteAll() {}
    override fun readKeys() {}
    override fun onSize() {}
    override fun onReadByIndex() {}

    // StorageMetrics - Fail States
    override fun onCreateFail() {}
    override fun onDeleteFail() {}
    override fun onReadFail() {}
    override fun onUpdateFail() {}
    override fun onUpdateFailNotFound() {}
    override fun onHasKeyFail() {}
    override fun onReadIdFail() {}

    // DatabaseTransactionMetrics
    override fun onTryUpdateTransaction() {}
    override fun onUpdateTransactionLimitReached() {}
    override fun onTimerUpdateTransaction(milliseconds: Long) {}
    override fun onTimerUpdatesSuccess(milliseconds: Long) {}
}