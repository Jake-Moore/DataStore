package com.kamikazejam.datastore.base.async.exception

class AsyncHandlerException(
    message: String,
    val exception: Throwable
    // Passing "exception" as the cause, to support the Throwable constructor
    // "exception" is also public and nonnull for direct access on this class
) : RuntimeException(message, exception)