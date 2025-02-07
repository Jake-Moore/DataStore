package com.kamikazejam.datastore.util

interface ErrorConsumer {
    fun accept(error: Throwable)
}