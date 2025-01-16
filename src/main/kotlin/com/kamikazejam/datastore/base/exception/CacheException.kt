package com.kamikazejam.datastore.base.exception

import com.kamikazejam.datastore.base.Cache

@Suppress("unused")
open class CacheException : Exception {
    constructor(cache: Cache<*, *>) : super("C: [" + cache.name + "] exception")

    constructor(message: String, cache: Cache<*, *>) : super("C: [" + cache.name + "] exception: " + message)

    constructor(
        message: String,
        cause: Throwable?,
        cache: Cache<*, *>
    ) : super("C: [" + cache.name + "] exception: " + message, cause)

    constructor(cause: Throwable?, cache: Cache<*, *>) : super("C: [" + cache.name + "] exception: ", cause)

    constructor(
        message: String,
        cause: Throwable?,
        enableSuppression: Boolean,
        writableStackTrace: Boolean,
        cache: Cache<*, *>
    ) : super("C: [" + cache.name + "] exception: " + message, cause, enableSuppression, writableStackTrace)
}
