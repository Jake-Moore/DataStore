package com.kamikazejam.datastore.base.metrics

sealed interface StorageMetrics {
    // Normal Usage
    fun onRead()
    fun onReadAll()
    fun onSave()
    fun onUpdate()
    fun onHasKey()
    fun onDelete()
    fun onDeleteAll()
    fun readKeys()
    fun onSize()
    fun onReadByIndex()

    // Fail States
    fun onCreateFail()
    fun onDeleteFail()
    fun onReadFail()
    fun onUpdateFail()
    fun onUpdateFailNotFound()
    fun onHasKeyFail()
    fun onReadIdFail()
}