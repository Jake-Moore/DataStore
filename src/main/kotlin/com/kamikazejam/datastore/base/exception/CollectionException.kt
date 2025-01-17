package com.kamikazejam.datastore.base.exception

import com.kamikazejam.datastore.base.Collection

@Suppress("unused")
open class CollectionException : Exception {
    constructor(collection: Collection<*, *>) : super("C: [" + collection.name + "] exception")

    constructor(message: String, collection: Collection<*, *>) : super("C: [" + collection.name + "] exception: " + message)

    constructor(
        message: String,
        cause: Throwable?,
        collection: Collection<*, *>
    ) : super("C: [" + collection.name + "] exception: " + message, cause)

    constructor(cause: Throwable?, collection: Collection<*, *>) : super("C: [" + collection.name + "] exception: ", cause)

    constructor(
        message: String,
        cause: Throwable?,
        enableSuppression: Boolean,
        writableStackTrace: Boolean,
        collection: Collection<*, *>
    ) : super("C: [" + collection.name + "] exception: " + message, cause, enableSuppression, writableStackTrace)
}
