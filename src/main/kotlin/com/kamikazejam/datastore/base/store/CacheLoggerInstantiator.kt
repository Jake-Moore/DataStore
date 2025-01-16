package com.kamikazejam.datastore.base.store

import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.log.LoggerService

fun interface CacheLoggerInstantiator {
    fun instantiate(cache: Cache<*, *>): LoggerService
}
