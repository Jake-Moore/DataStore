package com.kamikazejam.datastore.base.exception

/**
 * Base [Exception] class for ALL of DataStore
 */
open class DataStoreException(override val message: String, override val cause: Throwable? = null) : Exception(message, cause)