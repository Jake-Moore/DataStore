package com.kamikazejam.datastore.base.exception.update

import com.kamikazejam.datastore.base.exception.DataStoreException

open class RejectUpdateException(override val message: String, override val cause: Throwable? = null) : DataStoreException(message, cause)