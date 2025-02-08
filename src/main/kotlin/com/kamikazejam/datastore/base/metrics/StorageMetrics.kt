package com.kamikazejam.datastore.base.metrics

sealed interface StorageMetrics {
    fun onRead() {}
    fun onReadAll() {}
    fun onSave() {}
    fun onUpdate() {}
    fun onHas() {}
    fun onDelete() {}
    fun onDeleteAll() {}
    fun readKeys() {}
    fun onSize() {}
}