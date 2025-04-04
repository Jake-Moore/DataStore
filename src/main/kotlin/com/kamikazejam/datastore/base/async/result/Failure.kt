package com.kamikazejam.datastore.base.async.result

import com.kamikazejam.datastore.base.async.exception.AsyncHandlerException

data class Failure<T>(val error: AsyncHandlerException) : DefiniteResult<T>, OptionalResult<T>, UpdateResult<T> {
    override fun isSuccess(): Boolean = false
    override fun isFailure(): Boolean = true
    override fun isEmpty(): Boolean = false
    override fun isRejected(): Boolean = false
    
    override fun getOrNull(): T? = null
    override fun exceptionOrNull(): Throwable = error
}