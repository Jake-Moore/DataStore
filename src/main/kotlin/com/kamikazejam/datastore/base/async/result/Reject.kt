package com.kamikazejam.datastore.base.async.result

import com.kamikazejam.datastore.base.exception.update.RejectUpdateException

data class Reject<T>(val cause: RejectUpdateException) : UpdateResult<T> {
    override fun isSuccess(): Boolean = false
    override fun isFailure(): Boolean = false
    override fun isRejected(): Boolean = true

    override fun getOrNull(): T? = null
    override fun exceptionOrNull(): RejectUpdateException = cause
}