package com.kamikazejam.datastore.base.exception.update

class TransactionRetryLimitExceededException(override val message: String, override val cause: Throwable? = null) : UpdateException(message, cause)
