package com.kamikazejam.datastore.base.exception

import com.kamikazejam.datastore.base.Cache

@Suppress("unused")
class DuplicateCacheException : CacheException {
    constructor(cache: Cache<*, *>) : super(cache)

    constructor(message: String, cache: Cache<*, *>) : super(message, cache)

    constructor(message: String, cause: Throwable?, cache: Cache<*, *>) : super(message, cause, cache)

    constructor(cause: Throwable?, cache: Cache<*, *>) : super(cause, cache)

    constructor(
        message: String,
        cause: Throwable?,
        enableSuppression: Boolean,
        writableStackTrace: Boolean,
        cache: Cache<*, *>
    ) : super(message, cause, enableSuppression, writableStackTrace, cache)
}
