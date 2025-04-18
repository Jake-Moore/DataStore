package com.kamikazejam.datastore.util

import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.coroutine.DataStoreScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

/**
 * Executes a List of collections in smart order from their dependencies, as parallel as possible.
 * @param <T> the type of Collection
</T> */
class AsyncCollectionsExecutor<T : Collection<*, *>>(
    var collections: List<T>,
    private val timeoutSec: Long,
    private val execution: suspend (T) -> Unit,
) : DataStoreScope {

    private val queue: MutableMap<String, T> = ConcurrentHashMap() // Note: only remove from queue when T is completed
    private val activeJobs: MutableList<Job> = ArrayList()
    private val completed: MutableSet<String> = HashSet()
    private val executed: MutableSet<String> = HashSet()

    init {
        val size = collections.size.toLong()
        // Filter out null collections
        collections = collections.stream().filter { obj: T -> Objects.nonNull(obj) }.toList()
        require(collections.size.toLong() == size) { "We removed null collections from the list!" }

        val sorted = collections.stream().sorted().toList()
        sorted.forEach { c: T -> queue[c.name] = c }

        // Validation of dependencies
        val collNames = collections.map { it.name }.toSet()
        queue.values.forEach { collection: T ->
            collection.dependencyNames.forEach { dependency: String? ->
                require(collNames.contains(dependency)) { "Collection " + collection.name + " has a dependency on " + dependency + " which does not exist!" }
            }
        }
    }

    fun executeInOrder(): Deferred<Unit> = async {
        try {
            while (queue.isNotEmpty()) {
                tryQueue()
                // Wait for all current jobs to complete before next iteration
                activeJobs.joinAll()
                activeJobs.clear()
            }
        } catch (t: Throwable) {
            activeJobs.forEach { it.cancel() }
            throw t
        }
    }

    private suspend fun tryQueue() {
        // If there is nothing left in the queue, we are done
        if (queue.isEmpty()) {
            return
        }

        coroutineScope {
            val jobsToLaunch = ArrayList(queue.values).filter { c ->
                !executed.contains(c.name) && completed.containsAll(c.dependencyNames)
            }

            jobsToLaunch.forEach { c ->
                val job = launch {
                    try {
                        withTimeout(timeoutSec * 1000) {
                            execution(c)
                        }
                        queue.remove(c.name)
                        completed.add(c.name)
                    } catch (t: Throwable) {
                        cancel("Failed to execute collection ${c.name}", t)
                        throw t
                    }
                }
                executed.add(c.name)
                activeJobs.add(job)
            }
        }
    }
}
