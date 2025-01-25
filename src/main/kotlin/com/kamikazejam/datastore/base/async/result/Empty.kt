package com.kamikazejam.datastore.base.async.result

class Empty<T> : OptionalResult<T> {
    override fun isSuccess(): Boolean = false
    override fun isFailure(): Boolean = false
    override fun isEmpty(): Boolean = true

    override fun getOrNull(): T? = null
    override fun exceptionOrNull(): Throwable? = null
}