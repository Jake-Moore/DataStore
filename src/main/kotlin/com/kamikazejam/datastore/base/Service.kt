package com.kamikazejam.datastore.base

@Suppress("unused")
interface Service {
    suspend fun start(): Boolean

    suspend fun shutdown(): Boolean

    val running: Boolean
}
