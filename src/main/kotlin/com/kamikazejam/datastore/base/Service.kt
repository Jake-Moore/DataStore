package com.kamikazejam.datastore.base

@Suppress("unused")
interface Service {
    fun start(): Boolean

    fun shutdown(): Boolean

    val running: Boolean
}
