package com.kamikazejam.datastore.util

import com.kamikazejam.datastore.base.Cache
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

/**
 * Executes a List of caches in smart order from their dependencies, as parallel as possible.
 * @param <T> the type of Cache
</T> */
class AsyncCachesExecutor<T : Cache<*, *>>(var caches: List<T>, execution: Execution<T>, timeoutSec: Long) {
    fun interface Execution<T : Cache<*, *>> {
        fun run(cache: T)
    }

    private val queue: MutableMap<String, T> = HashMap() // Note: only remove from queue when T is completed
    private val currentExecutions: MutableList<CompletableFuture<String>> = ArrayList()
    private val execution: Execution<T>
    private val completed: MutableSet<String?> = HashSet()
    private val timeoutSec: Long

    private var future = CompletableFuture<Void>()
    fun executeInOrder(): CompletableFuture<Void> {
        future = CompletableFuture()
        // Bukkit.getLogger().severe("[AsyncCachesExecutor] FUTURE created");
        tryQueue()
        return future
    }

    private val executed: MutableSet<String> = HashSet()

    init {
        val size = caches.size.toLong()
        // Filter out null caches
        caches = caches.stream().filter { obj: T -> Objects.nonNull(obj) }.toList()
        require(caches.size.toLong() == size) { "We removed null caches from the list!" }

        val sorted = caches.stream().sorted().toList()
        sorted.forEach { c: T -> queue[c.name] = c }
        this.execution = execution
        this.timeoutSec = timeoutSec

        // Validation of dependencies
        val cacheNames = caches.stream().map { obj: T -> obj.name }.collect(Collectors.toSet())
        queue.values.forEach { cache: T ->
            cache.dependencyNames.forEach { dependency: String? ->
                require(cacheNames.contains(dependency)) { "Cache " + cache.name + " has a dependency on " + dependency + " which does not exist!" }
            }
        }
    }

    private fun tryQueue() {
        // If there is nothing left in the queue, we are done
        if (queue.isEmpty()) {
            // Bukkit.getLogger().severe("[AsyncCachesExecutor] FUTURE completed");
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


            // We have completed all required dependencies, so we can execute this cache
            val f = CompletableFuture.supplyAsync {
                // Bukkit.getLogger().warning("[AsyncCachesExecutor] Running " + c.getName());
                execution.run(c)
                c
            }.orTimeout(timeoutSec, TimeUnit.SECONDS)
            executed.add(c.name)
            f.whenComplete { cache: T, t: Throwable? ->
                // If we run into an exception running the Execution, we should complete exceptionally
                if (t != null) {
                    future.completeExceptionally(t)
                    currentExecutions.forEach { cf: CompletableFuture<String> -> cf.cancel(true) }
                    currentExecutions.clear()
                    return@whenComplete
                }
                try {
                    queue.remove(cache.name)
                    completed.add(cache.name)
                    // Bukkit.getLogger().warning("[AsyncCachesExecutor] " + cache.getName() + " completed, isDoneAlr:? " + future.isDone());
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
