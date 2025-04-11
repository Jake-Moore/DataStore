@file:Suppress("unused", "UnusedReceiverParameter")

package com.kamikazejam.datastore.base.coroutine

import com.kamikazejam.datastore.DataStoreSource
import com.kamikazejam.datastore.base.log.LoggerService
import com.kamikazejam.datastore.util.ErrorConsumer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.bukkit.Bukkit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Suppress("MemberVisibilityCanBePrivate")
object GlobalDataStoreScope : CoroutineScope {
    var CUSTOM_ERROR_CONSUMERS: MutableList<ErrorConsumer> = mutableListOf()

    // Shared SupervisorJob ensures that a child coroutine failure
    // doesn't cancel the parent job or other child coroutines.
    private val supervisorJob = SupervisorJob()

    // CoroutineExceptionHandler to log errors
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (CUSTOM_ERROR_CONSUMERS.isEmpty()) {
            DataStoreSource.get().logger.severe("Unhandled coroutine error: $throwable")
            throwable.printStackTrace()
        } else {
            CUSTOM_ERROR_CONSUMERS.forEach { it.accept(throwable) }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + supervisorJob + exceptionHandler

    // Cancels ALL DataStoreScope instances and ALL Coroutines
    fun cancelAll() {
        supervisorJob.cancel()
    }

    fun awaitAllChildrenCompletion(logger: LoggerService, maxWaitMS: Long = 60_000L): Boolean {
        var totalWait = 0L
        val delayMS = 100L

        var loop = 0
        while (true) {
            // Get the list of active child jobs
            val activeChildren = supervisorJob.children.toList()

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
            totalWait += delayMS
            if (totalWait >= maxWaitMS) {
                return false
            }
            loop++
        }
        return true
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
        Bukkit.getScheduler().runTask(DataStoreSource.get()) {
            try {
                block()
                continuation.resume(Unit)
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        }
    }
}

suspend fun <T> DataStoreScope.runSyncFetching(block: () -> T): T {
    return suspendCoroutine { continuation ->
        Bukkit.getScheduler().runTask(DataStoreSource.get()) {
            try {
                val result: T = block()
                continuation.resume(result)
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        }
    }
}
