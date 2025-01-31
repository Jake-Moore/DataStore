package com.kamikazejam.datastore.base.exception

import com.kamikazejam.datastore.base.Collection

@Suppress("unused")
class DuplicateCollectionException : CollectionException {
    constructor(collection: Collection<*, *>) : super(collection)

    constructor(message: String, collection: Collection<*, *>) : super(message, collection)

    constructor(message: String, cause: Throwable?, collection: Collection<*, *>) : super(message, cause, collection)

    constructor(cause: Throwable?, collection: Collection<*, *>) : super(cause, collection)
}
