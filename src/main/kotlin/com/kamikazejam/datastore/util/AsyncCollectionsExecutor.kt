package com.kamikazejam.datastore.util

import com.kamikazejam.datastore.base.Collection
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

/**
 * Executes a List of collections in smart order from their dependencies, as parallel as possible.
 * @param <T> the type of Collection
</T> */
class AsyncCollectionsExecutor<T : Collection<*, *>>(var collections: List<T>, execution: Execution<T>, timeoutSec: Long) {
    fun interface Execution<T : Collection<*, *>> {
        fun run(collection: T)
    }

    private val queue: MutableMap<String, T> = HashMap() // Note: only remove from queue when T is completed
    private val currentExecutions: MutableList<CompletableFuture<String>> = ArrayList()
    private val execution: Execution<T>
    private val completed: MutableSet<String?> = HashSet()
    private val timeoutSec: Long

    private var future = CompletableFuture<Void>()
    fun executeInOrder(): CompletableFuture<Void> {
        future = CompletableFuture()
        // Bukkit.getLogger().severe("[AsyncCollectionsExecutor] FUTURE created");
        tryQueue()
        return future
    }

    private val executed: MutableSet<String> = HashSet()

    init {
        val size = collections.size.toLong()
        // Filter out null collections
        collections = collections.stream().filter { obj: T -> Objects.nonNull(obj) }.toList()
        require(collections.size.toLong() == size) { "We removed null collections from the list!" }

        val sorted = collections.stream().sorted().toList()
        sorted.forEach { c: T -> queue[c.name] = c }
        this.execution = execution
        this.timeoutSec = timeoutSec

        // Validation of dependencies
        val collNames = collections.stream().map { obj: T -> obj.name }.collect(Collectors.toSet())
        queue.values.forEach { collection: T ->
            collection.dependencyNames.forEach { dependency: String? ->
                require(collNames.contains(dependency)) { "Collection " + collection.name + " has a dependency on " + dependency + " which does not exist!" }
            }
        }
    }

    private fun tryQueue() {
        // If there is nothing left in the queue, we are done
        if (queue.isEmpty()) {
            // Bukkit.getLogger().severe("[AsyncCollectionsExecutor] FUTURE completed");
            future.complete(null)
            return
        }

        // Form a separate list to prevent concurrent modification
        (ArrayList(queue.values)).forEach { c: T ->
            // Happens in rare cases where quick swaps occur, it's not an error, the plr is no longer here
            if (!completed.containsAll(c.dependencyNames)) {
                return@forEach
            } // Skip if dependencies aren't met

            if (executed.contains(c.name)) {
                return@forEach
            } // Skip if already running/ran


            // We have completed all required dependencies, so we can execute this collection
            val f = CompletableFuture.supplyAsync {
                // Bukkit.getLogger().warning("[AsyncCollectionsExecutor] Running " + c.getName());
                execution.run(c)
                c
            }.orTimeout(timeoutSec, TimeUnit.SECONDS)
            executed.add(c.name)
            f.whenComplete { collection: T, t: Throwable? ->
                // If we run into an exception running the Execution, we should complete exceptionally
                if (t != null) {
                    future.completeExceptionally(t)
                    currentExecutions.forEach { cf: CompletableFuture<String> -> cf.cancel(true) }
                    currentExecutions.clear()
                    return@whenComplete
                }
                try {
                    queue.remove(collection.name)
                    completed.add(collection.name)
                    // Bukkit.getLogger().warning("[AsyncCollectionsExecutor] " + collection.getName() + " completed, isDoneAlr:? " + future.isDone());
                    if (future.isDone) {
                        return@whenComplete
                    }

                    tryQueue()
                } catch (t2: Throwable) {
                    t2.printStackTrace()
                }
            }
        }
    }
}
