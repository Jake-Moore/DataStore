@file:Suppress("unused", "UnusedReceiverParameter")

package com.kamikazejam.datastore.base.coroutine

import com.kamikazejam.datastore.DataStoreSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Suppress("unused")
interface DataStoreScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO
}

fun DataStoreScope.runSync(runnable: Runnable) {
    val plugin = DataStoreSource.get()
    plugin.server.scheduler.runTask(plugin, runnable)
}

suspend fun DataStoreScope.runSyncBlocking(block: () -> Unit) {
    return suspendCoroutine { continuation ->
        val plugin = DataStoreSource.get()
        plugin.server.scheduler.runTask(plugin) {
            try {
                block()
                continuation.resume(Unit)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }
}

