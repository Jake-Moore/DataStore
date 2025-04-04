package com.kamikazejam.datastore.base.async.result

data class Success<T>(val value: T) : DefiniteResult<T>, OptionalResult<T>, UpdateResult<T> {
    override fun isSuccess(): Boolean = true
    override fun isFailure(): Boolean = false
    override fun isEmpty(): Boolean = false
    override fun isRejected(): Boolean = false

    override fun getOrNull(): T = value
    override fun exceptionOrNull(): Throwable? = null
}