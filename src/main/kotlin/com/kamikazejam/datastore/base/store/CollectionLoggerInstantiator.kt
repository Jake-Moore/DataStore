package com.kamikazejam.datastore.base.store

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.log.LoggerService

fun interface CollectionLoggerInstantiator {
    fun instantiate(collection: Collection<*, *>): LoggerService
}
