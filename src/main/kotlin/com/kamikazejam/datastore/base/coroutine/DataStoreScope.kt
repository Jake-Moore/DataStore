@file:Suppress("unused", "UnusedReceiverParameter")

package com.kamikazejam.datastore.base.coroutine

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.log.LoggerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object GlobalDataStoreScope : CoroutineScope {
    // Shared Job for all DataStoreScope instances
    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    // Cancels ALL DataStoreScope instances and ALL Coroutines
    fun cancelAll() {
        job.cancel()
    }

    fun awaitAllChildrenCompletion(logger: LoggerService) {
        val delayMS = 100L

        var loop = 0
        while (true) {
            // Get the list of active child jobs
            val activeChildren = job.children.toList()

            // Log the number of active children (every 1 second roughly)
            if (delayMS * loop >= 1000L) {
                logger.warn("Waiting on Async Tasks: ${activeChildren.size} tasks running.")
                loop = 0
            }

            // If there are no active children, break the loop
            if (activeChildren.isEmpty()) {
                break
            }

            // Delay before checking again
            Thread.sleep(delayMS)
            loop++
        }
    }

    fun logActiveCoroutines() {
        val job = coroutineContext[Job]
        val activeCoroutines = job?.children?.toList() ?: emptyList()
        println("Active coroutines count: ${activeCoroutines.size}")
        activeCoroutines.forEach { childJob ->
            println("\tChild Job: isActive=${childJob.isActive}, isCompleted=${childJob.isCompleted}, isCancelled=${childJob.isCancelled}")
        }
    }
}

@Suppress("unused")
interface DataStoreScope : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = GlobalDataStoreScope.coroutineContext
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

